---
phase: 01-css-token-engine
plan: 03
subsystem: css-parsing
tags: [java21, var-resolution, dfs-cycle-detection, recursive-fallbacks, tdd]

# Dependency graph
requires:
  - phase: 01-01
    provides: "Maven project, CssValue types, bundled CSS files"
  - phase: 01-02
    provides: "CssTokenParser.parse() producing raw Map<String, String> with var() references"
provides:
  - "CssVariableResolver.resolve() expands all var() references in raw token map"
  - "DFS graph coloring cycle detection (no stack overflow on circular references)"
  - "Nested fallback resolution to arbitrary depth"
  - "Embedded var() expansion within larger CSS value strings"
  - "Immutable resolved map output"
affects: [01-04, 01-05]

# Tech tracking
tech-stack:
  added: []
  patterns: [dfs-graph-coloring-cycle-detection, sentinel-value-pattern, memoized-recursive-resolution, cycle-participant-post-processing]

key-files:
  created:
    - src/main/java/com/dwc/laf/css/CssVariableResolver.java
    - src/test/java/com/dwc/laf/css/CssVariableResolverTest.java
  modified: []

key-decisions:
  - "DFS graph coloring (UNVISITED/VISITING/VISITED) for cycle detection instead of explicit graph construction"
  - "Cycle participants without fallbacks excluded via post-processing pass even when cycle broken by another variable's fallback"
  - "Sentinel UNRESOLVABLE string to distinguish 'resolved to empty' from 'cannot resolve'"
  - "Fallback values trimmed (leading space after comma removed) matching CSS spec behavior"

patterns-established:
  - "Memoized recursive resolution with sentinel values for unresolvable entries"
  - "Post-processing pass to handle order-independent cycle participant exclusion"
  - "Parenthesis-depth tracking reused across splitVarArgs, findMatchingParen, resolveValue"

# Metrics
duration: 5min
completed: 2026-02-10
---

# Phase 1 Plan 3: CSS Variable Resolver Summary

**Recursive var() resolver with DFS cycle detection, nested fallback support to arbitrary depth, and embedded var() expansion in composite CSS values like hsl() and shadow strings**

## Performance

- **Duration:** 5 min
- **Started:** 2026-02-10T08:05:15Z
- **Completed:** 2026-02-10T08:10:21Z
- **Tasks:** 3 (TDD: RED, GREEN, REFACTOR)
- **Files modified:** 2

## Accomplishments
- CssVariableResolver.resolve() takes raw token map from CssTokenParser and produces fully-resolved map with all var() references expanded
- DFS graph coloring detects circular references (self, mutual, three-way) without stack overflow, excluding unresolvable entries with JUL warnings
- Nested fallbacks work to arbitrary depth: var(--a, var(--b, var(--c, literal))) resolves correctly through all levels
- Embedded var() in composite values fully expanded: "hsl(var(--h), var(--s), 50%)" becomes "hsl(211, 100%, 50%)"
- Multiple var() per value supported: "var(--a) var(--b)" resolves to "10 20"
- Cycle participants without fallbacks excluded regardless of iteration order via post-processing pass
- Malformed var() (unclosed parens) kept as raw text with warning
- Result map is immutable via Collections.unmodifiableMap()
- 30 tests across 9 nested test classes; total suite now 97 tests all passing

## Task Commits

Each task was committed atomically:

1. **Task 1 (RED): Write failing tests** - `7653e46` (test)
2. **Task 2 (GREEN): Implement CssVariableResolver** - `f037903` (feat)
3. **Task 3 (REFACTOR): Review** - No changes needed, code already clean

## Files Created/Modified
- `src/main/java/com/dwc/laf/css/CssVariableResolver.java` - Pass 2 resolver: DFS cycle detection, recursive var() expansion, nested fallbacks, memoization, immutable result
- `src/test/java/com/dwc/laf/css/CssVariableResolverTest.java` - 30 tests across 9 nested classes covering passthrough, simple refs, chained, fallbacks, nested fallbacks, circular detection, missing refs, embedded var(), immutability, malformed, null/edge cases, realistic DWC patterns

## Decisions Made
- DFS graph coloring (UNVISITED/VISITING/VISITED enum) chosen over explicit graph construction for simplicity and O(n) performance with memoization
- Cycle participants tracked in a separate set and post-processed to ensure order-independent behavior: a variable in a cycle with no fallback is excluded even if the cycle was broken by another variable's fallback
- Sentinel UNRESOLVABLE string (containing null chars) used to distinguish "resolved to empty string" from "cannot resolve" without Optional overhead in recursive calls
- Fallback values trimmed after split (leading space after comma removed) to match CSS spec's treatment of var() fallback whitespace

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed fallback whitespace trimming**
- **Found during:** Task 2 (GREEN -- running tests)
- **Issue:** splitVarArgs returns fallback text including leading space after comma (e.g., " blue" instead of "blue"), causing resolved values to have extra leading whitespace
- **Fix:** Added .trim() to fallback value in resolveVarExpression after splitVarArgs
- **Files modified:** CssVariableResolver.java
- **Verification:** All 6 fallback-related tests pass with correct trimmed values
- **Committed in:** f037903 (Task 2 commit)

**2. [Rule 1 - Bug] Fixed iteration-order-dependent cycle exclusion**
- **Found during:** Task 2 (GREEN -- running tests)
- **Issue:** When a cycle participant without a fallback was iterated before the variable that breaks the cycle with a fallback, the participant would transitively resolve via the fallback-resolved variable instead of being excluded
- **Fix:** Added cycleParticipants tracking set and post-processing pass that excludes cycle participants whose raw value has no fallback, regardless of resolution order
- **Files modified:** CssVariableResolver.java
- **Verification:** circularWithFallback test passes: --a resolves to "safe", --b excluded
- **Committed in:** f037903 (Task 2 commit)

---

**Total deviations:** 2 auto-fixed (2 bugs)
**Impact on plan:** Both fixes necessary for correctness. No scope creep.

## Issues Encountered
None beyond the auto-fixed bugs documented above.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- CssVariableResolver.resolve() is ready for downstream consumers to get fully-resolved token values
- Two-pass pipeline complete: CssTokenParser.parse() -> CssVariableResolver.resolve() -> Map<String, String> with all var() expanded
- Ready for Plan 04 (CSS value parser) to parse resolved string values into typed CssValue instances (colors, dimensions, etc.)
- All 97 tests pass across the full suite

## Self-Check: PASSED

All 2 created files verified on disk. Both task commits (7653e46, f037903) verified in git history.

---
*Phase: 01-css-token-engine*
*Completed: 2026-02-10*
