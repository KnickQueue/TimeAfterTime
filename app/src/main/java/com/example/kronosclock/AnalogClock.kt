package com.example.kronosanalogclock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
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
    // Smooth in active, 1s-ticks in ambient
    val tickMs = if (ambientMode) 1000L else 16L

    val nowMs by produceState(initialValue = timeSourceMs(), key1 = zoneId, key2 = ambientMode) {
        while (true) {
            value = timeSourceMs()
            delay(tickMs)
        }
    }

    val zoned = Instant.ofEpochMilli(nowMs).atZone(zoneId)
    val seconds = zoned.second + zoned.nano / 1_000_000_000.0
    val minutes = zoned.minute + seconds / 60.0
    val hours = (zoned.hour % 12) + minutes / 60.0

    val secondAngle = (seconds / 60.0) * (2.0 * Math.PI)
    val minuteAngle = (minutes / 60.0) * (2.0 * Math.PI)
    val hourAngle = (hours / 12.0) * (2.0 * Math.PI)

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
        val r = min(size.width, size.height) / 2f
        val c = Offset(size.width / 2f, size.height / 2f)

        // Face + ring
        drawCircle(color = faceColor, radius = r, center = c)
        drawCircle(
            color = tickColor.copy(alpha = 0.35f),
            radius = r - ringStroke / 2f,
            center = c,
            style = Stroke(width = ringStroke)
        )

        // Ticks
        repeat(60) { i ->
            val ang = Math.toRadians(i * 6.0 - 90.0)
            val hourTick = i % 5 == 0
            val inner = if (hourTick) r * 0.82f else r * 0.88f
            val outer = r * 0.96f
            val w = if (hourTick) 3.5f else 1.8f
            val sx = (c.x + inner * cos(ang)).toFloat()
            val sy = (c.y + inner * sin(ang)).toFloat()
            val ex = (c.x + outer * cos(ang)).toFloat()
            val ey = (c.y + outer * sin(ang)).toFloat()
            drawLine(
                color = tickColor,
                start = Offset(sx, sy),
                end = Offset(ex, ey),
                strokeWidth = w,
                cap = StrokeCap.Round
            )
        }

        // Numerals
        if (showNumerals) {
            val textRadius = r * 0.70f
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
                    textSize = r * 0.14f
                }
                for (h in 1..12) {
                    val ang = Math.toRadians(h * 30.0 - 90.0)
                    val x = (c.x + textRadius * cos(ang)).toFloat()
                    val y = (c.y + textRadius * sin(ang)).toFloat() + paint.textSize / 3.5f
                    canvas.nativeCanvas.drawText(h.toString(), x, y, paint)
                }
            }
        }

        fun hand(angle: Double, length: Float): Offset =
            Offset(
                (c.x + length * cos(angle)).toFloat(),
                (c.y + length * sin(angle)).toFloat()
            )

        // Hour
        drawLine(
            color = hourMinuteColor,
            start = c,
            end = hand(hourAngle - Math.PI / 2.0, r * 0.52f),
            strokeWidth = hourStroke,
            cap = StrokeCap.Round
        )
        // Minute
        drawLine(
            color = hourMinuteColor,
            start = c,
            end = hand(minuteAngle - Math.PI / 2.0, r * 0.74f),
            strokeWidth = minuteStroke,
            cap = StrokeCap.Round
        )
        // Seconds (hidden in ambient)
        if (!ambientMode) {
            drawLine(
                color = secondColor,
                start = c,
                end = hand(secondAngle - Math.PI / 2.0, r * 0.82f),
                strokeWidth = secondStroke,
                cap = StrokeCap.Round
            )
            // Tail for balance
            drawLine(
                color = secondColor.copy(alpha = 0.7f),
                start = c,
                end = Offset(
                    (c.x - r * 0.18f * cos(secondAngle - Math.PI / 2.0)).toFloat(),
                    (c.y - r * 0.18f * sin(secondAngle - Math.PI / 2.0)).toFloat()
                ),
                strokeWidth = secondStroke,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )
        }

        // Center hub
        drawCircle(color = hourMinuteColor, radius = hourStroke * 0.9f, center = c)
        if (!ambientMode) drawCircle(color = secondColor, radius = secondStroke * 2.2f, center = c)
    }
}
