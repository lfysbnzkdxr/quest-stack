package com.queststack.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.queststack.data.DataContainer
import com.queststack.data.repository.QuestionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    /** categoryId → 题数（null key 为未分类） */
    val counts: Map<Long?, Int> = emptyMap(),
) {
    val totalCount: Int get() = counts.values.sum()
}

class HomeViewModel(
    private val questionRepository: QuestionRepository = DataContainer.questionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            questionRepository.observeCategoryCounts().collect { counts ->
                _uiState.update { it.copy(counts = counts.associate { it.categoryId to it.count }) }
            }
        }
    }
}