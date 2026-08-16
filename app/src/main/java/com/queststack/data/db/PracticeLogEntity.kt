package com.queststack.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 练题记录：闪卡页看完答案点"下一题"时写入一条。
 * 题目信息为快照（不关联题目表），题目删除后记录仍可展示。
 */
@Entity(
    tableName = "practice_logs",
    indices = [Index("practicedAt")],
)
data class PracticeLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionTitle: String,
    val categoryName: String?,
    val difficulty: Int,
    val practicedAt: Long,
)
