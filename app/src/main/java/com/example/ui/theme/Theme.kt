package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

val TulipShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val TulipLightColorScheme = lightColorScheme(
    primary = TulipPrimary,
    onPrimary = TulipOnPrimary,
    primaryContainer = TulipPrimaryTint,
    onPrimaryContainer = TulipPrimaryPressed,
    secondary = TulipAccentClay,
    onSecondary = TulipOnAccentClay,
    secondaryContainer = TulipSurfaceVariant,
    onSecondaryContainer = TulipOnSurface,
    tertiary = TulipPrimary,
    onTertiary = TulipOnPrimary,
    background = TulipBackground,
    onBackground = TulipOnSurface,
    surface = TulipSurface,
    onSurface = TulipOnSurface,
    surfaceVariant = TulipSurfaceVariant,
    onSurfaceVariant = TulipOnSurfaceVariant,
    surfaceContainer = TulipSurfaceSubtle,
    outline = TulipOutline,
    outlineVariant = TulipOutlineVariant,
    error = TulipError,
    onError = TulipOnError,
    errorContainer = TulipErrorContainer,
    onErrorContainer = TulipOnErrorContainer
)

@Composable
fun TulipTheme(
    content: @Composable () -> Unit
) {
    // Tulip intentionally uses a light botanical palette throughout as specified in the prompt
    MaterialTheme(
        colorScheme = TulipLightColorScheme,
        typography = Typography,
        shapes = TulipShapes,
        content = content
    )
}
