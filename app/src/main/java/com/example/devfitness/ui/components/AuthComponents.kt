package com.example.devfitness.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import com.example.devfitness.R
import com.example.devfitness.ui.motion.iosAppear
import com.example.devfitness.ui.theme.FitnessPalette
import com.example.devfitness.ui.theme.Outfit

private val FieldShape = RoundedCornerShape(14.dp)
private val ButtonShape = RoundedCornerShape(16.dp)

@Composable
fun FitnessBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val drift = rememberInfiniteTransition(label = "ambient")
    val shift = drift.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "drift",
    )
    Box(
        modifier
            .fillMaxSize()
            .background(FitnessPalette.Ink),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            fun floatingDumbbell(center: Offset, length: Float, angle: Float, alpha: Float) {
                rotate(angle, center) {
                    val plateHeight = length * 0.34f
                    val plateWidth = length * 0.10f
                    drawLine(
                        color = FitnessPalette.Lime.copy(alpha = alpha),
                        start = Offset(center.x - length * 0.34f, center.y),
                        end = Offset(center.x + length * 0.34f, center.y),
                        strokeWidth = length * 0.045f,
                        cap = StrokeCap.Round,
                    )
                    listOf(-0.39f, -0.29f, 0.29f, 0.39f).forEachIndexed { index, offset ->
                        val height = if (index == 0 || index == 3) plateHeight else plateHeight * 0.72f
                        drawRoundRect(
                            color = FitnessPalette.Lime.copy(alpha = alpha),
                            topLeft = Offset(center.x + length * offset - plateWidth / 2f, center.y - height / 2f),
                            size = androidx.compose.ui.geometry.Size(plateWidth, height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(plateWidth / 2f),
                        )
                    }
                }
            }
            val topCenter = Offset(
                size.width * (0.86f - 0.1f * shift.value),
                size.height * 0.04f,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(FitnessPalette.Lime.copy(alpha = 0.18f), Color.Transparent),
                    center = topCenter,
                    radius = size.minDimension * 0.72f,
                ),
                radius = size.minDimension * 0.72f,
                center = topCenter,
            )
            val bottomCenter = Offset(
                size.width * (0.08f + 0.12f * shift.value),
                size.height * 0.96f,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF3A4614).copy(alpha = 0.55f), Color.Transparent),
                    center = bottomCenter,
                    radius = size.minDimension * 0.55f,
                ),
                radius = size.minDimension * 0.55f,
                center = bottomCenter,
            )
            floatingDumbbell(
                center = Offset(size.width * 0.84f, size.height * (0.24f + shift.value * 0.025f)),
                length = size.minDimension * 0.24f,
                angle = -18f + shift.value * 8f,
                alpha = 0.13f,
            )
            floatingDumbbell(
                center = Offset(size.width * 0.14f, size.height * (0.66f - shift.value * 0.025f)),
                length = size.minDimension * 0.18f,
                angle = 22f - shift.value * 7f,
                alpha = 0.09f,
            )
        }
        content()
    }
}

@Composable
fun AuthScroll(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.ime)),
    ) {
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 18.dp, bottom = 28.dp),
        ) {
            content()
        }
    }
}

@Composable
fun AppMark(
    modifier: Modifier = Modifier,
    animated: Boolean = false,
) {
    val transition = rememberInfiniteTransition(label = "mark")
    val breathe = transition.animateFloat(
        initialValue = 1f,
        targetValue = if (animated) 1.055f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowIn),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )
    val halo = transition.animateFloat(
        initialValue = if (animated) 0.16f else 0.08f,
        targetValue = if (animated) 0.42f else 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowIn),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "halo",
    )
    Box(
        modifier = modifier
            .scale(breathe.value)
            .shadow(20.dp, CircleShape, clip = false),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = FitnessPalette.Lime.copy(alpha = halo.value),
                radius = size.minDimension * 0.49f,
            )
        }
        Image(
            painter = painterResource(R.drawable.club_emblem),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(if (animated) 4.dp else 2.dp).clip(CircleShape),
        )
    }
}

private val FastOutSlowIn = androidx.compose.animation.core.FastOutSlowInEasing

