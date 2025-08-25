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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.google.firebase.storage.FirebaseStorage
import com.sergiom.thebestdamkebap.core.imageloading.StorageImage
import com.sergiom.thebestdamkebap.viewmodel.home.homestart.HomeStartViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PromoCarouselFullBleed(
    promos: List<HomeStartViewModel.Promo>,
    autoAdvanceMillis: Long = 3200
) {
    val pagerState = rememberPagerState { promos.size }
    val colors = MaterialTheme.colorScheme
    val storage = remember { FirebaseStorage.getInstance() } // usa el bucket del google-services.json

    // Auto-slide
    LaunchedEffect(pagerState.pageCount) {
        if (pagerState.pageCount <= 1) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(autoAdvanceMillis)
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
            val ref = remember(promo.storagePath) { storage.reference.child(promo.storagePath) }

            // Imagen full-bleed desde downloadUrl (funciona con Coil por defecto)
            StorageImage(
                ref = ref,
                contentDescription = promo.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Overlay (gradiente + título)
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                colors.primary.copy(alpha = 0f),
                                colors.primary.copy(alpha = 0.55f)
                            )
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Text(promo.title, style = MaterialTheme.typography.titleLarge, color = colors.onPrimary)
            }
        }

        // Dots
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
                        .background(if (active) Color.White else Color.White.copy(alpha = 0.5f))
                )
            }
        }
    }
}

