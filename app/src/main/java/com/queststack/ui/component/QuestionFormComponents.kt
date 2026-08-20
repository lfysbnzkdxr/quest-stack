package com.queststack.ui.component

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.queststack.data.db.Category
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 内容区块：小标题 + 可选 Card 容器，对齐设置页 SettingsSectionCard 风格
 *  framed=true（默认）用于只读展示（如详情查看态的 Text）；
 *  framed=false 仅渲染小标题 + 内容，用于自带边框的输入框（避免与 TextField 边框叠加成双层） */
@Composable
fun ContentBlock(
    title: String,
    modifier: Modifier = Modifier,
    framed: Boolean = true,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        if (framed) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            ) {
                content()
            }
        } else {
            content()
        }
    }
}

/** 分类选择（收缩式下拉，样式与题库筛选栏一致；选项：未分类 + 各分类）
 *  modifier 默认全宽；与难度并排一行时传 weight(1f) */
@Composable
fun CategorySelector(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    var expanded by remember { mutableStateOf(false) }
    var selectorHeight by remember { mutableIntStateOf(0) }
    var selectorWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val currentName = categories.firstOrNull { it.id == selectedCategoryId }?.name

    Box(
        modifier = modifier.onSizeChanged {
            selectorHeight = it.height
            selectorWidth = it.width
        },
    ) {
        FilterDropdownButton(
            label = "分类：${currentName ?: "未分类"}",
            expanded = expanded,
            onClick = { expanded = true },
        )
        if (expanded) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, selectorHeight + with(density) { 4.dp.roundToPx() }),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                // 面板宽度与按钮对齐（= 各自半边），不越界
                Card(
                    modifier = Modifier
                        .width(with(density) { selectorWidth.toDp() })
                        .dropShadow(
                            shape = RoundedCornerShape(16.dp),
                            shadow = Shadow(radius = 12.dp, color = Color.Black, alpha = 0.15f),
                        ),
                    cornerRadius = 16.dp,
                    insideMargin = PaddingValues(0.dp),
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        categories.forEach { category ->
                            DropdownRow(
                                text = category.name,
                                isSelected = category.id == selectedCategoryId,
                                onClick = {
                                    onSelect(category.id)
                                    expanded = false
                                },
                            )
                        }
                        DropdownRow("未分类", isSelected = selectedCategoryId == null, onClick = {
                            onSelect(null)
                            expanded = false
                        })
                    }
                }
            }
        }
    }
}

/** 难度选择（收缩式下拉，样式与题库筛选栏一致；选项：简单/中等/困难） */
@Composable
fun DifficultySelector(
    difficulty: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    var expanded by remember { mutableStateOf(false) }
    var selectorHeight by remember { mutableIntStateOf(0) }
    var selectorWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val difficultyName = when (difficulty) {
        1 -> "简单"
        2 -> "中等"
        3 -> "困难"
        else -> "简单"
    }

    Box(
        modifier = modifier.onSizeChanged {
            selectorHeight = it.height
            selectorWidth = it.width
        },
    ) {
        FilterDropdownButton(
            label = "难度：$difficultyName",
            expanded = expanded,
            onClick = { expanded = true },
        )
        if (expanded) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, selectorHeight + with(density) { 4.dp.roundToPx() }),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                // 面板宽度与按钮对齐（= 各自半边），不越界
                Card(
                    modifier = Modifier
                        .width(with(density) { selectorWidth.toDp() })
                        .dropShadow(
                            shape = RoundedCornerShape(16.dp),
                            shadow = Shadow(radius = 12.dp, color = Color.Black, alpha = 0.15f),
                        ),
                    cornerRadius = 16.dp,
                    insideMargin = PaddingValues(0.dp),
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        DropdownRow("简单", isSelected = difficulty == 1, onClick = {
                            onSelect(1)
                            expanded = false
                        })
                        DropdownRow("中等", isSelected = difficulty == 2, onClick = {
                            onSelect(2)
                            expanded = false
                        })
                        DropdownRow("困难", isSelected = difficulty == 3, onClick = {
                            onSelect(3)
                            expanded = false
                        })
                    }
                }
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
