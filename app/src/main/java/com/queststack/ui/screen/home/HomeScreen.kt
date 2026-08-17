package com.queststack.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.queststack.ui.component.PageScaffold
import com.queststack.ui.screen.practice.PracticeSession
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 主页（仪表盘）：随机练题 */
@Composable
fun HomeScreen(
    onStartPractice: (PracticeSession) -> Unit,
    onGoLibrary: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PageScaffold(title = "题栈") { scrollBehavior ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                RandomPracticeCard(
                    totalCount = uiState.totalCount,
                    onStart = { onStartPractice(PracticeSession()) },
                    onGoLibrary = onGoLibrary,
                )
            }
        }
    }
}

/** 随机练题大卡：主题色渐变 + 大图标，一键开刷 */
@Composable
private fun RandomPracticeCard(
    totalCount: Int,
    onStart: () -> Unit,
    onGoLibrary: () -> Unit,
) {
    val hasQuestions = totalCount > 0
    val primary = MiuixTheme.colorScheme.primary
    val onPrimary = MiuixTheme.colorScheme.onPrimary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = if (hasQuestions) onStart else onGoLibrary),
        cornerRadius = 24.dp,
        insideMargin = PaddingValues(0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(primary.copy(alpha = 0.92f))
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Icon(
                imageVector = MiuixIcons.GridView,
                contentDescription = null,
                tint = onPrimary.copy(alpha = 0.9f),
                modifier = Modifier.size(34.dp),
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "随机练题",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = onPrimary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (hasQuestions) "从全部 $totalCount 道题中随机抽取"
                else "题库暂无题目，去添加第一道题",
                fontSize = 13.sp,
                color = onPrimary.copy(alpha = 0.8f),
            )
        }
    }
}