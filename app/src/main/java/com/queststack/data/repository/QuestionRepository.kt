package com.queststack.data.repository

import com.queststack.data.db.CategoryCount
import com.queststack.data.db.Question
import kotlinx.coroutines.flow.Flow

interface QuestionRepository {
    fun observeQuestions(categoryId: Long?, difficulty: Int?): Flow<List<Question>>
    fun observeCategoryCounts(): Flow<List<CategoryCount>>
    suspend fun getQuestion(id: Long): Question?
    fun observeQuestion(id: Long): Flow<Question?>
    suspend fun addQuestion(
        title: String,
        answer: String,
        categoryId: Long?,
        difficulty: Int
    ): Long
    suspend fun updateQuestion(question: Question)
    suspend fun deleteQuestion(id: Long)
    suspend fun randomQuestionIds(categoryId: Long?, difficulty: Int?): List<Long>
}
