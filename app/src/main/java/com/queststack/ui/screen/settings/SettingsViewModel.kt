package com.queststack.ui.screen.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.queststack.data.DataContainer
import com.queststack.data.backup.BackupRepository
import com.queststack.data.backup.WebDavClient
import com.queststack.data.backup.WebDavConfig
import com.queststack.data.db.Category
import com.queststack.ai.ChatMessage
import com.queststack.data.repository.AiConfig
import com.queststack.data.repository.CategoryRepository
import com.queststack.data.repository.SettingsRepository
import com.queststack.ui.theme.AppSettings
import com.queststack.ui.theme.ThemeMode
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val aiConfig: AiConfig = AiConfig(),
    val webDavConfig: WebDavConfig = WebDavConfig(),
    val categories: List<Category> = emptyList(),
    /** 是否正在进行备份/恢复类操作（禁用相关按钮） */
    val busy: Boolean = false,
    /** 当前忙碌操作标识（用于按钮上显示"处理中…"） */
    val busyAction: String? = null,
    /** 统一提示文案（成功 / 错误），由 UI 弹出 Snackbar 后消费 */
    val message: String? = null,
    /** 模型获取提示文案：在模型选择对话框内展示（不走页面级 Snackbar，避免被窗口级对话框遮挡） */
    val modelMessage: String? = null,
    /** 测试连接是否进行中 */
    val testBusy: Boolean = false,
    /** 测试连接结果文案（成功/失败），由 UI 展示 */
    val testMessage: String? = null,
    /** 获取模型列表的结果（实时拉取），null 表示尚未获取或获取失败 */
    val modelList: List<String>? = null,
)

