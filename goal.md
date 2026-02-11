# SwingWebTheme - Swing Look & Feel from HTML5 Web Component CSS

## Project Goal

Prototype a Java Swing Look & Feel library that makes Swing components **visually identical** to an HTML5 Web Components library by parsing the web components' CSS theme files and translating them into Swing painting logic.

**Scope: visual appearance only.** Layout, positioning, and responsive behavior are explicitly out of scope.

## Context

We have the complete source code of an HTML5 Web Components library (built on standard Web Component specs with Shadow DOM, CSS custom properties, and `::part()` selectors). Each component ships with CSS files that define its visual appearance through:

- CSS custom properties (design tokens) on `:root` and `:host`
- Component-scoped styles using `::part()` pseudo-elements
- State-based selectors (`:hover`, `:active`, `:focus`, `:disabled`, `[selected]`, etc.)
- Dark/light theme variants

The goal is to make a Swing desktop application look indistinguishable from the web application by deriving the Swing L&F directly from the same CSS source files.

## Architecture

### Overview

```
CSS Theme Files (.css)
        |
        v
[CSS Token Parser] ----> Map<String, CSSValue>
        |
        v
[Token-to-UIDefaults Mapper] ----> UIDefaults table
        |
        v
[Custom ComponentUI Delegates] -- read UIDefaults at paint time
        |
        v
Swing components render with web-identical visuals
```

### Three-Layer Design

**Layer 1 - CSS Parser & Token Extraction**

A parser that reads CSS files and extracts:

- Custom properties from `:root` / `:host` blocks (e.g., `--dwc-color-primary: #1a73e8`)
- Resolved values (following `var()` references and fallbacks)
- Component-specific token sets (per-component CSS files)
- Media query variants for dark/light themes

Output: a `ThemeTokens` object holding all resolved design tokens as typed Java values (Color, int, float, Font, Insets, etc.).

**Layer 2 - Token Mapping to UIDefaults**

A mapping layer that translates web component tokens to Swing's `UIDefaults` keys. This is where the semantic bridge lives:

```
--dwc-color-primary          -> Button.default.background
--dwc-color-on-primary       -> Button.default.foreground
--dwc-border-radius          -> Button.arc
--dwc-font-family-base       -> defaultFont (family)
--dwc-font-size-base         -> defaultFont (size)
--dwc-color-danger           -> Button.danger.background
--dwc-shadow-elevation-1     -> Component.shadowColor + shadowSize
```

This mapping should be configurable/overridable so it can adapt to different web component libraries.

**Layer 3 - ComponentUI Delegates**

Custom painting delegates for each Swing component that use the token values to replicate the web component's visual rendering. Each delegate handles:

- Background fills (solid, gradient)
- Border rendering (stroke, radius, color per state)
- Shadow/elevation painting
- Focus ring rendering
- State transitions (hover, pressed, disabled, focused)
- Icon tinting
- Text rendering (font, color, alignment, truncation)

### CSS-to-Java2D Mapping Reference

Use this table as the canonical reference for translating CSS visual properties:

