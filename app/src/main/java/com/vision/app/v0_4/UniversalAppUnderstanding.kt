package com.vision.app.v0_4

import android.app.ActivityManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfo(
    val packageName: String,
    val appName: String,
    val category: String,
    val mainFeatures: List<String>,
    val uiPatterns: List<String>,
    val lastUpdated: Long = System.currentTimeMillis()
)

class UniversalAppUnderstanding(
    private val context: Context
) {
    private val appCache = mutableMapOf<String, AppInfo>()

    suspend fun detectCurrentApp(): AppInfo? = withContext(Dispatchers.Default) {
        val packageName = getCurrentPackageName() ?: return@withContext null
        synchronized(appCache) {
            appCache[packageName]?.let {
                return@withContext it.copy(lastUpdated = System.currentTimeMillis())
            }
            val info = AppInfo(
                packageName = packageName,
                appName = packageName.substringAfterLast('.'),
                category = "unknown",
                mainFeatures = emptyList(),
                uiPatterns = emptyList()
            )
            appCache[packageName] = info
            info
        }
    }

    suspend fun learnAppPattern(packageName: String, action: String, result: String) =
        withContext(Dispatchers.IO) {
            synchronized(appCache) {
                appCache[packageName]?.let { old ->
                    appCache[packageName] = old.copy(
                        uiPatterns = (old.uiPatterns + "$action -> $result").takeLast(50),
                        lastUpdated = System.currentTimeMillis()
                    )
                }
            }
        }

    fun getAppCapabilities(packageName: String): List<String> =
        synchronized(appCache) { appCache[packageName]?.mainFeatures ?: emptyList() }

    fun clearAppCache() = synchronized(appCache) { appCache.clear() }

    fun getLearnedApps(): List<AppInfo> =
        synchronized(appCache) { appCache.values.toList() }

    private fun getCurrentPackageName(): String? = try {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        manager.appTasks.firstOrNull()?.taskInfo?.baseActivity?.packageName
    } catch (_: Exception) {
        null
    }
}
