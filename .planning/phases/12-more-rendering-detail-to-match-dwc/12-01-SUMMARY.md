---
phase: 12-more-rendering-detail-to-match-dwc
plan: 01
subsystem: ui
tags: [swing, token-mapping, button, font, css-tokens]

# Dependency graph
requires:
  - phase: 04-button-component
    provides: DwcButtonUI and DwcButtonBorder component delegates
  - phase: 02-uidefaults-bridge-laf-setup
    provides: Token mapping pipeline and UIDefaultsPopulator
provides:
  - Correct default button foreground (black on gray, not white on gray)
  - Correct default button border (subtle gray, not bright blue)
  - Bold button font weight matching DWC semibold (font-weight 500)
affects: [12-02, 12-03, button-rendering, token-mapping]

# Tech tracking
tech-stack:
  added: []
  patterns: [token-collision-resolution, font-derivation-override]

key-files:
  created: []
  modified:
    - src/main/resources/com/dwc/laf/token-mapping.properties
    - src/main/java/com/dwc/laf/DwcLookAndFeel.java

key-decisions:
  - "Remove --dwc-button-color and --dwc-button-border-color collision lines rather than reorder (flattened CSS always overwrites)"
  - "Map Button.borderColor to --dwc-color-default (same gray as button background) for subtle same-tone border"
  - "Use Font.BOLD for button text (Java nearest equivalent to DWC font-weight 500 semibold)"

patterns-established:
  - "Token collision resolution: when flattened CSS token overwrites a correct value, remove the collision source entirely and map to the correct token"
  - "Font weight override: derive component-specific bold font from defaultFont in initComponentDefaults rather than mapping from CSS token"

# Metrics
duration: 7min
completed: 2026-02-11
---

# Phase 12 Plan 01: Token Mapping Collisions Summary

**Fixed button foreground/border token collisions and added bold font derivation for DWC-matching button text weight**

## Performance

- **Duration:** 7 min
- **Started:** 2026-02-11T16:19:45Z
- **Completed:** 2026-02-11T16:27:10Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- Fixed critical token-mapping collision where --dwc-button-color (flattened to white) overwrote Button.foreground, making default button text invisible
- Fixed token-mapping collision where --dwc-button-border-color (flattened to primary blue) overwrote Button.borderColor, making default button border bright blue
- Added bold font derivation for buttons matching DWC font-weight 500 (semibold)

## Task Commits

Each task was committed atomically:

1. **Task 1: Fix token-mapping collisions for button foreground and border colors** - `885d5af` (fix)
2. **Task 2: Add bold font derivation for button text** - `05f3353` (feat)

## Files Created/Modified
- `src/main/resources/com/dwc/laf/token-mapping.properties` - Removed --dwc-button-color and --dwc-button-border-color collision lines, added Button.borderColor to --dwc-color-default
- `src/main/java/com/dwc/laf/DwcLookAndFeel.java` - Added Font.BOLD derivation for Button.font in initButtonDefaults

## Decisions Made
- Removed --dwc-button-color mapping entirely rather than trying to reorder: CSS flattening always picks the last declaration, so the only fix is removing the collision source
- Mapped Button.borderColor to --dwc-color-default (neutral gray) which gives the subtle same-tone border DWC uses for default buttons
- Used Font.BOLD as nearest Java equivalent to DWC's font-weight 500 (semibold), since Java Font API has no intermediate weight

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Button foreground and border colors now correctly mapped for default variant
- Button text appears with proper bold weight
- Ready for Plan 02 (further rendering refinements)

## Self-Check: PASSED

All files exist. All commits verified (885d5af, 05f3353).

---
*Phase: 12-more-rendering-detail-to-match-dwc*
*Completed: 2026-02-11*
