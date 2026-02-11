# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-10)

**Core value:** Swing applications look recognizably identical to their DWC web counterparts by deriving visual appearance directly from the same CSS design tokens, eliminating manual theme duplication.
**Current focus:** Phase 11 - Visual detail refinements for DWC fidelity.

## Current Position

Phase: 11 of 11 (Visual Details)
Plan: 2 of 2 in current phase (COMPLETE)
Status: Phase 11 Plan 02 Complete
Last activity: 2026-02-11 — Completed 11-02-PLAN.md (ComboBox arrow button rework)

Progress: [################################################] 100%

## Performance Metrics

**Velocity:**
- Total plans completed: 26
- Average duration: 4min
- Total execution time: 1.55 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01-css-token-engine | 5 | 25min | 5min |
| 02-uidefaults-bridge-laf-setup | 2 | 8min | 4min |
| 03-shared-painting-utilities | 2 | 6min | 3min |
| 04-button-component | 2 | 5min | 2.5min |
| 05-text-input-components | 2 | 5min | 2.5min |
| 06-selection-components | 2 | 9min | 4.5min |
| 07-display-container-components | 2 | 6min | 3min |
| 08-demo-application | 1 | 5min | 5min |
| 09-button-theme-variants | 2 | 11min | 5.5min |
| 10-more-controls | 4 | 15min | 3.8min |
| 11-visual-details | 2 | 2min | 1min |

**Recent Trend:**
- Last 5 plans: 3min, 4min, 4min, 1min, 1min
- Trend: Visual detail refinement plans execute quickly (single-file changes)

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
- [Phase 06]: ComboBox paintCurrentValueBackground no-op; background painted in custom paint() for correct z-order
- [Phase 06]: Hover exit sets client property to null (not FALSE) matching DwcTextFieldBorder's Boolean.TRUE check
- [Phase 06]: DwcComboBoxArrowButton extends JButton (not BasicArrowButton) for full painting control
- [Phase 07]: Removed getInsets override from DwcPanelUI (BasicPanelUI/ComponentUI has no getInsets method; shadow margin via focusWidth offset)
- [Phase 07]: Panel.shadowColor hard-coded Color(0,0,0,40) in initPanelDefaults (not token-mapped)
- [Phase 07]: Card-mode activation via client property: panel.putClientProperty("dwc.panelStyle", "card")
- [Phase 07]: Underline indicator painted in paintContentBorderTopEdge (at tab-content boundary), not paintTabBackground
- [Phase 07]: Non-matching content edges are no-op for clean look (no unnecessary borders)
- [Phase 07]: Tab focus ring ringWidth=2 (thinner than Component.focusWidth) for proportional appearance on tab targets
- [Phase 08]: Gallery uses BoxLayout Y_AXIS with JScrollPane for vertical scrollability
- [Phase 08]: No hardcoded colors in demo code; L&F handles all theming
- [Phase 08]: DwcComboBoxRenderer sets opaque=false for display area (index == -1) to fix disabled ComboBox gray background
- [Phase 09]: VariantColors record encapsulates 5 colors per variant (background, foreground, hover, pressed, focusRing)
- [Phase 09]: Map<String, VariantColors> replaces boolean isPrimary for N-variant support
- [Phase 09]: Per-variant focus ring computed from --dwc-color-{variant}-h/s with shared --dwc-focus-ring-l/a
- [Phase 09]: DwcButtonBorder resolves variant border via dwc.buttonType client property lookup
- [Phase 09]: Theme switcher uses boolean[] switching guard for re-entrancy from updateComponentTreeUI
- [Phase 09]: paintCurrentValue override required — BasicComboBoxUI overwrites renderer foreground with selectionForeground when focused
- [Phase 10]: Area intersection clipping for progress bar fill to preserve track rounded corners
- [Phase 10]: BasicToolTipUI.paint() text delegation via opaque=false (skip bg fill, let super handle text)
- [Phase 10]: dwc.progressType client property for color variant activation (consistent with dwc.buttonType pattern)
- [Phase 10]: Zero-size arrow buttons (FlatLaf pattern) for scrollbar with no decrease/increase buttons
- [Phase 10]: Stroked chevron icons (not filled triangles) for tree expand/collapse
- [Phase 10]: Full-width tree selection highlight via paintRow override (modern look)
- [Phase 10]: Tree.paintLines=false for clean modern appearance (no connecting lines)
- [Phase 10]: Renderer-based approach (not monolithic paint override) for JTable customization
- [Phase 10]: Table.alternateRowColor mapped from --dwc-surface-3 for subtle row striping
- [Phase 10]: TableHeader.bottomSeparatorColor mapped from --dwc-color-default-dark
- [Phase 10]: Tree.textBackground set to Tree.background to prevent grey rectangles from DefaultTreeCellRenderer
- [Phase 10]: DefaultTreeCellRenderer configured with matching non-selection/selection colors and null border selection color
- [Phase 11]: Separator uses same ComboBox.buttonArrowColor as chevron (--dwc-color-default-dark light gray, no alpha reduction needed)
- [Phase 11]: DWC suffix-separator pattern: 1px fillRect on arrow button left edge, inset 4px top/bottom

### Roadmap Evolution

- Phase 9 added: Button Theme Variants & Custom CSS Themes
- Phase 10 added: more controls
- Phase 11 added: Visual Details

### Pending Todos

None.

### Blockers/Concerns

None.

## Session Continuity

Last session: 2026-02-11
Stopped at: Completed 11-02-PLAN.md (ComboBox arrow button rework)
Resume file: None
Next action: Phase 11 complete. Run `mvn compile exec:java` to see gallery with refined ComboBox arrow rendering.