| CSS Property | Java2D Approach |
|---|---|
| `background-color` | `Graphics2D.fill()` with `Color` |
| `background: linear-gradient(...)` | `LinearGradientPaint` |
| `border-radius` | `RoundRectangle2D` or `Path2D` with arc segments |
| `border: 1px solid #ccc` | `Graphics2D.draw()` with `BasicStroke` on `RoundRectangle2D` |
| `box-shadow: 0 2px 4px rgba(0,0,0,0.2)` | Paint expanded rounded rect with blurred/faded color behind component, or use multi-pass Gaussian approach |
| `box-shadow: inset ...` | Paint inner shadow after background fill |
| `opacity` | `AlphaComposite.getInstance(SRC_OVER, alpha)` |
| `color` | `Graphics2D.setColor()` for text |
| `font-family` | `Font(name, style, size)` |
| `font-size` | `Font.deriveFont(float size)` |
| `font-weight` | `Font.BOLD` / `Font.PLAIN` (limited mapping) |
| `text-overflow: ellipsis` | `SwingUtilities.layoutCompoundLabel()` with clipping |
| `outline` (focus ring) | Draw secondary rounded rect outside component bounds with semi-transparent color |
| `transition` | `javax.swing.Timer` + easing interpolation |
| `transform: scale()` | `Graphics2D.scale()` (rare, mainly for press effects) |
| `cursor: pointer` | `Component.setCursor(Cursor.HAND_CURSOR)` |
| `::part(control)` | Sub-region painting within the delegate's `paint()` method |
| `:hover` | `MouseListener` + repaint, or `ButtonModel.isRollover()` |
| `:active` / pressed | `ButtonModel.isPressed()` |
| `:focus-visible` | `Component.hasFocus()` + paint focus ring |
| `:disabled` | `Component.isEnabled()` -> desaturated/faded rendering |
| `[selected]` / `:checked` | `ButtonModel.isSelected()` or `AbstractButton.isSelected()` |
| SVG icons | Render via Apache Batik, or pre-rasterize to `BufferedImage` at multiple sizes |

### Shadow Painting Strategy

Box shadows are one of the trickiest parts. Recommended approach:

1. Parse shadow values: `offsetX`, `offsetY`, `blurRadius`, `spreadRadius`, `color`
2. Create a `BufferedImage` slightly larger than the component (by `blurRadius + spreadRadius`)
3. Paint a filled rounded rect (matching component shape) into the image
4. Apply a Gaussian blur (use `ConvolveOp` or a dedicated blur implementation)
5. Paint the shadow image behind the component in `paintComponent()`
6. Cache the shadow image and invalidate only on resize

For the prototype, a simpler multi-rect approximation is acceptable: paint several increasingly transparent and slightly larger rounded rects behind the component.

### Animation / Transition Strategy

Web components use CSS transitions for hover/focus state changes. In Swing:

1. Maintain a `float animationProgress` (0.0 to 1.0) per animated state per component
2. On state change, start a `javax.swing.Timer` (16ms interval for ~60fps)
3. Interpolate between start and end values using an easing function
4. Call `component.repaint()` each tick
5. Use `AnimationSupport` utility class to avoid duplicating this across all delegates

Easing functions to implement:
- Linear
- Ease-in-out (cubic bezier approximation)
- Ease-out (for hover-in, most common in web UIs)

For the prototype, animation is a stretch goal. Start with instant state switches.

## Target Components (Priority Order)

### Phase 1 - Core (Prototype Scope)

1. **JButton** - including default/primary, outlined, text-only variants, icon buttons
2. **JTextField** - including placeholder text, prefix/suffix adornments, validation states
3. **JCheckBox** - custom painted check mark, indeterminate state
4. **JRadioButton** - custom painted dot
5. **JComboBox** - styled dropdown arrow, popup list styling
6. **JLabel** - mainly font/color, but also badge-style labels
7. **JPanel** - card-style elevation/shadow, rounded corners

### Phase 2 - Extended

8. **JTabbedPane** - tab strip styling, active/hover indicators
9. **JTable** - row striping, header styling, cell padding, selection highlighting
10. **JTree** - indentation, expand/collapse icons, selection styling
11. **JScrollPane** / **JScrollBar** - thin scrollbar, track/thumb styling
12. **JProgressBar** - determinate and indeterminate, color variants
13. **JSlider** - track and thumb styling
14. **JToolTip** - rounded, shadowed tooltip

### Phase 3 - Advanced

15. **JMenuBar** / **JMenu** / **JMenuItem** - modern menu styling
16. **JSplitPane** - divider styling
17. **JSpinner** - styled increment/decrement buttons
18. **JToggleButton** - switch-style toggle
19. **JList** - item styling, selection, hover

## Project Structure

