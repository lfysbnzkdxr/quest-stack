package com.queststack.ui.screen.add

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
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.queststack.ui.component.AiActionButtons
import com.queststack.ui.component.CategorySelector
import com.queststack.ui.component.DifficultySelector
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AddScreen(
    onBack: () -> Unit,
    viewModel: AddViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // AI 配置可用性：baseUrl 与 model 均已填写才启用 AI 按钮
    val aiConfig = uiState.aiConfig
    val aiConfigured = aiConfig != null && aiConfig.baseUrl.isNotBlank() && aiConfig.model.isNotBlank()

    // 每次进入页面重置表单：ViewModel 挂在 Activity 级 ViewModelStore，离开再进入会复用实例并残留上次草稿
    LaunchedEffect(Unit) {
        viewModel.reset()
    }
    // 输入由 TextFieldState 管理（本地展示），值同步到 ViewModel 的 String 状态
    val titleState = remember { TextFieldState() }
    val answerState = remember { TextFieldState() }

    // 输入变化 → ViewModel
    LaunchedEffect(titleState) {
        snapshotFlow { titleState.text.toString() }.collect { viewModel.onTitleChange(it) }
    }
    LaunchedEffect(answerState) {
        snapshotFlow { answerState.text.toString() }.collect { viewModel.onAnswerChange(it) }
    }
    // ViewModel 状态（保存清空 / 标准化规范化标题）→ 回写 TextFieldState
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
    // 保存结果提示：message 事件驱动页内 Snackbar，返回导航由独立事件 saved 驱动
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }
    // 保存成功事件：收到后返回（事件走 Channel，不残留，离开页面后再次进入不会误触发）
    LaunchedEffect(Unit) {
        viewModel.savedEvents.collect { onBack() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // 全屏 overlay 必须铺底，否则下面的 pager 内容透出来
            .background(MiuixTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SmallTopAppBar(
            title = "添加题目",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            // 与详情页编辑态布局一致：问题编辑栏 → 分类+难度并排 → 答案
            // 问题标题（单行）
            TextField(
                state = titleState,
                modifier = Modifier.fillMaxWidth(),
                label = "问题标题",
                useLabelAsPlaceholder = true,
                lineLimits = TextFieldLineLimits.SingleLine,
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 分类 + 难度（并排一行，收缩式下拉与题库一致）
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategorySelector(
                    categories = uiState.categories,
                    selectedCategoryId = uiState.selectedCategoryId,
                    onSelect = viewModel::selectCategory,
                    modifier = Modifier.weight(1f),
                )
                DifficultySelector(
                    difficulty = uiState.difficulty,
                    onSelect = viewModel::selectDifficulty,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 答案 / 问答内容（多行）
            TextField(
                state = answerState,
                modifier = Modifier.fillMaxWidth(),
                label = "答案或问答内容（选填）",
                useLabelAsPlaceholder = true,
                lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 12),
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 自动标准化格式（次要按钮）
            Button(onClick = { viewModel.standardize() }) {
                Text(text = "自动标准化格式", fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))

            // AI 功能区：标题已填答案为空 → 生成；答案非空 → 优化表述/格式
            AiActionButtons(
                titleIsBlank = uiState.title.isBlank(),
                answerIsBlank = uiState.answer.isBlank(),
                aiConfigured = aiConfigured,
                aiBusy = uiState.aiBusy,
                onGenerate = viewModel::generateAnswer,
                onOptimize = viewModel::optimizeAnswer,
                onFormat = viewModel::formatAnswer,
            )

            Spacer(modifier = Modifier.height(12.dp))
            // 保存（本版顶栏无 actions 位，置于表单底部）
            Button(
                onClick = { viewModel.save() },
                enabled = uiState.title.isNotBlank() && !uiState.saving,
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(text = if (uiState.saving) "保存中…" else "保存", fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        }
        SnackbarHost(
            state = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
