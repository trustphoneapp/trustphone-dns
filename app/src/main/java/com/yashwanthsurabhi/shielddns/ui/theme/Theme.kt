package com.yashwanthsurabhi.shielddns.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = ShieldTeal,
    onPrimary = ShieldNavy,
    primaryContainer = ShieldTeal.copy(alpha = 0.18f),
    onPrimaryContainer = ShieldNavy,
    secondary = ShieldAccent,
    background = ShieldSurface,
    surface = ShieldCard,
    surfaceVariant = ShieldSurface,
    onBackground = ShieldNavy,
    onSurface = ShieldNavy,
    onSurfaceVariant = ShieldMuted,
    outline = ShieldMuted.copy(alpha = 0.35f),
)

private val DarkColors = darkColorScheme(
    primary = ShieldTeal,
    onPrimary = ShieldNavy,
    primaryContainer = ShieldTealDark.copy(alpha = 0.45f),
    onPrimaryContainer = ShieldCard,
    secondary = ShieldAccent,
    background = ShieldSurfaceDark,
    surface = ShieldCardDark,
    surfaceVariant = ShieldNavyLight,
    onBackground = ShieldCard,
    onSurface = ShieldCard,
    onSurfaceVariant = ShieldMuted,
    outline = ShieldMuted.copy(alpha = 0.4f),
)

private val ShieldTypography = Typography().let { base ->
    base.copy(
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
    )
}

@Composable
fun ShieldDnsTheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val useDark = darkTheme ?: isSystemInDarkTheme()
    val colors = if (useDark) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !useDark
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = ShieldTypography,
        shapes = ShieldShapes,
        content = content,
    )
}
