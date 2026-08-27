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
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.queststack.ai.PRESETS
import com.queststack.ai.ModelPreset
import top.yukonga.miuix.kmp.basic.Slider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.queststack.data.backup.WebDavConfig
import com.queststack.data.db.Category
import com.queststack.data.repository.AiConfig
import com.queststack.ui.component.PageScaffold
import com.queststack.ui.component.FilterDropdownButton
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
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.window.WindowCascadingListPopup
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.HorizontalDivider
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
    var presetId by remember { mutableStateOf(uiState.aiConfig.presetId.ifBlank { "custom" }) }
    var temperature by remember { mutableStateOf(0.7f) }
    var timeoutSeconds by remember { mutableStateOf(30) }

    var providerPickerOpen by remember { mutableStateOf(false) }
    var modelPickerOpen by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.aiConfig) {
        baseUrlState.edit { replace(0, length, uiState.aiConfig.baseUrl) }
        apiKeyState.edit { replace(0, length, uiState.aiConfig.apiKey) }
        modelState.edit { replace(0, length, uiState.aiConfig.model) }
        presetId = uiState.aiConfig.presetId.ifBlank { "custom" }
        temperature = uiState.aiConfig.temperature
        timeoutSeconds = uiState.aiConfig.timeoutSeconds
    }

    fun buildConfig() = AiConfig(
        baseUrl = baseUrlState.text.toString().trim(),
        apiKey = apiKeyState.text.toString().trim(),
        model = modelState.text.toString().trim(),
        presetId = presetId,
        temperature = temperature,
        timeoutSeconds = timeoutSeconds,
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        // 1. 供应商预设
        SettingsSectionCard("预设供应商") {
            val presetName = when (presetId) {
                "custom" -> "自定义配置"
                else -> PRESETS.firstOrNull { it.id == presetId }?.name ?: "自定义配置"
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                FilterDropdownButton(
                    label = presetName,
                    expanded = providerPickerOpen,
                    onClick = { providerPickerOpen = true },
                )
                WindowCascadingListPopup(
                    show = providerPickerOpen,
                    entries = listOf(
                        DropdownEntry(
                            items = buildList {
                                add(
                                    DropdownItem(
                                        text = "自定义配置",
                                        selected = presetId == "custom",
                                        onClick = { presetId = "custom"; providerPickerOpen = false },
                                    ),
                                )
                                PRESETS.forEach { p ->
                                    add(
                                        DropdownItem(
                                            text = p.name,
                                            selected = presetId == p.id,
                                            onClick = {
                                                presetId = p.id
                                                if (p.defaultBaseUrl.isNotBlank()) {
                                                    baseUrlState.edit { replace(0, length, p.defaultBaseUrl) }
                                                }
                                                providerPickerOpen = false
                                            },
                                        ),
                                    )
                                }
                            },
                        ),
                    ),
                    onDismissRequest = { providerPickerOpen = false },
                    enableWindowDim = false,
                )
            }
        }

        // 2. 连接配置
        SettingsSectionCard("连接配置") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextField(
                    state = baseUrlState,
                    modifier = Modifier.fillMaxWidth(),
                    label = "Base URL",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("超时（秒）", fontSize = 14.sp, color = MiuixTheme.colorScheme.onBackground)
                    Spacer(Modifier.weight(1f))
                    Text(timeoutSeconds.toString(), fontSize = 14.sp, color = MiuixTheme.colorScheme.onBackgroundVariant)
                }
                Slider(
                    value = timeoutSeconds.toFloat(),
                    onValueChange = { timeoutSeconds = it.roundToInt() },
                    modifier = Modifier.fillMaxWidth(),
                    valueRange = 5f..120f,
                    steps = 115,
                )
            }
        }

        // 3. 模型
        SettingsSectionCard("模型") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { viewModel.fetchModels(buildConfig()) },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                ) {
                    Text(text = "获取模型列表", fontSize = 14.sp)
                }
                Button(
                    onClick = { modelPickerOpen = true },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                ) {
                    Text(
                        text = if (modelState.text.isBlank()) "选择模型" else "模型：${modelState.text}",
                        fontSize = 14.sp,
                    )
                }
                Text(
                    text = "当前模型：${if (modelState.text.isBlank()) "未选择" else modelState.text}",
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
            }
        }

        // 4. 参数
        SettingsSectionCard("参数") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("温度", fontSize = 14.sp, color = MiuixTheme.colorScheme.onBackground)
                    Spacer(Modifier.weight(1f))
                    Text("%.1f".format(temperature), fontSize = 14.sp, color = MiuixTheme.colorScheme.onBackgroundVariant)
                }
                Slider(
                    value = temperature,
                    onValueChange = { temperature = it },
                    modifier = Modifier.fillMaxWidth(),
                    valueRange = 0f..2f,
                    steps = 20,
                )
                Text(
                    text = "档位参考：0.2 严谨 · 0.7 平衡 · 1.2 发散（范围 0–2）",
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
            }
        }

        // 操作
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = {
                val config = buildConfig()
                when {
                    config.baseUrl.isEmpty() ->
                        Toast.makeText(context, "Base URL 不能为空", Toast.LENGTH_SHORT).show()
                    config.model.isEmpty() ->
                        Toast.makeText(context, "模型不能为空", Toast.LENGTH_SHORT).show()
                    else -> {
                        viewModel.saveAiConfig(config)
                        Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            colors = ButtonDefaults.buttonColorsPrimary(),
            modifier = Modifier.fillMaxWidth().height(44.dp),
        ) {
            Text(text = "保存配置", fontSize = 14.sp)
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { viewModel.testAiConnection(buildConfig()) },
            enabled = !uiState.testBusy,
            colors = ButtonDefaults.buttonColorsPrimary(),
            modifier = Modifier.fillMaxWidth().height(44.dp),
        ) {
            Text(
                text = if (uiState.testBusy) "连接测试中…" else "测试连接",
                fontSize = 14.sp,
            )
        }
        uiState.testMessage?.let {
            Spacer(Modifier.height(6.dp))
            Text(text = it, fontSize = 12.sp, color = MiuixTheme.colorScheme.onBackgroundVariant)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "AI 接口可选；未配置时 AI 功能不可用，本地功能不受影响。先填 API Key 再「获取模型列表」选择模型。",
            fontSize = 12.sp,
            color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.8f),
        )
        Spacer(Modifier.height(20.dp))
    }

    if (modelPickerOpen) {
        ModelPickerDialog(
            modelList = uiState.modelList,
            modelState = modelState,
            onFetch = { viewModel.fetchModels(buildConfig()) },
            onDismiss = { modelPickerOpen = false },
        )
    }
}

