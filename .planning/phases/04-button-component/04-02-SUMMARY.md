---
phase: 04-button-component
plan: 02
subsystem: ui
tags: [swing, button, buttonui, paint-pipeline, state-resolution, css-tokens, primary-variant]

# Dependency graph
requires:
  - phase: 04-button-component
    plan: 01
    provides: DwcButtonBorder, Button UIDefaults (margin, minimumWidth, focusRingColor, borderColor)
  - phase: 03-shared-painting-utilities
    provides: PaintUtils.paintRoundedBackground, FocusRingPainter.paintFocusRing, StateColorResolver.resolve/paintWithOpacity
  - phase: 02-uidefaults-bridge-laf-setup
    provides: UIDefaultsPopulator pipeline, DwcLookAndFeel base class
provides:
  - DwcButtonUI delegate with complete 6-step paint pipeline (background, border, icon, text, focus ring)
  - Primary variant support via dwc.buttonType client property
  - State-aware rendering (normal, hover, pressed, focused, disabled)
  - ButtonUI class default registration in DwcLookAndFeel
  - Button.border UIResource registration in initButtonDefaults
affects: [future component delegates follow same pattern, 05-text-field-component]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "ComponentUI delegate extending BasicXxxUI with full paint() override (no super.paint)"
    - "Per-component UI instances via createUI returning new instance"
    - "installDefaults reads all colors/dimensions from UIManager then caches as fields"
    - "resolveBackground/resolveForeground helpers encapsulate variant logic"
    - "isPrimary check via getClientProperty for variant switching"

key-files:
  created:
    - src/main/java/com/dwc/laf/ui/DwcButtonUI.java
    - src/test/java/com/dwc/laf/ui/DwcButtonUITest.java
  modified:
    - src/main/java/com/dwc/laf/DwcLookAndFeel.java

key-decisions:
  - "Per-component DwcButtonUI instances (not shared singleton) to allow future per-component state caching"
  - "Full paint() override without calling super.paint() for complete rendering control"
  - "LookAndFeel.installProperty for opaque=false (respects UIResource contract)"

patterns-established:
  - "ComponentUI delegate pattern: createUI -> installDefaults -> paint -> getPreferredSize"
  - "Variant detection via client property (dwc.buttonType=primary)"
  - "Disabled rendering: paintWithOpacity for background/icon, AlphaComposite for text"

# Metrics
duration: 2min
completed: 2026-02-10
---

# Phase 4 Plan 2: DwcButtonUI Delegate Summary

**DwcButtonUI delegate with 6-step paint pipeline, primary/default variant support, and 5-state rendering using StateColorResolver and PaintUtils**

## Performance

- **Duration:** 2 min
- **Started:** 2026-02-10T11:19:28Z
- **Completed:** 2026-02-10T11:21:38Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments
- DwcButtonUI extending BasicButtonUI with complete paint pipeline: background, border (via DwcButtonBorder), icon, text, focus ring
- Primary variant support via `button.putClientProperty("dwc.buttonType", "primary")` using accent-colored background/foreground
- Five visual states (normal, hover, pressed, focused, disabled) resolved by StateColorResolver
- Disabled state renders at reduced opacity using StateColorResolver.paintWithOpacity and AlphaComposite
- Minimum size enforcement: 72px width (text buttons only), 36px height
- ButtonUI registered in DwcLookAndFeel.initClassDefaults; Button.border registered as BorderUIResource
- 10 tests covering UI installation, per-component instances, property setup, preferred size, and paint smoke tests

## Task Commits

Each task was committed atomically:

1. **Task 1: DwcButtonUI delegate with complete paint pipeline** - `795865a` (feat)
2. **Task 2: L&F registration and DwcButtonUI tests** - `60f42a0` (feat)

## Files Created/Modified
- `src/main/java/com/dwc/laf/ui/DwcButtonUI.java` - Complete ButtonUI delegate with paint pipeline, variant support, state handling, size enforcement
- `src/test/java/com/dwc/laf/ui/DwcButtonUITest.java` - 10 tests for UI installation, per-component instances, rollover, opacity, border, preferred size, primary variant, and paint smoke tests
- `src/main/java/com/dwc/laf/DwcLookAndFeel.java` - Added ButtonUI class default and Button.border UIResource

## Decisions Made
- Per-component DwcButtonUI instances (not shared singleton) to allow future per-component state caching
- Full paint() override without calling super.paint() for complete rendering control
- LookAndFeel.installProperty for opaque=false (respects UIResource contract so app-set values are preserved)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- End-to-end CSS-to-delegate pipeline fully proven: CSS tokens parsed, mapped to UIDefaults, read by DwcButtonUI, painted onto JButton
- DwcButtonUI pattern established for all future component delegates (text fields, checkboxes, etc.)
- Phase 04 (Button Component) complete - both plans (border/UIDefaults + delegate) finished

## Self-Check: PASSED

- All 3 created/modified files exist on disk
- Both task commits (795865a, 60f42a0) found in git log
- DwcButtonUI.java: 307 lines (min 150 required)
- DwcButtonUITest.java: 136 lines (min 60 required)
- All 329 tests pass (0 failures, 0 skipped)

---
*Phase: 04-button-component*
*Completed: 2026-02-10*
