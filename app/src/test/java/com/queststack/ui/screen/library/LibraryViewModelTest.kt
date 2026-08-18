package com.queststack.ui.screen.library

import com.queststack.data.db.Category
import com.queststack.data.db.CategoryCount
import com.queststack.data.db.Question
import com.queststack.data.repository.CategoryRepository
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

class LibraryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val categories = listOf(Category(id = 1, name = "Android", sortOrder = 0))

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `初始加载填充分类与题目并结束 loading`() = runTest(dispatcher.scheduler) {
        val vm = LibraryViewModel(
            FakeQuestionRepository(
                questions = mapOf(null to null to listOf(q(1, null, 1))),
            ),
            FakeCategoryRepository(categories),
        )
        advanceUntilIdle()
        assertEquals(categories, vm.uiState.value.categories)
        assertEquals(listOf(q(1, null, 1)), vm.uiState.value.questions)
        assertEquals(false, vm.uiState.value.loading)
    }

    @Test
    fun `selectCategory 同时更新驱动源与显示状态并触发重查`() = runTest(dispatcher.scheduler) {
        val vm = LibraryViewModel(
            FakeQuestionRepository(
                questions = mapOf(
                    null to null to listOf(q(1, null, 1)),
                    2L to null to listOf(q(2, 2, 1)),
                ),
            ),
            FakeCategoryRepository(categories),
        )
        advanceUntilIdle()
        assertEquals(listOf(q(1, null, 1)), vm.uiState.value.questions)

        vm.selectCategory(2L)
        advanceUntilIdle()
        // 双份状态同步：显示状态跟随
        assertEquals(2L, vm.uiState.value.selectedCategoryId)
        // 驱动源变化触发 flatMapLatest 重新查询对应筛选
        assertEquals(listOf(q(2, 2, 1)), vm.uiState.value.questions)
    }

    @Test
    fun `selectCategory 未分类哨兵筛选未分类题目并同步显示状态`() = runTest(dispatcher.scheduler) {
        val vm = LibraryViewModel(
            FakeQuestionRepository(
                questions = mapOf(
                    null to null to listOf(q(1, null, 1)),
                    Category.UNCATEGORIZED_ID to null to listOf(q(2, null, 1)),
                ),
            ),
            FakeCategoryRepository(categories),
        )
        advanceUntilIdle()
        assertEquals(listOf(q(1, null, 1)), vm.uiState.value.questions)

        vm.selectCategory(Category.UNCATEGORIZED_ID)
        advanceUntilIdle()
        // 双份状态同步：显示状态跟随
        assertEquals(Category.UNCATEGORIZED_ID, vm.uiState.value.selectedCategoryId)
        // 驱动源变化触发重查未分类范围
        assertEquals(listOf(q(2, null, 1)), vm.uiState.value.questions)
    }

    @Test
    fun `selectDifficulty 同时更新驱动源与显示状态并触发重查`() = runTest(dispatcher.scheduler) {
        val vm = LibraryViewModel(
            FakeQuestionRepository(
                questions = mapOf(
                    null to null to listOf(q(1, null, 1)),
                    null to 2 to listOf(q(2, null, 2)),
                ),
            ),
            FakeCategoryRepository(categories),
        )
        advanceUntilIdle()

        vm.selectDifficulty(2)
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.difficulty)
        assertEquals(listOf(q(2, null, 2)), vm.uiState.value.questions)
    }

    @Test
    fun `筛选组合后选中分类显示与题目均切换`() = runTest(dispatcher.scheduler) {
        val vm = LibraryViewModel(
            FakeQuestionRepository(
                questions = mapOf(
                    null to null to listOf(q(1, null, 1)),
                    2L to 3 to listOf(q(3, 2, 3)),
                ),
            ),
            FakeCategoryRepository(categories),
        )
        advanceUntilIdle()

        vm.selectCategory(2L)
        vm.selectDifficulty(3)
        advanceUntilIdle()
        assertEquals(2L, vm.uiState.value.selectedCategoryId)
        assertEquals(3, vm.uiState.value.difficulty)
        assertEquals(listOf(q(3, 2, 3)), vm.uiState.value.questions)
    }

    @Test
    fun `deleteQuestion 调用仓库删除`() = runTest(dispatcher.scheduler) {
        val fakeQ = FakeQuestionRepository(
            questions = mapOf(null to null to listOf(q(1, null, 1))),
        )
        val vm = LibraryViewModel(fakeQ, FakeCategoryRepository(categories))
        advanceUntilIdle()

        vm.deleteQuestion(q(1, null, 1))
        advanceUntilIdle()
        assertEquals(listOf(1L), fakeQ.deletedIds)
    }

    private fun q(id: Long, categoryId: Long?, difficulty: Int) = Question(
        id = id,
        title = "题$id",
        categoryId = categoryId,
        difficulty = difficulty,
        createdAt = 0,
        updatedAt = 0,
    )

    private class FakeQuestionRepository(
        private val questions: Map<Pair<Long?, Int?>, List<Question>>,
    ) : QuestionRepository {
        val deletedIds = mutableListOf<Long>()

        override fun observeQuestions(categoryId: Long?, difficulty: Int?): Flow<List<Question>> =
            flowOf(questions[categoryId to difficulty] ?: emptyList())

        override fun observeCategoryCounts(): Flow<List<CategoryCount>> = flowOf(emptyList())
        override suspend fun getQuestion(id: Long): Question? = null
        override suspend fun addQuestion(title: String, answer: String, categoryId: Long?, difficulty: Int): Long = 0
        override suspend fun updateQuestion(question: Question) {}
        override suspend fun deleteQuestion(id: Long) { deletedIds.add(id) }
        override suspend fun randomQuestionIds(categoryId: Long?, difficulty: Int?): List<Long> = emptyList()
    }

    private class FakeCategoryRepository(
        private val categories: List<Category>,
    ) : CategoryRepository {
        override fun observeCategories(): Flow<List<Category>> = flowOf(categories)
        override suspend fun addCategory(name: String) {}
        override suspend fun renameCategory(category: Category, newName: String) {}
        override suspend fun deleteCategory(category: Category) {}
    }
}