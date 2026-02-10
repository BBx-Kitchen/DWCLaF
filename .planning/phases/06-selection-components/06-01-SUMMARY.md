---
phase: 06-selection-components
plan: 01
subsystem: ui
tags: [swing, checkbox, radiobutton, combobox, icon, laf, css-tokens]

# Dependency graph
requires:
  - phase: 03-shared-painting-utilities
    provides: PaintUtils, FocusRingPainter, StateColorResolver
  - phase: 04-button-component
    provides: DwcButtonUI per-component pattern, DwcButtonBorder
  - phase: 05-text-input-components
    provides: DwcTextFieldBorder (reused for ComboBox)
provides:
  - DwcCheckBoxIcon custom icon painting checkmark with Path2D
  - DwcRadioButtonIcon custom icon painting circular indicator with Ellipse2D center dot
  - DwcCheckBoxUI delegate extending BasicRadioButtonUI
  - DwcRadioButtonUI delegate extending BasicRadioButtonUI
  - DwcComboBoxUI placeholder delegate extending BasicComboBoxUI
  - Token mappings for CheckBox, RadioButton, and ComboBox UIDefaults
  - Pre-computed contrast text colors in default-light.css
affects: [06-02-combobox-delegate]

# Tech tracking
tech-stack:
  added: []
  patterns: [icon-based-indicator-painting, BasicRadioButtonUI-extension-for-both-checkbox-and-radio]

key-files:
  created:
    - src/main/java/com/dwc/laf/ui/DwcCheckBoxIcon.java
    - src/main/java/com/dwc/laf/ui/DwcRadioButtonIcon.java
    - src/main/java/com/dwc/laf/ui/DwcCheckBoxUI.java
    - src/main/java/com/dwc/laf/ui/DwcRadioButtonUI.java
    - src/main/java/com/dwc/laf/ui/DwcComboBoxUI.java
    - src/test/java/com/dwc/laf/ui/DwcCheckBoxUITest.java
    - src/test/java/com/dwc/laf/ui/DwcRadioButtonUITest.java
  modified:
    - src/main/resources/com/dwc/laf/token-mapping.properties
    - src/main/java/com/dwc/laf/DwcLookAndFeel.java
    - src/test/java/com/dwc/laf/DwcLookAndFeelTest.java
    - src/main/resources/com/dwc/laf/themes/default-light.css

key-decisions:
  - "CheckBox and RadioButton icons use 16px content area (matching desktop conventions) with focusWidth*2 reserved for focus ring"
  - "Both DwcCheckBoxUI and DwcRadioButtonUI extend BasicRadioButtonUI (not BasicCheckBoxUI) with only getPropertyPrefix() overridden"
  - "Pre-computed primary-text contrast colors in CSS to bypass calc() limitation in token pipeline"
  - "Checkmark color fixed: --dwc-color-on-primary-text (white) not --dwc-color-primary (blue)"

patterns-established:
  - "Icon-based indicator painting: custom Icon registered via UIDefaults key, UI delegate inherits layout from BasicRadioButtonUI"
  - "Icon dimensions include focus ring space: ICON_SIZE + focusWidth * 2"

# Metrics
duration: 7min
completed: 2026-02-10
---

# Phase 6 Plan 01: CheckBox/RadioButton Icons and UI Delegates Summary

**Custom DwcCheckBoxIcon (Path2D checkmark) and DwcRadioButtonIcon (Ellipse2D center dot) with 4-state color resolution, token mappings for all Phase 6 components, and L&F registration of CheckBoxUI/RadioButtonUI/ComboBoxUI**

## Performance

- **Duration:** 7 min
- **Started:** 2026-02-10T12:24:35Z
- **Completed:** 2026-02-10T12:32:08Z
- **Tasks:** 2
- **Files modified:** 12

## Accomplishments
- DwcCheckBoxIcon paints rounded-rect box with Path2D checkmark on selected state (white checkmark on blue background)
- DwcRadioButtonIcon paints circle with Ellipse2D center dot on selected state
- Both icons handle 4 visual states (normal, hover, selected, disabled) with focus ring painting
- Token mappings populated for CheckBox, RadioButton, and ComboBox UIDefaults (ComboBox ready for Plan 02)
- Corrected checkmark color mapping from --dwc-color-primary (wrong: blue on blue) to --dwc-color-on-primary-text (correct: white on blue)

## Task Commits

Each task was committed atomically:

