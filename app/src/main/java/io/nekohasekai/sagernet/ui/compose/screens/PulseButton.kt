package io.nekohasekai.sagernet.ui.compose.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max

// Нормализованная форма ЭКГ (x 0..1, y 0..1, 0.5 = базовая линия)
private val ECG: List<Pair<Float, Float>> = listOf(
    0f to 0.5f, 0.12f to 0.5f, 0.18f to 0.44f, 0.24f to 0.56f, 0.30f to 0.5f,
    0.40f to 0.5f, 0.44f to 0.60f, 0.47f to 0.12f, 0.50f to 0.90f, 0.54f to 0.5f,
    0.62f to 0.5f, 0.70f to 0.43f, 0.77f to 0.57f, 0.84f to 0.5f, 1f to 0.5f,
)

private fun ecgY(x: Float): Float {
    for (i in 0 until ECG.size - 1) {
        val x0 = ECG[i].first; val y0 = ECG[i].second
        val x1 = ECG[i + 1].first; val y1 = ECG[i + 1].second
        if (x in x0..x1) {
            val t = if (x1 > x0) (x - x0) / (x1 - x0) else 0f
            return y0 + (y1 - y0) * t
        }
    }
    return 0.5f
}

/**
 * Кнопка-«сердце»: спит когда выключено (ровная линия + редкий удар), бьётся ритмом
 * когда подключено (ЭКГ-пульс + свечение на ударе). Изюминка SuperNet.
 */
@Composable
fun HeartbeatButton(
    connected: Boolean,
    connecting: Boolean,
    gold: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "hb")
    val period = if (connected) 1100 else if (connecting) 700 else 2600
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(period, easing = LinearEasing)),
        label = "phase",
    )
    // вспышка свечения когда «удар» (пик R на ~0.5) проходит под лучом
    val nearSpike = max(0f, 1f - abs(phase - 0.5f) / 0.12f)
    val glow = if (connected) nearSpike else nearSpike * 0.35f

    Box(
        modifier = modifier
            .size(210.dp)
            .clickable(enabled = !connecting) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(210.dp)) {
            val w = size.width
            val cx = w / 2f
            val cy = size.height / 2f
            val r = w / 2f - 6.dp.toPx()

            if (glow > 0f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(gold.copy(alpha = 0.38f * glow), Color.Transparent),
                        center = Offset(cx, cy), radius = r * 1.18f,
                    ),
                    radius = r * 1.18f, center = Offset(cx, cy),
                )
            }
            drawCircle(color = Color(0xFF0E0C09), radius = r, center = Offset(cx, cy))
            drawCircle(
                color = gold.copy(alpha = if (connected) 1f else 0.5f),
                radius = r, center = Offset(cx, cy),
                style = Stroke(width = (if (connected) 4f else 2f).dp.toPx()),
            )

            val left = cx - r * 0.72f
            val bandW = (cx + r * 0.72f) - left
            val top = cy - r * 0.40f
            val bandH = r * 0.80f
            val baseA = if (connected) 0.30f else 0.16f
            val addA = if (connected) 0.70f else 0.5f
            var nx = 0f
            var prev = Offset(left, top + ecgY(0f) * bandH)
            while (nx <= 1f) {
                val cur = Offset(left + nx * bandW, top + ecgY(nx) * bandH)
                val bright = max(0f, 1f - abs(nx - phase) / 0.14f)
                val a = (baseA + bright * addA).coerceIn(0f, 1f)
                drawLine(
                    color = gold.copy(alpha = a), start = prev, end = cur,
                    strokeWidth = (2f + bright * 2.5f).dp.toPx(),
                )
                prev = cur
                nx += 0.01f
            }
            // яркая точка-луч на текущей позиции
            drawCircle(
                color = gold,
                radius = 3.dp.toPx() + glow * 3.dp.toPx(),
                center = Offset(left + phase * bandW, top + ecgY(phase) * bandH),
            )
        }
        Icon(
            imageVector = Icons.Filled.PowerSettingsNew,
            contentDescription = if (connected) "Отключить" else "Подключить",
            tint = gold.copy(alpha = if (connected) 0.92f else 0.75f),
            modifier = Modifier.size(if (connected) 30.dp else 54.dp),
        )
    }
}
