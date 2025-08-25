package com.sergiom.thebestdamkebap.core.imageloading

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await

/**
 * Carga imágenes de Firebase Storage con:
 *  - Cache propia (memoria) path -> downloadUrl para evitar repetir getDownloadUrl().
 *  - Cache de Coil (memoria + disco) basada en la URL.
 *  - Retry automático si la URL caduca (invalidación + nuevo resolve).
 *
 * Estados:
 *  - null   -> loading (resolviendo URL)
 *  - ""     -> error resolviendo URL (muestra placeholder)
 *  - "http" -> URL lista: SubcomposeAsyncImage con caching de Coil
 */
@Composable
internal fun StorageImage(
    ref: StorageReference,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    var retryKey by remember(ref.path) { mutableIntStateOf(0) }

    // 1) Cache hit inmediato si existe
    val cached = remember(ref.path, retryKey) { StorageUrlMemoryCache.get(ref.path) }

    // 2) Resolver una sola vez por path (o tras retry)
    val urlState = produceState(initialValue = cached, ref.path, retryKey) {
        if (cached != null) return@produceState
        // Si falla la resolución de URL, dejamos "" para mostrar placeholder de error.
        value = try {
            ref.downloadUrl.await().toString().also { StorageUrlMemoryCache.put(ref.path, it) }
        } catch (_: Throwable) {
            ""
        }
    }

    when (val u = urlState.value) {
        null -> Box(modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        "" -> Box(modifier, contentAlignment = Alignment.Center) {
            Text("No se pudo cargar la imagen")
        }
        else -> SubcomposeAsyncImage(
            model = u, // URL https estable → Coil cachea memoria+disco
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
            loading = {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            },
            error = {
                // Si la URL dejó de valer (token rotado, 401/403...), invalida y reintenta:
                StorageUrlMemoryCache.invalidate(ref.path)
                retryKey++ // fuerza nuevo resolve y nueva carga
            }
        )
    }
}
