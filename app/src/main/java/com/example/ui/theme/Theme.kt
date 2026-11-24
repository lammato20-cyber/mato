package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PulseDarkColorScheme = darkColorScheme(
    primary = PulseCoral,
    onPrimary = Color.White,
    primaryContainer = PulseCoralContainer,
    onPrimaryContainer = OnPulseCoralContainer,
    secondary = ArcadeMint,
    onSecondary = Color(0xFF002418),
    secondaryContainer = ArcadeMintContainer,
    onSecondaryContainer = OnArcadeMintContainer,
    tertiary = StreamCyan,
    onTertiary = Color(0xFF002233),
    tertiaryContainer = StreamCyanContainer,
    onTertiaryContainer = OnStreamCyanContainer,
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = StudioSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLow = StudioSurface,
    surfaceContainer = StudioSurfaceElevated,
    surfaceContainerHigh = StudioSurfaceBright,
    outline = StudioOutline,
    error = Color(0xFFFF6B6B),
    onError = Color.Black
)

private val PulseLightColorScheme = lightColorScheme(
    primary = Color(0xFFE11D48),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE4E6),
    onPrimaryContainer = Color(0xFF4C0519),
    secondary = Color(0xFF059669),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF022C22),
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0F2FE),
    onTertiaryContainer = Color(0xFF082F49),
    background = LightBg,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFCBD5E1)
)

@Composable
fun PulseDeckTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) PulseDarkColorScheme else PulseLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PulseTypography,
        content = content
    )
}
