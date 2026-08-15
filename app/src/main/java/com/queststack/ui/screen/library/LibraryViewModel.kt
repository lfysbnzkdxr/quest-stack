package com.queststack.ui.screen.library

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val difficulty: Int? = null,
    val questions: List<QuestionWithRounds> = emptyList(),
    val loading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val questionRepository: QuestionRepository = DataContainer.questionRepository,
    private val categoryRepository: CategoryRepository = DataContainer.categoryRepository,
) : ViewModel() {

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    private val _difficulty = MutableStateFlow<Int?>(null)

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        // 分类列表独立收集
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
        // 题目列表：筛选条件变化时用 flatMapLatest 重新收集对应 Flow
        viewModelScope.launch {
            combine(_selectedCategoryId, _difficulty) { categoryId, difficulty -> categoryId to difficulty }
                .flatMapLatest { (categoryId, difficulty) ->
                    questionRepository.observeQuestions(categoryId, difficulty)
                }
                .collect { questions ->
                    _uiState.update { it.copy(questions = questions, loading = false) }
                }
        }
    }

    fun selectCategory(id: Long?) {
        _selectedCategoryId.value = id
        _uiState.update { it.copy(selectedCategoryId = id) }
    }

    fun selectDifficulty(d: Int?) {
        _difficulty.value = d
        _uiState.update { it.copy(difficulty = d) }
    }

    fun deleteQuestion(question: QuestionWithRounds) {
        viewModelScope.launch {
            questionRepository.deleteQuestion(question.question.id)
        }
    }
}
