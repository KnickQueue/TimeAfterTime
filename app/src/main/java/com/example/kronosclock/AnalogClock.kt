package com.example.kronosanalogclock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun AnalogClock(
    modifier: Modifier = Modifier,
    timeSourceMs: () -> Long,
    zoneId: ZoneId,
    showNumerals: Boolean,
    ambientMode: Boolean,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    // Tick rate: in ambient we only step once per second, otherwise ~60 FPS
    val tickMs = if (ambientMode) 1000L else 16L

    // Drive the clock
    val nowMs by produceState(initialValue = timeSourceMs(), key1 = zoneId, key2 = ambientMode) {
        while (true) {
            value = timeSourceMs()
            delay(tickMs)
        }
    }

    val instant = remember(nowMs, zoneId) { Instant.ofEpochMilli(nowMs).atZone(zoneId) }
    val seconds = instant.second + instant.nano / 1_000_000_000.0
    val minutes = instant.minute + seconds / 60.0
    val hours = (instant.hour % 12) + minutes / 60.0

    val minuteAngle = (minutes / 60.0) * 2.0 * Math.PI
    val hourAngle = (hours / 12.0) * 2.0 * Math.PI
    val secondAngle = (seconds / 60.0) * 2.0 * Math.PI

    val scheme = MaterialTheme.colorScheme
    val faceColor = scheme.surface
    val tickColor = scheme.onSurface.copy(alpha = 0.8f)
    val numeralColor = scheme.onSurface
    val hourMinuteColor = scheme.onSurface
    val secondColor = accentColor

    val density = LocalDensity.current
    val hourStroke = with(density) { 6.dp.toPx() }
    val minuteStroke = with(density) { 4.dp.toPx() }
    val secondStroke = with(density) { 2.dp.toPx() }
    val ringStroke = with(density) { 2.dp.toPx() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        val radius = min(size.width, size.height) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Face ring
        drawCircle(
            color = faceColor,
            radius = radius,
            center = center
        )
        drawCircle(
            color = tickColor.copy(alpha = 0.35f),
            radius = radius - ringStroke / 2f,
            center = center,
            style = Stroke(width = ringStroke)
        )

        // Minute/Hour ticks
        repeat(60) { i ->
            val angle = Math.toRadians(i * 6.0 - 90.0)
            val isHour = i % 5 == 0
            val inner = if (isHour) radius * 0.82f else radius * 0.88f
            val outer = radius * 0.96f
            val stroke = if (isHour) 3.5f else 1.8f
            val sx = (center.x + inner * cos(angle)).toFloat()
            val sy = (center.y + inner * sin(angle)).toFloat()
            val ex = (center.x + outer * cos(angle)).toFloat()
            val ey = (center.y + outer * sin(angle)).toFloat()
            drawLine(
                color = tickColor,
                start = Offset(sx, sy),
                end = Offset(ex, ey),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }

        // Numerals
        if (showNumerals) {
            val textRadius = radius * 0.70f
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.argb(
                        (numeralColor.alpha * 255).toInt(),
                        (numeralColor.red * 255).toInt(),
                        (numeralColor.green * 255).toInt(),
                        (numeralColor.blue * 255).toInt()
                    )
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = radius * 0.14f
                }
                for (h in 1..12) {
                    val ang = Math.toRadians(h * 30.0 - 90.0)
                    val tx = (center.x + textRadius * cos(ang)).toFloat()
                    val ty = (center.y + textRadius * sin(ang)).toFloat() + paint.textSize / 3.5f
                    canvas.nativeCanvas.drawText(h.toString(), tx, ty, paint)
                }
            }
        }

        // Hands
        fun handEnd(angle: Double, length: Float): Offset =
            Offset(
                (center.x + length * cos(angle)).toFloat(),
                (center.y + length * sin(angle)).toFloat()
            )

        // Hour hand
        drawLine(
            color = hourMinuteColor,
            start = center,
            end = handEnd(hourAngle - Math.PI / 2.0, radius * 0.52f),
            strokeWidth = hourStroke,
            cap = StrokeCap.Round
        )

        // Minute hand
        drawLine(
            color = hourMinuteColor,
            start = center,
            end = handEnd(minuteAngle - Math.PI / 2.0, radius * 0.74f),
            strokeWidth = minuteStroke,
            cap = StrokeCap.Round
        )

        // Second hand (skip in ambient)
        if (!ambientMode) {
            drawLine(
                color = secondColor,
                start = center,
                end = handEnd(secondAngle - Math.PI / 2.0, radius * 0.82f),
                strokeWidth = secondStroke,
                cap = StrokeCap.Round
            )

            // Tail for balance
            drawLine(
                color = secondColor.copy(alpha = 0.7f),
                start = center,
                end = Offset(
                    (center.x - radius * 0.18f * cos(secondAngle - Math.PI / 2.0)).toFloat(),
                    (center.y - radius * 0.18f * sin(secondAngle - Math.PI / 2.0)).toFloat()
                ),
                strokeWidth = secondStroke,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )
        }

        // Center hub
        drawCircle(color = hourMinuteColor, radius = hourStroke * 0.9f, center = center)
        if (!ambientMode) {
            drawCircle(color = secondColor, radius = secondStroke * 2.2f, center = center)
        }
    }
}
