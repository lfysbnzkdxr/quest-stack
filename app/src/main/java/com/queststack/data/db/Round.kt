package com.queststack.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rounds",
    foreignKeys = [
        ForeignKey(
            entity = Question::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("questionId")]
)
data class Round(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: Long,
    /** 追问轮次序号，0 起（纯追问链，题目本身答案在 question.answer） */
    val orderIndex: Int,
    val question: String,
    val answer: String,
    val source: String
)
