package com.queststack.ui.screen.interview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.queststack.data.DataContainer
import com.queststack.data.db.QuestionWithRounds
import com.queststack.data.repository.QuestionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InterviewUiState(
    /** 打乱后的题序（随机取 [INTERVIEW_QUESTION_COUNT] 题，不足则全量） */
    val questions: List<QuestionWithRounds> = emptyList(),
    val currentIndex: Int = 0,
    /** 已揭示答案条数：0 = 未看主答案；第 1 条是主答案，其后依次是追问链答案 */
    val revealed: Int = 0,
    val loading: Boolean = true,
    val empty: Boolean = false,
    /** 全部题目完成后置 true，展示小结 */
    val finished: Boolean = false,
) {
    val current: QuestionWithRounds? get() = questions.getOrNull(currentIndex)

    /** 当前题是否已全部揭示（主答案 + 所有追问） */
    val currentExhausted: Boolean get() {
        val q = current ?: return false
        return revealed >= 1 + q.rounds.size
    }
}

/**
 * 模拟面试状态机：主问题 → 参考答案 → 追问链逐轮 reveal → 下一题，直至全部完成。
 * 面试模式不接 AI，追问与参考答案均来自预设数据；不做会话记录。
 */
class InterviewViewModel(
    categoryId: Long?,
    difficulty: Int?,
    private val questionRepository: QuestionRepository = DataContainer.questionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InterviewUiState())
    val uiState: StateFlow<InterviewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val questions = questionRepository.observeQuestions(categoryId, difficulty)
                .first()
                .shuffled()
                .take(INTERVIEW_QUESTION_COUNT)
            _uiState.update {
                it.copy(
                    questions = questions,
                    loading = false,
                    empty = questions.isEmpty(),
                )
            }
        }
    }

    /** 揭示当前轮答案（主答案或下一轮追问）；当前题耗尽后 no-op */
    fun revealAnswer() {
        val state = _uiState.value
        val q = state.current ?: return
        if (!state.currentExhausted) {
            _uiState.update { it.copy(revealed = it.revealed + 1) }
        }
    }

    /** 进入下一题；已是最后一题则完成面试 */
    fun nextQuestion() {
        val state = _uiState.value
        if (state.currentIndex >= state.questions.lastIndex) {
            _uiState.update { it.copy(finished = true) }
        } else {
            _uiState.update { it.copy(currentIndex = it.currentIndex + 1, revealed = 0) }
        }
    }

    companion object {
        const val INTERVIEW_QUESTION_COUNT = 10
    }
}
