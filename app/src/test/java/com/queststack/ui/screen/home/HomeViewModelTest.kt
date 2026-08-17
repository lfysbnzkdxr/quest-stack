package com.queststack.ui.screen.home

import com.queststack.data.db.CategoryCount
import com.queststack.data.db.Question
import com.queststack.data.repository.QuestionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `counts 含未分类时 totalCount 正确汇总`() = runTest(dispatcher.scheduler) {
        val repo = FakeQuestionRepository(
            counts = listOf(
                CategoryCount(categoryId = null, count = 3),
                CategoryCount(categoryId = 1L, count = 5),
                CategoryCount(categoryId = 2L, count = 2),
            )
        )
        val vm = HomeViewModel(repo)
        advanceUntilIdle()
        assertEquals(10, vm.uiState.value.totalCount)
    }

    @Test
    fun `空库时 totalCount 为零`() = runTest(dispatcher.scheduler) {
        val vm = HomeViewModel(FakeQuestionRepository(counts = emptyList()))
        advanceUntilIdle()
        assertEquals(0, vm.uiState.value.totalCount)
    }

    private class FakeQuestionRepository(
        private val counts: List<CategoryCount>,
    ) : QuestionRepository {
        override fun observeCategoryCounts(): Flow<List<CategoryCount>> = flowOf(counts)
        override fun observeQuestions(categoryId: Long?, difficulty: Int?): Flow<List<Question>> = flowOf(emptyList())
        override suspend fun getQuestion(id: Long): Question? = null
        override suspend fun addQuestion(title: String, answer: String, categoryId: Long?, difficulty: Int): Long = 0
        override suspend fun updateQuestion(question: Question) {}
        override suspend fun deleteQuestion(id: Long) {}
        override suspend fun randomQuestionIds(categoryId: Long?, difficulty: Int?): List<Long> = emptyList()
    }
}