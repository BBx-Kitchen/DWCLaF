# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-10)

**Core value:** Swing applications look recognizably identical to their DWC web counterparts by deriving visual appearance directly from the same CSS design tokens, eliminating manual theme duplication.
**Current focus:** Phase 2 - UIDefaults Bridge & L&F Setup

## Current Position

Phase: 2 of 8 (UIDefaults Bridge & L&F Setup)
Plan: 1 of 2 in current phase (02-01 done)
Status: Executing
Last activity: 2026-02-10 — Completed 02-01-PLAN.md (Token Mapping & UIDefaults Populator)

Progress: [#######░░░] 15%

## Performance Metrics

**Velocity:**
- Total plans completed: 6
- Average duration: 5min
- Total execution time: 0.50 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01-css-token-engine | 5 | 25min | 5min |
| 02-uidefaults-bridge-laf-setup | 1 | 5min | 5min |

**Recent Trend:**
- Last 5 plans: 3min, 5min, 4min, 5min, 5min
- Trend: Stable (~5min/plan)

*Updated after each plan completion*

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- Extend BasicLookAndFeel (cleanest foundation, no Metal visual opinions to override)
- Parse compiled CSS, not SCSS (simpler, more reliable; SCSS requires separate compiler)
- Bundle default CSS + allow external override (ship working out-of-box, but let users customize)
- Java 21+ minimum (enables records, sealed classes, modern APIs)
- Zero external runtime deps (L&F JARs need to be lightweight and conflict-free)
- HSL token model in Java (DWC uses HSL natively; convert at parse time to java.awt.Color)
- POM explicitly overrides external Maven profile that skips tests (skip=false on surefire and compiler)
- CssValue sealed interface with 6 record types: ColorValue, DimensionValue, IntegerValue, FloatValue, StringValue, RawValue
- NamedCssColors uses 149 entries (148 CSS standard + transparent)
- default-light.css hand-compiled from actual DWC SCSS mixin source files
- Hand-written character-by-character CSS parser (not regex) for robustness with nested parens and multi-line values
- Document-order flattening: later declarations override earlier for same property name (component naturally overrides :root)
- DFS graph coloring (UNVISITED/VISITING/VISITED) for var() cycle detection instead of explicit graph construction
- Cycle participants without fallbacks excluded via post-processing pass (order-independent behavior)
- Sentinel UNRESOLVABLE string to distinguish "resolved to empty" from "cannot resolve"
- Hand-written CSS HSL-to-RGB algorithm (not Java HSB) with hue wrapping and value clamping
- Character-scanning dimension parser to split numeric/unit boundary (not regex)
- Classification order: functions -> string patterns -> color -> dimension -> raw (prevents multi-value shorthand mistyping)
- Top-level comma detection with paren depth tracking distinguishes font stacks from hsl()/rgb() commas
- CssTokenMap package-private constructor (only CssThemeLoader creates instances)
- Missing override file is non-fatal (warning logged, bundled defaults preserved)
- Properties file format for token-mapping config (external, overridable, zero-code-change)
- ColorUIResource wrapping for UIResource contract compliance (theme switching)
- 16px base font size for rem/em to pixel conversion (matches browser default)
- INSETS type deferred to component delegate phase (needs multi-value parsing)
- RawValue skipped in AUTO mode (calc expressions not convertible to UIDefaults)

### Pending Todos

None yet.

### Blockers/Concerns

None.

## Session Continuity

Last session: 2026-02-10
Stopped at: Completed 02-01-PLAN.md (Token Mapping & UIDefaults Populator)
Resume file: None
Next action: Execute 02-02-PLAN.md (DwcLookAndFeel class, installTheme, getDefaults)
