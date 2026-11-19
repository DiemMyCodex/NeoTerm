## What changed

<!-- Short summary. Link the issue if there is one. -->

## Theme / UI checklist

- [ ] Terminal focus contract intact: chrome views non-focusable, `blocksDescendants`, `adjustResize|stateVisible`, focus + IME restored on dismissal.
- [ ] Collapse contract intact: instant width snap, `setCurrentItem(..., false)` re-measure, no `LayoutTransition` on the dock.
- [ ] Theme work is synchronous (tap + `onCreate()`), no polling/background loops added.
- [ ] New colors/dimens are `neoterm_*` tokens; no magic hex in drawables.
- [ ] No new Gradle dependencies.
- [ ] Pure-JVM theme/model changes have JUnit coverage.

## Verification

- [ ] `Build`, `Unit tests` and `Validate Gradle Wrapper` CI jobs are green.
- [ ] If the APK changed, downloaded the ARM64 artifact and verified `SHA256SUMS.txt`.
- [ ] `CHANGELOG.md` updated under **Unreleased** for user-visible changes.

## Screenshots

<!-- Before/after or the four theme captures if the visual surface changed. -->
