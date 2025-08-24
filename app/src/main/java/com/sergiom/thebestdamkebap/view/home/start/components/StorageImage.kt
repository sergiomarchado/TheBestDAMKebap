package com.sergiom.thebestdamkebap.view.home.start.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import com.sergiom.thebestdamkebap.view.home.start.utils.StorageUrlMemoryCache

@Composable
internal fun StorageImage(
    ref: com.google.firebase.storage.StorageReference,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    // 1) Intento obtener la URL desde la caché
    val cached = remember(ref.path) { StorageUrlMemoryCache.get(ref.path) }

    // 2) Estado de la URL (null=cargando, ""=error, otra=ok)
    val urlState = produceState(initialValue = cached, ref.path) {
        if (cached != null) return@produceState  // nada que resolver
        ref.downloadUrl
            .addOnSuccessListener { url ->
                val u = url.toString()
                StorageUrlMemoryCache.put(ref.path, u)
                value = u
            }
            .addOnFailureListener {
                value = ""
            }
    }

    when (val u = urlState.value) {
        null -> Box(modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        ""   -> Box(modifier, contentAlignment = Alignment.Center) { Text("No cargó 😕") }
        else -> SubcomposeAsyncImage(
            model = u,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
            loading = { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() } },
            error   = {
                // Si la URL dejó de valer (p.ej. has reemplazado el fichero y cambió el token),
                // invalida la caché y vuelve a resolver en caliente.
                StorageUrlMemoryCache.invalidate(ref.path)
                // Forzamos un nuevo intento:
                // (resetearíamos a null para disparar de nuevo produceState)
            }
        )
    }
}

