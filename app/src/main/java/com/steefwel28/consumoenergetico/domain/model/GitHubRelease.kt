package com.steefwel28.consumoenergetico.domain.model

/**
 * Release estable publicada en el repositorio oficial de GitHub.
 *
 * @property version versión normalizada sin prefijo `v` (por ejemplo `0.2.0`).
 * @property tagName etiqueta original del release (por ejemplo `v0.2.0`).
 * @property apkDownloadUrl URL directa del `.apk` adjunto, o `null` si no existe.
 */
data class GitHubRelease(
    val version: String,
    val tagName: String,
    val title: String,
    val notes: String,
    val releaseUrl: String,
    val apkDownloadUrl: String?,
    val publishedAt: String?,
)
