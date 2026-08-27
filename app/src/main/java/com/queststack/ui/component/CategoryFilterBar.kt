package com.queststack.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.queststack.data.db.Category
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowCascadingListPopup

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
    // 值前带"分类/难度"前缀，避免"全部/全部"语义不清
    val currentCategoryName = when (selectedCategoryId) {
        null -> "分类：全部"
        Category.UNCATEGORIZED_ID -> "分类：未分类"
        else -> "分类：${categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "全部"}"
    }
    val currentDifficultyName = "难度：${difficultyLabel(difficulty)}"
    val categoryItems = categoryDropdownItems(categories, selectedCategoryId) {
        onSelectCategory(it)
        categoryMenuExpanded = false
    }
    val difficultyItems = difficultyDropdownItems(difficulty) {
        onSelectDifficulty(it)
        difficultyMenuExpanded = false
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 左侧：分类选择（Miuix 原生弹层）
        Box(modifier = Modifier.weight(1f)) {
            FilterDropdownButton(
                label = currentCategoryName,
                expanded = categoryMenuExpanded,
                onClick = { categoryMenuExpanded = true },
            )
            WindowCascadingListPopup(
                show = categoryMenuExpanded,
                entries = listOf(DropdownEntry(items = categoryItems)),
                onDismissRequest = { categoryMenuExpanded = false },
                enableWindowDim = false,
                alignment = PopupPositionProvider.Align.Start,
            )
        }
        // 右侧：难度选择（Miuix 原生弹层）
        Box(modifier = Modifier.weight(1f)) {
            FilterDropdownButton(
                label = currentDifficultyName,
                expanded = difficultyMenuExpanded,
                onClick = { difficultyMenuExpanded = true },
            )
            WindowCascadingListPopup(
                show = difficultyMenuExpanded,
                entries = listOf(DropdownEntry(items = difficultyItems)),
                onDismissRequest = { difficultyMenuExpanded = false },
                enableWindowDim = false,
            )
        }
    }
}

/**
 * 收缩式筛选/选择按钮：与展开面板同宽、胶囊背景 + 当前值左对齐；
 * 箭头固定在右端：收起时单向右箭头，展开时向下箭头。
 * 供题库筛选栏与添加/编辑题目的分类、难度选择共用，保证样式一致。
 */
@Composable
fun FilterDropdownButton(
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

/** 分类下拉条目（全部 / 各分类 / 未分类），选中态由 Miuix 弹层渲染 */
private fun categoryDropdownItems(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onSelect: (Long?) -> Unit,
): List<DropdownItem> = buildList {
    add(DropdownItem(text = "全部", selected = selectedCategoryId == null, onClick = { onSelect(null) }))
    categories.forEach { category ->
        add(
            DropdownItem(
                text = category.name,
                selected = category.id == selectedCategoryId,
                onClick = { onSelect(category.id) },
            ),
        )
    }
    add(
        DropdownItem(
            text = "未分类",
            selected = selectedCategoryId == Category.UNCATEGORIZED_ID,
            onClick = { onSelect(Category.UNCATEGORIZED_ID) },
        ),
    )
}

/** 难度下拉条目（全部 / 简单 / 中等 / 困难） */
private fun difficultyDropdownItems(
    difficulty: Int?,
    onSelect: (Int?) -> Unit,
): List<DropdownItem> = buildList {
    add(DropdownItem(text = "全部", selected = difficulty == null, onClick = { onSelect(null) }))
    add(DropdownItem(text = "简单", selected = difficulty == 1, onClick = { onSelect(1) }))
    add(DropdownItem(text = "中等", selected = difficulty == 2, onClick = { onSelect(2) }))
    add(DropdownItem(text = "困难", selected = difficulty == 3, onClick = { onSelect(3) }))
}
