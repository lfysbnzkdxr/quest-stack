package com.queststack.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 页面级骨架（参考 KernelSU 的页内顶栏模式）：
 *
 * - 每个 Tab 页自带顶栏，随 HorizontalPager 一起横滑，与内容视觉一体；
 * - 顶栏与内容采集层互为兄弟（Column 上下排列），避免循环采样崩溃；
 * - 顶栏接 [MiuixScrollBehavior]，内容滚动时标题收缩（MIUI 风格）；
 * - 内容始终从顶栏下方开始，不受顶栏收缩影响（顶栏收缩时内容随之移动）。
 *
 * 根布局铺 surface 底色：作为全屏 overlay（如添加题页）使用时不会透出下层页面。
 *
 * @param title 顶栏标题。
 * @param content 页面内容，参数为滚动行为（挂到滚动容器的 nestedScroll 上即可联动顶栏）。
 */
@Composable
fun PageScaffold(
    title: String,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (ScrollBehavior) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val surface = MiuixTheme.colorScheme.surface
    val pageBackdrop = rememberLayerBackdrop {
        drawRect(surface)
        drawContent()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(surface),
    ) {
        GlassTopAppBar(
            title = title,
            scrollBehavior = scrollBehavior,
            backdrop = pageBackdrop,
            navigationIcon = navigationIcon,
            actions = actions,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .layerBackdrop(pageBackdrop),
        ) {
            content(scrollBehavior)
        }
    }
}
