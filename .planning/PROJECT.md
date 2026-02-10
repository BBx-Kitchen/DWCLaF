# DWC Swing Look & Feel

## What This Is

A standalone Java Swing Look & Feel engine that makes Swing components visually match DWC (Dynamic Web Client) web components by parsing the web components' CSS theme files and translating design tokens into Swing painting logic. Ships as a JAR with bundled default themes, with the option to load external CSS files for custom theming. BBj-agnostic — works with any Swing application.

## Core Value

Swing applications look recognizably identical to their DWC web counterparts by deriving visual appearance directly from the same CSS design tokens, eliminating manual theme duplication.

## Requirements

### Validated

- ✓ DWC web component CSS source available locally (63 components with SCSS, design token system using HSL color model, spacing/font/shadow/border token scales) — existing
- ✓ DWC uses CSS custom properties (`--dwc-*`) organized via SCSS mixins with light/dark theme variants — existing

### Active

- [ ] CSS token parser extracts custom properties from compiled CSS files
- [ ] `var()` references resolved with fallback support
- [ ] HSL color model tokens converted to Java Color objects
- [ ] Token-to-UIDefaults mapping layer bridges CSS tokens to Swing keys
- [ ] Mapping is configurable via external properties file
- [ ] JButton styled with DWC button appearance (default, primary, outlined, text-only variants)
- [ ] JTextField styled with DWC input appearance (placeholder, prefix/suffix, validation states)
- [ ] JCheckBox styled with custom painted check mark and indeterminate state
- [ ] JRadioButton styled with custom painted dot
- [ ] JComboBox styled with dropdown arrow and popup list
- [ ] JLabel styled with DWC typography (font, color, badge variant)
- [ ] JPanel styled with card elevation/shadow and rounded corners
- [ ] JTabbedPane styled with DWC tab strip (active/hover indicators)
- [ ] Components respond to state changes (hover, pressed, focused, disabled)
- [ ] Focus ring rendering matches DWC focus-visible style
- [ ] Shadow/elevation painting for card-style panels
- [ ] Light and dark theme variants loadable
- [ ] Default compiled CSS bundled as JAR resource
- [ ] External CSS file path override supported
- [ ] Demo application showing all themed components in all states
- [ ] Theme switcher in demo (light/dark toggle at runtime)

### Out of Scope

- Layout, positioning, responsive behavior — visual appearance only
- Animation/transitions — stretch goal post-prototype, start with instant state switches
- SVG icon rendering — defer to post-prototype
- CSS hot-reload — stretch goal
- Maven Central publishing — prototype first, packaging later
- SCSS parsing — parse compiled CSS output, not raw SCSS
- Pixel-perfect fidelity — recognizably same visual language, pragmatic about Swing limitations
- Components beyond Phase 1 (JTable, JTree, JScrollBar, JMenu, etc.) — extend after prototype validates approach

## Context

- DWC design tokens use an HSL color model with separate `--dwc-color-{name}-h` (hue), `-s` (saturation), `-c` (chroma) properties that generate color variations programmatically
- 63 DWC web components exist with individual SCSS files, but the prototype targets the 8 most common Swing equivalents
- DWC's theme system defines tokens via SCSS mixins (`props-colors`, `props-spaces`, `props-fonts`, `props-shadows`, `props-borders`) included into `:root` in `light.scss`/`dark.scss`
- BBj uses system/default Java L&F by default but is compatible with FlatLaf and custom L&Fs
- The prototype serves dual purpose: BBj desktop client theming AND potential webforJ/Webswing offering for non-BBj Swing apps
- FlatLaf is the reference modern Swing L&F to study for architecture patterns (shadow painting, theme parsing, UI delegate structure)
- DWC source code lives in `dwc/` subdirectory for reference; BBj source at `/Users/beff/svn/trunk/com/basis/`

## Constraints

- **Java version**: Java 21+ minimum
- **Base class**: Extend `BasicLookAndFeel`, not `MetalLookAndFeel` — cleanest foundation with minimal visual opinions
- **Painting**: Pure Java2D, no AWT peers — cross-platform consistency
- **Antialiasing**: Always on (`RenderingHints.KEY_ANTIALIASING` = `VALUE_ANTIALIAS_ON`)
- **HiDPI**: Float-based painting, no hardcoded pixel values, use Graphics2D scaling
- **Public API only**: No reflection hacks — stay within official Swing L&F mechanism
- **Thread safety**: All UI painting on EDT, animation timers dispatch to EDT
- **No runtime deps**: Core L&F has zero mandatory external dependencies (CSS parser is built-in)
- **Token agnostic**: Mapping layer should work with any web component library's CSS tokens, not just DWC — the mapping file is the adaptation layer

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Extend BasicLookAndFeel | Cleanest foundation, no Metal visual opinions to override | — Pending |
| Parse compiled CSS, not SCSS | Simpler, more reliable; SCSS requires a separate compiler | — Pending |
| Bundle default CSS + allow external override | Ship working out-of-box, but let users customize themes | — Pending |
| Java 21+ minimum | Enables records, sealed classes, modern APIs | — Pending |
| Zero external runtime deps | L&F JARs need to be lightweight and conflict-free on any classpath | — Pending |
| HSL token model in Java | DWC uses HSL natively; convert at parse time to java.awt.Color | — Pending |

---
*Last updated: 2026-02-10 after initialization*
