package com.wuxianzhi.chat.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Stores conversations as a single JSON file. Simple and durable enough for the chat use case.
 * The original web project only had ONE rolling session — here we keep a full list, each with
 * its own model and system prompt.
 */
class ConversationStore(context: Context) {

    private val file = File(context.filesDir, "conversations.json")
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    init { load() }

    private fun load() {
        try {
            if (file.exists()) {
                val raw = file.readText()
                if (raw.isNotBlank()) {
                    _conversations.value = json.decodeFromString<List<Conversation>>(raw)
                }
            }
        } catch (_: Throwable) {
            _conversations.value = emptyList()
        }
    }

    private suspend fun persist(list: List<Conversation>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            file.writeText(json.encodeToString(list))
        }
    }

    suspend fun upsert(c: Conversation) {
        val updated = _conversations.value.toMutableList()
        val idx = updated.indexOfFirst { it.id == c.id }
        if (idx >= 0) updated[idx] = c else updated.add(0, c)
        updated.sortByDescending { it.updatedAt }
        _conversations.value = updated
        persist(updated)
    }

    suspend fun delete(id: String) {
        val next = _conversations.value.filterNot { it.id == id }
        _conversations.value = next
        persist(next)
    }

    suspend fun clear() {
        _conversations.value = emptyList()
        persist(emptyList())
    }

    fun get(id: String): Conversation? = _conversations.value.firstOrNull { it.id == id }
}
