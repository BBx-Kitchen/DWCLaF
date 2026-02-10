# Phase 2: UIDefaults Bridge & L&F Setup - Research

**Researched:** 2026-02-10
**Domain:** Java Swing LookAndFeel Architecture, UIDefaults Population, CSS Token Mapping
**Confidence:** HIGH

## Summary

This phase bridges Phase 1's CSS token engine (CssTokenMap with typed CssValue records) to Swing's UIDefaults system, and creates the DwcLookAndFeel class that extends BasicLookAndFeel. The core challenge is designing a configurable mapping layer that translates CSS custom property names (like `--dwc-color-primary-40`) to Swing UIDefaults keys (like `Button.default.background`), handling type conversion from CssValue records to the specific Java types UIDefaults expects (ColorUIResource, Integer, Float, FontUIResource, InsetsUIResource).

The mapping must be one-to-many (one CSS token can populate multiple UIDefaults keys) and externally configurable via a properties file. The DwcLookAndFeel skeleton must properly implement the BasicLookAndFeel contract: overriding `initClassDefaults()` to register custom ComponentUI delegate class names, overriding `initComponentDefaults()` to populate UIDefaults from CSS tokens via the mapping layer, and implementing the five abstract methods from LookAndFeel (getName, getID, getDescription, isNativeLookAndFeel, isSupportedLookAndFeel). The L&F must activate via standard `UIManager.setLookAndFeel()`.

FlatLaf provides the reference architecture: it extends BasicLookAndFeel, registers custom delegates via properties files, creates a UIDefaults table with ~1500 initial capacity, and uses ColorUIResource/FontUIResource wrappers. Our approach is simpler -- we use a properties file mapping CSS token names to UIDefaults keys, and the mapping layer reads CssTokenMap to populate UIDefaults with correctly-typed values. Build requirements ensure the single JAR has zero external runtime dependencies and works on Java 21+.

**Primary recommendation:** Build three components in sequence: (1) token-mapping.properties format and parser, (2) UIDefaultsPopulator that reads the mapping and CssTokenMap to populate a UIDefaults table with typed values, (3) DwcLookAndFeel skeleton that wires CSS loading to UIDefaults population during initialization.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Java | 21+ | Language & runtime | Records, sealed classes, pattern matching; project minimum |
| javax.swing.plaf.basic.BasicLookAndFeel | JDK built-in | L&F base class | Cleanest foundation; no Metal visual opinions; FlatLaf uses same base |
| javax.swing.UIDefaults | JDK built-in | Default values table | Standard Swing mechanism for L&F defaults |
| java.util.Properties | JDK built-in | Mapping configuration | Standard Java properties file parsing; zero dependencies |
| javax.swing.plaf.ColorUIResource | JDK built-in | Color wrapper | Required UIResource marker for L&F-provided colors |
| javax.swing.plaf.FontUIResource | JDK built-in | Font wrapper | Required UIResource marker for L&F-provided fonts |
| javax.swing.plaf.InsetsUIResource | JDK built-in | Insets wrapper | Required UIResource marker for L&F-provided insets |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| JUnit 5 | 5.11.4 | Testing | All unit tests for mapping, population, L&F activation |
| Maven | 3.9+ | Build | Single JAR production, classpath resource bundling |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| .properties file for mapping | JSON/YAML mapping | Properties is simpler, zero-dep, standard Java; JSON adds parser dependency |
| Custom mapping parser | Annotation-based mapping | Properties is external and overridable (MAP-04); annotations are compile-time only |
| Manual UIDefaults population | FlatLaf's UIDefaultsLoader | Adding FlatLaf as dependency violates zero-dep requirement (BUILD-01) |

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/com/dwc/laf/
├── DwcLookAndFeel.java              # Main L&F entry point, extends BasicLookAndFeel
├── defaults/
│   ├── TokenMappingConfig.java      # Reads and parses token-mapping.properties
│   └── UIDefaultsPopulator.java     # Converts CssTokenMap entries to UIDefaults via mapping
├── css/                             # (Phase 1 - already exists)
│   ├── CssThemeLoader.java
│   ├── CssTokenMap.java
│   ├── CssValue.java
│   └── ... (other Phase 1 classes)
src/main/resources/com/dwc/laf/
├── token-mapping.properties         # CSS token -> UIDefaults key mapping
├── themes/
│   └── default-light.css            # (Phase 1 - already exists)
```

### Pattern 1: Properties File Mapping Format
**What:** A properties file where each line maps a CSS token name to one or more UIDefaults keys with a type hint.
**When to use:** For all CSS-to-UIDefaults mappings.
**Format:**
```properties
# Format: css.token.name = type:UIDefaultsKey[, type:UIDefaultsKey, ...]
# Type prefixes: color:, int:, float:, font:, insets:, string:
# When type is omitted, auto-detect from CssValue type

