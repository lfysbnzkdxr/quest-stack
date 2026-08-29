package com.queststack.ui.screen.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.queststack.data.DataContainer
import com.queststack.data.db.Category
import com.queststack.data.db.Question
import com.queststack.data.repository.CategoryRepository
import com.queststack.data.repository.QuestionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 一次闪卡练题会话：
 * - categoryId / difficulty 为空表示不限（全库）；
 * - startQuestionId 非空时（从题库点击进入）优先展示该题，之后随机换题。
 */
data class PracticeSession(
    val categoryId: Long? = null,
    val difficulty: Int? = null,
    val startQuestionId: Long? = null,
)

data class PracticeSessionUiState(
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val difficulty: Int? = null,
    val current: Question? = null,
    val revealed: Boolean = false,
    val canPrevious: Boolean = false,
    val loading: Boolean = true,
    val empty: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeSessionViewModel(
    private var session: PracticeSession,
    private val questionRepository: QuestionRepository = DataContainer.questionRepository,
    private val categoryRepository: CategoryRepository = DataContainer.categoryRepository,
) : ViewModel() {

    private val _selectedCategoryId = MutableStateFlow(session.categoryId)
    private val _difficulty = MutableStateFlow(session.difficulty)
    private val _nextTick = MutableStateFlow(0)
    /** 已看过的题 id 栈（下一题时压入，上一题时弹出），首题时为空 */
    private val history = ArrayDeque<Long>()
    private var firstLoad = true

    private val _uiState = MutableStateFlow(
        PracticeSessionUiState(
            selectedCategoryId = session.categoryId,
            difficulty = session.difficulty,
        )
    )
    val uiState: StateFlow<PracticeSessionUiState> = _uiState.asStateFlow()

    init {
        // 分类列表独立收集
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
        // 筛选 / 换题触发重抽：flatMapLatest 取消旧加载并重新随机
        viewModelScope.launch {
            combine(_selectedCategoryId, _difficulty, _nextTick) { c, d, _ -> c to d }
                .flatMapLatest { (categoryId, difficulty) ->
                    flow {
                        _uiState.update { it.copy(loading = true) }
                        emit(loadRandom(categoryId, difficulty))
                    }
                }
                .collect { current ->
                    // revealed 与 current 同帧重置，避免旧题停留时先收起答案造成闪帧
                    _uiState.update {
                        it.copy(
                            current = current,
                            revealed = false,
                            loading = false,
                            empty = current == null,
                        )
                    }
                }
        }
    }

    /** 范围内随机抽题：首次优先展示会话指定题，之后排除当前题避免连出同一道 */
    private suspend fun loadRandom(categoryId: Long?, difficulty: Int?): Question? {
        val ids = questionRepository.randomQuestionIds(categoryId, difficulty)
        if (ids.isEmpty()) return null
        val startId = session.startQuestionId
        val target = when {
            firstLoad && startId != null && ids.contains(startId) -> startId
            else -> ids.firstOrNull { it != _uiState.value.current?.id } ?: ids.first()
        }
        firstLoad = false
        return questionRepository.getQuestion(target)
    }

    fun selectCategory(id: Long?) {
        if (_selectedCategoryId.value != id) _selectedCategoryId.value = id
        history.clear()
        _uiState.update { it.copy(selectedCategoryId = id, canPrevious = false) }
    }

    fun selectDifficulty(d: Int?) {
        if (_difficulty.value != d) _difficulty.value = d
        history.clear()
        _uiState.update { it.copy(difficulty = d, canPrevious = false) }
    }

    /** 切换答案展开/收起状态 */
    fun toggleReveal() {
        _uiState.update { it.copy(revealed = !it.revealed) }
    }

    /** 下一题：把当前题压入历史栈，随机重抽并收起答案 */
    fun next() {
        _uiState.value.current?.let { history.addLast(it.id) }
        _uiState.update { it.copy(canPrevious = history.isNotEmpty()) }
        _nextTick.value++
    }

    /** 上一题：弹出历史栈顶并直接加载该题（不走随机），首题时无操作 */
    fun previous() {
        val id = history.removeLastOrNull() ?: return
        viewModelScope.launch {
            val question = questionRepository.getQuestion(id)
            _uiState.update {
                it.copy(
                    current = question ?: it.current,
                    revealed = false,
                    canPrevious = history.isNotEmpty(),
                )
            }
        }
    }

    /**
     * 重新开始一次会话（进入练题页时由 UI 调用）：
     * ViewModel 挂在 Activity 的 ViewModelStore，同 key 会话会复用同一个实例，
     * 必须在此重置会话参数与显示状态，否则旧题/答案展开状态残留、startQuestionId 失效。
     */
    fun start(session: PracticeSession) {
        this.session = session
        firstLoad = true
        history.clear()
        _selectedCategoryId.value = session.categoryId
        _difficulty.value = session.difficulty
        _uiState.update {
            it.copy(
                selectedCategoryId = session.categoryId,
                difficulty = session.difficulty,
                current = null,
                revealed = false,
                canPrevious = false,
                loading = true,
                empty = false,
            )
        }
        // 筛选值可能未变化（同 key 复用场景），用 tick 强制触发重抽
        _nextTick.value++
    }
}
