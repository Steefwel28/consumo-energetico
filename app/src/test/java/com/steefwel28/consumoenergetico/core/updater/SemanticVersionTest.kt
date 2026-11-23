package com.steefwel28.consumoenergetico.core.updater

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticVersionTest {

    @Test
    fun `normaliza prefijo v y metadata de compilacion`() {
        assertEquals("1.2.3", SemanticVersion.normalize("v1.2.3+build.5"))
        assertEquals("0.1.0", SemanticVersion.normalize(" 0.1.0 "))
    }

    @Test
    fun `detecta una version candidata mayor`() {
        assertTrue(SemanticVersion.isNewer("0.2.0", "0.1.9"))
        assertTrue(SemanticVersion.isNewer("1.0", "0.9.9"))
    }

    @Test
    fun `no marca como nueva una version igual o anterior`() {
        assertFalse(SemanticVersion.isNewer("0.1.0", "0.1.0"))
        assertFalse(SemanticVersion.isNewer("0.0.9", "0.1.0"))
    }

    @Test
    fun `un release estable supera a su pre-release`() {
        assertTrue(SemanticVersion.isNewer("1.0.0", "1.0.0-beta"))
    }
}
