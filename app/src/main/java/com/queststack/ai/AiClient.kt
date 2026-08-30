package com.queststack.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/** OpenAI 兼容聊天消息 */
data class ChatMessage(val role: String, val content: String)

/**
 * OpenAI 兼容 API 客户端。所有方法运行在 IO 调度器且不捕获异常，
 * 失败时抛 [IOException]/[IllegalArgumentException]/[TimeoutCancellationException]，由调用方（ViewModel）处理。
 */
class AiClient(private val okHttpClient: OkHttpClient) {

    /** 普通对话：返回 choices[0].message.content 文本 */
    suspend fun chat(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        temperature: Float = DEFAULT_TEMPERATURE,
        timeoutSeconds: Int = DEFAULT_TIMEOUT_SECONDS
    ): String {
        val responseBody = execute(buildRequest(chatCompletionsUrl(baseUrl), apiKey, chatRequestBody(model, messages, temperature)), timeoutSeconds)
        val content = json.parseToJsonElement(responseBody).jsonObject["choices"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
        return content ?: throw IllegalArgumentException("AI 响应中缺少 choices[0].message.content")
    }

    /** 生成参考答案：返回回答文本；categories 非空时模型会在首行额外建议「【分类】xxx」 */
    suspend fun generateAnswer(
        baseUrl: String,
        apiKey: String,
        model: String,
        title: String,
        difficulty: Int = 1,
        categories: List<String> = emptyList(),
        temperature: Float = DEFAULT_TEMPERATURE,
        timeoutSeconds: Int = DEFAULT_TIMEOUT_SECONDS
    ): String {
        val categorySpec = if (categories.isEmpty()) "" else
            "\n\n【分类】在开始作答前，先单独输出一行：【分类】与题目主题最贴合的分类名，" +
                "必须逐字取自以下选项之一：${categories.joinToString("、")}；" +
                "没有任何一项明显匹配时输出「【分类】未分类」。" +
                "分类以题目考查的主要知识域为准，而非表面背景词：如题目借 AI、大模型等场景考查软件工程、架构、分布式等后端知识，应归入对应工程分类。" +
                "该行之后再接上述输出结构。"
        val system = "你是面试辅导专家。针对给出的面试问题编写一份参考答案，" +
            "以应聘者的口吻撰写，让面试者可以直接照着说。\n\n$ANSWER_OUTPUT_SPEC\n\n$ANSWER_WRITING_SPEC\n\n${difficultyGuidance(difficulty)}$categorySpec"
        return chat(
            baseUrl, apiKey, model,
            listOf(ChatMessage("system", system), ChatMessage("user", "问题：$title")),
            temperature, timeoutSeconds,
        ).trim()
    }

    /** 润色回答：返回润色后的文本 */
    suspend fun optimizeAnswer(
        baseUrl: String,
        apiKey: String,
        model: String,
        title: String,
        answer: String,
        difficulty: Int = 1,
        temperature: Float = DEFAULT_TEMPERATURE,
        timeoutSeconds: Int = DEFAULT_TIMEOUT_SECONDS
    ): String {
        val system = "你是面试辅导专家。把下述回答重构为规范的「精炼回答 + 详尽回答」两段式结构，使其更符合面试口述习惯。" +
            "事实与观点一律以下列原答案为准：不得曲解、不得自行加入原文没有的实质性技术内容；" +
            "原文内容充分时只做结构梳理与语句润色，仅对单薄之处围绕原意轻量补全；不得缩水。\n\n" +
            "$ANSWER_OUTPUT_SPEC\n\n$ANSWER_WRITING_SPEC\n\n${difficultyGuidance(difficulty)}"
        return chat(
            baseUrl, apiKey, model,
            listOf(ChatMessage("system", system), ChatMessage("user", "问题：$title\n\n我的回答：$answer")),
            temperature, timeoutSeconds,
        ).trim()
    }

    /** 难度数值 → 深度校准说明（1=简单 2=中等 3=困难，非法值按简单处理） */
    private fun difficultyGuidance(difficulty: Int): String = when (difficulty) {
        2 -> "难度校准：这是中等题。除要点准确外，要讲清背后的原理，并结合一个典型应用场景说明。"
        3 -> "难度校准：这是困难题。需深挖原理与机制，讨论不同方案的权衡取舍，并给出实战经验或常见坑。"
        else -> "难度校准：这是简单题。概念与要点准确清晰即可，不必堆砌进阶内容。"
    }

    /** 拉取模型列表：GET {baseUrl}/models（兼容 /v1、/v3、/v4 端点），返回模型 id 列表 */
    suspend fun listModels(
        baseUrl: String,
        apiKey: String,
        timeoutSeconds: Int = DEFAULT_TIMEOUT_SECONDS
    ): List<String> {
        val responseBody = execute(buildRequest(modelsUrl(baseUrl), apiKey, "", "GET"), timeoutSeconds)
        return try {
            json.parseToJsonElement(responseBody).jsonObject["data"]
                ?.jsonArray
                ?.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.contentOrNull }
                .orEmpty()
        } catch (e: Exception) {
            throw IllegalArgumentException("解析模型列表失败：${e.message}")
        }
    }

