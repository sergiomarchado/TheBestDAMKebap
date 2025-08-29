package com.sergiom.thebestdamkebap.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/* =========================
   Formas globales
   ========================= */
private val AppShapes = Shapes(
    small  = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp), // campos
    large  = RoundedCornerShape(20.dp)  // botones
)

/* =========================
   Esquema “oscuro de marca” — lo usaremos en ambos modos
   Nota: definimos TODOS los roles que tocan muchos componentes
   (surfaceContainer*, surfaceVariant, outlineVariant, etc.)
   ========================= */
private fun brandDarkScheme(): ColorScheme = darkColorScheme(
    // Primarios / secundarios
    primary            = BrandAmber,
    onPrimary          = BrandBlack,
    primaryContainer   = BrandAmber,      // si usas *Container* para destacar, conserva ámbar
    onPrimaryContainer = BrandBlack,

    secondary            = BrandAmber,
    onSecondary          = BrandBlack,
    // Para FilledTonalButton por defecto preferimos gris oscuro (no ámbar) como en tus capturas
    secondaryContainer   = SurfaceContainerHigh,
    onSecondaryContainer = TextOnDark,

    tertiary            = BrandAmberDark,
    onTertiary          = BrandBlack,
    tertiaryContainer   = SurfaceContainerHigh,
    onTertiaryContainer = TextOnDark,

    // Errores (consistentes)
    error            = Error,
    onError          = OnError,
    errorContainer   = ErrorContainer,
    onErrorContainer = OnErrorContainer,

    // Fondos / superficies
    background = Surface,
    onBackground = TextOnDark,
    surface = Surface,
    onSurface = TextOnDark,

    // Variantes de superficie (listas, cards, chips)
    surfaceVariant  = SurfaceVariantDark,
    onSurfaceVariant = TextOnDark.copy(alpha = 0.75f),
    surfaceTint = BrandAmber, // influencia del “elevation overlay”

    // Capas de contenedores (Material 3 las usa muchísimo: diálogos, hojas, cards…)
    surfaceDim               = SurfaceDim,
    surfaceBright            = SurfaceBright,
    surfaceContainerLowest   = SurfaceContainerLowest,
    surfaceContainerLow      = SurfaceContainerLow,
    surfaceContainer         = SurfaceContainer,
    surfaceContainerHigh     = SurfaceContainerHigh,
    surfaceContainerHighest  = SurfaceContainerHighest,

    // Bordes / outline (usamos ámbar para tu marca)
    outline        = OutlineBrand,
    outlineVariant = OutlineBrand,

    // Inversos / scrim
    inverseSurface    = SurfaceBright,
    inverseOnSurface  = TextOnDark,
    inversePrimary    = BrandAmber,
    scrim             = BrandBlack.copy(alpha = 0.7f)
)

/* =========================
   Clon “light” que en realidad usa los mismos colores oscuros.
   (hacemos esto para que, aunque el sistema esté en claro,
   Compose pinte igual que en oscuro)
   ========================= */
private fun brandDarkSchemeAsLight(): ColorScheme = lightColorScheme(
    primary            = BrandAmber,
    onPrimary          = BrandBlack,
    primaryContainer   = BrandAmber,
    onPrimaryContainer = BrandBlack,

    secondary            = BrandAmber,
    onSecondary          = BrandBlack,
    secondaryContainer   = SurfaceContainerHigh,
    onSecondaryContainer = TextOnDark,

    tertiary            = BrandAmberDark,
    onTertiary          = BrandBlack,
    tertiaryContainer   = SurfaceContainerHigh,
    onTertiaryContainer = TextOnDark,

    error            = Error,
    onError          = OnError,
    errorContainer   = ErrorContainer,
    onErrorContainer = OnErrorContainer,

    background = Surface,
    onBackground = TextOnDark,
    surface = Surface,
    onSurface = TextOnDark,

    surfaceVariant  = SurfaceVariantDark,
    onSurfaceVariant = TextOnDark.copy(alpha = 0.75f),
    surfaceTint = BrandAmber,

    surfaceDim               = SurfaceDim,
    surfaceBright            = SurfaceBright,
    surfaceContainerLowest   = SurfaceContainerLowest,
    surfaceContainerLow      = SurfaceContainerLow,
    surfaceContainer         = SurfaceContainer,
    surfaceContainerHigh     = SurfaceContainerHigh,
    surfaceContainerHighest  = SurfaceContainerHighest,

    outline        = OutlineBrand,
    outlineVariant = OutlineBrand,

    inverseSurface    = SurfaceBright,
    inverseOnSurface  = TextOnDark,
    inversePrimary    = BrandAmber,
    scrim             = BrandBlack.copy(alpha = 0.7f)
)

/* =========================
   Tema Compose
   ========================= */
@Composable
fun TheBestDAMKebapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    @Suppress("unused") dynamicColor: Boolean = false, // mantenemos marca estable en Android 12+
    content: @Composable () -> Unit
) {
    // Usamos el mismo esquema “oscuro” en ambos modos del sistema
    val colorScheme = if (darkTheme) brandDarkScheme() else brandDarkSchemeAsLight()

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        shapes      = AppShapes,
        content     = content
    )
}