```
swing-web-theme/
  pom.xml (or build.gradle)
  src/main/java/
    com/swingwebtheme/
      SwingWebThemeLookAndFeel.java          -- Main L&F entry point
      theme/
        CSSTokenParser.java                   -- Parses CSS files, extracts custom properties
        ThemeTokens.java                      -- Typed token container
        TokenResolver.java                    -- Resolves var() references and fallbacks
        UIDefaultsMapper.java                 -- Maps tokens to Swing UIDefaults
        ThemeVariant.java                     -- Enum: LIGHT, DARK
      painting/
        ShadowPainter.java                    -- Reusable shadow rendering
        FocusRingPainter.java                 -- Reusable focus ring rendering
        BorderRadiusPainter.java              -- Rounded rect utilities
        GradientPainter.java                  -- Linear/radial gradient painting
        AnimationSupport.java                 -- Timer-based state transitions
        StateColorResolver.java               -- Resolves color for current component state
      ui/
        WebButtonUI.java
        WebTextFieldUI.java
        WebCheckBoxUI.java
        WebRadioButtonUI.java
        WebComboBoxUI.java
        WebLabelUI.java
        WebPanelUI.java
        ... (one per component)
      icons/
        SVGIcon.java                          -- SVG-to-Icon adapter (optional, can use flat PNGs)
        IconTinter.java                       -- Recolors icons based on theme
  src/main/resources/
    themes/
      default-light.css                       -- Copied/linked from web component source
      default-dark.css
    token-mapping.properties                  -- Configurable CSS token -> UIDefaults mapping
  src/test/java/
    ...
  demo/
    SwingWebThemeDemo.java                    -- Visual showcase of all themed components
    ComponentGallery.java                     -- Side-by-side comparison panel
```

## Key Implementation Details

### SwingWebThemeLookAndFeel.java

```java
public class SwingWebThemeLookAndFeel extends BasicLookAndFeel {

    private ThemeTokens tokens;

    @Override
    public String getName() { return "SwingWebTheme"; }

    @Override
    public String getID() { return "SwingWebTheme"; }

    @Override
    public boolean isNativeLookAndFeel() { return false; }

    @Override
    public boolean isSupportedLookAndFeel() { return true; }

    @Override
    protected void initClassDefaults(UIDefaults table) {
        super.initClassDefaults(table);
        // Register all custom UI delegates
        table.put("ButtonUI", "com.swingwebtheme.ui.WebButtonUI");
        table.put("TextFieldUI", "com.swingwebtheme.ui.WebTextFieldUI");
        // ... etc
    }

    @Override
    protected void initComponentDefaults(UIDefaults table) {
        super.initComponentDefaults(table);
        // Load CSS tokens and populate UIDefaults
        CSSTokenParser parser = new CSSTokenParser();
        this.tokens = parser.parse(getThemeCSS());
        UIDefaultsMapper mapper = new UIDefaultsMapper(tokens);
        mapper.applyTo(table);
    }
}
```

### CSSTokenParser - Core Parsing Logic

The parser needs to handle:

