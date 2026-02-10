# Phase 9: Button Theme Variants & Custom CSS Themes - Research

**Researched:** 2026-02-10
**Domain:** Swing L&F button variant theming, CSS token-driven color resolution, runtime theme switching
**Confidence:** HIGH

## Summary

This phase adds four semantic button variants (success, danger, warning, info) to the existing DwcButtonUI and surfaces the custom CSS theme loading mechanism in the demo gallery. The CSS token infrastructure is already fully built -- the default-light.css theme contains all necessary semantic color aliases for every variant (e.g., `--dwc-color-success`, `--dwc-color-success-light`, `--dwc-color-success-dark`, `--dwc-color-on-success-text`, etc.). The CssThemeLoader already supports external CSS override via the `dwc.theme` system property, and CssTokenParser correctly handles both `:root` and arbitrary selector blocks (verified by examining theme1.css and theme2.css).

The primary engineering work is: (1) extending DwcButtonUI to resolve colors per-variant instead of the current two-branch primary/default logic, (2) adding token-mapping entries so variant colors flow through UIDefaults, (3) computing per-variant focus ring colors in DwcLookAndFeel, and (4) updating the gallery demo with variant showcase and theme switching UI. The DWC web component SCSS (dwc-button.scss) uses a `button-theme-generator` mixin that maps each theme name to `--dwc-color-{name}`, `--dwc-color-{name}-light`, `--dwc-color-{name}-dark`, `--dwc-color-on-{name}-text`, etc. -- this is the exact pattern to replicate in Java.

**Primary recommendation:** Add a `dwc.buttonType` client property enum (success/danger/warning/info/primary/default), map each to its semantic color tokens via UIDefaults, and refactor DwcButtonUI's color resolution from a boolean `isPrimary` branch to a variant-keyed lookup table.

## Standard Stack

### Core (already in project)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Swing BasicButtonUI | JDK 17+ | Base class for DwcButtonUI | Standard Swing L&F delegation pattern |
| UIManager/UIDefaults | JDK 17+ | Theme color storage and lookup | Standard Swing theme mechanism |
| CssThemeLoader | internal | Loads + merges CSS token files | Already built in Phase 1 |
| CssTokenMap | internal | Typed access to resolved CSS tokens | Already built in Phase 1 |
| UIDefaultsPopulator | internal | Bridges CSS tokens to UIDefaults | Already built in Phase 2 |
| StateColorResolver | internal | Resolves state-dependent colors | Already built in Phase 3 |

### No new external dependencies required
This phase is purely additive to the existing internal architecture. No new libraries needed.

## Architecture Patterns

### Current Button Architecture
```
DwcLookAndFeel.initComponentDefaults()
    -> CssThemeLoader.load()  [CSS -> CssTokenMap]
    -> UIDefaultsPopulator.populate()  [CssTokenMap -> UIDefaults via token-mapping.properties]
    -> initButtonDefaults()  [computed values: focus ring, margin, etc.]

DwcButtonUI.installDefaults()
    -> reads UIManager.getColor("Button.background") etc.
    -> reads UIManager.getColor("Button.default.background") etc. [primary variant]

DwcButtonUI.paint()
    -> isPrimary(b) [checks clientProperty "dwc.buttonType" == "primary"]
    -> resolveBackground(b, primary) [2-branch: default or primary colors]
    -> resolveForeground(b, primary) [2-branch]
```

### Target Architecture: Variant-Keyed Color Resolution
```
DwcButtonUI.installDefaults()
    -> reads per-variant colors from UIManager:
       Button.success.background, Button.success.foreground, etc.
       Button.danger.background, Button.danger.foreground, etc.
       Button.warning.background, Button.warning.foreground, etc.
       Button.info.background, Button.info.foreground, etc.

DwcButtonUI.paint()
    -> getVariant(b) [returns enum/string: default/primary/success/danger/warning/info]
    -> resolveBackground(b, variant) [variant-keyed lookup]
    -> resolveForeground(b, variant) [variant-keyed lookup]
    -> resolveFocusRingColor(variant) [variant-keyed focus ring]
```

