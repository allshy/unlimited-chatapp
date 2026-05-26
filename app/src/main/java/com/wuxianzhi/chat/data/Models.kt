package com.wuxianzhi.chat.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class Role { user, assistant, system }

@Serializable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "新对话",
    val model: String = "",
    val systemPrompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class ModelOption(
    val id: String,
    val label: String,
)

object DefaultModels {
    // NVIDIA integrate.api.nvidia.com hosts an OpenAI-compatible endpoint with many models.
    // Pick whatever you have access to — these are stable, real model ids.
    val list = listOf(
        ModelOption("deepseek-ai/deepseek-r1", "deepseek-r1"),
        ModelOption("deepseek-ai/deepseek-r1-distill-llama-70b", "deepseek-r1-distill-70b"),
        ModelOption("openai/gpt-oss-120b", "gpt-oss-120b"),
        ModelOption("meta/llama-3.3-70b-instruct", "llama-3.3-70b"),
        ModelOption("qwen/qwen2.5-coder-32b-instruct", "qwen2.5-coder-32b"),
        ModelOption("nvidia/llama-3.1-nemotron-70b-instruct", "nemotron-70b"),
    )
    const val DEFAULT_ID = "deepseek-ai/deepseek-r1"
    const val DEFAULT_BASE_URL = "https://integrate.api.nvidia.com/v1"
}

object PresetPrompts {
    val list = listOf(
        "通用助手" to "你是一个有帮助、知识渊博、表达清晰的中文助手。回答尽量直接、准确，遇到不确定的事情就说不确定。",
        "代码搭子" to "你是一名资深软件工程师。回答精准、可执行；优先给可运行代码与最短示例；解释保持简洁；如有最佳实践与坑点要明确指出。",
        "翻译" to "你是一名专业中英互译。直接给出译文；保留原文格式（Markdown/代码块/列表）；专有名词保留英文并附中文注释。",
        "写作伙伴" to "你是一名擅长中文创作的写作伙伴。语言自然有节奏，避免空话套话；按用户要求的风格与长度输出。",
    )
}
