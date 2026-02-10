# Architecture Research

**Domain:** Swing Look & Feel with CSS Token Integration
**Researched:** 2026-02-10
**Confidence:** HIGH (verified against FlatLaf source architecture)

## Standard Architecture

### System Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    CSS Theme Files                            │
│  light.css  dark.css  [external override.css]                │
└──────────────────────────┬──────────────────────────────────┘
                           │ parse
┌──────────────────────────▼──────────────────────────────────┐
│              Layer 1: CSS Token Engine                        │
│  ┌──────────┐  ┌──────────────┐  ┌────────────────┐         │
│  │  Lexer   │→ │ Property     │→ │ Var() Resolver │         │
│  │          │  │ Extractor    │  │ + Color Parser │         │
│  └──────────┘  └──────────────┘  └───────┬────────┘         │
│                                          │                   │
│                              ThemeTokens (Map<String,Value>) │
└──────────────────────────────────────────┬──────────────────┘
                                           │ map
┌──────────────────────────────────────────▼──────────────────┐
│              Layer 2: Token → UIDefaults Bridge               │
│  ┌──────────────────┐  ┌─────────────────────────────┐      │
│  │ Mapping Config   │→ │ UIDefaultsMapper            │      │
│  │ (.properties)    │  │ token → UIDefaults key      │      │
│  └──────────────────┘  └────────────┬────────────────┘      │
│                                     │                        │
│                          UIDefaults table populated           │
└─────────────────────────────────────┬───────────────────────┘
                                      │ read at paint time
┌─────────────────────────────────────▼───────────────────────┐
│              Layer 3: ComponentUI Delegates                   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│  │ ButtonUI │ │ FieldUI  │ │CheckBoxUI│ │ PanelUI  │ ...   │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘       │
│       │            │            │            │              │
│  ┌────▼────────────▼────────────▼────────────▼────────┐     │
│  │           Shared Painting Utilities                 │     │
│  │  ShadowPainter  FocusRingPainter  BorderPainter    │     │
│  │  StateColorResolver  GradientPainter               │     │
│  └────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Responsibility | Typical Implementation |
|-----------|----------------|------------------------|
| CSSTokenParser | Read CSS file, extract `--custom-property: value` declarations | Hand-written lexer scanning for `:root`/`:host` blocks |
| VarResolver | Resolve `var(--name, fallback)` references recursively with cycle detection | Recursive resolution with visited-set; max depth limit |
| ColorParser | Parse CSS color formats (hex, rgb, hsl, named) to java.awt.Color | Static utility methods; sRGB ColorSpace explicitly |
| ThemeTokens | Immutable container of all resolved tokens as typed Java values | Record or sealed class hierarchy (ColorValue, NumberValue, StringValue) |
| UIDefaultsMapper | Read mapping config, convert ThemeTokens → UIDefaults entries | Properties file drives mapping; one CSS token → multiple UIDefaults keys |
| DwcLookAndFeel | Main L&F entry point; extends BasicLookAndFeel | Override `initClassDefaults`, `initComponentDefaults`; load CSS on init |
| ComponentUI delegates | Paint individual components using UIDefaults values | One class per component; `createUI()` + `installDefaults()` + `paint()` |
| ShadowPainter | Render box-shadow equivalent using cached blurred images | LRU cache keyed by (elevation, width, height, arc); Gaussian blur |
| FocusRingPainter | Render focus-visible ring outside component bounds | Draw rounded rect with semi-transparent color + offset |
| StateColorResolver | Determine correct color for component's current state (hover/press/focus/disabled) | Read ButtonModel or component state flags; interpolate if animated |

## Recommended Project Structure