### Pattern 1: Variant Color Set Encapsulation
**What:** Group the 5 state colors (background, foreground, hover, pressed, border) into a value object per variant
**When to use:** When extending the button to support N>2 variants
**Example:**
```java
// Encapsulate all colors for one button variant
private record VariantColors(
    Color background,
    Color foreground,
    Color hoverBackground,
    Color pressedBackground,
    Color borderColor,
    Color focusRingColor
) {}

// Keyed lookup in installDefaults()
private final Map<String, VariantColors> variantColors = new HashMap<>();

// In installDefaults():
for (String variant : List.of("default", "primary", "success", "danger", "warning", "info")) {
    String prefix = variant.equals("default") ? "Button" : "Button." + variant;
    variantColors.put(variant, new VariantColors(
        UIManager.getColor(prefix + ".background"),
        UIManager.getColor(prefix + ".foreground"),
        UIManager.getColor(prefix + ".hoverBackground"),
        UIManager.getColor(prefix + ".pressedBackground"),
        UIManager.getColor(prefix + ".borderColor"),
        UIManager.getColor("Component.focusRingColor." + variant)
    ));
}
```

### Pattern 2: UIDefaults Key Naming Convention
**What:** Follow the existing Swing/FlatLaf convention for variant-specific UIDefaults keys
**When to use:** When adding new variant keys to token-mapping.properties
**Example:**
```properties
# Existing pattern (primary uses "Button.default.*" -- Swing convention for "default button")
--dwc-color-primary = color:Button.default.background
--dwc-color-on-primary-text = color:Button.default.foreground

# New variant keys follow same pattern:
--dwc-color-success = color:Button.success.background
--dwc-color-on-success-text = color:Button.success.foreground
--dwc-color-success-light = color:Button.success.hoverBackground
--dwc-color-success-dark = color:Button.success.pressedBackground
```

### Pattern 3: Client Property API for Variant Selection
**What:** Use `putClientProperty("dwc.buttonType", "success")` to select variant
**When to use:** Application code setting button themes
**Example:**
```java
JButton btn = new JButton("Delete");
btn.putClientProperty("dwc.buttonType", "danger");

JButton btn2 = new JButton("Save");
btn2.putClientProperty("dwc.buttonType", "success");
```

### Pattern 4: Per-Variant Focus Ring Color Computation
**What:** Compute focus ring color from HSL tokens for each variant, not just primary
**When to use:** In DwcLookAndFeel.initButtonDefaults()
**Example:**
```java
// Current: only computes focus ring from --dwc-color-primary-h/s
// Target: compute for each variant using --dwc-color-{variant}-h/s
private void initFocusRingColors(UIDefaults table) {
    for (String variant : List.of("primary", "success", "danger", "warning", "info")) {
        String hToken = "--dwc-color-" + variant + "-h";
        String sToken = "--dwc-color-" + variant + "-s";
        // ... same HSL computation as existing initFocusRingColor()
        table.put("Component.focusRingColor." + variant, new ColorUIResource(color));
    }
}
```

### Anti-Patterns to Avoid
- **Hard-coding HSL values in Java:** Use CSS token resolution, not hard-coded color constants. The CSS has all the semantic aliases already.
- **Using Java Color HSB (not HSL):** Java's `Color.getHSBColor()` uses HSB/HSV, not CSS HSL. The existing `hslToColor()` in DwcLookAndFeel handles this correctly -- reuse it.
- **Duplicating color resolution logic per variant:** Use a data-driven approach (map/record), not a cascade of if/else branches.
- **Re-creating the L&F to switch themes:** The `dwc.theme` property + `UIManager.setLookAndFeel(new DwcLookAndFeel())` is the correct pattern. Do not try to hot-swap individual UIDefaults entries.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| HSL-to-RGB conversion | Custom math | Existing `DwcLookAndFeel.hslToColor()` | Already tested and matches CSS spec |
| State color resolution | Per-variant if/else | `StateColorResolver.resolve()` | Already handles disabled/hover/pressed/focus priority chain |
| CSS token parsing | Manual string parsing | `CssThemeLoader.load()` + `CssTokenMap` | Handles var() resolution, type parsing, merging |
| Focus ring painting | Custom ring code | `FocusRingPainter.paintFocusRing()` | Already handles sub-pixel anti-aliased ring with configurable width |
| Button border painting | Custom border | `DwcButtonBorder` (extend for variant border color) | Already computes focus-width-aware insets |

**Key insight:** The CSS token engine (Phase 1) and painting utilities (Phase 3) handle all the hard infrastructure. This phase is primarily a mapping/wiring exercise.

## Common Pitfalls

