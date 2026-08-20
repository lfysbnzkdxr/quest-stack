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

    /** 生成参考答案：返回回答文本 */
    suspend fun generateAnswer(
        baseUrl: String,
        apiKey: String,
        model: String,
        title: String,
        temperature: Float = DEFAULT_TEMPERATURE,
        timeoutSeconds: Int = DEFAULT_TIMEOUT_SECONDS
    ): String {
        val system = "你是一个面试辅导专家。针对给出的面试问题编写一份详细、结构清晰的参考答案。" +
            "直接输出回答文本，不要加任何前缀说明。"
        val body = chatRequestBody(
            model,
            listOf(ChatMessage("system", system), ChatMessage("user", title)),
            temperature
        )
        return execute(buildRequest(chatCompletionsUrl(baseUrl), apiKey, body), timeoutSeconds).trim()
    }

    /** 润色回答：返回润色后的文本 */
    suspend fun optimizeAnswer(
        baseUrl: String,
        apiKey: String,
        model: String,
        title: String,
        answer: String,
        temperature: Float = DEFAULT_TEMPERATURE,
        timeoutSeconds: Int = DEFAULT_TIMEOUT_SECONDS
    ): String {
        val system = "你是面试辅导专家。润色以下回答，使其更有条理、更专业、更适合面试口述。" +
            "直接输出润色后的回答文本，不要加任何前缀说明。"
        val body = chatRequestBody(
            model,
            listOf(ChatMessage("system", system), ChatMessage("user", "问题：$title\n\n我的回答：$answer")),
            temperature
        )
        return execute(buildRequest(chatCompletionsUrl(baseUrl), apiKey, body), timeoutSeconds).trim()
    }

    /** 整理自由填写的问答文本为结构化参考答案 */
    suspend fun formatAnswer(
        baseUrl: String,
        apiKey: String,
        model: String,
        title: String,
        answer: String,
        temperature: Float = DEFAULT_TEMPERATURE,
        timeoutSeconds: Int = DEFAULT_TIMEOUT_SECONDS
    ): String {
        val system = "将以下面试问答内容整理为一份结构清晰、条理分明的参考答案。" +
            "直接输出整理后的回答文本，不要加任何前缀说明。"
        val body = chatRequestBody(
            model,
            listOf(ChatMessage("system", system), ChatMessage("user", "问题：$title\n\n内容：$answer")),
            temperature
        )
        return execute(buildRequest(chatCompletionsUrl(baseUrl), apiKey, body), timeoutSeconds).trim()
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

        private val json = Json { ignoreUnknownKeys = true }
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
