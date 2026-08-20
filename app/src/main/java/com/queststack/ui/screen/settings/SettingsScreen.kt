package com.queststack.ui.screen.settings

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.queststack.ui.component.PageScaffold
import com.queststack.ui.theme.AppSettings
import com.queststack.ui.theme.ThemeMode
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Backup
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 设置主屏：KernelSU 风格的纯下钻列表（SuperArrow 行）。
 * 5 项全部进入二级全屏页（SettingsSubScreen，由 MainScreen 以 overlay 承载）。
 */
@Composable
fun SettingsScreen(
    onNavigateSub: (SettingsSubRoute) -> Unit = {},
    viewModel: SettingsViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application,
        ),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        PageScaffold(title = "设置") { scrollBehavior ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    insideMargin = PaddingValues(0.dp),
                ) {
                    Column {
                        SettingsRow(
                            icon = MiuixIcons.Theme,
                            title = "外观",
                            summary = when (AppSettings.themeMode) {
                                ThemeMode.System -> "跟随系统"
                                ThemeMode.Light -> "浅色"
                                ThemeMode.Dark -> "深色"
                            },
                            onClick = { onNavigateSub(SettingsSubRoute.Appearance) },
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = null,
                            title = "AI 接口设置",
                            summary = if (uiState.aiConfig.apiKey.isBlank()) "未配置" else "已配置",
                            onClick = { onNavigateSub(SettingsSubRoute.Ai) },
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = MiuixIcons.Folder,
                            title = "分类管理",
                            summary = "${uiState.categories.size} 个分类",
                            onClick = { onNavigateSub(SettingsSubRoute.Category) },
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = MiuixIcons.Backup,
                            title = "数据备份",
                            summary = "本地与 WebDAV",
                            onClick = { onNavigateSub(SettingsSubRoute.Backup) },
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = MiuixIcons.Info,
                            title = "关于",
                            summary = "版本 0.1.0",
                            onClick = { onNavigateSub(SettingsSubRoute.About) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector?,
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(24.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = MiuixTheme.colorScheme.onBackground,
            )
            if (summary.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = summary,
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
            }
        }
        Icon(
            imageVector = MiuixIcons.ChevronForward,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.75.dp)
            .background(MiuixTheme.colorScheme.dividerLine),
    )
}
