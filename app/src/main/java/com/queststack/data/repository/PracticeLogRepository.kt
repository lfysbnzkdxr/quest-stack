package com.queststack.data.repository

import com.queststack.data.db.DayPracticeCount
import com.queststack.data.db.PracticeLogEntity
import kotlinx.coroutines.flow.Flow

interface PracticeLogRepository {
    fun observeDailyCounts(dayOffsetMillis: Long): Flow<List<DayPracticeCount>>
    fun observeRecent(limit: Int): Flow<List<PracticeLogEntity>>
    fun observeLogsOfDay(dayEpoch: Long, dayOffsetMillis: Long): Flow<List<PracticeLogEntity>>
    suspend fun insert(log: PracticeLogEntity)
}