# One-to-one mapping
--dwc-color-body-text = color:Label.foreground

# One-to-many mapping (MAP-02)
--dwc-border-radius = int:Button.arc, int:CheckBox.arc, int:ComboBox.arc, int:TextField.arc

# Color mappings
--dwc-color-primary = color:Button.default.background, color:CheckBox.checkedBackground
--dwc-color-default = color:Button.background, color:ComboBox.background
--dwc-surface-3 = color:Panel.background, color:control

# Dimension -> int conversion (rem/px -> pixels)
--dwc-font-size = int:defaultFont.size
--dwc-border-width = int:Component.borderWidth

# Font family mapping
--dwc-font-family = string:defaultFont.family
```

### Pattern 2: UIResource Wrapping (Critical)
**What:** All values placed in UIDefaults by the L&F MUST be wrapped in UIResource marker types.
**When to use:** Every single value written to UIDefaults during L&F initialization.
**Why:** Swing uses `instanceof UIResource` checks to distinguish L&F defaults from developer-set values. If you put a plain `Color` in UIDefaults, Swing's `installColors()` will NOT override it on L&F change, because it only replaces `null` or `UIResource` instances. Using plain values breaks theme switching.
**Example:**
```java
// WRONG - breaks installColors() contract
defaults.put("Button.background", new Color(230, 230, 240));

// CORRECT - wraps in UIResource so installColors() knows this is an L&F default
defaults.put("Button.background", new ColorUIResource(new Color(230, 230, 240)));
defaults.put("Button.font", new FontUIResource("SansSerif", Font.PLAIN, 14));
defaults.put("Button.margin", new InsetsUIResource(2, 14, 2, 14));
defaults.put("Button.arc", 6);  // Integer values don't need UIResource wrapping
```

### Pattern 3: BasicLookAndFeel Initialization Order
**What:** BasicLookAndFeel.getDefaults() calls three methods in order: initClassDefaults(), initSystemColorDefaults(), initComponentDefaults().
**When to use:** Always. This is the fixed contract.
**How to override:**
```java
public class DwcLookAndFeel extends BasicLookAndFeel {

    @Override
    protected void initClassDefaults(UIDefaults table) {
        super.initClassDefaults(table);  // MUST call super first
        // Register custom ComponentUI delegates (for future phases)
        // table.put("ButtonUI", "com.dwc.laf.ui.DwcButtonUI");
        // table.put("TextFieldUI", "com.dwc.laf.ui.DwcTextFieldUI");
        // ... etc
    }

