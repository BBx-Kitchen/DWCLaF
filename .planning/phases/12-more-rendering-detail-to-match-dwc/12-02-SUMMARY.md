---
phase: 12-more-rendering-detail-to-match-dwc
plan: 02
subsystem: ui
tags: [swing, progressbar, luminance, w3c, border-crispness, paintutils, even-odd-fill]

# Dependency graph
requires:
  - phase: 10-more-controls
    provides: DwcProgressBarUI with variant color support and rounded fill
  - phase: 03-shared-painting-utilities
    provides: PaintUtils.paintOutline even-odd fill technique
provides:
  - W3C relative luminance calculation for contrast-aware text color in ProgressBar
  - Integer pixel boundary snapping for maximum 1px border crispness across all components
affects: [12-more-rendering-detail-to-match-dwc]

# Tech tracking
tech-stack:
  added: []
  patterns: [w3c-relative-luminance, srgb-linearization, integer-pixel-snapping]

key-files:
  created: []
  modified:
    - src/main/java/com/dwc/laf/ui/DwcProgressBarUI.java
    - src/main/java/com/dwc/laf/painting/PaintUtils.java

key-decisions:
  - "Luminance threshold 0.4 for text color switching (white on dark fills, black on light fills)"
  - "Single text color for entire string based on dominant area (>50% fill or track), not split at fill boundary"
  - "Math.round() snapping on all paintOutline coordinates for integer pixel boundaries"

patterns-established:
  - "W3C luminance contrast: contrastTextColor(bg) with linearize(srgb) for accessible text color selection"
  - "Integer pixel snapping in paintOutline for crisp 1px borders at all scale factors"

# Metrics
duration: 2min
completed: 2026-02-11
---

# Phase 12 Plan 02: ProgressBar Text Contrast and Border Crispness Summary

**W3C luminance-based ProgressBar text color switching (white on dark fills, black on light fills) and integer pixel snapping for crisp 1px borders across all components**

## Performance

- **Duration:** 2 min
- **Started:** 2026-02-11T16:19:41Z
- **Completed:** 2026-02-11T16:27:37Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- ProgressBar percentage text dynamically switches between black and white based on fill color luminance using W3C relative luminance formula
- Dark fills (primary blue L~0.13, danger red, success green) get white text; light fills (warning amber L~0.74, track gray) get black text
- PaintUtils.paintOutline snaps all coordinates to integer pixel boundaries, preventing subpixel anti-aliasing from spreading 1px outlines across 2 device pixels
- All 505 existing tests pass without modification

## Task Commits

Each task was committed atomically:

1. **Task 1: Implement contrast-aware text color in DwcProgressBarUI** - `59bd25c` (feat)
2. **Task 2: Improve border crispness in PaintUtils.paintOutline** - `00ff09e` (feat)

## Files Created/Modified
- `src/main/java/com/dwc/laf/ui/DwcProgressBarUI.java` - Added contrastTextColor() and linearize() methods; paintString() now selects black/white text based on dominant area luminance
- `src/main/java/com/dwc/laf/painting/PaintUtils.java` - paintOutline() snaps x, y, w, h, lineWidth to integer boundaries before constructing even-odd fill path

## Decisions Made
- Luminance threshold of 0.4 (not 0.5) ensures white text on medium-dark fills like primary blue; threshold derived from DWC visual behavior where primary blue (L~0.13), danger red, and success green get white text while warning amber (L~0.74) gets black text
- Single text color for entire string based on dominant area (fill vs track) rather than split-color at fill boundary -- simpler implementation, correct for the majority of the visible text
- Math.round() for coordinate snapping (not floor/ceil) to minimize coordinate drift while ensuring integer alignment

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- ProgressBar text contrast is complete and handles all 5 variants (default/primary, success, danger, warning, info)
- Border crispness improvement affects all components using PaintUtils.paintOutline: DwcButtonBorder, DwcTextFieldBorder, DwcToolTipBorder
- Ready for plan 03 (remaining rendering refinements)

## Self-Check: PASSED

All files exist, all commits verified.

---
*Phase: 12-more-rendering-detail-to-match-dwc*
*Completed: 2026-02-11*
