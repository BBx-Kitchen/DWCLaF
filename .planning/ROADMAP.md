# Roadmap: DWC Swing Look & Feel

## Overview

This roadmap builds a Java Swing Look & Feel that makes Swing applications visually match DWC web components by parsing CSS design tokens and translating them into Swing painting logic. The journey progresses from CSS parsing foundation through UIDefaults integration, shared painting utilities, eight core component delegates, and finally a demo application proving visual parity. Each phase delivers a complete, testable capability that unblocks the next.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: CSS Token Engine** - Parse CSS custom properties with variable resolution
- [x] **Phase 2: UIDefaults Bridge & L&F Setup** - Map CSS tokens to Swing, enable L&F activation
- [x] **Phase 3: Shared Painting Utilities** - Cross-cutting rendering (shadows, focus rings, antialiasing)
- [x] **Phase 4: Button Component** - First delegate validates architecture
- [x] **Phase 5: Text Input Components** - TextField with placeholder and focus styling
- [x] **Phase 6: Selection Components** - CheckBox, RadioButton, ComboBox with custom rendering
- [x] **Phase 7: Display & Container Components** - Label, Panel, TabbedPane complete component set
- [x] **Phase 8: Demo Application** - Component gallery with theme switching proves visual parity
- [ ] **Phase 9: Button Theme Variants & Custom CSS Themes** - Semantic button variants and runtime theme switching

## Phase Details

### Phase 1: CSS Token Engine
**Goal**: CSS parser extracts custom properties and resolves variable references to typed Java values
**Depends on**: Nothing (foundation)
**Requirements**: CSS-01, CSS-02, CSS-03, CSS-04, CSS-05, CSS-06, CSS-07, CSS-08
**Success Criteria** (what must be TRUE):
  1. Parser extracts `--custom-property` declarations from `:root` blocks in CSS files
  2. Parser resolves `var(--name, fallback)` references including nested fallbacks without infinite loops
  3. HSL color values from DWC tokens convert to java.awt.Color objects in sRGB color space
  4. Parser loads CSS from both classpath resources (bundled) and external file paths (override)
  5. Circular variable references are detected and reported as errors instead of causing stack overflow
**Plans**: 5 plans

Plans:
- [x] 01-01-PLAN.md — Project scaffolding, CssValue types, NamedCssColors, bundled CSS
- [x] 01-02-PLAN.md — CssTokenParser: extract custom properties from CSS text (TDD)
- [x] 01-03-PLAN.md — CssVariableResolver: resolve var() references with cycle detection (TDD)
- [x] 01-04-PLAN.md — CssColorParser + CssDimensionParser: typed value conversion (TDD)
- [x] 01-05-PLAN.md — CssThemeLoader + CssTokenMap: public API and integration

### Phase 2: UIDefaults Bridge & L&F Setup
**Goal**: CSS tokens map to Swing UIDefaults enabling L&F activation via UIManager
**Depends on**: Phase 1
**Requirements**: MAP-01, MAP-02, MAP-03, MAP-04, LAF-01, LAF-02, LAF-03, LAF-04, BUILD-01, BUILD-02, BUILD-03
**Success Criteria** (what must be TRUE):
  1. Mapping layer reads properties file that maps CSS token names to Swing UIDefaults keys
  2. One CSS token can populate multiple UIDefaults keys (e.g., border-radius to Button.arc and CheckBox.arc)
  3. DwcLookAndFeel extends BasicLookAndFeel and activates via UIManager.setLookAndFeel()
  4. Maven builds single JAR with bundled light theme CSS as classpath resource
  5. JAR has zero external runtime dependencies and works on any Java 21+ Swing application
**Plans**: 2 plans

Plans:
- [x] 02-01-PLAN.md — Token mapping config parser, UIDefaults populator, bundled properties file
- [x] 02-02-PLAN.md — DwcLookAndFeel skeleton wiring CSS loading to UIDefaults population

### Phase 3: Shared Painting Utilities
**Goal**: Reusable painting utilities provide antialiasing, shadows, focus rings, and rounded borders for all delegates
**Depends on**: Phase 2
**Requirements**: PAINT-01, PAINT-02, PAINT-03, PAINT-04, PAINT-05, PAINT-06
**Success Criteria** (what must be TRUE):
  1. All component painting uses antialiased rendering with HiDPI-aware float coordinates
  2. Rounded corners paint correctly with configurable arc radius from CSS tokens
  3. Focus ring paints outside component bounds with semi-transparent color matching DWC focus-visible
  4. Box shadow renders behind components using cached blurred images for performance
  5. State color resolver picks correct color for hover/pressed/focused/disabled states consistently
**Plans**: 2 plans

Plans:
- [x] 03-01-PLAN.md — PaintUtils (antialiasing, rounded shapes, outlines), HiDpiUtils, StateColorResolver
- [x] 03-02-PLAN.md — FocusRingPainter (DWC focus ring) and ShadowPainter (cached Gaussian blur)

### Phase 4: Button Component
**Goal**: JButton renders with DWC appearance proving CSS-to-delegate pipeline works end-to-end
**Depends on**: Phase 3
**Requirements**: BTN-01, BTN-02, BTN-03, BTN-04
**Success Criteria** (what must be TRUE):
  1. JButton paints with rounded background, border, and text derived from CSS tokens
  2. JButton shows visually distinct states: normal, hover, pressed, focused, disabled
  3. JButton supports primary variant with accent color background and default variant with standard colors
  4. JButton renders icons alongside text correctly
**Plans**: 2 plans

Plans:
- [x] 04-01-PLAN.md — DwcButtonBorder, token mapping additions, focus ring color computation in L&F
- [x] 04-02-PLAN.md — DwcButtonUI delegate with full paint pipeline and L&F registration