    @Override
    protected void initComponentDefaults(UIDefaults table) {
        super.initComponentDefaults(table);  // MUST call super first
        // Then overlay CSS token-derived values
        CssTokenMap tokens = CssThemeLoader.load();
        UIDefaultsPopulator.populate(table, tokens);
    }
}
```

### Pattern 4: Dimension-to-Pixel Conversion
**What:** CSS dimension values (rem, em, px) must be converted to pixel integers for Swing.
**When to use:** Whenever a CSS dimension token maps to a Swing integer key (arc, margin, border width).
**Conversion rules:**
- `px` values: use as-is (round to int)
- `rem` values: multiply by base font size (default 16px, but configurable)
- `em` values: multiply by component font size (default to base font size during initialization)
- `%` values: context-dependent, generally not mappable to static UIDefaults

```java
// In UIDefaultsPopulator
private static int dimensionToPixels(CssValue.DimensionValue dim, int baseFontSizePx) {
    return switch (dim.unit()) {
        case "px" -> dim.intValue();
        case "rem" -> Math.round(dim.value() * baseFontSizePx);
        case "em" -> Math.round(dim.value() * baseFontSizePx);
        default -> dim.intValue();  // fallback: treat as px
    };
}
```

### Pattern 5: Font Family Mapping
**What:** CSS font-family stacks don't directly map to Java font names. Must resolve to platform-available fonts.
**When to use:** When mapping `--dwc-font-family` to Swing default font.
**Strategy:**
```java
// CSS: -apple-system, BlinkMacSystemFont, 'Roboto', 'Segoe UI', Helvetica, Arial, sans-serif
// Java: Must resolve to a single available font name
private static String resolveFontFamily(String cssFontStack) {
    // Parse comma-separated list, trim quotes
    String[] families = cssFontStack.split(",");
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    Set<String> available = Set.of(ge.getAvailableFontFamilyNames());

    for (String family : families) {
        String name = family.trim().replace("'", "").replace("\"", "");
        // Map CSS generic names to Java logical font names
        if (name.equals("sans-serif")) return "SansSerif";
        if (name.equals("serif")) return "Serif";
        if (name.equals("monospace")) return "Monospaced";
        // Map system font aliases
        if (name.equals("-apple-system") || name.equals("BlinkMacSystemFont")) {
            if (available.contains(".AppleSystemUIFont")) return ".AppleSystemUIFont";
            if (available.contains("Helvetica Neue")) return "Helvetica Neue";
            continue;
        }
        // Check if font is available on this system
        if (available.contains(name)) return name;
    }
    return "SansSerif";  // ultimate fallback
}
```

### Anti-Patterns to Avoid
- **Hardcoding colors in L&F class:** All colors must come from CSS tokens via the mapping. This is the entire point of the project.
- **Not calling super.initClassDefaults/initComponentDefaults:** BasicLookAndFeel sets up essential keyboard bindings, default focus traversal, and base fonts. Skipping super breaks keyboard navigation and accessibility.
- **Putting non-UIResource values in UIDefaults:** Breaks L&F switching and the installColors/installBorder contract. Always wrap Colors in ColorUIResource, Fonts in FontUIResource, Insets in InsetsUIResource.
- **Loading CSS on every component creation:** CSS should be loaded once during L&F initialization. The resulting UIDefaults table is the runtime cache.
- **Mutable shared state in mapping config:** TokenMappingConfig should be immutable after load. Multiple threads may read UIDefaults concurrently.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Properties file parsing | Custom parser | java.util.Properties | Standard, handles escaping, comments, Unicode, multi-line |
| UIDefaults key discovery | Manual key enumeration | BasicLookAndFeel.initComponentDefaults() super call | Super call populates all standard keys; we overlay our values |
| Font name resolution | Hardcoded font map | GraphicsEnvironment.getAvailableFontFamilyNames() | Platform-specific; query at runtime |
| System color defaults | Custom system color table | BasicLookAndFeel.initSystemColorDefaults() super call | Handles platform-native color integration |
| Classpath resource loading | Custom classloader code | CssThemeLoader (Phase 1) | Already handles classpath + file override + merge |

**Key insight:** The mapping layer's job is purely mechanical translation -- read CssTokenMap, look up mapping, convert types, put in UIDefaults. The complexity lives in getting the mapping right (which tokens map to which keys) and handling type conversion correctly. The actual code should be straightforward.

## Common Pitfalls

### Pitfall 1: Missing UIResource Wrapping
**What goes wrong:** Colors, fonts, and insets placed in UIDefaults without UIResource wrappers are treated as developer-set values. When the L&F changes or theme switches, Swing's `installDefaults()` checks `if (value instanceof UIResource)` before replacing. Non-UIResource values are never replaced, causing stale values from old themes.
**Why it happens:** UIResource is a Swing-specific marker interface that's easy to forget. The code works initially -- components render correctly on first load. The bug only surfaces during L&F switching.
**How to avoid:** All Color values -> ColorUIResource. All Font values -> FontUIResource. All Insets values -> InsetsUIResource. All Border values -> implement UIResource. Integer and float values are primitives and don't need wrapping.
**Warning signs:** Theme switching leaves some components with old colors. `UIManager.put("key", value)` works but L&F-provided defaults don't update.

### Pitfall 2: initClassDefaults Delegate Registration Timing
**What goes wrong:** Registering custom ComponentUI delegate class names in Phase 2 before the delegates exist (they're built in Phases 4-7). The L&F tries to instantiate the class, gets ClassNotFoundException, and falls back to BasicLookAndFeel delegates silently.
**Why it happens:** Wanting to complete L&F setup in one pass.
**How to avoid:** In Phase 2, only override initClassDefaults to call super. Do NOT register custom delegates until their classes exist. Add delegate registrations in each component phase (4-7). The L&F skeleton should be designed to allow incremental delegate addition.
**Warning signs:** ClassNotFoundException in logs (often swallowed by Swing). Components rendering with Basic L&F appearance instead of custom.

### Pitfall 3: Properties File Loading Order and Override
**What goes wrong:** The mapping properties file is loaded from classpath but the user also wants to override it. If you only support one location, you break MAP-04 (external overridable configuration).
**Why it happens:** java.util.Properties only loads from one source at a time. Need to load bundled default, then overlay external override.
**How to avoid:** Load bundled `token-mapping.properties` from classpath first, then check for system property (e.g., `dwc.mapping`) pointing to external file. Merge external on top (external wins for duplicate keys). Log warnings for missing external file. Same pattern as CssThemeLoader's override mechanism.
**Warning signs:** User's custom mapping has no effect. External file silently ignored.

### Pitfall 4: Dimension Unit Conversion Errors
**What goes wrong:** CSS `rem` values like `0.25em` for border-radius get passed through as-is (0.25), producing nearly invisible 0px borders. Or `1rem` gets passed as integer 1 instead of 16 (pixels).
**Why it happens:** Forgetting that CSS dimensions need unit-aware conversion to pixel integers.
**How to avoid:** Every DimensionValue mapped to an int UIDefaults key MUST go through unit conversion. Default rem-to-px factor is 16. Document the conversion in the mapping file format.
**Warning signs:** Components have zero-width borders, invisible padding, or unexpectedly tiny/huge arc radii.

### Pitfall 5: calc() and Unresolvable CSS Values
**What goes wrong:** Some CSS tokens in default-light.css use `calc()` expressions (e.g., `--dwc-border-radius-pill: calc(var(--dwc-size-m) / 2)`) or complex multi-value properties (shadows). These become RawValue in Phase 1's CssValueTyper. The mapping layer tries to convert a RawValue to Color or int and fails.
**Why it happens:** Not all CSS tokens are resolvable to simple typed values.
**How to avoid:** The mapping layer must handle RawValue gracefully -- log a warning and skip, or provide a fallback value in the mapping config. Don't map tokens that are known to produce RawValue. Focus mappings on tokens that resolve to ColorValue, IntegerValue, FloatValue, DimensionValue.
**Warning signs:** NullPointerException or NumberFormatException during L&F initialization. Missing UIDefaults entries.

### Pitfall 6: Missing CssTokenMap.getDimension() Accessor
**What goes wrong:** Phase 1's CssTokenMap has getColor(), getInt(), getFloat(), getString() accessors, but no getDimension() method. DimensionValue tokens (like `--dwc-border-radius: 0.25em`) are only accessible via the generic `get()` method and require pattern matching.
**Why it happens:** Phase 1 focused on the most common accessors.
**How to avoid:** Either add a getDimension() accessor to CssTokenMap (preferred -- it's a natural extension), or use `get()` with pattern matching in the populator. Since CssTokenMap has package-private constructor, adding a method is safe.
**Warning signs:** Clunky switch/instanceof chains in the populator code.

## Code Examples

### Example 1: DwcLookAndFeel Skeleton
```java
// Source: Based on BasicLookAndFeel JDK 21 API + FlatLaf architecture pattern
package com.dwc.laf;