1. **Task 1: Token mappings and DwcLookAndFeel setup** - `7d07712` (feat)
2. **Task 2: CheckBox/RadioButton icons, UI delegates, and tests** - `3dbbb3d` (test)

## Files Created/Modified
- `src/main/java/com/dwc/laf/ui/DwcCheckBoxIcon.java` - Custom Icon painting checkbox indicator with checkmark path (141 lines)
- `src/main/java/com/dwc/laf/ui/DwcRadioButtonIcon.java` - Custom Icon painting circular radio indicator with center dot (134 lines)
- `src/main/java/com/dwc/laf/ui/DwcCheckBoxUI.java` - CheckBoxUI delegate extending BasicRadioButtonUI (45 lines)
- `src/main/java/com/dwc/laf/ui/DwcRadioButtonUI.java` - RadioButtonUI delegate extending BasicRadioButtonUI (43 lines)
- `src/main/java/com/dwc/laf/ui/DwcComboBoxUI.java` - Placeholder ComboBoxUI for Plan 02 (25 lines)
- `src/test/java/com/dwc/laf/ui/DwcCheckBoxUITest.java` - 9 tests for checkbox UI/icon
- `src/test/java/com/dwc/laf/ui/DwcRadioButtonUITest.java` - 9 tests for radio button UI/icon
- `src/main/resources/com/dwc/laf/token-mapping.properties` - Added checkbox/radio/combobox color mappings
- `src/main/java/com/dwc/laf/DwcLookAndFeel.java` - Added 3 class defaults + 3 init methods
- `src/test/java/com/dwc/laf/DwcLookAndFeelTest.java` - Added 9 tests for new mappings/registrations
- `src/main/resources/com/dwc/laf/themes/default-light.css` - Pre-computed primary-text contrast colors

## Decisions Made
- **16px icon size:** Content area is 16px (not 18px from DWC web or 15px from FlatLaf). Total icon dimension with focus ring = 16 + focusWidth*2 = 22px.
- **Extend BasicRadioButtonUI for both:** BasicCheckBoxUI is literally BasicRadioButtonUI with only getPropertyPrefix() changed. Extending BasicRadioButtonUI directly is cleaner (same as FlatLaf).
- **Pre-computed contrast colors:** The CSS `calc()` expressions in `--dwc-color-primary-text-*` entries were RawValues that the token pipeline skipped. Pre-computed them in CSS for `--dwc-color-primary-c: 50` (N < 50 = white, N >= 50 = black).
- **Checkmark color fix:** Moved CheckBox.icon.checkmarkColor from --dwc-color-primary (blue -- invisible on blue background) to --dwc-color-on-primary-text (white -- visible contrast).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Pre-computed primary-text contrast colors in CSS**
- **Found during:** Task 1 (token mapping verification)
- **Issue:** `--dwc-color-on-primary-text` resolved through `calc()` expressions that the CSS parser stored as RawValue, causing `CheckBox.icon.checkmarkColor` and `RadioButton.icon.dotColor` to be null in UIDefaults
- **Fix:** Replaced calc-based `--dwc-color-primary-text-*` entries in default-light.css with pre-computed `hsl(0, 0%, 100%)` (white) / `hsl(0, 0%, 0%)` (black) values based on `--dwc-color-primary-c: 50`
- **Files modified:** `src/main/resources/com/dwc/laf/themes/default-light.css`
- **Verification:** Tests pass confirming checkmark color is white-ish (RGB > 200)
- **Committed in:** `7d07712` (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** Essential fix -- without it, checkbox checkmark and radio dot would be invisible (null color). No scope creep.

## Issues Encountered
- BasicRadioButtonUI stores the default icon in its own `icon` field, not on the AbstractButton. Tests needed to use `getDefaultIcon()` instead of `cb.getIcon()` to verify icon installation.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- ComboBoxUI class default registered and DwcComboBoxUI placeholder created (ready for Plan 02)
- All ComboBox token mappings in place (background, borderColor, hoverBorderColor, foreground, selectionBackground, selectionForeground, popupBackground, buttonArrowColor)
- ComboBox border already set to DwcTextFieldBorder (reuse pattern)

## Self-Check: PASSED

- All 7 created files verified on disk
- Both task commits (7d07712, 3dbbb3d) found in git log
- All 379 tests pass (mvn test exit code 0)

---
*Phase: 06-selection-components*
*Completed: 2026-02-10*
