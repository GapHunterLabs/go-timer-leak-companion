package dev.gaphunter.gotimerleakcompanion.detect

/**
 * Decides from a module's `go.mod` whether the `time.After` leak can still happen.
 *
 * Go 1.23 changed timers: an unreferenced, unstopped Timer is now recovered by the garbage collector before it
 * fires. The Go 1.23 release notes tie that to the `go` line of the main module's `go.mod`: the new behavior is
 * enabled when it says 1.23.0 or later, and older modules keep the old one even when built with a newer toolchain.
 * So the `go` line, not the installed Go version, is what matters.
 */
object GoVersionGate {

    private val GO_LINE = Regex("""^\s*go\s+(\d+)\.(\d+)(?:\.\d+)?\s*(?://.*)?$""", RegexOption.MULTILINE)

    /** The `go` directive of a go.mod as (major, minor), or null when there is none. */
    fun goDirective(goModText: String): Pair<Int, Int>? {
        val match = GO_LINE.find(goModText) ?: return null
        return match.groupValues[1].toInt() to match.groupValues[2].toInt()
    }

    /** True when the module declares Go 1.23 or later, where an unstopped `time.After` timer no longer leaks. */
    fun timersCollectedWithoutStop(goModText: String): Boolean {
        val (major, minor) = goDirective(goModText) ?: return false
        return major > 1 || (major == 1 && minor >= 23)
    }
}
