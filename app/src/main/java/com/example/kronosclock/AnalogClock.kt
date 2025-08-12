// FILE 9: app/src/main/java/com/example/kronosanalogclock/AnalogClock.kt
package com.example.kronosanalogclock

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.delay
import kotlin.math.*
import java.time.Instant
import java.time.ZoneId

@Composable
fun AnalogClock(
    modifier: Modifier = Modifier,
    timeSourceMs: () -> Long,
    zoneId: ZoneId,
    showNumerals: Boolean,
    ambientMode: Boolean,
    accentColor: Color
) {
    val nowMs = remember { mutableLongStateOf(timeSourceMs()) }

    LaunchedEffect(ambientMode) {
        while (true) {
            if (ambientMode) {
                val ms = timeSourceMs()
                val untilNextMinute = 60_000L - (ms % 60_000L)
                nowMs.longValue = ms
                delay(untilNextMinute.coerceIn(500L, 60_000L))
            } else {
                withFrameNanos { nowMs.longValue = timeSourceMs() }
            }
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = min(cx, cy) * 0.92f

        val onBg = if (ambientMode) Color.Gray.copy(alpha = 0.75f) else Color.Black.copy(alpha = 0.85f)
        val faceStroke = if (ambientMode) onBg else onBg.copy(alpha = 0.9f)
        val tickColor = if (ambientMode) onBg.copy(alpha = 0.6f) else onBg
        val hourMinColor = onBg
        val secondColor = if (ambientMode) Color.Transparent else accentColor

        drawCircle(style = Stroke(width = radius * 0.02f), color = faceStroke)

        for (i in 0 until 60) {
            val a = angleFor(i.toFloat(), 60f)
            val outer = pointOnCircle(cx, cy, radius, a)
            val inner = pointOnCircle(cx, cy, radius * if (i % 5 == 0) 0.86f else 0.93f, a)
            drawLine(
                start = inner, end = outer,
                color = tickColor,
                strokeWidth = radius * if (i % 5 == 0) 0.02f else 0.008f
            )
        }

        if (showNumerals) drawNumerals(cx, cy, radius * 0.74f, onBg)

        val ms = nowMs.longValue
        val zoned = Instant.ofEpochMilli(ms).atZone(zoneId)
        val seconds = zoned.second.toFloat() + zoned.nano / 1_000_000_000f
        val minutes = zoned.minute.toFloat() + seconds / 60f
        val hours = (zoned.hour % 12).toFloat() + minutes / 60f

        val hourLen = radius * 0.55f
        val minLen  = radius * 0.75f
        val secLen  = radius * 0.85f

        drawLine(
            start = Offset(cx, cy),
            end = pointOnCircle(cx, cy, hourLen, angleFor(hours, 12f)),
            color = hourMinColor,
            strokeWidth = radius * 0.04f,
            cap = StrokeCap.Round
        )
        drawLine(
            start = Offset(cx, cy),
            end = pointOnCircle(cx, cy, minLen, angleFor(minutes, 60f)),
            color = hourMinColor,
            strokeWidth = radius * 0.03f,
            cap = StrokeCap.Round
        )

        if (secondColor.alpha > 0f) {
            val sAngle = angleFor(seconds, 60f)
            val tip = pointOnCircle(cx, cy, secLen, sAngle)
            val tail = pointOnCircle(cx, cy, radius * 0.22f, sAngle + PI.toFloat())
            drawLine(
                start = tail, end = tip,
                color = secondColor,
                strokeWidth = radius * 0.012f,
                cap = StrokeCap.Round
            )
            drawCircle(color = secondColor, radius = radius * 0.028f, center = tail)
            drawCircle(color = secondColor, radius = radius * 0.010f, center = tip)
        }

        drawCircle(color = hourMinColor, radius = radius * 0.035f, center = Offset(cx, cy))
        drawCircle(color = accentColor.copy(alpha = if (ambientMode) 0.3f else 0.9f),
            radius = radius * 0.018f, center = Offset(cx, cy))
    }
}

private fun angleFor(unit: Float, max: Float): Float =
    ((unit / max) * 360f - 90f).toRadians()

private fun Float.toRadians(): Float = (this * Math.PI / 180.0).toFloat()

private fun pointOnCircle(cx: Float, cy: Float, r: Float, angle: Float): Offset =
    Offset(cx + r * cos(angle), cy + r * sin(angle))

private fun DrawScope.drawNumerals(cx: Float, cy: Float, radius: Float, color: Color) {
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
        textSize = radius * 0.18f
        this.color = color.toArgb()
        typeface = android.graphics.Typeface.SANS_SERIF
    }
    drawIntoCanvas { c ->
        val nc = c.nativeCanvas
        for (i in 1..12) {
            val angle = angleFor(i.toFloat(), 12f)
            val pos = pointOnCircle(cx, cy, radius, angle)
            val textY = pos.y + (paint.textSize * 0.35f)
            nc.drawText(i.toString(), pos.x, textY, paint)
        }
    }
}

