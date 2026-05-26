package com.wuxianzhi.chat.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * API key + endpoint + model defaults live in an EncryptedSharedPreferences so the
 * key never sits in plain-text on disk — this was a real problem in the source web
 * project, which stashed everything in localStorage.
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val _apiKey = MutableStateFlow(prefs.getString(K_API_KEY, "") ?: "")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _baseUrl = MutableStateFlow(prefs.getString(K_BASE_URL, DefaultModels.DEFAULT_BASE_URL) ?: DefaultModels.DEFAULT_BASE_URL)
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    private val _defaultModel = MutableStateFlow(prefs.getString(K_DEFAULT_MODEL, DefaultModels.DEFAULT_ID) ?: DefaultModels.DEFAULT_ID)
    val defaultModel: StateFlow<String> = _defaultModel.asStateFlow()

    private val _temperature = MutableStateFlow(prefs.getFloat(K_TEMP, 0.7f))
    val temperature: StateFlow<Float> = _temperature.asStateFlow()

    private val _streaming = MutableStateFlow(prefs.getBoolean(K_STREAM, true))
    val streaming: StateFlow<Boolean> = _streaming.asStateFlow()

    fun setApiKey(v: String) {
        prefs.edit().putString(K_API_KEY, v.trim()).apply()
        _apiKey.value = v.trim()
    }

    fun setBaseUrl(v: String) {
        val cleaned = v.trim().trimEnd('/')
        prefs.edit().putString(K_BASE_URL, cleaned).apply()
        _baseUrl.value = cleaned
    }

    fun setDefaultModel(v: String) {
        prefs.edit().putString(K_DEFAULT_MODEL, v).apply()
        _defaultModel.value = v
    }

    fun setTemperature(v: Float) {
        prefs.edit().putFloat(K_TEMP, v).apply()
        _temperature.value = v
    }

    fun setStreaming(v: Boolean) {
        prefs.edit().putBoolean(K_STREAM, v).apply()
        _streaming.value = v
    }

    fun isConfigured(): Boolean = _apiKey.value.isNotBlank() && _baseUrl.value.isNotBlank()

    companion object {
        private const val K_API_KEY = "api_key"
        private const val K_BASE_URL = "base_url"
        private const val K_DEFAULT_MODEL = "default_model"
        private const val K_TEMP = "temperature"
        private const val K_STREAM = "streaming"
    }
}
