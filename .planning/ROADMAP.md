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
- [ ] **Phase 3: Shared Painting Utilities** - Cross-cutting rendering (shadows, focus rings, antialiasing)
- [ ] **Phase 4: Button Component** - First delegate validates architecture
- [ ] **Phase 5: Text Input Components** - TextField with placeholder and focus styling
- [ ] **Phase 6: Selection Components** - CheckBox, RadioButton, ComboBox with custom rendering
- [ ] **Phase 7: Display & Container Components** - Label, Panel, TabbedPane complete component set
- [ ] **Phase 8: Demo Application** - Component gallery with theme switching proves visual parity

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
**Plans**: TBD

Plans:
- [ ] TBD (defined during plan-phase)

### Phase 4: Button Component
**Goal**: JButton renders with DWC appearance proving CSS-to-delegate pipeline works end-to-end
**Depends on**: Phase 3
**Requirements**: BTN-01, BTN-02, BTN-03, BTN-04
**Success Criteria** (what must be TRUE):
  1. JButton paints with rounded background, border, and text derived from CSS tokens
  2. JButton shows visually distinct states: normal, hover, pressed, focused, disabled
  3. JButton supports primary variant with accent color background and default variant with standard colors
  4. JButton renders icons alongside text correctly
**Plans**: TBD

Plans:
- [ ] TBD (defined during plan-phase)

### Phase 5: Text Input Components
**Goal**: JTextField renders with DWC input appearance including placeholder and focus styling
**Depends on**: Phase 4
**Requirements**: TF-01, TF-02, TF-03, TF-04
**Success Criteria** (what must be TRUE):
  1. JTextField paints with rounded border and background from CSS tokens
  2. JTextField shows visually distinct states: normal, hover, focused, disabled
  3. JTextField renders placeholder text in muted color when empty and unfocused
  4. JTextField shows focus ring on focus matching DWC input focus style
**Plans**: TBD

Plans:
- [ ] TBD (defined during plan-phase)

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
**Plans**: TBD

Plans:
- [ ] TBD (defined during plan-phase)

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
**Plans**: TBD

Plans:
- [ ] TBD (defined during plan-phase)

### Phase 8: Demo Application
**Goal**: Runnable demo application proves visual parity between Swing and DWC web components
**Depends on**: Phase 7
**Requirements**: DEMO-01, DEMO-02, DEMO-03
**Success Criteria** (what must be TRUE):
  1. Demo application shows all 8 themed components in a scrollable gallery
  2. Demo shows each component in all its states (normal, hover, pressed, focused, disabled)
  3. Demo is a single runnable main class with no framework dependencies
  4. User can visually compare Swing components to DWC web equivalents side-by-side
**Plans**: TBD

Plans:
- [ ] TBD (defined during plan-phase)

## Progress

**Execution Order:**
Phases execute in numeric order: 1 -> 2 -> 3 -> 4 -> 5 -> 6 -> 7 -> 8

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. CSS Token Engine | 5/5 | ✓ Complete | 2026-02-10 |
| 2. UIDefaults Bridge & L&F Setup | 2/2 | ✓ Complete | 2026-02-10 |
| 3. Shared Painting Utilities | 0/TBD | Not started | - |
| 4. Button Component | 0/TBD | Not started | - |
| 5. Text Input Components | 0/TBD | Not started | - |
| 6. Selection Components | 0/TBD | Not started | - |
| 7. Display & Container Components | 0/TBD | Not started | - |
| 8. Demo Application | 0/TBD | Not started | - |
