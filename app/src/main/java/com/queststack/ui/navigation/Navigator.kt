package com.queststack.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey

/**
 * 轻量导航栈（参考 KernelSU 的 Navigator，去掉了 result 通道与 saveable 逻辑）。
 * 持有 [backStack]，由 [androidx.navigation3.ui.NavDisplay] 消费并渲染当前 entry。
 */
class Navigator(initialKey: NavKey) {
    val backStack: SnapshotStateList<NavKey> = mutableStateListOf(initialKey)

    fun push(key: NavKey) {
        backStack.add(key)
    }

    fun replace(key: NavKey) {
        if (backStack.isNotEmpty()) {
            backStack[backStack.lastIndex] = key
        } else {
            backStack.add(key)
        }
    }

    fun replaceAll(keys: List<NavKey>) {
        if (keys.isEmpty()) return
        if (backStack.isNotEmpty()) {
            backStack.clear()
            backStack.addAll(keys)
        }
    }

    fun pop() {
        backStack.removeLastOrNull()
    }

    fun popUntil(predicate: (NavKey) -> Boolean) {
        while (backStack.isNotEmpty() && !predicate(backStack.last())) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    fun current(): NavKey? = backStack.lastOrNull()

    fun backStackSize(): Int = backStack.size
}

@Composable
fun rememberNavigator(startRoute: NavKey): Navigator = remember { Navigator(startRoute) }

val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("LocalNavigator not provided")
}
