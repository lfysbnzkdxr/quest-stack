package com.queststack.util

import com.queststack.data.db.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerMetaTest {

    private val categories = listOf(Category(1L, "RAG"), Category(2L, "后端"))

    @Test
    fun `提取首行分类并映射为 id 且剥离正文`() {
        val parsed = AnswerMeta.parse("【分类】RAG\n【精炼回答】\n正文", categories, fallbackId = 9L)
        assertTrue(parsed.hasSuggestion)
        assertEquals(1L, parsed.categoryId)
        assertEquals("【精炼回答】\n正文", parsed.body)
    }

    @Test
    fun `未分类建议映射为 null`() {
        val parsed = AnswerMeta.parse("【分类】未分类\n\n答案内容", categories, fallbackId = 9L)
        assertTrue(parsed.hasSuggestion)
        assertNull(parsed.categoryId)
        assertEquals("答案内容", parsed.body)
    }

    @Test
    fun `幻觉分类名回退 fallbackId`() {
        val parsed = AnswerMeta.parse("【分类】量子计算\n正文", categories, fallbackId = 9L)
        assertTrue(parsed.hasSuggestion)
        assertEquals(9L, parsed.categoryId)
        assertEquals("正文", parsed.body)
    }

    @Test
    fun `无分类行时不回填不改正文`() {
        val text = "【精炼回答】\n正文"
        val parsed = AnswerMeta.parse(text, categories, fallbackId = 9L)
        assertFalse(parsed.hasSuggestion)
        assertNull(parsed.categoryId)
        assertEquals(text, parsed.body)
    }

    @Test
    fun `分类行前允许空行`() {
        val parsed = AnswerMeta.parse("\n\n【分类】后端\n\n答案", categories, fallbackId = null)
        assertTrue(parsed.hasSuggestion)
        assertEquals(2L, parsed.categoryId)
        assertEquals("答案", parsed.body)
    }

    @Test
    fun `分类行不在首行时不剥离`() {
        val text = "【精炼回答】\n【分类】RAG"
        val parsed = AnswerMeta.parse(text, categories, fallbackId = 9L)
        assertFalse(parsed.hasSuggestion)
        assertEquals(text, parsed.body)
    }

    @Test
    fun `分类名为空时不剥离`() {
        val parsed = AnswerMeta.parse("【分类】\n正文", categories, fallbackId = 9L)
        assertFalse(parsed.hasSuggestion)
        assertEquals("【分类】\n正文", parsed.body)
    }
}
