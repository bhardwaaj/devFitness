package com.example.devfitness.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.LocalTextStyle
import com.example.devfitness.R

/**
 * Outfit is bundled under the SIL Open Font License.
 * See app/third_party/outfit/OFL.txt.
 */
val Outfit = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
)

object FitnessPalette {
    val Ink = Color(0xFF050506)
    val InkRaised = Color(0xFF1C1C1E)
    val InkPressed = Color(0xFF2C2C2E)
    val Separator = Color(0xFF3A3A3C)
    val Label = Color(0xFFF5F5F7)
    val Secondary = Color(0xFF98989F)
    val Tertiary = Color(0xFF6E6E73)
    val Lime = Color(0xFFD6FF3F)
    val LimeDeep = Color(0xFFB6E010)
    val OnLime = Color(0xFF16180C)
    val Danger = Color(0xFFFF453A)
    val Success = Color(0xFF32D74B)
    val SuccessWash = Color(0xFF163014)
}

private val FitnessTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.7).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
)

private val FitnessColors = darkColorScheme(
    primary = FitnessPalette.Lime,
    onPrimary = FitnessPalette.OnLime,
    background = FitnessPalette.Ink,
    onBackground = FitnessPalette.Label,
    surface = FitnessPalette.InkRaised,
    onSurface = FitnessPalette.Label,
    error = FitnessPalette.Danger,
    onError = Color.White,
)

@Composable
fun DevFitnessTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FitnessColors,
        typography = FitnessTypography,
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides TextStyle(
                fontFamily = Outfit,
                color = FitnessPalette.Label,
            ),
            content = content,
        )
    }
}