import com.dwc.laf.css.CssThemeLoader;
import com.dwc.laf.css.CssTokenMap;
import com.dwc.laf.defaults.TokenMappingConfig;
import com.dwc.laf.defaults.UIDefaultsPopulator;

import javax.swing.*;
import javax.swing.plaf.basic.BasicLookAndFeel;

public class DwcLookAndFeel extends BasicLookAndFeel {

    private CssTokenMap tokenMap;

    @Override
    public String getName() { return "DWC"; }

    @Override
    public String getID() { return "DwcLaf"; }

    @Override
    public String getDescription() {
        return "A Swing Look and Feel derived from DWC CSS design tokens";
    }

    @Override
    public boolean isNativeLookAndFeel() { return false; }

    @Override
    public boolean isSupportedLookAndFeel() { return true; }

    @Override
    protected void initClassDefaults(UIDefaults table) {
        super.initClassDefaults(table);
        // Custom ComponentUI delegates will be registered in Phases 4-7
        // Example (future):
        // table.put("ButtonUI", "com.dwc.laf.ui.DwcButtonUI");
    }

    @Override
    protected void initComponentDefaults(UIDefaults table) {
        super.initComponentDefaults(table);

        // Load CSS tokens (uses bundled default + optional external override)
        tokenMap = CssThemeLoader.load();

        // Load mapping configuration
        TokenMappingConfig mapping = TokenMappingConfig.loadDefault();

        // Populate UIDefaults from CSS tokens via mapping
        UIDefaultsPopulator.populate(table, tokenMap, mapping);
    }

