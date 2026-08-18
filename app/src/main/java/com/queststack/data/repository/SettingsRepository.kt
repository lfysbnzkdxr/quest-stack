package com.queststack.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.queststack.data.SecureStorage
import com.queststack.data.backup.WebDavConfig
import com.queststack.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** AI 客户端配置 */
data class AiConfig(
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val timeoutSeconds: Int = 30
)

/** 设置持久化仓库（DataStore Preferences） */
class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.dataStore

    /** AI 配置流（合并 baseUrl/apiKey/model/timeoutSeconds 四个 key） */
    val aiConfig: Flow<AiConfig> = dataStore.data.map { prefs ->
        AiConfig(
            baseUrl = prefs[KEY_BASE_URL] ?: "",
            apiKey = SecureStorage.decrypt(prefs[KEY_API_KEY] ?: ""),
            model = prefs[KEY_MODEL] ?: "",
            timeoutSeconds = prefs[KEY_TIMEOUT_SECONDS] ?: 30
        )
    }

    /** 主题模式流（0=System 1=Light 2=Dark，默认 System） */
    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        when (prefs[KEY_THEME_MODE] ?: 0) {
            1 -> ThemeMode.Light
            2 -> ThemeMode.Dark
            else -> ThemeMode.System
        }
    }

    suspend fun setAiConfig(config: AiConfig) {
        dataStore.edit { prefs ->
            prefs[KEY_BASE_URL] = config.baseUrl
            prefs[KEY_API_KEY] = SecureStorage.encrypt(config.apiKey)
            prefs[KEY_MODEL] = config.model
            prefs[KEY_TIMEOUT_SECONDS] = config.timeoutSeconds
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = when (mode) {
                ThemeMode.System -> 0
                ThemeMode.Light -> 1
                ThemeMode.Dark -> 2
            }
        }
    }

    /** WebDAV 备份配置流（合并 url/username/password 三个 key） */
    val webDavConfig: Flow<WebDavConfig> = dataStore.data.map { prefs ->
        WebDavConfig(
            url = prefs[KEY_WEBDAV_URL] ?: "",
            username = prefs[KEY_WEBDAV_USERNAME] ?: "",
            password = SecureStorage.decrypt(prefs[KEY_WEBDAV_PASSWORD] ?: "")
        )
    }

    suspend fun setWebDavConfig(config: WebDavConfig) {
        dataStore.edit { prefs ->
            prefs[KEY_WEBDAV_URL] = config.url
            prefs[KEY_WEBDAV_USERNAME] = config.username
            prefs[KEY_WEBDAV_PASSWORD] = SecureStorage.encrypt(config.password)
        }
    }

    /** 预设种子题是否已注入过（防止用户删光题库后重启被重新注入） */
    val seeded: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_SEEDED] ?: false
    }

    suspend fun markSeeded() {
        dataStore.edit { prefs -> prefs[KEY_SEEDED] = true }
    }

    private companion object {
        val KEY_BASE_URL = stringPreferencesKey("base_url")
        val KEY_API_KEY = stringPreferencesKey("api_key")
        val KEY_MODEL = stringPreferencesKey("model")
        val KEY_TIMEOUT_SECONDS = intPreferencesKey("timeout_seconds")
        val KEY_THEME_MODE = intPreferencesKey("theme_mode")
        val KEY_WEBDAV_URL = stringPreferencesKey("webdav_url")
        val KEY_WEBDAV_USERNAME = stringPreferencesKey("webdav_username")
        val KEY_WEBDAV_PASSWORD = stringPreferencesKey("webdav_password")
        val KEY_SEEDED = booleanPreferencesKey("seeded")
    }
}