```
src/main/java/com/dwclaf/
├── DwcLookAndFeel.java              # Main L&F entry point
├── DwcLightLaf.java                 # Light theme (loads light.css)
├── DwcDarkLaf.java                  # Dark theme (loads dark.css)
├── theme/
│   ├── CSSTokenParser.java          # Parses CSS files → raw property map
│   ├── VarResolver.java             # Resolves var() references
│   ├── ColorParser.java             # Parses CSS color values → Color
│   ├── ValueParser.java             # Parses numbers, insets, fonts, shadows
│   ├── ThemeTokens.java             # Immutable resolved token container
│   └── TokenMapping.java            # Reads mapping config
├── defaults/
│   ├── UIDefaultsMapper.java        # Populates UIDefaults from tokens
│   └── UIDefaultsKeys.java          # Constants for all UIDefaults keys used
├── painting/
│   ├── ShadowPainter.java           # Box-shadow rendering with cache
│   ├── FocusRingPainter.java        # Focus-visible ring
│   ├── BorderPainter.java           # Rounded border with stroke
│   ├── GradientPainter.java         # Linear gradient backgrounds
│   └── DwcPaintingUtils.java        # Antialiasing setup, HiDPI helpers
├── ui/
│   ├── DwcButtonUI.java             # JButton delegate
│   ├── DwcTextFieldUI.java          # JTextField delegate
│   ├── DwcCheckBoxUI.java           # JCheckBox delegate
│   ├── DwcRadioButtonUI.java        # JRadioButton delegate
│   ├── DwcComboBoxUI.java           # JComboBox delegate
│   ├── DwcLabelUI.java              # JLabel delegate
│   ├── DwcPanelUI.java              # JPanel delegate
│   └── DwcTabbedPaneUI.java         # JTabbedPane delegate
├── state/
│   ├── ComponentStateTracker.java   # Tracks hover/focus per component
│   └── StateColors.java             # Resolves color for current state
└── util/
    ├── HiDPIUtils.java              # Scaling utilities
    └── ColorUtils.java              # Color manipulation (darken, lighten, alpha)
src/main/resources/
├── com/dwclaf/
│   ├── themes/
│   │   ├── light.css                # Compiled DWC light theme
│   │   └── dark.css                 # Compiled DWC dark theme
│   └── token-mapping.properties     # CSS token → UIDefaults key mapping
src/test/java/com/dwclaf/
├── theme/
│   ├── CSSTokenParserTest.java
│   ├── VarResolverTest.java
│   └── ColorParserTest.java
├── defaults/
│   └── UIDefaultsMapperTest.java
├── ui/
│   └── ... (per-component visual tests)
└── demo/
    └── DwcLafDemo.java              # Demo application
```

### Structure Rationale

- **`theme/`**: CSS parsing is self-contained. No dependency on Swing. Can be tested independently
- **`defaults/`**: Mapping layer depends on `theme/` but not on `ui/`. Bridges CSS world to Swing world
- **`painting/`**: Shared utilities used by all delegates. Depends on `defaults/` for reading UIDefaults
- **`ui/`**: One delegate per component. Depends on `painting/` and `defaults/`
- **`state/`**: Cross-cutting state tracking. Used by `ui/` delegates

## How FlatLaf Does It

FlatLaf's architecture is the reference pattern:

### Theme Loading (Properties → UIDefaults)
1. `FlatLaf.getDefaults()` creates UIDefaults table with ~1500 initial capacity
2. Calls `super.initClassDefaults()`, `super.initComponentDefaults()` (from BasicLookAndFeel)
3. Loads `.properties` files matching class hierarchy: `FlatLaf.properties` → `FlatLightLaf.properties`
4. `UIDefaultsLoader` parses each line: `key = value` with type inference
5. Variables (`@name`) are resolved within properties files (not added to UIDefaults)
6. Color functions `darken()`, `lighten()` computed at load time

### ComponentUI Registration
```java
// In FlatLaf.initClassDefaults():
table.put("ButtonUI", "com.formdev.flatlaf.ui.FlatButtonUI");
table.put("TextFieldUI", "com.formdev.flatlaf.ui.FlatTextFieldUI");
// ... one per component
```

