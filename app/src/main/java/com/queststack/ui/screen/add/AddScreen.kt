package com.queststack.ui.screen.add

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.queststack.data.db.Category
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownArrowEndAction
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AddScreen(
    onBack: () -> Unit,
    viewModel: AddViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var categorySelectorHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

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
    // 保存结果提示：message 仅用于 Toast，返回导航由独立事件 saved 驱动
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }
    // 保存成功事件：收到后返回（事件走 Channel，不残留，离开页面后再次进入不会误触发）
    LaunchedEffect(Unit) {
        viewModel.savedEvents.collect { onBack() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // 全屏 overlay 必须铺底，否则下面的 pager 内容透出来
            .background(MiuixTheme.colorScheme.surface),
    ) {
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
            // 分类选择
            SectionLabel("分类")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { categorySelectorHeight = it.height },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MiuixTheme.colorScheme.secondaryContainer)
                        .clickable { categoryMenuExpanded = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = categoryLabel(uiState.categories, uiState.selectedCategoryId),
                        fontSize = 15.sp,
                        color = MiuixTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    DropdownArrowEndAction(actionColor = MiuixTheme.colorScheme.onSecondaryContainer)
                }
                if (categoryMenuExpanded) {
                    Popup(
                        alignment = Alignment.TopStart,
                        offset = IntOffset(0, categorySelectorHeight + with(density) { 4.dp.roundToPx() }),
                        onDismissRequest = { categoryMenuExpanded = false },
                        properties = PopupProperties(focusable = true),
                    ) {
                        AddCategoryPanel(
                            categories = uiState.categories,
                            selectedCategoryId = uiState.selectedCategoryId,
                            onSelect = { id ->
                                viewModel.selectCategory(id)
                                categoryMenuExpanded = false
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 难度
            SectionLabel("难度")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DifficultyChip("简单", selected = uiState.difficulty == 1, onClick = { viewModel.selectDifficulty(1) })
                DifficultyChip("中等", selected = uiState.difficulty == 2, onClick = { viewModel.selectDifficulty(2) })
                DifficultyChip("困难", selected = uiState.difficulty == 3, onClick = { viewModel.selectDifficulty(3) })
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 问题标题（单行）
            TextField(
                state = titleState,
                modifier = Modifier.fillMaxWidth(),
                label = "问题标题",
                useLabelAsPlaceholder = true,
                lineLimits = TextFieldLineLimits.SingleLine,
            )
            Spacer(modifier = Modifier.height(12.dp))

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

            // AI 功能区：未配置接口时按钮禁用，点击兜底 Toast 提示
            if (uiState.title.isNotBlank() && uiState.answer.isBlank()) {
                // 标题已填、答案为空 → 主按钮：AI 生成答案
                Button(
                    onClick = {
                        if (!aiConfigured) {
                            Toast.makeText(context, "请先在设置中配置 AI 接口", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.generateAnswer()
                        }
                    },
                    enabled = aiConfigured && !uiState.aiBusy,
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                ) {
                    Text(
                        text = if (uiState.aiBusy) "AI 处理中…" else "AI 生成答案",
                        fontSize = 14.sp,
                    )
                }
            } else if (uiState.answer.isNotBlank()) {
                // 答案非空 → 次要按钮：AI 优化表述 / AI 优化格式
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (!aiConfigured) {
                                Toast.makeText(context, "请先在设置中配置 AI 接口", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.optimizeAnswer()
                            }
                        },
                        enabled = aiConfigured && !uiState.aiBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = if (uiState.aiBusy) "AI 处理中…" else "AI 优化表述",
                            fontSize = 14.sp,
                        )
                    }
                    Button(
                        onClick = {
                            if (!aiConfigured) {
                                Toast.makeText(context, "请先在设置中配置 AI 接口", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.formatAnswer()
                            }
                        },
                        enabled = aiConfigured && !uiState.aiBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = if (uiState.aiBusy) "AI 处理中…" else "AI 优化格式",
                            fontSize = 14.sp,
                        )
                    }
                }
            }

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
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = MiuixTheme.colorScheme.onBackgroundVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun DifficultyChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(
                if (selected) MiuixTheme.colorScheme.primary
                else MiuixTheme.colorScheme.surfaceContainer,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) MiuixTheme.colorScheme.onPrimary
            else MiuixTheme.colorScheme.onSurfaceContainer,
        )
    }
}

private fun categoryLabel(categories: List<Category>, selectedId: Long?): String =
    if (selectedId == null) "未分类"
    else categories.firstOrNull { it.id == selectedId }?.name ?: "未分类"

/** 分类下拉面板（自绘，miuix 0.9.3 无公开 DropdownMenu 弹层组件） */
@Composable
private fun AddCategoryPanel(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onSelect: (Long?) -> Unit,
) {
    Card(
        modifier = Modifier
            .widthIn(min = 200.dp, max = 280.dp)
            .dropShadow(
                shape = RoundedCornerShape(16.dp),
                shadow = Shadow(radius = 12.dp, color = Color.Black, alpha = 0.15f),
            ),
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(0.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            DropdownRow("未分类", isSelected = selectedCategoryId == null, onClick = { onSelect(null) })
            categories.forEach { category ->
                DropdownRow(
                    text = category.name,
                    isSelected = category.id == selectedCategoryId,
                    onClick = { onSelect(category.id) },
                )
            }
        }
    }
}

@Composable
private fun DropdownRow(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            color = if (isSelected) MiuixTheme.colorScheme.primary
            else MiuixTheme.colorScheme.onSurfaceContainer,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (isSelected) {
            Icon(
                imageVector = MiuixIcons.Basic.Check,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
