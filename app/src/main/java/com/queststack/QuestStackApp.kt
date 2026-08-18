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
            DataContainer.database.withTransaction {
                Seed.seedCategories(
                    DataContainer.database.categoryDao(),
                    DataContainer.database.questionDao(),
                )
            }
        }
    }
}
