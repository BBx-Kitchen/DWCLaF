---
phase: 07-display-container-components
plan: 01
subsystem: ui
tags: [swing, label, panel, card, shadow, opacity, disabled-state, rounded-corners]

# Dependency graph
requires:
  - phase: 03-shared-painting-utilities
    provides: PaintUtils.paintRoundedBackground, ShadowPainter.paintShadow
  - phase: 02-uidefaults-bridge-laf-setup
    provides: DwcLookAndFeel class defaults registration, token mapping pipeline
provides:
  - DwcLabelUI delegate with disabled opacity rendering (no chiseled text)
  - DwcPanelUI delegate with card-style shadow + rounded corner painting
  - Panel.arc, Panel.shadowColor, Panel.shadowBlurRadius, Panel.shadowOffsetY UIDefaults
  - LabelUI and PanelUI class defaults in DwcLookAndFeel
affects: [07-02-PLAN, 08-integration-testing]

# Tech tracking
tech-stack:
  added: []
  patterns: [card-mode-client-property, disabled-opacity-override]

key-files:
  created:
    - src/main/java/com/dwc/laf/ui/DwcLabelUI.java
    - src/main/java/com/dwc/laf/ui/DwcPanelUI.java
    - src/test/java/com/dwc/laf/ui/DwcLabelUITest.java
    - src/test/java/com/dwc/laf/ui/DwcPanelUITest.java
  modified:
    - src/main/java/com/dwc/laf/DwcLookAndFeel.java
    - src/main/resources/com/dwc/laf/token-mapping.properties
    - src/test/java/com/dwc/laf/DwcLookAndFeelTest.java

key-decisions:
  - "Removed getInsets override from DwcPanelUI: BasicPanelUI/ComponentUI has no getInsets method; shadow margin handled via focusWidth offset in painting"
  - "Panel.shadowColor hard-coded as Color(0,0,0,40) in initPanelDefaults, not token-mapped (semi-transparent shadow not directly derivable from CSS tokens)"
  - "Panel.arc added to --dwc-border-radius token mapping line for automatic inheritance"

patterns-established:
  - "Card-mode activation via client property: panel.putClientProperty('dwc.panelStyle', 'card')"
  - "Disabled opacity override: replace chiseled text with AlphaComposite reduction in paintDisabledText"

# Metrics
duration: 3min
completed: 2026-02-10
---

# Phase 7 Plan 1: Label & Panel Summary

**DwcLabelUI with disabled-opacity rendering and DwcPanelUI with card-mode shadow/rounded-corner painting via dwc.panelStyle client property**

## Performance

- **Duration:** 3 min
- **Started:** 2026-02-10T17:37:08Z
- **Completed:** 2026-02-10T17:40:14Z
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments
- DwcLabelUI renders disabled text with opacity reduction instead of BasicLabelUI's chiseled light/dark text effect
- DwcPanelUI supports card-style rendering with rounded corners and elevation shadow via `dwc.panelStyle=card` client property
- Both delegates registered in DwcLookAndFeel with Panel-specific UIDefaults (arc, shadowColor, shadowBlurRadius, shadowOffsetY)
- 17 new tests (7 Label + 8 Panel + 2 L&F registration) all passing, total test count: 406

## Task Commits

Each task was committed atomically:

1. **Task 1: DwcLabelUI + DwcPanelUI delegates, token mappings, and L&F registration** - `a8b8327` (feat)
2. **Task 2: Tests for DwcLabelUI, DwcPanelUI, and L&F registration** - `612e9fe` (test)

## Files Created/Modified
- `src/main/java/com/dwc/laf/ui/DwcLabelUI.java` - LabelUI delegate with disabled opacity painting via AlphaComposite
- `src/main/java/com/dwc/laf/ui/DwcPanelUI.java` - PanelUI delegate with card-mode shadow+rounded background painting
- `src/main/java/com/dwc/laf/DwcLookAndFeel.java` - Added LabelUI/PanelUI class defaults, initLabelDefaults, initPanelDefaults
- `src/main/resources/com/dwc/laf/token-mapping.properties` - Added int:Panel.arc to --dwc-border-radius mapping
- `src/test/java/com/dwc/laf/ui/DwcLabelUITest.java` - 7 tests for label UI behavior
- `src/test/java/com/dwc/laf/ui/DwcPanelUITest.java` - 8 tests for panel UI behavior including card mode
- `src/test/java/com/dwc/laf/DwcLookAndFeelTest.java` - 2 new tests (LabelUI + PanelUI registration)

## Decisions Made
- **Removed getInsets override from DwcPanelUI:** BasicPanelUI/ComponentUI has no `getInsets(JComponent)` method to override. Shadow margin handled via focusWidth offset in the painting coordinate system.
- **Panel.shadowColor hard-coded:** Set to `new Color(0, 0, 0, 40)` in initPanelDefaults rather than token-mapped, since semi-transparent shadow color is not directly derivable from CSS tokens.
- **Panel.arc token-mapped:** Added `int:Panel.arc` to the existing `--dwc-border-radius` mapping line for automatic inheritance from CSS.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Removed getInsets override from DwcPanelUI**
- **Found during:** Task 1 (DwcPanelUI implementation)
- **Issue:** Plan specified overriding `getInsets(JComponent)` on DwcPanelUI, but `BasicPanelUI` -> `PanelUI` -> `ComponentUI` does not have this method
- **Fix:** Removed the getInsets override entirely; shadow margin is already handled by the focusWidth coordinate offset in update()
- **Files modified:** src/main/java/com/dwc/laf/ui/DwcPanelUI.java
- **Verification:** Compilation succeeds, all tests pass
- **Committed in:** a8b8327 (Task 1 commit)

**2. [Rule 1 - Bug] Fixed UIManager reference in initPanelDefaults**
- **Found during:** Task 1 (DwcLookAndFeel modifications)
- **Issue:** Used `UIManager.getInt("Component.arc")` in initPanelDefaults but UIManager not imported and values are in the `table` parameter
- **Fix:** Changed to `table.getInt("Component.arc")`
- **Files modified:** src/main/java/com/dwc/laf/DwcLookAndFeel.java
- **Verification:** Compilation succeeds, all tests pass
- **Committed in:** a8b8327 (Task 1 commit)

---

**Total deviations:** 2 auto-fixed (1 blocking, 1 bug)
**Impact on plan:** Both auto-fixes necessary for compilation. No scope creep.

## Issues Encountered
None beyond the auto-fixed deviations above.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Label and Panel delegates complete, ready for Phase 07 Plan 02 (remaining display/container components)
- Card-mode panel pattern established for potential reuse in other container components

---
*Phase: 07-display-container-components*
*Completed: 2026-02-10*
