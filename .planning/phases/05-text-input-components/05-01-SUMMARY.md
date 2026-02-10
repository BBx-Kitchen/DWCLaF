---
phase: 05-text-input-components
plan: 01
subsystem: ui
tags: [swing, textfield, border, uidefaults, css-tokens, laf]

# Dependency graph
requires:
  - phase: 04-button-component
    provides: "DwcButtonBorder pattern, PaintUtils, DwcLookAndFeel with initButtonDefaults"
  - phase: 03-shared-painting-utilities
    provides: "PaintUtils.paintOutline, setupPaintingHints"
provides:
  - "DwcTextFieldBorder with focus-width-aware insets and state-aware outline"
  - "TextField hover/focus border and background colors in UIDefaults"
  - "TextFieldUI class default registration"
  - "TextField.border and TextField.margin in UIDefaults"
affects: [05-02-textfield-ui-delegate]

# Tech tracking
tech-stack:
  added: []
  patterns: ["Client property hover state communication (DwcTextFieldUI.hover)", "JTextComponent margin UIResource check pattern"]

key-files:
  created:
    - src/main/java/com/dwc/laf/ui/DwcTextFieldBorder.java
    - src/test/java/com/dwc/laf/ui/DwcTextFieldBorderTest.java
  modified:
    - src/main/resources/com/dwc/laf/token-mapping.properties
    - src/main/java/com/dwc/laf/DwcLookAndFeel.java
    - src/test/java/com/dwc/laf/DwcLookAndFeelTest.java

key-decisions:
  - "Hover state via DwcTextFieldUI.hover client property (JTextComponent has no rollover model)"
  - "TextField uses 2,6,2,6 default margin (tighter than button's 2,14,2,14)"
  - "Removed --dwc-input-color mapping (currentColor unparseable; --dwc-color-black already covers it)"
  - "Removed TextField.background from --dwc-color-white (--dwc-input-background is more specific)"

patterns-established:
  - "Client property for hover state: borders read JComponent.getClientProperty for state without rollover model"
  - "Token conflict resolution: more-specific CSS token wins over generic color when both map to same key"

# Metrics
duration: 3min
completed: 2026-02-10
---

# Phase 5 Plan 1: TextField Border and UIDefaults Summary

**DwcTextFieldBorder with state-aware outline painting, token mapping fixes for hover/focus colors, and TextFieldUI L&F registration**

## Performance

- **Duration:** 3 min
- **Started:** 2026-02-10T11:52:00Z
- **Completed:** 2026-02-10T11:55:18Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments
- DwcTextFieldBorder with focus-width-aware insets (focusWidth + borderWidth + margin) and state-aware border color resolution (hover/focus/normal)
- Fixed token mapping conflicts: removed TextField.background from --dwc-color-white (--dwc-input-background wins), removed unparseable --dwc-input-color line
- Added hover border color and hover background token mappings for text field interactive states
- Registered TextFieldUI class default and installed TextField.border/TextField.margin in UIDefaults

## Task Commits

Each task was committed atomically:

1. **Task 1: DwcTextFieldBorder with focus-width-aware insets and state-aware outline painting** - `cbc8543` (feat)
2. **Task 2: Token mapping updates and TextField UIDefaults in DwcLookAndFeel** - `e12e171` (feat)

## Files Created/Modified
- `src/main/java/com/dwc/laf/ui/DwcTextFieldBorder.java` - Custom border with focus ring space, state-aware outline color (hover/focus/normal)
- `src/test/java/com/dwc/laf/ui/DwcTextFieldBorderTest.java` - 8 tests covering insets, margin override, paint behavior, and state-based color
- `src/main/resources/com/dwc/laf/token-mapping.properties` - Fixed conflicts, added hover mappings
- `src/main/java/com/dwc/laf/DwcLookAndFeel.java` - TextFieldUI registration, initTextFieldDefaults method
- `src/test/java/com/dwc/laf/DwcLookAndFeelTest.java` - 5 new tests for TextField UIDefaults population

## Decisions Made
- Hover state communicated via `DwcTextFieldUI.hover` client property since JTextComponent has no rollover model (DwcTextFieldUI delegate in Plan 05-02 will set this from MouseListener)
- TextField uses tighter 2,6,2,6 default margin compared to button's 2,14,2,14
- Removed `--dwc-input-color` mapping entirely because `currentColor` CSS value is unparseable and `--dwc-color-black` already provides `TextField.foreground`
- Removed `TextField.background` from `--dwc-color-white` line so the more-specific `--dwc-input-background` mapping prevails

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- DwcTextFieldBorder is ready for DwcTextFieldUI delegate (Plan 05-02) to use
- TextFieldUI class default already points to DwcTextFieldUI (will be created in Plan 05-02)
- Hover border color and background tokens are in UIDefaults, ready for the delegate's paint logic
- The DwcTextFieldUI.hover client property protocol is established for the delegate's MouseListener

## Self-Check: PASSED

All 6 files verified present. Both task commits (cbc8543, e12e171) verified in git log. 342 tests pass with zero failures.

---
*Phase: 05-text-input-components*
*Completed: 2026-02-10*
