package com.sergiom.thebestdamkebap.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = BrandAmber,
    onPrimary = BrandBlack,

    secondary = BrandAmber,
    onSecondary = BrandBlack,

    tertiary = BrandAmberDark,
    onTertiary = BrandBlack,

    primaryContainer = BrandWhite,

    background = BrandBlack,
    onBackground = TextOnDark,

    surface = BrandBlack,
    onSurface = TextOnDark,

    outline = OutlineBrand
)

private val LightColorScheme = lightColorScheme(
    primary = BrandAmber,
    onPrimary = BrandBlack,

    secondary = BrandAmber,
    onSecondary = BrandBlack,

    tertiary = BrandAmber,
    onTertiary = BrandBlack,

    background = BrandBlack,
    onBackground = TextOnDark,

    surface = BrandBlack,
    onSurface = TextOnDark,

    outline = OutlineBrand
)

// Formas globales (coinciden con tus pantallas)
private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp), // campos
    large = RoundedCornerShape(20.dp)   // botones
)

@Composable
fun TheBestDAMKebapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    @Suppress("unused") dynamicColor: Boolean = false, // mantenemos marca estable en Android 12+
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
