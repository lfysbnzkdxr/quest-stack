package com.queststack.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 页面内容采集层，由 MainScreen 创建并挂载到内容容器上，
 * 供玻璃顶栏 / 玻璃底栏模糊采样（参考 miuix example 的 LayerBackdrop 用法）。
 */
internal val LocalGlassBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/**
 * 带模糊效果的顶部栏（参考 MIUI 风格），顶栏背景向上延伸覆盖状态栏，
 * 与系统通知栏无缝衔接（配合 MainActivity 的 enableEdgeToEdge）。
 *
 * 支持运行时着色器时：以半透明 surface 铺底并对页面内容做模糊；
 * 不支持时降级为纯色 TopAppBar，不崩溃。
 *
 * 由 [PageScaffold] 使用：backdrop 为页面自己的采集层（与顶栏互为兄弟，
 * 避免顶栏在采集节点后代内采样 backdrop 形成循环采样）；不传时回退到
 * [LocalGlassBackdrop]（MainScreen 全局层）。
 *
 * @param title 标题文本。
 * @param scrollBehavior 滚动行为（MiuixScrollBehavior），滚动时标题随内容收缩。
 * @param backdrop 模糊采样层，null 时取 LocalGlassBackdrop。
 * @param navigationIcon 标题左侧的操作区内容（如返回按钮）。
 * @param actions 标题右侧的操作区内容。
 */
@Composable
fun GlassTopAppBar(
    title: String,
    scrollBehavior: ScrollBehavior,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val blurBackdrop = backdrop ?: LocalGlassBackdrop.current
    val topBar = @Composable {
        Column {
            // 状态栏高度占位：让顶栏背景（毛玻璃/纯色）盖满状态栏区域
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            TopAppBar(
                title = title,
                color = Color.Transparent,
                navigationIcon = navigationIcon,
                actions = actions,
                scrollBehavior = scrollBehavior,
            )
        }
    }
    if (isRuntimeShaderSupported() && blurBackdrop != null) {
        val surface = MiuixTheme.colorScheme.surface
        val colors = BlurDefaults.blurColors(
            blendColors = listOf(
                BlendColorEntry(color = surface.copy(alpha = 0.6f)),
            ),
        )
        Box(
            modifier = modifier.textureBlur(
                backdrop = blurBackdrop,
                shape = RectangleShape,
                blurRadius = 20f,
                noiseCoefficient = BlurDefaults.NoiseCoefficient,
                colors = colors,
            ),
        ) {
            topBar()
        }
    } else {
        Box(modifier = modifier) {
            topBar()
        }
    }
}