    /** Expose loaded token map for downstream use (e.g., custom painting). */
    public CssTokenMap getTokenMap() {
        return tokenMap;
    }
}
```

### Example 2: Token Mapping Properties File
```properties
# token-mapping.properties
# Format: --css-token-name = type:UIDefaults.key [, type:UIDefaults.key ...]
#
# Type prefixes:
#   color:  -> ColorUIResource (from ColorValue)
#   int:    -> Integer (from IntegerValue or DimensionValue with unit conversion)
#   float:  -> Float (from FloatValue)
#   string: -> String (from StringValue)
#   insets: -> InsetsUIResource (from space-separated values: "top left bottom right")
#
# Lines starting with # are comments. Empty lines are ignored.
# One CSS token can map to multiple UIDefaults keys (comma-separated).

# --- Global Colors ---
--dwc-color-primary = color:Button.default.background, color:ProgressBar.foreground
--dwc-color-on-primary-text = color:Button.default.foreground
--dwc-color-default = color:Button.background, color:ToggleButton.background
--dwc-color-on-default-text = color:Button.foreground, color:ToggleButton.foreground
--dwc-color-body-text = color:Label.foreground, color:text, color:textText
--dwc-color-white = color:TextField.background, color:TextArea.background, color:List.background

# --- Surfaces ---
--dwc-surface-3 = color:Panel.background, color:control, color:window

# --- Border Radius ---
--dwc-border-radius = int:Button.arc, int:Component.arc

# --- Typography ---
--dwc-font-size = int:defaultFont.size
--dwc-font-weight = int:defaultFont.style
--dwc-font-family = string:defaultFont.family

# --- Focus Ring ---
--dwc-focus-ring-width = int:Component.focusWidth

# --- Disabled ---
--dwc-disabled-opacity = float:Component.disabledOpacity
```

### Example 3: UIDefaultsPopulator
```java
// Source: Pattern derived from FlatLaf UIDefaultsLoader architecture
package com.dwc.laf.defaults;

import com.dwc.laf.css.CssTokenMap;
import com.dwc.laf.css.CssValue;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;
import java.awt.*;
import java.util.logging.Logger;

public final class UIDefaultsPopulator {

    private static final Logger LOG = Logger.getLogger(UIDefaultsPopulator.class.getName());
    private static final int DEFAULT_BASE_FONT_SIZE_PX = 16;

    private UIDefaultsPopulator() {}

    public static void populate(UIDefaults table, CssTokenMap tokens,
                                TokenMappingConfig mapping) {
        for (var entry : mapping.entries()) {
            String cssTokenName = entry.cssTokenName();
            CssValue value = tokens.get(cssTokenName).orElse(null);
            if (value == null) {
                LOG.fine("CSS token not found: " + cssTokenName);
                continue;
            }

            for (var target : entry.targets()) {
                Object converted = convertValue(value, target.type());
                if (converted != null) {
                    table.put(target.key(), converted);
                }
            }
        }
    }

    private static Object convertValue(CssValue value, MappingType type) {
        return switch (type) {
            case COLOR -> switch (value) {
                case CssValue.ColorValue cv -> new ColorUIResource(cv.color());
                default -> null;
            };
            case INT -> switch (value) {
                case CssValue.IntegerValue iv -> iv.value();
                case CssValue.DimensionValue dv ->
                    dimensionToPixels(dv, DEFAULT_BASE_FONT_SIZE_PX);
                case CssValue.FloatValue fv -> Math.round(fv.value());
                default -> null;
            };
            case FLOAT -> switch (value) {
                case CssValue.FloatValue fv -> fv.value();
                case CssValue.IntegerValue iv -> (float) iv.value();
                default -> null;
            };
            case STRING -> switch (value) {
                case CssValue.StringValue sv -> sv.value();
                default -> null;
            };
            case INSETS -> null; // TODO: implement insets parsing
        };
    }

