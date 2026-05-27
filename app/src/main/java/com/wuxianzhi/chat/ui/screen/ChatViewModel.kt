package com.wuxianzhi.chat.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wuxianzhi.chat.App
import com.wuxianzhi.chat.data.ChatMessage
import com.wuxianzhi.chat.data.Conversation
import com.wuxianzhi.chat.data.DefaultModels
import com.wuxianzhi.chat.data.PresetPrompts
import com.wuxianzhi.chat.data.Role
import com.wuxianzhi.chat.network.ChatApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ChatUiState(
    val conversation: Conversation = Conversation(),
    val isStreaming: Boolean = false,
    val error: String? = null,
)

class ChatViewModel(private val conversationId: String) : ViewModel() {

    private val app = App.instance
    private val store = app.conversations
    private val settings = app.settings
    private val api: ChatApi = app.chatApi

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var streamJob: Job? = null

    init {
        val existing = store.get(conversationId)
        if (existing != null) {
            _state.value = ChatUiState(conversation = existing)
        } else {
            // brand new conversation seeded with defaults — default to the unlimited persona
            // since that's the whole point of this project. User can clear/swap in chat settings.
            val fresh = Conversation(
                id = conversationId,
                model = settings.defaultModel.value.ifBlank { DefaultModels.DEFAULT_ID },
                systemPrompt = PresetPrompts.UNLIMITED_PYRITE,
            )
            _state.value = ChatUiState(conversation = fresh)
        }
    }

    fun setModel(modelId: String) {
        mutate { it.copy(model = modelId) }
    }

    fun setSystemPrompt(prompt: String) {
        mutate { it.copy(systemPrompt = prompt) }
    }

    fun setTitle(title: String) {
        mutate { it.copy(title = title.ifBlank { "新对话" }) }
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        if (state.value.isStreaming) return

        val convo = state.value.conversation
        val userMsg = ChatMessage(role = Role.user, content = trimmed)

        val newTitle = if (convo.messages.isEmpty() && convo.title == "新对话") {
            trimmed.take(20)
        } else convo.title

        val withUser = convo.copy(
            title = newTitle,
            messages = convo.messages + userMsg,
            updatedAt = System.currentTimeMillis(),
        )
        _state.value = _state.value.copy(conversation = withUser, error = null)
        persist()

        startStream()
    }

    fun regenerate() {
        if (state.value.isStreaming) return
        val convo = state.value.conversation
        // drop trailing assistant message(s) so we re-ask from the same user prompt
        val trimmed = convo.messages.dropLastWhile { it.role == Role.assistant }
        if (trimmed.isEmpty() || trimmed.last().role != Role.user) return
        _state.value = _state.value.copy(
            conversation = convo.copy(messages = trimmed),
            error = null,
        )
        persist()
        startStream()
    }

    fun stopStreaming() {
        streamJob?.cancel()
        streamJob = null
        _state.value = _state.value.copy(isStreaming = false)
        persist()
    }

    fun deleteMessage(id: String) {
        mutate { c -> c.copy(messages = c.messages.filterNot { it.id == id }) }
    }

    private fun startStream() {
        val convo = state.value.conversation
        val placeholder = ChatMessage(role = Role.assistant, content = "")
        _state.value = _state.value.copy(
            conversation = convo.copy(messages = convo.messages + placeholder),
            isStreaming = true,
            error = null,
        )

        streamJob = viewModelScope.launch {
            val histForSend = state.value.conversation.messages.dropLast(1)
            val modelId = state.value.conversation.model.ifBlank { settings.defaultModel.value }
            val sysPrompt = state.value.conversation.systemPrompt
            val temp = settings.temperature.value

            try {
                api.streamChat(
                    model = modelId,
                    systemPrompt = sysPrompt,
                    history = histForSend,
                    temperature = temp,
                ).collectLatest { ev ->
                    when (ev) {
                        is ChatApi.StreamEvent.Delta -> appendToLastAssistant(ev.text)
                        is ChatApi.StreamEvent.Done -> Unit
                        is ChatApi.StreamEvent.Failure -> {
                            _state.value = _state.value.copy(error = ev.message)
                            replaceLastAssistantIfEmpty("[请求失败] ${ev.message}")
                        }
                    }
                }
            } finally {
                _state.value = _state.value.copy(isStreaming = false)
                persist()
            }
        }
    }

    private fun appendToLastAssistant(delta: String) {
        val convo = state.value.conversation
        val msgs = convo.messages.toMutableList()
        val last = msgs.lastOrNull() ?: return
        if (last.role != Role.assistant) return
        msgs[msgs.lastIndex] = last.copy(content = last.content + delta)
        _state.value = _state.value.copy(conversation = convo.copy(messages = msgs))
    }

    private fun replaceLastAssistantIfEmpty(text: String) {
        val convo = state.value.conversation
        val msgs = convo.messages.toMutableList()
        val last = msgs.lastOrNull() ?: return
        if (last.role == Role.assistant && last.content.isEmpty()) {
            msgs[msgs.lastIndex] = last.copy(content = text)
            _state.value = _state.value.copy(conversation = convo.copy(messages = msgs))
        }
    }

    private fun mutate(block: (Conversation) -> Conversation) {
        val updated = block(state.value.conversation).copy(updatedAt = System.currentTimeMillis())
        _state.value = _state.value.copy(conversation = updated)
        persist()
    }

    private fun persist() {
        viewModelScope.launch { store.upsert(state.value.conversation) }
    }

    override fun onCleared() {
        streamJob?.cancel()
        super.onCleared()
    }
}
