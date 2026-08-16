package com.queststack.data

import android.content.Context
import android.util.Log
import com.queststack.ai.AiClient
import com.queststack.data.backup.BackupRepository
import com.queststack.data.backup.WebDavClient
import com.queststack.data.db.AppDatabase
import com.queststack.data.repository.CategoryRepository
import com.queststack.data.repository.CategoryRepositoryImpl
import com.queststack.data.repository.PracticeLogRepository
import com.queststack.data.repository.PracticeLogRepositoryImpl
import com.queststack.data.repository.QuestionRepository
import com.queststack.data.repository.QuestionRepositoryImpl
import com.queststack.data.repository.SettingsRepository
import com.queststack.ui.theme.AppSettings
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
    lateinit var practiceLogRepository: PracticeLogRepository
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
        backupRepository = BackupRepository(database.questionDao(), database.categoryDao())
        practiceLogRepository = PracticeLogRepositoryImpl(database.practiceLogDao())
        webDavClient = WebDavClient(
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
        )
        scope.launch {
            settingsRepository.themeMode.collect { AppSettings.themeMode = it }
        }
    }
}