### Pitfall 1: The "Button.default.*" Naming Confusion
**What goes wrong:** In Swing, "Button.default.*" keys (like `Button.default.background`) refer to the "default button" (the button that responds to Enter key) -- which in DWC maps to the "primary" variant. New variant keys should NOT use this overloaded name.
**Why it happens:** The existing code uses `Button.default.background` for the primary variant, following FlatLaf convention where "default" means the JRootPane's default button.
**How to avoid:** Keep `Button.default.*` for primary. Use `Button.success.*`, `Button.danger.*`, etc. for new variants. The `dwc.buttonType` client property value "primary" maps to UIDefaults `Button.default.*`.
**Warning signs:** Confusing "default" (Swing's default button / primary) with "default" (DWC's neutral gray theme).

### Pitfall 2: Missing Semantic Color Tokens for Variants
**What goes wrong:** Trying to use `--dwc-color-success-40` directly instead of `--dwc-color-success` (the semantic alias).
**Why it happens:** The CSS has both raw lightness steps (5-95) and semantic aliases (base/light/dark).
**How to avoid:** Map to the semantic aliases: `--dwc-color-success`, `--dwc-color-success-light`, `--dwc-color-success-dark`, `--dwc-color-on-success-text`, `--dwc-color-on-success-text-light`, `--dwc-color-on-success-text-dark`. These are what the DWC button SCSS `button-theme-generator` mixin uses.
**Warning signs:** Using numeric suffixes (e.g., `-40`, `-45`) instead of semantic names (`-light`, `-dark`).

### Pitfall 3: DwcButtonBorder Uses Global Border Color
**What goes wrong:** All button variants share the same `Button.borderColor` because `DwcButtonBorder.paintBorder()` reads from `UIManager.getColor("Button.borderColor")` statically.
**Why it happens:** The border is painted by `DwcButtonBorder`, not by `DwcButtonUI`. The border class has no access to the button's variant.
**How to avoid:** Two options: (1) Make DwcButtonBorder variant-aware by reading the button's client property and looking up variant-specific border color from UIManager, or (2) Store the resolved border color on the button (e.g., via a transient client property set by DwcButtonUI) and have DwcButtonBorder read it. Option 1 is cleaner.
**Warning signs:** All variant buttons showing the same border color (gray default border).

### Pitfall 4: Focus Ring Color Per-Variant
**What goes wrong:** All variant buttons show the primary blue focus ring.
**Why it happens:** `Component.focusRingColor` is a single global value. DwcButtonUI reads it in `installDefaults()` and uses it for all buttons.
**How to avoid:** Compute per-variant focus ring colors in `initButtonDefaults()` and store as `Component.focusRingColor.success`, etc. In DwcButtonUI, resolve the focus ring color based on the button's variant.
**Warning signs:** A green success button with a blue focus ring.

### Pitfall 5: Theme Switching Requires Full L&F Re-installation
**What goes wrong:** Changing the `dwc.theme` system property without re-installing the L&F.
**Why it happens:** `CssThemeLoader.load()` reads the system property at load time. The token map is immutable.
**How to avoid:** For theme switching in the demo: `System.setProperty("dwc.theme", path)` then `UIManager.setLookAndFeel(new DwcLookAndFeel())` then `SwingUtilities.updateComponentTreeUI(frame)`.
**Warning signs:** Theme selector changes but UI doesn't update.

### Pitfall 6: hslToColor is Private in DwcLookAndFeel
**What goes wrong:** Cannot reuse the existing HSL-to-Color conversion from outside the class.
**Why it happens:** `hslToColor()` is `private static` in DwcLookAndFeel.
**How to avoid:** Either make it package-private/protected, extract to a shared utility, or inline the computation. Since the focus ring computation for variants will be in DwcLookAndFeel itself, this is only an issue if computed elsewhere.
**Warning signs:** Duplicated HSL conversion code.

## Code Examples

### CSS Token Pattern for Each Variant
Each variant has this set of semantic aliases in default-light.css (verified):

```css
/* SUCCESS example -- same pattern for danger/warning/info */
--dwc-color-success: var(--dwc-color-success-25);              /* base bg */
--dwc-color-success-light: var(--dwc-color-success-30);         /* hover bg */
--dwc-color-success-dark: var(--dwc-color-success-20);          /* pressed bg */
--dwc-color-on-success-text: var(--dwc-color-success-text-25);  /* base fg */
--dwc-color-on-success-text-light: var(--dwc-color-success-text-30); /* hover fg */
--dwc-color-on-success-text-dark: var(--dwc-color-success-text-20);  /* pressed fg */
--dwc-border-color-success: var(--dwc-color-success);           /* border */
```

