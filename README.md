# Go Timer Leak Companion

Warning on a `case <-time.After(...)` inside a `select` block in a
module that targets Go before 1.23. Until Go 1.23, the Timer created by
`time.After` is not recovered by the garbage collector until it fires,
and a `select` inside any repeatedly-executed loop (the overwhelmingly
common place a `select` appears) creates a new one on every iteration,
leaking memory until each one's duration elapses. `time.NewTimer(...)`
plus a deferred/explicit `.Stop()` is the documented, correct
alternative.

Go 1.23 changed this: when the module's `go.mod` declares `go 1.23` or
later, unstopped timers are garbage collected, and the plugin does not
report them. A module without a `go` line keeps the warning.

## Why it exists

`select { case <-time.After(5 * time.Second): ... }` inside a `for`
loop compiles fine and reads naturally — it looks like the idiomatic
way to add a timeout to a select. It quietly leaks a Timer on every
iteration; in a hot loop this is measured in real, documented memory
growth (~200 bytes per timer as of Go 1.15, adding up fast at scale).
Modules that still declare an older `go` version in `go.mod` keep that
behavior even when built with a newer toolchain.

## Why built this way

- **100% static text analysis** — a brace/keyword-based line scanner,
  not a real Go PSI parser, so it works whether the Go plugin is
  installed or not.

Confirmed real and undetected: GoLand 2025.3's new resource leak
inspection covers types implementing `io.Closer` (files, network
connections, `sql.Rows`, etc.) — `time.After`'s returned channel does
not implement `io.Closer`, so it's structurally outside that
inspection's scope (confirmed by reading the feature's own
announcement post).

## v0.1 scope — stated honestly, not exhaustively

Flags `time.After(` as a `select` case regardless of whether the
enclosing `select` is itself inside a loop — reliably detecting
"inside a loop" via text alone across nested braces would be fragile.
A `select` that only ever runs once is a rare, low-cost false
positive.

## Usage

Open any `.go` file. A `case <-time.After(...)` inside a `select`
block shows a warning.

## Support

- **Bugs and feature requests:** [GitHub Issues](https://github.com/GapHunterLabs/go-timer-leak-companion/issues)
- **Questions, or custom rules for a team's codebase:** **gaphunterlabs@gmail.com**
- **Security vulnerabilities:** report privately as described in [SECURITY.md](SECURITY.md), not in a public issue.
- **Privacy and network behavior:** [PRIVACY.md](PRIVACY.md)

## Development

```
./gradlew test           # unit tests
./gradlew buildPlugin    # generates build/distributions/*.zip
./gradlew verifyPlugin   # checks compatibility against real IDEs
```

## License

Apache-2.0. See `LICENSE`.
