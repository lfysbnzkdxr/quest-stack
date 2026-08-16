package com.queststack.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.queststack.ui.component.GlassNavigationBar
import com.queststack.ui.component.LocalGlassBackdrop
import com.queststack.ui.screen.add.AddScreen
import com.queststack.ui.screen.home.HomeScreen
import com.queststack.ui.screen.library.LibraryScreen
import com.queststack.ui.screen.practice.PracticeSession
import com.queststack.ui.screen.practice.PracticeSessionScreen
import com.queststack.ui.screen.practiceLog.PracticeLogScreen
import com.queststack.ui.screen.settings.SettingsScreen
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs

/** 底部导航 tab 定义（顺序即页面横滑顺序） */
enum class MainTab(val icon: ImageVector, val label: String) {
    Home(MiuixIcons.Home, "主页"),
    Library(MiuixIcons.GridView, "题库"),
    Settings(MiuixIcons.Settings, "设置"),
}

/**
 * 主界面（参考 KernelSU 架构）：
 * - 无全局顶栏：HorizontalPager 全屏承载三个 Tab，每个 Tab 页自带顶栏随页面横滑；
 * - 底栏悬浮毛玻璃导航（采样全局 backdrop，overlay 在内容之上，不占用内容区域）；
 * - 闪卡练题 / 添加页为全屏 overlay，盖在 pager 之上；
 * - 分类练习弹窗的变暗遮罩在同一窗口绘制，避免独立 Popup 窗口导致状态栏闪烁/恢复。
 */
@Composable
fun MainScreen() {
    val pagerState = rememberPagerState(pageCount = { MainTab.entries.size })
    val scope = rememberCoroutineScope()
    var currentTabIndex by remember { mutableIntStateOf(0) }
    var practiceSession by remember { mutableStateOf<PracticeSession?>(null) }
    var addOpen by remember { mutableStateOf(false) }
    var practiceLogOpen by remember { mutableStateOf(false) }
    // 分类面板展开状态提升到 MainScreen，遮罩与 Activity 同窗口，点击遮罩/返回键关闭
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    // 首帧完成后再开启预加载，避免启动时同时创建三页（参考 KernelSU rememberContentReady）
    var contentReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { contentReady = true }

    // 滑动/点击切换时实时同步当前 tab（驱动底栏选中态）
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collect { page -> currentTabIndex = page }
    }

    // 采集整页内容作为玻璃底栏的模糊背景层（底栏 overlay 与采集层互为兄弟）
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val navItems = remember { MainTab.entries.map { NavigationItem(label = it.label, icon = it.icon) } }

    CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                    ) { page ->
                        when (MainTab.entries[page]) {
                            MainTab.Home -> HomeScreen(
                                onStartPractice = { practiceSession = it },
                                onGoLibrary = { scope.launch { pagerState.springAnimateToPage(MainTab.Library.ordinal) } },
                                onOpenPracticeLog = { practiceLogOpen = true },
                                categoryMenuExpanded = categoryMenuExpanded,
                                onCategoryMenuExpandedChange = { categoryMenuExpanded = it },
                            )
                            MainTab.Library -> LibraryScreen(
                                onStartPractice = { practiceSession = it },
                                onAddClick = { addOpen = true },
                            )
                            MainTab.Settings -> SettingsScreen()
                        }
                    }
                }
            }
            // 悬浮底栏 overlay（参考 KernelSU FloatingBottomBar）：不占内容区域，内容可延伸到底栏后方
            GlassNavigationBar(
                modifier = Modifier.fillMaxSize(),
                selected = currentTabIndex,
                onSelect = { index ->
                    // 快速连点会中断当前动画、直接滑向新目标；
                    // 弹簧动画（KernelSU 同款）起步跟手、收尾轻弹
                    scope.launch { pagerState.springAnimateToPage(index) }
                },
                items = navItems,
            )
            // 全屏 overlay：闪卡练题
            practiceSession?.let { session ->
                PracticeSessionScreen(
                    session = session,
                    onBack = { practiceSession = null },
                )
            }
            // 全屏 overlay：添加题目
            if (addOpen) {
                AddScreen(onBack = { addOpen = false })
            }
            // 全屏 overlay：练题记录
            if (practiceLogOpen) {
                PracticeLogScreen(onBack = { practiceLogOpen = false })
            }
            // 分类面板遮罩：与 Activity 同窗口，盖住顶栏/内容/底栏，点击关闭
            if (categoryMenuExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            onClick = { categoryMenuExpanded = false },
                        ),
                )
            }
        }
    }
    // 返回键优先级：先关分类面板，再关 overlay，再回主页 Tab，最后才退出应用
    BackHandler(
        enabled = categoryMenuExpanded || practiceSession != null || addOpen || practiceLogOpen || currentTabIndex != 0,
    ) {
        when {
            categoryMenuExpanded -> categoryMenuExpanded = false
            practiceLogOpen -> practiceLogOpen = false
            practiceSession != null -> practiceSession = null
            addOpen -> addOpen = false
            currentTabIndex != 0 -> scope.launch { pagerState.springAnimateToPage(0) }
        }
    }
}

/**
 * 底部导航切换弹簧（参考 KernelSU 的 PagerNavigationSpringSpec）：
 * stiffness 322.2 介于 StiffnessLow(150) 与 Medium(400) 之间，
 * dampingRatio ≈ 0.9 轻微欠阻尼，起步跟手、收尾带一丝弹性的"丝滑"手感。
 */
private val PagerNavigationSpringSpec: SpringSpec<Float> = spring(
    stiffness = 322.2f,
    dampingRatio = 32.31f / (2f * kotlin.math.sqrt(322.2f)),
    visibilityThreshold = 0.5f,
)

/**
 * 弹簧动画滚动到指定页（参考 KernelSU 的 springAnimateToPage）：
 * 用 Animatable 逐帧计算位移并 scrollBy 驱动，速度曲线完全由弹簧决定，
 * 比 animateScrollToPage 的默认动画更顺滑；跨多页时连续滑过中间页面。
 */
private suspend fun PagerState.springAnimateToPage(target: Int) {
    if (target !in 0 until pageCount) return
    var shouldSnapToTarget = false
    scroll(MutatePriority.UserInput) {
        val pageSize = layoutInfo.pageSize + layoutInfo.pageSpacing
        val distance = target - currentPage - currentPageOffsetFraction
        val scrollPixels = distance * pageSize
        if (abs(scrollPixels) <= 0.5f) return@scroll

        var consumedScroll = 0f
        var skipScroll = false
        Animatable(0f).animateTo(
            targetValue = scrollPixels,
            animationSpec = PagerNavigationSpringSpec,
        ) {
            if (skipScroll) return@animateTo

            val delta = value - consumedScroll
            if (abs(delta) > 0.5f) {
                val consumed = scrollBy(delta)
                consumedScroll += consumed
                if (abs(delta - consumed) > 0.1f) {
                    shouldSnapToTarget = true
                    skipScroll = true
                }
            } else {
                consumedScroll = value
            }

            if (abs(velocity) < 0.1f && abs(scrollPixels - consumedScroll) < 1.0f) {
                skipScroll = true
            }
        }

        val remaining = scrollPixels - consumedScroll
        if (abs(remaining) > 0.5f) {
            scrollBy(remaining)
        }
    }

    if (shouldSnapToTarget || currentPage != target) {
        scrollToPage(target)
    }
}
