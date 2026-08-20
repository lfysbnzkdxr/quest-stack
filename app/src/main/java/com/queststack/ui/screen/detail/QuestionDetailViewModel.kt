package com.queststack.ui.screen.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.queststack.ai.AiClient
import com.queststack.data.DataContainer
import com.queststack.data.db.Category
import com.queststack.data.db.Question
import com.queststack.data.repository.AiConfig
import com.queststack.data.repository.CategoryRepository
import com.queststack.data.repository.QuestionRepository
import com.queststack.data.repository.SettingsRepository
import com.queststack.util.TextStandardizer
import java.io.IOException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuestionDetailUiState(
    val question: Question? = null,
    val categories: List<Category> = emptyList(),
    val title: String = "",
    val answer: String = "",
    val selectedCategoryId: Long? = null,
    val difficulty: Int = 1,
    val saving: Boolean = false,
    val message: String? = null,
    val aiBusy: Boolean = false,
    val aiConfig: AiConfig? = null,
)

/** 题目详情页：按 id 观察单题与分类；内联编辑态可改标题/答案/分类/难度并支持 AI 生成优化 */
class QuestionDetailViewModel(
    private val questionId: Long,
    private val questionRepository: QuestionRepository = DataContainer.questionRepository,
    private val categoryRepository: CategoryRepository = DataContainer.categoryRepository,
    private val aiClient: AiClient = DataContainer.aiClient,
    private val settingsRepository: SettingsRepository = DataContainer.settingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestionDetailUiState())
    val uiState: StateFlow<QuestionDetailUiState> = _uiState.asStateFlow()

    /** 保存成功一次性事件：由 Screen 本地编辑态监听后退出编辑（不存 state，避免残留） */
    private val savedEventsChannel = Channel<Unit>(Channel.BUFFERED)
    val savedEvents: Flow<Unit> = savedEventsChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            questionRepository.observeQuestion(questionId).collect { question ->
                _uiState.update { it.copy(question = question) }
            }
        }
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
        viewModelScope.launch {
            settingsRepository.aiConfig.collect { config ->
                _uiState.update { it.copy(aiConfig = config) }
            }
        }
    }

    // ------------------------------------------------------------------
    // 编辑态
    // ------------------------------------------------------------------

    /** 进入编辑态：用当前题目内容预填表单（编辑态开关由 Screen 本地状态控制，避免残留闪烁） */
    fun startEdit() {
        val question = _uiState.value.question ?: return
        _uiState.update {
            it.copy(
                title = question.title,
                answer = question.answer,
                selectedCategoryId = question.categoryId,
                difficulty = question.difficulty,
                saving = false,
                aiBusy = false,
                message = null,
            )
        }
    }

    fun onTitleChange(value: String) {
        if (value == _uiState.value.title) return
        _uiState.update { it.copy(title = value) }
    }

    fun onAnswerChange(value: String) {
        if (value == _uiState.value.answer) return
        _uiState.update { it.copy(answer = value) }
    }

    fun selectCategory(id: Long?) {
        _uiState.update { it.copy(selectedCategoryId = id) }
    }

    fun selectDifficulty(d: Int) {
        _uiState.update { it.copy(difficulty = d) }
    }

    /** 标准化答案文本（统一换行、去除首尾空白） */
    fun standardize() {
        val current = _uiState.value
        if (current.answer.isBlank()) return
        _uiState.update {
            it.copy(
                answer = TextStandardizer.normalize(current.answer),
                title = TextStandardizer.normalize(current.title),
                message = "已标准化格式",
            )
        }
    }

    /** 保存编辑：校验 title 非空后更新该题，成功后发保存事件（observeById 自动刷新展示） */
    fun save() {
        val current = _uiState.value
        val question = current.question ?: return
        if (current.title.isBlank() || current.saving) return
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, message = null) }
            try {
                questionRepository.updateQuestion(
                    question.copy(
                        title = TextStandardizer.normalize(current.title),
                        answer = current.answer,
                        categoryId = current.selectedCategoryId,
                        difficulty = current.difficulty,
                    ),
                )
                _uiState.update { it.copy(saving = false, message = "已保存修改") }
                savedEventsChannel.trySend(Unit)
            } catch (e: Exception) {
                _uiState.update { it.copy(saving = false, message = "保存失败，请重试") }
            }
        }
    }

    /** UI 弹出 Toast 后消费掉 message，避免重复提示 */
    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    // ------------------------------------------------------------------
    // AI 功能（与添加页逻辑一致）
    // ------------------------------------------------------------------

    private fun requireAiConfig(): AiConfig? {
        val config = _uiState.value.aiConfig ?: return null
        return if (config.baseUrl.isNotBlank() && config.model.isNotBlank()) config else null
    }

    private fun handleAiError(config: AiConfig, e: Exception) {
        val message = when (e) {
            is IOException -> "AI 请求失败：网络错误或接口不可用"
            is TimeoutCancellationException -> "AI 请求超时（已设置 ${config.timeoutSeconds} 秒），可去设置调整"
            is IllegalArgumentException -> "AI 返回格式异常"
            else -> "AI 调用失败，请稍后重试"
        }
        _uiState.update { it.copy(aiBusy = false, message = message) }
    }

    /** AI 生成参考答案：结果写回答案输入框 */
    fun generateAnswer() {
        val current = _uiState.value
        if (current.aiBusy || current.title.isBlank() || current.answer.isNotBlank()) return
        val config = requireAiConfig()
        if (config == null) {
            _uiState.update { it.copy(message = "请先在设置中配置 AI 接口") }
            return
        }
        _uiState.update { it.copy(aiBusy = true, message = null) }
        viewModelScope.launch {
            try {
                val answer = aiClient.generateAnswer(
                    config.baseUrl, config.apiKey, config.model, current.title,
                    temperature = config.temperature,
                    timeoutSeconds = config.timeoutSeconds,
                )
                if (answer.isBlank()) {
                    _uiState.update {
                        it.copy(aiBusy = false, message = "AI 未返回有效内容，请重试")
                    }
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        aiBusy = false,
                        answer = answer,
                        message = "AI 已生成答案（可预览后保存）",
                    )
                }
            } catch (e: Exception) {
                handleAiError(config, e)
            }
        }
    }

    /** AI 优化表述：结果写回答案输入框 */
    fun optimizeAnswer() {
        val current = _uiState.value
        if (current.aiBusy || current.answer.isBlank()) return
        val config = requireAiConfig()
        if (config == null) {
            _uiState.update { it.copy(message = "请先在设置中配置 AI 接口") }
            return
        }
        _uiState.update { it.copy(aiBusy = true, message = null) }
        viewModelScope.launch {
            try {
                val optimized = aiClient.optimizeAnswer(
                    config.baseUrl, config.apiKey, config.model, current.title, current.answer,
                    temperature = config.temperature,
                    timeoutSeconds = config.timeoutSeconds,
                )
                _uiState.update {
                    it.copy(
                        aiBusy = false,
                        answer = optimized,
                        message = "已优化表述",
                    )
                }
            } catch (e: Exception) {
                handleAiError(config, e)
            }
        }
    }

    /** AI 优化格式：整理答案文本结构并写回答案输入框 */
    fun formatAnswer() {
        val current = _uiState.value
        if (current.aiBusy || current.answer.isBlank()) return
        val config = requireAiConfig()
        if (config == null) {
            _uiState.update { it.copy(message = "请先在设置中配置 AI 接口") }
            return
        }
        _uiState.update { it.copy(aiBusy = true, message = null) }
        viewModelScope.launch {
            try {
                val formatted = aiClient.formatAnswer(
                    config.baseUrl, config.apiKey, config.model, current.title, current.answer,
                    temperature = config.temperature,
                    timeoutSeconds = config.timeoutSeconds,
                )
                if (formatted.isBlank()) {
                    _uiState.update {
                        it.copy(aiBusy = false, message = "AI 未返回有效内容，请重试")
                    }
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        aiBusy = false,
                        answer = formatted,
                        message = "已按 AI 整理格式",
                    )
                }
            } catch (e: Exception) {
                handleAiError(config, e)
            }
        }
    }
}
