package com.sergiom.thebestdamkebap.core.firebase

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.storage.FirebaseStorage

@Composable
fun rememberStorage(): FirebaseStorage {
    val bucket = remember { Firebase.app.options.storageBucket }
    return remember(bucket) {
        if (bucket.isNullOrBlank()) FirebaseStorage.getInstance()
        else FirebaseStorage.getInstance("gs://$bucket")
    }
}


