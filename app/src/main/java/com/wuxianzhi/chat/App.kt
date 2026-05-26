package com.wuxianzhi.chat

import android.app.Application
import com.wuxianzhi.chat.data.ConversationStore
import com.wuxianzhi.chat.data.SettingsStore
import com.wuxianzhi.chat.network.ChatApi

/** Tiny manual DI — no need to pull in Hilt for an app this size. */
class App : Application() {

    lateinit var settings: SettingsStore
        private set
    lateinit var conversations: ConversationStore
        private set
    lateinit var chatApi: ChatApi
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        settings = SettingsStore(this)
        conversations = ConversationStore(this)
        chatApi = ChatApi(
            getApiKey = { settings.apiKey.value },
            getBaseUrl = { settings.baseUrl.value },
        )
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
