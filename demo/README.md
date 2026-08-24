# Demo data — Go Timer Leak Companion

For capturing the real Marketplace screenshot:

1. `./gradlew runIde`
2. Open `demo/poller.go` as a scratch/standalone file (or drop it into
   any sandbox project) inside the sandbox IDE.
3. The `case <-time.After(5 * time.Second):` line inside `poll` shows
   the warning — hover it for the tooltip. `pollCorrect`'s
   `time.NewTimer` + `.Stop()` pattern stays clean, for contrast.
4. Enter Full Screen (`View > Appearance > Enter Full Screen`), capture
   with `Win+Shift+S`, save directly to `docs/screenshots/` in this
   repo.