/**
 * 设置页 ViewModel。
 *
 * 备份/恢复类操作在 viewModelScope + Dispatchers.IO 中执行，
 * 结果与错误统一写入 [SettingsUiState.message]，由 UI 层 Snackbar 提示；
 * 分类删除时仓库抛出的 IllegalStateException（"分类下还有题目"）也在此捕获。
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository = DataContainer.settingsRepository,
    private val categoryRepository: CategoryRepository = DataContainer.categoryRepository,
    private val backupRepository: BackupRepository = DataContainer.backupRepository,
    private val webDavClient: WebDavClient = DataContainer.webDavClient,
    application: Application,
) : AndroidViewModel(application) {

    /**
     * 供 AndroidViewModelFactory 反射构造（其仅支持"单一 Application 参数"的构造函数）；
     * 其余依赖均使用 DataContainer 默认实现。
     */
    constructor(application: Application) : this(
        settingsRepository = DataContainer.settingsRepository,
        categoryRepository = DataContainer.categoryRepository,
        backupRepository = DataContainer.backupRepository,
        webDavClient = DataContainer.webDavClient,
        application = application,
    )

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.aiConfig,
                settingsRepository.webDavConfig,
                categoryRepository.observeCategories(),
            ) { aiConfig, webDavConfig, categories -> Triple(aiConfig, webDavConfig, categories) }
                .collect { (aiConfig, webDavConfig, categories) ->
                    _uiState.update {
                        it.copy(aiConfig = aiConfig, webDavConfig = webDavConfig, categories = categories)
                    }
                }
        }
    }

    // ------------------------------------------------------------------
    // 主题
    // ------------------------------------------------------------------

    /** 切换主题：立即生效（AppSettings 为全局 mutableStateOf），并持久化到 DataStore */
    fun setThemeMode(mode: ThemeMode) {
        if (mode == AppSettings.themeMode) return
        AppSettings.themeMode = mode
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    // ------------------------------------------------------------------
    // AI 配置 / WebDAV 配置
    // ------------------------------------------------------------------

    fun saveAiConfig(config: AiConfig) {
        viewModelScope.launch {
            settingsRepository.setAiConfig(config)
        }
    }

    /** 获取模型列表：调用 AiClient.listModels，结果写入 uiState.modelList（实时拉取，覆盖内嵌型号）。
     * 传入的是当前（可能尚未保存）的配置，便于用户改完 Base URL/Key 后直接测试。
     * @param inDialog true 时提示写入 modelMessage（模型选择对话框内展示，避免被窗口级对话框遮挡页面级 Snackbar）；
     *                 false 时提示写入 message（页面级 Snackbar）。两条通道互斥，避免残留旧提示。 */
    fun fetchModels(config: AiConfig, inDialog: Boolean = false) {
        fun notify(msg: String) {
            _uiState.update {
                if (inDialog) it.copy(modelMessage = msg, message = null)
                else it.copy(message = msg, modelMessage = null)
            }
        }
        if (config.baseUrl.isBlank() || config.apiKey.isBlank()) {
            notify("请先填写 Base URL 与 API Key")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val models = DataContainer.aiClient.listModels(config.baseUrl, config.apiKey, config.timeoutSeconds)
                notify(if (models.isEmpty()) "未获取到模型，请手动输入" else "已获取 ${models.size} 个模型")
                _uiState.update { it.copy(modelList = models) }
            } catch (e: Exception) {
                val msg = when (e) {
                    is IOException -> "获取失败：网络错误或接口不可用"
                    is TimeoutCancellationException -> "获取超时（${config.timeoutSeconds} 秒）"
                    else -> "获取失败：${e.message ?: "未知错误"}"
                }
                _uiState.update { it.copy(modelList = null) }
                notify(msg)
            }
        }
    }

    /** 测试连接：发一次最小 chat 请求，验证 chat 端点路径 + 选中模型可用 + 参数被接受。
     * 传入当前（可能尚未保存）的配置。 */
    fun testAiConnection(config: AiConfig) {
        if (config.baseUrl.isBlank() || config.model.isBlank()) {
            _uiState.update { it.copy(message = "请先填写 Base URL 与模型") }
            return
        }
        if (_uiState.value.testBusy) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(testBusy = true, testMessage = null) }
            val start = System.currentTimeMillis()
            try {
                DataContainer.aiClient.chat(
                    baseUrl = config.baseUrl,
                    apiKey = config.apiKey,
                    model = config.model,
                    messages = listOf(ChatMessage("user", "ping")),
                    temperature = config.temperature,
                    timeoutSeconds = minOf(config.timeoutSeconds, 15),
                )
                val cost = System.currentTimeMillis() - start
                _uiState.update { it.copy(testBusy = false, testMessage = "连接成功（耗时 ${cost}ms）") }
            } catch (e: Exception) {
                val msg = when (e) {
                    is IOException -> "连接失败：网络错误或接口不可用"
                    is TimeoutCancellationException -> "连接超时（${minOf(config.timeoutSeconds, 15)} 秒）"
                    is IllegalArgumentException -> "返回解析失败：${e.message}"
                    else -> "连接失败：${e.message ?: "未知错误"}"
                }
                _uiState.update { it.copy(testBusy = false, testMessage = msg) }
            }
        }
    }

    fun saveWebDavConfig(config: WebDavConfig) {
        viewModelScope.launch {
            settingsRepository.setWebDavConfig(config)
        }
    }

    // ------------------------------------------------------------------
    // 分类管理
    // ------------------------------------------------------------------

    fun addCategory(name: String) {
        viewModelScope.launch {
            categoryRepository.addCategory(name.trim())
        }
    }

    fun renameCategory(category: Category, newName: String) {
        viewModelScope.launch {
            categoryRepository.renameCategory(category, newName.trim())
        }
    }

    /** 删除分类；分类下还有题目时仓库抛 IllegalStateException，捕获后经 message 提示 */
    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            try {
                categoryRepository.deleteCategory(category)
            } catch (e: IllegalStateException) {
                _uiState.update { it.copy(message = e.message ?: "删除失败") }
            }
        }
    }

    // ------------------------------------------------------------------
    // 本地备份（SAF）
    // ------------------------------------------------------------------

    /** 导出到用户通过 SAF 选择的文件；成功提示字节数（KB） */
    fun exportLocal(uri: Uri) {
        if (_uiState.value.busy) return
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, busyAction = "local_export", message = null) }
            try {
                val bytes = withContext(Dispatchers.IO) { backupRepository.exportToJson().toByteArray() }
                val resolver = getApplication<Application>().contentResolver
                withContext(Dispatchers.IO) {
                    val output = resolver.openOutputStream(uri) ?: throw IOException("无法写入所选文件")
                    output.use { it.write(bytes) }
                }
                _uiState.update {
                    it.copy(busy = false, busyAction = null, message = "已导出（${bytes.size / 1024} KB）")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(busy = false, busyAction = null, message = "导出失败：${e.message ?: "未知错误"}")
                }
            }
        }
    }

    /** 从用户通过 SAF 选择的文件导入；成功提示新增题目数 */
    fun importLocal(uri: Uri) {
        if (_uiState.value.busy) return
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, busyAction = "local_import", message = null) }
            try {
                val resolver = getApplication<Application>().contentResolver
                val text = withContext(Dispatchers.IO) {
                    val input = resolver.openInputStream(uri) ?: throw IOException("无法读取所选文件")
                    input.use { it.readBytes().toString(Charsets.UTF_8) }
                }
                val count = withContext(Dispatchers.IO) { backupRepository.importFromJson(text) }
                _uiState.update {
                    it.copy(busy = false, busyAction = null, message = "导入成功，新增 $count 题")
                }
            } catch (e: IllegalArgumentException) {
                _uiState.update {
                    it.copy(busy = false, busyAction = null, message = backupErrorMessage(e))
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(busy = false, busyAction = null, message = "导入失败：${e.message ?: "未知错误"}")
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // WebDAV 备份
    // ------------------------------------------------------------------

    /** 备份到 WebDAV：导出 JSON → 确保父目录存在 → PUT 到配置的完整地址 */
    fun webDavBackup() {
        val config = _uiState.value.webDavConfig
        if (config.url.isBlank()) {
            _uiState.update { it.copy(message = "请先填写 WebDAV 地址") }
            return
        }
        if (_uiState.value.busy) return
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, busyAction = "webdav_backup", message = null) }
            try {
                val json = withContext(Dispatchers.IO) { backupRepository.exportToJson() }
                val directory = webDavDirectory(config.url)
                withContext(Dispatchers.IO) {
                    webDavClient.ensureCollection(directory, config.username, config.password)
                }
                withContext(Dispatchers.IO) {
                    webDavClient.put(config.url, config.username, config.password, json)
                }
                _uiState.update { it.copy(busy = false, busyAction = null, message = "已备份") }
            } catch (e: IOException) {
                _uiState.update {
                    it.copy(busy = false, busyAction = null, message = "WebDAV 操作失败：${e.message ?: "未知错误"}")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(busy = false, busyAction = null, message = "WebDAV 操作失败：${e.message ?: "未知错误"}")
                }
            }
        }
    }

    /** 从 WebDAV 恢复：GET → 导入；404（"远程文件不存在"）单独提示 */
    fun webDavRestore() {
        val config = _uiState.value.webDavConfig
        if (config.url.isBlank()) {
            _uiState.update { it.copy(message = "请先填写 WebDAV 地址") }
            return
        }
        if (_uiState.value.busy) return
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, busyAction = "webdav_restore", message = null) }
            try {
                val text = withContext(Dispatchers.IO) {
                    webDavClient.get(config.url, config.username, config.password)
                }
                val count = withContext(Dispatchers.IO) { backupRepository.importFromJson(text) }
                _uiState.update {
                    it.copy(busy = false, busyAction = null, message = "恢复成功，新增 $count 题")
                }
            } catch (e: IOException) {
                val message = if (e.message?.contains("远程文件不存在") == true) "远程文件不存在"
                else "WebDAV 操作失败：${e.message ?: "未知错误"}"
                _uiState.update { it.copy(busy = false, busyAction = null, message = message) }
            } catch (e: IllegalArgumentException) {
                _uiState.update { it.copy(busy = false, busyAction = null, message = backupErrorMessage(e)) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(busy = false, busyAction = null, message = "WebDAV 操作失败：${e.message ?: "未知错误"}")
                }
            }
        }
    }

    /**
     * 备份导入异常文案：版本过高提示升级应用，其余统一"格式不正确"。
     */
    private fun backupErrorMessage(e: IllegalArgumentException): String =
        if (e.message?.contains("版本过高") == true) e.message.orEmpty() else "备份文件格式不正确"

    /** UI 弹出 Snackbar 后消费掉 message，避免重复提示 */
    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    /** 模型选择对话框展示后消费掉 modelMessage */
    fun consumeModelMessage() {
        _uiState.update { it.copy(modelMessage = null) }
    }

    /**
     * 从完整文件 URL 推导父目录（collection）地址：
     * "https://host/dav/file.json" → "https://host/dav/"
     * "https://host/dav" → "https://host/dav/"
     */
    private fun webDavDirectory(url: String): String {
        val trimmed = url.trim().trimEnd('/')
        val lastSlash = trimmed.lastIndexOf('/')
        return if (lastSlash > "https://".length) trimmed.substring(0, lastSlash + 1) else "$trimmed/"
    }
}
