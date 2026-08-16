package com.queststack.ui.screen.practiceLog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.queststack.data.db.PracticeLogEntity
import com.queststack.ui.component.PageScaffold
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

/** 练题记录页：顶部近一年练题热力图（可点选日期）+ 下方该日练题列表 */
@Composable
fun PracticeLogScreen(
    onBack: () -> Unit,
    viewModel: PracticeLogViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PageScaffold(title = "练题记录") { scrollBehavior ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                PracticeHeatmap(
                    dailyCounts = uiState.dailyCounts,
                    selectedDayEpoch = uiState.selectedDayEpoch,
                    onSelectDay = viewModel::selectDay,
                )
            }
            item { DayTitle(dayEpoch = uiState.selectedDayEpoch, logCount = uiState.selectedDayLogs.size) }
            if (uiState.selectedDayLogs.isEmpty()) {
                item {
                    Text(
                        text = "这一天还没有练题记录",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 4.dp),
                    )
                }
            }
            items(uiState.selectedDayLogs, key = { it.id }) { log ->
                PracticeLogItem(log = log)
            }
        }
    }
}

/** GitHub 风格练题热力图：7 行（周日开头）× 列，覆盖最近三个月，格子较大便于点选，颜色按练题数分档 */
@Composable
private fun PracticeHeatmap(
    dailyCounts: Map<Long, Int>,
    selectedDayEpoch: Long,
    onSelectDay: (Long) -> Unit,
) {
    val today = remember { LocalDate.now() }
    val gridStart = remember {
        // 近 90 天，起点对齐到周日，保证列对齐且整周铺满
        val start = today.minusDays(90)
        start.minusDays(start.dayOfWeek.value % 7L)
    }
    val totalDays = remember { ChronoUnit.DAYS.between(gridStart, today) + 1 }
    val cols = ((totalDays + 6) / 7).toInt()
    val primary = MiuixTheme.colorScheme.primary
    val surfaceContainer = MiuixTheme.colorScheme.surfaceContainerHighest

    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(0.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(7) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(cols) { col ->
                        val day = gridStart.plusDays((col * 7L + row))
                        if (day.isAfter(today)) {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                            )
                        } else {
                            val dayEpoch = day.toEpochDay()
                            val count = dailyCounts[dayEpoch] ?: 0
                            val color = when {
                                count == 0 -> surfaceContainer
                                count <= 2 -> primary.copy(alpha = 0.4f)
                                count <= 5 -> primary.copy(alpha = 0.65f)
                                count <= 9 -> primary.copy(alpha = 0.85f)
                                else -> primary
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(color)
                                    .then(
                                        if (dayEpoch == selectedDayEpoch) {
                                            Modifier.border(1.5.dp, primary, RoundedCornerShape(3.dp))
                                        } else Modifier
                                    )
                                    .clickable { onSelectDay(dayEpoch) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayTitle(dayEpoch: Long, logCount: Int) {
    val today = LocalDate.now()
    val date = LocalDate.ofEpochDay(dayEpoch)
    val title = when (date) {
        today -> "今天"
        today.minusDays(1) -> "昨天"
        else ->
            if (date.year == today.year) "${date.monthValue}月${date.dayOfMonth}日"
            else "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
    }
    Text(
        text = "$title · 练了 $logCount 题",
        fontSize = 13.sp,
        color = MiuixTheme.colorScheme.onBackgroundVariant,
        modifier = Modifier.padding(top = 8.dp, start = 4.dp, bottom = 2.dp),
    )
}

private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

/** 单条练题记录：时间 + 题目 + 分类·难度 */
@Composable
private fun PracticeLogItem(log: PracticeLogEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = timeFormatter.format(Date(log.practicedAt)),
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                modifier = Modifier.width(48.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.questionTitle,
                    fontSize = 15.sp,
                    color = MiuixTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${log.categoryName ?: "未分类"} · ${difficultyLabel(log.difficulty)}",
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
            }
        }
    }
}

private fun difficultyLabel(d: Int): String = when (d) {
    1 -> "简单"
    2 -> "中等"
    3 -> "困难"
    else -> "未知"
}
