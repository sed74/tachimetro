package com.sed.tachimetro

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Test JVM puri per [formatVersionLabel] -- nessun runtime Android.
 * Blocca il formato dell'etichetta versione mostrata sul telefono e sulla Surface di
 * Android Auto ("v<versionName> (<versionCode>)"), quick 260929-cyv.
 */
class VersionLabelTest {

    @Test
    fun betaName_formatsNameAndCode() {
        assertEquals("v2.1-beta (6)", formatVersionLabel("2.1-beta", 6))
    }

    @Test
    fun releaseName_formatsNameAndCode() {
        assertEquals("v2.0 (4)", formatVersionLabel("2.0", 4))
    }

    @Test
    fun firstVersion_formatsNameAndCode() {
        assertEquals("v1.0 (1)", formatVersionLabel("1.0", 1))
    }

    @Test
    fun blankName_usesQuestionMark() {
        assertEquals("v? (6)", formatVersionLabel("", 6))
        assertEquals("v? (6)", formatVersionLabel("   ", 6))
    }

    @Test
    fun paddedName_isTrimmed() {
        assertEquals("v2.1-beta (6)", formatVersionLabel(" 2.1-beta ", 6))
    }
}
