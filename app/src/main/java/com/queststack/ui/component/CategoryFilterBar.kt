package com.queststack.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.queststack.data.db.Category
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 练题页 / 题库页共用的筛选栏：单行布局，左分类、右难度两个收缩式下拉。
 * 两页必须走同一组件，避免布局漂移。
 */
@Composable
fun CategoryFilterBar(
    categories: List<Category>,
    selectedCategoryId: Long?,
    difficulty: Int?,
    onSelectCategory: (Long?) -> Unit,
    onSelectDifficulty: (Int?) -> Unit,
) {
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var difficultyMenuExpanded by remember { mutableStateOf(false) }
    var categoryButtonHeight by remember { mutableIntStateOf(0) }
    var difficultyButtonHeight by remember { mutableIntStateOf(0) }
    var categoryButtonWidth by remember { mutableIntStateOf(0) }
    var difficultyButtonWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    // 值前带"分类/难度"前缀，避免"全部/全部"语义不清
    val currentCategoryName = when (selectedCategoryId) {
        null -> "分类：全部"
        Category.UNCATEGORIZED_ID -> "分类：未分类"
        else -> "分类：${categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "全部"}"
    }
    val currentDifficultyName = "难度：${difficultyLabel(difficulty)}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 左侧：分类选择（收缩式下拉）
        Box(
            modifier = Modifier
                .weight(1f)
                .onSizeChanged {
                    categoryButtonHeight = it.height
                    categoryButtonWidth = it.width
                },
        ) {
            FilterDropdownButton(
                label = currentCategoryName,
                expanded = categoryMenuExpanded,
                onClick = { categoryMenuExpanded = true },
            )
            if (categoryMenuExpanded) {
                Popup(
                    alignment = Alignment.TopStart,
                    offset = IntOffset(0, categoryButtonHeight + with(density) { 4.dp.roundToPx() }),
                    onDismissRequest = { categoryMenuExpanded = false },
                    properties = PopupProperties(focusable = true),
                ) {
                    // 面板宽度与按钮对齐（= 各自半边），不越界
                    CategoryDropdownPanel(
                        categories = categories,
                        selectedCategoryId = selectedCategoryId,
                        panelWidth = with(density) { categoryButtonWidth.toDp() },
                        onSelect = { id ->
                            onSelectCategory(id)
                            categoryMenuExpanded = false
                        },
                    )
                }
            }
        }
        // 右侧：难度选择（收缩式下拉，与分类同款式）
        Box(
            modifier = Modifier
                .weight(1f)
                .onSizeChanged {
                    difficultyButtonHeight = it.height
                    difficultyButtonWidth = it.width
                },
        ) {
            FilterDropdownButton(
                label = currentDifficultyName,
                expanded = difficultyMenuExpanded,
                onClick = { difficultyMenuExpanded = true },
            )
            if (difficultyMenuExpanded) {
                Popup(
                    alignment = Alignment.TopStart,
                    offset = IntOffset(0, difficultyButtonHeight + with(density) { 4.dp.roundToPx() }),
                    onDismissRequest = { difficultyMenuExpanded = false },
                    properties = PopupProperties(focusable = true),
                ) {
                    // 面板宽度与按钮对齐（= 各自半边），不越界
                    DifficultyDropdownPanel(
                        difficulty = difficulty,
                        panelWidth = with(density) { difficultyButtonWidth.toDp() },
                        onSelect = { d ->
                            onSelectDifficulty(d)
                            difficultyMenuExpanded = false
                        },
                    )
                }
            }
        }
    }
}

/**
 * 收缩式筛选按钮：与展开面板同宽（占满各自半边）、胶囊背景 + 当前值左对齐；
 * 箭头固定在右端：收起时单向右箭头，展开时向下箭头。
 */
@Composable
private fun FilterDropdownButton(
    label: String,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (expanded) {
            // 展开态：单向下箭头
            Icon(
                imageVector = MiuixIcons.ExpandMore,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onBackgroundVariant,
                modifier = Modifier.size(16.dp),
            )
        } else {
            // 收起态：单向右箭头
            Icon(
                imageVector = MiuixIcons.ChevronForward,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onBackgroundVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** 难度数值 → 展示文案（null = 全部） */
private fun difficultyLabel(d: Int?): String = when (d) {
    null -> "全部"
    1 -> "简单"
    2 -> "中等"
    3 -> "困难"
    else -> "全部"
}

/** 难度下拉面板 */
@Composable
private fun DifficultyDropdownPanel(
    difficulty: Int?,
    panelWidth: Dp,
    onSelect: (Int?) -> Unit,
) {
    Card(
        modifier = Modifier
            .width(panelWidth)
            .dropShadow(
                shape = RoundedCornerShape(16.dp),
                shadow = Shadow(radius = 12.dp, color = Color.Black, alpha = 0.15f),
            ),
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(0.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            DropdownRow("全部", isSelected = difficulty == null, onClick = { onSelect(null) })
            DropdownRow("简单", isSelected = difficulty == 1, onClick = { onSelect(1) })
            DropdownRow("中等", isSelected = difficulty == 2, onClick = { onSelect(2) })
            DropdownRow("困难", isSelected = difficulty == 3, onClick = { onSelect(3) })
        }
    }
}

/** 分类下拉面板（自绘，miuix 0.9.3 无公开 DropdownMenu 弹层组件） */
@Composable
private fun CategoryDropdownPanel(
    categories: List<Category>,
    selectedCategoryId: Long?,
    panelWidth: Dp,
    onSelect: (Long?) -> Unit,
) {
    Card(
        modifier = Modifier
            .width(panelWidth)
            .dropShadow(
                shape = RoundedCornerShape(16.dp),
                shadow = Shadow(radius = 12.dp, color = Color.Black, alpha = 0.15f),
            ),
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(0.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            DropdownRow("全部", isSelected = selectedCategoryId == null, onClick = { onSelect(null) })
            categories.forEach { category ->
                DropdownRow(
                    text = category.name,
                    isSelected = category.id == selectedCategoryId,
                    onClick = { onSelect(category.id) },
                )
            }
            DropdownRow("未分类", isSelected = selectedCategoryId == Category.UNCATEGORIZED_ID, onClick = { onSelect(Category.UNCATEGORIZED_ID) })
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
