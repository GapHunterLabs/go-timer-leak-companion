package dev.gaphunter.gotimerleakcompanion.detect

import dev.gaphunter.gotimerleakcompanion.model.TimerLeakHit

/**
 * Plain-text scan for a `case <-time.After(...)` line inside a
 * `select { ... }` block -- Go's own documented behavior: the Timer
 * created by `time.After` is not recovered by the garbage collector
 * until it fires, and a `select` inside any repeatedly-executed loop
 * (the overwhelmingly common place a `select` appears) creates a new
 * one on every iteration, leaking memory until each one's duration
 * elapses. `time.NewTimer(...)` + a deferred/explicit `.Stop()` is the
 * documented, correct alternative.
 *
 * **v0.1 scope, stated honestly:** plain-text brace/keyword scanning,
 * not real Go PSI (no Go language plugin dependency, works whether or
 * not the Go plugin is installed) -- flags `time.After(` appearing as
 * a `select` case regardless of whether the enclosing `select` is
 * itself inside a loop, since that's the overwhelmingly common shape
 * and reliably detecting "inside a loop" via text alone across nested
 * braces would be unreliably fragile. A `select` that only runs once
 * (never in a loop) is a rare, low-cost false positive.
 */
object SelectTimeAfterScanner {

    private val SELECT_OPEN = Regex("""\bselect\s*\{""")
    private val CASE_TIME_AFTER = Regex("""^\s*case\s+.*<-\s*time\.After\(""")

    fun scan(text: String): List<TimerLeakHit> {
        val hits = mutableListOf<TimerLeakHit>()
        var selectDepth = 0
        var braceDepthAtSelectEntry = 0
        var currentDepth = 0
        var offset = 0

        for ((index, line) in text.lines().withIndex()) {
            val lineStart = offset
            offset += line.length + 1

            if (selectDepth == 0 && SELECT_OPEN.containsMatchIn(line)) {
                selectDepth = 1
                braceDepthAtSelectEntry = currentDepth + line.count { it == '{' } - line.count { it == '}' }
                currentDepth = braceDepthAtSelectEntry
                continue
            }

            if (selectDepth > 0) {
                if (CASE_TIME_AFTER.containsMatchIn(line)) {
                    hits += TimerLeakHit(index + 1, lineStart, lineStart + line.length)
                }
                currentDepth += line.count { it == '{' } - line.count { it == '}' }
                if (currentDepth < braceDepthAtSelectEntry) {
                    selectDepth = 0
                }
            } else {
                currentDepth += line.count { it == '{' } - line.count { it == '}' }
            }
        }
        return hits
    }
}
