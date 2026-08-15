package com.queststack.ui.screen.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.queststack.data.db.Category
import com.queststack.data.db.QuestionWithRounds
import com.queststack.ui.component.CategoryFilterBar
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 难度数值 → 展示文案（1=简单 2=中等 3=困难，非法值回退"简单"） */
private fun difficultyLabel(d: Int): String = when (d) {
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
fun PracticeScreen(
    mode: PracticeMode,
    onStartInterview: (Long?, Int?) -> Unit,
    viewModel: PracticeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // 练题模式：点"开始练习"在当前页弹出浮层卡片刷题，不跳转页面
    var popupQuestionId by remember { mutableStateOf<Long?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶栏由 MainScreen 的 Scaffold topBar 槽位统一渲染（标题"练题"，点击切换刷题/面试）
        CategoryFilterBar(
            categories = uiState.categories,
            selectedCategoryId = uiState.selectedCategoryId,
            difficulty = uiState.difficulty,
            onSelectCategory = viewModel::selectCategory,
            onSelectDifficulty = viewModel::selectDifficulty,
            onShuffle = viewModel::shuffle,
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when {
                uiState.loading -> LoadingPlaceholder()
                uiState.empty -> EmptyPlaceholder()
                else -> uiState.current?.let { item ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PracticeCard(
                            item = item,
                            categories = uiState.categories,
                            showRounds = mode == PracticeMode.Interview,
                            onStart = { popupQuestionId = item.question.id },
                        )
                        if (mode == PracticeMode.Interview) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    onStartInterview(uiState.selectedCategoryId, uiState.difficulty)
                                },
                                colors = ButtonDefaults.buttonColorsPrimary(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .height(48.dp),
                            ) {
                                Text(text = "开始面试", fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (popupQuestionId != null) {
        PracticePopup(
            questionId = popupQuestionId!!,
            onDismiss = { popupQuestionId = null },
        )
    }
}

/** 练题模式刷题浮层：Dialog 全屏窗口（盖住顶栏/导航栏），
 * 卡片顶到半个顶栏、底到半个导航栏，问题/答案内容可滚动，操作按钮固定在卡片最底端 */
@Composable
private fun PracticePopup(
    questionId: Long,
    onDismiss: () -> Unit,
    viewModel: PracticeChatViewModel = viewModel(key = "practice_popup_$questionId") {
        PracticeChatViewModel(questionId)
    },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.reset() }
    val question = uiState.question
    val revealed = uiState.revealed.coerceIn(0, 1)
    val exhausted = revealed >= 1

    Dialog(
        onDismissRequest = onDismiss,
        // 全屏窗口：覆盖状态栏与系统导航栏，否则 Dialog 窗口只覆盖内容区
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        // 全屏遮罩：点外部关闭；卡片区域消费点击避免误关
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(onClick = onDismiss),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    // 顶到顶栏一半（露出顶栏标题上半）、底到半个底部导航栏
                    .padding(top = 59.dp, bottom = 41.dp)
                    .padding(horizontal = 24.dp)
                    .pointerInput(Unit) { detectTapGestures { } },
                cornerRadius = 24.dp,
                insideMargin = PaddingValues(0.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "面试官",
                            fontSize = 11.sp,
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "完成",
                            fontSize = 14.sp,
                            color = MiuixTheme.colorScheme.primary,
                            modifier = Modifier.clickable(onClick = onDismiss),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            text = question?.question?.title
                                ?: if (uiState.loading) "加载中…" else "题目不存在",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MiuixTheme.colorScheme.onBackground,
                            lineHeight = 23.sp,
                        )
                        if (exhausted && question != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "参考答案",
                                fontSize = 11.sp,
                                color = MiuixTheme.colorScheme.onBackgroundVariant,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = question.question.answer.ifBlank { "（答案为空）" },
                                fontSize = 14.sp,
                                color = MiuixTheme.colorScheme.onSurfaceContainer,
                                lineHeight = 21.sp,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (exhausted) {
                            Button(
                                onClick = viewModel::prevQuestion,
                                enabled = uiState.history.isNotEmpty(),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(text = "上一问", fontSize = 14.sp)
                            }
                            Button(
                                onClick = viewModel::nextQuestion,
                                colors = ButtonDefaults.buttonColorsPrimary(),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(text = "下一问", fontSize = 14.sp)
                            }
                        } else {
                            Button(
                                onClick = viewModel::revealAnswer,
                                colors = ButtonDefaults.buttonColorsPrimary(),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(text = "查看答案", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 随机题目大卡片：分类/难度标签 + 大字标题 +（面试模式）追问轮数 + 开始练习按钮 */
@Composable
private fun PracticeCard(
    item: QuestionWithRounds,
    categories: List<Category>,
    showRounds: Boolean,
    onStart: () -> Unit,
) {
    val question = item.question
    val categoryName = categories.firstOrNull { it.id == question.categoryId }?.name ?: "未分类"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clickable(onClick = onStart),
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                    text = difficultyLabel(question.difficulty),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = difficultyColor(question.difficulty),
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(difficultyColor(question.difficulty).copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = question.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (showRounds) {
                Text(
                    text = "${item.rounds.size} 轮追问",
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(text = "开始练习", fontSize = 15.sp)
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
private fun EmptyPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = MiuixIcons.ListView,
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
                text = "去「添加」页创建第一道题吧",
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.6f),
            )
        }
    }
}
