## What changed

- release the HyperGesture 1.0 baseline and restore the Xposed scope configuration
- improve task-center touch selection, reverse ordering, motion timing, haptics, and visual stability
- preserve the existing centered system full-screen launch animation behind a setting
- add freely adjustable portrait and landscape freeform positions with scale-aware previews
- add honeycomb layout preview/editing with long-press drag-to-swap and immediate persistence
- keep blurred system wallpaper as the default honeycomb background while adding independent foreground-app color and live-interface blur options
- remove the discarded vector icons, three-column icon wall, and task-card tilt treatment
- add policy and geometry unit coverage plus implementation design notes

## Why

The previous gesture and task-center behavior could keep selecting the first app after edge dragging, move the whole task center unexpectedly, and expose animation and layout controls that did not match their real runtime behavior. The honeycomb background experiment also temporarily replaced the established blurred-wallpaper default instead of extending it.

## Impact

Users get more direct task selection, smoother and adjustable motion, predictable freeform placement, and an editable honeycomb layout. Existing wallpaper blur remains the default, while the new background modes are opt-in and fall back safely when live blur is unavailable.

## Root cause

Selection state was coupled to container movement thresholds, several animation settings shared or bypassed runtime timing paths, and preview/background configuration did not mirror the runtime geometry and precedence rules.

## Validation

- `./gradlew testDebugUnitTest assembleDebug`
- installed and verified `1.0-debug` build 136 on device `b88eeb49`
- `git diff --check`
