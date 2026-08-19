package com.queststack.ui.screen.detail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.queststack.ui.component.AiActionButtons
import com.queststack.ui.component.AnswerText
import com.queststack.ui.component.CategorySelector
import com.queststack.ui.component.DifficultyChip
import com.queststack.ui.component.PageScaffold
import com.queststack.ui.component.SectionLabel
import com.queststack.ui.screen.library.difficultyColor
import com.queststack.ui.screen.library.difficultyLabel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 题目详情页：全屏覆盖，默认展示该题的问题/答案/分类/难度；点编辑在页内切换为可编辑态 */
@Composable
fun QuestionDetailScreen(
    questionId: Long,
    onBack: () -> Unit,
) {
    val viewModel: QuestionDetailViewModel = viewModel(
        key = "question_detail_$questionId",
        initializer = { QuestionDetailViewModel(questionId) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val aiConfig = uiState.aiConfig
    val aiConfigured = aiConfig != null && aiConfig.baseUrl.isNotBlank() && aiConfig.model.isNotBlank()

    // 编辑态为 Screen 本地状态：重进详情页时重新初始化为查看态，避免 VM 复用导致的编辑态残留闪烁
    var editing by remember { mutableStateOf(false) }

    // 编辑态输入：TextFieldState 本地展示，值同步到 ViewModel
    val titleState = remember { TextFieldState() }
    val answerState = remember { TextFieldState() }

    LaunchedEffect(titleState) {
        snapshotFlow { titleState.text.toString() }.collect { viewModel.onTitleChange(it) }
    }
    LaunchedEffect(answerState) {
        snapshotFlow { answerState.text.toString() }.collect { viewModel.onAnswerChange(it) }
    }
    LaunchedEffect(uiState.title) {
        if (titleState.text.toString() != uiState.title) {
            titleState.edit { replace(0, length, uiState.title) }
        }
    }
    LaunchedEffect(uiState.answer) {
        if (answerState.text.toString() != uiState.answer) {
            answerState.edit { replace(0, length, uiState.answer) }
        }
    }
    // 保存成功事件：收到后退出编辑态（事件不残留）
    LaunchedEffect(Unit) {
        viewModel.savedEvents.collect { editing = false }
    }
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    PageScaffold(
        title = if (editing) "编辑题目" else "题目详情",
        navigationIcon = {
            IconButton(onClick = { if (editing) editing = false else onBack() }) {
                Icon(
                    imageVector = MiuixIcons.ChevronBackward,
                    contentDescription = if (editing) "取消编辑" else "返回",
                    tint = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
        actions = {
            if (editing) {
                IconButton(
                    onClick = { viewModel.save() },
                    enabled = uiState.title.isNotBlank() && !uiState.saving,
                ) {
                    Icon(
                        imageVector = MiuixIcons.Basic.Check,
                        contentDescription = "保存",
                        tint = MiuixTheme.colorScheme.onBackground,
                        modifier = Modifier.size(22.dp),
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        viewModel.startEdit()
                        editing = true
                    },
                    enabled = uiState.question != null,
                ) {
                    Icon(
                        imageVector = MiuixIcons.Edit,
                        contentDescription = "编辑",
                        tint = MiuixTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
    ) { _ ->
        if (editing) {
            EditContent(
                uiState = uiState,
                titleState = titleState,
                answerState = answerState,
                aiConfigured = aiConfigured,
                viewModel = viewModel,
            )
        } else {
            ViewContent(uiState = uiState)
        }
    }
}

@Composable
private fun ViewContent(uiState: QuestionDetailUiState) {
    val question = uiState.question
    if (question == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "题目不存在或已删除",
                fontSize = 14.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                text = question.title,
                fontSize = 22.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetaChip(
                    text = uiState.categories.firstOrNull { it.id == question.categoryId }?.name
                        ?: "未分类",
                )
                MetaChip(
                    text = difficultyLabel(question.difficulty),
                    textColor = difficultyColor(question.difficulty),
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            if (question.answer.isBlank()) {
                Text(
                    text = "暂无答案",
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
            } else {
                AnswerText(
                    text = question.answer,
                    modifier = Modifier.fillMaxWidth(),
                    color = MiuixTheme.colorScheme.onBackground,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun EditContent(
    uiState: QuestionDetailUiState,
    titleState: TextFieldState,
    answerState: TextFieldState,
    aiConfigured: Boolean,
    viewModel: QuestionDetailViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        SectionLabel("分类")
        CategorySelector(
            categories = uiState.categories,
            selectedCategoryId = uiState.selectedCategoryId,
            onSelect = viewModel::selectCategory,
        )
        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel("难度")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DifficultyChip("简单", selected = uiState.difficulty == 1, onClick = { viewModel.selectDifficulty(1) })
            DifficultyChip("中等", selected = uiState.difficulty == 2, onClick = { viewModel.selectDifficulty(2) })
            DifficultyChip("困难", selected = uiState.difficulty == 3, onClick = { viewModel.selectDifficulty(3) })
        }
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            state = titleState,
            modifier = Modifier.fillMaxWidth(),
            label = "问题标题",
            useLabelAsPlaceholder = true,
            lineLimits = TextFieldLineLimits.SingleLine,
        )
        Spacer(modifier = Modifier.height(12.dp))

        TextField(
            state = answerState,
            modifier = Modifier.fillMaxWidth(),
            label = "答案或问答内容（选填）",
            useLabelAsPlaceholder = true,
            lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 12),
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { viewModel.standardize() }) {
            Text(text = "自动标准化格式", fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))

        AiActionButtons(
            titleIsBlank = uiState.title.isBlank(),
            answerIsBlank = uiState.answer.isBlank(),
            aiConfigured = aiConfigured,
            aiBusy = uiState.aiBusy,
            onGenerate = viewModel::generateAnswer,
            onOptimize = viewModel::optimizeAnswer,
            onFormat = viewModel::formatAnswer,
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun MetaChip(text: String, textColor: Color = MiuixTheme.colorScheme.onSecondaryContainer) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(MiuixTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
        )
    }
}
