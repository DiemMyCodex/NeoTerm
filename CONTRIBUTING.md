# Contributing to NeoTerm

Thanks for helping make NeoTerm faster and prettier. This fork keeps the Termux engine intact and pushes all innovation into the Android UI layer, so contributions have a few non-negotiable invariants.

## Ground rules

1. **Preserve the focus contract.** `TerminalView` must remain the only focusable view. Every new chrome view needs `android:focusable="false"`, `android:focusableInTouchMode="false"`, and its container should use `android:descendantFocusability="blocksDescendants"`. Call `restoreTerminalFocusAndIme(shouldRestoreIme())` whenever a popup/dialog closes without opening another UI. The window keeps `adjustResize|stateVisible`.
2. **Preserve the collapse contract.** The extra-keys dock snaps its width instantly (no `LayoutTransition`, no width animator) and calls `setCurrentItem(position, false)` so ViewPager children re-measure. The collapsed pill stays pinned right.
3. **No background polling for themes.** Theme work is synchronous on tap and in `onCreate()` only. The only permitted periodic thread is `SessionProcessMonitor` (1800ms, off-main, change-gated).
4. **Native Android SDK only.** Do not add new Gradle dependencies for UI work.
5. **Design tokens, not magic hex.** Colors/dimens belong in `app/src/main/res/values/` or `termux-shared/src/main/res/values/` as `neoterm_*` tokens.

## Development setup

- **JDK 17**
- Android SDK platform + NDK versions declared in `gradle.properties` (currently platform 36, NDK 29.0.14206865)
- Network access for the bootstrap zip on first build

```bash
git clone https://github.com/DiemMyCodex/NeoTerm.git
cd NeoTerm
./gradlew assembleDebug          # ARM64 debug APK
./gradlew test                   # fast JVM unit suite
```

We do not require a local Android SDK for review — **all verification happens in GitHub Actions** on the PR. Push a branch and open a PR; the `Build`, `Unit tests` and `Validate Gradle Wrapper` jobs must all be green.

## Architecture notes

- `com.termux.app.terminal.theme.ThemeRegistry` / `TerminalThemeSpec` are pure JVM models. Keep them free of Android imports so `ThemeRegistryTest` runs without a device.
- Chrome state changes live in small testable specs (`SessionTabUiLogic`, `ExtraKeysDockState`, `SessionProcessMonitor`) with JUnit coverage.
- Applying a theme touches four surfaces: terminal ANSI palette via `TerminalColors`, glass chrome drawables, `TermuxSessionTabAdapter` colors, and `ExtraKeysView` keycaps. Use `GradientDrawable` fill/stroke and text color setters only — never recreate the activity for a theme tap.

## Commit & PR process

- Use [Conventional Commits](https://www.conventionalcommits.org): `feat(theme): add Dracula palette`, `fix(focus): restore IME after palette dismissal`.
- Keep PRs focused; one theme, one bug, one refactor.
- Fill in the PR template checklist, especially the focus and collapse items.
- Update `CHANGELOG.md` under **Unreleased** for user-visible changes.
- For screenshot changes, run `scripts/capture_showcase.sh` on a connected device and commit the four PNGs.

## Reporting issues

Use the issue templates and include the exact theme ID (`obsidian`, `catppuccin_mocha`, `tokyo_night`, `dracula`), Android version, device model, and the SHA-256 of the APK you installed if possible.

NeoTerm is GPL-3.0 only; by contributing you agree your code is licensed the same way. Please retain the upstream `termux/termux-app` attribution.
