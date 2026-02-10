# Feature Research

**Domain:** Swing Look & Feel with CSS Token Integration
**Researched:** 2026-02-10
**Confidence:** HIGH (verified against FlatLaf docs and source)

## Feature Landscape

### Table Stakes (Users Expect These)

Features that every production Swing L&F must have or it's unusable.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Consistent component styling | All visible components must look intentional, not half-styled | HIGH | Every unstyled component breaks the illusion |
| State-based rendering (hover, pressed, focus, disabled) | Users expect visual feedback on interaction | MEDIUM | FlatLaf tracks rollover via MouseListener on every component |
| Focus indication | Keyboard users need to see which component has focus | MEDIUM | FlatLaf uses `Component.focusWidth` + `Component.focusColor` |
| HiDPI / Retina support | Modern displays require scaled painting | MEDIUM | FlatLaf has dedicated `HiDPIUtils` and `UIScale` classes |
| Light and dark themes | Every modern UI supports theme switching | MEDIUM | Two CSS files (light.scss, dark.scss) already exist in DWC |
| Antialiased rendering | Aliased painting looks broken on modern displays | LOW | Single `RenderingHints` setup in base paint method |
| Rounded corners (border-radius) | Modern UIs use rounded elements; square corners look dated | LOW | FlatLaf uses `Component.arc`, `Button.arc`, `CheckBox.arc` |
| Proper insets/padding | Components need breathing room matching web spacing | LOW | Map from `--dwc-space-*` tokens to `Insets` |
| Correct font rendering | Text must use expected typeface, size, weight | MEDIUM | Map `--dwc-font-*` tokens to `Font` objects; handle platform font stack |
| Keyboard navigation | Tab, Enter, Space, mnemonics must work | LOW | BasicLookAndFeel provides this; just don't break it |
| Disabled state visual | Grayed-out / faded appearance for disabled components | LOW | Standard pattern: reduce alpha or desaturate colors |

### Differentiators (Competitive Advantage)

Features that set this CSS-driven L&F apart from FlatLaf, Darcula, etc.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| CSS-driven theming | Theme defined by CSS files, not Java code or properties — same source as web | HIGH | Core innovation; no other Swing L&F does this |
| Web-Swing visual parity | Swing app looks like the web app — same design tokens | MEDIUM | Unique value prop for DWC/webforJ ecosystem |
| Token mapping configurability | Switch between web component libraries by changing a mapping file | LOW | Properties file maps `--dwc-*` → `UIDefaults` keys |
| Runtime theme switching via CSS | Load different CSS file = different theme; no recompilation | MEDIUM | FlatLaf requires theme class switch; CSS file is simpler |
| Shadow/elevation system | Card-style elevation matching web `box-shadow` | HIGH | FlatLaf has limited shadow; DWC has full elevation scale |
| Semantic color system | Primary/success/warning/danger/info mapped from CSS tokens | LOW | FlatLaf has `@accentColor`; we'd have full semantic palette |
| External CSS override | Users drop a CSS file to customize without touching Java | LOW | FlatLaf has properties files; CSS is more widely known |

### Anti-Features (Commonly Requested, Often Problematic)

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| Animated transitions | Web components have CSS transitions | Adds 2-3x complexity to every delegate; Timer management, state machines, EDT overhead | Start with instant state switches; add animation as post-prototype enhancement |
| Full CSS selector matching | "Parse all CSS, not just tokens" | Full cascade/specificity is a browser engine problem; massive scope | Only parse `:root`/`:host` custom properties; map component styles manually |
| SVG icon rendering | DWC uses SVG icons | Requires Batik (~5MB) or custom SVG parser; huge dependency | Use pre-rasterized PNGs or Java2D-painted icons for prototype |
| CSS `calc()` support | DWC CSS uses `calc()` | Expression evaluator adds complexity; most `calc()` values can be pre-computed | Pre-compute in the SCSS build step; ship resolved values |
| Runtime SCSS compilation | "Parse SCSS directly" | SCSS compiler in Java doesn't exist without massive dependency | Parse compiled CSS output only |
| Pixel-perfect web matching | "Must look identical to browser" | Font metrics, subpixel rendering, and antialiasing differ fundamentally | "Recognizably same" visual language; document known divergences |
| Support for all 63 DWC components | "Style everything" | 63 ComponentUI delegates is 6+ months of work | Start with 8 core components; add incrementally |

## Feature Dependencies

```
CSS Token Parser
    └──requires──> Color Parser (HSL, RGB, hex)
    └──requires──> Var() Resolver
                       └──requires──> Token Store

Token-to-UIDefaults Mapper
    └──requires──> CSS Token Parser (must parse first)
    └──requires──> Mapping Configuration

ComponentUI Delegates (all)
    └──requires──> UIDefaults populated (mapper must run first)
    └──requires──> Shared Painting Utilities

Shadow Painter
    └──requires──> Color Parser (shadow color)
    └──enhances──> JPanel delegate (elevation)
    └──enhances──> JButton delegate (elevated buttons)

Focus Ring Painter
    └──enhances──> All interactive component delegates

Theme Switching
    └──requires──> CSS Token Parser (re-parse new file)
    └──requires──> UIDefaults Mapper (re-map)
    └──requires──> SwingUtilities.updateComponentTreeUI()
```

