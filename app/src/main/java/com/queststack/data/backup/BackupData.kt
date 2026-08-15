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
    /** 追问链（纯追问轮次，不含主问题） */
    val rounds: List<BackupRound>,
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
