# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-10)

**Core value:** Swing applications look recognizably identical to their DWC web counterparts by deriving visual appearance directly from the same CSS design tokens, eliminating manual theme duplication.
**Current focus:** Phase 6 in progress - CheckBox/RadioButton icons and UI delegates complete, ComboBox delegate next

## Current Position

Phase: 6 of 8 (Selection Components)
Plan: 1 of 2 in current phase
Status: Plan 01 Complete
Last activity: 2026-02-10 — Completed 06-01-PLAN.md (CheckBox/RadioButton icons and L&F setup)

Progress: [################░] 50%

## Performance Metrics

**Velocity:**
- Total plans completed: 14
- Average duration: 4min
- Total execution time: 0.88 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01-css-token-engine | 5 | 25min | 5min |
| 02-uidefaults-bridge-laf-setup | 2 | 8min | 4min |
| 03-shared-painting-utilities | 2 | 6min | 3min |
| 04-button-component | 2 | 5min | 2.5min |
| 05-text-input-components | 2 | 5min | 2.5min |
| 06-selection-components | 1 | 7min | 7min |

**Recent Trend:**
- Last 5 plans: 3min, 2min, 3min, 2min, 7min
- Trend: Slight increase on 06-01 (CSS calc() fix added complexity)

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
- [Phase 05]: Hover state via DwcTextFieldUI.hover client property (JTextComponent has no rollover model)
- [Phase 05]: TextField uses 2,6,2,6 default margin (tighter than button's 2,14,2,14)
- [Phase 05]: Removed --dwc-input-color mapping (currentColor unparseable; --dwc-color-black already covers it)
- [Phase 05]: Removed TextField.background from --dwc-color-white (more-specific --dwc-input-background prevails)
- [Phase 05]: paintSafely override (not paint) since BasicTextUI.paint() is final
- [Phase 05]: super.paintSafely(g) called with original Graphics (not g.create() clone) for correct clip regions
- [Phase 05]: Placeholder text via JTextField.placeholderText client property (same as FlatLaf)
- [Phase 05]: Desktop font hints used when available for placeholder antialiasing
- [Phase 06]: 16px icon content area for CheckBox/RadioButton (not 18px DWC web or 15px FlatLaf)
- [Phase 06]: Both DwcCheckBoxUI and DwcRadioButtonUI extend BasicRadioButtonUI (not BasicCheckBoxUI)
- [Phase 06]: Pre-computed primary-text contrast colors in CSS to bypass calc() limitation
- [Phase 06]: Checkmark color = --dwc-color-on-primary-text (white), not --dwc-color-primary (blue)
- [Phase 06]: Icon dimensions include focusWidth*2 for focus ring space

### Pending Todos

None yet.

### Blockers/Concerns

None.

## Session Continuity

Last session: 2026-02-10
Stopped at: Completed 06-01-PLAN.md (CheckBox/RadioButton icons and L&F setup)
Resume file: None
Next action: Execute 06-02-PLAN.md (ComboBox delegate)
