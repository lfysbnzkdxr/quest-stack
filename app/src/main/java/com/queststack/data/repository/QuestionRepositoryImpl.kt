package com.queststack.data.repository

import androidx.room.withTransaction
import com.queststack.data.db.AppDatabase
import com.queststack.data.db.CategoryCount
import com.queststack.data.db.Question
import com.queststack.data.db.QuestionDao
import kotlinx.coroutines.flow.Flow

class QuestionRepositoryImpl(
    private val database: AppDatabase,
    private val questionDao: QuestionDao
) : QuestionRepository {

    override fun observeQuestions(categoryId: Long?, difficulty: Int?): Flow<List<Question>> =
        questionDao.observeFiltered(categoryId, difficulty)

    override fun observeCategoryCounts(): Flow<List<CategoryCount>> =
        questionDao.observeCategoryCounts()

    override suspend fun getQuestion(id: Long): Question? =
        questionDao.getById(id)

    override fun observeQuestion(id: Long): Flow<Question?> =
        questionDao.observeById(id)

    override suspend fun addQuestion(
        title: String,
        answer: String,
        categoryId: Long?,
        difficulty: Int
    ): Long = database.withTransaction {
        val now = System.currentTimeMillis()
        val question = Question(
            title = title,
            answer = answer,
            categoryId = categoryId,
            difficulty = difficulty,
            createdAt = now,
            updatedAt = now
        )
        questionDao.insert(question)
    }

    override suspend fun updateQuestion(question: Question) {
        database.withTransaction {
            questionDao.update(question.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    override suspend fun deleteQuestion(id: Long) {
        database.withTransaction {
            questionDao.deleteById(id)
        }
    }

    override suspend fun randomQuestionIds(categoryId: Long?, difficulty: Int?): List<Long> =
        questionDao.getIds(categoryId, difficulty).shuffled()
}
