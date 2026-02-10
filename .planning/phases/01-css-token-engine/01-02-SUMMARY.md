---
phase: 01-css-token-engine
plan: 02
subsystem: css-parsing
tags: [java21, hand-written-parser, css-custom-properties, character-parser, tdd]

# Dependency graph
requires:
  - phase: 01-01
    provides: "Maven project, CssValue types, bundled CSS files, test-tokens.css"
provides:
  - "CssTokenParser.parse() extracts custom properties from CSS text into Map<String, String>"
  - "Comment stripping, brace-matching, parenthesis-aware value extraction"
  - "Component-level properties override :root via document-order flattening"
  - "Handles 540+ properties from default-light.css, 47 from test-tokens.css"
affects: [01-03, 01-04, 01-05]

# Tech tracking
tech-stack:
  added: []
  patterns: [hand-written-character-parser, comment-stripping, brace-depth-tracking, parenthesis-aware-value-extraction, string-literal-awareness]

key-files:
  created:
    - src/main/java/com/dwc/laf/css/CssTokenParser.java
    - src/test/java/com/dwc/laf/css/CssTokenParserTest.java
  modified: []

key-decisions:
  - "Hand-written character-by-character parser (not regex) for robustness with nested parens and multi-line values"
  - "Document-order flattening: later declarations override earlier ones for same property name (component naturally overrides :root)"
  - "Quote-aware value scanning: single and double quotes tracked to avoid false semicolon termination"
  - "Whitespace normalization: multi-line values collapsed to single-line with internal spaces preserved"

patterns-established:
  - "Two-step parse: strip comments first, then walk blocks and extract declarations"
  - "Parenthesis depth tracking for value boundaries (var(), rgb(), calc(), hsla())"
  - "LinkedHashMap with later-wins semantics for CSS cascade flattening"
  - "Package-private static helpers for testability (stripComments, normalizeWhitespace)"

# Metrics
duration: 3min
completed: 2026-02-10
---

# Phase 1 Plan 2: CSS Token Parser Summary

**Hand-written character-level CSS parser extracting 540+ custom properties from :root and component selectors into a flattened Map<String, String> with document-order override semantics**

## Performance

- **Duration:** 3 min
- **Started:** 2026-02-10T07:59:51Z
- **Completed:** 2026-02-10T08:03:10Z
- **Tasks:** 3 (TDD: RED, GREEN, REFACTOR)
- **Files modified:** 2

## Accomplishments
- CssTokenParser.parse() extracts all --custom-property declarations from CSS text using a hand-written character-by-character parser
- Parser handles comment stripping, brace-depth matching, parenthesis-aware value extraction, and string literal awareness
- Component-level selectors (.dwc-button, .dwc-input, etc.) naturally override :root properties via document-order flattening
- Parses bundled default-light.css (540 entries) and test-tokens.css (47 entries) correctly
- Multi-line shadow values, font-family stacks with commas, nested var() fallbacks, calc() expressions, and quoted strings all preserved accurately
- 40 tests across 8 nested test classes covering :root extraction, component selectors, flattening, value preservation, comments, edge cases, realistic files, and whitespace formatting
- Total test suite: 67 tests all passing

## Task Commits

Each task was committed atomically:

1. **Task 1 (RED): Write failing tests** - `2ec15d2` (test)
2. **Task 2 (GREEN): Implement CssTokenParser** - `5392e92` (feat)
3. **Task 3 (REFACTOR): Review** - No changes needed, code already clean

## Files Created/Modified
- `src/main/java/com/dwc/laf/css/CssTokenParser.java` - Pass 1 parser: strips comments, finds selector blocks, extracts --custom-property declarations into Map<String, String>
- `src/test/java/com/dwc/laf/css/CssTokenParserTest.java` - 40 tests across 8 nested classes covering all extraction scenarios

## Decisions Made
- Used hand-written character-by-character parser rather than regex for robustness with nested parentheses, multi-line values, and string literals
- Document-order flattening (later declarations override earlier for same property name) naturally handles component-over-root precedence without explicit priority tracking
- Quote-aware value scanning prevents false semicolon termination inside string literals like `'\2022'`
- Multi-line whitespace normalization collapses newlines/runs into single spaces while preserving intentional internal spacing
- Test assertion for `--dwc-shadow-1` corrected to `--dwc-shadow` to match actual CSS property names in default-light.css

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed test assertion for shadow property name**
- **Found during:** Task 2 (GREEN -- running tests against real CSS)
- **Issue:** Test asserted `--dwc-shadow-1` but actual bundled CSS uses `--dwc-shadow` (named by size: xs/s/m/l/xl, not numbered)
- **Fix:** Changed assertion to check for `--dwc-shadow` instead of `--dwc-shadow-1`
- **Files modified:** CssTokenParserTest.java
- **Verification:** Test passes against actual default-light.css
- **Committed in:** 5392e92 (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 bug in test)
**Impact on plan:** Trivial test assertion fix. No scope change.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- CssTokenParser.parse() is ready for Pass 2 (CssVariableResolver) to consume raw token map and resolve var() references
- All 540+ default-light.css tokens and 47 test-tokens.css tokens extracted correctly
- The returned Map<String, String> is immutable and preserves insertion order via LinkedHashMap
- Component-level overrides work correctly for downstream merge/overlay patterns

## Self-Check: PASSED

All 2 created files verified on disk. Both task commits (2ec15d2, 5392e92) verified in git history.

---
*Phase: 01-css-token-engine*
*Completed: 2026-02-10*
