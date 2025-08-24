package com.sergiom.thebestdamkebap.view.home.start.utils

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.neonGlow(
    enabled: Boolean,
    color: Color,
    shape: Shape,
    thickness: Dp = 2.dp
): Modifier = composed {
    if (!enabled) return@composed Modifier

    val infinite = rememberInfiniteTransition(label = "neonGlow")
    // Barrido del brillo por el borde
    val sweep by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "sweep"
    )
    // Respiración del halo
    val haloAlpha by infinite.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "halo"
    )

    drawWithCache {
        // Convertimos el shape a Path para dibujar el contorno exactamente igual que el botón
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = Path().apply {
            when (outline) {
                is Outline.Rounded   -> addRoundRect(outline.roundRect)
                is Outline.Rectangle -> addRect(outline.rect)
                is Outline.Generic   -> addPath(outline.path)
            }
        }
        val strokePx = thickness.toPx()

        onDrawWithContent {
            drawContent()

            // 1) Halo interior suave (varias pasadas con diferente grosor/alpha)
            drawPath(path, color = color.copy(alpha = haloAlpha * 0.25f), style = Stroke(width = strokePx * 4))
            drawPath(path, color = color.copy(alpha = haloAlpha * 0.15f), style = Stroke(width = strokePx * 7))

            // 2) Borde base muy sutil
            drawPath(path, color = color.copy(alpha = 0.25f), style = Stroke(width = strokePx))

            // 3) Brillo que se desplaza por el borde (neón)
            val w = size.width
            val startX = -w + (w * 2f * sweep) // de izq -> der y vuelve a empezar
            val brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    color.copy(alpha = haloAlpha),
                    Color.Transparent
                ),
                start = Offset(startX, 0f),
                end = Offset(startX + w / 2f, size.height)
            )
            drawPath(path, brush = brush, style = Stroke(width = strokePx * 1.9f))
        }
    }
}

