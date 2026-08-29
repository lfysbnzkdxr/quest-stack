package com.queststack.ui.screen.practice

import com.queststack.data.db.Category
import com.queststack.data.db.CategoryCount
import com.queststack.data.db.Question
import com.queststack.data.repository.CategoryRepository
import com.queststack.data.repository.QuestionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeSessionViewModelTest {

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
    fun `初始加载随机抽取一道题`() = runTest(dispatcher.scheduler) {
        val vm = PracticeSessionViewModel(
            session = PracticeSession(),
            questionRepository = FakeQuestionRepository(
                idsByFilter = mapOf(null to null to listOf(1L, 2L)),
                questions = mapOf(1L to q(1), 2L to q(2)),
            ),
            categoryRepository = FakeCategoryRepository(),
        )
        advanceUntilIdle()
        assertEquals(q(1), vm.uiState.value.current)
        assertEquals(false, vm.uiState.value.loading)
        assertEquals(false, vm.uiState.value.empty)
    }

    @Test
    fun `startQuestionId 首次优先展示指定题`() = runTest(dispatcher.scheduler) {
        val vm = PracticeSessionViewModel(
            session = PracticeSession(startQuestionId = 2L),
            questionRepository = FakeQuestionRepository(
                idsByFilter = mapOf(null to null to listOf(1L, 2L, 3L)),
                questions = mapOf(1L to q(1), 2L to q(2), 3L to q(3)),
            ),
            categoryRepository = FakeCategoryRepository(),
        )
        advanceUntilIdle()
        assertEquals(q(2), vm.uiState.value.current)
    }

    @Test
    fun `next 换题且排除当前题`() = runTest(dispatcher.scheduler) {
        val vm = PracticeSessionViewModel(
            session = PracticeSession(),
            questionRepository = FakeQuestionRepository(
                idsByFilter = mapOf(null to null to listOf(1L, 2L, 3L)),
                questions = mapOf(1L to q(1), 2L to q(2), 3L to q(3)),
            ),
            categoryRepository = FakeCategoryRepository(),
        )
        advanceUntilIdle()
        assertEquals(q(1), vm.uiState.value.current)

        vm.next()
        advanceUntilIdle()
        val second = checkNotNull(vm.uiState.value.current)
        assertTrue(second.id != 1L)
        assertTrue(second.id in listOf(2L, 3L))

        vm.next()
        advanceUntilIdle()
        val third = checkNotNull(vm.uiState.value.current)
        assertTrue(third.id != second.id)
        assertTrue(third.id in listOf(1L, 2L, 3L))
    }

    @Test
    fun `空库时 empty 为 true`() = runTest(dispatcher.scheduler) {
        val vm = PracticeSessionViewModel(
            session = PracticeSession(),
            questionRepository = FakeQuestionRepository(
                idsByFilter = mapOf(null to null to emptyList()),
                questions = emptyMap(),
            ),
            categoryRepository = FakeCategoryRepository(),
        )
        advanceUntilIdle()
        assertNull(vm.uiState.value.current)
        assertTrue(vm.uiState.value.empty)
    }

    @Test
    fun `展开答案 next 后收起并换题`() = runTest(dispatcher.scheduler) {
        val vm = PracticeSessionViewModel(
            session = PracticeSession(),
            questionRepository = FakeQuestionRepository(
                idsByFilter = mapOf(null to null to listOf(1L, 2L)),
                questions = mapOf(1L to q(1), 2L to q(2)),
            ),
            categoryRepository = FakeCategoryRepository(),
        )
        advanceUntilIdle()

        vm.toggleReveal()
        assertEquals(true, vm.uiState.value.revealed)

        vm.next()
        advanceUntilIdle()
        assertEquals(false, vm.uiState.value.revealed)
        assertEquals(q(2), vm.uiState.value.current)
    }

    @Test
    fun `toggleReveal 可再次收起答案`() = runTest(dispatcher.scheduler) {
        val vm = PracticeSessionViewModel(
            session = PracticeSession(),
            questionRepository = FakeQuestionRepository(
                idsByFilter = mapOf(null to null to listOf(1L)),
                questions = mapOf(1L to q(1)),
            ),
            categoryRepository = FakeCategoryRepository(),
        )
        advanceUntilIdle()

        vm.toggleReveal()
        assertEquals(true, vm.uiState.value.revealed)
        vm.toggleReveal()
        assertEquals(false, vm.uiState.value.revealed)
    }

    @Test
    fun `筛选变化触发重抽并同步显示状态`() = runTest(dispatcher.scheduler) {
        val vm = PracticeSessionViewModel(
            session = PracticeSession(),
            questionRepository = FakeQuestionRepository(
                idsByFilter = mapOf(
                    null to null to listOf(1L),
                    5L to null to listOf(2L),
                ),
                questions = mapOf(1L to q(1), 2L to q(2)),
            ),
            categoryRepository = FakeCategoryRepository(),
        )
        advanceUntilIdle()
        assertEquals(q(1), vm.uiState.value.current)

        vm.selectCategory(5L)
        advanceUntilIdle()
        assertEquals(5L, vm.uiState.value.selectedCategoryId)
        assertEquals(q(2), vm.uiState.value.current)
    }

    @Test
    fun `筛选未分类哨兵触发从未分类范围重抽并同步显示状态`() = runTest(dispatcher.scheduler) {
        val vm = PracticeSessionViewModel(
            session = PracticeSession(),
            questionRepository = FakeQuestionRepository(
                idsByFilter = mapOf(
                    null to null to listOf(1L),
                    Category.UNCATEGORIZED_ID to null to listOf(2L),
                ),
                questions = mapOf(1L to q(1), 2L to q(2)),
            ),
            categoryRepository = FakeCategoryRepository(),
        )
        advanceUntilIdle()
        assertEquals(q(1), vm.uiState.value.current)

        vm.selectCategory(Category.UNCATEGORIZED_ID)
        advanceUntilIdle()
        assertEquals(Category.UNCATEGORIZED_ID, vm.uiState.value.selectedCategoryId)
        assertEquals(q(2), vm.uiState.value.current)
    }

    @Test
    fun `start 重置会话：复用 VM 后按新 session 抽题并收起答案`() = runTest(dispatcher.scheduler) {
        val vm = PracticeSessionViewModel(
            session = PracticeSession(),
            questionRepository = FakeQuestionRepository(
                idsByFilter = mapOf(
                    null to null to listOf(1L, 2L),
                    5L to null to listOf(2L, 3L),
                ),
                questions = mapOf(1L to q(1), 2L to q(2), 3L to q(3)),
            ),
            categoryRepository = FakeCategoryRepository(),
        )
        advanceUntilIdle()
        assertEquals(q(1), vm.uiState.value.current)

        vm.toggleReveal()
        assertEquals(true, vm.uiState.value.revealed)

        // 模拟同 key 复用：重新开始一个带 startQuestionId 的新会话
        vm.start(PracticeSession(categoryId = 5L, startQuestionId = 3L))
        advanceUntilIdle()
        assertEquals(5L, vm.uiState.value.selectedCategoryId)
        assertEquals(q(3), vm.uiState.value.current)
        assertEquals(false, vm.uiState.value.revealed)
    }

    private fun q(id: Long) = Question(
        id = id,
        title = "题$id",
        answer = "答$id",
        categoryId = null,
        difficulty = 1,
        createdAt = 0,
        updatedAt = 0,
    )

    private class FakeQuestionRepository(
        private val idsByFilter: Map<Pair<Long?, Int?>, List<Long>>,
        private val questions: Map<Long, Question>,
    ) : QuestionRepository {
        override fun observeQuestions(categoryId: Long?, difficulty: Int?): Flow<List<Question>> = flowOf(emptyList())
        override fun observeCategoryCounts(): Flow<List<CategoryCount>> = flowOf(emptyList())
        override suspend fun getQuestion(id: Long): Question? = questions[id]
        override fun observeQuestion(id: Long): Flow<Question?> = flowOf(questions[id])
        override suspend fun addQuestion(title: String, answer: String, categoryId: Long?, difficulty: Int): Long = 0
        override suspend fun updateQuestion(question: Question) {}
        override suspend fun deleteQuestion(id: Long) {}
        override suspend fun randomQuestionIds(categoryId: Long?, difficulty: Int?): List<Long> =
            idsByFilter[categoryId to difficulty] ?: emptyList()
    }

    private class FakeCategoryRepository : CategoryRepository {
        override fun observeCategories(): Flow<List<Category>> = flowOf(emptyList())
        override suspend fun addCategory(name: String) {}
        override suspend fun renameCategory(category: Category, newName: String) {}
        override suspend fun deleteCategory(category: Category) {}
    }
}