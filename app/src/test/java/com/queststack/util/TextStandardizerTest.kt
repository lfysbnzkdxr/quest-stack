package com.queststack.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TextStandardizerTest {

    // ------------------------------------------------------------------
    // normalize
    // ------------------------------------------------------------------

    @Test
    fun `normalize 统一换行符`() {
        assertEquals("a\nb", TextStandardizer.normalize("a\r\nb"))
        assertEquals("a\nb", TextStandardizer.normalize("a\rb"))
    }

    @Test
    fun `normalize 去除每行首尾空白`() {
        assertEquals("a\nb", TextStandardizer.normalize("  a  \n\tb\t"))
    }

    @Test
    fun `normalize 压缩连续空行并去除整体首尾空行`() {
        assertEquals("a\nb", TextStandardizer.normalize("\n\n\na\n\n\n\nb\n\n"))
    }

    // ------------------------------------------------------------------
    // parseQaPairs：显式标记格式
    // ------------------------------------------------------------------

    @Test
    fun `显式标记格式 单行 Q冒号 A冒号`() {
        assertEquals(listOf("q1" to "a1"), TextStandardizer.parseQaPairs("Q:q1\nA:a1"))
    }

    @Test
    fun `显式标记格式 中文问答标记`() {
        assertEquals(
            listOf("问题一" to "答案一"),
            TextStandardizer.parseQaPairs("问：问题一\n答：答案一"),
        )
        assertEquals(
            listOf("问题二" to "答案二"),
            TextStandardizer.parseQaPairs("问题：问题二\n答案：答案二"),
        )
    }

    @Test
    fun `显式标记格式 同行内多段 Q 与 A`() {
        assertEquals(
            listOf("q1" to "a1", "q2" to "a2"),
            TextStandardizer.parseQaPairs("Q:q1 A:a1 Q:q2 A:a2"),
        )
    }

    @Test
    fun `显式标记格式 答案跨多行`() {
        assertEquals(
            listOf("q1" to "a1\n续行"),
            TextStandardizer.parseQaPairs("Q:q1\nA:a1\n续行"),
        )
    }

    @Test
    fun `显式标记格式 问题跨多行`() {
        assertEquals(
            listOf("q1\n续行" to "a1"),
            TextStandardizer.parseQaPairs("Q:q1\n续行\nA:a1"),
        )
    }

    @Test
    fun `正文中的请问不误判为问题标记`() {
        assertEquals(
            listOf("为什么要这样做？请问" to "因为测试"),
            TextStandardizer.parseQaPairs("Q:为什么要这样做？请问\nA:因为测试"),
        )
    }

    @Test
    fun `答案先于问题时以空问题兜底`() {
        assertEquals(
            listOf("" to "先答", "问题1" to "答1"),
            TextStandardizer.parseQaPairs("A:先答\nQ:问题1\nA:答1"),
        )
    }

    // ------------------------------------------------------------------
    // parseQaPairs：编号列表格式
    // ------------------------------------------------------------------

    @Test
    fun `编号列表格式 编号加答案标记`() {
        assertEquals(
            listOf("题1" to "答1"),
            TextStandardizer.parseQaPairs("1. 题1\n答案：答1"),
        )
    }

    @Test
    fun `编号列表格式 无答案标记时答案为空`() {
        assertEquals(
            listOf("题1" to "", "题2" to ""),
            TextStandardizer.parseQaPairs("1. 题1\n2. 题2"),
        )
    }

    @Test
    fun `编号列表格式 问题N冒号与第N题`() {
        assertEquals(
            listOf("题A" to "答A"),
            TextStandardizer.parseQaPairs("问题1: 题A\n答案：答A"),
        )
        assertEquals(
            listOf("题B" to "答B"),
            TextStandardizer.parseQaPairs("第1题 题B\n答案：答B"),
        )
    }

    @Test
    fun `编号列表格式 答案跨行归入答案`() {
        assertEquals(
            listOf("题1" to "答1\n续行"),
            TextStandardizer.parseQaPairs("1. 题1\n答案：答1\n续行"),
        )
    }

    // ------------------------------------------------------------------
    // parseQaPairs：单块文本与其他
    // ------------------------------------------------------------------

    @Test
    fun `单块文本 无任何标记时全文为问题答案为空`() {
        assertEquals(
            listOf("一整段自由文本" to ""),
            TextStandardizer.parseQaPairs("一整段自由文本"),
        )
    }

    @Test
    fun `空输入返回空列表`() {
        assertEquals(emptyList<Pair<String, String>>(), TextStandardizer.parseQaPairs(""))
        assertEquals(emptyList<Pair<String, String>>(), TextStandardizer.parseQaPairs("   \n  "))
    }
}