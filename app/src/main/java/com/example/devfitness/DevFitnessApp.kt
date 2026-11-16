package com.example.devfitness

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.devfitness.data.AuthResult
import com.example.devfitness.data.AuthStore
import com.example.devfitness.data.SessionStore
import com.example.devfitness.ui.auth.LoginScreen
import com.example.devfitness.ui.auth.RegisterScreen
import com.example.devfitness.ui.dashboard.RoleDashboard
import com.example.devfitness.ui.splash.SplashScreen

private val IosPush = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

private object Routes {
    const val Splash = "splash"
    const val Login = "login"
    const val Register = "register"
    const val Dashboard = "dashboard"
}

@Composable
fun DevFitnessApp(authStore: AuthStore, sessionStore: SessionStore) {
    val nav = rememberNavController()
    var session by remember { mutableStateOf(sessionStore.load()) }
    NavHost(
        navController = nav,
        startDestination = if (session == null) Routes.Splash else Routes.Dashboard,
    ) {
        composable(
            route = Routes.Splash,
            exitTransition = {
                fadeOut(tween(380)) + slideOutVertically(tween(420, easing = IosPush)) { -it / 10 }
            },
        ) {
            SplashScreen(
                onFinished = {
                    nav.navigate(Routes.Login) {
                        popUpTo(Routes.Splash) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = Routes.Login,
            enterTransition = {
                fadeIn(tween(420)) + slideInVertically(
                    animationSpec = tween(480, easing = IosPush),
                ) { it / 8 }
            },
            exitTransition = {
                fadeOut(tween(220)) + slideOutHorizontally(tween(460, easing = IosPush)) { -it / 4 }
            },
            popEnterTransition = {
                fadeIn(tween(220)) + slideInHorizontally(tween(460, easing = IosPush)) { -it / 4 }
            },
        ) { entry ->
            val prefill by entry.savedStateHandle.getStateFlow("email", "").collectAsState()
            val notice by entry.savedStateHandle.getStateFlow("notice", "").collectAsState()
            LoginScreen(
                authStore = authStore,
                prefillEmail = prefill,
                notice = notice,
                onConsumeNotice = { entry.savedStateHandle["notice"] = "" },
                onRegister = { nav.navigate(Routes.Register) },
                onLoginSuccess = { user ->
                    session = user
                    sessionStore.save(user)
                    nav.navigate(Routes.Dashboard) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = Routes.Register,
            enterTransition = {
                slideInHorizontally(tween(460, easing = IosPush)) { it } + fadeIn(tween(200))
            },
            popExitTransition = {
                slideOutHorizontally(tween(420, easing = IosPush)) { it } + fadeOut(tween(160))
            },
        ) {
            RegisterScreen(
                authStore = authStore,
                onBack = { nav.popBackStack() },
                onRegistered = { email, notice ->
                    nav.previousBackStackEntry?.savedStateHandle?.set("email", email)
                    nav.previousBackStackEntry?.savedStateHandle?.set("notice", notice)
                    nav.popBackStack()
                },
            )
        }
        composable(
            route = Routes.Dashboard,
            enterTransition = { fadeIn(tween(300)) },
            exitTransition = { fadeOut(tween(180)) },
        ) {
            val user = session
            if (user == null) {
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    nav.navigate(Routes.Login) {
                        popUpTo(Routes.Dashboard) { inclusive = true }
                    }
                }
            } else {
                RoleDashboard(
                    name = user.name,
                    email = user.email,
                    role = user.role,
                    token = user.token,
                    onLogout = {
                        session = null
                        sessionStore.clear()
                        nav.navigate(Routes.Login) {
                            popUpTo(Routes.Dashboard) { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}
