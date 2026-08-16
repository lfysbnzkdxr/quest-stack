package com.queststack.ui.screen.practiceLog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.queststack.data.DataContainer
import com.queststack.data.db.PracticeLogEntity
import com.queststack.data.repository.PracticeLogRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.TimeZone

data class PracticeLogUiState(
    /** dayEpoch（自 1970-01-01 的本地日天数）→ 练题数 */
    val dailyCounts: Map<Long, Int> = emptyMap(),
    val selectedDayEpoch: Long = LocalDate.now().toEpochDay(),
    val selectedDayLogs: List<PracticeLogEntity> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeLogViewModel(
    private val practiceLogRepository: PracticeLogRepository = DataContainer.practiceLogRepository,
) : ViewModel() {

    private val _selectedDayEpoch = MutableStateFlow(LocalDate.now().toEpochDay())

    private val _uiState = MutableStateFlow(PracticeLogUiState())
    val uiState: StateFlow<PracticeLogUiState> = _uiState.asStateFlow()

    init {
        // 时区偏移：按本地自然日归组（与主页"今日"口径一致）
        val dayOffsetMillis = TimeZone.getDefault().getOffset(System.currentTimeMillis()).toLong()
        viewModelScope.launch {
            practiceLogRepository.observeDailyCounts(dayOffsetMillis).collect { list ->
                _uiState.update { it.copy(dailyCounts = list.associate { d -> d.dayEpoch to d.count }) }
            }
        }
        viewModelScope.launch {
            _selectedDayEpoch.flatMapLatest { day ->
                practiceLogRepository.observeLogsOfDay(day, dayOffsetMillis)
            }.collect { logs ->
                _uiState.update { it.copy(selectedDayLogs = logs) }
            }
        }
    }

    fun selectDay(dayEpoch: Long) {
        _selectedDayEpoch.value = dayEpoch
        _uiState.update { it.copy(selectedDayEpoch = dayEpoch) }
    }
}
