package com.queststack.ui.screen.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.queststack.data.DataContainer
import com.queststack.data.db.Category
import com.queststack.data.db.QuestionWithRounds
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

data class PracticeUiState(
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val difficulty: Int? = null,
    val current: QuestionWithRounds? = null,
    val loading: Boolean = true,
    val empty: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeViewModel(
    private val questionRepository: QuestionRepository = DataContainer.questionRepository,
    private val categoryRepository: CategoryRepository = DataContainer.categoryRepository,
) : ViewModel() {

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    private val _difficulty = MutableStateFlow<Int?>(null)
    // 换一题触发信号（每次 +1，与筛选条件一起驱动重载）
    private val _refreshTick = MutableStateFlow(0)

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    init {
        // 分类列表独立收集
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
        // 筛选变化 / 手动换题时，flatMapLatest 取消旧加载并重新随机
        viewModelScope.launch {
            combine(_selectedCategoryId, _difficulty, _refreshTick) { c, d, _ -> c to d }
                .flatMapLatest { (categoryId, difficulty) ->
                    flow {
                        _uiState.update { it.copy(loading = true) }
                        emit(loadRandom(categoryId, difficulty))
                    }
                }
                .collect { current ->
                    _uiState.update {
                        it.copy(current = current, loading = false, empty = current == null)
                    }
                }
        }
    }

    /** 随机取一个完整题目（id 列表首元素 → getQuestion 取全量） */
    private suspend fun loadRandom(categoryId: Long?, difficulty: Int?): QuestionWithRounds? {
        val ids = questionRepository.randomQuestionIds(categoryId, difficulty)
        if (ids.isEmpty()) return null
        return questionRepository.getQuestion(ids.first())
    }

    fun selectCategory(id: Long?) {
        if (_selectedCategoryId.value != id) _selectedCategoryId.value = id
        _uiState.update { it.copy(selectedCategoryId = id) }
    }

    fun selectDifficulty(d: Int?) {
        if (_difficulty.value != d) _difficulty.value = d
        _uiState.update { it.copy(difficulty = d) }
    }

    fun shuffle() {
        _refreshTick.update { it + 1 }
    }
}
