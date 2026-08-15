package com.queststack.data.backup

import com.queststack.data.db.Category
import com.queststack.data.db.CategoryDao
import com.queststack.data.db.Question
import com.queststack.data.db.QuestionDao
import com.queststack.data.db.QuestionWithRounds
import com.queststack.data.db.Round
import com.queststack.data.db.RoundDao
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * 备份数据仓库：JSON 导出/导入（本地文件），追加合并、不覆盖现有数据。
 */
class BackupRepository(
    private val questionDao: QuestionDao,
    private val categoryDao: CategoryDao,
    private val roundDao: RoundDao
) {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /** 导出全部数据为 JSON 字符串 */
    suspend fun exportToJson(): String {
        val categories = categoryDao.observeAll().first()
        val questions = questionDao.observeAllWithRounds().first()
        val categoryNameById = categories.associate { it.id to it.name }
        val backupFile = BackupFile(
            version = CURRENT_BACKUP_VERSION,
            exportedAt = System.currentTimeMillis(),
            categories = categories.map { BackupCategory(it.name, it.sortOrder) },
            questions = questions.map { qwr ->
                val q = qwr.question
                BackupQuestion(
                    title = q.title,
                    answer = q.answer,
                    categoryName = categoryNameById[q.categoryId],
                    difficulty = q.difficulty,
                    createdAt = q.createdAt,
                    updatedAt = q.updatedAt,
                    rounds = qwr.rounds.sortedBy { it.orderIndex }.map {
                        BackupRound(it.orderIndex, it.question, it.answer, it.source)
                    }
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
        val existingTitles = questionDao.observeAllWithRounds().first()
            .map { it.question.title }
            .toHashSet()
        var importedCount = 0
        for (backupQuestion in backupFile.questions) {
            if (backupQuestion.title in existingTitles) continue // 不重复导入
            // v1 兼容：rounds 含"第 0 轮 = 主问题"，其 answer 迁入 question.answer，其余轮次作为追问链重排
            val (mainAnswer, followUpRounds) = if (backupFile.version >= 2) {
                backupQuestion.answer to backupQuestion.rounds
            } else {
                val sorted = backupQuestion.rounds.sortedBy { it.orderIndex }
                (sorted.firstOrNull()?.answer ?: "") to sorted.drop(1)
            }
            val questionId = questionDao.insert(
                Question(
                    title = backupQuestion.title,
                    answer = mainAnswer,
                    categoryId = categoryIdByName[backupQuestion.categoryName], // 未知分类名 → null
                    difficulty = backupQuestion.difficulty,
                    createdAt = backupQuestion.createdAt,
                    updatedAt = backupQuestion.updatedAt
                )
            )
            if (followUpRounds.isNotEmpty()) {
                roundDao.insertAll(
                    followUpRounds.mapIndexed { index, round ->
                        Round(
                            questionId = questionId,
                            orderIndex = index,
                            question = round.question,
                            answer = round.answer,
                            source = round.source
                        )
                    }
                )
            }
            existingTitles.add(backupQuestion.title)
            importedCount++
        }
        return importedCount
    }

    companion object {
        const val CURRENT_BACKUP_VERSION = 2
    }
}
