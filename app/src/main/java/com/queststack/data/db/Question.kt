package com.queststack.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId"), Index("difficulty")]
)
data class Question(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** 主问题参考答案（原子化后题目自包含答案） */
    val answer: String = "",
    val categoryId: Long?,
    val difficulty: Int,
    val createdAt: Long,
    val updatedAt: Long
)
