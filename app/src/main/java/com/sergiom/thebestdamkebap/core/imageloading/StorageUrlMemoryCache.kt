package com.sergiom.thebestdamkebap.core.imageloading

/**
 * Caché thread-safe en memoria (LRU + TTL) para path -> downloadUrl.
 * - LRU: mantiene hasta MAX_SIZE URLs, expulsando las menos usadas.
 * - TTL: invalida la entrada al expirar (si se define).
 */
internal object StorageUrlMemoryCache {

    private const val MAX_SIZE = 128
    private val lock = Any()

    // LRU real gracias a accessOrder = true, y expulsión en removeEldestEntry
    private val lru = object : LinkedHashMap<String, String>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean =
            size > MAX_SIZE
    }

    // Mapa de expiraciones (en epoch millis). Solo hay entrada si hay TTL.
    private val expires = HashMap<String, Long>()

    fun get(path: String): String? = synchronized(lock) {
        val exp = expires[path]
        if (exp != null && System.currentTimeMillis() > exp) {
            // Expirada: eliminar y devolver null
            lru.remove(path)
            expires.remove(path)
            return null
        }
        lru[path]
    }

    fun put(path: String, url: String, ttlMillis: Long? = null) = synchronized(lock) {
        lru[path] = url
        if (ttlMillis != null) {
            expires[path] = System.currentTimeMillis() + ttlMillis
        } else {
            expires.remove(path)
        }
    }

    fun invalidate(path: String) = synchronized(lock) {
        lru.remove(path)
        expires.remove(path)
    }

    // (Opcional) útil para debug/metricas
    fun size(): Int = synchronized(lock) { lru.size }

    fun clear() = synchronized(lock) {
        lru.clear()
        expires.clear()
    }
}
