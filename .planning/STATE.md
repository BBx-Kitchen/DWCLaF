# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-10)

**Core value:** Swing applications look recognizably identical to their DWC web counterparts by deriving visual appearance directly from the same CSS design tokens, eliminating manual theme duplication.
**Current focus:** Phase 4 complete and verified - ready for Phase 5 (Text Input Components)

## Current Position

Phase: 4 of 8 (Button Delegates)
Plan: 2 of 2 in current phase (phase complete)
Status: Phase Complete
Last activity: 2026-02-10 — Completed 04-02-PLAN.md (DwcButtonUI delegate)

Progress: [############░░░] 35%

## Performance Metrics

**Velocity:**
- Total plans completed: 11
- Average duration: 4min
- Total execution time: 0.68 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01-css-token-engine | 5 | 25min | 5min |
| 02-uidefaults-bridge-laf-setup | 2 | 8min | 4min |
| 03-shared-painting-utilities | 2 | 6min | 3min |
| 04-button-component | 2 | 5min | 2.5min |

**Recent Trend:**
- Last 5 plans: 3min, 3min, 3min, 3min, 2min
- Trend: Stable (~3min/plan)

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
- CSS font-weight >= 600 maps to Font.BOLD, < 600 maps to Font.PLAIN
- CSS font-family stack resolved left-to-right with GraphicsEnvironment availability check
- CSS generic font names mapped to Java logical fonts (sans-serif->SansSerif, serif->Serif, monospace->Monospaced)
- Platform font aliases: -apple-system/BlinkMacSystemFont -> .AppleSystemUIFont
- 14 component font keys set from single CSS font token for consistent typography
- Pressed state requires both isArmed() AND isPressed() on ButtonModel (FlatLaf pattern)
- HiDPI scale read from Graphics2D transform only, never system properties or Toolkit
- hasFocus() used on Component (not isFocusOwner) for broader compatibility
- Shadow blur radius clamped to 50px max (DWC uses up to 100px but excessive for Swing)
- ShadowCacheKey as Java record with auto hashCode/equals for all visual parameters
- applyGaussianBlur package-private for direct testability (project convention)
- Shadow images at logical dimensions; Graphics2D transform handles HiDPI scaling
- [Phase 04]: Focus ring color stored as Component.focusRingColor (shared across all components) rather than Button-specific key
- [Phase 04]: Private hslToColor helper in DwcLookAndFeel (not reusing CssColorParser internals)
- [Phase 04]: DimensionValue with % unit accessed via tokenMap.get() pattern match on CssValue.DimensionValue
- [Phase 04]: Per-component DwcButtonUI instances (not shared singleton) for future per-component state caching
- [Phase 04]: Full paint() override without calling super.paint() for complete rendering control
- [Phase 04]: LookAndFeel.installProperty for opaque=false (respects UIResource contract)

### Pending Todos

None yet.

### Blockers/Concerns

None.

## Session Continuity

Last session: 2026-02-10
Stopped at: Completed 04-02-PLAN.md (DwcButtonUI delegate) - Phase 04 complete
Resume file: None
Next action: Research/plan Phase 05 (Text Input Components)
