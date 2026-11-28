package com.steefwel28.consumoenergetico.core.updater

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pruebas JVM de la selección pura sobre la lista `/releases` de GitHub:
 * el JSON se construye con la forma exacta que devuelve la API.
 */
class GitHubReleaseCheckerTest {

    private fun release(tag: String, draft: Boolean = false, prerelease: Boolean = false): JSONObject =
        JSONObject()
            .put("tag_name", tag)
            .put("name", "Consumo Energético $tag")
            .put("draft", draft)
            .put("prerelease", prerelease)
            .put("body", "Notas del release $tag")
            .put("html_url", "https://github.com/Steefwel28/consumo-energetico/releases/tag/$tag")
            .put("published_at", "2026-11-01T12:00:00Z")
            .put(
                "assets",
                JSONArray().put(
                    JSONObject()
                        .put("name", "consumo-energetico-$tag.apk")
                        .put(
                            "browser_download_url",
                            "https://github.com/Steefwel28/consumo-energetico/releases/download/$tag/consumo-energetico-$tag.apk",
                        ),
                ),
            )

    @Test
    fun `selecciona prerelease sobre estable`() {
        val payload = JSONArray()
            .put(release("v0.7.1"))
            .put(release("v0.7.2-beta", prerelease = true))

        val selected = GitHubReleaseChecker.selectLatestRelease(payload, emptySet())!!

        assertEquals("v0.7.2-beta", selected.optString("tag_name"))
    }

    @Test
    fun `ignora borradores`() {
        val payload = JSONArray()
            .put(release("v0.8.0", draft = true))
            .put(release("v0.7.2-beta", prerelease = true))

        val selected = GitHubReleaseChecker.selectLatestRelease(payload, emptySet())!!

        assertEquals("v0.7.2-beta", selected.optString("tag_name"))
    }

    @Test
    fun `version omitida cae a la siguiente mas alta`() {
        val payload = JSONArray()
            .put(release("v0.7.2-beta", prerelease = true))
            .put(release("v0.7.1"))

        val selected = GitHubReleaseChecker.selectLatestRelease(payload, setOf("0.7.2-beta"))!!

        assertEquals("v0.7.1", selected.optString("tag_name"))
    }

    @Test
    fun `lista vacia devuelve null`() {
        assertNull(GitHubReleaseChecker.selectLatestRelease(JSONArray(), emptySet()))
    }
}
