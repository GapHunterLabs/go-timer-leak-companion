package dev.gaphunter.gotimerleakcompanion.detect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectTimeAfterScannerTest {

    @Test
    fun `flags time-After case inside a select in a for loop`() {
        val code = """
            func poll(ch chan int) {
                for {
                    select {
                    case v := <-ch:
                        process(v)
                    case <-time.After(5 * time.Second):
                        return
                    }
                }
            }
        """.trimIndent()
        val hits = SelectTimeAfterScanner.scan(code)
        assertEquals(1, hits.size)
    }

    @Test
    fun `does not flag time-NewTimer usage`() {
        val code = """
            func poll(ch chan int) {
                timer := time.NewTimer(5 * time.Second)
                defer timer.Stop()
                for {
                    select {
                    case v := <-ch:
                        process(v)
                    case <-timer.C:
                        return
                    }
                }
            }
        """.trimIndent()
        assertTrue(SelectTimeAfterScanner.scan(code).isEmpty())
    }

    @Test
    fun `does not flag time-After outside of a select block`() {
        val code = """
            func wait() {
                <-time.After(5 * time.Second)
            }
        """.trimIndent()
        assertTrue(SelectTimeAfterScanner.scan(code).isEmpty())
    }

    @Test
    fun `select ends correctly at closing brace, does not leak into next function`() {
        val code = """
            func poll(ch chan int) {
                select {
                case <-time.After(time.Second):
                    return
                }
            }

            func unrelated() {
                select {
                case v := <-ch:
                    process(v)
                }
            }
        """.trimIndent()
        val hits = SelectTimeAfterScanner.scan(code)
        assertEquals(1, hits.size)
    }
}
