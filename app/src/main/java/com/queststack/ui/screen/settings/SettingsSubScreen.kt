package com.queststack.ui.screen.settings

import android.app.Application
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.queststack.data.backup.WebDavConfig
import com.queststack.data.db.Category
import com.queststack.data.repository.AiConfig
import com.queststack.ui.component.PageScaffold
import com.queststack.ui.theme.AppSettings
import com.queststack.ui.theme.ThemeMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 本地导出文件名：quest-stack-backup-YYYYMMDD.json */
private fun backupFileName(): String =
    "quest-stack-backup-${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}.json"

/** 设置二级页路由（参考 KernelSU 的 Destination 枚举） */
sealed class SettingsSubRoute(val title: String) {
    object Appearance : SettingsSubRoute("外观")
    object Ai : SettingsSubRoute("AI 接口设置")
    object Category : SettingsSubRoute("分类管理")
    object Backup : SettingsSubRoute("数据备份")
    object About : SettingsSubRoute("关于")
}

/**
 * 设置二级页容器：全屏覆盖，复用 PageScaffold（毛玻璃顶栏 + 返回）+ BackHandler。
 * 与设置主屏共享同一 SettingsViewModel 实例（Activity 作用域、同 key，自动复用）。
 */
@Composable
fun SettingsSubScreen(
    route: SettingsSubRoute,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application,
        ),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 统一 Toast 提示（保存结果 / 错误）
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    BackHandler { onBack() }

    Column(modifier = Modifier.fillMaxSize()) {
        PageScaffold(
            title = route.title,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = MiuixIcons.ChevronBackward,
                        contentDescription = "返回",
                        tint = MiuixTheme.colorScheme.onBackground,
                    )
                }
            },
        ) { scrollBehavior ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                when (route) {
                    SettingsSubRoute.Appearance -> AppearanceContent(viewModel)
                    SettingsSubRoute.Ai -> AiContent(uiState, viewModel)
                    SettingsSubRoute.Category -> CategoryContent(uiState, viewModel)
                    SettingsSubRoute.Backup -> BackupContent(uiState, viewModel)
                    SettingsSubRoute.About -> AboutContent()
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun AppearanceContent(viewModel: SettingsViewModel) {
    SettingsSectionCard("外观") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppThemeChip("跟随系统", ThemeMode.System, AppSettings.themeMode) {
                viewModel.setThemeMode(ThemeMode.System)
            }
            AppThemeChip("浅色", ThemeMode.Light, AppSettings.themeMode) {
                viewModel.setThemeMode(ThemeMode.Light)
            }
            AppThemeChip("深色", ThemeMode.Dark, AppSettings.themeMode) {
                viewModel.setThemeMode(ThemeMode.Dark)
            }
        }
    }
}

