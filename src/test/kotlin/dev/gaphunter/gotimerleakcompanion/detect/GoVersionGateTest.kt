package dev.gaphunter.gotimerleakcompanion.detect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoVersionGateTest {

    @Test
    fun `reads the go directive`() {
        val goMod = """
            module example.com/poller

            go 1.22.5

            require golang.org/x/sync v0.7.0
        """.trimIndent()
        assertEquals(1 to 22, GoVersionGate.goDirective(goMod))
    }

    @Test
    fun `go 1_22 still leaks`() {
        assertFalse(GoVersionGate.timersCollectedWithoutStop("module m\n\ngo 1.22\n"))
    }

    @Test
    fun `go 1_23 and later do not leak`() {
        assertTrue(GoVersionGate.timersCollectedWithoutStop("module m\n\ngo 1.23\n"))
        assertTrue(GoVersionGate.timersCollectedWithoutStop("module m\n\ngo 1.23.0\n"))
        assertTrue(GoVersionGate.timersCollectedWithoutStop("module m\n\ngo 1.25.1 // pinned\n"))
    }

    @Test
    fun `the toolchain line does not count, only the go line`() {
        val goMod = "module m\n\ngo 1.21\n\ntoolchain go1.24.2\n"
        assertFalse(GoVersionGate.timersCollectedWithoutStop(goMod))
    }

    @Test
    fun `a go_mod without a go line keeps the warning`() {
        assertNull(GoVersionGate.goDirective("module m\n"))
        assertFalse(GoVersionGate.timersCollectedWithoutStop("module m\n"))
    }

    @Test
    fun `a go line inside a comment is ignored`() {
        assertNull(GoVersionGate.goDirective("module m\n// go 1.23\n"))
    }
}
