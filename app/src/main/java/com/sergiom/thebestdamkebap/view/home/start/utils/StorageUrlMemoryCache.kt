package com.sergiom.thebestdamkebap.view.home.start.utils

/* Cache simple en memoria para mapping path -> downloadUrl */
internal object StorageUrlMemoryCache {
    private val map = java.util.concurrent.ConcurrentHashMap<String, String>()
    fun get(path: String): String? = map[path]
    fun put(path: String, url: String) { map[path] = url }
    fun invalidate(path: String) { map.remove(path) }
}