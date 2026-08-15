package com.queststack.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 带模糊效果的顶部栏（参考 MIUI 风格）。
 *
 * 支持运行时着色器时：以半透明 surface 铺底并对背后的页面内容做模糊；
 * 不支持时降级为纯色 surface 的 [SmallTopAppBar]，不崩溃。
 *
 * @param title 标题文本。
 * @param modifier 应用于顶栏根节点的修饰符。
 * @param onClick 非空时标题文字可点击（用于练题页点击标题弹出模式选择面板）。
 * @param navigationIcon 标题左侧的操作区内容（如返回按钮）。
 * @param actions 标题右侧的操作区内容。
 */
@Composable
fun GlassTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val backdrop = LocalGlassBackdrop.current
    if (isRuntimeShaderSupported() && backdrop != null) {
        val surface = MiuixTheme.colorScheme.surface
        val colors = BlurDefaults.blurColors(
            blendColors = listOf(
                BlendColorEntry(color = surface.copy(alpha = 0.6f)),
            ),
        )
        Box(
            modifier = modifier.textureBlur(
                backdrop = backdrop,
                shape = RectangleShape,
                blurRadius = 20f,
                noiseCoefficient = BlurDefaults.NoiseCoefficient,
                colors = colors,
            ),
        ) {
            // 模糊层已提供背景色，内部 SmallTopAppBar 只负责布局与标题
            SmallTopAppBar(
                title = if (onClick == null) title else "",
                color = Color.Transparent,
                navigationIcon = navigationIcon,
                actions = actions,
            )
            if (onClick != null) {
                // 标题文字覆盖层：居中于顶栏，仅文字区域可点击
                // （SmallTopAppBar 的 title 为 String 无法直接挂点击，故自绘同款样式的标题层）
                Box(
                    modifier = Modifier.matchParentSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = title,
                        style = MiuixTheme.textStyles.title3,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onBackground,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .clickable(onClick = onClick)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }
        }
    } else {
        Box(modifier = modifier) {
            SmallTopAppBar(
                title = if (onClick == null) title else "",
                color = Color.Transparent,
                navigationIcon = navigationIcon,
                actions = actions,
            )
            if (onClick != null) {
                Box(
                    modifier = Modifier.matchParentSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = title,
                        style = MiuixTheme.textStyles.title3,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onBackground,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .clickable(onClick = onClick)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}
