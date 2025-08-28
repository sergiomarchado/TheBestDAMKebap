package com.sergiom.thebestdamkebap.core.imageloading

/**
 * Caché en memoria **thread-safe** para almacenar temporalmente mappings de
 * `path` de Firebase Storage → `downloadUrl` firmado.
 *
 * Características principales:
 * - **LRU (Least Recently Used)**: mantiene hasta [MAX_SIZE] entradas; al superarlo,
 *   expulsa automáticamente las menos usadas.
 * - **TTL (Time To Live)**: permite invalidar automáticamente una entrada tras cierto tiempo.
 *
 * Usos:
 * - Evitar múltiples llamadas a `getDownloadUrl()` para el mismo objeto en Storage.
 * - Reducir latencia en pantallas con muchas imágenes.
 *
 * Notas:
 * - Se sincroniza manualmente con un `lock` para asegurar **seguridad en hilos**.
 * - Solo cachea en memoria del proceso; se vacía al matar la app o en casos de `clear()`.
 */
internal object StorageUrlMemoryCache {

    /** Máximo de entradas en caché antes de expulsar (LRU). */
    private const val MAX_SIZE = 128

    /** Lock global para sincronizar todas las operaciones y evitar condiciones de carrera. */
    private val lock = Any()

    /**
     * Implementación LRU real usando [LinkedHashMap] con `accessOrder = true`.
     * Cada acceso mueve la entrada al final; al superar [MAX_SIZE], expulsa la más antigua.
     */
    private val lru = object : LinkedHashMap<String, String>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean =
            size > MAX_SIZE
    }

    /**
     * Mapa de expiraciones (timestamp en epoch millis).
     * Solo contiene entrada si se insertó con un TTL.
     */
    private val expires = HashMap<String, Long>()


    /**
     * Recupera la URL en caché para un [path].
     * - Si existe y no ha caducado → devuelve la URL.
     * - Si está caducada → se elimina y devuelve `null`.
     * - Si no existe → devuelve `null`.
     */
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

    /**
     * Inserta una URL asociada a [path].
     * - Si [ttlMillis] es no nulo, se programará la caducidad a ese tiempo en el futuro.
     * - Si es nulo, la entrada no caduca automáticamente.
     */
    fun put(path: String, url: String, ttlMillis: Long? = null) = synchronized(lock) {
        lru[path] = url
        if (ttlMillis != null) {
            expires[path] = System.currentTimeMillis() + ttlMillis
        } else {
            expires.remove(path)
        }
    }

    /**
     * Invalida manualmente un [path]:
     * - Elimina tanto de la caché LRU como del mapa de expiraciones.
     */
    fun invalidate(path: String) = synchronized(lock) {
        lru.remove(path)
        expires.remove(path)
    }


    /**
     * Devuelve el número actual de entradas en la caché (solo para debug/métricas).
     */
    fun size(): Int = synchronized(lock) { lru.size }

    /**
     * Limpia por completo la caché y las expiraciones.
     * Útil en eventos como `onLowMemory` o `onTrimMemory`.
     */
    fun clear() = synchronized(lock) {
        lru.clear()
        expires.clear()
    }
}