    private static int dimensionToPixels(CssValue.DimensionValue dim,
                                          int baseFontSizePx) {
        return switch (dim.unit()) {
            case "px" -> dim.intValue();
            case "rem", "em" -> Math.round(dim.value() * baseFontSizePx);
            default -> dim.intValue();
        };
    }
}
```

### Example 4: TokenMappingConfig Parser
```java
package com.dwc.laf.defaults;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

public final class TokenMappingConfig {

    private static final Logger LOG = Logger.getLogger(TokenMappingConfig.class.getName());
    private static final String DEFAULT_RESOURCE = "com/dwc/laf/token-mapping.properties";
    private static final String OVERRIDE_SYSTEM_PROPERTY = "dwc.mapping";

    private final List<MappingEntry> entries;

    private TokenMappingConfig(List<MappingEntry> entries) {
        this.entries = Collections.unmodifiableList(entries);
    }

    public List<MappingEntry> entries() { return entries; }

    public static TokenMappingConfig loadDefault() {
        // Load bundled mapping from classpath
        Properties props = loadFromClasspath(DEFAULT_RESOURCE);

        // Check for external override
        String overridePath = System.getProperty(OVERRIDE_SYSTEM_PROPERTY);
        if (overridePath != null && !overridePath.isBlank()) {
            Properties overrideProps = loadFromFile(overridePath);
            if (overrideProps != null) {
                props.putAll(overrideProps);  // override wins
            }
        }

        return parse(props);
    }

    private static TokenMappingConfig parse(Properties props) {
        List<MappingEntry> entries = new ArrayList<>();
        for (String cssToken : props.stringPropertyNames()) {
            String targetSpec = props.getProperty(cssToken);
            List<MappingTarget> targets = parseTargets(targetSpec);
            if (!targets.isEmpty()) {
                entries.add(new MappingEntry(cssToken, targets));
            }
        }
        return new TokenMappingConfig(entries);
    }

    private static List<MappingTarget> parseTargets(String spec) {
        List<MappingTarget> targets = new ArrayList<>();
        for (String part : spec.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;

            int colonIdx = trimmed.indexOf(':');
            if (colonIdx > 0) {
                String typeStr = trimmed.substring(0, colonIdx).trim();
                String key = trimmed.substring(colonIdx + 1).trim();
                MappingType type = MappingType.fromString(typeStr);
                targets.add(new MappingTarget(key, type));
            } else {
                // No type prefix -- auto-detect from CssValue at runtime
                targets.add(new MappingTarget(trimmed, MappingType.AUTO));
            }
        }
        return targets;
    }

    // ... classpath/file loading methods (similar to CssThemeLoader pattern)
}
```

### Example 5: L&F Activation Test
```java
// Verifying LAF-04: standard UIManager activation
@Test
void lafActivatesViaUIManager() throws Exception {
    UIManager.setLookAndFeel(new DwcLookAndFeel());
    LookAndFeel current = UIManager.getLookAndFeel();
    assertEquals("DWC", current.getName());
    assertEquals("DwcLaf", current.getID());
    assertTrue(current.isSupportedLookAndFeel());
    assertFalse(current.isNativeLookAndFeel());
}

