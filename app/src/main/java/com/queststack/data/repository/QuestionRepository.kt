package com.queststack.data.repository

import com.queststack.data.db.Question
import com.queststack.data.db.QuestionWithRounds
import com.queststack.data.db.Round
import kotlinx.coroutines.flow.Flow

interface QuestionRepository {
    fun observeQuestions(categoryId: Long?, difficulty: Int?): Flow<List<QuestionWithRounds>>
    suspend fun getQuestion(id: Long): QuestionWithRounds?
    suspend fun addQuestion(
        title: String,
        answer: String,
        categoryId: Long?,
        difficulty: Int,
        rounds: List<Pair<String, String>>
    ): Long
    suspend fun updateQuestion(question: Question, rounds: List<Round>)
    suspend fun deleteQuestion(id: Long)
    suspend fun randomQuestionIds(categoryId: Long?, difficulty: Int?): List<Long>
}
