package com.sergiom.thebestdamkebap.ui.theme

import androidx.compose.ui.graphics.Color

/* =========================
   Paleta de marca
   ========================= */
val BrandAmber     = Color(0xFFD38601)
val BrandAmberDark = Color(0xFF8A5C00)
val BrandBlack     = Color(0xFF000000)
val BrandWhite     = Color(0xFFFFFFFF)

/* =========================
   Neutros para superficies (modo oscuro permanente)
   ========================= */
val TextOnDark = BrandWhite
val OutlineBrand = BrandAmber

// Capas de superficie (consistentes en ambos “temas”)
val SurfaceDim                = Color(0xFF0A0A0A)
val Surface                   = BrandBlack
val SurfaceBright             = Color(0xFF1A1A1A)
val SurfaceContainerLowest    = Color(0xFF000000)
val SurfaceContainerLow       = Color(0xFF0A0A0A)
val SurfaceContainer          = Color(0xFF111111)
val SurfaceContainerHigh      = Color(0xFF161616)
val SurfaceContainerHighest   = Color(0xFF1C1C1C)
val SurfaceVariantDark        = Color(0xFF171717)

/* =========================
   Errores (fijos, legibles en dark)
   ========================= */
val Error            = Color(0xFFB3261E)
val OnError          = BrandWhite
val ErrorContainer   = Color(0xFF8C1D18)   // rojo oscuro para chips/tonal
val OnErrorContainer = BrandWhite