### Delegate Pattern (FlatButtonUI example)
1. `createUI(JComponent)` — static factory; returns shared or per-instance delegate
2. `installDefaults(AbstractButton)` — reads UIDefaults: `Button.background`, `Button.foreground`, `Button.arc`, etc.
3. `paint(Graphics, JComponent)` — calls `paintBackground()`, `paintBorder()`, `paintText()`, `paintIcon()`
4. `paintBackground()` — uses `RoundRectangle2D` with `Button.arc`; different colors for hover/pressed/focused
5. `uninstallDefaults()` — cleanup

### Key FlatLaf Patterns We Should Follow
- **UIDefaults keys follow `Component.property` convention**: `Button.arc`, `Button.focusedBackground`, `TextField.placeholderForeground`
- **Shared painting utilities**: `FlatUIUtils` has `paintComponentBackground()`, `paintOutlinedComponent()`, `paintRoundedRect()`
- **State tracking via AbstractButton.getModel()**: `isRollover()`, `isPressed()`, `isSelected()`, `isEnabled()`
- **HiDPI-aware painting**: All coordinates use `float` via `UIScale.scale()` and `Graphics2D` transforms
- **Properties files loaded in class hierarchy order**: Base → Light/Dark → IntelliJ → User override

## Data Flow

### CSS File → Tokens → UIDefaults → Painting

```
1. Application startup: UIManager.setLookAndFeel(new DwcLightLaf())
                                    │
2. DwcLightLaf.initComponentDefaults(UIDefaults table)
                                    │
3. CSSTokenParser.parse("themes/light.css")
   │  - Lexes CSS into tokens
   │  - Extracts --custom-property declarations from :root blocks
   │  - Passes to VarResolver
   │
4. VarResolver.resolve(rawProperties)
   │  - Resolves var(--name, fallback) references
   │  - Detects cycles (visited set)
   │  - Returns Map<String, String> of resolved values
   │
5. ValueParser.parseAll(resolvedStrings)
   │  - Converts string values to typed Java objects
   │  - ColorParser: "#hex" / "rgb()" / "hsl()" → Color (sRGB)
   │  - Numbers: "0.5rem" → float (converted to pixels)
   │  - Fonts: family + size + weight → Font
   │  - Shadows: "0 2px 4px rgba(...)" → ShadowSpec
   │  - Returns ThemeTokens (immutable)
   │
6. UIDefaultsMapper.apply(ThemeTokens, UIDefaults, "token-mapping.properties")
   │  - Reads mapping: --dwc-color-primary → Button.default.background
   │  - One CSS token may map to multiple UIDefaults keys
   │  - Populates UIDefaults table
   │
7. Component created: new JButton("Click me")
   │  - Swing calls DwcButtonUI.createUI(button)
   │  - DwcButtonUI.installDefaults() reads UIDefaults:
   │      Color bg = UIManager.getColor("Button.background")
   │      int arc = UIManager.getInt("Button.arc")
   │
8. Paint cycle: DwcButtonUI.paint(Graphics g, JComponent c)
   │  - Read component state: model.isRollover(), model.isPressed()
   │  - StateColorResolver picks correct color for state
   │  - ShadowPainter.paint() draws elevation shadow
   │  - Fill rounded rect with background color
   │  - BorderPainter.paint() draws border stroke
   │  - Paint text + icon
   │  - FocusRingPainter.paint() if focused
```

### Theme Switch Flow

```
1. User triggers theme switch
2. DwcLookAndFeel.switchTheme("dark.css")
3. CSSTokenParser.parse("dark.css") → new ThemeTokens
4. UIDefaultsMapper.apply(newTokens, UIManager.getDefaults())
5. SwingUtilities.updateComponentTreeUI(rootFrame)
6. All components repaint with new colors
```

## Build Order

Dependencies flow top-down. Each layer must be complete before the next starts.

