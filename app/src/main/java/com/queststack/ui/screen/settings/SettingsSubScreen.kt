package com.queststack.ui.screen.settings

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.queststack.ai.PRESETS
import com.queststack.ui.component.ConfirmDialog
import com.queststack.ui.component.InputDialog
import top.yukonga.miuix.kmp.basic.Slider
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
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    // 分类管理页的"添加分类"对话框由顶栏右上角按钮触发
    var categoryAddOpen by remember { mutableStateOf(false) }

    // 统一页内 Snackbar 提示（保存结果 / 错误）
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                actions = {
                    if (route == SettingsSubRoute.Category) {
                        IconButton(onClick = { categoryAddOpen = true }) {
                            Icon(
                                imageVector = MiuixIcons.Add,
                                contentDescription = "添加分类",
                                tint = MiuixTheme.colorScheme.onBackground,
                                modifier = Modifier.size(22.dp),
                            )
                        }
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
                        SettingsSubRoute.Ai -> AiContent(uiState, viewModel, showMessage)
                        SettingsSubRoute.Category -> CategoryContent(
                            uiState = uiState,
                            viewModel = viewModel,
                            addOpen = categoryAddOpen,
                            onAddDismiss = { categoryAddOpen = false },
                        )
                        SettingsSubRoute.Backup -> BackupContent(uiState, viewModel, showMessage)
                        SettingsSubRoute.About -> AboutContent()
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
        SnackbarHost(
            state = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun AppearanceContent(viewModel: SettingsViewModel) {
    SettingsCard(insideMargin = PaddingValues(0.dp)) {
        val options = listOf("跟随系统", "浅色", "深色")
        val selectedIndex = when (AppSettings.themeMode) {
            ThemeMode.System -> 0
            ThemeMode.Light -> 1
            ThemeMode.Dark -> 2
        }
        WindowDropdownPreference(
            items = options,
            selectedIndex = selectedIndex,
            title = "主题模式",
            startAction = {
                Icon(
                    imageVector = MiuixIcons.Theme,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                    tint = MiuixTheme.colorScheme.onBackground,
                )
            },
            onSelectedIndexChange = { index ->
                viewModel.setThemeMode(
                    when (index) {
                        0 -> ThemeMode.System
                        1 -> ThemeMode.Light
                        else -> ThemeMode.Dark
                    },
                )
            },
        )
    }
}

@Composable
private fun AiContent(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    showMessage: (String) -> Unit,
) {
    val baseUrlState = remember { TextFieldState() }
    val apiKeyState = remember { TextFieldState() }
    val modelState = remember { TextFieldState() }
    var presetId by remember { mutableStateOf(uiState.aiConfig.presetId.ifBlank { "custom" }) }
    var temperature by remember { mutableStateOf(0.7f) }
    var timeoutSeconds by remember { mutableStateOf(30) }

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

    SettingsCard(insideMargin = PaddingValues(0.dp)) {
        val presetNames = buildList {
            add("自定义配置")
            PRESETS.forEach { add(it.name) }
        }
        val selectedIndex = when (val id = presetId) {
            "custom" -> 0
            else -> PRESETS.indexOfFirst { it.id == id }.let { if (it >= 0) it + 1 else 0 }
        }
        // 1. 供应商预设
        WindowDropdownPreference(
            items = presetNames,
            selectedIndex = selectedIndex,
            title = "预设供应商",
            onSelectedIndexChange = { index ->
                if (index == 0) {
                    presetId = "custom"
                } else {
                    val preset = PRESETS[index - 1]
                    presetId = preset.id
                    if (preset.defaultBaseUrl.isNotBlank()) {
                        baseUrlState.edit { replace(0, length, preset.defaultBaseUrl) }
                    }
                }
            },
        )
        HorizontalDivider()

        // 2. 连接配置
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
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
        HorizontalDivider()

        // 3. 模型
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
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
        HorizontalDivider()

        // 4. 参数
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
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
        HorizontalDivider()

        // 5. 操作
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = {
                    val config = buildConfig()
                    when {
                        config.baseUrl.isEmpty() ->
                            showMessage("Base URL 不能为空")
                        config.model.isEmpty() ->
                            showMessage("模型不能为空")
                        else -> {
                            viewModel.saveAiConfig(config)
                            showMessage("已保存")
                        }
                    }
                },
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier.fillMaxWidth().height(44.dp),
            ) {
                Text(text = "保存配置", fontSize = 14.sp)
            }
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
                Text(text = it, fontSize = 12.sp, color = MiuixTheme.colorScheme.onBackgroundVariant)
            }
            Text(
                text = "AI 接口可选；未配置时 AI 功能不可用，本地功能不受影响。先填 API Key 再「获取模型列表」选择模型。",
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.8f),
            )
        }
    }

    if (modelPickerOpen) {
        ModelPickerDialog(
            modelList = uiState.modelList,
            modelState = modelState,
            modelMessage = uiState.modelMessage,
            onMessageShown = viewModel::consumeModelMessage,
            onFetch = { viewModel.fetchModels(buildConfig(), inDialog = true) },
            onDismiss = { modelPickerOpen = false },
        )
    }
}

