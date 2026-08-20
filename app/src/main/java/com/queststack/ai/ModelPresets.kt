package com.queststack.ai

/**
 * 供应商预设：仅提供名称与默认 base URL。
 * 具体模型型号不内嵌（迭代快），由「获取模型列表」实时拉取后供用户选择。
 */
data class ModelPreset(
    val id: String,
    val name: String,
    val defaultBaseUrl: String,
)

/** 国内主流大模型预设（均提供 OpenAI 兼容接口） */
val PRESETS: List<ModelPreset> = listOf(
    ModelPreset("deepseek", "DeepSeek", "https://api.deepseek.com"),
    ModelPreset("qwen", "通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1"),
    ModelPreset("zhipu", "智谱 GLM", "https://open.bigmodel.cn/api/paas/v4"),
    ModelPreset("kimi", "Kimi", "https://api.moonshot.cn/v1"),
    ModelPreset("doubao", "火山方舟豆包", "https://ark.cn-beijing.volces.com/api/v3"),
    ModelPreset("hunyuan", "腾讯混元", "https://api.hunyuan.cloud.tencent.com/v1"),
    ModelPreset("xiaomi", "小米 MiMo", "https://api.xiaomimimo.com/v1"),
)

/** 按 id 查找预设（自定义厂商返回 null） */
fun presetById(id: String): ModelPreset? = PRESETS.firstOrNull { it.id == id }
