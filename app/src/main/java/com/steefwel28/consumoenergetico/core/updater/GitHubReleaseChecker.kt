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
 * Verificador del release estable más reciente publicado en GitHub.
 *
 * Es el único punto de red de la app: usa `HttpURLConnection` nativo con
 * timeout de 4 s, ignora pre-releases y borradores, y recuerda en DataStore
 * las versiones que el usuario decidió omitir.
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
        val release = try {
            fetchLatestStableRelease()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            return@withContext UpdateCheckResult.NetworkError(error)
        } ?: return@withContext UpdateCheckResult.NoStableRelease

        val skippedVersions = updatePreferences.skippedVersions.first()
        when {
            !SemanticVersion.isNewer(release.version, currentVersionName) -> {
                clearPendingRelease()
                UpdateCheckResult.UpToDate
            }

            release.version in skippedVersions -> {
                clearPendingRelease()
                UpdateCheckResult.UpdateSkipped(release)
            }

            else -> {
                _pendingRelease.value = release
                _hasPendingUpdate.value = true
                UpdateCheckResult.UpdateAvailable(release)
            }
        }
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

    private fun fetchLatestStableRelease(): GitHubRelease? {
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
            parseStableRelease(JSONObject(payload))
        } finally {
            connection.disconnect()
        }
    }

    private fun parseStableRelease(payload: JSONObject): GitHubRelease? {
        if (payload.optBoolean("draft") || payload.optBoolean("prerelease")) return null

        val tagName = payload.optString("tag_name").takeIf(String::isNotBlank) ?: return null

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

    private companion object {
        const val TIMEOUT_MILLIS = 4_000
        const val USER_AGENT = "consumo-energetico-android"
    }
}
