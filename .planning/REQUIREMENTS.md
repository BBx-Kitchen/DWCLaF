# Requirements: DWC Swing Look & Feel

**Defined:** 2026-02-10
**Core Value:** Swing applications look recognizably identical to their DWC web counterparts by deriving visual appearance directly from the same CSS design tokens

## v1 Requirements

Requirements for prototype release. Each maps to roadmap phases.

### CSS Parsing

- [ ] **CSS-01**: Parser extracts custom property declarations (`--name: value`) from `:root` and `:host` blocks in CSS files
- [ ] **CSS-02**: Parser resolves `var(--name, fallback)` references with nested fallback support
- [ ] **CSS-03**: Parser detects circular `var()` references and reports error instead of infinite loop
- [ ] **CSS-04**: Parser handles CSS color formats: `#hex` (3/4/6/8 digit), `rgb()`, `rgba()`, `hsl()`, `hsla()`, and space-separated modern syntax
- [ ] **CSS-05**: Parser converts HSL color values (used by DWC tokens) to java.awt.Color in sRGB color space
- [ ] **CSS-06**: Parser converts numeric values with units (`rem`, `px`, `em`) to Java float/int values
- [ ] **CSS-07**: Parser loads CSS from classpath resource (bundled in JAR)
- [ ] **CSS-08**: Parser loads CSS from external file path (user override)

### Token Mapping

- [ ] **MAP-01**: Mapping layer reads a properties file that maps CSS token names to Swing UIDefaults keys
- [ ] **MAP-02**: One CSS token can map to multiple UIDefaults keys (e.g., `--dwc-border-radius` → `Button.arc` AND `CheckBox.arc`)
- [ ] **MAP-03**: Mapping populates UIDefaults with typed Java values (Color, int, float, Font, Insets)
- [ ] **MAP-04**: Mapping configuration is external and overridable without code changes

### Look & Feel Core

- [ ] **LAF-01**: L&F extends BasicLookAndFeel and registers all custom ComponentUI delegates
- [ ] **LAF-02**: L&F loads bundled light theme CSS on initialization
- [ ] **LAF-03**: L&F populates UIDefaults from parsed CSS tokens via mapping layer
- [ ] **LAF-04**: L&F can be activated via standard `UIManager.setLookAndFeel()` API

### Shared Painting

- [ ] **PAINT-01**: All painting uses antialiased rendering (RenderingHints)
- [ ] **PAINT-02**: Rounded corners painted via configurable arc radius from CSS tokens
- [ ] **PAINT-03**: Focus ring painted outside component bounds with semi-transparent color matching DWC focus-visible style
- [ ] **PAINT-04**: Box shadow / elevation painted behind components using cached blurred images
- [ ] **PAINT-05**: HiDPI-aware painting using float coordinates and Graphics2D scaling
- [ ] **PAINT-06**: State color resolver picks correct color for hover/pressed/focused/disabled states

### Component: JButton

- [ ] **BTN-01**: JButton paints with rounded background, border, and text from CSS tokens
- [ ] **BTN-02**: JButton shows distinct visual states: normal, hover, pressed, focused, disabled
- [ ] **BTN-03**: JButton supports primary/default variant with accent color background
- [ ] **BTN-04**: JButton supports icon rendering alongside text

### Component: JTextField

- [ ] **TF-01**: JTextField paints with rounded border and background from CSS tokens
- [ ] **TF-02**: JTextField shows distinct visual states: normal, hover, focused, disabled
- [ ] **TF-03**: JTextField renders placeholder text in muted color when empty and unfocused
- [ ] **TF-04**: JTextField shows focus ring on focus matching DWC input focus style

### Component: JCheckBox

- [ ] **CB-01**: JCheckBox paints custom check mark (not system default) matching DWC checkbox
- [ ] **CB-02**: JCheckBox shows distinct visual states: normal, hover, checked, disabled
- [ ] **CB-03**: JCheckBox uses accent color for checked state background

### Component: JRadioButton

- [ ] **RB-01**: JRadioButton paints custom circular indicator with dot matching DWC radio
- [ ] **RB-02**: JRadioButton shows distinct visual states: normal, hover, selected, disabled

### Component: JComboBox

- [ ] **CMB-01**: JComboBox paints with styled dropdown arrow and rounded border
- [ ] **CMB-02**: JComboBox popup list uses themed styling (selection highlight, hover)
- [ ] **CMB-03**: JComboBox shows distinct visual states: normal, hover, focused, disabled

### Component: JLabel

- [ ] **LBL-01**: JLabel renders with typography tokens from CSS (font family, size, weight, color)

### Component: JPanel

- [ ] **PNL-01**: JPanel supports card-style rendering with rounded corners and elevation shadow
- [ ] **PNL-02**: JPanel background color derived from CSS surface token

### Component: JTabbedPane

- [ ] **TAB-01**: JTabbedPane tab strip styled with DWC tab appearance (active indicator, hover effect)
- [ ] **TAB-02**: JTabbedPane shows distinct tab states: normal, hover, selected, disabled
- [ ] **TAB-03**: JTabbedPane content area has consistent background with panel styling