### Dependency Notes

- **ComponentUI delegates require UIDefaults**: The mapper must populate UIDefaults before any delegate can paint. Build order: Parser → Mapper → Delegates
- **Shadow painter enhances multiple delegates**: Build shared utility before component-specific delegates
- **Focus ring is cross-cutting**: Build once, use in all interactive components
- **Theme switching requires full pipeline**: Parse → Map → UpdateUI. Must be fast enough for runtime switching

## MVP Definition

### Launch With (v1 — Prototype)

- [ ] CSS custom property parser with var() resolution — proves the CSS→Java pipeline
- [ ] HSL/RGB/hex color parsing — essential for any visual output
- [ ] Token-to-UIDefaults mapping with properties file — proves configurability
- [ ] JButton with state rendering (hover, pressed, focused, disabled) — most visible component
- [ ] JTextField with placeholder and focus styling — second most common component
- [ ] JCheckBox with custom painted check mark — validates custom painting approach
- [ ] JRadioButton with custom painted dot — validates variant of checkbox approach
- [ ] JComboBox with styled dropdown — complex component validates delegate pattern
- [ ] JLabel with typography tokens — validates font mapping
- [ ] JPanel with card elevation/shadow — validates shadow painting
- [ ] JTabbedPane with tab styling — validates multi-part component painting
- [ ] Light and dark theme loading — validates theme switching
- [ ] Demo application with all components — visual proof of concept

### Add After Validation (v1.x)

- [ ] JTable with row striping and header styling — most requested after core
- [ ] JTree with custom expand/collapse icons — common in BBj applications
- [ ] JScrollBar with thin modern styling — visible in every scrollable container
- [ ] JProgressBar with color variants — simple but visually impactful
- [ ] Animated hover/focus transitions — polish
- [ ] JToolTip with rounded shadow — small but noticeable

### Future Consideration (v2+)

- [ ] Full menu system (JMenuBar, JMenu, JMenuItem)
- [ ] JSplitPane divider styling
- [ ] JSpinner styled increment/decrement
- [ ] JToggleButton switch-style rendering
- [ ] SVG icon support with theme tinting
- [ ] CSS hot-reload (file watcher)
- [ ] IntelliJ theme JSON compatibility (like FlatLaf)
- [ ] Maven Central publishing

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| CSS token parser | HIGH | MEDIUM | P1 |
| Color parsing (HSL/RGB/hex) | HIGH | LOW | P1 |
| Token-to-UIDefaults mapper | HIGH | LOW | P1 |
| JButton styling | HIGH | MEDIUM | P1 |
| JTextField styling | HIGH | MEDIUM | P1 |
| JCheckBox/JRadioButton | MEDIUM | MEDIUM | P1 |
| JComboBox styling | MEDIUM | HIGH | P1 |
| JPanel with shadow | MEDIUM | MEDIUM | P1 |
| JTabbedPane | MEDIUM | HIGH | P1 |
| Light/dark themes | HIGH | LOW | P1 |
| Demo application | HIGH | LOW | P1 |
| Animated transitions | LOW | HIGH | P3 |
| SVG icons | LOW | HIGH | P3 |
| Full CSS selector matching | LOW | VERY HIGH | P3 |

## Competitor Feature Analysis

| Feature | FlatLaf | Darcula/IntelliJ | Our Approach |
|---------|---------|-------------------|--------------|
| Theme source format | .properties files | .theme.json files | CSS files (compiled from SCSS) |
| Color system | @variable + darken()/lighten() | JSON color definitions | CSS custom properties with HSL |
| Component arc/radius | Component.arc UIDefault | Hardcoded per component | --dwc-border-radius token |
| Shadow/elevation | Limited (popup shadows only) | Drop shadow on popups | Full elevation scale from CSS |
| HiDPI support | UIScale utility class | JetBrains JRE patches | Graphics2D scaling + float painting |
| Theme customization | Properties file override | Theme plugin system | External CSS file override |
| Font handling | Platform-specific fallback | Bundled JetBrains font | CSS font-family stack mapping |
| Semantic colors | @accentColor only | Limited palette | Full primary/success/warning/danger/info/gray |
| Number of styled components | ~45 | ~50 | 8 (prototype), extensible |

## Sources

- [FlatLaf GitHub](https://github.com/JFormDesigner/FlatLaf) — Feature comparison, architecture patterns
- [FlatLaf How to Customize](https://www.formdev.com/flatlaf/how-to-customize/) — UIDefaults key system
- [FlatLaf Properties Files](https://www.formdev.com/flatlaf/properties-files/) — Theme format details
- [FlatLaf Themes](https://www.formdev.com/flatlaf/themes/) — IntelliJ theme support

---
*Feature research for: Swing Look & Feel with CSS Token Integration*
*Researched: 2026-02-10*
