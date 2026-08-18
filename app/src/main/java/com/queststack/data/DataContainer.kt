package com.queststack.data

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.queststack.ai.AiClient
import com.queststack.data.backup.BackupRepository
import com.queststack.data.backup.WebDavClient
import com.queststack.data.db.AppDatabase
import com.queststack.data.repository.CategoryRepository
import com.queststack.data.repository.CategoryRepositoryImpl
import com.queststack.data.repository.QuestionRepository
import com.queststack.data.repository.QuestionRepositoryImpl
import com.queststack.data.repository.SettingsRepository
import com.queststack.ui.theme.AppSettings
import com.queststack.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object DataContainer {
    lateinit var database: AppDatabase
        private set
    lateinit var questionRepository: QuestionRepository
        private set
    lateinit var categoryRepository: CategoryRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var aiClient: AiClient
        private set
    lateinit var backupRepository: BackupRepository
        private set
    lateinit var webDavClient: WebDavClient
        private set

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, e ->
            Log.e("DataContainer", "Settings collect failed", e)
        }
    )

    fun init(context: Context) {
        database = AppDatabase.getInstance(context)
        questionRepository = QuestionRepositoryImpl(database, database.questionDao())
        categoryRepository = CategoryRepositoryImpl(database.categoryDao())
        settingsRepository = SettingsRepository(context)
        aiClient = AiClient(okHttpClient)
        backupRepository = BackupRepository(
            questionDao = database.questionDao(),
            categoryDao = database.categoryDao(),
            transactionRunner = { database.withTransaction(it) },
        )
        webDavClient = WebDavClient(
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
        )
        // 启动时同步加载主题模式，避免首帧以默认主题渲染后再切换（深色用户闪白）；
        // 读取失败（如 DataStore 文件损坏）时回退默认主题，避免启动崩溃（与下方 collect 的兜底一致）
        AppSettings.themeMode = try {
            runBlocking { settingsRepository.themeMode.first() }
        } catch (e: Exception) {
            Log.w("DataContainer", "读取主题模式失败，使用默认主题", e)
            ThemeMode.System
        }
        scope.launch {
            settingsRepository.themeMode.collect { AppSettings.themeMode = it }
        }
    }
}