### Token Mapping Additions
```properties
# SUCCESS button variant
--dwc-color-success = color:Button.success.background
--dwc-color-on-success-text = color:Button.success.foreground
--dwc-color-success-light = color:Button.success.hoverBackground
--dwc-color-success-dark = color:Button.success.pressedBackground
--dwc-border-color-success = color:Button.success.borderColor

# DANGER button variant
--dwc-color-danger = color:Button.danger.background
--dwc-color-on-danger-text = color:Button.danger.foreground
--dwc-color-danger-light = color:Button.danger.hoverBackground
--dwc-color-danger-dark = color:Button.danger.pressedBackground
--dwc-border-color-danger = color:Button.danger.borderColor

# WARNING button variant
--dwc-color-warning = color:Button.warning.background
--dwc-color-on-warning-text = color:Button.warning.foreground
--dwc-color-warning-light = color:Button.warning.hoverBackground
--dwc-color-warning-dark = color:Button.warning.pressedBackground
--dwc-border-color-warning = color:Button.warning.borderColor

# INFO button variant
--dwc-color-info = color:Button.info.background
--dwc-color-on-info-text = color:Button.info.foreground
--dwc-color-info-light = color:Button.info.hoverBackground
--dwc-color-info-dark = color:Button.info.pressedBackground
--dwc-border-color-info = color:Button.info.borderColor
```

**IMPORTANT:** Some of these CSS tokens (e.g., `--dwc-color-danger`) are already mapped to non-button UIDefaults keys. The token-mapping.properties format supports comma-separated targets, so the new button keys should be appended to existing mappings where applicable. For example:
```properties
# BEFORE (existing):
--dwc-color-danger = color:TextField.errorBorderColor, color:Component.error.focusedBorderColor
# AFTER (appended):
--dwc-color-danger = color:TextField.errorBorderColor, color:Component.error.focusedBorderColor, color:Button.danger.background
```

### Theme Switching in Demo
```java
// Create theme selector combo box
JComboBox<String> themeSelector = new JComboBox<>(
    new String[]{"Default (bundled)", "Theme 1 (Teal)", "Theme 2 (Purple)"});

themeSelector.addActionListener(e -> {
    int idx = themeSelector.getSelectedIndex();
    switch (idx) {
        case 0 -> System.clearProperty("dwc.theme");
        case 1 -> System.setProperty("dwc.theme", "css/theme1.css");
        case 2 -> System.setProperty("dwc.theme", "css/theme2.css");
    }
    try {
        UIManager.setLookAndFeel(new DwcLookAndFeel());
        SwingUtilities.updateComponentTreeUI(
            SwingUtilities.getWindowAncestor(themeSelector));
    } catch (Exception ex) {
        ex.printStackTrace();
    }
});
```

### DWC Button SCSS Theme Generator (Reference)
Source: `/Users/beff/_lab/dwclaf/dwc/src/components/dwc-button/dwc-button.scss` lines 214-261

The SCSS mixin `button-theme-generator` generates CSS for each variant. For the Java port, the pattern is:
```
variant "success" ->
  background:  --dwc-color-success
  foreground:  --dwc-color-on-success-text
  hover bg:    --dwc-color-success-light
  hover fg:    --dwc-color-on-success-text-light
  pressed bg:  --dwc-color-success-dark
  pressed fg:  --dwc-color-on-success-text-dark
  border:      --dwc-color-success (same as background)
  focus ring:  hsla(success-h, success-s, focus-ring-l, focus-ring-a)
```

## State of the Art

| Old Approach (current) | New Approach (target) | Impact |
|------------------------|-----------------------|--------|
| Boolean `isPrimary()` check | String-based variant lookup | Supports N variants cleanly |
| 2 color sets (default + primary) | N color sets in Map<String, VariantColors> | Eliminates code duplication |
| Single `Component.focusRingColor` | Per-variant focus ring colors | Correct visual feedback per variant |
| No theme switching in demo | Theme selector with `dwc.theme` + full L&F reinstall | Demonstrates custom CSS theme support |
| `DwcButtonBorder` reads global border color | `DwcButtonBorder` reads variant-specific border color | Correct border color per variant |

## Critical Design Decisions

### 1. How to handle the "default" variant vs Swing's "default button"
The existing code uses `Button.default.*` for primary because Swing's "default button" concept (JRootPane.setDefaultButton) maps to the visually prominent button. DWC's "default" theme is the neutral gray variant.

**Recommendation:** Keep the existing convention unchanged:
- `dwc.buttonType` = null or missing -> DWC "default" theme -> `Button.background` (gray)
- `dwc.buttonType` = "primary" -> DWC "primary" theme -> `Button.default.background` (blue)
- `dwc.buttonType` = "success" -> DWC "success" theme -> `Button.success.background` (green)
- etc.