    /** 构建 chat/completions 地址：兼容 OpenAI(/v1) 与智谱(/v4)、火山方舟(/v3) 等端点 */
    private fun chatCompletionsUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim().trimEnd('/')
        return when {
            trimmed.endsWith("/chat/completions") -> trimmed
            trimmed.endsWith("/v1") -> "$trimmed/chat/completions"
            trimmed.endsWith("/v3") || trimmed.endsWith("/v4") -> "$trimmed/chat/completions"
            else -> "$trimmed/v1/chat/completions"
        }
    }

    /** 构建模型列表地址：由 chat 地址派生，把 /chat/completions 替换为 /models */
    private fun modelsUrl(baseUrl: String): String =
        chatCompletionsUrl(baseUrl).replace("/chat/completions", "/models")

    private fun chatRequestBody(model: String, messages: List<ChatMessage>, temperature: Float): String =
        buildJsonObject {
            put("model", model)
            put("messages", JsonArray(messages.map { message ->
                buildJsonObject {
                    put("role", message.role)
                    put("content", message.content)
                }
            }))
            put("temperature", temperature)
            // 部分兼容网关默认输出上限过低，显式放宽避免参考答案被截断
            put("max_tokens", DEFAULT_MAX_TOKENS)
        }.toString()

    private fun buildRequest(url: String, apiKey: String, body: String, method: String = "POST"): Request {
        val builder = Request.Builder()
            .url(url)
        if (method.equals("GET", ignoreCase = true)) {
            builder.get()
        } else {
            builder.post(body.toRequestBody(JSON_MEDIA_TYPE))
        }
        if (apiKey.isNotBlank()) {
            builder.header("Authorization", "Bearer $apiKey")
        }
        return builder.build()
    }

    /** 发送请求并返回响应体；HTTP 非 2xx 抛 IOException；超时抛 TimeoutCancellationException */
    private suspend fun execute(request: Request, timeoutSeconds: Int): String =
        withTimeout(timeoutSeconds * 1000L) {
            withContext(Dispatchers.IO) {
                okHttpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body.string()
                    if (!response.isSuccessful) {
                        throw IOException("HTTP ${response.code}: $responseBody")
                    }
                    responseBody
                }
            }
        }

    companion object {
        const val DEFAULT_TIMEOUT_SECONDS = 30
        const val DEFAULT_TEMPERATURE = 0.7f
        const val DEFAULT_MAX_TOKENS = 2048

        /** 输出结构规范：两段式、纯文本（应用内答案按纯文本渲染，禁用 Markdown） */
        private const val ANSWER_OUTPUT_SPEC = "【输出结构】严格按下述纯文本格式输出，两个小标题都要有，除此之外不要有任何其他内容（不要开场白、不要复述问题、不要结尾客套）：\n" +
            "【精炼回答】\n" +
            "（一到两句话，50~100 字，点出核心结论，可直接作为面试开场回答）\n\n" +
            "【详尽回答】\n" +
            "（2~4 个编号要点，形如 1) 2) 3)，每个要点用\u201c关键词：解释\u201d展开，最后用一句实践建议或面试加分点收尾，300~600 字）"

        /** 行文话术规范 */
        private const val ANSWER_WRITING_SPEC = "【行文要求】\n" +
            "- 面试口述话术：专业但自然流畅，避免\u201c综上所述\u201d\u201c本文将\u201d这类书面腔\n" +
            "- 专业术语首次出现给中英对照，如：RAG（检索增强生成）\n" +
            "- 内容要落到具体技术、参数或场景，不说空泛套话\n" +
            "- 严禁使用任何 Markdown 标记（#、**、- 列表符、反引号代码块），只用编号与空行分段"

        private val json = Json { ignoreUnknownKeys = true }
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
