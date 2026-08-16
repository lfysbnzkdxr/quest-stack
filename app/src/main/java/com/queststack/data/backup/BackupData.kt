package com.queststack.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupCategory(val name: String, val sortOrder: Int)

@Serializable
data class BackupQuestion(
    val title: String,
    /** v2 起：主问题参考答案（v1 文件反序列化时取默认值 ""） */
    val answer: String = "",
    val categoryName: String?,
    val difficulty: Int,
    val createdAt: Long,
    val updatedAt: Long,
    /** v1/v2 遗留字段：v3 起不再导出（默认空列表），仅用于兼容旧文件导入 */
    val rounds: List<BackupRound> = emptyList(),
)

@Serializable
data class BackupRound(val orderIndex: Int, val question: String, val answer: String, val source: String)

@Serializable
data class BackupFile(
    val version: Int = 1,
    val exportedAt: Long,
    val categories: List<BackupCategory>,
    val questions: List<BackupQuestion>,
)