/** 模型选择器对话框：展示实时拉取的模型列表 + 手动输入兜底（Miuix WindowDialog）。
 *  获取模型的提示（modelMessage）在对话框内展示，避免被窗口级对话框遮挡页面级 Snackbar。 */
@Composable
private fun ModelPickerDialog(
    modelList: List<String>?,
    modelState: TextFieldState,
    modelMessage: String?,
    onMessageShown: () -> Unit,
    onFetch: () -> Unit,
    onDismiss: () -> Unit,
) {
    // 对话框内暂存提示文案：modelMessage 变化时显示并立即消费（消费后 uiState 清空，本地保留显示）
    var shownMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(modelMessage) {
        modelMessage?.let {
            shownMessage = it
            onMessageShown()
        }
    }

    WindowDialog(
        show = true,
        title = "选择模型",
        onDismissRequest = onDismiss,
    ) {
        Column {
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
                            HorizontalDivider()
                        }
                    }
                }
            }
            shownMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
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
            Row {
                TextButton(
                    text = "获取模型列表",
                    onClick = onFetch,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = "完成",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}

@Composable
private fun CategoryContent(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    addOpen: Boolean,
    onAddDismiss: () -> Unit,
) {
    var renameTarget by remember { mutableStateOf<Category?>(null) }
    var deleteTarget by remember { mutableStateOf<Category?>(null) }

    // 每个分类单独一张卡片，间距 12dp；添加分类入口在顶栏右上角
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (uiState.categories.isEmpty()) {
            SettingsCard {
                Text(
                    text = "暂无分类，点右上角「+」添加",
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
            }
        } else {
            uiState.categories.forEach { category ->
                SettingsCard {
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
                }
            }
        }
    }

    if (addOpen) {
        InputDialog(
            title = "添加分类",
            label = "分类名称",
            confirmLabel = "添加",
            onConfirm = { name ->
                if (name.isNotBlank()) viewModel.addCategory(name)
                onAddDismiss()
            },
            onDismiss = onAddDismiss,
        )
    }
    renameTarget?.let { target ->
        InputDialog(
            title = "重命名分类",
            label = "分类名称",
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
        ConfirmDialog(
            title = "删除分类",
            message = "确定要删除分类「${target.name}」吗？该操作无法恢复。",
            confirmLabel = "删除",
            destructive = true,
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
    showMessage: (String) -> Unit,
) {
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

    // 本地备份与 WebDAV 备份分两张卡片
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsCard {
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
            }
        }
        SettingsCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        showMessage("已保存")
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
}

@Composable
private fun AboutContent() {
    SettingsCard() {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "版本 0.2.0",
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

/** 设置分组卡片（对齐 KernelSU：无组标题文字，纯 Card 分组）。
 *  @param insideMargin 卡片内边距；preference 组件卡传 PaddingValues(0.dp) 让选项行铺满卡片，
 *  按压覆盖（MiuixIndication 矩形）视觉上与 KernelSU 一致（整卡变暗）；内容卡保留默认边距。 */
@Composable
private fun SettingsCard(
    insideMargin: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        insideMargin = insideMargin,
    ) {
        content()
    }
}
