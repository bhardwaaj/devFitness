package com.example.devfitness.ui.auth

import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.devfitness.R
import com.example.devfitness.data.AuthFailure
import com.example.devfitness.data.AuthResult
import com.example.devfitness.data.AuthStore
import com.example.devfitness.ui.components.AppMark
import com.example.devfitness.ui.components.AuthScroll
import com.example.devfitness.ui.components.EnvelopeIcon
import com.example.devfitness.ui.components.EyeIcon
import com.example.devfitness.ui.components.FitnessBackground
import com.example.devfitness.ui.components.FooterLink
import com.example.devfitness.ui.components.IosButton
import com.example.devfitness.ui.components.IosField
import com.example.devfitness.ui.components.LockIcon
import com.example.devfitness.ui.components.NoticeBanner
import com.example.devfitness.ui.motion.iosAppear
import com.example.devfitness.ui.motion.iosShake
import com.example.devfitness.ui.theme.FitnessPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authStore: AuthStore,
    prefillEmail: String,
    notice: String,
    onConsumeNotice: () -> Unit,
    onRegister: () -> Unit,
    onLoginSuccess: (AuthResult.Success) -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    var shake by remember { mutableIntStateOf(0) }
    var overlay by remember { mutableStateOf<SessionOverlay?>(null) }
    var rendered by remember { mutableStateOf<SessionOverlay?>(null) }
    var banner by remember { mutableStateOf("") }
    var authenticatedUser by remember { mutableStateOf<AuthResult.Success?>(null) }
    if (overlay != null) rendered = overlay
    if (notice.isNotBlank()) banner = notice

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val emailRequired = stringResource(R.string.err_email_required)
    val emailInvalid = stringResource(R.string.err_email_invalid)
    val passwordRequired = stringResource(R.string.err_password_required)
    val checkingIn = stringResource(R.string.checking_in)
    val welcomeTitle = stringResource(R.string.welcome_back_title)
    val sessionReady = stringResource(R.string.session_ready)
    val couldnt = stringResource(R.string.couldnt_sign_in)
    val invalidCredentials = stringResource(R.string.invalid_credentials)
    val networkMessage = stringResource(R.string.err_network)
    val configMessage = stringResource(R.string.err_db_config)
    val inactiveMessage = stringResource(R.string.err_account_inactive)
    val showPassword = stringResource(R.string.show_password)
    val hidePassword = stringResource(R.string.hide_password)
    val logoDescription = stringResource(R.string.logo)

    androidx.compose.runtime.LaunchedEffect(prefillEmail) {
        if (prefillEmail.isNotBlank()) email = prefillEmail
    }
    androidx.compose.runtime.LaunchedEffect(notice) {
        if (notice.isNotBlank()) {
            delay(3800)
            onConsumeNotice()
        }
    }

    BackHandler(enabled = overlay != null) {
        if (overlay?.phase == SessionPhase.Error) overlay = null
    }

    fun submit() {
        if (overlay != null) return
        val nextEmailError = when {
            email.isBlank() -> emailRequired
            !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> emailInvalid
            else -> null
        }
        val nextPasswordError = if (password.isBlank()) passwordRequired else null
        emailError = nextEmailError
        passwordError = nextPasswordError
        if (nextEmailError != null || nextPasswordError != null) {
            shake += 1
            return
        }
        focusManager.clearFocus()
        scope.launch {
            overlay = SessionOverlay(SessionPhase.Lifting, checkingIn, "")
            val result = coroutineScope {
                val floor = async { delay(1100) }
                val auth = async(Dispatchers.IO) { authStore.login(email, password) }
                floor.await()
                auth.await()
            }
            overlay = when (result) {
                is AuthResult.Success -> SessionOverlay(SessionPhase.Success, welcomeTitle, sessionReady).also {
                    password = ""
                    authenticatedUser = result
                }
                is AuthResult.Failure -> {
                    val detail = when (result.reason) {
                        AuthFailure.InvalidCredentials, AuthFailure.EmailTaken -> invalidCredentials
                        AuthFailure.Unavailable -> networkMessage
                        AuthFailure.NotConfigured -> configMessage
                        AuthFailure.Inactive -> inactiveMessage
                    }
                    SessionOverlay(SessionPhase.Error, couldnt, detail)
                }
            }
        }
    }

    FitnessBackground {
        AuthScroll {
            AppMark(
                modifier = Modifier
                    .size(120.dp)
                    .iosAppear(0)
                    .semantics { contentDescription = logoDescription },
                animated = true,
            )
            Spacer(Modifier.height(22.dp))
            Text(
                text = stringResource(R.string.welcome_back),
                color = FitnessPalette.Label,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.7).sp,
                modifier = Modifier.iosAppear(1),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.sign_in_subtitle),
                color = FitnessPalette.Secondary,
                fontSize = 16.sp,
                modifier = Modifier.iosAppear(2),
            )
            AnimatedVisibility(
                visible = notice.isNotBlank(),
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
            ) {
                NoticeBanner(
                    text = banner,
                    modifier = Modifier.padding(top = 18.dp),
                )
            }
            Spacer(Modifier.height(28.dp))
            IosField(
                value = email,
                onValueChange = {
                    email = it
                    emailError = null
                },
                label = stringResource(R.string.email),
                placeholder = stringResource(R.string.email_placeholder),
                icon = { tint -> EnvelopeIcon(tint, Modifier.fillMaxSize()) },
                error = emailError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                contentType = ContentType.EmailAddress,
                enabled = overlay == null,
                modifier = Modifier.iosAppear(3).iosShake(shake),
            )
            Spacer(Modifier.height(16.dp))
            IosField(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = null
                },
                label = stringResource(R.string.password),
                placeholder = stringResource(R.string.password_placeholder),
                icon = { tint -> LockIcon(tint, Modifier.fillMaxSize()) },
                error = passwordError,
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                trailing = {
                    EyeToggle(
                        visible = passwordVisible,
                        description = if (passwordVisible) hidePassword else showPassword,
                        onClick = { passwordVisible = !passwordVisible },
                    )
                },
                contentType = ContentType.Password,
                enabled = overlay == null,
                modifier = Modifier.iosAppear(4).iosShake(shake),
            )
            Spacer(Modifier.height(28.dp))
            IosButton(
                text = stringResource(R.string.sign_in),
                onClick = ::submit,
                enabled = email.isNotBlank() && password.isNotBlank(),
                loading = overlay?.phase == SessionPhase.Lifting,
                modifier = Modifier.iosAppear(5),
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Inactive accounts must be reactivated by the club administrator.",
                color = FitnessPalette.Tertiary,
                fontSize = 13.sp,
                modifier = Modifier.iosAppear(6),
            )
            Spacer(Modifier.height(18.dp))
            FooterLink(
                prefix = "New to DEV Fitness?",
                action = "Create account",
                onClick = onRegister,
                modifier = Modifier.iosAppear(7),
            )
        }
        AnimatedVisibility(
            visible = overlay != null,
            enter = fadeIn(tweenMillis(220)),
            exit = fadeOut(tweenMillis(180)),
            modifier = Modifier.fillMaxSize(),
        ) {
            rendered?.let { model ->
                GymSessionOverlay(
                    model = model,
                    onTryAgain = { overlay = null },
                    onFinished = {
                        val user = authenticatedUser
                        overlay = null
                        authenticatedUser = null
                        if (user != null) onLoginSuccess(user)
                    },
                )
            }
        }
    }
}

@Composable
private fun EyeToggle(visible: Boolean, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .semantics { contentDescription = description }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        EyeIcon(
            tint = FitnessPalette.Secondary,
            visible = visible,
            modifier = Modifier.size(22.dp),
        )
    }
}

private fun tweenMillis(millis: Int) = androidx.compose.animation.core.tween<Float>(durationMillis = millis)
