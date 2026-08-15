package com.queststack.ui.screen.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.queststack.ai.AiClient
import com.queststack.data.DataContainer
import com.queststack.data.db.Category
import com.queststack.data.repository.AiConfig
import com.queststack.data.repository.CategoryRepository
import com.queststack.data.repository.QuestionRepository
import com.queststack.data.repository.SettingsRepository
import com.queststack.util.TextStandardizer
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AddUiState(
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val difficulty: Int = 1,
    val title: String = "",
    val answer: String = "",
    val roundsPreview: List<Pair<String, String>>? = null,
    val standardizeDone: Boolean = false,
    val saving: Boolean = false,
    val message: String? = null,
    val aiBusy: Boolean = false,
    val aiConfig: AiConfig? = null,
)

class AddViewModel(
    private val questionRepository: QuestionRepository = DataContainer.questionRepository,
    private val categoryRepository: CategoryRepository = DataContainer.categoryRepository,
    private val aiClient: AiClient = DataContainer.aiClient,
    private val settingsRepository: SettingsRepository = DataContainer.settingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddUiState())
    val uiState: StateFlow<AddUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
        // 收集 AI 配置：设置页修改后本页按钮可用性实时更新
        viewModelScope.launch {
            settingsRepository.aiConfig.collect { config ->
                _uiState.update { it.copy(aiConfig = config) }
            }
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

    /** 按本地规则解析答案中的问答对：第一对 → 主答案，其余 → 追问链预览 */
    fun standardize() {
        val current = _uiState.value
        val pairs = TextStandardizer.parseQaPairs(current.answer)
        val mainAnswer: String
        val followUps: List<Pair<String, String>>
        if (pairs.isEmpty()) {
            mainAnswer = current.answer
            followUps = emptyList()
        } else {
            val first = pairs.first()
            mainAnswer = first.second.ifBlank { first.first }
            followUps = pairs.drop(1)
        }
        _uiState.update {
            it.copy(
                answer = mainAnswer,
                roundsPreview = followUps.ifEmpty { null },
                standardizeDone = true,
                title = TextStandardizer.normalize(current.title),
            )
        }
    }

    // ------------------------------------------------------------------
    // AI 功能
    // ------------------------------------------------------------------

    /** AI 配置是否可用（baseUrl 与 model 均已填写）；不可用返回 null */
    private fun requireAiConfig(): AiConfig? {
        val config = _uiState.value.aiConfig ?: return null
        return if (config.baseUrl.isNotBlank() && config.model.isNotBlank()) config else null
    }

    /** AI 调用失败统一处理：复位 aiBusy，并按异常类型设置提示文案 */
    private fun handleAiError(config: AiConfig, e: Exception) {
        val message = when (e) {
            is IOException -> "AI 请求失败：网络错误或接口不可用"
            is TimeoutCancellationException -> "AI 请求超时（已设置 ${config.timeoutSeconds} 秒），可去设置调整"
            is IllegalArgumentException -> "AI 返回格式异常"
            else -> "AI 调用失败，请稍后重试"
        }
        _uiState.update { it.copy(aiBusy = false, message = message) }
    }

    /** AI 生成追问链：第一轮答案作为主答案，后续轮次作为追问链预览 */
    fun generateChain() {
        val current = _uiState.value
        if (current.aiBusy || current.title.isBlank() || current.answer.isNotBlank()) return
        val config = requireAiConfig()
        if (config == null) {
            _uiState.update { it.copy(message = "请先在设置中配置 AI 接口") }
            return
        }
        _uiState.update { it.copy(aiBusy = true, message = null) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val rounds = aiClient.generateQuestionChain(
                        config.baseUrl, config.apiKey, config.model, current.title,
                        timeoutSeconds = config.timeoutSeconds,
                    )
                    if (rounds.isEmpty()) {
                        _uiState.update {
                            it.copy(aiBusy = false, message = "AI 未返回有效内容，请重试")
                        }
                        return@withContext
                    }
                    _uiState.update {
                        it.copy(
                            aiBusy = false,
                            answer = rounds.first().second,
                            roundsPreview = rounds.drop(1).ifEmpty { null },
                            standardizeDone = false,
                            message = "AI 生成 ${rounds.size} 轮内容（可预览后保存）",
                        )
                    }
                } catch (e: Exception) {
                    handleAiError(config, e)
                }
            }
        }
    }

    /** AI 优化表述：结果写回答案输入框（旧轮次预览作废，避免保存到过期内容） */
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
            withContext(Dispatchers.IO) {
                try {
                    val optimized = aiClient.optimizeAnswer(
                        config.baseUrl, config.apiKey, config.model, current.title, current.answer,
                        timeoutSeconds = config.timeoutSeconds,
                    )
                    _uiState.update {
                        it.copy(
                            aiBusy = false,
                            answer = optimized,
                            roundsPreview = null,
                            standardizeDone = false,
                            message = "已优化表述",
                        )
                    }
                } catch (e: Exception) {
                    handleAiError(config, e)
                }
            }
        }
    }

    /** AI 优化格式：第一轮答案作为主答案，后续轮次作为追问链预览 */
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
            withContext(Dispatchers.IO) {
                try {
                    val rounds = aiClient.formatAnswer(
                        config.baseUrl, config.apiKey, config.model, current.title, current.answer,
                        timeoutSeconds = config.timeoutSeconds,
                    )
                    if (rounds.isEmpty()) {
                        _uiState.update {
                            it.copy(aiBusy = false, message = "AI 未返回有效内容，请重试")
                        }
                        return@withContext
                    }
                    _uiState.update {
                        it.copy(
                            aiBusy = false,
                            answer = rounds.first().second,
                            roundsPreview = rounds.drop(1).ifEmpty { null },
                            standardizeDone = false,
                            message = "已按 AI 整理为 ${rounds.size} 轮内容",
                        )
                    }
                } catch (e: Exception) {
                    handleAiError(config, e)
                }
            }
        }
    }

    /** 校验 title 非空后入库；roundsPreview 为追问链，主答案存 question.answer */
    fun save() {
        val current = _uiState.value
        if (current.title.isBlank() || current.saving) return
        val rounds = current.roundsPreview.orEmpty()
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, message = null) }
            try {
                questionRepository.addQuestion(
                    title = TextStandardizer.normalize(current.title),
                    answer = current.answer,
                    categoryId = current.selectedCategoryId,
                    difficulty = current.difficulty,
                    rounds = rounds,
                )
                _uiState.update {
                    it.copy(
                        saving = false,
                        title = "",
                        answer = "",
                        roundsPreview = null,
                        standardizeDone = false,
                        message = "已添加",
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(saving = false, message = "保存失败，请重试") }
            }
        }
    }

    /** UI 弹出 Toast 后消费掉 message，避免重复提示 */
    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
