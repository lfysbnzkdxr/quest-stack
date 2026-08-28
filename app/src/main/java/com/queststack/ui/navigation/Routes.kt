package com.queststack.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.queststack.ui.screen.practice.PracticeSession
import com.queststack.ui.screen.settings.SettingsSubRoute

/**
 * 类型安全的导航目标（参考 KernelSU 的 navigation3 架构）。
 * 每个目的地都是 [NavKey]，下钻页（详情/添加/练题/设置二级页）作为独立 entry 入栈，
 * 由 [androidx.navigation3.ui.NavDisplay] 的默认转场实现"覆盖式 + 旧页左移"。
 */
sealed interface Route : NavKey {
    data object Main : Route
    data class Detail(val questionId: Long) : Route
    data object Add : Route
    data class Practice(val session: PracticeSession) : Route
    data class SettingsSub(val route: SettingsSubRoute) : Route
}
