package com.example.devfitness.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.devfitness.ui.theme.FitnessPalette

@Composable
fun GymRings(
    modifier: Modifier = Modifier,
    tint: Color = FitnessPalette.Lime,
) {
    val transition = rememberInfiniteTransition(label = "rings")
    val pulse = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse",
    )
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        repeat(2) { index ->
            val progress = (pulse.value + index * 0.5f) % 1f
            drawCircle(
                color = tint.copy(alpha = (1f - progress) * 0.35f),
                radius = size.minDimension * (0.22f + progress * 0.28f),
                center = center,
                style = Stroke(width = size.minDimension * 0.012f),
            )
        }
    }
}

@Composable
fun GymLoader(
    modifier: Modifier = Modifier,
    tint: Color = FitnessPalette.Lime,
    hole: Color = FitnessPalette.Ink,
    glow: Color = tint.copy(alpha = 0.35f),
) {
    val transition = rememberInfiniteTransition(label = "lift")
    val lift = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1600
                0f at 0
                0.08f at 160
                1f at 520 using FastOutSlowInEasing
                0.94f at 820
                0f at 1600
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "bar",
    )
    Canvas(modifier) {
        drawDumbbell(
            lift = lift.value,
            tint = tint,
            hole = hole,
            glow = glow,
        )
    }
}

fun DrawScope.drawDumbbell(
    lift: Float,
    tint: Color,
    hole: Color,
    glow: Color = tint.copy(alpha = 0.28f),
) {
    val width = size.width
    val height = size.height
    val centerX = width / 2f
    val travel = height * 0.16f
    val centerY = height * 0.58f - travel * lift

    val barLength = width * 0.40f
    val barThickness = (height * 0.048f).coerceAtLeast(2.5f)
    val outerWidth = width * 0.078f
    val outerHeight = height * 0.34f
    val innerWidth = width * 0.052f
    val innerHeight = height * 0.22f

    val shadowScale = 1f - 0.25f * lift
    drawOval(
        color = glow.copy(alpha = glow.alpha * (0.55f + 0.45f * (1f - lift))),
        topLeft = Offset(centerX - width * 0.20f * shadowScale, height * 0.78f),
        size = Size(width * 0.40f * shadowScale, height * 0.055f),
    )

    fun plate(center: Float, plateWidth: Float, plateHeight: Float) {
        drawRoundRect(
            color = tint,
            topLeft = Offset(center - plateWidth / 2f, centerY - plateHeight / 2f),
            size = Size(plateWidth, plateHeight),
            cornerRadius = CornerRadius(plateWidth * 0.34f, plateWidth * 0.34f),
        )
        drawCircle(
            color = hole,
            radius = plateHeight * 0.14f,
            center = Offset(center, centerY),
        )
    }

    drawRoundRect(
        color = tint,
        topLeft = Offset(centerX - barLength / 2f, centerY - barThickness / 2f),
        size = Size(barLength, barThickness),
        cornerRadius = CornerRadius(barThickness, barThickness),
    )

    val innerInset = barLength / 2f - innerWidth * 0.05f
    val outerInset = barLength / 2f + outerWidth * 0.62f
    plate(centerX - innerInset, innerWidth, innerHeight)
    plate(centerX + innerInset, innerWidth, innerHeight)
    plate(centerX - outerInset, outerWidth, outerHeight)
    plate(centerX + outerInset, outerWidth, outerHeight)
}

@Composable
fun SuccessMark(
    modifier: Modifier = Modifier,
    tint: Color = FitnessPalette.Lime,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = 680, easing = FastOutSlowInEasing))
    }
    Canvas(modifier) {
        val value = progress.value
        val strokeWidth = size.minDimension * 0.055f
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        val inset = size.minDimension * 0.08f
        drawArc(
            color = tint,
            startAngle = -90f,
            sweepAngle = 360f * (value / 0.55f).coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.minDimension - inset * 2f, size.minDimension - inset * 2f),
            style = stroke,
        )
        if (value > 0.42f) {
            val trim = ((value - 0.42f) / 0.58f).coerceIn(0f, 1f)
            val check = Path().apply {
                val s = size.minDimension
                moveTo(s * 0.30f, s * 0.52f)
                lineTo(s * 0.44f, s * 0.66f)
                lineTo(s * 0.72f, s * 0.36f)
            }
            val measure = PathMeasure()
            measure.setPath(check, false)
            val segment = Path()
            measure.getSegment(0f, measure.length * trim, segment, true)
            drawPath(segment, tint, style = stroke)
        }
    }
}

@Composable
fun ErrorMark(
    modifier: Modifier = Modifier,
    tint: Color = FitnessPalette.Danger,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = 420, easing = FastOutSlowInEasing))
    }
    Canvas(modifier) {
        val value = progress.value
        val stroke = Stroke(width = size.minDimension * 0.055f, cap = StrokeCap.Round)
        val inset = size.minDimension * 0.08f
        drawArc(
            color = tint,
            startAngle = -90f,
            sweepAngle = 360f * value,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.minDimension - inset * 2f, size.minDimension - inset * 2f),
            style = stroke,
        )
        if (value > 0.45f) {
            val arm = size.minDimension * 0.16f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawLine(tint, center - Offset(arm, arm), center + Offset(arm, arm), stroke.width, StrokeCap.Round)
            drawLine(tint, center + Offset(-arm, arm), center + Offset(arm, -arm), stroke.width, StrokeCap.Round)
        }
    }
}