### Phase 5: Text Input Components
**Goal**: JTextField renders with DWC input appearance including placeholder and focus styling
**Depends on**: Phase 4
**Requirements**: TF-01, TF-02, TF-03, TF-04
**Success Criteria** (what must be TRUE):
  1. JTextField paints with rounded border and background from CSS tokens
  2. JTextField shows visually distinct states: normal, hover, focused, disabled
  3. JTextField renders placeholder text in muted color when empty and unfocused
  4. JTextField shows focus ring on focus matching DWC input focus style
**Plans**: 2 plans

Plans:
- [x] 05-01-PLAN.md — DwcTextFieldBorder, token mapping fixes, L&F registration
- [x] 05-02-PLAN.md — DwcTextFieldUI delegate with paintSafely pipeline and placeholder text

### Phase 6: Selection Components
**Goal**: CheckBox, RadioButton, and ComboBox render with custom DWC styling completing form controls
**Depends on**: Phase 5
**Requirements**: CB-01, CB-02, CB-03, RB-01, RB-02, CMB-01, CMB-02, CMB-03
**Success Criteria** (what must be TRUE):
  1. JCheckBox paints custom check mark (not system default) with accent color for checked state
  2. JCheckBox shows visually distinct states: normal, hover, checked, disabled
  3. JRadioButton paints custom circular indicator with dot matching DWC radio styling
  4. JRadioButton shows visually distinct states: normal, hover, selected, disabled
  5. JComboBox paints with styled dropdown arrow, rounded border, and themed popup list
**Plans**: 2 plans

Plans:
- [x] 06-01-PLAN.md — Token mappings + L&F setup + CheckBox/RadioButton icons + UI delegates
- [x] 06-02-PLAN.md — DwcComboBoxUI delegate with arrow button, renderer, hover tracking

### Phase 7: Display & Container Components
**Goal**: Label, Panel, and TabbedPane complete the 8-component set
**Depends on**: Phase 6
**Requirements**: LBL-01, PNL-01, PNL-02, TAB-01, TAB-02, TAB-03
**Success Criteria** (what must be TRUE):
  1. JLabel renders with typography tokens from CSS (font family, size, weight, color)
  2. JPanel supports card-style rendering with rounded corners and elevation shadow
  3. JPanel background color derives from CSS surface token
  4. JTabbedPane tab strip styled with DWC tab appearance (active indicator, hover effect)
  5. JTabbedPane shows distinct tab states: normal, hover, selected, disabled
**Plans**: 2 plans

Plans:
- [x] 07-01-PLAN.md — DwcLabelUI + DwcPanelUI delegates with card-style shadow painting
- [x] 07-02-PLAN.md — DwcTabbedPaneUI delegate with underline indicator and hover states

### Phase 8: Demo Application
**Goal**: Runnable demo application proves visual parity between Swing and DWC web components
**Depends on**: Phase 7
**Requirements**: DEMO-01, DEMO-02, DEMO-03
**Success Criteria** (what must be TRUE):
  1. Demo application shows all 8 themed components in a scrollable gallery
  2. Demo shows each component in all its states (normal, hover, pressed, focused, disabled)
  3. Demo is a single runnable main class with no framework dependencies
  4. User can visually compare Swing components to DWC web equivalents side-by-side
**Plans**: 1 plan

Plans:
- [x] 08-01-PLAN.md — DwcComponentGallery: comprehensive 8-component gallery with exec-maven-plugin

### Phase 9: Button Theme Variants & Custom CSS Themes
**Goal**: Buttons support success/danger/warning/info variants and demo showcases custom CSS theme switching
**Depends on**: Phase 8
**Requirements**: VARIANT-01, VARIANT-02, VARIANT-03, THEME-DEMO-01, THEME-DEMO-02
**Success Criteria** (what must be TRUE):
  1. JButton supports success, danger, warning, info variants via putClientProperty("dwc.buttonType", "success") etc.
  2. Each variant uses its semantic CSS color tokens for background, foreground, hover, pressed, and border
  3. Focus ring color matches the variant's theme color (green for success, red for danger, etc.)
  4. DwcComponentGallery shows all button variants in the button section
  5. DwcComponentGallery includes a theme switcher that loads custom CSS files and re-applies the L&F
**Plans**: 2 plans

Plans:
- [ ] 09-01-PLAN.md — Button variant infrastructure: token mappings, per-variant focus ring colors, DwcButtonUI refactor, DwcButtonBorder variant awareness
- [ ] 09-02-PLAN.md — Gallery update: variant button rows, theme switcher dropdown, visual verification

## Progress

**Execution Order:**
Phases execute in numeric order: 1 -> 2 -> 3 -> 4 -> 5 -> 6 -> 7 -> 8 -> 9

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. CSS Token Engine | 5/5 | ✓ Complete | 2026-02-10 |
| 2. UIDefaults Bridge & L&F Setup | 2/2 | ✓ Complete | 2026-02-10 |
| 3. Shared Painting Utilities | 2/2 | ✓ Complete | 2026-02-10 |
| 4. Button Component | 2/2 | ✓ Complete | 2026-02-10 |
| 5. Text Input Components | 2/2 | ✓ Complete | 2026-02-10 |
| 6. Selection Components | 2/2 | ✓ Complete | 2026-02-10 |
| 7. Display & Container Components | 2/2 | ✓ Complete | 2026-02-10 |
| 8. Demo Application | 1/1 | ✓ Complete | 2026-02-10 |
| 9. Button Theme Variants & CSS Themes | 0/2 | Planned | — |