@Composable
private fun AiContent(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
) {
    val context = LocalContext.current
    val baseUrlState = remember { TextFieldState() }
    val apiKeyState = remember { TextFieldState() }
    val modelState = remember { TextFieldState() }
    val timeoutState = remember { TextFieldState() }
    LaunchedEffect(uiState.aiConfig) {
        baseUrlState.edit { replace(0, length, uiState.aiConfig.baseUrl) }
        apiKeyState.edit { replace(0, length, uiState.aiConfig.apiKey) }
        modelState.edit { replace(0, length, uiState.aiConfig.model) }
        timeoutState.edit { replace(0, length, uiState.aiConfig.timeoutSeconds.toString()) }
    }

    SettingsSectionCard("AI 接口") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextField(
                state = baseUrlState,
                modifier = Modifier.fillMaxWidth(),
                label = "Base URL（示例 https://api.openai.com/v1）",
                useLabelAsPlaceholder = true,
                lineLimits = TextFieldLineLimits.SingleLine,
            )
            TextField(
                state = apiKeyState,
                modifier = Modifier.fillMaxWidth(),
                label = "API Key",
                useLabelAsPlaceholder = true,
                lineLimits = TextFieldLineLimits.SingleLine,
            )
            TextField(
                state = modelState,
                modifier = Modifier.fillMaxWidth(),
                label = "模型（示例 gpt-4o）",
                useLabelAsPlaceholder = true,
                lineLimits = TextFieldLineLimits.SingleLine,
            )
            TextField(
                state = timeoutState,
                modifier = Modifier.fillMaxWidth(),
                label = "超时秒数（示例 30）",
                useLabelAsPlaceholder = true,
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Button(
                onClick = {
                    val baseUrl = baseUrlState.text.toString().trim()
                    val model = modelState.text.toString().trim()
                    val apiKey = apiKeyState.text.toString().trim()
                    val timeout = timeoutState.text.toString().trim().toIntOrNull() ?: 30
                    when {
                        baseUrl.isEmpty() ->
                            Toast.makeText(context, "Base URL 不能为空", Toast.LENGTH_SHORT).show()
                        model.isEmpty() ->
                            Toast.makeText(context, "模型不能为空", Toast.LENGTH_SHORT).show()
                        else -> {
                            viewModel.saveAiConfig(
                                AiConfig(
                                    baseUrl = baseUrl,
                                    apiKey = apiKey,
                                    model = model,
                                    timeoutSeconds = timeout,
                                ),
                            )
                            Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
            ) {
                Text(text = "保存配置", fontSize = 14.sp)
            }
            Text(
                text = "AI 接口可选；未配置时 AI 功能不可用，本地功能不受影响",
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun CategoryContent(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
) {
    var addDialogOpen by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Category?>(null) }
    var deleteTarget by remember { mutableStateOf<Category?>(null) }

    SettingsSectionCard("分类管理") {
        if (uiState.categories.isEmpty()) {
            Text(
                text = "暂无分类",
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        uiState.categories.forEachIndexed { index, category ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = category.name,
                    fontSize = 15.sp,
                    color = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = { renameTarget = category }) {
                    Icon(
                        imageVector = MiuixIcons.Edit,
                        contentDescription = "重命名",
                        tint = MiuixTheme.colorScheme.onBackgroundVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(onClick = { deleteTarget = category }) {
                    Icon(
                        imageVector = MiuixIcons.Delete,
                        contentDescription = "删除",
                        tint = MiuixTheme.colorScheme.onBackgroundVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            if (index != uiState.categories.lastIndex) {
                Spacer(modifier = Modifier.height(10.dp))
                DividerLine()
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
        if (uiState.categories.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            DividerLine()
            Spacer(modifier = Modifier.height(4.dp))
        }
        TextButton(
            text = "+ 添加分类",
            onClick = { addDialogOpen = true },
            modifier = Modifier.padding(top = 6.dp),
        )
    }

    if (addDialogOpen) {
        CategoryInputDialog(
            title = "添加分类",
            initial = "",
            confirmLabel = "添加",
            onConfirm = { name ->
                if (name.isNotBlank()) viewModel.addCategory(name)
                addDialogOpen = false
            },
            onDismiss = { addDialogOpen = false },
        )
    }
    renameTarget?.let { target ->
        CategoryInputDialog(
            title = "重命名分类",
            initial = target.name,
            confirmLabel = "保存",
            onConfirm = { name ->
                if (name.isNotBlank()) viewModel.renameCategory(target, name)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }
    deleteTarget?.let { target ->
        CategoryDeleteDialog(
            category = target,
            onConfirm = {
                viewModel.deleteCategory(target)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun BackupContent(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
) {
    val context = LocalContext.current
    val webDavUrlState = remember { TextFieldState() }
    val webDavUserState = remember { TextFieldState() }
    val webDavPassState = remember { TextFieldState() }
    LaunchedEffect(uiState.webDavConfig) {
        webDavUrlState.edit { replace(0, length, uiState.webDavConfig.url) }
        webDavUserState.edit { replace(0, length, uiState.webDavConfig.username) }
        webDavPassState.edit { replace(0, length, uiState.webDavConfig.password) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(viewModel::exportLocal) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::importLocal) }

    SettingsSectionCard("数据备份") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "本地",
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { exportLauncher.launch(backupFileName()) },
                    enabled = !uiState.busy,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = if (uiState.busy && uiState.busyAction == "local_export") "处理中…"
                        else "导出到本地",
                        fontSize = 13.sp,
                    )
                }
                Button(
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                    enabled = !uiState.busy,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = if (uiState.busy && uiState.busyAction == "local_import") "处理中…"
                        else "从本地导入",
                        fontSize = 13.sp,
                    )
                }
            }
            DividerLine()
            Text(
                text = "WebDAV",
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )
            TextField(
                state = webDavUrlState,
                modifier = Modifier.fillMaxWidth(),
                label = "WebDAV 地址",
                useLabelAsPlaceholder = true,
                lineLimits = TextFieldLineLimits.SingleLine,
            )
            TextField(
                state = webDavUserState,
                modifier = Modifier.fillMaxWidth(),
                label = "用户名",
                useLabelAsPlaceholder = true,
                lineLimits = TextFieldLineLimits.SingleLine,
            )
            TextField(
                state = webDavPassState,
                modifier = Modifier.fillMaxWidth(),
                label = "密码",
                useLabelAsPlaceholder = true,
                lineLimits = TextFieldLineLimits.SingleLine,
            )
            Button(
                onClick = {
                    viewModel.saveWebDavConfig(
                        WebDavConfig(
                            url = webDavUrlState.text.toString().trim(),
                            username = webDavUserState.text.toString().trim(),
                            password = webDavPassState.text.toString().trim(),
                        ),
                    )
                    Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
            ) {
                Text(text = "保存配置", fontSize = 14.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { viewModel.webDavBackup() },
                    enabled = !uiState.busy,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = if (uiState.busy && uiState.busyAction == "webdav_backup") "处理中…"
                        else "备份到 WebDAV",
                        fontSize = 13.sp,
                    )
                }
                Button(
                    onClick = { viewModel.webDavRestore() },
                    enabled = !uiState.busy,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = if (uiState.busy && uiState.busyAction == "webdav_restore") "处理中…"
                        else "从 WebDAV 恢复",
                        fontSize = 13.sp,
                    )
                }
            }
            Text(
                text = "URL 为完整文件地址；备份/恢复固定使用文件名 quest-stack-backup.json，父目录不存在时会自动创建",
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun AboutContent() {
    SettingsSectionCard("关于") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "版本 0.1.0",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onBackground,
            )
            Text(
                text = "题栈 —— 本地面试练习助手。数据存储于本地，AI 功能可选。",
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            DividerLine()
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "UI 基于 Miuix (top.yukonga.miuix.kmp) · 图标来自 Miuix Icons",
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.7f),
            )
        }
    }
}

/** 设置卡片：小标题 + 圆角卡片内容 */
@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Text(
            text = title,
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        ) {
            content()
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

/** 细分割线 */
@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.75.dp)
            .background(MiuixTheme.colorScheme.dividerLine),
    )
}

/** 外观主题三选一 chip：当前主题高亮 */
@Composable
private fun AppThemeChip(label: String, mode: ThemeMode, current: ThemeMode, onClick: () -> Unit) {
    val selected = mode == current
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

/** 分类 添加/重命名 对话框（自绘，miuix 无 AlertDialog） */
@Composable
private fun CategoryInputDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = remember(initial) { TextFieldState(initial) }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp,
            insideMargin = PaddingValues(horizontal = 24.dp, vertical = 22.dp),
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextField(
                    state = state,
                    modifier = Modifier.fillMaxWidth(),
                    label = "分类名称",
                    useLabelAsPlaceholder = true,
                    lineLimits = TextFieldLineLimits.SingleLine,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(text = "取消", onClick = onDismiss)
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = { onConfirm(state.text.toString().trim()) },
                        enabled = state.text.isNotBlank(),
                        colors = ButtonDefaults.buttonColorsPrimary(),
                    ) {
                        Text(text = confirmLabel, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

/** 分类删除确认对话框（自绘） */
@Composable
private fun CategoryDeleteDialog(
    category: Category,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp,
            insideMargin = PaddingValues(horizontal = 24.dp, vertical = 22.dp),
        ) {
            Column {
                Text(
                    text = "删除分类",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "确定要删除分类「${category.name}」吗？该操作无法恢复。",
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(text = "取消", onClick = onDismiss)
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColorsPrimary(
                            color = MiuixTheme.colorScheme.error,
                            contentColor = MiuixTheme.colorScheme.onError,
                        ),
                    ) {
                        Text(text = "删除", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
