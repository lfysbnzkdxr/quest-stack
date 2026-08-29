package com.queststack.ui.component.animation

import androidx.compose.animation.core.Easing

/**
 * 覆盖层滑入/滑出缓动：复刻 miuix NavDisplay 内部 NavTransitionEasing(0.8f, 0.95f) 的
 * 欠阻尼曲线，用于主页左移、练题页切题等需要与覆盖层转场（同为 500ms）同步的动画。
 */
internal val NavSlideEasing: Easing = object : Easing {
    private val r: Float
    private val w: Float
    private val c2: Float

    init {
        val omega = 2.0 * Math.PI / 0.8
        val k = omega * omega
        val c = 0.95 * 4.0 * Math.PI / 0.8
        w = (Math.sqrt(4.0 * k - c * c) / 2.0).toFloat()
        r = (-c / 2.0).toFloat()
        c2 = r / w
    }

    override fun transform(fraction: Float): Float {
        val t = fraction.toDouble()
        val decay = Math.exp(r * t)
        return (decay * (-Math.cos(w * t) + c2 * Math.sin(w * t)) + 1.0).toFloat()
    }
}