package com.queststack.ui.screen.practice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.queststack.data.db.Question
import com.queststack.ui.component.CategoryFilterBar
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.Help
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 闪卡练题页：全屏覆盖，看题 → 思考 → 显示答案 → 下一题 */
@Composable
fun PracticeSessionScreen(
    session: PracticeSession,
    onBack: () -> Unit,
) {
    val viewModel: PracticeSessionViewModel = viewModel(
        key = "practice_session_${session.categoryId}_${session.difficulty}_${session.startQuestionId}",
        initializer = { PracticeSessionViewModel(session) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            // 全屏 overlay 必须铺底，否则下面的 pager 内容透出来
            .background(MiuixTheme.colorScheme.surface),
    ) {
        SmallTopAppBar(
            title = "练题",
            color = MiuixTheme.colorScheme.surface,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = MiuixIcons.ChevronBackward,
                        contentDescription = "返回",
                        tint = MiuixTheme.colorScheme.onBackground,
                        modifier = Modifier.size(22.dp),
                    )
                }
            },
        )
        // 闪卡内筛选：分类 + 难度，变更后自动换题
        CategoryFilterBar(
            categories = uiState.categories,
            selectedCategoryId = uiState.selectedCategoryId,
            difficulty = uiState.difficulty,
            onSelectCategory = viewModel::selectCategory,
            onSelectDifficulty = viewModel::selectDifficulty,
        )
        when {
            uiState.loading -> LoadingPlaceholder()
            uiState.empty -> EmptyPlaceholder(onBack = onBack)
            else -> uiState.current?.let { question ->
                FlashCard(
                    question = question,
                    categoryName = uiState.categories.firstOrNull { it.id == question.categoryId }?.name ?: "未分类",
                    revealed = uiState.revealed,
                    onReveal = viewModel::revealAnswer,
                    onNext = viewModel::next,
                )
            }
        }
    }
}

@Composable
private fun FlashCard(
    question: Question,
    categoryName: String,
    revealed: Boolean,
    onReveal: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        // 题目 + 答案区（可滚动，避免长题/长答案溢出）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                insideMargin = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = categoryName,
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(MiuixTheme.colorScheme.surfaceContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                        Text(
                            text = question.difficultyLabel(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = question.difficultyColor(),
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(question.difficultyColor().copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = question.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.onBackground,
                        lineHeight = 31.sp,
                    )
                }
            }
            AnimatedVisibility(
                visible = revealed,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    cornerRadius = 20.dp,
                    insideMargin = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                ) {
                    Column {
                        Text(
                            text = "参考答案",
                            fontSize = 11.sp,
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = question.answer.ifBlank { "（答案为空）" },
                            fontSize = 15.sp,
                            color = MiuixTheme.colorScheme.onSurfaceContainer,
                            lineHeight = 22.sp,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        // 底部操作按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        ) {
            if (!revealed) {
                Button(
                    onClick = onReveal,
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(text = "显示答案", fontSize = 15.sp)
                }
            } else {
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(text = "下一题", fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun LoadingPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "加载中…",
            fontSize = 14.sp,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
        )
    }
}

@Composable
private fun EmptyPlaceholder(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = MiuixIcons.Help,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "该范围内暂无题目",
                fontSize = 15.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "换个筛选条件，或先去「题库」添加题目",
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBack) {
                Text(text = "返回", fontSize = 14.sp)
            }
        }
    }
}

/** 难度数值 → 展示文案（1=简单 2=中等 3=困难，非法值回退"简单"） */
private fun Question.difficultyLabel(): String = when (difficulty) {
    1 -> "简单"
    2 -> "中等"
    3 -> "困难"
    else -> "简单"
}

private fun Question.difficultyColor(): Color = when (difficulty) {
    1 -> Color(0xFF00A871)
    2 -> Color(0xFFE8890C)
    3 -> Color(0xFFE5484D)
    else -> Color(0xFF00A871)
}
