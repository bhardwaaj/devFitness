package com.example.devfitness.ui.auth

import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import com.example.devfitness.ui.components.IosBackButton
import com.example.devfitness.ui.components.IosButton
import com.example.devfitness.ui.components.IosField
import com.example.devfitness.ui.components.LockIcon
import com.example.devfitness.ui.components.PersonIcon
import com.example.devfitness.ui.motion.iosAppear
import com.example.devfitness.ui.motion.iosShake
import com.example.devfitness.ui.theme.FitnessPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    authStore: AuthStore,
    onBack: () -> Unit,
    onRegistered: (email: String, notice: String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmError by rememberSaveable { mutableStateOf<String?>(null) }
    var shake by remember { mutableIntStateOf(0) }
    var overlay by remember { mutableStateOf<SessionOverlay?>(null) }
    var rendered by remember { mutableStateOf<SessionOverlay?>(null) }
    if (overlay != null) rendered = overlay

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val errName = stringResource(R.string.err_name)
    val errEmailRequired = stringResource(R.string.err_email_required)
    val errEmailInvalid = stringResource(R.string.err_email_invalid)
    val errPasswordShort = stringResource(R.string.err_password_short)
    val errPasswordRequired = stringResource(R.string.err_password_required)
    val errMatch = stringResource(R.string.err_password_match)
    val settingLocker = stringResource(R.string.setting_locker)
    val youreIn = stringResource(R.string.youre_in)
    val couldnt = stringResource(R.string.couldnt_register)
    val emailTaken = stringResource(R.string.email_taken)
    val networkMessage = stringResource(R.string.err_network)
    val configMessage = stringResource(R.string.err_db_config)
    val createdBanner = stringResource(R.string.account_created_banner)
    val showPassword = stringResource(R.string.show_password)
    val hidePassword = stringResource(R.string.hide_password)
    val fallbackName = stringResource(R.string.app_name)
    val firstName = name.trim().substringBefore(' ').ifBlank { name.trim() }.ifBlank { fallbackName }
    val welcomeDetail = stringResource(R.string.welcome_name, firstName)
    val passwordStrength = when {
        password.isEmpty() -> "Use 8+ characters with a number and symbol"
        password.length < 8 -> "Password strength: weak"
        password.any(Char::isDigit) && password.any { !it.isLetterOrDigit() } -> "Password strength: strong"
        else -> "Password strength: medium"
    }

    BackHandler(enabled = overlay != null) {
        if (overlay?.phase == SessionPhase.Error) overlay = null
    }

    fun submit() {
        if (overlay != null) return
        val nextName = if (name.trim().length < 2) errName else null
        val nextEmail = when {
            email.isBlank() -> errEmailRequired
            !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> errEmailInvalid
            else -> null
        }
        val nextPassword = when {
            password.isBlank() -> errPasswordRequired
            password.length < 6 -> errPasswordShort
            else -> null
        }
        val nextConfirm = when {
            confirm.isBlank() -> errMatch
            confirm != password -> errMatch
            else -> null
        }
        nameError = nextName
        emailError = nextEmail
        passwordError = nextPassword
        confirmError = nextConfirm
        if (listOf(nextName, nextEmail, nextPassword, nextConfirm).any { it != null }) {
            shake += 1
            return
        }
        val registeredEmail = email.trim()
        focusManager.clearFocus()
        scope.launch {
            overlay = SessionOverlay(SessionPhase.Lifting, settingLocker, "")
            val result = coroutineScope {
                val floor = async { delay(1100) }
                val auth = async(Dispatchers.IO) { authStore.register(name, email, password) }
                floor.await()
                auth.await()
            }
            when (result) {
                is AuthResult.Success -> {
                    overlay = SessionOverlay(SessionPhase.Success, youreIn, welcomeDetail)
                    delay(1500)
                    onRegistered(registeredEmail, createdBanner)
                }
                is AuthResult.Failure -> {
                    val detail = when (result.reason) {
                        AuthFailure.EmailTaken -> emailTaken
                        AuthFailure.InvalidCredentials -> emailTaken
                        AuthFailure.Unavailable -> networkMessage
                        AuthFailure.NotConfigured -> configMessage
                        AuthFailure.Inactive -> networkMessage
                    }
                    overlay = SessionOverlay(SessionPhase.Error, couldnt, detail)
                }
            }
        }
    }

    FitnessBackground {
        AuthScroll {
            IosBackButton(
                onClick = onBack,
                contentDescription = stringResource(R.string.back),
            )
            Spacer(Modifier.height(18.dp))
            AppMark(modifier = Modifier.size(92.dp).iosAppear(0), animated = true)
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.create_account_title),
                color = FitnessPalette.Label,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.7).sp,
                modifier = Modifier.iosAppear(1),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.create_subtitle),
                color = FitnessPalette.Secondary,
                fontSize = 16.sp,
                modifier = Modifier.iosAppear(2),
            )
            Spacer(Modifier.height(28.dp))
            IosField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = null
                },
                label = stringResource(R.string.name),
                placeholder = stringResource(R.string.name_placeholder),
                icon = { tint -> PersonIcon(tint, Modifier.fillMaxSize()) },
                error = nameError,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                contentType = ContentType.PersonFullName,
                enabled = overlay == null,
                modifier = Modifier.iosAppear(3).iosShake(shake),
            )
            Spacer(Modifier.height(16.dp))
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
                modifier = Modifier.iosAppear(4).iosShake(shake),
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
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                trailing = {
                    PasswordEye(
                        visible = passwordVisible,
                        description = if (passwordVisible) hidePassword else showPassword,
                        onClick = { passwordVisible = !passwordVisible },
                    )
                },
                contentType = ContentType.NewPassword,
                enabled = overlay == null,
                modifier = Modifier.iosAppear(5).iosShake(shake),
            )
            if (passwordError == null) {
                Text(
                    text = passwordStrength,
                    color = if (
                        password.length >= 8 &&
                        password.any(Char::isDigit) &&
                        password.any { !it.isLetterOrDigit() }
                    ) FitnessPalette.Lime else FitnessPalette.Tertiary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            IosField(
                value = confirm,
                onValueChange = {
                    confirm = it
                    confirmError = null
                },
                label = stringResource(R.string.confirm_password),
                placeholder = stringResource(R.string.confirm_placeholder),
                icon = { tint -> LockIcon(tint, Modifier.fillMaxSize()) },
                error = confirmError,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                contentType = ContentType.NewPassword,
                enabled = overlay == null,
                modifier = Modifier.iosAppear(6).iosShake(shake),
            )
            Spacer(Modifier.height(28.dp))
            IosButton(
                text = stringResource(R.string.join),
                onClick = ::submit,
                enabled = name.isNotBlank() && email.isNotBlank() && password.isNotBlank() && confirm.isNotBlank(),
                loading = overlay?.phase == SessionPhase.Lifting,
                modifier = Modifier.iosAppear(7),
            )
            Spacer(Modifier.height(18.dp))
            FooterLink(
                prefix = stringResource(R.string.have_account),
                action = stringResource(R.string.sign_in_link),
                onClick = onBack,
                modifier = Modifier.iosAppear(8),
            )
        }
        AnimatedVisibility(
            visible = overlay != null,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.fillMaxSize(),
        ) {
            rendered?.let { model ->
                GymSessionOverlay(
                    model = model,
                    onTryAgain = { overlay = null },
                    onFinished = {},
                )
            }
        }
    }
}

@Composable
private fun PasswordEye(visible: Boolean, description: String, onClick: () -> Unit) {
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
