package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = ClinicalTeal,
    onPrimary = Color.White,
    primaryContainer = MintContainer,
    onPrimaryContainer = OnMintContainer,
    secondary = OceanSecondary,
    onSecondary = Color.White,
    secondaryContainer = OceanContainer,
    onSecondaryContainer = OnOceanContainer,
    tertiary = WarmAmberTertiary,
    onTertiary = Color.White,
    tertiaryContainer = WarmAmberContainer,
    onTertiaryContainer = OnWarmAmberContainer,
    background = SurfacePorcelain,
    onBackground = OnSurfaceInk,
    surface = SurfacePorcelain,
    onSurface = OnSurfaceInk,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceMuted,
    surfaceContainerLowest = SurfaceCardLight,
    surfaceContainerLow = Color(0xFFF0F7F6),
    surfaceContainer = Color(0xFFE9F3F1),
    outline = OutlineSubtle,
    error = RiskRedFg,
    onError = Color.White,
    errorContainer = RiskRedBg,
    onErrorContainer = RiskRedFg
)

private val DarkColors = darkColorScheme(
    primary = LuminousTeal,
    onPrimary = Color(0xFF003733),
    primaryContainer = DarkMintContainer,
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFF81D4FA),
    onSecondary = Color(0xFF003547),
    secondaryContainer = Color(0xFF10485C),
    onSecondaryContainer = Color(0xFFD6EEF6),
    tertiary = Color(0xFFFFB74D),
    onTertiary = Color(0xFF422200),
    tertiaryContainer = Color(0xFF5D3404),
    onTertiaryContainer = Color(0xFFFFE8CC),
    background = DarkSurface,
    onBackground = OnDarkSurface,
    surface = DarkSurface,
    onSurface = OnDarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkMuted,
    surfaceContainerLowest = DarkCard,
    surfaceContainerLow = Color(0xFF122326),
    surfaceContainer = Color(0xFF172C30),
    outline = Color(0xFF2B4B50),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF5C0B0B),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

val YaqeenShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun YaqeenHealthTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isRtl: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = YaqeenShapes,
            content = content
        )
    }
}
