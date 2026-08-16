package com.queststack.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** 某自然日的练题数（dayEpoch = (practicedAt + 时区偏移) / 86400000，按本地时区归组） */
data class DayPracticeCount(val dayEpoch: Long, val count: Int)

@Dao
interface PracticeLogDao {
    @Insert
    suspend fun insert(log: PracticeLogEntity)

    @Query("SELECT (practicedAt + :dayOffsetMillis) / 86400000 AS dayEpoch, COUNT(*) AS count FROM practice_logs GROUP BY dayEpoch")
    fun observeDailyCounts(dayOffsetMillis: Long): Flow<List<DayPracticeCount>>

    @Query("SELECT * FROM practice_logs ORDER BY practicedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<PracticeLogEntity>>

    @Query("SELECT * FROM practice_logs WHERE (practicedAt + :dayOffsetMillis) / 86400000 = :dayEpoch ORDER BY practicedAt DESC")
    fun observeLogsOfDay(dayEpoch: Long, dayOffsetMillis: Long): Flow<List<PracticeLogEntity>>
}
