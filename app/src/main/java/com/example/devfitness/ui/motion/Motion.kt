package com.example.devfitness.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun Modifier.iosAppear(index: Int): Modifier {
    val progress = remember { Animatable(0f) }
    val density = LocalDensity.current
    val travel = with(density) { 22.dp.toPx() }
    LaunchedEffect(index) {
        delay(60L * index)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.72f,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
    }
    val value = progress.value
    return graphicsLayer {
        alpha = value.coerceIn(0f, 1f)
        translationY = (1f - value.coerceIn(0f, 1f)) * travel
    }
}

@Composable
fun Modifier.iosShake(trigger: Int): Modifier {
    val shift = remember { Animatable(0f) }
    val density = LocalDensity.current
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        shift.snapTo(0f)
        shift.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 460
                0f at 0 using FastOutSlowInEasing
                -11f at 50
                9f at 110
                -7f at 180
                4f at 260
                -2f at 340
                0f at 460
            },
        )
    }
    val pixels = with(density) { shift.value.dp.toPx() }
    return graphicsLayer { translationX = pixels }
}
