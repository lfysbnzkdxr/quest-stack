package com.queststack.data.repository

import com.queststack.data.db.DayPracticeCount
import com.queststack.data.db.PracticeLogDao
import com.queststack.data.db.PracticeLogEntity
import kotlinx.coroutines.flow.Flow

class PracticeLogRepositoryImpl(
    private val practiceLogDao: PracticeLogDao
) : PracticeLogRepository {

    override fun observeDailyCounts(dayOffsetMillis: Long): Flow<List<DayPracticeCount>> =
        practiceLogDao.observeDailyCounts(dayOffsetMillis)

    override fun observeRecent(limit: Int): Flow<List<PracticeLogEntity>> =
        practiceLogDao.observeRecent(limit)

    override fun observeLogsOfDay(dayEpoch: Long, dayOffsetMillis: Long): Flow<List<PracticeLogEntity>> =
        practiceLogDao.observeLogsOfDay(dayEpoch, dayOffsetMillis)

    override suspend fun insert(log: PracticeLogEntity) {
        practiceLogDao.insert(log)
    }
}
