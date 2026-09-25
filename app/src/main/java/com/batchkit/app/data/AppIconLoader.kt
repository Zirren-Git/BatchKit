package com.batchkit.app.data

import android.content.Context
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Loads and caches app icons off the main thread.
 *
 * Icons are drawn at a fixed 96 px size which is plenty for the list rows and
 * keeps the cache small even with several hundred apps installed.
 */
class AppIconLoader(private val context: Context) {

    private val cache = LruCache<String, ImageBitmap>(MAX_ENTRIES)
    private val lock = Mutex()

    suspend fun load(packageName: String): ImageBitmap? {
        cache.get(packageName)?.let { return it }
        return withContext(Dispatchers.IO) {
            lock.withLock {
                cache.get(packageName)?.let { return@withLock it }
                val bitmap = runCatching {
                    val drawable = context.packageManager.getApplicationIcon(packageName)
                    drawable.toBitmap(width = ICON_PIXELS, height = ICON_PIXELS).asImageBitmap()
                }.getOrNull()
                bitmap?.also { cache.put(packageName, it) }
            }
        }
    }

    private companion object {
        const val MAX_ENTRIES = 256
        const val ICON_PIXELS = 96
    }
}
