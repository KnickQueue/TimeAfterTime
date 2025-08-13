package com.example.kronosclock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnalogClock(
    modifier: Modifier = Modifier,
    clockColor: Color = Color.Black,
    handColor: Color = Color.Black,
    secondHandColor: Color = Color.Red
) {
    var time by remember { mutableStateOf(LocalTime.now(ZoneId.systemDefault())) }

    LaunchedEffect(Unit) {
        while (true) {
            time = LocalTime.now(ZoneId.systemDefault())
            delay(1000L)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 * 0.9f

        // Draw clock circle
        drawCircle(
            color = clockColor,
            center = center,
            radius = radius,
            style = Stroke(width = 4.dp.toPx())
        )

        val second = time.second
        val minute = time.minute
        val hour = time.hour % 12

        val secondAngle = (second * 6) * (PI / 180)
        val minuteAngle = ((minute + second / 60.0) * 6) * (PI / 180)
        val hourAngle = ((hour + minute / 60.0) * 30) * (PI / 180)

        val secondHand = Offset(
            (center.x + radius * 0.9f * cos(secondAngle)).toFloat(),
            (center.y + radius * 0.9f * sin(secondAngle)).toFloat()
        )
        val minuteHand = Offset(
            (center.x + radius * 0.7f * cos(minuteAngle)).toFloat(),
            (center.y + radius * 0.7f * sin(minuteAngle)).toFloat()
        )
        val hourHand = Offset(
            (center.x + radius * 0.5f * cos(hourAngle)).toFloat(),
            (center.y + radius * 0.5f * sin(hourAngle)).toFloat()
        )

        drawLine(secondHandColor, center, secondHand, strokeWidth = 2f, cap = StrokeCap.Round)
        drawLine(handColor, center, minuteHand, strokeWidth = 5f, cap = StrokeCap.Round)
        drawLine(handColor, center, hourHand, strokeWidth = 8f, cap = StrokeCap.Round)
    }
}
