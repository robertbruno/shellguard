package dev.shellguard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Semantic colors travel through the theme so every component reacts to
// light/dark without hardcoding hex values at the call site.
@Immutable
data class StatusColors(
    val ok: Color,
    val warning: Color,
    val high: Color,
    val critical: Color,
    val info: Color,
    val neutral: Color,
)

val LocalStatusColors = staticCompositionLocalOf {
    StatusColors(
        ok = StatusOk,
        warning = StatusWarning,
        high = StatusHigh,
        critical = StatusCritical,
        info = StatusInfo,
        neutral = StatusNeutral,
    )
}

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = StatusCritical,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = StatusCriticalDark,
    onError = Color(0xFF450A0A),
)

private val LightStatus = StatusColors(
    ok = StatusOk,
    warning = StatusWarning,
    high = StatusHigh,
    critical = StatusCritical,
    info = StatusInfo,
    neutral = StatusNeutral,
)

private val DarkStatus = StatusColors(
    ok = StatusOkDark,
    warning = StatusWarningDark,
    high = StatusHighDark,
    critical = StatusCriticalDark,
    info = StatusInfoDark,
    neutral = StatusNeutralDark,
)

@Composable
fun ShellGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalStatusColors provides if (darkTheme) DarkStatus else LightStatus
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            content = content,
        )
    }
}

/** Shorthand for the semantic palette inside composables. */
val statusColors: StatusColors
    @Composable get() = LocalStatusColors.current