1. Standard CSS custom property declarations: `--name: value;`
2. `var()` references with optional fallbacks: `var(--name, fallback)`
3. Nested `var()` in fallbacks: `var(--a, var(--b, #fff))`
4. Color formats: hex (#rgb, #rrggbb, #rrggbbaa), rgb(), rgba(), hsl(), hsla(), named colors
5. Numeric values with units: px, em, rem, %, unitless
6. Shorthand properties: `border`, `padding`, `margin`, `box-shadow`, `font`
7. Multiple selectors/blocks (`:root`, `:host`, `.theme-dark`, media queries)

For the prototype, focus on `:root` / `:host` custom properties and basic `var()` resolution. Ignore media queries initially - handle dark mode via separate CSS file loading.

### Token Mapping Configuration

The `token-mapping.properties` file allows non-code customization:

```properties
# Format: css-token = swing-uidefaults-key [| type-hint]

--dwc-color-primary = Button.default.background | color
--dwc-color-primary-dark = Button.default.pressedBackground | color
--dwc-color-on-primary = Button.default.foreground | color
--dwc-color-surface = Panel.background | color
--dwc-color-on-surface = Panel.foreground | color
--dwc-border-radius = Button.arc | int
--dwc-border-radius = CheckBox.arc | int
--dwc-font-size-base = defaultFont.size | float
--dwc-font-family-base = defaultFont.family | string
--dwc-shadow-elevation-1 = Component.shadowSpec | shadow
--dwc-color-danger = Button.danger.background | color
--dwc-color-success = Component.successColor | color
--dwc-color-warning = Component.warningColor | color
--dwc-spacing-s = Component.innerPadding | insets
```

### Demo Application

The demo should provide:

1. **Component gallery** - every themed component in all its states (normal, hover, pressed, focused, disabled) shown in a scrollable panel
2. **Theme switcher** - toggle between light and dark themes at runtime
3. **Side-by-side mode** (stretch goal) - embed a JavaFX WebView showing the actual web component next to the Swing equivalent for visual comparison

## Build & Dependencies

### Maven Setup

- Java 17+ (for modern APIs, records, sealed classes where useful)
- No mandatory external dependencies for core L&F
- Optional: Apache Batik for SVG icon rendering
- Optional: FlatLaf as reference/fallback (study its architecture, but don't depend on it at runtime)
- JUnit 5 for tests
- Demo: single main class, no framework needed

### Build Command

```bash
mvn clean package
java -cp target/swing-web-theme-1.0-SNAPSHOT.jar com.swingwebtheme.demo.SwingWebThemeDemo
```

## Constraints & Decisions

- **Extend `BasicLookAndFeel`**, not `MetalLookAndFeel`. Basic provides the cleanest foundation with minimal visual opinions.
- **No AWT peers** - all painting is pure Java2D. This ensures cross-platform consistency.
- **Antialiasing always on** - set `RenderingHints.KEY_ANTIALIASING` to `VALUE_ANTIALIAS_ON` globally.
- **HiDPI aware** - use `Graphics2D` scaling, avoid hardcoded pixel values. Use float-based painting where possible.
- **No reflection hacks** - stay within public Swing API. If something can't be styled through the official L&F mechanism, document it as a limitation.
- **Thread safety** - all UI painting on EDT as per Swing convention. Animation timers dispatch to EDT.
- **Token names are configurable** - the library should work with any web component library's CSS, not just one specific set. The token mapping file is the adaptation layer.

## Prototype Success Criteria

The prototype is successful when:

1. The CSS parser correctly extracts custom properties from a real web component theme CSS file
2. At least JButton, JTextField, JCheckBox, and JComboBox are visually styled
3. Components respond to state changes (hover, press, focus, disabled) with correct visual feedback
4. Light and dark themes can be switched at runtime
5. A demo window shows all themed components and they look recognizably similar to the web originals
6. The shadow and focus ring painting produces visually acceptable results (not necessarily pixel-perfect)

## Stretch Goals (Post-Prototype)

- Animated state transitions (hover fade-in, press scale, focus ring animation)
- SVG icon support with theme-aware tinting
- Runtime CSS hot-reload (watch CSS file for changes, re-parse and repaint)
- Automatic mapping discovery (heuristic matching of CSS token names to Swing UIDefaults keys)
- Gradle plugin / Maven plugin to generate L&F classes from CSS at build time
- Screenshot comparison tests (render Swing component, render web component via headless browser, diff images)

## Reference Material

- FlatLaf source: https://github.com/JFormDesigner/FlatLaf - best modern Swing L&F, study its `FlatButtonUI`, `FlatTextFieldUI`, shadow painting, and theme file parsing
- Swing L&F documentation: Oracle's "A Synth Primer" and the `javax.swing.plaf.basic` package source
- CSS Values and Units spec: how `var()`, `calc()`, color functions work
- Web Components spec: Shadow DOM, `::part()`, CSS custom properties inheritance
