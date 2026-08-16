package com.queststack

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import com.queststack.ui.MainScreen
import com.queststack.ui.theme.AppSettings
import com.queststack.ui.theme.AppTheme
import com.queststack.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 跟随主题判定系统栏图标明暗（浅色主题深图标 / 深色主题浅图标）
            val dark = when (AppSettings.themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            // 全面屏适配（参考 KernelSU）：透明系统栏 + 内容延伸到状态栏，
            // 顶栏/底栏毛玻璃背景与系统栏无缝衔接
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT
                    ) { dark },
                    navigationBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT
                    ) { dark },
                )
                window.isNavigationBarContrastEnforced = false
                onDispose { }
            }
            AppTheme {
                MainScreen()
            }
        }
    }
}
