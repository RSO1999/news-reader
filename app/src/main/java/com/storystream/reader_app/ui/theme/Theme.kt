package com.storystream.reader_app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface

// ---- Extended color tokens accessible via LocalAppColors ----
data class AppColors(
    val sectionBadge: Color,
    val save: Color,
    val tabActive: Color,
    val tabInactive: Color,
    val premium: Color,
    val premiumForeground: Color,
    val surfaceElevated: Color
)

val LightAppColors = AppColors(
    sectionBadge = PrimaryLight,
    save = TertiaryLight,
    tabActive = PrimaryLight,
    tabInactive = OnSurfaceVariantLight,
    premium = PremiumColor,
    premiumForeground = PremiumForeground,
    surfaceElevated = SurfaceElevatedLight
)

val DarkAppColors = AppColors(
    sectionBadge = PrimaryDark,
    save = TertiaryDark,
    tabActive = PrimaryDark,
    tabInactive = OnSurfaceVariantDark,
    premium = PremiumColor,
    premiumForeground = PremiumForeground,
    surfaceElevated = SurfaceElevatedDark
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

// ---- Material Color Schemes ----
private val LightColors: ColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    secondary = SecondaryLight,
    onSecondary = OnPrimaryLight,
    tertiary = TertiaryLight,
    onTertiary = OnPrimaryLight,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    error = Color(0xFFD32F2F),
    onError = Color.White
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    secondary = SecondaryDark,
    onSecondary = OnPrimaryDark,
    tertiary = TertiaryDark,
    onTertiary = OnPrimaryDark,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    error = Color(0xFFEF5350),
    onError = Color.White
)

// ---- Shapes ----
private val AppShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val appColors = if (darkTheme) DarkAppColors else LightAppColors

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}

// Minimal helper composable for usage in previews or sample screens
@Suppress("unused")
@Composable
fun ThemedSurface(content: @Composable () -> Unit) {
    Surface {
        content()
    }
}
