package hu.nova.mobile.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NovaLightColors = lightColorScheme(
    primary = NovaPrimary,
    onPrimary = NovaOnPrimary,
    secondary = NovaSecondary,
    error = NovaError,
    background = NovaBackgroundLight,
    surface = NovaSurfaceLight,
    onBackground = NovaTextLight,
    onSurface = NovaTextLight,
    surfaceVariant = NovaSurfaceLight,
    onSurfaceVariant = NovaTextMutedLight
)

private val NovaDarkColors = darkColorScheme(
    primary = NovaPrimary,
    onPrimary = NovaOnPrimary,
    secondary = NovaSecondary,
    error = NovaError,
    background = NovaBackgroundDark,
    surface = NovaSurfaceDark,
    onBackground = NovaTextDark,
    onSurface = NovaTextDark,
    surfaceVariant = NovaSurfaceDark,
    onSurfaceVariant = NovaTextMutedDark
)

/**
 * NOVA's Material3 theme. [dynamicColor] intentionally defaults to false: NOVA has a
 * deliberate brand palette (indigo/teal) rather than deferring to Android's wallpaper-based
 * dynamic color, per the "should not look like a generic AI chatbot" requirement.
 */
@Composable
fun NovaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> NovaDarkColors
        else -> NovaLightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NovaTypography,
        content = content
    )
}
