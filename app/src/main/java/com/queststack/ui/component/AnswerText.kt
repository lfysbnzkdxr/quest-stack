package com.queststack.ui.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 行首列表项：数字编号（1. / 1) / 1、）或短横线/圆点 */
private val LIST_PREFIX = Regex("^\\s*(?:\\d+[).、]|[-•])\\s*")

/** 行内加粗标记：**内容**（不支持嵌套） */
private val BOLD = Regex("\\*\\*(.+?)\\*\\*")

/**
 * 轻量富文本渲染器（供长答案等使用）：
 * - `**加粗**` → 粗体 + 主题色强调；
 * - 空行分段 → 段前间距；
 * - 行首 `1.`/`1)`/`1、`/`-`/`•` → 列表项（编号加粗、换行悬挂缩进）。
 *
 * 纯文本自动降级为常规段落，无标记不影响显示。
 *
 * 默认文字色用主题 onBackground（miuix/KernelSU 显式配色风格）：
 * 深色模式下不依赖 LocalContentColor（未设置时默认黑色，黑字黑底不可见）。
 */
@Composable
fun AnswerText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 15.sp,
    lineHeight: TextUnit = 24.sp,
    color: Color = MiuixTheme.colorScheme.onBackground,
    highlightColor: Color = MiuixTheme.colorScheme.primary,
) {
    // 结果仅依赖文本与强调色，缓存避免每次重组重复解析
    val annotated = remember(text, highlightColor) { parseAnswer(text, highlightColor) }
    BasicText(
        text = annotated,
        modifier = modifier,
        style = TextStyle(fontSize = fontSize, lineHeight = lineHeight, color = color),
    )
}

/** 把答案文本解析为带粗体/分段/列表样式的 AnnotatedString */
internal fun parseAnswer(text: String, highlightColor: Color): AnnotatedString = buildAnnotatedString {
    val lines = text.split('\n')
    var hasContent = false // 是否已输出内容：首段前不加换行
    var prevBlank = false // 上一行是否为空行：决定本段是否补空行制造分段间距
    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            prevBlank = true
            continue
        }
        if (hasContent) {
            append('\n')
            // 空行分段：补一个换行形成空白行（Compose 段落间距无 spaceBefore 参数）
            if (prevBlank) append('\n')
        }
        hasContent = true
        prevBlank = false
        appendSegment(trimmed, highlightColor)
    }
}

/** 追加一个段落：行首列表项做缩进，段内处理 **加粗** */
private fun AnnotatedString.Builder.appendSegment(segment: String, highlightColor: Color) {
    val listMatch = LIST_PREFIX.find(segment)
    if (listMatch != null) {
        val rest = segment.substring(listMatch.range.last + 1)
        withStyle(ParagraphStyle(textIndent = TextIndent(restLine = 22.sp))) {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = highlightColor)) {
                append(listMatch.value.trim() + " ")
            }
            appendStyled(rest, highlightColor)
        }
    } else {
        appendStyled(segment, highlightColor)
    }
}

/** 追加一段文本，段内 **加粗** 标记转为粗体主题色 */
private fun AnnotatedString.Builder.appendStyled(segment: String, highlightColor: Color) {
    var last = 0
    for (m in BOLD.findAll(segment)) {
        if (m.range.first > last) append(segment.substring(last, m.range.first))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = highlightColor)) {
            append(m.groupValues[1])
        }
        last = m.range.last + 1
    }
    if (last < segment.length) append(segment.substring(last))
}