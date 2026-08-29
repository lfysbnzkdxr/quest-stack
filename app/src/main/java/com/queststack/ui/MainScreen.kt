package com.queststack.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.queststack.ui.component.FloatingBottomBar
import com.queststack.ui.component.FloatingBottomBarItem
import com.queststack.ui.component.LocalGlassBackdrop
import com.queststack.ui.component.MainPagerState
import com.queststack.ui.component.animation.NavSlideEasing
import com.queststack.ui.component.rememberMainPagerState
import com.queststack.ui.navigation.LocalNavigator
import com.queststack.ui.navigation.Navigator
import com.queststack.ui.navigation.Route
import com.queststack.ui.navigation.rememberNavigator
import com.queststack.ui.screen.add.AddScreen
import com.queststack.ui.screen.detail.QuestionDetailScreen
import com.queststack.ui.screen.home.HomeScreen
import com.queststack.ui.screen.library.LibraryScreen
import com.queststack.ui.screen.practice.PracticeSession
import com.queststack.ui.screen.practice.PracticeSessionScreen
import com.queststack.ui.screen.settings.SettingsScreen
import com.queststack.ui.screen.settings.SettingsSubRoute
import com.queststack.ui.screen.settings.SettingsSubScreen
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 底部导航 tab 定义（顺序即页面横滑顺序） */
enum class MainTab(val icon: ImageVector, val label: String) {
    Home(MiuixIcons.Home, "主页"),
    Library(MiuixIcons.GridView, "题库"),
    Settings(MiuixIcons.Settings, "设置"),
}

/**
 * 主界面（参考 KernelSU 架构）：
 * - 无全局顶栏：HorizontalPager 全屏承载三个 Tab，每个 Tab 页自带顶栏随页面横滑；
 * - 底栏为液态玻璃悬浮导航（KernelSU FloatingBottomBar 移植）；
 * - 主页常驻于 NavDisplay 之下，闪卡练题 / 添加页 / 题目详情 / 设置二级页作为覆盖层入栈；
 *   主页左移由 slide 动画与覆盖层滑入并行驱动，实现"覆盖式进入 + 旧页左移"（与 KernelSU 一致）。
 */

@Composable
fun MainScreen() {
    val pagerState = rememberPagerState(pageCount = { MainTab.entries.size })
    val scope = rememberCoroutineScope()
    val mainState = rememberMainPagerState(pagerState, scope)
    val navigator = rememberNavigator(Route.Main)
    // 首帧完成后再开启预加载，避免启动时同时创建三页
    var contentReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { contentReady = true }

    // 用户手动滑动页面时回写选中态（导航动画期间不打断）
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collect { page -> mainState.syncPage() }
    }

    // 主页左移动画：下钻页（栈深 > 1）入栈时主页向左平移 1/4，返回栈根时归零；
    // 与 NavDisplay 覆盖层转场并行（同为 500ms、同款缓动），实现"覆盖式进入 + 旧页左移"
    val slide = remember { Animatable(0f) }
    LaunchedEffect(navigator.backStackSize()) {
        val target = if (navigator.backStackSize() > 1) -0.25f else 0f
        slide.animateTo(target, tween(500, easing = NavSlideEasing))
    }

    // 采集整页内容作为玻璃底栏的模糊背景层（底栏 overlay 与采集层互为兄弟）
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    CompositionLocalProvider(LocalGlassBackdrop provides backdrop, LocalNavigator provides navigator) {
        // 主页常驻于 NavDisplay 之下：下钻页作为覆盖层由 NavDisplay 管理，主页永不离开组合，
        // 从根上避免 pop 回主页时重型页面重进组合导致的首帧卡顿（闪烁）；
        // 左移效果由上方 slide 动画驱动，不依赖 NavDisplay 内部 scene
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = slide.value * size.width },
        ) {
            MainContent(
                pagerState = pagerState,
                mainState = mainState,
                navigator = navigator,
                contentReady = contentReady,
                backdrop = backdrop,
            )
        }
        NavDisplay(
            backStack = navigator.backStack,
            // 仅在下钻页（栈深 > 1）时出栈覆盖层；栈根为 Route.Main 时 NavDisplay 不回调此处，
            // 其返回由 MainContent 内的 BackHandler 处理（非首页 tab 切回主页，否则退出应用）
            onBack = {
                if (navigator.backStackSize() > 1) navigator.pop()
            },
            entryProvider = entryProvider {
                entry<Route.Main> {
                    // 透明占位根：主页已在 NavDisplay 之下常驻渲染，这里只作为导航栈根，
                    // 不拦截触摸、不绘制内容，确保下层主页可见且可交互
                    Box(modifier = Modifier.fillMaxSize())
                }
                entry<Route.Detail> { key ->
                    QuestionDetailScreen(
                        questionId = key.questionId,
                        onBack = { navigator.pop() },
                    )
                }
                entry<Route.Add> {
                    AddScreen(onBack = { navigator.pop() })
                }
                entry<Route.Practice> { key ->
                    PracticeSessionScreen(
                        session = key.session,
                        onBack = { navigator.pop() },
                    )
                }
                entry<Route.SettingsSub> { key ->
                    SettingsSubScreen(
                        route = key.route,
                        onBack = { navigator.pop() },
                    )
                }
            },
        )
    }
}

