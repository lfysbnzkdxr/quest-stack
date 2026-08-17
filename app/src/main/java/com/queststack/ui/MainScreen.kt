package com.queststack.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.queststack.ui.component.FloatingBottomBar
import com.queststack.ui.component.FloatingBottomBarItem
import com.queststack.ui.component.LocalGlassBackdrop
import com.queststack.ui.component.rememberMainPagerState
import com.queststack.ui.screen.add.AddScreen
import com.queststack.ui.screen.home.HomeScreen
import com.queststack.ui.screen.library.LibraryScreen
import com.queststack.ui.screen.practice.PracticeSession
import com.queststack.ui.screen.practice.PracticeSessionScreen
import com.queststack.ui.screen.settings.SettingsScreen
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
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
 * - 底栏为液态玻璃悬浮导航（KernelSU FloatingBottomBar 移植）：采样全局 backdrop，
 *   overlay 在内容之上，可拖动指示器（松手才切页）、按压放大、重力感应高光；
 * - 闪卡练题 / 添加页为全屏 overlay，盖在 pager 之上。
 */
@Composable
fun MainScreen() {
    val pagerState = rememberPagerState(pageCount = { MainTab.entries.size })
    val scope = rememberCoroutineScope()
    val mainState = rememberMainPagerState(pagerState, scope)
    var practiceSession by remember { mutableStateOf<PracticeSession?>(null) }
    var addOpen by remember { mutableStateOf(false) }
    // 首帧完成后再开启预加载，避免启动时同时创建三页（参考 KernelSU rememberContentReady）
    var contentReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { contentReady = true }

    // 用户手动滑动页面时回写选中态（导航动画期间不打断）
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collect { page -> mainState.syncPage() }
    }

    // 采集整页内容作为玻璃底栏的模糊背景层（底栏 overlay 与采集层互为兄弟）
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

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
                                onGoLibrary = { mainState.animateToPage(MainTab.Library.ordinal) },
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
        }
    }
    // 返回键优先级：先关 overlay，再回主页 Tab，最后才退出应用
    BackHandler(
        enabled = practiceSession != null || addOpen || mainState.selectedPage != 0,
    ) {
        when {
            practiceSession != null -> practiceSession = null
            addOpen -> addOpen = false
            mainState.selectedPage != 0 -> mainState.animateToPage(0)
        }
    }
}
