package com.queststack.ui.screen.library

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.queststack.data.db.Category
import com.queststack.data.db.QuestionWithRounds
import com.queststack.ui.component.CategoryFilterBar
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 难度数值 → 展示文案（1=简单 2=中等 3=困难，非法值回退"简单"） */
fun difficultyLabel(d: Int): String = when (d) {
    1 -> "简单"
    2 -> "中等"
    3 -> "困难"
    else -> "简单"
}

private fun difficultyColor(d: Int): Color = when (d) {
    1 -> Color(0xFF00A871)
    2 -> Color(0xFFE8890C)
    3 -> Color(0xFFE5484D)
    else -> Color(0xFF00A871)
}

@Composable
fun LibraryScreen(
    onQuestionClick: (Long) -> Unit,
    viewModel: LibraryViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var deleteTarget by remember { mutableStateOf<QuestionWithRounds?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        CategoryFilterBar(
            categories = uiState.categories,
            selectedCategoryId = uiState.selectedCategoryId,
            difficulty = uiState.difficulty,
            onSelectCategory = viewModel::selectCategory,
            onSelectDifficulty = viewModel::selectDifficulty,
        )
        when {
            uiState.loading -> LoadingPlaceholder()
            uiState.questions.isEmpty() -> EmptyPlaceholder(
                hasFilter = uiState.selectedCategoryId != null || uiState.difficulty != null,
            )
            else -> QuestionList(
                questions = uiState.questions,
                categories = uiState.categories,
                onQuestionClick = onQuestionClick,
                onDelete = { deleteTarget = it },
            )
        }
    }

    deleteTarget?.let { target ->
        DeleteConfirmDialog(
            target = target,
            onConfirm = {
                viewModel.deleteQuestion(target)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun QuestionList(
    questions: List<QuestionWithRounds>,
    categories: List<Category>,
    onQuestionClick: (Long) -> Unit,
    onDelete: (QuestionWithRounds) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(questions, key = { it.question.id }) { item ->
            QuestionCard(
                item = item,
                categories = categories,
                onQuestionClick = { onQuestionClick(item.question.id) },
                onDelete = { onDelete(item) },
            )
        }
    }
}

@Composable
private fun QuestionCard(
    item: QuestionWithRounds,
    categories: List<Category>,
    onQuestionClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val question = item.question
    val categoryName = categories.firstOrNull { it.id == question.categoryId }?.name ?: "未分类"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onQuestionClick),
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = question.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                // TODO: 编辑功能（下一里程碑）
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = MiuixIcons.Delete,
                        contentDescription = "删除",
                        tint = MiuixTheme.colorScheme.onBackgroundVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = categoryName,
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
                Text(
                    text = difficultyLabel(question.difficulty),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = difficultyColor(question.difficulty),
                )
                Text(
                    text = "${item.rounds.size} 轮追问",
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
            }
        }
    }
}

@Composable
private fun DeleteConfirmDialog(
    target: QuestionWithRounds,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp,
            insideMargin = PaddingValues(horizontal = 24.dp, vertical = 22.dp),
        ) {
            Column {
                Text(
                    text = "删除题目",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "确定要删除「${target.question.title}」吗？该题及其追问轮次都会被移除，且无法恢复。",
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(text = "取消", onClick = onDismiss)
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColorsPrimary(
                            color = MiuixTheme.colorScheme.error,
                            contentColor = MiuixTheme.colorScheme.onError,
                        ),
                    ) {
                        Text(text = "删除", fontSize = 14.sp)
                    }
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
private fun EmptyPlaceholder(hasFilter: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = MiuixIcons.GridView,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "暂无题目",
                fontSize = 15.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (hasFilter) "换个筛选条件试试" else "去「添加」页创建第一道题吧",
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.6f),
            )
        }
    }
}
