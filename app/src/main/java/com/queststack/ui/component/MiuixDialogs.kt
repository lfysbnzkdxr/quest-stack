package com.queststack.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 确认对话框（Miuix WindowDialog 封装，对齐 KernelSU 对话框规范）：
 * 按钮行「取消 / 确认」对半 + 确认钮 textButtonColorsPrimary；destructive 时确认用 error 色。
 * 由调用方以条件组合控制显隐（如 deleteTarget != null）。
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "确定",
    destructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    WindowDialog(
        show = true,
        title = title,
        onDismissRequest = onDismiss,
    ) {
        Column {
            Text(
                text = message,
                fontSize = 14.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row {
                TextButton(
                    text = "取消",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(20.dp))
                TextButton(
                    text = confirmLabel,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors = if (destructive) {
                        ButtonDefaults.textButtonColorsPrimary(
                            color = MiuixTheme.colorScheme.error,
                            textColor = MiuixTheme.colorScheme.onError,
                        )
                    } else {
                        ButtonDefaults.textButtonColorsPrimary()
                    },
                )
            }
        }
    }
}

/** 文本输入对话框（WindowDialog 封装，供分类添加/重命名等单输入场景使用） */
@Composable
fun InputDialog(
    title: String,
    label: String,
    initial: String = "",
    confirmLabel: String = "确定",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = remember(initial) { TextFieldState(initial) }
    WindowDialog(
        show = true,
        title = title,
        onDismissRequest = onDismiss,
    ) {
        Column {
            TextField(
                state = state,
                modifier = Modifier.fillMaxWidth(),
                label = label,
                useLabelAsPlaceholder = true,
                lineLimits = TextFieldLineLimits.SingleLine,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row {
                TextButton(
                    text = "取消",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(20.dp))
                TextButton(
                    text = confirmLabel,
                    onClick = { onConfirm(state.text.toString().trim()) },
                    enabled = state.text.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}