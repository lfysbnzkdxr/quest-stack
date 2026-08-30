package com.queststack.util

import com.queststack.data.db.Category

/**
 * 解析 AI 生成答案开头的「【分类】xxx」建议行：剥离后返回（分类名, 正文）。
 * 仅匹配首个非空行；不匹配时分类为 null、正文原样返回（兼容未按约定输出的模型）。
 */
object AnswerMeta {

    private val CATEGORY_LINE = Regex("^【分类】\\s*(.+?)\\s*$")

    /** 表示"无归属分类"的建议名（与表单"未分类"选项文案一致） */
    const val UNCATEGORIZED = "未分类"

    /** 生成答案解析结果：正文 + 建议分类 id + 是否有建议行（hasSuggestion=false 时不应改分类） */
    data class ParsedAnswer(val body: String, val categoryId: Long?, val hasSuggestion: Boolean)

    /**
     * 完整解析 AI 生成答案：剥离首行分类建议并映射为已有分类 id。
     * 「未分类」→ null；匹配已有分类名 → 对应 id；幻觉名 → fallbackId（保持用户所选）。
     */
    fun parse(text: String, categories: List<Category>, fallbackId: Long?): ParsedAnswer {
        val (name, body) = extractCategory(text)
        if (name == null) return ParsedAnswer(body = text, categoryId = null, hasSuggestion = false)
        val id = when {
            name == UNCATEGORIZED -> null
            else -> categories.firstOrNull { it.name == name }?.id ?: fallbackId
        }
        return ParsedAnswer(body = body, categoryId = id, hasSuggestion = true)
    }

    private fun extractCategory(text: String): Pair<String?, String> {
        val lines = text.lines()
        val index = lines.indexOfFirst { it.isNotBlank() }
        if (index < 0) return null to text
        val match = CATEGORY_LINE.matchEntire(lines[index].trim()) ?: return null to text
        val name = match.groupValues[1].trim()
        if (name.isEmpty()) return null to text
        val body = lines.filterIndexed { i, _ -> i != index }.joinToString("\n").trim()
        return name to body
    }
}
