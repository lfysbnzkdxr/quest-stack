package com.queststack.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0
) {
    companion object {
        /** 未分类筛选哨兵 id（真实分类 id 均为正数，-1 表示"仅未分类题目"） */
        const val UNCATEGORIZED_ID = -1L
    }
}
