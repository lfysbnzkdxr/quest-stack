package com.queststack.ui.screen.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.queststack.data.DataContainer
import com.queststack.data.db.QuestionWithRounds
import com.queststack.data.repository.QuestionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ChatUiState(
    val question: QuestionWithRounds? = null,
    /** 0 = 未看主答案，1 = 已看主答案（单题刷只揭示主问题答案，不播放追问链） */
    val revealed: Int = 0,
    val history: List<Long> = emptyList(),
    val currentId: Long,
    val loading: Boolean = true,
)

/**
 * 答题聊天页状态机：
 * - [ChatUiState.revealed]：是否已揭示主答案（0/1）。
 * - [ChatUiState.history]：上一题栈，"上一问"时 pop。
 */
class PracticeChatViewModel(
    questionId: Long,
    private val questionRepository: QuestionRepository = DataContainer.questionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(currentId = questionId))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load(questionId)
    }

    private fun load(id: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val question = questionRepository.getQuestion(id)
            if (!isActive) return@launch
            _uiState.update {
                it.copy(
                    question = question,
                    currentId = id,
                    revealed = 0,
                    loading = false,
                )
            }
        }
    }

    /** 揭示主问题参考答案；已揭示后 no-op */
    fun revealAnswer() {
        val state = _uiState.value
        if (state.revealed < 1) {
            _uiState.update { it.copy(revealed = 1) }
        }
    }

    /** 浮层重新打开时重置为未看答案状态（保留当前题，清空上一问栈） */
    fun reset() {
        _uiState.update { it.copy(revealed = 0, history = emptyList()) }
    }

    /** 随机取一个新题：当前 id 入 history，重新加载，revealed 重置 0 */
    fun nextQuestion() {
        val state = _uiState.value
        viewModelScope.launch {
            val ids = questionRepository.randomQuestionIds(null, null)
            val next = ids.firstOrNull { it != state.currentId } ?: return@launch
            _uiState.update { it.copy(history = it.history + it.currentId) }
            load(next)
        }
    }

    /** 上一问：history 非空时 pop 并重新加载，revealed 重置 0 */
    fun prevQuestion() {
        val state = _uiState.value
        if (state.history.isEmpty()) return
        val prev = state.history.last()
        _uiState.update { it.copy(history = it.history.dropLast(1)) }
        load(prev)
    }
}
