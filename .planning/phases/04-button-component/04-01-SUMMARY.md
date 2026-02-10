---
phase: 04-button-component
plan: 01
subsystem: ui
tags: [swing, button, border, focus-ring, hsl, uidefaults, css-tokens]

# Dependency graph
requires:
  - phase: 03-shared-painting-utilities
    provides: PaintUtils.paintOutline, PaintUtils.setupPaintingHints/restorePaintingHints
  - phase: 02-uidefaults-bridge-laf-setup
    provides: CssTokenMap, UIDefaultsPopulator, token-mapping.properties pipeline
  - phase: 01-css-token-engine
    provides: CssThemeLoader, CssValue types (DimensionValue, IntegerValue, FloatValue)
provides:
  - DwcButtonBorder with focus-width-aware insets and outline painting
  - Button.borderColor mapped from CSS token --dwc-button-border-color
  - Component.focusRingColor computed from CSS HSL tokens and stored in UIDefaults
  - Button.margin, Button.minimumWidth, Button.iconTextGap, Button.rollover in UIDefaults
affects: [04-02 DwcButtonUI delegate, future component borders]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Focus-width-aware border insets (focusWidth + borderWidth + margin)"
    - "HSL-to-RGB conversion from CSS token map values in L&F initialization"
    - "DimensionValue percentage extraction via tokenMap.get() and pattern match"

key-files:
  created:
    - src/main/java/com/dwc/laf/ui/DwcButtonBorder.java
    - src/test/java/com/dwc/laf/ui/DwcButtonBorderTest.java
  modified:
    - src/main/resources/com/dwc/laf/token-mapping.properties
    - src/main/java/com/dwc/laf/DwcLookAndFeel.java
    - src/test/java/com/dwc/laf/DwcLookAndFeelTest.java

key-decisions:
  - "Focus ring color stored as Component.focusRingColor (not Button.focusRingColor) so all components share it"
  - "Private hslToColor helper in DwcLookAndFeel (not reusing CssColorParser internals)"
  - "DimensionValue with % unit read via tokenMap.get() pattern match (not getString)"

patterns-established:
  - "Border insets = focusWidth + borderWidth + margin pattern for all future component borders"
  - "initButtonDefaults method pattern for component-specific UIDefaults setup"

# Metrics
duration: 3min
completed: 2026-02-10
---

# Phase 4 Plan 1: DwcButtonBorder and Button UIDefaults Summary

**DwcButtonBorder with focus-width-aware insets, outline painting via PaintUtils, and computed HSL focus ring color from CSS tokens**

## Performance

- **Duration:** 3 min
- **Started:** 2026-02-10T11:13:21Z
- **Completed:** 2026-02-10T11:16:41Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments
- DwcButtonBorder with getBorderInsets that includes focusWidth + borderWidth + margin on all sides, respecting application-set non-UIResource margins
- Border outline painting via PaintUtils.paintOutline with borderColor null-guard and isBorderPainted() check
- Focus ring color computed from 4 CSS HSL tokens (primary-h, primary-s, focus-ring-l, focus-ring-a) and stored as Component.focusRingColor
- Button.borderColor mapped from --dwc-button-border-color in token-mapping.properties
- Button.margin (2,14,2,14), Button.minimumWidth (72), Button.iconTextGap (4), Button.rollover (true) installed in UIDefaults

## Task Commits

Each task was committed atomically:

1. **Task 1: DwcButtonBorder with focus-width-aware insets and outline painting** - `d91d16b` (feat)
2. **Task 2: Token mapping additions and button-specific UIDefaults in DwcLookAndFeel** - `002520b` (feat)

## Files Created/Modified
- `src/main/java/com/dwc/laf/ui/DwcButtonBorder.java` - Custom border with focus-width-aware insets and outline painting via PaintUtils
- `src/test/java/com/dwc/laf/ui/DwcButtonBorderTest.java` - 7 tests covering insets calculation, margin override, and painting
- `src/main/resources/com/dwc/laf/token-mapping.properties` - Added --dwc-button-border-color to Button.borderColor mapping
- `src/main/java/com/dwc/laf/DwcLookAndFeel.java` - Added initButtonDefaults(), initFocusRingColor(), hslToColor(), hueToRgb() helpers
- `src/test/java/com/dwc/laf/DwcLookAndFeelTest.java` - 5 new tests for focus ring color, margin, minimumWidth, borderColor, rollover

## Decisions Made
- Focus ring color stored as Component.focusRingColor (shared across all components) rather than Button-specific key
- Private hslToColor/hueToRgb helper in DwcLookAndFeel rather than exposing CssColorParser internals
- DimensionValue with "%" unit accessed via tokenMap.get() with pattern matching on CssValue.DimensionValue

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- DwcButtonBorder is ready for DwcButtonUI to use as its border (Plan 04-02)
- Component.focusRingColor, Button.borderColor, Button.margin, and all dimension UIDefaults available for the ButtonUI delegate
- ui/ package created and ready for DwcButtonUI class

## Self-Check: PASSED

- All 5 created/modified files exist on disk
- Both task commits (d91d16b, 002520b) found in git log
- DwcButtonBorder.java: 84 lines (min 60 required)
- DwcButtonBorderTest.java: 188 lines (min 40 required)
- All 319 tests pass (0 failures, 0 skipped)

---
*Phase: 04-button-component*
*Completed: 2026-02-10*
