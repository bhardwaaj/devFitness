package com.example.devfitness

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.example.devfitness.data.AuthStore
import com.example.devfitness.data.SessionStore
import com.example.devfitness.ui.theme.DevFitnessTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            val authStore = remember { AuthStore() }
            val sessionStore = remember { SessionStore(applicationContext) }
            DevFitnessTheme {
                DevFitnessApp(authStore, sessionStore)
            }
        }
    }
}
