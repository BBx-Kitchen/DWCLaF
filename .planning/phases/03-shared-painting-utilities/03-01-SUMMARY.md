---
phase: 03-shared-painting-utilities
plan: 01
subsystem: ui
tags: [java2d, graphics2d, antialiasing, hidpi, rounded-corners, state-colors, swing-painting]

# Dependency graph
requires:
  - phase: 02-uidefaults-bridge-laf-setup
    provides: "DwcLookAndFeel class and UIDefaults population pipeline"
provides:
  - "PaintUtils: antialiasing hint management, rounded shape factory, outline painting, rounded background fill"
  - "HiDpiUtils: Graphics2D scale factor detection and device-resolution image creation"
  - "StateColorResolver: component state-to-color resolution with disabled>pressed>hover>focused>enabled priority"
affects: [04-button-delegates, 05-input-delegates, 06-selection-delegates, 07-container-delegates, 03-02-focus-ring-shadow]

# Tech tracking
tech-stack:
  added: []
  patterns: [static-utility-with-hint-save-restore, shape-factory-degeneration, even-odd-fill-outline, graphics2d-transform-scale-detection, state-priority-chain]

key-files:
  created:
    - src/main/java/com/dwc/laf/painting/PaintUtils.java
    - src/main/java/com/dwc/laf/painting/HiDpiUtils.java
    - src/main/java/com/dwc/laf/painting/StateColorResolver.java
    - src/test/java/com/dwc/laf/painting/PaintUtilsTest.java
    - src/test/java/com/dwc/laf/painting/HiDpiUtilsTest.java
    - src/test/java/com/dwc/laf/painting/StateColorResolverTest.java
  modified: []

key-decisions:
  - "Pressed state requires both isArmed() AND isPressed() on ButtonModel (FlatLaf pattern for correct mouse-within-bounds detection)"
  - "hasFocus() used directly on Component (not isFocusOwner) for broader compatibility"
  - "HiDPI scale read from Graphics2D transform only, never system properties or Toolkit"

patterns-established:
  - "Static utility with hint save/restore: setupPaintingHints returns saved state, restorePaintingHints restores it"
  - "Shape factory degeneration: arc=0 -> Rectangle2D, arc>=min(w,h) on square -> Ellipse2D, else -> RoundRectangle2D"
  - "Even-odd fill outline: Path2D.WIND_EVEN_ODD with outer+inner shapes for pixel-exact rings"
  - "State color resolution: disabled>pressed>hover>focused>enabled priority chain with null fallback to enabled"

# Metrics
duration: 3min
completed: 2026-02-10
---

# Phase 3 Plan 1: Foundation Painting Utilities Summary

**Three stateless Java2D painting utilities (PaintUtils, HiDpiUtils, StateColorResolver) providing antialiased shape rendering, HiDPI-aware image creation, and state-driven color selection for all future component delegates**

## Performance

- **Duration:** 3 min
- **Started:** 2026-02-10T09:59:49Z
- **Completed:** 2026-02-10T10:02:54Z
- **Tasks:** 3
- **Files created:** 6

## Accomplishments
- PaintUtils with 5 static methods: hint save/restore, smart rounded shape factory with degeneration, even-odd outline painting, and rounded background fill
- HiDpiUtils with Graphics2D transform-based scale detection and device-resolution BufferedImage creation
- StateColorResolver with disabled>pressed>hover>focused>enabled priority chain and opacity painting with composite save/restore
- 28 new tests, all passing alongside 265 existing tests (293 total, 0 failures)

## Task Commits

Each task was committed atomically:

1. **Task 1: PaintUtils - antialiasing, rounded shapes, outlines, and background fill** - `97cb5f8` (feat)
2. **Task 2: HiDpiUtils - scale detection and device-resolution image creation** - `952a96d` (feat)
3. **Task 3: StateColorResolver - component state priority chain and opacity painting** - `1e3f270` (feat)

## Files Created/Modified
- `src/main/java/com/dwc/laf/painting/PaintUtils.java` - Antialiasing setup/restore, rounded shape factory with degeneration, outline via WIND_EVEN_ODD, rounded background fill
- `src/main/java/com/dwc/laf/painting/HiDpiUtils.java` - Scale factor from Graphics2D transform, HiDPI-resolution BufferedImage creation
- `src/main/java/com/dwc/laf/painting/StateColorResolver.java` - State-to-color resolution with priority chain, opacity painting with composite management
- `src/test/java/com/dwc/laf/painting/PaintUtilsTest.java` - 10 tests: hint save/restore, shape degeneration, outline painting, background fill
- `src/test/java/com/dwc/laf/painting/HiDpiUtilsTest.java` - 7 tests: 1x/2x scale, null graphics, zero dimensions, image type
- `src/test/java/com/dwc/laf/painting/StateColorResolverTest.java` - 11 tests: all priority combinations, fallback, opacity clamping

## Decisions Made
- Pressed state checks both `isArmed() AND isPressed()` (FlatLaf pattern) -- ensures mouse is still within button bounds during press
- Used `hasFocus()` on Component rather than `isFocusOwner()` for broader compatibility with components that override `hasFocus()`
- HiDPI scale always from `Graphics2D.getTransform().getScaleX()`, never from system properties or `Toolkit.getScreenResolution()`

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- All three painting utilities ready for consumption by Plan 03-02 (FocusRingPainter, ShadowPainter)
- `PaintUtils.createRoundedShape` and `PaintUtils.paintOutline` are prerequisites for focus ring painting
- `HiDpiUtils.createHiDpiImage` needed for shadow cache images at device resolution
- `StateColorResolver.resolve` ready for all component delegates in Phases 4-7

## Self-Check: PASSED

All 7 files verified on disk. All 3 task commits verified in git log.

---
*Phase: 03-shared-painting-utilities*
*Completed: 2026-02-10*
