---
phase: 02-uidefaults-bridge-laf-setup
plan: 01
subsystem: ui
tags: [swing, uidefaults, css-tokens, properties, coloruiresource, mapping]

# Dependency graph
requires:
  - phase: 01-css-token-engine
    provides: "CssValue sealed interface, CssTokenMap, CssThemeLoader"
provides:
  - "TokenMappingConfig: properties-based CSS-to-UIDefaults mapping parser"
  - "UIDefaultsPopulator: typed CssValue-to-UIDefaults conversion engine"
  - "MappingType/MappingTarget/MappingEntry: mapping data model"
  - "Bundled token-mapping.properties covering 8 target components"
affects: [02-02-PLAN, 03-component-delegates]

# Tech tracking
tech-stack:
  added: []
  patterns: ["properties-file-driven mapping", "UIResource wrapping for L&F contract", "rem/em-to-pixel conversion"]

key-files:
  created:
    - src/main/java/com/dwc/laf/defaults/MappingType.java
    - src/main/java/com/dwc/laf/defaults/MappingTarget.java
    - src/main/java/com/dwc/laf/defaults/MappingEntry.java
    - src/main/java/com/dwc/laf/defaults/TokenMappingConfig.java
    - src/main/java/com/dwc/laf/defaults/UIDefaultsPopulator.java
    - src/main/resources/com/dwc/laf/token-mapping.properties
    - src/test/java/com/dwc/laf/defaults/TokenMappingConfigTest.java
    - src/test/java/com/dwc/laf/defaults/UIDefaultsPopulatorTest.java
  modified: []

key-decisions:
  - "Properties file format for mapping config (external, overridable, zero-code-change)"
  - "ColorUIResource wrapping for UIResource contract compliance (theme switching)"
  - "16px base font size for rem/em to pixel conversion"
  - "INSETS type deferred to later phase (needs multi-value parsing)"
  - "RawValue skipped in AUTO mode (calc expressions not convertible)"

patterns-established:
  - "UIResource wrapping: all Color values in UIDefaults must be ColorUIResource"
  - "External override via system property: dwc.mapping for token-mapping.properties"
  - "One-to-many mapping: single CSS token populates multiple UIDefaults keys"

# Metrics
duration: 5min
completed: 2026-02-10
---

# Phase 2 Plan 1: Token Mapping & UIDefaults Populator Summary

**Properties-driven CSS-to-UIDefaults bridge with typed conversion (ColorUIResource, Integer, Float, String) and rem/em/px dimension support**

## Performance

- **Duration:** 5 min
- **Started:** 2026-02-10T09:21:05Z
- **Completed:** 2026-02-10T09:26:05Z
- **Tasks:** 2
- **Files created:** 8

## Accomplishments
- TokenMappingConfig parses properties files that map CSS token names to typed UIDefaults keys with one-to-many support
- UIDefaultsPopulator converts CssValue types to UIDefaults-compatible Java types with ColorUIResource wrapping
- Bundled token-mapping.properties covers global colors, surfaces, border radius, typography, disabled state, button/input tokens, and component states
- External override support via dwc.mapping system property for customization without code changes
- 38 tests total (18 config + 20 populator) covering parsing, type conversion, integration, and edge cases

## Task Commits

Each task was committed atomically:

1. **Task 1: Token mapping types, config parser, and properties file** - `2bd9312` (feat)
2. **Task 2: UIDefaultsPopulator with typed value conversion and UIResource wrapping** - `a8abed1` (feat)

## Files Created/Modified
- `src/main/java/com/dwc/laf/defaults/MappingType.java` - Enum: COLOR, INT, FLOAT, STRING, INSETS, AUTO with fromString factory
- `src/main/java/com/dwc/laf/defaults/MappingTarget.java` - Record: UIDefaults key name + MappingType
- `src/main/java/com/dwc/laf/defaults/MappingEntry.java` - Record: CSS token name + unmodifiable list of MappingTargets
- `src/main/java/com/dwc/laf/defaults/TokenMappingConfig.java` - Properties parser with classpath + external override, immutable config
- `src/main/java/com/dwc/laf/defaults/UIDefaultsPopulator.java` - Typed conversion engine: CssValue -> UIDefaults with UIResource wrapping
- `src/main/resources/com/dwc/laf/token-mapping.properties` - Default mapping for 8 target components (~25 CSS tokens)
- `src/test/java/com/dwc/laf/defaults/TokenMappingConfigTest.java` - 18 tests: parsing, types, classpath, external override
- `src/test/java/com/dwc/laf/defaults/UIDefaultsPopulatorTest.java` - 20 tests: color, int, float, string, auto, missing, integration

## Decisions Made
- **Properties file format:** Chosen for external overridability without code changes. Uses standard java.util.Properties for robust parsing.
- **ColorUIResource wrapping:** All color values wrapped per Swing UIResource contract. Without this, theme switching breaks (Swing checks instanceof UIResource to distinguish L&F vs app-set values).
- **16px base font size:** Standard browser default for rem/em to pixel conversion. Matches DWC web rendering.
- **INSETS deferred:** Insets type returns null with log message. Multi-value parsing (top/right/bottom/left) deferred to component delegate phase when actual component needs arise.
- **RawValue handling:** Skipped in AUTO mode (calc expressions, complex shorthands). STRING mode converts RawValue.raw() to String for cases where raw text is useful.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed string value test expectations for CSS value typing**
- **Found during:** Task 2 (UIDefaultsPopulatorTest)
- **Issue:** Tests used quoted CSS strings (`"Helvetica"`) expecting StringValue, but the CSS typer classifies single quoted words as RawValue. Only comma-separated font stacks and CSS keywords produce StringValue.
- **Fix:** Changed test CSS values to comma-separated font stacks (e.g., `Helvetica, Arial, sans-serif`) which the CssValueTyper correctly classifies as StringValue.
- **Files modified:** src/test/java/com/dwc/laf/defaults/UIDefaultsPopulatorTest.java
- **Verification:** All 20 populator tests pass
- **Committed in:** a8abed1 (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** Test fix aligned expectations with actual CSS typer behavior. No scope creep.

## Issues Encountered
None beyond the test expectation fix documented above.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Token mapping and UIDefaults population layer complete
- Ready for Plan 02: DwcLookAndFeel class setup, installTheme, and getDefaults integration
- UIDefaultsPopulator.populate() is the method that Plan 02's DwcLookAndFeel.getDefaults() will call
- Full test suite: 255 tests passing (Phase 1 + Phase 2 Plan 01)

## Self-Check: PASSED

All 8 created files verified on disk. Both commit hashes (2bd9312, a8abed1) confirmed in git log. Full test suite: 255 tests passing.

---
*Phase: 02-uidefaults-bridge-laf-setup*
*Completed: 2026-02-10*
