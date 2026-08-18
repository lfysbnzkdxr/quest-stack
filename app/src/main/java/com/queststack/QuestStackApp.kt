package com.queststack

import android.app.Application
import android.util.Log
import androidx.room.withTransaction
import com.queststack.data.DataContainer
import com.queststack.data.Seed
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class QuestStackApp : Application() {

    private val appScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, e ->
            Log.e("QuestStack", "Background task failed", e)
        }
    )

    override fun onCreate() {
        super.onCreate()
        DataContainer.init(this)
        appScope.launch {
            // seeded 标记防止用户删光题库后重启被重新注入；
            // Seed 内部另有"分类表为空"检查，避免升级用户（无标记但有数据）被重复注入
            val alreadySeeded = DataContainer.settingsRepository.seeded.first()
            if (!alreadySeeded) {
                DataContainer.database.withTransaction {
                    Seed.seedCategories(
                        DataContainer.database.categoryDao(),
                        DataContainer.database.questionDao(),
                    )
                }
                DataContainer.settingsRepository.markSeeded()
            }
        }
    }
}
