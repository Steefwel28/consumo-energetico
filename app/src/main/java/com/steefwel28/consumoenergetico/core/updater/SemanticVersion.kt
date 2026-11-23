package com.steefwel28.consumoenergetico.core.updater

import com.steefwel28.consumoenergetico.core.signature.CraftedBy

/**
 * Compara versiones semánticas sin dependencias externas.
 *
 * Solo se aceptan releases estables, pero la versión instalada puede incluir
 * un sufijo de pre-release (`1.0.0-beta`) o de depuración (`0.1.0-debug`).
 */
@CraftedBy("steefwel28")
internal object SemanticVersion {

    fun normalize(raw: String): String = raw.trim()
        .removePrefix("v")
        .removePrefix("V")
        .substringBefore('+')

    fun isNewer(candidate: String, current: String): Boolean {
        val candidateVersion = ParsedVersion.parse(candidate)
        val currentVersion = ParsedVersion.parse(current)

        val length = maxOf(candidateVersion.core.size, currentVersion.core.size)
        for (index in 0 until length) {
            val candidatePart = candidateVersion.core.getOrElse(index) { 0 }
            val currentPart = currentVersion.core.getOrElse(index) { 0 }
            if (candidatePart != currentPart) return candidatePart > currentPart
        }

        return currentVersion.isPreRelease && !candidateVersion.isPreRelease
    }

    private data class ParsedVersion(
        val core: List<Int>,
        val isPreRelease: Boolean,
    ) {
        companion object {
            fun parse(raw: String): ParsedVersion {
                val normalized = normalize(raw)
                val withoutPreRelease = normalized.substringBefore('-')
                val core = withoutPreRelease
                    .split('.')
                    .map { part -> part.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
                return ParsedVersion(
                    core = core,
                    isPreRelease = normalized.contains('-'),
                )
            }
        }
    }
}
