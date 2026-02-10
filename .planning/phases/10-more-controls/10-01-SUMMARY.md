---
phase: 10-more-controls
plan: 01
subsystem: ui
tags: [swing, tooltip, progressbar, rounded-corners, shadow, css-tokens, color-variants]

# Dependency graph
requires:
  - phase: 09-button-theme-variants
    provides: "L&F infrastructure, PaintUtils, ShadowPainter, variant pattern"
provides:
  - "DwcToolTipUI with rounded background and shadow border"
  - "DwcToolTipBorder with ShadowPainter drop shadow"
  - "DwcProgressBarUI with determinate, indeterminate, and 4 color variants"
  - "Token mappings for tooltip and progress bar components"
affects: [10-02, 10-03, 10-04, demo-application]

# Tech tracking
tech-stack:
  added: []
  patterns: ["Area intersection clipping for rounded fill bars", "BasicToolTipUI text delegation with opaque=false"]

key-files:
  created:
    - src/main/java/com/dwc/laf/ui/DwcToolTipUI.java
    - src/main/java/com/dwc/laf/ui/DwcToolTipBorder.java
    - src/main/java/com/dwc/laf/ui/DwcProgressBarUI.java
    - src/test/java/com/dwc/laf/ui/DwcToolTipUITest.java
    - src/test/java/com/dwc/laf/ui/DwcProgressBarUITest.java
  modified:
    - src/main/resources/com/dwc/laf/token-mapping.properties
    - src/main/java/com/dwc/laf/DwcLookAndFeel.java

key-decisions:
  - "Area intersection clipping for progress bar fill to preserve track rounded corners"
  - "BasicToolTipUI.paint() text delegation via opaque=false (skip bg fill, let super handle text)"
  - "dwc.progressType client property for color variant activation (consistent with dwc.buttonType pattern)"

patterns-established:
  - "Area intersection clipping: fill shape intersected with track shape for correct rounded corner rendering"
  - "Client property variant pattern: dwc.{component}Type for variant color resolution"

# Metrics
duration: 4min
completed: 2026-02-10
---

# Phase 10 Plan 01: Tooltip & Progress Bar Summary

**JToolTip with rounded corners and ShadowPainter drop shadow, JProgressBar with rounded track/fill, 4 color variants, and indeterminate bounce animation**

## Performance

- **Duration:** 4 min
- **Started:** 2026-02-10T20:57:40Z
- **Completed:** 2026-02-10T21:02:02Z
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments
- JToolTip renders with rounded background, themed foreground/background from CSS tokens, and drop shadow via DwcToolTipBorder
- JProgressBar renders with rounded track and rounded fill bar clipped via Area intersection
- JProgressBar supports 4 color variants (success, danger, warning, info) via dwc.progressType client property
- JProgressBar indeterminate mode renders a bouncing rounded bar within the track
- Both components registered in DwcLookAndFeel.initClassDefaults with full token mapping

## Task Commits

Each task was committed atomically:

1. **Task 1: DwcToolTipUI + DwcToolTipBorder with shadow and rounded corners** - `d5f4cb0` (feat)
2. **Task 2: DwcProgressBarUI with rounded bars and color variants** - `b4953d4` (feat)

## Files Created/Modified
- `src/main/java/com/dwc/laf/ui/DwcToolTipUI.java` - ToolTip UI delegate with rounded background painting
- `src/main/java/com/dwc/laf/ui/DwcToolTipBorder.java` - Rounded border with shadow insets for tooltip
- `src/main/java/com/dwc/laf/ui/DwcProgressBarUI.java` - ProgressBar UI delegate with determinate and indeterminate painting
- `src/test/java/com/dwc/laf/ui/DwcToolTipUITest.java` - 6 tests for tooltip UI
- `src/test/java/com/dwc/laf/ui/DwcProgressBarUITest.java` - 9 tests for progress bar UI
- `src/main/resources/com/dwc/laf/token-mapping.properties` - Added tooltip and progress bar token mappings
- `src/main/java/com/dwc/laf/DwcLookAndFeel.java` - Registered ToolTipUI and ProgressBarUI, added init methods

## Decisions Made
- Area intersection clipping for progress bar fill: fill shape intersected with track shape ensures correct rounding at both ends without overdraw
- BasicToolTipUI.paint() text delegation: set opaque=false so super.paint() only draws text (skips bg fill), allowing custom rounded background
- dwc.progressType client property for color variant activation: consistent with dwc.buttonType pattern from Phase 09

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Tooltip and progress bar components fully functional with themed styling
- Pattern established for remaining Phase 10 components (slider, spinner, etc.)
- Full test suite green (15 tests for new components, all existing tests pass)

## Self-Check: PASSED

All 6 created files verified. Both task commits (d5f4cb0, b4953d4) verified in git log.

---
*Phase: 10-more-controls*
*Completed: 2026-02-10*
