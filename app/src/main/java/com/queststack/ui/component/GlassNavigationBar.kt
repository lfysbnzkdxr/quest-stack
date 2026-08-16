package com.queststack.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.noiseDither
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.max

/**
 * 页面内容采集层，由 MainScreen 创建并挂载到内容容器上，
 * 供玻璃顶栏 / 玻璃底栏模糊采样（参考 miuix example 的 LayerBackdrop 用法）。
 */
internal val LocalGlassBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/**
 * 悬浮式液态玻璃底部导航。
 *
 * 支持运行时着色器时：采集页面内容做模糊 + 噪点抖动 + 边缘高光，
 * 以悬浮胶囊形态浮于屏幕底部（带阴影、圆角、与底部留间距）。
 * 不支持时降级为普通半透明背景的 [NavigationBar]，不崩溃。
 *
 * 注意：该组件设计为 overlay 使用（由 MainScreen 放在 Scaffold 内容之上），
 * 不占用 Scaffold 的 bottomBar 槽位，因此内容可以延伸到底栏后方。
 *
 * @param selected 当前选中的 item 下标。
 * @param onSelect item 点击回调，参数为 item 下标。
 * @param items 导航 item 列表（图标 + 标签）。
 * @param modifier 应用于底栏根节点的修饰符；通常由调用方传入 `Modifier.fillMaxSize()`
 *                 使内部胶囊可底部居中，同时不限制内容区域。
 */
@Composable
fun GlassNavigationBar(
    selected: Int,
    onSelect: (Int) -> Unit,
    items: List<NavigationItem>,
    modifier: Modifier = Modifier,
) {
    val backdrop = LocalGlassBackdrop.current
    if (isRuntimeShaderSupported() && backdrop != null) {
        FloatingGlassNavigationBar(
            selected = selected,
            onSelect = onSelect,
            items = items,
            backdrop = backdrop,
            modifier = modifier,
        )
    } else {
        FallbackNavigationBar(
            selected = selected,
            onSelect = onSelect,
            items = items,
            modifier = modifier,
        )
    }
}

@Composable
private fun FloatingGlassNavigationBar(
    selected: Int,
    onSelect: (Int) -> Unit,
    items: List<NavigationItem>,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
) {
    val isDark = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    val shape = CircleShape
    val containerColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
    val highlight = remember(isDark) {
        if (isDark) Highlight.GlassStrokeMiddleDark else Highlight.GlassStrokeMiddleLight
    }
    // 悬浮间距：与屏幕底部留出间隔（含系统导航栏 inset）
    val navBarBottomPadding = WindowInsets.navigationBars
        .only(WindowInsetsSides.Bottom)
        .asPaddingValues()
        .calculateBottomPadding()
    val bottomSpacing = if (navBarBottomPadding != 0.dp) 10.dp + navBarBottomPadding else 24.dp

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .padding(bottom = bottomSpacing)
                .selectableGroup()
                .dropShadow(
                    shape = shape,
                    shadow = Shadow(
                        radius = 10.dp,
                        color = Color.Black,
                        // 浅色主题阴影过重会发灰，深色主题加重阴影
                        alpha = if (isDark) 0.2f else 0.1f,
                    ),
                )
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        // 先扩大采集层以容纳模糊溢出（参考 LiquidGlassNavigationBar）
                        padding = max(padding, 32f * density)
                        noiseDither(BlurDefaults.NoiseCoefficient)
                        blur(24f * density, 24f * density)
                    },
                    highlight = { highlight },
                    onDrawSurface = { drawRect(containerColor) },
                )
                .height(60.dp)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                // 自绘 item：固定等宽（参考 iOS 悬浮胶囊），避免 IntrinsicSize.Min + weight 挤压
                val selected = index == selected
                Column(
                    modifier = Modifier
                        .width(64.dp)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(index) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = if (selected) MiuixTheme.colorScheme.primary
                        else MiuixTheme.colorScheme.onBackgroundVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        color = if (selected) MiuixTheme.colorScheme.primary
                        else MiuixTheme.colorScheme.onBackgroundVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun FallbackNavigationBar(
    selected: Int,
    onSelect: (Int) -> Unit,
    items: List<NavigationItem>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        NavigationBar(
            modifier = Modifier.fillMaxSize(),
            color = MiuixTheme.colorScheme.surface.copy(alpha = 0.85f),
            showDivider = false,
        ) {
            items.forEachIndexed { index, item ->
                NavigationBarItem(
                    selected = index == selected,
                    onClick = { onSelect(index) },
                    icon = item.icon,
                    label = item.label,
                )
            }
        }
    }
}