@Composable
fun IosBackButton(onClick: () -> Unit, contentDescription: String) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "back-scale",
    )
    Box(
        modifier = Modifier
            .scale(scale)
            .size(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(FitnessPalette.InkRaised)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        ChevronLeftIcon(
            tint = FitnessPalette.Label,
            modifier = Modifier.size(22.dp),
            contentDescription = contentDescription,
        )
    }
}

@Composable
fun IosField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: @Composable (Color) -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailing: @Composable (() -> Unit)? = null,
    contentType: ContentType? = null,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val border = when {
        error != null -> FitnessPalette.Danger
        focused -> FitnessPalette.Lime
        else -> FitnessPalette.Separator
    }
    val iconTint = when {
        error != null -> FitnessPalette.Danger
        focused -> FitnessPalette.Lime
        else -> FitnessPalette.Secondary
    }
    var shownError by remember { mutableStateOf(error) }
    if (error != null) shownError = error

    Column(modifier) {
        androidx.compose.material3.Text(
            text = label,
            color = if (error != null) FitnessPalette.Danger else FitnessPalette.Secondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(FieldShape)
                .background(FitnessPalette.InkRaised)
                .border(1.dp, border, FieldShape)
                .then(
                    if (contentType != null) {
                        Modifier.semantics { this.contentType = contentType }
                    } else {
                        Modifier
                    },
                ),
            enabled = enabled,
            textStyle = TextStyle(
                color = FitnessPalette.Label,
                fontSize = 17.sp,
                fontFamily = Outfit,
            ),
            singleLine = true,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            cursorBrush = SolidColor(FitnessPalette.Lime),
            interactionSource = interaction,
            decorationBox = { inner ->
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                        icon(iconTint)
                    }
                    Spacer(Modifier.size(10.dp))
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            androidx.compose.material3.Text(
                                text = placeholder,
                                color = FitnessPalette.Tertiary,
                                fontSize = 17.sp,
                            )
                        }
                        inner()
                    }
                    if (trailing != null) {
                        Spacer(Modifier.size(8.dp))
                        trailing()
                    }
                }
            },
        )
        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn(tween(160)) + slideInVertically { -it / 2 },
            exit = fadeOut(tween(120)) + slideOutVertically { -it / 2 },
        ) {
            androidx.compose.material3.Text(
                text = shownError.orEmpty(),
                color = FitnessPalette.Danger,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp),
            )
        }
    }
}

@Composable
fun IosButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val view = LocalView.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val active = enabled && !loading
    val scale by animateFloatAsState(
        targetValue = if (pressed && active) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 520f),
        label = "button-scale",
    )
    val background = if (active || loading) FitnessPalette.Lime else FitnessPalette.InkPressed
    val content = if (active || loading) FitnessPalette.OnLime else FitnessPalette.Tertiary
    Box(
        modifier
            .scale(scale)
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .then(
                if (active) {
                    Modifier.shadow(
                        elevation = 14.dp,
                        shape = ButtonShape,
                        ambientColor = FitnessPalette.Lime.copy(alpha = 0.22f),
                        spotColor = FitnessPalette.Lime.copy(alpha = 0.38f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(ButtonShape)
            .background(background)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = active,
                role = Role.Button,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            GymLoader(
                modifier = Modifier.size(28.dp),
                tint = FitnessPalette.OnLime,
                hole = FitnessPalette.Lime,
                glow = Color.Transparent,
            )
        } else {
            androidx.compose.material3.Text(
                text = text,
                color = content,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun FooterLink(
    prefix: String,
    action: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Text(
            text = prefix,
            color = FitnessPalette.Secondary,
            fontSize = 15.sp,
        )
        androidx.compose.material3.Text(
            text = action,
            color = FitnessPalette.Lime.copy(alpha = if (pressed) 0.65f else 1f),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(start = 6.dp)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ),
        )
    }
}

@Composable
fun NoticeBanner(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FitnessPalette.SuccessWash)
            .border(1.dp, FitnessPalette.Success.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .iosAppear(0),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SuccessMark(modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(10.dp))
        androidx.compose.material3.Text(
            text = text,
            color = FitnessPalette.Label,
            fontSize = 14.sp,
            lineHeight = 18.sp,
        )
    }
}
