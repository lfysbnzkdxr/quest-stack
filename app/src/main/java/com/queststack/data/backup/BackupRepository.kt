package com.queststack.data.backup

import com.queststack.data.db.Category
import com.queststack.data.db.CategoryDao
import com.queststack.data.db.Question
import com.queststack.data.db.QuestionDao
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * 备份数据仓库：JSON 导出/导入（本地文件），追加合并、不覆盖现有数据。
 *
 * 导出为 v3 格式（原子问题，无追问链）；兼容导入 v1/v2 旧文件：
 * v1 的 rounds 首轮答案提升为主答案，其余轮次丢弃。
 */
class BackupRepository(
    private val questionDao: QuestionDao,
    private val categoryDao: CategoryDao
) {

    private val json = Json {
        prettyPrint = true
        // 不输出默认值：v3 文件不再包含 rounds/空 answer 字段
        encodeDefaults = false
        ignoreUnknownKeys = true
    }

    /** 导出全部数据为 JSON 字符串 */
    suspend fun exportToJson(): String {
        val categories = categoryDao.observeAll().first()
        val questions = questionDao.observeAll().first()
        val categoryNameById = categories.associate { it.id to it.name }
        val backupFile = BackupFile(
            version = CURRENT_BACKUP_VERSION,
            exportedAt = System.currentTimeMillis(),
            categories = categories.map { BackupCategory(it.name, it.sortOrder) },
            questions = questions.map { q ->
                BackupQuestion(
                    title = q.title,
                    answer = q.answer,
                    categoryName = categoryNameById[q.categoryId],
                    difficulty = q.difficulty,
                    createdAt = q.createdAt,
                    updatedAt = q.updatedAt,
                )
            }
        )
        return json.encodeToString(BackupFile.serializer(), backupFile)
    }

    /** 从 JSON 导入数据，返回新增的题目数量 */
    suspend fun importFromJson(jsonText: String): Int {
        val backupFile = try {
            json.decodeFromString(BackupFile.serializer(), jsonText)
        } catch (e: SerializationException) {
            throw IllegalArgumentException("备份文件格式不正确")
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("备份文件格式不正确")
        }

        if (backupFile.version > CURRENT_BACKUP_VERSION) {
            throw IllegalArgumentException("备份文件版本过高（v${backupFile.version}），当前应用支持 v$CURRENT_BACKUP_VERSION，请升级应用后重试")
        }

        // 分类：按名匹配，不存在则新建
        val existingCategories = categoryDao.observeAll().first()
        val categoryIdByName = existingCategories.associate { it.name to it.id }.toMutableMap()
        val usedSortOrders = existingCategories.map { it.sortOrder }.toHashSet()
        for (backupCategory in backupFile.categories) {
            if (backupCategory.name in categoryIdByName) continue // 复用现有 id
            var sortOrder = backupCategory.sortOrder
            while (sortOrder in usedSortOrders) sortOrder++ // 冲突 +1
            usedSortOrders.add(sortOrder)
            val id = categoryDao.insert(Category(name = backupCategory.name, sortOrder = sortOrder))
            categoryIdByName[backupCategory.name] = id
        }

        // 题目：按 title 去重，同名跳过
        val existingTitles = questionDao.observeAll().first()
            .map { it.title }
            .toHashSet()
        var importedCount = 0
        for (backupQuestion in backupFile.questions) {
            if (backupQuestion.title in existingTitles) continue // 不重复导入
            // v1 兼容：rounds 含"第 0 轮 = 主问题"，其 answer 迁入主答案；v2/v3 直接用 answer
            val mainAnswer = if (backupFile.version >= 2) {
                backupQuestion.answer
            } else {
                backupQuestion.rounds.sortedBy { it.orderIndex }.firstOrNull()?.answer ?: ""
            }
            questionDao.insert(
                Question(
                    title = backupQuestion.title,
                    answer = mainAnswer,
                    categoryId = categoryIdByName[backupQuestion.categoryName], // 未知分类名 → null
                    difficulty = backupQuestion.difficulty,
                    createdAt = backupQuestion.createdAt,
                    updatedAt = backupQuestion.updatedAt
                )
            )
            existingTitles.add(backupQuestion.title)
            importedCount++
        }
        return importedCount
    }

    companion object {
        const val CURRENT_BACKUP_VERSION = 3
    }
}
