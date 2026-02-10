---
phase: 01-css-token-engine
plan: 05
subsystem: css-parsing
tags: [java21, value-typing, token-map, theme-loading, pipeline-integration, system-property]

# Dependency graph
requires:
  - phase: 01-01
    provides: "CssValue sealed interface, NamedCssColors, bundled default-light.css"
  - phase: 01-02
    provides: "CssTokenParser.parse() for extracting raw tokens from CSS"
  - phase: 01-03
    provides: "CssVariableResolver.resolve() for expanding var() references"
  - phase: 01-04
    provides: "CssColorParser and CssDimensionParser for value parsing"
provides:
  - "CssValueTyper.type() converts resolved strings to typed CssValue records (Pass 3)"
  - "CssTokenMap immutable typed token map with getColor/getInt/getFloat/getString API"
  - "CssThemeLoader entry point: load from classpath, file, string, or with system property override"
  - "Complete CSS token pipeline: parse -> resolve -> type -> CssTokenMap"
  - "External override via -Ddwc.theme system property with merge overlay model"
affects: [02-uidefaults-bridge]

# Tech tracking
tech-stack:
  added: []
  patterns: [top-level-comma-detection, classify-order-functions-strings-colors-dimensions, merge-overlay-model, system-property-configuration]

key-files:
  created:
    - src/main/java/com/dwc/laf/css/CssValueTyper.java
    - src/main/java/com/dwc/laf/css/CssTokenMap.java
    - src/main/java/com/dwc/laf/css/CssThemeLoader.java
    - src/test/java/com/dwc/laf/css/CssValueTyperTest.java
    - src/test/java/com/dwc/laf/css/CssTokenMapTest.java
    - src/test/java/com/dwc/laf/css/CssThemeLoaderTest.java
  modified: []

key-decisions:
  - "Classification order: functions -> string patterns -> color -> dimension -> raw (prevents multi-value CSS shorthands from being mistyped as dimensions)"
  - "Top-level comma detection with parenthesis depth tracking distinguishes font stacks from hsl()/rgb() function commas"
  - "CssTokenMap is package-private constructor: only CssThemeLoader creates instances"
  - "Missing override file is non-fatal: warning logged, bundled defaults preserved"

patterns-established:
  - "Type classification pipeline: check functions first, then string patterns, then attempt typed parsing, fallback to raw"
  - "CssTokenMap dual-map pattern: typed CssValue map for API + raw string map for debugging"
  - "System property pattern: -Ddwc.theme for external CSS override path"
  - "Merge overlay: LinkedHashMap copy of base, then putAll(override) for key-based replacement"

# Metrics
duration: 5min
completed: 2026-02-10
---

# Phase 1 Plan 5: CSS Engine Facade Summary

**Complete CSS token pipeline wiring CssThemeLoader entry point through parse-resolve-type pipeline to produce immutable CssTokenMap with typed accessors, supporting bundled defaults and external file override via -Ddwc.theme system property**

## Performance

- **Duration:** 5 min
- **Started:** 2026-02-10T08:18:30Z
- **Completed:** 2026-02-10T08:23:52Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments
- CssValueTyper (Pass 3) classifies resolved string values into ColorValue, DimensionValue, IntegerValue, FloatValue, StringValue, or RawValue with correct ordering to avoid misclassification of multi-value CSS shorthands
- CssTokenMap provides clean typed API (getColor, getInt, getFloat, getString, get) with Optional-based returns, default-value overloads, and bulk access (propertyNames, size, contains)
- CssThemeLoader orchestrates the complete pipeline: load CSS -> parse raw tokens -> merge optional override -> resolve var() -> type values -> wrap in CssTokenMap
- External override via -Ddwc.theme system property correctly merges file-based tokens over bundled defaults
- End-to-end verified with real DWC tokens: --dwc-color-primary-50 resolves HSL(211, 100%, 50%) through var() chain to correct blue Color, --dwc-font-weight-normal types as IntegerValue(400), --dwc-font-family-sans types as StringValue
- All 8 CSS requirements (CSS-01 through CSS-08) satisfied and tested
- Total test suite: 217 tests all passing (59 new in this plan)

