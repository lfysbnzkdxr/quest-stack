package com.queststack.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerTextTest {

    @Test
    fun `空行分段保留为空白行`() {
        val a = parseAnswer("第一段\n\n第二段", Color.Blue)
        assertEquals("第一段\n\n第二段", a.text)
    }

    @Test
    fun `相邻非空行只以单个换行分隔`() {
        val a = parseAnswer("行一\n行二", Color.Blue)
        assertEquals("行一\n行二", a.text)
    }

    @Test
    fun `首尾空行被忽略且不产生前导空行`() {
        val a = parseAnswer("\n\n甲\n\n", Color.Blue)
        assertEquals("甲", a.text)
    }

    @Test
    fun `列表行加悬挂缩进且编号加粗`() {
        val a = parseAnswer("1) 甲\n2) 乙", Color.Blue)
        assertEquals("1) 甲\n2) 乙", a.text)
        assertTrue(a.paragraphStyles.all { it.item.textIndent == TextIndent(restLine = 22.sp) })
        val bold = a.spanStyles.filter { it.item.fontWeight == FontWeight.SemiBold }
        assertEquals(listOf("1)", "2)"), bold.map { a.text.substring(it.start, it.end).trim() })
    }

    @Test
    fun `加粗标记转为粗体强调且不留在文本中`() {
        val a = parseAnswer("**关键词** 正文", Color.Blue)
        assertEquals("关键词 正文", a.text)
        val bold = a.spanStyles.single { it.item.fontWeight == FontWeight.Bold }
        assertEquals("关键词", a.text.substring(bold.start, bold.end))
        assertEquals(Color.Blue, bold.item.color)
    }

    @Test
    fun `纯文本无标记时原样输出无样式`() {
        val a = parseAnswer("这是一段没有标记的普通文本", Color.Blue)
        assertEquals("这是一段没有标记的普通文本", a.text)
        assertTrue(a.spanStyles.isEmpty())
    }

    @Test
    fun `未闭合加粗标记保留原文`() {
        val a = parseAnswer("有个**未闭合标记", Color.Blue)
        assertEquals("有个**未闭合标记", a.text)
        assertTrue(a.spanStyles.none { it.item.fontWeight == FontWeight.Bold })
    }

    @Test
    fun `小节标题行整行加粗着色`() {
        val a = parseAnswer("【精炼回答】\n正文一句", Color.Blue)
        assertEquals("【精炼回答】\n正文一句", a.text)
        val header = a.spanStyles.single { it.item.fontWeight == FontWeight.Bold }
        assertEquals("【精炼回答】", a.text.substring(header.start, header.end))
        assertEquals(Color.Blue, header.item.color)
    }

    @Test
    fun `列表项关键词冒号前加粗本色`() {
        val a = parseAnswer("1) 约束注入：让AI先读懂规则", Color.Blue)
        assertEquals("1) 约束注入：让AI先读懂规则", a.text)
        val bold = a.spanStyles.single { it.item.fontWeight == FontWeight.Bold }
        assertEquals("约束注入：", a.text.substring(bold.start, bold.end))
    }

    @Test
    fun `列表项冒号前过长不加粗`() {
        val a = parseAnswer("1) 这是一个非常非常非常长的句子，中间有个：冒号", Color.Blue)
        assertTrue(a.spanStyles.none { it.item.fontWeight == FontWeight.Bold })
    }

    @Test
    fun `空文本输出为空`() {
        assertEquals("", parseAnswer("", Color.Blue).text)
        assertEquals("", parseAnswer("  \n  ", Color.Blue).text)
    }
}