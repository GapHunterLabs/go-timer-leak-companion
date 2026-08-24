package dev.gaphunter.gotimerleakcompanion.model

/** One `case <-time.After(...)` line found inside a `select` block. */
data class TimerLeakHit(val lineNumber: Int, val lineStartOffset: Int, val lineEndOffset: Int)