## Task Commits

Each task was committed atomically:

1. **Task 1: CssValueTyper and CssTokenMap** - `1f7951f` (feat)
2. **Task 2: CssThemeLoader and integration tests** - `d0fa93e` (feat)

## Files Created/Modified
- `src/main/java/com/dwc/laf/css/CssValueTyper.java` - Pass 3: converts resolved string map to typed CssValue map using ordered classification (functions -> strings -> colors -> dimensions -> raw)
- `src/main/java/com/dwc/laf/css/CssTokenMap.java` - Immutable public API: typed accessors for colors, ints, floats, strings with Optional returns and default-value overloads
- `src/main/java/com/dwc/laf/css/CssThemeLoader.java` - Pipeline entry point: load(), loadFromClasspath(), loadFromFile(), loadFromString() with system property override support
- `src/test/java/com/dwc/laf/css/CssValueTyperTest.java` - 18 tests: color/dimension/string/raw classification, edge cases, immutability
- `src/test/java/com/dwc/laf/css/CssTokenMapTest.java` - 23 tests: all typed accessors, defaults, bulk access, immutability
- `src/test/java/com/dwc/laf/css/CssThemeLoaderTest.java` - 18 tests: classpath, string, full pipeline, override merge, system property, file loading, missing resources, end-to-end with real DWC tokens

## Decisions Made
- Classification order set to: functions -> string patterns -> color -> dimension -> raw. This prevents multi-value CSS shorthands (shadows, borders) that start with digits from being misclassified as DimensionValue by the dimension parser
- Top-level comma detection uses parenthesis depth tracking to distinguish font-family stacks ("-apple-system, sans-serif") from function-internal commas ("hsl(211, 100%, 50%)")
- Removed "transparent" from CSS_KEYWORDS set because CssColorParser correctly handles it as a named color (rgba(0,0,0,0)), avoiding double-classification conflict
- CssTokenMap constructor is package-private, enforcing that only CssThemeLoader creates instances (controlled pipeline)
- Missing override file treated as non-fatal: warning logged, bundled defaults used alone, enabling graceful degradation

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed classification order to prevent multi-value shorthand mistyping**
- **Found during:** Task 1 (running CssValueTyperTest)
- **Issue:** Shadow value "0 2px 4px rgba(0,0,0,0.1), 0 4px 8px rgba(0,0,0,0.05)" was being classified as DimensionValue because it starts with "0" and CssDimensionParser runs before string detection. Similarly, plan's original classification order (color -> dimension -> string -> raw) would have hsl() comma-separated values caught by the string comma check.
- **Fix:** Reordered classification to: functions -> string patterns (with top-level comma detection) -> color -> dimension -> raw. Added hasTopLevelComma() method that tracks parenthesis depth so commas inside hsl()/rgb() are not treated as string separators.
- **Files modified:** CssValueTyper.java
- **Verification:** All 18 CssValueTyperTest tests pass including both shadow (StringValue) and hsl color (ColorValue)
- **Committed in:** 1f7951f (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (1 bug in classification logic)
**Impact on plan:** Fix was necessary for correct type classification. No scope creep.

## Issues Encountered
None beyond the auto-fixed classification order issue documented above.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Phase 1 CSS Token Engine is complete: all 5 plans executed, all 8 CSS requirements satisfied
- CssThemeLoader.load() is the single entry point Phase 2 (UIDefaults Bridge) will call
- CssTokenMap.getColor/getInt/getFloat/getString are the typed accessors component delegates will use
- External override via -Ddwc.theme ready for user customization
- All 217 tests pass across the full suite
- Zero runtime dependencies (only JUnit in test scope)

## Self-Check: PASSED

All 6 created files verified on disk. Both task commits (1f7951f, d0fa93e) verified in git history.

---
*Phase: 01-css-token-engine*
*Completed: 2026-02-10*
