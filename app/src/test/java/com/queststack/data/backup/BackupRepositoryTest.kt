package com.queststack.data.backup

import com.queststack.data.db.Category
import com.queststack.data.db.CategoryCount
import com.queststack.data.db.CategoryDao
import com.queststack.data.db.Question
import com.queststack.data.db.QuestionDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRepositoryTest {

    // ------------------------------------------------------------------
    // 导出
    // ------------------------------------------------------------------

    @Test
    fun `exportToJson 生成 v3 结构且不含 rounds 字段`() = runTest {
        val questionDao = FakeQuestionDao(
            mutableListOf(
                q(1, title = "题1", answer = "答1", categoryId = 1),
            )
        )
        val categoryDao = FakeCategoryDao(mutableListOf(Category(id = 1, name = "Android", sortOrder = 0)))
        val json = BackupRepository(questionDao, categoryDao).exportToJson()

        assertTrue(json.contains("\"version\": 3"))
        assertTrue(json.contains("\"title\": \"题1\""))
        assertTrue(json.contains("\"name\": \"Android\""))
        // v3 不输出遗留 rounds 字段
        assertTrue(!json.contains("rounds"))
    }

    @Test
    fun `exportToJson 未分类题目导出空分类名`() = runTest {
        val questionDao = FakeQuestionDao(mutableListOf(q(1, title = "题1", answer = "答1", categoryId = null)))
        val categoryDao = FakeCategoryDao(mutableListOf())
        val json = BackupRepository(questionDao, categoryDao).exportToJson()

        assertTrue(json.contains("\"categoryName\": null"))
    }

    // ------------------------------------------------------------------
    // 导入
    // ------------------------------------------------------------------

    @Test
    fun `importFromJson 导入新分类与题目`() = runTest {
        val questionDao = FakeQuestionDao()
        val categoryDao = FakeCategoryDao()
        val count = BackupRepository(questionDao, categoryDao).importFromJson(v3File)

        assertEquals(3, count)
        assertEquals(2, categoryDao.data.size)
        assertEquals(3, questionDao.data.size)
        // 题1 关联新分类 Android
        assertEquals(categoryDao.data.first { it.name == "Android" }.id, questionDao.data.first { it.title == "题1" }.categoryId)
        // 未知分类名与 null 分类名均落为未分类
        assertTrue(questionDao.data.first { it.title == "题2" }.categoryId == null)
        assertTrue(questionDao.data.first { it.title == "题3" }.categoryId == null)
    }

    @Test
    fun `importFromJson 同名题目去重不重复导入`() = runTest {
        val questionDao = FakeQuestionDao(mutableListOf(q(1, title = "题1", answer = "旧答", categoryId = null)))
        val categoryDao = FakeCategoryDao()
        val count = BackupRepository(questionDao, categoryDao).importFromJson(v3File)

        assertEquals(2, count)
        assertEquals(1, questionDao.data.count { it.title == "题1" })
        // 已有题目不被覆盖
        assertEquals("旧答", questionDao.data.first { it.title == "题1" }.answer)
    }

    @Test
    fun `importFromJson 分类名匹配时复用现有 id 不新建`() = runTest {
        val questionDao = FakeQuestionDao()
        val categoryDao = FakeCategoryDao(mutableListOf(Category(id = 9, name = "Android", sortOrder = 0)))
        val count = BackupRepository(questionDao, categoryDao).importFromJson(v3File)

        assertEquals(3, count)
        // Android 复用现有 id；文件中的 Kotlin 是新分类故新建，总数 2
        assertEquals(2, categoryDao.data.size)
        assertEquals(9L, categoryDao.data.first { it.name == "Android" }.id)
        assertEquals(9L, questionDao.data.first { it.title == "题1" }.categoryId)
    }

    @Test
    fun `importFromJson sortOrder 冲突时递增`() = runTest {
        val questionDao = FakeQuestionDao()
        val categoryDao = FakeCategoryDao(mutableListOf(Category(id = 1, name = "已有", sortOrder = 0)))
        val file = """
            {
              "version": 3,
              "exportedAt": 1,
              "categories": [{"name": "新类", "sortOrder": 0}],
              "questions": []
            }
        """.trimIndent()
        BackupRepository(questionDao, categoryDao).importFromJson(file)

        assertEquals(1, categoryDao.data.first { it.name == "新类" }.sortOrder)
    }

    @Test
    fun `importFromJson v1 文件迁移 rounds 首轮答案为主答案`() = runTest {
        val questionDao = FakeQuestionDao()
        val categoryDao = FakeCategoryDao()
        val file = """
            {
              "version": 1,
              "exportedAt": 1,
              "categories": [{"name": "Android", "sortOrder": 0}],
              "questions": [
                {
                  "title": "题1",
                  "categoryName": "Android",
                  "difficulty": 1,
                  "createdAt": 1,
                  "updatedAt": 1,
                  "rounds": [
                    {"orderIndex": 0, "question": "题1", "answer": "主答案", "source": "a"},
                    {"orderIndex": 1, "question": "题1", "answer": "追问", "source": "b"}
                  ]
                }
              ]
            }
        """.trimIndent()
        val count = BackupRepository(questionDao, categoryDao).importFromJson(file)

        assertEquals(1, count)
        assertEquals("主答案", questionDao.data.single().answer)
    }

    @Test
    fun `importFromJson 兼容未知字段`() = runTest {
        val questionDao = FakeQuestionDao()
        val categoryDao = FakeCategoryDao()
        val file = """
            {
              "version": 3,
              "exportedAt": 1,
              "unknownField": "future",
              "categories": [{"name": "Android", "sortOrder": 0, "extra": true}],
              "questions": [
                {"title": "题1", "answer": "答1", "categoryName": "Android", "difficulty": 1, "createdAt": 1, "updatedAt": 1, "rounds": []}
              ]
            }
        """.trimIndent()
        val count = BackupRepository(questionDao, categoryDao).importFromJson(file)

        assertEquals(1, count)
        assertEquals("答1", questionDao.data.single().answer)
    }

    @Test
    fun `importFromJson 版本过高报请升级应用`() = runTest {
        val file = """
            {
              "version": 99,
              "exportedAt": 1,
              "categories": [],
              "questions": []
            }
        """.trimIndent()
        val e = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { BackupRepository(FakeQuestionDao(), FakeCategoryDao()).importFromJson(file) }
        }
        assertTrue(e.message.orEmpty().contains("请升级应用"))
    }

    @Test
    fun `importFromJson 损坏 JSON 报格式不正确`() = runTest {
        val e = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { BackupRepository(FakeQuestionDao(), FakeCategoryDao()).importFromJson("这不是 JSON{{{") }
        }
        assertTrue(e.message.orEmpty().contains("格式不正确"))
    }

    // ------------------------------------------------------------------
    // 测试数据与 fake
    // ------------------------------------------------------------------

    private val v3File = """
        {
          "version": 3,
          "exportedAt": 1700000000000,
          "categories": [
            {"name": "Android", "sortOrder": 0},
            {"name": "Kotlin", "sortOrder": 1}
          ],
          "questions": [
            {"title": "题1", "answer": "答1", "categoryName": "Android", "difficulty": 1, "createdAt": 1, "updatedAt": 2},
            {"title": "题2", "answer": "答2", "categoryName": "未知分类", "difficulty": 2, "createdAt": 3, "updatedAt": 4},
            {"title": "题3", "answer": "答3", "categoryName": null, "difficulty": 1, "createdAt": 5, "updatedAt": 6}
          ]
        }
    """.trimIndent()

    private fun q(id: Long, title: String, answer: String, categoryId: Long?) = Question(
        id = id,
        title = title,
        answer = answer,
        categoryId = categoryId,
        difficulty = 1,
        createdAt = 0,
        updatedAt = 0,
    )

    private class FakeQuestionDao(initial: MutableList<Question> = mutableListOf()) : QuestionDao {
        val data = initial.toMutableList()
        private var nextId = 100L

        override suspend fun insert(question: Question): Long {
            val saved = question.copy(id = nextId)
            data.add(saved)
            nextId++
            return saved.id
        }

        override suspend fun update(question: Question) {
            val i = data.indexOfFirst { it.id == question.id }
            if (i >= 0) data[i] = question
        }

        override fun observeAll(): Flow<List<Question>> = flowOf(data.toList())
        override fun observeFiltered(categoryId: Long?, difficulty: Int?): Flow<List<Question>> = flowOf(data.toList())
        override suspend fun getById(id: Long): Question? = data.firstOrNull { it.id == id }
        override suspend fun deleteById(id: Long) { data.removeAll { it.id == id } }
        override suspend fun getIds(categoryId: Long?, difficulty: Int?): List<Long> = data.map { it.id }
        override fun observeCategoryCounts(): Flow<List<CategoryCount>> = flowOf(emptyList())
    }

    private class FakeCategoryDao(initial: MutableList<Category> = mutableListOf()) : CategoryDao {
        val data = initial.toMutableList()
        private var nextId = 10L

        override suspend fun insert(category: Category): Long {
            val saved = category.copy(id = nextId)
            data.add(saved)
            nextId++
            return saved.id
        }

        override suspend fun update(category: Category) {
            val i = data.indexOfFirst { it.id == category.id }
            if (i >= 0) data[i] = category
        }

        override suspend fun delete(category: Category) { data.removeAll { it.id == category.id } }
        override fun observeAll(): Flow<List<Category>> = flowOf(data.toList())
        override suspend fun countQuestions(categoryId: Long): Int = 0
    }
}