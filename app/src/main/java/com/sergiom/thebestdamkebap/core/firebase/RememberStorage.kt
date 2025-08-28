package com.sergiom.thebestdamkebap.core.firebase

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.storage.FirebaseStorage

/**
 * Devuelve una instancia de [FirebaseStorage] memorizada para el bucket actual.
 *
 * ### Qué hace
 * - Obtiene el bucket configurado en `Firebase.app.options.storageBucket`.
 * - Si no hay bucket definido, devuelve la instancia por defecto de Firebase Storage.
 * - Memoriza la instancia usando [remember] para evitar recrearla en cada recomposición.
 *
 * ### Por qué así
 * - En Jetpack Compose, las funciones se pueden volver a ejecutar muchas veces.
 *   Con `remember(bucket)` nos aseguramos de no crear un nuevo objeto de [FirebaseStorage]
 *   en cada recomposición, solo cuando cambia el bucket (lo cual es muy raro).
 *
 * ### Notas
 * - El bucket puede venir nulo o vacío si no está configurado en `google-services.json`.
 * - `FirebaseStorage.getInstance(\"gs://...\")` es la forma explícita de apuntar a un bucket concreto.
 */
@Composable
fun rememberStorage(): FirebaseStorage {
    val bucket = remember { Firebase.app.options.storageBucket }
    return remember(bucket) {
        if (bucket.isNullOrBlank()) FirebaseStorage.getInstance()
        else FirebaseStorage.getInstance("gs://$bucket")
    }
}