/** 模型选择器对话框：展示实时拉取的模型列表 + 手动输入兜底 */
@Composable
private fun ModelPickerDialog(
    modelList: List<String>?,
    modelState: TextFieldState,
    onFetch: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("选择模型", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                when {
                    modelList == null ->
                        Text("尚未获取模型列表，请先点「获取模型列表」，或手动输入", fontSize = 12.sp, color = MiuixTheme.colorScheme.onBackgroundVariant)
                    modelList.isEmpty() ->
                        Text("未获取到模型，请手动输入", fontSize = 12.sp, color = MiuixTheme.colorScheme.onBackgroundVariant)
                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            modelList.forEach { m ->
                                val selected = modelState.text.toString() == m
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            modelState.edit { replace(0, length, m) }
                                            onDismiss()
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = m,
                                        fontSize = 14.sp,
                                        color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onBackground,
                                    )
                                }
                                Box(Modifier.fillMaxWidth().height(1.dp).background(MiuixTheme.colorScheme.dividerLine))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("手动输入", fontSize = 13.sp, color = MiuixTheme.colorScheme.onBackgroundVariant)
                TextField(
                    state = modelState,
                    modifier = Modifier.fillMaxWidth(),
                    label = "模型名称",
                    useLabelAsPlaceholder = true,
                    lineLimits = TextFieldLineLimits.SingleLine,
                )
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(text = "获取模型列表", onClick = onFetch)
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColorsPrimary()) {
                        Text("完成")
                    }
                }
            }
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
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
        if (uiState.categories.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
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
            HorizontalDivider()
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
            HorizontalDivider()
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
        SmallTitle(text = title, insideMargin = PaddingValues(0.dp, 8.dp))
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
