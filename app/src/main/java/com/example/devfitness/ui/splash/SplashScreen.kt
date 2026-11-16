package com.example.devfitness.ui.splash

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.devfitness.R
import com.example.devfitness.ui.components.AppMark
import com.example.devfitness.ui.components.FitnessBackground
import com.example.devfitness.ui.components.GymRings
import com.example.devfitness.ui.motion.iosAppear
import com.example.devfitness.ui.theme.FitnessPalette
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val phrases = stringArrayResource(R.array.lift_phrases)
    val logoDescription = stringResource(R.string.logo)
    var phraseIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        delay(2500)
        onFinished()
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(680)
            phraseIndex = (phraseIndex + 1) % phrases.size
        }
    }

    FitnessBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(248.dp).iosAppear(0)) {
                GymRings(Modifier.fillMaxSize())
                AppMark(
                    modifier = Modifier
                        .size(176.dp)
                        .semantics { contentDescription = logoDescription },
                    animated = true,
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = stringResource(R.string.tagline),
                color = FitnessPalette.Secondary,
                fontSize = 16.sp,
                modifier = Modifier.iosAppear(3),
            )
            Spacer(Modifier.height(22.dp))
            Box(modifier = Modifier.height(22.dp).iosAppear(4), contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = phrases[phraseIndex],
                    transitionSpec = {
                        (fadeIn(tween(180)) + slideInVertically { it / 2 }) togetherWith
                            (fadeOut(tween(140)) + slideOutVertically { -it / 2 })
                    },
                    label = "splash-phrase",
                ) { phrase ->
                    Text(
                        text = phrase,
                        color = FitnessPalette.Lime,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
