package com.steefwel28.consumoenergetico.core.updater

import com.steefwel28.consumoenergetico.domain.model.GitHubRelease

/**
 * Resultado de una consulta al endpoint de releases de GitHub.
 */
sealed interface UpdateCheckResult {

    data class UpdateAvailable(val release: GitHubRelease) : UpdateCheckResult

    data class UpdateSkipped(val release: GitHubRelease) : UpdateCheckResult

    data object UpToDate : UpdateCheckResult

    data object NoStableRelease : UpdateCheckResult

    data class NetworkError(val cause: Throwable) : UpdateCheckResult
}
