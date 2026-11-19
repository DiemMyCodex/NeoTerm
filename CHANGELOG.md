# Changelog

All notable changes to NeoTerm are documented here. This project adheres to
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and uses semantic version names
from `app/build.gradle`.

## [Unreleased]

### Added
- Four synchronized 2026 themes (`obsidian`, `catppuccin_mocha`, `tokyo_night`, `dracula`) applied atomically to the ANSI canvas, glass pill bar, ambient glow, tab chips and keycaps.
- Live palette switcher: a 34dp `ic_palette` button beside the `+` tab opens a focus-safe 220dp glass popup with dual swatches and haptic feedback. Selection is persisted in `SharedPreferences` under `neoterm_active_theme`.
- Pure-JVM `ThemeRegistry` / `TerminalThemeSpec` core with 16-color ANSI palettes and full chrome ARGB tokens, covered by `ThemeRegistryTest` (zero Android UI dependencies).
- `.github/workflows/release.yml` producing ARM64 + universal APKs, `SHA256SUMS.txt` and public GitHub Releases with generated notes.
- Flagship `README.md`, `CONTRIBUTING.md`, this changelog, issue/PR templates and `scripts/capture_showcase.sh`.

### Changed
- Repository presentation, topics and description aligned with the NeoTerm 2026 identity while retaining upstream GPLv3 attribution to `termux/termux-app`.

---

## [0.2.0] — Floating pill keyboard hardening

### Added
- `SessionProcessMonitor`: single daemon thread scanning `/proc` every 1800ms, diffing state off-main and posting to the UI thread only on real changes, with `onStart/onStop/onDestroy` lifecycle cleanup.
- `SessionTabUiLogic`, `ExtraKeysDockState` and `TactilePressTouchListener` pure-JVM specs with 20 fast JUnit tests.
- 33 semantic `neoterm_*` design tokens extracted in `app/` and `termux-shared/` resource values (no visual change).

### Fixed
- Terminal focus contract preserved: `adjustResize|stateVisible`, all chrome `focusable=false`, `blocksDescendants`, and `restoreTerminalFocusAndIme()` on every dismissal.
- Collapse remains non-animated: instant width snap plus `setCurrentItem(..., false)` re-measure; no layout transitions on the dock.

---

## [0.1.0] — Liquid Glass chrome

### Added
- Floating glass session tab bar with live idle/busy status dots.
- Collapsible glass extra-keys dock with weighted keycaps and right-pinned collapsed pill.
- ARM64-only GitHub Actions build pipeline with per-commit debug APK artifacts and `sha256sums`.
