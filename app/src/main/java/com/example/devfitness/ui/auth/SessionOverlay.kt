package com.example.devfitness.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.devfitness.R
import com.example.devfitness.ui.components.AppMark
import com.example.devfitness.ui.components.ErrorMark
import com.example.devfitness.ui.components.GymLoader
import com.example.devfitness.ui.components.GymRings
import com.example.devfitness.ui.components.IosButton
import com.example.devfitness.ui.components.SuccessMark
import com.example.devfitness.ui.theme.FitnessPalette
import kotlinx.coroutines.delay

enum class SessionPhase {
    Lifting,
    Success,
    Error,
}

data class SessionOverlay(
    val phase: SessionPhase,
    val title: String,
    val detail: String,
)

@Composable
fun GymSessionOverlay(
    model: SessionOverlay,
    onTryAgain: () -> Unit,
    onFinished: () -> Unit,
) {
    val phrases = stringArrayResource(R.array.lift_phrases)
    var phraseIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(model.phase) {
        if (model.phase == SessionPhase.Success) {
            delay(1500)
            onFinished()
        }
    }
    LaunchedEffect(model.phase) {
        if (model.phase != SessionPhase.Lifting) return@LaunchedEffect
        while (true) {
            delay(700)
            phraseIndex = (phraseIndex + 1) % phrases.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(104.dp)) {
                if (model.phase == SessionPhase.Lifting) GymRings(Modifier.fillMaxSize())
                AppMark(modifier = Modifier.size(78.dp), animated = model.phase == SessionPhase.Lifting)
            }
            Spacer(Modifier.height(4.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(168.dp)) {
                when (model.phase) {
                    SessionPhase.Lifting -> {
                        GymRings(Modifier.fillMaxSize())
                        GymLoader(Modifier.size(108.dp))
                    }
                    SessionPhase.Success -> SuccessMark(Modifier.size(96.dp))
                    SessionPhase.Error -> ErrorMark(Modifier.size(96.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = model.title,
                color = FitnessPalette.Label,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.4).sp,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.Center) {
                if (model.phase == SessionPhase.Lifting) {
                    AnimatedContent(
                        targetState = phrases[phraseIndex],
                        transitionSpec = {
                            (fadeIn(tween(180)) + slideInVertically { it / 2 }) togetherWith
                                (fadeOut(tween(140)) + slideOutVertically { -it / 2 })
                        },
                        label = "lift-phrase",
                    ) { phrase ->
                        Text(
                            text = phrase,
                            color = FitnessPalette.Secondary,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    Text(
                        text = model.detail,
                        color = FitnessPalette.Secondary,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            if (model.phase == SessionPhase.Error) {
                Spacer(Modifier.height(28.dp))
                IosButton(text = androidx.compose.ui.res.stringResource(R.string.try_again), onClick = onTryAgain)
            }
        }
    }
}