### Demo

- [ ] **DEMO-01**: Demo application shows all 8 themed components in a scrollable gallery
- [ ] **DEMO-02**: Demo shows each component in all its states (normal, hover, pressed, focused, disabled)
- [ ] **DEMO-03**: Demo is a single runnable main class with no framework dependencies

### Build & Packaging

- [ ] **BUILD-01**: Project builds with Maven producing a single JAR with zero external runtime dependencies
- [ ] **BUILD-02**: Bundled CSS theme file included as classpath resource in JAR
- [ ] **BUILD-03**: JAR works on Java 21+ with any Swing application via `UIManager.setLookAndFeel()`

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

### Extended Themes

- **THEME-01**: Dark theme CSS bundled and loadable
- **THEME-02**: Runtime theme switching (light ↔ dark) without application restart
- **THEME-03**: External CSS file override path for custom themes

### Extended Components

- **XTND-01**: JTable with row striping, header styling, selection highlighting
- **XTND-02**: JTree with custom expand/collapse icons, selection styling
- **XTND-03**: JScrollBar with thin modern track/thumb styling
- **XTND-04**: JProgressBar with color variants (primary, success, danger)
- **XTND-05**: JToolTip with rounded corners and shadow

### Polish

- **POLISH-01**: Animated state transitions (hover fade, press scale, focus ring animation)
- **POLISH-02**: SVG icon support with theme-aware tinting
- **POLISH-03**: CSS hot-reload (file watcher re-parses and repaints)

## Out of Scope

| Feature | Reason |
|---------|--------|
| Full CSS cascade/specificity engine | Browser engine problem; only custom properties needed |
| SCSS parsing | Requires SCSS compiler; parse compiled CSS output instead |
| CSS `calc()` expressions | Pre-compute in SCSS build; adds parser complexity for little value |
| Layout / positioning / responsive | Visual appearance only; Swing has its own layout system |
| Pixel-perfect web matching | Font metrics, subpixel rendering differ fundamentally between web and Java2D |
| Animation / transitions | Adds 2-3x complexity to every delegate; defer to v2 |
| All 63 DWC components | 8 core components prove feasibility; extend incrementally |
| Maven Central publishing | Prototype first; packaging/publishing is a future step |
| IntelliJ .theme.json compatibility | FlatLaf already does this; not our differentiator |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| CSS-01 | Phase 1 | Pending |
| CSS-02 | Phase 1 | Pending |
| CSS-03 | Phase 1 | Pending |
| CSS-04 | Phase 1 | Pending |
| CSS-05 | Phase 1 | Pending |
| CSS-06 | Phase 1 | Pending |
| CSS-07 | Phase 1 | Pending |
| CSS-08 | Phase 1 | Pending |
| MAP-01 | Phase 2 | Pending |
| MAP-02 | Phase 2 | Pending |
| MAP-03 | Phase 2 | Pending |
| MAP-04 | Phase 2 | Pending |
| LAF-01 | Phase 2 | Pending |
| LAF-02 | Phase 2 | Pending |
| LAF-03 | Phase 2 | Pending |
| LAF-04 | Phase 2 | Pending |
| PAINT-01 | Phase 3 | Pending |
| PAINT-02 | Phase 3 | Pending |
| PAINT-03 | Phase 3 | Pending |
| PAINT-04 | Phase 3 | Pending |
| PAINT-05 | Phase 3 | Pending |
| PAINT-06 | Phase 3 | Pending |
| BTN-01 | Phase 4 | Pending |
| BTN-02 | Phase 4 | Pending |
| BTN-03 | Phase 4 | Pending |
| BTN-04 | Phase 4 | Pending |
| TF-01 | Phase 5 | Pending |
| TF-02 | Phase 5 | Pending |
| TF-03 | Phase 5 | Pending |
| TF-04 | Phase 5 | Pending |
| CB-01 | Phase 6 | Pending |
| CB-02 | Phase 6 | Pending |
| CB-03 | Phase 6 | Pending |
| RB-01 | Phase 6 | Pending |
| RB-02 | Phase 6 | Pending |
| CMB-01 | Phase 6 | Pending |
| CMB-02 | Phase 6 | Pending |
| CMB-03 | Phase 6 | Pending |
| LBL-01 | Phase 7 | Pending |
| PNL-01 | Phase 7 | Pending |
| PNL-02 | Phase 7 | Pending |
| TAB-01 | Phase 7 | Pending |
| TAB-02 | Phase 7 | Pending |
| TAB-03 | Phase 7 | Pending |
| DEMO-01 | Phase 8 | Pending |
| DEMO-02 | Phase 8 | Pending |
| DEMO-03 | Phase 8 | Pending |
| BUILD-01 | Phase 2 | Pending |
| BUILD-02 | Phase 2 | Pending |
| BUILD-03 | Phase 2 | Pending |

**Coverage:**
- v1 requirements: 48 total
- Mapped to phases: 48
- Unmapped: 0

---
*Requirements defined: 2026-02-10*
*Last updated: 2026-02-10 after roadmap creation*
