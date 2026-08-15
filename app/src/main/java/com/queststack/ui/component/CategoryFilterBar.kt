package com.queststack.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.queststack.data.db.Category
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownArrowEndAction
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 练题页 / 题库页共用的筛选栏：分类下拉 + 难度四档 chips + 可选右侧"换一题"。
 *
 * 两页必须走同一组件，避免布局漂移；难度行固定 44dp 高，保证有无"换一题"
 * 按钮时 chips 的垂直位置完全一致。
 *
 * @param onShuffle 非空时在难度行右侧渲染"换一题"按钮（题库页传 null）。
 */
@Composable
fun CategoryFilterBar(
    categories: List<Category>,
    selectedCategoryId: Long?,
    difficulty: Int?,
    onSelectCategory: (Long?) -> Unit,
    onSelectDifficulty: (Int?) -> Unit,
    onShuffle: (() -> Unit)? = null,
) {
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var categoryButtonHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val currentCategoryName =
        if (selectedCategoryId == null) "全部"
        else categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "全部"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 分类选择（DropdownMenu）
        Box(modifier = Modifier.onSizeChanged { categoryButtonHeight = it.height }) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MiuixTheme.colorScheme.surfaceContainer)
                    .clickable { categoryMenuExpanded = true }
                    .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = currentCategoryName,
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                    maxLines = 1,
                )
                DropdownArrowEndAction(actionColor = MiuixTheme.colorScheme.onBackgroundVariant)
            }
            if (categoryMenuExpanded) {
                Popup(
                    alignment = Alignment.TopStart,
                    offset = IntOffset(0, categoryButtonHeight + with(density) { 4.dp.roundToPx() }),
                    onDismissRequest = { categoryMenuExpanded = false },
                    properties = PopupProperties(focusable = true),
                ) {
                    CategoryDropdownPanel(
                        categories = categories,
                        selectedCategoryId = selectedCategoryId,
                        onSelect = { id ->
                            onSelectCategory(id)
                            categoryMenuExpanded = false
                        },
                    )
                }
            }
        }
        // 难度筛选 chips（null = 全部）+ 可选右侧"换一题"（固定行高保证两页对齐）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DifficultyChip("全部", selected = difficulty == null, onClick = { onSelectDifficulty(null) })
                DifficultyChip("简单", selected = difficulty == 1, onClick = { onSelectDifficulty(1) })
                DifficultyChip("中等", selected = difficulty == 2, onClick = { onSelectDifficulty(2) })
                DifficultyChip("困难", selected = difficulty == 3, onClick = { onSelectDifficulty(3) })
            }
            if (onShuffle != null) {
                IconButton(onClick = onShuffle) {
                    Icon(
                        imageVector = MiuixIcons.Refresh,
                        contentDescription = "换一题",
                        tint = MiuixTheme.colorScheme.onBackgroundVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
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

/** 分类下拉面板（自绘，miuix 0.9.3 无公开 DropdownMenu 弹层组件） */
@Composable
private fun CategoryDropdownPanel(
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
            DropdownRow("全部", isSelected = selectedCategoryId == null, onClick = { onSelect(null) })
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
