---
phase: 01-css-token-engine
plan: 01
subsystem: css-parsing
tags: [java21, sealed-interface, records, css-colors, maven, junit5]

# Dependency graph
requires: []
provides:
  - "Maven project with Java 21 and JUnit 5 build infrastructure"
  - "CssValue sealed interface with 6 typed record variants"
  - "NamedCssColors lookup for all 148 CSS named colors + transparent"
  - "Bundled default-light.css with 565+ DWC custom properties from SCSS source"
  - "test-tokens.css with 48 edge-case properties for parser testing"
affects: [01-02, 01-03, 01-04, 01-05]

# Tech tracking
tech-stack:
  added: [java-21, junit-jupiter-5.11.4, maven-compiler-3.13.0, maven-surefire-3.5.2]
  patterns: [sealed-interface-type-hierarchy, record-immutability, case-insensitive-map-lookup]

key-files:
  created:
    - pom.xml
    - src/main/java/com/dwc/laf/css/CssValue.java
    - src/main/java/com/dwc/laf/css/NamedCssColors.java
    - src/main/resources/com/dwc/laf/themes/default-light.css
    - src/test/java/com/dwc/laf/css/CssValueTest.java
    - src/test/java/com/dwc/laf/css/NamedCssColorsTest.java
    - src/test/resources/test-tokens.css
  modified: []

key-decisions:
  - "POM explicitly overrides external Maven profile that skips tests"
  - "NamedCssColors uses 149 entries (148 CSS named + transparent) with LinkedHashMap"
  - "CssValue.DimensionValue stores raw float + unit, intValue() uses Math.round"
  - "default-light.css compiled by hand from actual DWC SCSS source files"

patterns-established:
  - "Sealed interface + records for type-safe CSS value hierarchy"
  - "Optional-based lookup API for nullable results (NamedCssColors.resolve)"
  - "Locale.ROOT for case-insensitive string operations"

# Metrics
duration: 8min
completed: 2026-02-10
---

# Phase 1 Plan 1: Project Foundation Summary

**Maven project with CssValue sealed interface (6 record types), NamedCssColors (149 entries), and bundled DWC light theme CSS (565+ custom properties from SCSS source)**

## Performance

- **Duration:** 8 min
- **Started:** 2026-02-10T07:49:06Z
- **Completed:** 2026-02-10T07:57:12Z
- **Tasks:** 3
- **Files modified:** 7

## Accomplishments
- Maven project compiles with Java 21 and runs 27 JUnit 5 tests
- CssValue sealed interface provides typed wrappers for all CSS value kinds (ColorValue, DimensionValue, IntegerValue, FloatValue, StringValue, RawValue)
- NamedCssColors resolves all 148 CSS named colors plus transparent to java.awt.Color
- Bundled default-light.css contains 565+ DWC custom properties accurately compiled from the actual SCSS mixin source files (fonts, sizes, spaces, borders, surfaces, shadows, colors with 7 palette variations, transitions, easings, cursors, z-indexes, focus-ring, disabled, windows)
- test-tokens.css covers 48 edge cases for parser testing (hex, rgb, hsl, named colors, var() refs, nested fallbacks, circular refs, dimensions, calc, component selectors)

## Task Commits

Each task was committed atomically:

1. **Task 1: Create Maven project structure and POM** - `0db023e` (chore)
2. **Task 2: Create CssValue sealed interface and NamedCssColors** - `dc3669e` (feat)
3. **Task 3: Create bundled default light theme CSS** - `a0c491d` (feat)

## Files Created/Modified
- `pom.xml` - Maven build with Java 21, JUnit 5.11.4, zero runtime deps
- `src/main/java/com/dwc/laf/css/CssValue.java` - Sealed interface with 6 record types for typed CSS values
- `src/main/java/com/dwc/laf/css/NamedCssColors.java` - Static lookup of 149 CSS named colors (148 standard + transparent)
- `src/main/resources/com/dwc/laf/themes/default-light.css` - Bundled DWC light theme with 565+ custom properties
- `src/test/java/com/dwc/laf/css/CssValueTest.java` - 11 tests for CssValue types, pattern matching, equality
- `src/test/java/com/dwc/laf/css/NamedCssColorsTest.java` - 16 tests for color resolution, case-insensitivity, edge cases
- `src/test/resources/test-tokens.css` - 48 edge-case properties for downstream parser tests

## Decisions Made
- POM explicitly sets `<skip>false</skip>` on surefire and compiler test-compile execution to override external Maven profile that globally skips tests
- NamedCssColors stores 149 entries (148 CSS named + transparent) in a LinkedHashMap wrapped with Collections.unmodifiableMap
- CssValue.DimensionValue stores raw float + unit string; intValue() uses Math.round (conversion to px at rem base deferred to parser layer)
- default-light.css compiled by hand-expanding actual DWC SCSS mixins from the dwc/ source directory, including the colors-generator @for loop output for 7 palettes

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed Maven test skipping from external profile**
- **Found during:** Task 2 (running `mvn test`)
- **Issue:** User's `~/.m2/settings.xml` has an external profile named "skip tests" that sets `maven.test.skip=true`, causing all test compilation and execution to be skipped
- **Fix:** Added explicit `<skip>false</skip>` configuration to both maven-compiler-plugin (default-testCompile execution) and maven-surefire-plugin, plus POM-level properties `maven.test.skip=false` and `skipTests=false`
- **Files modified:** pom.xml
- **Verification:** `mvn clean test` runs all 27 tests successfully without any CLI flags
- **Committed in:** dc3669e (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Auto-fix was necessary for test execution. No scope creep.

## Issues Encountered
None beyond the Maven test-skip issue documented above.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Project compiles and tests pass, ready for Plan 02 (CSS token parser)
- CssValue type hierarchy is ready for downstream parsers to produce typed instances
- NamedCssColors lookup is ready for the color parser to use
- Bundled CSS and test CSS are ready for parser plans to consume
- All 7 color palettes with 19 lightness steps each provide realistic test data for var() resolution

## Self-Check: PASSED

All 7 created files verified on disk. All 3 task commits (0db023e, dc3669e, a0c491d) verified in git history.

---
*Phase: 01-css-token-engine*
*Completed: 2026-02-10*
