---
phase: 09-button-theme-variants-custom-css-themes
plan: 01
subsystem: ui
tags: [swing, button-variants, css-tokens, focus-ring, uidefaults, hsl]

# Dependency graph
requires:
  - phase: 04-button-component
    provides: DwcButtonUI, DwcButtonBorder, focus ring color computation
  - phase: 01-css-token-engine
    provides: CssTokenMap, CssThemeLoader, token-mapping.properties
provides:
  - VariantColors record and Map-based variant lookup in DwcButtonUI
  - Token mappings for success/danger/warning/info button variants
  - Per-variant focus ring colors (Component.focusRingColor.{variant})
  - Variant-aware DwcButtonBorder reading dwc.buttonType client property
affects: [09-02, demo-application, button-component]

# Tech tracking
tech-stack:
  added: []
  patterns: [variant-keyed-color-resolution, per-variant-focus-ring, client-property-variant-selection]

key-files:
  created: []
  modified:
    - src/main/resources/com/dwc/laf/token-mapping.properties
    - src/main/java/com/dwc/laf/DwcLookAndFeel.java
    - src/main/java/com/dwc/laf/ui/DwcButtonUI.java
    - src/main/java/com/dwc/laf/ui/DwcButtonBorder.java
    - src/test/java/com/dwc/laf/ui/DwcButtonUITest.java

key-decisions:
  - "VariantColors record encapsulates 5 colors per variant (background, foreground, hover, pressed, focusRing)"
  - "Map<String, VariantColors> replaces boolean isPrimary for N-variant support"
  - "Per-variant focus ring computed from --dwc-color-{variant}-h/s with shared --dwc-focus-ring-l/a"
  - "DwcButtonBorder resolves variant border via dwc.buttonType client property lookup"

patterns-established:
  - "Variant-keyed color resolution: Map<String, VariantColors> pattern for supporting N button variants"
  - "Per-variant focus ring: Component.focusRingColor.{variant} UIDefaults key convention"
  - "Client property variant selection: dwc.buttonType maps to variant name string"

# Metrics
duration: 3min
completed: 2026-02-10
---

# Phase 9 Plan 1: Button Variant Infrastructure Summary

**VariantColors record with Map-based lookup for 6 button variants, per-variant focus ring HSL computation, and variant-aware border color resolution**

## Performance

- **Duration:** 3 min
- **Started:** 2026-02-10T20:12:24Z
- **Completed:** 2026-02-10T20:16:16Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments
- Added 20 token mapping entries for success/danger/warning/info button variant colors from CSS semantic tokens
- Refactored DwcButtonUI from boolean isPrimary to VariantColors record + Map-based variant lookup supporting 6 variants
- Per-variant focus ring colors computed from HSL tokens (variant-specific hue/saturation, shared lightness/alpha)
- DwcButtonBorder reads dwc.buttonType client property and resolves variant-specific border colors
- 6 new tests covering all variant smoke paints, unknown variant fallback, and variant color differentiation

## Task Commits

Each task was committed atomically:

1. **Task 1: Token mappings and per-variant focus ring colors** - `ddb9f64` (feat)
2. **Task 2: Variant-keyed DwcButtonUI refactor and variant-aware DwcButtonBorder** - `0ef5f43` (feat)

## Files Created/Modified
- `src/main/resources/com/dwc/laf/token-mapping.properties` - Added 20 variant token mappings (5 per variant x 4 variants), appended to existing --dwc-color-danger and --dwc-color-primary lines
- `src/main/java/com/dwc/laf/DwcLookAndFeel.java` - Added initVariantFocusRingColors() method and Component.focusRingColor.primary key
- `src/main/java/com/dwc/laf/ui/DwcButtonUI.java` - Replaced isPrimary boolean with VariantColors record and Map<String, VariantColors> variant lookup
- `src/main/java/com/dwc/laf/ui/DwcButtonBorder.java` - Added resolveBorderColor() method reading dwc.buttonType for per-variant border colors
- `src/test/java/com/dwc/laf/ui/DwcButtonUITest.java` - Added 6 new tests (249 lines total)

## Decisions Made
- VariantColors record encapsulates 5 colors per variant (background, foreground, hoverBackground, pressedBackground, focusRingColor)
- Map<String, VariantColors> replaces boolean isPrimary for clean N-variant support
- Per-variant focus ring computed from --dwc-color-{variant}-h/s with shared --dwc-focus-ring-l/a
- DwcButtonBorder resolves variant border via dwc.buttonType client property lookup (not subclassing)
- Primary variant borderColor mapped from --dwc-color-primary (border = background for primary, matching DWC SCSS pattern)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- All 4 button variants (success, danger, warning, info) resolve their colors from CSS semantic tokens through UIDefaults
- Ready for Plan 02: gallery demo with variant showcase and theme switching UI
- The dwc.buttonType client property API is fully functional for application code

## Self-Check: PASSED

All files verified present. All commit hashes verified in git log.

---
*Phase: 09-button-theme-variants-custom-css-themes*
*Completed: 2026-02-10*