### 2. Token mapping collision handling
Some CSS tokens like `--dwc-color-success-light` are not currently mapped to anything in token-mapping.properties. Others like `--dwc-color-danger` already have existing mappings. New button keys must be appended as additional targets, not replace existing ones.

### 3. Variant-specific border color in DwcButtonBorder
DwcButtonBorder currently reads `UIManager.getColor("Button.borderColor")` which is the default variant's border. For themed variants, the border should match the variant's background color (as the DWC SCSS shows: `--dwc-button-border-color: var(--dwc-button-background)`).

**Recommendation:** Have DwcButtonBorder check the button's `dwc.buttonType` client property and look up `Button.{variant}.borderColor` from UIManager. Fall back to `Button.borderColor` for the default variant.

## Open Questions

1. **Should gray variant be included?**
   - What we know: The DWC SCSS generates themes for default, primary, success, warning, danger, info, AND gray (7 variants total).
   - What's unclear: The user only mentioned success, danger, warning, info (4 new variants). Gray was not mentioned.
   - Recommendation: Include gray if trivial (same pattern), but focus on the 4 requested variants. The implementation pattern is identical for all.

2. **Theme switching UX in demo**
   - What we know: `dwc.theme` system property + L&F reinstall works. The user has 2 CSS files in `css/`.
   - What's unclear: Should theme switching be in the main gallery or a separate demo?
   - Recommendation: Add a toolbar/header row to the existing DwcComponentGallery with a theme dropdown. All components benefit from seeing the theme change.

3. **DwcButtonBorder variant awareness -- scope of change**
   - What we know: The border currently uses a single global border color.
   - What's unclear: Whether to subclass DwcButtonBorder or modify it in place.
   - Recommendation: Modify DwcButtonBorder in place. It already has access to the component in `paintBorder(Component c, ...)`. Adding a variant lookup is minimal.

## Sources

### Primary (HIGH confidence)
- `/Users/beff/_lab/dwclaf/src/main/resources/com/dwc/laf/themes/default-light.css` - All semantic color tokens verified
- `/Users/beff/_lab/dwclaf/src/main/java/com/dwc/laf/ui/DwcButtonUI.java` - Current variant architecture
- `/Users/beff/_lab/dwclaf/src/main/java/com/dwc/laf/DwcLookAndFeel.java` - initButtonDefaults, initFocusRingColor
- `/Users/beff/_lab/dwclaf/src/main/resources/com/dwc/laf/token-mapping.properties` - Current token mappings
- `/Users/beff/_lab/dwclaf/src/main/java/com/dwc/laf/css/CssThemeLoader.java` - Theme loading mechanism
- `/Users/beff/_lab/dwclaf/src/main/java/com/dwc/laf/ui/DwcButtonBorder.java` - Border color resolution
- `/Users/beff/_lab/dwclaf/dwc/src/components/dwc-button/dwc-button.scss` - DWC button-theme-generator mixin (reference implementation)
- `/Users/beff/_lab/dwclaf/dwc/src/components/dwc-button/test/themes/index.html` - All 7 DWC button themes listed
- `/Users/beff/_lab/dwclaf/css/theme1.css` - Sample DWC Themer output (`:root` not used, uses `html[data-app-theme]` selector)
- `/Users/beff/_lab/dwclaf/css/theme2.css` - Sample DWC Themer output (uses `:root` selector)

### Secondary (HIGH confidence -- same codebase)
- `/Users/beff/_lab/dwclaf/src/main/java/com/dwc/laf/painting/StateColorResolver.java` - State priority chain
- `/Users/beff/_lab/dwclaf/src/main/java/com/dwc/laf/css/CssTokenParser.java` - Handles arbitrary selector blocks
- `/Users/beff/_lab/dwclaf/src/main/java/com/dwc/laf/DwcComponentGallery.java` - Current gallery structure
- `/Users/beff/_lab/dwclaf/src/test/java/com/dwc/laf/ui/DwcButtonUITest.java` - Existing test patterns

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - all components are internal, directly examined
- Architecture: HIGH - pattern derived from existing code + DWC SCSS reference implementation
- Pitfalls: HIGH - identified from direct code analysis of border, focus ring, and naming conventions
- Token mapping: HIGH - verified all CSS tokens exist in default-light.css with correct semantic names

**Research date:** 2026-02-10
**Valid until:** indefinite (internal codebase, no external dependencies)
