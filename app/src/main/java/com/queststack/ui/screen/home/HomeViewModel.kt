package com.queststack.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.queststack.data.DataContainer
import com.queststack.data.db.Category
import com.queststack.data.repository.CategoryRepository
import com.queststack.data.repository.PracticeLogRepository
import com.queststack.data.repository.QuestionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.TimeZone

data class HomeUiState(
    val categories: List<Category> = emptyList(),
    /** categoryId → 题数（null key 为未分类） */
    val counts: Map<Long?, Int> = emptyMap(),
    /** 今日练题数（练题记录） */
    val todayPracticeCount: Int = 0,
    /** 累计练题数 */
    val totalPracticeCount: Int = 0,
) {
    val totalCount: Int get() = counts.values.sum()
}

class HomeViewModel(
    private val questionRepository: QuestionRepository = DataContainer.questionRepository,
    private val categoryRepository: CategoryRepository = DataContainer.categoryRepository,
    private val practiceLogRepository: PracticeLogRepository = DataContainer.practiceLogRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // 时区偏移：练题记录按本地自然日归组（与练题记录页保持一致）
        val dayOffsetMillis = TimeZone.getDefault().getOffset(System.currentTimeMillis()).toLong()
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
        viewModelScope.launch {
            questionRepository.observeCategoryCounts().collect { counts ->
                _uiState.update { it.copy(counts = counts.associate { it.categoryId to it.count }) }
            }
        }
        viewModelScope.launch {
            practiceLogRepository.observeDailyCounts(dayOffsetMillis).collect { list ->
                val todayEpoch = LocalDate.now().toEpochDay()
                _uiState.update {
                    it.copy(
                        todayPracticeCount = list.firstOrNull { d -> d.dayEpoch == todayEpoch }?.count ?: 0,
                        totalPracticeCount = list.sumOf { d -> d.count },
                    )
                }
            }
        }
    }
}
