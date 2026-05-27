package com.wuxianzhi.chat.network

import com.wuxianzhi.chat.data.ChatMessage
import com.wuxianzhi.chat.data.Role
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit

/**
 * Thin OpenAI-compatible chat client. Streams via SSE.
 * Improvement over the upstream Worker: streaming can be cancelled (see cancel handle in ViewModel).
 */
class ChatApi(
    private val getApiKey: () -> String,
    private val getBaseUrl: () -> String,
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)  // SSE is long-lived
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    sealed interface StreamEvent {
        data class Delta(val text: String) : StreamEvent
        data class Done(val usage: Usage?) : StreamEvent
        data class Failure(val message: String) : StreamEvent
    }

    @Serializable
    data class Usage(
        @SerialName("prompt_tokens") val promptTokens: Int = 0,
        @SerialName("completion_tokens") val completionTokens: Int = 0,
        @SerialName("total_tokens") val totalTokens: Int = 0,
    )

    fun streamChat(
        model: String,
        systemPrompt: String,
        history: List<ChatMessage>,
        temperature: Float,
    ): Flow<StreamEvent> = callbackFlow {

        val apiKey = getApiKey()
        val base = getBaseUrl().trimEnd('/')
        if (apiKey.isBlank()) {
            trySend(StreamEvent.Failure("API Key 未配置 — 请到设置里填入"))
            close()
            return@callbackFlow
        }

        val msgs = buildList {
            if (systemPrompt.isNotBlank()) add(Msg("system", systemPrompt))
            history.forEach {
                if (it.role == Role.user || it.role == Role.assistant) {
                    add(Msg(it.role.name, it.content))
                }
            }
        }

        val body = json.encodeToString(
            ChatRequest.serializer(),
            ChatRequest(
                model = model,
                stream = true,
                temperature = temperature,
                messages = msgs,
                streamOptions = StreamOptions(includeUsage = true),
            )
        )

        val req = Request.Builder()
            .url("$base/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val source: EventSource = EventSources.createFactory(http).newEventSource(
            req,
            object : EventSourceListener() {
                override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                    if (data == "[DONE]") return
                    try {
                        val chunk = json.decodeFromString(ChatChunk.serializer(), data)
                        chunk.choices.firstOrNull()?.delta?.content?.let { text ->
                            if (text.isNotEmpty()) trySend(StreamEvent.Delta(text))
                        }
                        chunk.usage?.let { trySend(StreamEvent.Done(it)) }
                    } catch (_: Throwable) {
                        // ignore unparseable chunks (keep-alive lines, partial frames)
                    }
                }

                override fun onClosed(eventSource: EventSource) {
                    trySend(StreamEvent.Done(null))
                    close()
                }

                override fun onFailure(eventSource: EventSource, t: Throwable?, response: okhttp3.Response?) {
                    val code = response?.code
                    val errBody = try { response?.body?.string() } catch (_: Throwable) { null }

                    val msg = when (code) {
                        404 -> "模型 ID 不存在或已下架（HTTP 404）。\n当前用的：$model\n去对话顶部 ⚙ → 模型，换一个或自己改 ID 试试。"
                        401, 403 -> "API Key 无效或没权限（HTTP $code）。检查设置里的 Key。"
                        429 -> "请求过快或额度用尽（HTTP 429）。等一下再试，或切到别的接口。"
                        in 500..599 -> "上游服务器错误（HTTP $code）。换个模型或稍后重试。\n${errBody?.take(200).orEmpty()}"
                        null -> "网络连接失败：${t?.message ?: "未知错误"}"
                        else -> buildString {
                            append("请求失败（HTTP $code）")
                            if (!errBody.isNullOrBlank()) append(": ${errBody.take(300)}")
                            else if (t != null) append(": ${t.message}")
                        }
                    }
                    trySend(StreamEvent.Failure(msg))
                    close()
                }
            }
        )

        awaitClose { source.cancel() }
    }

    @Serializable
    private data class Msg(val role: String, val content: String)

    @Serializable
    private data class StreamOptions(@SerialName("include_usage") val includeUsage: Boolean)

    @Serializable
    private data class ChatRequest(
        val model: String,
        val stream: Boolean,
        val temperature: Float,
        val messages: List<Msg>,
        @SerialName("stream_options") val streamOptions: StreamOptions,
    )

    @Serializable
    private data class Delta(val content: String? = null)

    @Serializable
    private data class Choice(val delta: Delta = Delta())

    @Serializable
    private data class ChatChunk(
        val choices: List<Choice> = emptyList(),
        val usage: Usage? = null,
    )
}