@Test
void lafPopulatesUIDefaults() throws Exception {
    UIManager.setLookAndFeel(new DwcLookAndFeel());
    // Verify CSS tokens populated UIDefaults
    Color bg = UIManager.getColor("Panel.background");
    assertNotNull(bg, "Panel.background should be populated from CSS tokens");
    assertInstanceOf(ColorUIResource.class, bg,
        "All L&F colors must be ColorUIResource instances");
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| MetalLookAndFeel as base | BasicLookAndFeel as base | FlatLaf 2019+ | No Metal gradient/3D border opinions to override |
| XML-based theming (Synth) | Properties/code-based theming | Modern L&F projects | More flexible for custom Java2D painting |
| Hardcoded colors in delegates | UIDefaults-driven colors | Established Swing pattern | Enables runtime theme switching |
| Font hardcoding | Platform-aware font resolution | Modern L&Fs | Correct system font on macOS/Windows/Linux |

**Deprecated/outdated:**
- `MetalLookAndFeel` as base class: adds unwanted visual opinions (gradients, ocean theme colors)
- `SynthLookAndFeel` for this use case: XML-based theming cannot express custom Java2D painting
- Direct `new Color(r,g,b)` without UIResource wrapping: breaks theme switching

## Open Questions

1. **Base font size for rem conversion**
   - What we know: CSS default is 16px. DWC likely uses 16px as base.
   - What's unclear: Should this be configurable? Should it respect the system's actual DPI/scaling?
   - Recommendation: Default to 16px, make configurable via a special mapping entry or system property. Revisit when testing on HiDPI displays in Phase 3.

2. **Which CSS tokens to map initially**
   - What we know: default-light.css has hundreds of tokens. Only a subset maps to standard Swing UIDefaults keys.
   - What's unclear: The exact token-to-key mapping needs definition. Many DWC tokens are component-specific (`.dwc-button` selectors) -- Phase 1's parser extracts from `:root` and component selectors separately.
   - Recommendation: Start with a minimal mapping covering the 8 target components' basic keys (background, foreground, font, arc, margin). Expand as components are implemented in Phases 4-7.

3. **Component-selector tokens (e.g., .dwc-button)**
   - What we know: Phase 1's CssTokenParser extracts tokens from both `:root` and component selectors like `.dwc-button`. The default-light.css has component-level tokens like `--dwc-button-background`.
   - What's unclear: How the mapping layer accesses these -- are they in the same CssTokenMap or separate? Need to verify CssTokenParser behavior with component selectors.
   - Recommendation: Verify Phase 1's CssTokenParser flattens all tokens (both `:root` and component-selector) into a single map. If component-selector tokens are prefixed or namespaced, the mapping file can reference them directly.

4. **Insets mapping**
   - What we know: CSS uses separate properties for padding (padding-top, padding-right, etc.) or shorthand. Swing uses Insets as a single object.
   - What's unclear: Whether DWC tokens include shorthand inset values or only individual sides.
   - Recommendation: For Phase 2, hardcode reasonable default insets (e.g., Button.margin = 2,14,2,14) and defer CSS-driven insets to later phases when component delegates need them.

## Sources

### Primary (HIGH confidence)
- [BasicLookAndFeel JDK 21 API](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/swing/plaf/basic/BasicLookAndFeel.html) - Initialization order, method contracts, abstract methods
- [LookAndFeel JDK 21 API](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/swing/LookAndFeel.html) - UIResource wrapping contract, installColors/installBorder/installProperty methods
- [FlatLaf Properties Files](https://www.formdev.com/flatlaf/properties-files/) - Properties file format, value types, key naming conventions
- [FlatLaf How to Customize](https://www.formdev.com/flatlaf/how-to-customize/) - UIDefaults key patterns, programmatic customization
- [FlatLaf FlatLaf.java source](https://github.com/JFormDesigner/FlatLaf/blob/main/flatlaf-core/src/main/java/com/formdev/flatlaf/FlatLaf.java) - getDefaults() implementation, initialization architecture
- [FlatLaf FlatLaf.properties](https://github.com/JFormDesigner/FlatLaf/blob/main/flatlaf-core/src/main/resources/com/formdev/flatlaf/FlatLaf.properties) - Reference for UIDefaults key names and default values

### Secondary (MEDIUM confidence)
- [Java Swing UIDefaults keys gist](https://gist.github.com/itzg/5938035) - Complete list of standard UIDefaults keys for reference
- [Swing UIManager Keys](https://www.javaprogramto.com/2019/03/java-uimanager.html) - UIDefaults enumeration patterns
- [FlatLaf Customizing](https://www.formdev.com/flatlaf/customizing/) - Custom keys like Button.arc, Component.arc, CheckBox.arc

### Tertiary (LOW confidence)
- None. All findings verified against primary sources.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Pure JDK APIs, no third-party libraries needed, verified against JDK 21 docs
- Architecture: HIGH - Pattern verified against FlatLaf source code (production-proven) and JDK API contracts
- Pitfalls: HIGH - UIResource wrapping and initialization order verified in official JDK LookAndFeel docs
- Mapping format: MEDIUM - Properties file format is straightforward, but exact token-to-key mapping needs runtime validation

**Research date:** 2026-02-10
**Valid until:** 2026-03-10 (stable domain -- Swing API hasn't changed significantly in years)
