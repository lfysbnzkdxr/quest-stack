package com.queststack.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** 每分类题目数量（categoryId 为 null 表示未分类） */
data class CategoryCount(val categoryId: Long?, val count: Int)

@Dao
interface QuestionDao {
    @Insert
    suspend fun insert(question: Question): Long

    @Update
    suspend fun update(question: Question)

    @Query("SELECT * FROM questions ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<Question>>

    /**
     * 按筛选查询：categoryId 为 null = 全部；为 -1（[Category.UNCATEGORIZED_ID]）= 仅未分类；
     * 其他值 = 指定分类。difficulty 为 null = 不限难度。
     */
    @Query("SELECT * FROM questions WHERE (:categoryId IS NULL OR categoryId = :categoryId OR (:categoryId = -1 AND categoryId IS NULL)) AND (:difficulty IS NULL OR difficulty = :difficulty) ORDER BY updatedAt DESC")
    fun observeFiltered(categoryId: Long?, difficulty: Int?): Flow<List<Question>>

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getById(id: Long): Question?

    /** 按 id 观察单题（详情页用，编辑后库更新自动触发刷新） */
    @Query("SELECT * FROM questions WHERE id = :id")
    fun observeById(id: Long): Flow<Question?>

    @Query("DELETE FROM questions WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** 获取筛选范围内题目的 id（练题随机用），语义同 [observeFiltered] */
    @Query("SELECT id FROM questions WHERE (:categoryId IS NULL OR categoryId = :categoryId OR (:categoryId = -1 AND categoryId IS NULL)) AND (:difficulty IS NULL OR difficulty = :difficulty)")
    suspend fun getIds(categoryId: Long?, difficulty: Int?): List<Long>

    @Query("SELECT categoryId, COUNT(*) AS count FROM questions GROUP BY categoryId")
    fun observeCategoryCounts(): Flow<List<CategoryCount>>
}