```
Phase 1: Foundation (no Swing dependencies)
  ├── 1a. CSSTokenParser + VarResolver    ← parse CSS, resolve variables
  ├── 1b. ColorParser + ValueParser       ← typed value conversion
  └── 1c. ThemeTokens container           ← immutable token store

Phase 2: Swing Bridge
  ├── 2a. UIDefaultsMapper               ← token → UIDefaults mapping
  ├── 2b. DwcLookAndFeel skeleton        ← L&F registration, CSS loading
  └── 2c. token-mapping.properties       ← DWC-specific mappings

Phase 3: Shared Painting
  ├── 3a. DwcPaintingUtils               ← antialiasing, HiDPI setup
  ├── 3b. BorderPainter                  ← rounded borders
  ├── 3c. ShadowPainter                  ← elevation shadows with cache
  ├── 3d. FocusRingPainter               ← focus-visible ring
  └── 3e. StateColorResolver             ← state-based color picking

Phase 4: Component Delegates (can parallelize)
  ├── 4a. DwcButtonUI                    ← most complex, do first
  ├── 4b. DwcTextFieldUI                 ← second most complex
  ├── 4c. DwcCheckBoxUI + DwcRadioButtonUI  ← similar painting patterns
  ├── 4d. DwcLabelUI + DwcPanelUI        ← simpler delegates
  ├── 4e. DwcComboBoxUI                  ← complex (popup, arrow, list)
  └── 4f. DwcTabbedPaneUI               ← complex (tab strip, content area)

Phase 5: Themes & Demo
  ├── 5a. Light/dark CSS bundled          ← compile DWC SCSS, bundle
  ├── 5b. DwcLightLaf + DwcDarkLaf       ← theme variant classes
  └── 5c. Demo application               ← component gallery + theme switch
```

## Anti-Patterns

### Anti-Pattern 1: Hardcoding Colors in Delegates

**What people do:** Paint with `new Color(0x1a73e8)` directly in ComponentUI.paint()
**Why it's wrong:** Bypasses the entire token/UIDefaults system; themes won't work
**Do this instead:** Always read from UIDefaults: `UIManager.getColor("Button.background")`

### Anti-Pattern 2: Full CSS Cascade Implementation

**What people do:** Build a complete CSS engine with specificity, inheritance, cascade
**Why it's wrong:** That's a browser engine; takes years to build correctly
**Do this instead:** Only parse `:root`/`:host` custom property declarations. Use the mapping properties file for the semantic bridge

### Anti-Pattern 3: Modifying Graphics State Without Restore

**What people do:** Set antialiasing, color, stroke on the passed-in Graphics without creating a copy
**Why it's wrong:** Corrupts painting state for subsequent components
**Do this instead:**
```java
Graphics2D g2 = (Graphics2D) g.create();
try {
    g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
    // paint
} finally {
    g2.dispose();
}
```

### Anti-Pattern 4: Computing Shadows in Every Paint Call

**What people do:** Generate Gaussian blur image on every `paint()` invocation
**Why it's wrong:** Gaussian blur is O(n*m*k) per pixel; 20 components = 20x that cost per frame
**Do this instead:** Cache shadow images keyed by (width, height, elevation, arc). Invalidate only on resize

### Anti-Pattern 5: Blocking EDT During CSS Parsing

**What people do:** Parse CSS file synchronously during L&F initialization on EDT
**Why it's wrong:** Large CSS files could freeze the UI during startup
**Do this instead:** For prototype, synchronous is fine (CSS files are small). For production, parse on background thread and apply via `SwingUtilities.invokeLater()`

## Sources

- [FlatLaf GitHub - FlatLaf.java](https://github.com/JFormDesigner/FlatLaf/blob/main/flatlaf-core/src/main/java/com/formdev/flatlaf/FlatLaf.java) — Main L&F class architecture
- [FlatLaf GitHub - UIDefaultsLoader.java](https://github.com/JFormDesigner/FlatLaf/blob/main/flatlaf-core/src/main/java/com/formdev/flatlaf/UIDefaultsLoader.java) — Properties parsing pattern
- [FlatLaf How to Customize](https://www.formdev.com/flatlaf/how-to-customize/) — UIDefaults key conventions
- [FlatLaf Properties Files](https://www.formdev.com/flatlaf/properties-files/) — Theme file format

---
*Architecture research for: Swing Look & Feel with CSS Token Integration*
*Researched: 2026-02-10*
