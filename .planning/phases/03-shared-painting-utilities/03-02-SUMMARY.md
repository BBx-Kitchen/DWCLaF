---
phase: 03-shared-painting-utilities
plan: 02
subsystem: ui
tags: [java2d, focus-ring, box-shadow, gaussian-blur, convolveop, softreference, caching, even-odd-fill]

# Dependency graph
requires:
  - phase: 03-shared-painting-utilities
    plan: 01
    provides: "PaintUtils (createRoundedShape, setupPaintingHints, restorePaintingHints) and HiDpiUtils"
provides:
  - "FocusRingPainter: semi-transparent focus ring outside component bounds via even-odd fill"
  - "ShadowPainter: Gaussian-blurred box shadow with SoftReference image caching"
affects: [04-button-delegates, 05-input-delegates, 06-selection-delegates, 07-container-delegates]

# Tech tracking
tech-stack:
  added: []
  patterns: [even-odd-focus-ring, two-pass-separable-gaussian-blur, softreference-image-cache, concurrent-cache-key-record]

key-files:
  created:
    - src/main/java/com/dwc/laf/painting/FocusRingPainter.java
    - src/main/java/com/dwc/laf/painting/ShadowPainter.java
    - src/test/java/com/dwc/laf/painting/FocusRingPainterTest.java
    - src/test/java/com/dwc/laf/painting/ShadowPainterTest.java
  modified: []

key-decisions:
  - "Shadow blur radius clamped to 50px max (DWC uses up to 100px but excessive for Swing)"
  - "Shadow cache uses record ShadowCacheKey with auto hashCode/equals for all visual parameters"
  - "applyGaussianBlur made package-private for direct testability (consistent with project pattern)"
  - "Shadow images drawn at logical dimensions, Graphics2D transform handles HiDPI scaling"

patterns-established:
  - "Focus ring via Path2D.WIND_EVEN_ODD: outer shape (component + ringWidth) minus inner shape (component)"
  - "Outer arc expansion: outerArc = componentArc + ringWidth for smooth ring curvature"
  - "Two-pass separable Gaussian blur: horizontal ConvolveOp then vertical ConvolveOp, EDGE_ZERO_FILL"
  - "SoftReference image cache: ConcurrentHashMap<CacheKey, SoftReference<BufferedImage>> for GC-friendly caching"
  - "Blur padding: 3*blurRadius on each side to prevent edge clipping"

# Metrics
duration: 3min
completed: 2026-02-10
---

# Phase 3 Plan 2: FocusRingPainter and ShadowPainter Summary

**DWC-style focus ring via even-odd fill outside component bounds, and Gaussian-blurred box shadow with ConcurrentHashMap/SoftReference image caching**

## Performance

- **Duration:** 3 min
- **Started:** 2026-02-10T10:05:11Z
- **Completed:** 2026-02-10T10:08:40Z
- **Tasks:** 2
- **Files created:** 4

## Accomplishments
- FocusRingPainter with even-odd fill between outer (expanded by ringWidth) and inner rounded shapes, matching DWC `box-shadow: 0 0 0 3px hsla(H,S,45%,0.4)` focus style
- ShadowPainter with two-pass separable ConvolveOp Gaussian blur, EDGE_ZERO_FILL for soft shadow edges, and SoftReference image cache keyed by all visual parameters
- 14 new tests, all passing alongside 293 existing tests (307 total, 0 failures, 0 skipped)
- Complete painting package: PaintUtils, HiDpiUtils, StateColorResolver, FocusRingPainter, ShadowPainter

## Task Commits

Each task was committed atomically:

1. **Task 1: FocusRingPainter - DWC-style focus ring outside component bounds** - `95dc8ef` (feat)
2. **Task 2: ShadowPainter - box shadow with Gaussian blur and SoftReference caching** - `8e34478` (feat)

## Files Created/Modified
- `src/main/java/com/dwc/laf/painting/FocusRingPainter.java` - Semi-transparent focus ring via Path2D WIND_EVEN_ODD fill with outer arc expansion
- `src/main/java/com/dwc/laf/painting/ShadowPainter.java` - Box shadow with two-pass Gaussian blur, SoftReference cache, 50px blur clamp
- `src/test/java/com/dwc/laf/painting/FocusRingPainterTest.java` - 6 tests: null/zero guards, pixel painting, color preservation, hint restore, arc expansion
- `src/test/java/com/dwc/laf/painting/ShadowPainterTest.java` - 8 tests: null/zero guards, soft pixels, caching, cache miss, blur clamping, blur spread, zero radius

## Decisions Made
- Shadow blur radius clamped to 50px max -- DWC CSS uses up to 100px but that is excessive for Swing component rendering and risks large memory allocations
- ShadowCacheKey implemented as a Java record for automatic hashCode/equals covering all visual parameters (width, height, arc, blurRadius, colorRgb)
- applyGaussianBlur made package-private (not private) for direct testability, consistent with project's convention for testable helpers
- Shadow images drawn at logical dimensions; the Graphics2D transform handles HiDPI scaling naturally (device-resolution shadow images deferred as optimization)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- All 5 painting utilities complete: PaintUtils, HiDpiUtils, StateColorResolver, FocusRingPainter, ShadowPainter
- Phase 03 (Shared Painting Utilities) fully complete
- Ready for Phase 04 (Button Delegates) which will use FocusRingPainter for focus-visible styling and ShadowPainter for elevation effects
- All component delegates in Phases 4-7 can now call `FocusRingPainter.paintFocusRing()` and `ShadowPainter.paintShadow()`

## Self-Check: PASSED

All 5 files verified on disk. Both task commits verified in git log.

---
*Phase: 03-shared-painting-utilities*
*Completed: 2026-02-10*