@Composable
private fun MainContent(
    pagerState: PagerState,
    mainState: MainPagerState,
    navigator: Navigator,
    contentReady: Boolean,
    backdrop: LayerBackdrop,
) {
    val activity = LocalActivity.current
    val settledPage = pagerState.settledPage
    Box(modifier = Modifier.fillMaxSize()) {
        // 根级返回：栈仅剩主页根时，非首页 tab 优先切回主页；否则退出应用。
        // 下钻页（栈深 > 1）时此 BackHandler 禁用，返回由 NavDisplay 出栈覆盖层（对齐 KernelSU）
        BackHandler(enabled = navigator.backStackSize() <= 1) {
            if (mainState.selectedPage != 0) {
                mainState.animateToPage(0)
            } else {
                activity?.finish()
            }
        }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MiuixTheme.colorScheme.background,
            // 底栏不再放在 bottomBar 槽位，而是作为 overlay 浮于内容上方
            bottomBar = {},
        ) { _ ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    // 预加载相邻页面，避免点击切换时因创建中间页掉帧（KernelSU 同款）
                    beyondViewportPageCount = if (contentReady) MainTab.entries.size - 1 else 0,
                    overscrollEffect = null,
                    // 主 Tab 横滑恒定开启：下钻页是 NavDisplay 顶层 scene 会拦截底层触摸，
                    // 且 NavDisplay 默认 blockInputDuringTransition 已阻止转场期间误触；
                    // 若随路由在返回瞬间 false→true 翻转，会触发 pager 重组闪烁（对齐 KernelSU 的稳定值）
                    userScrollEnabled = true,
                ) { page ->
                    val isCurrentPage = page == settledPage
                    when (MainTab.entries[page]) {
                        MainTab.Home -> if (isCurrentPage || contentReady) HomeScreen(
                            onStartPractice = { navigator.push(Route.Practice(it)) },
                            onGoLibrary = { mainState.animateToPage(MainTab.Library.ordinal) },
                        )

                        MainTab.Library -> if (isCurrentPage || contentReady) LibraryScreen(
                            onQuestionClick = { navigator.push(Route.Detail(it)) },
                            onAddClick = { navigator.push(Route.Add) },
                        )

                        MainTab.Settings -> if (isCurrentPage || contentReady) SettingsScreen(
                            onNavigateSub = { navigator.push(Route.SettingsSub(it)) },
                        )
                    }
                }
            }
        }
        // 液态玻璃悬浮底栏 overlay（KernelSU FloatingBottomBar 移植）：
        // 不占内容区域，内容可延伸到底栏后方
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            FloatingBottomBar(
                modifier = Modifier.padding(
                    bottom = 12.dp + WindowInsets.navigationBars
                        .only(WindowInsetsSides.Bottom)
                        .asPaddingValues()
                        .calculateBottomPadding(),
                ),
                selectedIndex = { mainState.selectedPage },
                onSelected = { mainState.animateToPage(it) },
                backdrop = backdrop,
                tabsCount = MainTab.entries.size,
                // 不支持 AGSL 运行时着色器时降级为普通半透明胶囊
                isBlurEnabled = isRuntimeShaderSupported(),
            ) {
                MainTab.entries.forEach { tab ->
                    FloatingBottomBarItem(
                        onClick = { mainState.animateToPage(tab.ordinal) },
                        modifier = Modifier.defaultMinSize(minWidth = 76.dp),
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                        )
                        Text(
                            text = tab.label,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
