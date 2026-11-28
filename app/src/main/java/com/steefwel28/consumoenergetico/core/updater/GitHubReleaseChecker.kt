package com.steefwel28.consumoenergetico.core.updater

import com.steefwel28.consumoenergetico.BuildConfig
import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import com.steefwel28.consumoenergetico.data.datastore.UpdatePreferences
import com.steefwel28.consumoenergetico.domain.model.GitHubRelease
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Verificador del release publicado más reciente en GitHub.
 *
 * Es el único punto de red de la app: usa `HttpURLConnection` nativo con
 * timeout de 4 s contra la lista `/releases`. Ignora borradores pero sí
 * considera pre-releases (`v0.7.2-beta`), salta las versiones que el usuario
 * decidió omitir en DataStore cayendo a la siguiente más alta.
 */
@CraftedBy("steefwel28")
class GitHubReleaseChecker(
    private val updatePreferences: UpdatePreferences,
    private val currentVersionName: String = BuildConfig.VERSION_NAME,
    private val releasesApiUrl: String = BuildConfig.GITHUB_RELEASES_API,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    private val _hasPendingUpdate = MutableStateFlow(false)
    val hasPendingUpdate: StateFlow<Boolean> = _hasPendingUpdate.asStateFlow()

    private val _pendingRelease = MutableStateFlow<GitHubRelease?>(null)
    val pendingRelease: StateFlow<GitHubRelease?> = _pendingRelease.asStateFlow()

    /**
     * Consulta el endpoint y actualiza [hasPendingUpdate].
     *
     * No lanza excepciones de red: los fallos se reportan como
     * [UpdateCheckResult.NetworkError] para que la UI decida si reintenta.
     */
    suspend fun checkForUpdate(): UpdateCheckResult = withContext(ioDispatcher) {
        val skippedVersions = updatePreferences.skippedVersions.first()
        val release = try {
            fetchLatestRelease(skippedVersions)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            return@withContext UpdateCheckResult.NetworkError(error)
        } ?: return@withContext UpdateCheckResult.NoStableRelease

        if (!SemanticVersion.isNewer(release.version, currentVersionName)) {
            clearPendingRelease()
            return@withContext UpdateCheckResult.UpToDate
        }

        _pendingRelease.value = release
        _hasPendingUpdate.value = true
        UpdateCheckResult.UpdateAvailable(release)
    }

    /**
     * Persiste la versión omitida y oculta el aviso si era la pendiente.
     */
    suspend fun skipVersion(version: String) {
        updatePreferences.skipVersion(version)
        if (_pendingRelease.value?.version == version) clearPendingRelease()
    }

    /**
     * Permite volver a mostrar avisos de versiones omitidas previamente.
     */
    suspend fun clearSkippedVersions() {
        updatePreferences.clearSkippedVersions()
    }

    private fun clearPendingRelease() {
        _pendingRelease.value = null
        _hasPendingUpdate.value = false
    }

    private fun fetchLatestRelease(skippedVersions: Set<String>): GitHubRelease? {
        val connection = (URL(releasesApiUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", USER_AGENT)
        }

        return try {
            val status = connection.responseCode
            if (status != HttpURLConnection.HTTP_OK) {
                throw IOException("GitHub respondió HTTP $status")
            }
            val payload = connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { reader -> reader.readText() }
            selectLatestRelease(JSONArray(payload), skippedVersions)?.let(::parseRelease)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseRelease(payload: JSONObject): GitHubRelease {
        val tagName = payload.optString("tag_name")

        return GitHubRelease(
            version = SemanticVersion.normalize(tagName),
            tagName = tagName,
            title = payload.optString("name").ifBlank { tagName },
            notes = payload.optString("body"),
            releaseUrl = payload.optString("html_url"),
            apkDownloadUrl = findApkDownloadUrl(payload.optJSONArray("assets")),
            publishedAt = payload.optString("published_at").takeIf(String::isNotBlank),
        )
    }

    private fun findApkDownloadUrl(assets: JSONArray?): String? {
        if (assets == null) return null

        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            val name = asset.optString("name")
            if (!name.endsWith(".apk", ignoreCase = true)) continue

            return asset.optString("browser_download_url").takeIf(String::isNotBlank)
        }
        return null
    }

    companion object {
        /**
         * Selecciona de la lista `/releases` el JSON del release publicable más
         * alto: descarta borradores y versiones omitidas, considera
         * pre-releases (`v0.7.2-beta`) y compara con [SemanticVersion].
         *
         * Función pura sin red ni dependencias de Android: las pruebas JVM la
         * ejercitan con un payload con la forma exacta de la API de GitHub.
         */
        fun selectLatestRelease(payload: JSONArray, skipped: Set<String>): JSONObject? {
            var best: JSONObject? = null
            for (index in 0 until payload.length()) {
                val release = payload.optJSONObject(index) ?: continue
                if (release.optBoolean("draft")) continue
                val tagName = release.optString("tag_name").takeIf(String::isNotBlank) ?: continue
                if (SemanticVersion.normalize(tagName) in skipped) continue

                val current = best
                if (current == null || SemanticVersion.isNewer(
                        SemanticVersion.normalize(tagName),
                        SemanticVersion.normalize(current.optString("tag_name")),
                    )
                ) {
                    best = release
                }
            }
            return best
        }

        private const val TIMEOUT_MILLIS = 4_000
        private const val USER_AGENT = "ConsumoEnergetico-App"
    }
}
