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
import androidx.compose.ui.res.stringResource
import coil.compose.SubcomposeAsyncImage
import com.google.firebase.storage.StorageReference
import com.sergiom.thebestdamkebap.R
import kotlinx.coroutines.tasks.await

/**
 * Muestra una imagen alojada en **Firebase Storage** utilizando:
 *
 * - **Cache propia en memoria** (path -> downloadUrl) para evitar repetir `getDownloadUrl()`.
 * - **Cache de Coil** (memoria + disco) basada en la URL resuelta.
 * - **Reintento automático** si la URL caduca (invalidación y nueva resolución).
 *
 * Estados manejados durante la carga de la URL:
 * - `null`  → cargando (resolviendo la URL firmada).
 * - `""`    → error al resolver la URL (se muestra placeholder de error).
 * - `"http…"` → URL lista: se delega la carga de la imagen a `SubcomposeAsyncImage` (Coil).
 *
 * @param ref Referencia a un objeto de Firebase Storage (p. ej. `images/123.jpg`).
 * @param contentDescription Descripción accesible de la imagen.
 * @param modifier Modificadores de Compose para el contenedor de imagen.
 * @param contentScale Cómo se escala el contenido dentro del espacio disponible.
 */
@Composable
internal fun StorageImage(
    ref: StorageReference,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    // Clave robusta por si hay multibucket (siempre existe ref.bucket)
    val cacheKey = remember(ref) { "${ref.bucket}/${ref.path}" }

    var retryKey by remember(cacheKey) { mutableIntStateOf(0) }

    // 1) Cache hit inmediato (memoria propia)
    val cached = remember(cacheKey, retryKey) { StorageUrlMemoryCache.get(cacheKey) }

    // 2) Resolver URL firmada si no está en cache o tras reintento
    val urlState = produceState(initialValue = cached, cacheKey, retryKey) {
        if (cached != null) return@produceState
        value = try {
            ref.downloadUrl.await().toString().also { StorageUrlMemoryCache.put(cacheKey, it) }
        } catch (_: Throwable) {
            ""
        }
    }

    when (val u = urlState.value) {
        null -> Box(modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        ""   -> Box(modifier, contentAlignment = Alignment.Center) {
            Text(
                text= stringResource(R.string.storage_image_could_not_be_loaded)
            )
        }
        else -> SubcomposeAsyncImage(
            model = u,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
            loading = {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            },
            // 3) Reintento seguro: invalidamos y re-resolvemos fuera del árbol de composición
            onError = {
                StorageUrlMemoryCache.invalidate(cacheKey)
                retryKey++
            }
        )
    }
}
