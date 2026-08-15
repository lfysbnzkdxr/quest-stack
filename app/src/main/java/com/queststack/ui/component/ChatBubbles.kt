package com.queststack.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 面试官问题气泡：左侧对齐，surfaceContainer 背景，右下小圆角（微信风格）。
 * round 为 null 时不显示"第 N 问"（练题模式无追问链）。 */
@Composable
fun InterviewerBubble(text: String, round: Int?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        Column(modifier = Modifier.widthIn(max = 300.dp)) {
            Text(
                text = if (round == null) "面试官" else "面试官 · 第 $round 问",
                fontSize = 11.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            )
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomEnd = 12.dp,
                            bottomStart = 4.dp,
                        ),
                    )
                    .background(MiuixTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text = text.ifBlank { "（问题为空）" },
                    fontSize = 15.sp,
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                    lineHeight = 21.sp,
                )
            }
        }
    }
}

/** 参考答案气泡：右侧对齐，primary 背景，左下小圆角（微信风格） */
@Composable
fun AnswerBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 300.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = "参考答案",
                fontSize = 11.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                modifier = Modifier.padding(end = 4.dp, bottom = 4.dp),
            )
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomEnd = 4.dp,
                            bottomStart = 12.dp,
                        ),
                    )
                    .background(MiuixTheme.colorScheme.primary)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text = text.ifBlank { "（答案为空）" },
                    fontSize = 15.sp,
                    color = MiuixTheme.colorScheme.onPrimary,
                    lineHeight = 21.sp,
                )
            }
        }
    }
}
