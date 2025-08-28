package com.sergiom.thebestdamkebap.view.home.start.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.sergiom.thebestdamkebap.core.firebase.rememberStorage
import com.sergiom.thebestdamkebap.core.imageloading.StorageImage
import com.sergiom.thebestdamkebap.viewmodel.home.homestart.HomeStartViewModel
import kotlinx.coroutines.delay

/**
 * Carrusel de promociones a pantalla completa (full-bleed).
 *
 * - `HorizontalPager` para paginar banners.
 * - `StorageImage` (Firebase Storage + caché propia + Coil).
 * - Overlay con gradiente + título para legibilidad.
 * - Dots de paginación con colores de tema para buen contraste.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PromoCarouselFullBleed(
    promos: List<HomeStartViewModel.Promo>,
    autoAdvanceMillis: Long = 3200
) {
    val pagerState = rememberPagerState { promos.size }
    val colors = MaterialTheme.colorScheme
    val storage = rememberStorage() // usa el bucket configurado (y fácil de redirigir a emulador)

    // Auto-slide cuando hay más de una promo
    LaunchedEffect(pagerState.pageCount, autoAdvanceMillis) {
        if (pagerState.pageCount <= 1) return@LaunchedEffect
        while (true) {
            delay(autoAdvanceMillis)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % pagerState.pageCount)
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val promo = promos[page]
            // Resolvemos la referencia una sola vez por path
            val ref = remember(promo.storagePath) { storage.reference.child(promo.storagePath) }

            // Imagen full-bleed (Coil cachea memoria+disco tras resolver URL)
            StorageImage(
                ref = ref,
                contentDescription = promo.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Overlay con gradiente para mejorar contraste del título
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                colors.surface.copy(alpha = 0f),
                                colors.surface.copy(alpha = 0f),
                                colors.surface.copy(alpha = 0.55f)
                            )
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Text(promo.title, style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            }
        }

        // Dots de página (usa colores del tema para buen contraste en claro/oscuro)
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(pagerState.pageCount) { i ->
                val active = i == pagerState.currentPage
                Box(
                    Modifier
                        .size(if (active) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) colors.onPrimary
                            else colors.onPrimary.copy(alpha = 0.5f)
                        )
                )
            }
        }
    }
}
