<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Go Timer Leak Companion Changelog

## [Unreleased]

## [0.1.0]

### Added

- Warning on `case <-time.After(...)` inside a `select` block --
  leaks its Timer until it fires, especially costly inside a loop.
- 100% static text analysis, no Go plugin dependency, no network
  calls, no telemetry. Free.

[Unreleased]: https://github.com/GapHunterLabs/go-timer-leak-companion/compare/0.1.0...HEAD
[0.1.0]: https://github.com/GapHunterLabs/go-timer-leak-companion/commits/0.1.0
