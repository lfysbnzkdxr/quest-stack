package com.queststack.ui.component

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.queststack.data.db.Category
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownArrowEndAction
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 表单节标题（分类/难度等分区） */
@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = MiuixTheme.colorScheme.onBackgroundVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

/** 难度选择 chip（1=简单 2=中等 3=困难） */
@Composable
fun DifficultyChip(label: String, selected: Boolean, onClick: () -> Unit) {
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

fun categoryLabel(categories: List<Category>, selectedId: Long?): String =
    if (selectedId == null) "未分类"
    else categories.firstOrNull { it.id == selectedId }?.name ?: "未分类"

/** 分类下拉选择器（自绘，miuix 0.9.3 无公开 DropdownMenu 弹层组件） */
@Composable
fun CategorySelector(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var selectorHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { selectorHeight = it.height },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MiuixTheme.colorScheme.secondaryContainer)
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = categoryLabel(categories, selectedCategoryId),
                fontSize = 15.sp,
                color = MiuixTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            DropdownArrowEndAction(actionColor = MiuixTheme.colorScheme.onSecondaryContainer)
        }
        if (expanded) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, selectorHeight + with(density) { 4.dp.roundToPx() }),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                CategoryPanel(
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    onSelect = { id ->
                        onSelect(id)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** AI 功能区按钮：标题已填答案为空时显示「生成答案」，答案非空时显示「优化表述/优化格式」 */
@Composable
fun AiActionButtons(
    titleIsBlank: Boolean,
    answerIsBlank: Boolean,
    aiConfigured: Boolean,
    aiBusy: Boolean,
    onGenerate: () -> Unit,
    onOptimize: () -> Unit,
    onFormat: () -> Unit,
) {
    val context = LocalContext.current
    val notConfiguredTip: () -> Unit = {
        Toast.makeText(context, "请先在设置中配置 AI 接口", Toast.LENGTH_SHORT).show()
    }

    if (!titleIsBlank && answerIsBlank) {
        Button(
            onClick = {
                if (!aiConfigured) notConfiguredTip() else onGenerate()
            },
            enabled = aiConfigured && !aiBusy,
            colors = ButtonDefaults.buttonColorsPrimary(),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
        ) {
            Text(
                text = if (aiBusy) "AI 处理中…" else "AI 生成答案",
                fontSize = 14.sp,
            )
        }
    } else if (!answerIsBlank) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (!aiConfigured) notConfiguredTip() else onOptimize()
                },
                enabled = aiConfigured && !aiBusy,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = if (aiBusy) "AI 处理中…" else "AI 优化表述",
                    fontSize = 14.sp,
                )
            }
            Button(
                onClick = {
                    if (!aiConfigured) notConfiguredTip() else onFormat()
                },
                enabled = aiConfigured && !aiBusy,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = if (aiBusy) "AI 处理中…" else "AI 优化格式",
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun CategoryPanel(
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
