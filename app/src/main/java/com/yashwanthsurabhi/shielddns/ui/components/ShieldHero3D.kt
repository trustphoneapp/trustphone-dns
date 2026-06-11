package com.yashwanthsurabhi.shielddns.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import com.yashwanthsurabhi.shielddns.ui.theme.ShieldAccent
import com.yashwanthsurabhi.shielddns.ui.theme.ShieldSuccess
import com.yashwanthsurabhi.shielddns.ui.theme.ShieldTeal
import com.yashwanthsurabhi.shielddns.ui.theme.ShieldTealDark
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ShieldHero3D(
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val transition = rememberInfiniteTransition(label = "shield")
    val rotation by transition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            tween(3200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "rot",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    val glow by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = if (active) 0.45f else 0.2f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow",
    )
    val packetPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing)),
        label = "packetPhase",
    )

    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val inactiveFace = if (isDark) {
        listOf(primary.copy(alpha = 0.35f), ShieldTealDark.copy(alpha = 0.5f))
    } else {
        listOf(ShieldTeal.copy(alpha = 0.4f), ShieldTealDark.copy(alpha = 0.3f))
    }
    val activeFace = if (isDark) {
        listOf(ShieldAccent, primary, ShieldSuccess)
    } else {
        listOf(ShieldAccent, ShieldTeal, ShieldSuccess)
    }

    Canvas(
        modifier = modifier.fillMaxWidth().height(220.dp),
    ) {
        val cx = size.width / 2f
        val cy = size.height * 0.52f
        val shieldH = size.height * 0.62f
        val shieldW = shieldH * 0.78f
        val ringColor = if (active) primary else outline

        if (active) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = glow), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = shieldW * 0.9f,
                ),
                radius = shieldW * 0.9f,
                center = Offset(cx, cy),
            )
        }

        repeat(3) { index ->
            val radius = shieldW * (0.72f + index * 0.15f)
            drawCircle(
                color = ringColor.copy(alpha = if (active) 0.16f - index * 0.03f else 0.08f),
                radius = radius,
                center = Offset(cx, cy),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.4.dp.toPx()),
            )
        }

        if (active) {
            repeat(6) { index ->
                val degrees = packetPhase + index * 60f
                val radians = degrees / 180f * PI.toFloat()
                val radius = shieldW * if (index % 2 == 0) 0.98f else 0.76f
                val point = Offset(
                    x = cx + cos(radians) * radius,
                    y = cy + sin(radians) * radius * 0.62f,
                )
                drawCircle(
                    color = if (index % 2 == 0) ShieldAccent else ShieldSuccess,
                    radius = 3.5.dp.toPx(),
                    center = point,
                    alpha = 0.72f,
                )
            }
        } else {
            repeat(5) { index ->
                val x = cx - shieldW * 0.72f + index * shieldW * 0.36f
                drawLine(
                    color = ringColor.copy(alpha = 0.08f),
                    start = Offset(x, cy - shieldH * 0.42f),
                    end = Offset(x, cy + shieldH * 0.42f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }

        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    ShieldTealDark.copy(alpha = if (active) 0.4f else 0.14f),
                    Color.Transparent,
                ),
            ),
            topLeft = Offset(cx - shieldW * 0.7f, cy + shieldH * 0.38f),
            size = Size(shieldW * 1.4f, shieldH * 0.22f),
        )

        rotate(if (active) rotation else 0f, pivot = Offset(cx, cy)) {
            scale(if (active) pulse else 0.96f, pivot = Offset(cx, cy)) {
                val back = shieldPath(cx + 8f, cy + 10f, shieldW, shieldH)
                drawPath(
                    back,
                    brush = Brush.linearGradient(listOf(ShieldTealDark, ShieldTealDark.copy(alpha = 0.55f))),
                )

                val front = shieldPath(cx, cy, shieldW, shieldH)
                drawPath(
                    front,
                    brush = Brush.linearGradient(
                        colors = if (active) activeFace else inactiveFace,
                        start = Offset(cx, cy - shieldH / 2),
                        end = Offset(cx, cy + shieldH / 2),
                    ),
                )

                drawPath(
                    front,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = if (active) 0.5f else 0.12f),
                            Color.Transparent,
                        ),
                        start = Offset(cx - shieldW * 0.3f, cy - shieldH * 0.35f),
                        end = Offset(cx + shieldW * 0.2f, cy),
                    ),
                    alpha = 0.55f,
                )
            }
        }
    }
}

private fun Color.luminance(): Float =
    0.299f * red + 0.587f * green + 0.114f * blue

private fun shieldPath(cx: Float, cy: Float, w: Float, h: Float): Path = Path().apply {
    moveTo(cx, cy - h / 2)
    cubicTo(cx + w * 0.55f, cy - h * 0.35f, cx + w * 0.5f, cy + h * 0.05f, cx, cy + h / 2)
    cubicTo(cx - w * 0.5f, cy + h * 0.05f, cx - w * 0.55f, cy - h * 0.35f, cx, cy - h / 2)
    close()
}
