# Phase 12: More Rendering Detail to Match DWC - Research

**Researched:** 2026-02-11
**Domain:** Swing Look & Feel rendering fidelity -- font matching, button colors, ComboBox rework, TextField flatness, ProgressBar rounding, border crispness, typography accuracy
**Confidence:** HIGH (all findings derived from direct source code and CSS token analysis)

## Summary

Phase 12 closes remaining visual rendering gaps between the 13 existing Swing component delegates and their DWC web counterparts. Research reveals a critical token-mapping collision that causes the default button's foreground color to be overwritten by the primary button's foreground (both map to `Button.foreground`, and the primary variant's CSS block comes later in the flattened token map). This is the root cause of the black/wrong-color text on the default button variant.

Font analysis shows the current `--dwc-font-size` resolves through `var(--dwc-font-size-s)` = `0.875rem` = 14px. The DWC button uses `--dwc-button-font-weight: var(--dwc-font-weight-semibold)` = 500, which currently maps to `Button.font.style` but the existing font weight mapping only distinguishes BOLD (>=600) from PLAIN (<600), so 500 becomes PLAIN. This is correct for body text but wrong for button text which should appear semi-bold. Since Java Font only supports PLAIN and BOLD, the nearest achievable fidelity is to map the button font-weight of 500 to BOLD since it is a heavier weight intended to stand out from body text.

The ProgressBar already has correct track rounding from Phase 11, but needs contrast-aware text color switching for the percentage string. The TextField "sunken" appearance stems from the `--dwc-input-background` color being slightly darker than pure white (it maps to `--dwc-color-default-light` = `hsl(211, 38%, 95%)` which is a tinted off-white). This is intentional in DWC but may need the border approach adjusted to remove any remaining 3D appearance. The ComboBox needs comprehensive visual rework across border, background, separator, and arrow rendering.

**Primary recommendation:** Fix the `--dwc-button-color` token-mapping collision first (it affects the most visible component), then address font weight for buttons, button border colors for colored variants, ProgressBar text contrast, and finally the ComboBox full rework as the largest single change.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **Font family:** Use closest available system font to match DWC's web font stack. Claude derives the correct system font mapping from DWC's CSS font-family declarations. Do NOT bundle a .ttf file -- use platform-native fonts.
- **Font size and weight:** Both size and weight are off compared to DWC -- need correction. Claude should audit all font token mappings against DWC CSS and fix mismatches. Section headers, button text, label text, and body text all need review.
- **Button text color:** Default variant button has black text where DWC renders white text on blue background. All colored variant buttons should have white text matching DWC. Claude to audit foreground color tokens for every button variant and fix.
- **Button borders:** Colored variant buttons should have barely-visible or same-color borders matching DWC. Current Swing borders are too prominent on colored variants. Match DWC exactly -- derive border colors from DWC tokens.
- **ComboBox rendering (full rework):** Everything needs rework: border, background, arrow area, separator, overall crispness. Current rendering looks blurry and substantially different from DWC. Target: thin crisp border, flat background, small chevron, subtle separator line. Claude has discretion on implementation approach but must match DWC visual closely.
- **TextField appearance:** Must be fully flat -- zero inset/shadow/3D effect. Thin border only, no sunken look whatsoever. Current Swing rendering has a visible sunken/inset appearance that DWC does not have.
- **ProgressBar track rounding:** DWC rounds both the colored fill AND the gray track (the 100% background container). Current Swing only rounds the fill bar. Track (background container) must also have rounded ends matching DWC.
- **Border thickness and crispness:** High priority -- DWC borders are very thin and crisp (1px). Swing TextField and ComboBox borders appear thicker or less crisp. Claude to audit border rendering for 1px precision at all scale factors.
- **ProgressBar text contrast:** DWC dynamically switches percentage text color (dark/white) based on fill color contrast. Swing should do the same -- use white text on dark fills, dark text on light fills. Claude to implement contrast-aware text color selection.

### Claude's Discretion
- Exact system font selection per platform (Mac/Windows/Linux)
- ComboBox implementation approach (within constraint of matching DWC visuals)
- Border rendering technique for 1px crispness (stroke alignment, subpixel handling)
- ProgressBar contrast threshold for text color switching
- Any shadow parameter tuning (panels were explicitly skipped in discussion)

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope
</user_constraints>

## Architecture Patterns

### Existing Project Structure (no changes needed)
```
src/main/java/com/dwc/laf/
  css/         # CSS parsing (CssTokenParser, CssVariableResolver, etc.)
  defaults/    # UIDefaults population (TokenMappingConfig, UIDefaultsPopulator)
  painting/    # Shared paint utilities (PaintUtils, FocusRingPainter, etc.)
  ui/          # Component UI delegates (DwcButtonUI, DwcComboBoxUI, etc.)
  DwcLookAndFeel.java  # Main L&F entry point
src/main/resources/com/dwc/laf/
  themes/default-light.css   # Bundled CSS theme
  token-mapping.properties   # CSS token -> UIDefaults key mapping
```

### Pattern 1: Token Collision Resolution
**What:** When the CSS parser flattens all blocks (`:root`, `.dwc-button`, `.dwc-button[theme='primary']`), later blocks override earlier ones for the same property name. If two CSS tokens both map to the same UIDefaults key, the later mapping entry wins.
**Current state:** `--dwc-color-on-default-text` and `--dwc-button-color` both map to `Button.foreground`. The `.dwc-button[theme='primary']` block overrides `--dwc-button-color` from dark blue to white. Since `--dwc-button-color` mapping comes after `--dwc-color-on-default-text` in token-mapping.properties, `Button.foreground` ends up as white (wrong for default button).
**Fix:** Remove the `--dwc-button-color = color:Button.foreground` mapping entirely. The default button foreground should come solely from `--dwc-color-on-default-text`. The primary button foreground is already correctly mapped via `--dwc-color-on-primary-text = color:Button.default.foreground`.

### Pattern 2: Per-Variant Border Color Matching
**What:** DWC colored buttons (primary, success, danger, warning, info) use the same color for both background and border, making the border nearly invisible. The default button uses `--dwc-color-default` for both background and border.
**Current state:** The token-mapping already has `--dwc-border-color-success`, `--dwc-border-color-danger`, etc. mapping to `Button.{variant}.borderColor`. These resolve to the variant's accent color (same as background). The `DwcButtonBorder.resolveBorderColor()` method already looks up `Button.{variant}.borderColor`.
**Fix:** Verify that variant border colors match the variant background colors. The CSS sets `--dwc-border-color-success: var(--dwc-color-success)` which is the same as the button background, creating the "barely visible" effect the user wants.

### Pattern 3: Font Weight Mapping for Button Text
**What:** DWC buttons use `--dwc-button-font-weight: var(--dwc-font-weight-semibold)` = 500. Java Font only supports PLAIN (0) and BOLD (1).
**Current state:** The existing mapping `--dwc-button-font-weight = int:Button.font.style` converts 500 to an int but it's never used to derive the actual button font (the button font comes from the default font set in `initDefaultFont()`). The default font weight `--dwc-font-weight: var(--dwc-font-weight-normal)` = 400 maps correctly to PLAIN.
**Fix:** Create a separate button font with BOLD style in `initButtonDefaults()` by deriving from the default font with `deriveFont(Font.BOLD)`. This gives buttons the heavier weight that DWC achieves with font-weight: 500. Apply the same approach for any other components that use `--dwc-font-weight-semibold`.

### Pattern 4: Contrast-Aware Text Color for ProgressBar
**What:** DWC dynamically switches percentage text between dark and white based on the fill color's relative luminance.
**Current state:** `DwcProgressBarUI.paintString()` always uses `pb.getForeground()` which is a single static color.
**Fix:** Calculate relative luminance of the fill color using the W3C formula `L = 0.2126*R + 0.7152*G + 0.0722*B` (where R,G,B are linearized sRGB). If L > 0.5, use dark text; otherwise use white text. This matches the DWC pattern of contrast-based text color selection. The threshold of 0.5 is a good starting point; the WCAG recommendation for contrast ratio could also be used but 0.5 luminance threshold achieves the visual effect the user described.

### Pattern 5: PaintOutline Even-Odd Fill for 1px Borders
**What:** The existing `PaintUtils.paintOutline()` uses the even-odd fill rule to create a precise ring between an outer and inner rounded rectangle. This is already the correct technique for 1px crisp borders.
**Current state:** Both `DwcTextFieldBorder` and `DwcButtonBorder` use this approach. The `Component.borderWidth` token maps from `--dwc-border-width: 1px` which gives `borderWidth=1`.
**Analysis:** The even-odd fill rule produces exact 1px outlines at all scale factors because it fills the mathematical area between two shapes rather than stroking, which avoids subpixel rendering artifacts. The `RenderingHints.VALUE_STROKE_NORMALIZE` hint (set by `PaintUtils.setupPaintingHints()`) further helps crisp rendering. If borders still appear thick or blurry, the issue is likely the arc radius being too large relative to the component size, causing the rounded corners to consume more visual space.

### Anti-Patterns to Avoid
- **Using Graphics2D.drawRoundRect() for borders:** Produces fuzzy borders due to stroke centering on pixel boundaries. Always use the even-odd fill approach via `PaintUtils.paintOutline()`.
- **Mapping component-scoped CSS tokens that clash with variant-scoped overrides:** The CSS parser flattens all blocks. If `.dwc-button` sets `--dwc-button-color` and `.dwc-button[theme='primary']` also sets it, the primary value wins. Don't map such tokens to non-variant-specific UIDefaults keys.
- **Using Java Font.ITALIC for semi-bold:** Java Font has no semi-bold concept. Map font-weight >= 500 to BOLD for components where DWC uses semi-bold (buttons, labels).

## Detailed Findings

### Finding 1: Button Foreground Color Root Cause (CRITICAL)

**Confidence: HIGH** (verified by tracing the full token resolution chain)

The CSS file has two blocks that set `--dwc-button-color`:
1. `.dwc-button { --dwc-button-color: var(--dwc-color-primary-text); }` (line 675) -- resolves to `hsl(211, 100%, 35%)` = dark blue
2. `.dwc-button[theme='primary'] { --dwc-button-color: var(--dwc-color-on-primary-text); }` (line 691) -- resolves to `hsl(0, 0%, 100%)` = white

The parser flattens these by document order, so `--dwc-button-color` = white in the token map.

The token-mapping.properties file has:
- Line 17: `--dwc-color-on-default-text = color:Button.foreground, ...` -- maps default text (black) to `Button.foreground`
- Line 44: `--dwc-button-color = color:Button.foreground` -- maps button-specific color to `Button.foreground`

Since line 44 is processed after line 17, `Button.foreground` = white (from the flattened `--dwc-button-color`).

**Impact:** Default buttons get white text on a light gray background -- nearly invisible. This is THE most visible rendering mismatch.

**Fix approach:**
1. Remove `color:Button.foreground` from the `--dwc-button-color` mapping line in token-mapping.properties.
2. `Button.foreground` will then come from `--dwc-color-on-default-text` only, which resolves to `var(--dwc-color-default-text-90)` = `hsl(0, 0%, 0%)` = black. This is correct for the default variant.
3. The primary button foreground already comes from `--dwc-color-on-primary-text = color:Button.default.foreground` which = white. Correct.
4. For success/danger/warning/info, the mappings `--dwc-color-on-{variant}-text = color:Button.{variant}.foreground` are already correct (white text on colored backgrounds).

### Finding 2: Button Border Color for Colored Variants

**Confidence: HIGH** (verified from CSS and token-mapping)

DWC CSS for the primary button: `--dwc-button-border-color: var(--dwc-button-background)` (line 690). The border is the SAME color as the background, making it invisible.

The token-mapping has `--dwc-border-color-{variant} = color:Button.{variant}.borderColor` for each variant. These map from `--dwc-border-color-success: var(--dwc-color-success)` etc. The variant button background comes from `--dwc-color-success = color:Button.success.background`. So `Button.success.borderColor` = `--dwc-color-success` and `Button.success.background` = `--dwc-color-success` -- they are the same color. This is correct.

For the default button: `--dwc-button-border-color = color:Button.borderColor` (line 47). In the CSS, `.dwc-button { --dwc-button-border-color: var(--dwc-color-default) }` (line 674). But `.dwc-button[theme='primary']` also sets `--dwc-button-border-color: var(--dwc-button-background)` (line 690). Due to flattening, `--dwc-button-border-color` = `var(--dwc-button-background)` (primary's value). And `--dwc-button-background` = `var(--dwc-color-primary)` (also from primary block, line 689). So `Button.borderColor` = primary blue, not default gray.

**Impact:** The default button border renders as bright blue instead of the subtle gray it should be.

**Fix approach:** Similar to Finding 1 -- remove the `--dwc-button-border-color = color:Button.borderColor` mapping. Instead, use the existing `--dwc-color-default = color:Button.background` and set `Button.borderColor` from `--dwc-color-default-dark` (or `--dwc-color-default` itself) which is the correct DWC default button border color. The cleanest approach: add a direct mapping `--dwc-color-default = color:Button.borderColor` (since DWC default button border = `var(--dwc-color-default)`).

But wait -- the flattened `--dwc-button-border-color` token value is already wrong. The fix is to stop using `--dwc-button-border-color` for the default variant entirely. Replace with:
- `--dwc-color-default` mapping should include `color:Button.borderColor`

### Finding 3: Font Size and Weight Audit

**Confidence: HIGH** (verified from CSS values and UIDefaultsPopulator conversion logic)

Current font configuration:
- `--dwc-font-size: var(--dwc-font-size-s)` = `0.875rem` = 0.875 * 16px = **14px** -- correct
- `--dwc-font-weight: var(--dwc-font-weight-normal)` = **400** -> Font.PLAIN -- correct for body text
- `--dwc-font-family: var(--dwc-font-family-sans)` = `-apple-system, BlinkMacSystemFont, 'Roboto', 'Segoe UI', ...`

Font family resolution (existing `resolveFontFamily()` in `DwcLookAndFeel.java`):
- Mac: `-apple-system` -> `.AppleSystemUIFont` (via `mapPlatformAlias`) -- correct
- Windows: `Segoe UI` -> `Segoe UI` (via `mapPlatformAlias`) -- correct
- Linux: Falls through to `Helvetica` or `Arial` or `sans-serif` -> `SansSerif` -- correct

DWC button font weight: `--dwc-button-font-weight: var(--dwc-font-weight-semibold)` = **500**. Java Font has no semi-bold. Current mapping `--dwc-button-font-weight = int:Button.font.style` puts integer 500 into UIDefaults as `Button.font.style`, but this key is never read by `DwcButtonUI` -- the button uses the default font set by `initDefaultFont()`.

**Fix approach for button font weight:**
In `initButtonDefaults()`, derive a bold font from the default font and set it as `Button.font`:
```java
FontUIResource defaultFont = (FontUIResource) table.get("defaultFont");
if (defaultFont != null) {
    table.put("Button.font", new FontUIResource(
        defaultFont.getFamily(), Font.BOLD, defaultFont.getSize()));
}
```

The DWC input (TextField) font weight is also `--dwc-input-font-weight: var(--dwc-font-weight-semibold)` = 500. Apply the same bold font derivation in `initTextFieldDefaults()`.

### Finding 4: TextField "Sunken" Appearance

**Confidence: HIGH** (verified from Phase 11 decision and CSS analysis)

Phase 11 determined: "TextField border already flat via PaintUtils.paintOutline even-odd fill; '3D' appearance is intentional DWC input-background tint."

The input background color is `--dwc-input-background: var(--dwc-color-default-light)` which resolves to `var(--dwc-color-default-95)` = `hsl(211, 38%, 95%)`. This is a very slightly blue-tinted off-white (#ECEEF4 approximately). Against a pure white or #FFFFFF panel background, this tinted background creates a subtle contrast that can appear as a "sunken" or inset look.

The user requirement says "must be fully flat -- zero inset/shadow/3D effect." Since the border is already 1px flat (even-odd fill), the remaining sunken appearance can only come from:
1. The background color contrast between TextField and its parent Panel
2. Or the border color being too dark/thick

The DWC web rendering also has this slight background tint, so it should look the same. However, if the Swing rendering appears more sunken than web, it could be due to:
- The border width appearing >1px due to antialiasing or HiDPI scaling
- The VALUE_STROKE_NORMALIZE hint potentially rounding up subpixel borders

**Fix approach:** Audit the actual rendered border width. If it appears thicker than 1 CSS pixel, consider using `VALUE_STROKE_PURE` instead of `VALUE_STROKE_NORMALIZE` for the border painting to get exact fractional-pixel rendering. Alternatively, if the issue is purely the background color, changing `TextField.background` to pure white would flatten it completely (but would diverge from DWC's actual CSS).

### Finding 5: ProgressBar Track Rounding

**Confidence: HIGH** (verified from source code)

Phase 11 already fixed the fill bar rounding (`ProgressBar.arc` mapped from `--dwc-border-radius-xl` = 12px). Looking at `DwcProgressBarUI.paintDeterminateContent()`:

```java
Shape trackShape = PaintUtils.createRoundedShape(x, y, width, height, arc);
g2.setColor(background);
g2.fill(trackShape);
```

The track IS already painted with the rounded shape. The `arc=12` for a 12px-high bar clamps to `min(12, 12) = 12`, creating fully rounded pill ends. This finding contradicts the CONTEXT claim "Current Swing only rounds the fill bar."

**Re-analysis:** Both track AND fill are already rounded. If the user still sees flat track ends, it may be that the ProgressBar height is larger than expected, making the rounding less visible. At the default preferred height of 8px, `arc=12` clamps to `min(12, 8)=8`, which gives full rounding. The code appears correct.

**Action:** Verify visually and confirm no code change needed for track rounding. The CONTEXT item may be resolved by Phase 11's changes.

### Finding 6: ProgressBar Text Contrast

**Confidence: HIGH** (algorithm is well-established)

Current `DwcProgressBarUI.paintString()`:
```java
g2.setColor(pb.getForeground());
```

This uses a single static color for all text regardless of the fill color underneath.

**Fix approach:** Implement luminance-based text color switching:
```java
private Color contrastTextColor(Color fillColor) {
    // Linearize sRGB components
    double r = linearize(fillColor.getRed() / 255.0);
    double g = linearize(fillColor.getGreen() / 255.0);
    double b = linearize(fillColor.getBlue() / 255.0);
    // W3C relative luminance
    double luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;
    return luminance > 0.4 ? Color.BLACK : Color.WHITE;
}

private double linearize(double srgb) {
    return srgb <= 0.04045
        ? srgb / 12.92
        : Math.pow((srgb + 0.055) / 1.055, 2.4);
}
```

The threshold of 0.4 (slightly below mid-grey) ensures white text on medium-dark fills and black text on light fills, matching DWC's visual behavior. This is a discretion area -- the exact threshold can be tuned.

For the text positioning: the current implementation centers text on the entire bar. DWC may split the text color at the fill boundary (different color for the portion over the fill vs over the empty track). However, implementing split-color text is significantly more complex (requires clipping regions). Start with a single color based on the dominant area (if fill > 50%, use fill color for contrast; otherwise use track color for contrast), and iterate if needed.

### Finding 7: ComboBox Full Rework

**Confidence: HIGH** (verified from existing DwcComboBoxUI source)

The existing ComboBox implementation uses:
1. `DwcTextFieldBorder` for the outer border (shared with TextField)
2. `DwcComboBoxArrowButton` inner class for the arrow area with separator
3. `DwcComboBoxRenderer` for popup list items

Phase 11 already reworked the arrow button with a separator line and compact chevron. The remaining issues described in CONTEXT are:
- **Overall crispness:** Same border rendering as TextField (even-odd fill). See Finding 4.
- **Background:** Uses `--dwc-input-background` same as TextField. Same tint issue.
- **Border:** Uses `DwcTextFieldBorder` which is already flat 1px.
- **Arrow area:** Already reworked in Phase 11 with separator and small chevron.

**Assessment:** The ComboBox "full rework" requested in CONTEXT may be substantially addressed by the border crispness improvements, font weight correction, and the Phase 11 arrow rework. The remaining visual gap is likely:
1. Font weight (text inside ComboBox should be semi-bold like buttons/inputs per DWC)
2. Border crispness (same as TextField -- see Finding 4)
3. Arrow chevron may need size/position tuning to match DWC exactly

**Fix approach:**
1. Apply the same font weight fix (BOLD derivation) to ComboBox font
2. Audit the overall ComboBox dimensions against DWC (height should be 36px = `--dwc-size-m`)
3. Fine-tune the arrow button size and chevron proportions
4. Ensure the separator line color and position match DWC's `[part='suffix-separator']` exactly

### Finding 8: Border Crispness Audit

**Confidence: MEDIUM** (the rendering approach is correct; visual sharpness depends on runtime factors)

The even-odd fill technique in `PaintUtils.paintOutline()` is the correct approach for crisp borders. The key rendering hints are:
- `VALUE_ANTIALIAS_ON` -- necessary for smooth rounded corners but can soften straight edges
- `VALUE_STROKE_NORMALIZE` -- normalizes strokes to pixel boundaries for crispness

For a 1px border on a non-HiDPI display, `VALUE_STROKE_NORMALIZE` should align the border to pixel grid. On HiDPI (2x), the Java2D transform scales everything by 2x, so 1 logical pixel = 2 device pixels, which is crisp.

Potential issue: the even-odd fill path is filled, not stroked, so `STROKE_NORMALIZE` may not directly apply to it. The fill is governed by `ANTIALIAS_ON`, which can cause 1px filled shapes to appear slightly soft due to coverage-based anti-aliasing.

**Fix approach for maximum crispness:**
1. For straight edges (non-rounded parts of the border), ensure coordinates align to pixel boundaries (use integer coordinates, not fractional).
2. Consider temporarily disabling anti-aliasing for the outline fill when the arc is 0 (rectangular borders).
3. Test with `VALUE_STROKE_PURE` instead of `VALUE_STROKE_NORMALIZE` to see if it improves the visual result.
4. The most reliable approach: ensure all paint coordinates in `PaintUtils.paintOutline()` are snapped to half-pixel boundaries (x + 0.5, y + 0.5) which centers the fill on the pixel grid. However, the even-odd fill approach should already produce sharp 1px results since it fills an exact 1px-wide mathematical area.

## Code Examples

### Example 1: Fix Button Foreground Collision (token-mapping.properties)

```properties
# BEFORE (broken):
--dwc-button-color = color:Button.foreground

# AFTER (fixed -- remove Button.foreground from this line):
# --dwc-button-color mapping removed; default button foreground comes from
# --dwc-color-on-default-text which already maps to Button.foreground
```

### Example 2: Button Font Weight Derivation (DwcLookAndFeel.java)

```java
private void initButtonDefaults(UIDefaults table) {
    // ... existing code ...

    // Derive bold font for buttons (DWC uses font-weight: 500 semibold)
    FontUIResource defaultFont = (FontUIResource) table.get("defaultFont");
    if (defaultFont != null) {
        FontUIResource buttonFont = new FontUIResource(
            defaultFont.getFamily(), Font.BOLD, defaultFont.getSize());
        table.put("Button.font", buttonFont);
    }
}
```

### Example 3: ProgressBar Contrast Text Color (DwcProgressBarUI.java)

```java
private void paintString(Graphics2D g2, JProgressBar pb,
                          int x, int y, int width, int height, int amountFull) {
    String text = pb.getString();
    if (text == null || text.isEmpty()) return;

    FontMetrics fm = g2.getFontMetrics(pb.getFont());
    int textWidth = fm.stringWidth(text);
    int textX = x + (width - textWidth) / 2;
    int textY = y + (height - fm.getHeight()) / 2 + fm.getAscent();

    g2.setFont(pb.getFont());

    // Contrast-aware text color
    Color fillColor = resolveVariantColor(pb);
    boolean fillDominant = (pb.getOrientation() == JProgressBar.HORIZONTAL)
        ? amountFull > width / 2
        : amountFull > height / 2;
    Color bgForContrast = fillDominant ? fillColor : background;
    g2.setColor(contrastTextColor(bgForContrast));

    g2.drawString(text, textX, textY);
}

private Color contrastTextColor(Color bg) {
    double r = linearize(bg.getRed() / 255.0);
    double g = linearize(bg.getGreen() / 255.0);
    double b = linearize(bg.getBlue() / 255.0);
    double luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;
    return luminance > 0.4 ? Color.BLACK : Color.WHITE;
}

private double linearize(double srgb) {
    return srgb <= 0.04045 ? srgb / 12.92
        : Math.pow((srgb + 0.055) / 1.055, 2.4);
}
```

### Example 4: Default Button Border Color Fix (token-mapping.properties)

```properties
# BEFORE (broken -- flattened --dwc-button-border-color = primary blue):
--dwc-button-border-color = color:Button.borderColor

# AFTER: Add Button.borderColor to --dwc-color-default mapping
--dwc-color-default = color:Button.background, color:ToggleButton.background, ..., color:Button.borderColor
# And remove the --dwc-button-border-color line entirely
```

## Common Pitfalls

### Pitfall 1: CSS Flattening Collisions
**What goes wrong:** Component-scoped CSS tokens (`.dwc-button`) and variant-scoped tokens (`.dwc-button[theme='primary']`) both declare the same custom property name. The parser flattens them by document order, so the last one wins. If the token-mapping maps this flattened token to a non-variant UIDefaults key, the variant value overwrites the default value.
**Why it happens:** The CSS parser intentionally flattens for simplicity -- it doesn't track selector scope. This is fine for `:root`-only tokens but breaks for multi-scope properties.
**How to avoid:** Never map a component-scoped CSS token (like `--dwc-button-color`) to a "default variant" UIDefaults key (like `Button.foreground`) when the same CSS token is also set in a variant block. Instead, map the semantic color aliases (`--dwc-color-on-default-text`, `--dwc-color-on-primary-text`) which are `:root`-only and don't have variant overrides.
**Warning signs:** A UIDefaults color that should be different per variant has the same (wrong) value for all variants.

### Pitfall 2: Java Font Semi-Bold Limitation
**What goes wrong:** Attempting to set font-weight: 500 (semi-bold) produces PLAIN text because the threshold (>=600 for BOLD) excludes 500.
**Why it happens:** Java Font only supports PLAIN and BOLD. There's no semi-bold, medium, or other weight gradations.
**How to avoid:** For components where DWC uses semi-bold (500), use Font.BOLD in Java. It will be slightly heavier than DWC's 500 but much closer than PLAIN. The visual difference between 500 and 700 on screen at 14px is subtle.
**Warning signs:** Button text, label text, or input text appears too light/thin compared to DWC web rendering.

### Pitfall 3: Even-Odd Fill Border Softness
**What goes wrong:** 1px borders painted with anti-aliased even-odd fill can appear slightly soft or wider than 1px due to subpixel coverage.
**Why it happens:** Anti-aliasing spreads pixel coverage to neighboring pixels. For a 1px filled area, adjacent pixels get partial coverage, creating a softer appearance.
**How to avoid:** Ensure border coordinates are on integer pixel boundaries. The even-odd fill between `(x, y, w, h)` and `(x+1, y+1, w-2, h-2)` where x,y are integers will produce the sharpest result. Avoid fractional coordinates for the outline path.
**Warning signs:** Borders appear 2px or fuzzy compared to 1px DWC web borders.

### Pitfall 4: Modifying Component-Scoped CSS Tokens
**What goes wrong:** Adding new component-scoped CSS tokens to default-light.css (inside `.dwc-button {}` etc.) without understanding the flattening behavior causes tokens to resolve to the last block's value.
**Why it happens:** The CSS parser treats all blocks equally and flattens by document order.
**How to avoid:** Only use `:root` tokens for mappings, or use component-scoped tokens that are unique to their block (not overridden by variant blocks).
**Warning signs:** A newly mapped token resolves to an unexpected value that matches a different CSS block.

## Open Questions

1. **TextField Background: Flat White or DWC Tint?**
   - What we know: DWC sets `--dwc-input-background: var(--dwc-color-default-light)` = `hsl(211, 38%, 95%)` which is slightly tinted. This is the DWC-correct value. But the user describes the TextField as "still sunken."
   - What's unclear: Is the user comparing against the DWC web version (which has the same tint) or against an ideal "fully white" TextField? If the DWC web also has the tint, then the Swing rendering is correct.
   - Recommendation: Keep the DWC-correct tinted background initially. If the user still perceives "sunken" appearance, offer to change to pure white as an option. The "sunken" perception may also be resolved by fixing the border crispness.

2. **ComboBox Rework Scope After Phase 11**
   - What we know: Phase 11 already reworked the arrow button with separator line and compact chevron. The CONTEXT says "everything needs rework."
   - What's unclear: How much of the Phase 11 rework addressed the user's concerns. The user may not have seen the Phase 11 result when providing Phase 12 CONTEXT.
   - Recommendation: Focus the ComboBox work on font weight, border crispness, and overall proportions. Don't re-rework the arrow button unless the Phase 11 result is visibly wrong.

3. **Font Weight for Labels and Body Text**
   - What we know: The CONTEXT says "Section headers, button text, label text, and body text all need review." Body text weight is 400=PLAIN (correct). DWC label weight is inherited from `--dwc-font-weight` = 400.
   - What's unclear: Whether "label text" means JLabel (which should be 400/PLAIN) or form field labels (which in DWC use `--dwc-input-label-font-weight: var(--dwc-font-weight-semibold)` = 500). JLabel is generic and should match body text.
   - Recommendation: Keep JLabel at PLAIN weight. Only apply BOLD to Button and TextField (and ComboBox) where DWC explicitly sets semibold.

## Sources

### Primary (HIGH confidence)
- `/Users/beff/_lab/dwclaf/src/main/resources/com/dwc/laf/themes/default-light.css` -- full DWC CSS token values
- `/Users/beff/_lab/dwclaf/src/main/resources/com/dwc/laf/token-mapping.properties` -- all token-to-UIDefaults mappings
- `/Users/beff/_lab/dwclaf/src/main/java/com/dwc/laf/DwcLookAndFeel.java` -- L&F initialization code
- `/Users/beff/_lab/dwclaf/src/main/java/com/dwc/laf/ui/DwcButtonUI.java` -- button rendering logic
- `/Users/beff/_lab/dwclaf/src/main/java/com/dwc/laf/ui/DwcButtonBorder.java` -- button border rendering
- `/Users/beff/_lab/dwclaf/src/main/java/com/dwc/laf/ui/DwcComboBoxUI.java` -- combobox rendering logic
- `/Users/beff/_lab/dwclaf/src/main/java/com/dwc/laf/ui/DwcTextFieldUI.java` -- textfield rendering logic
- `/Users/beff/_lab/dwclaf/src/main/java/com/dwc/laf/ui/DwcTextFieldBorder.java` -- textfield border rendering
- `/Users/beff/_lab/dwclaf/src/main/java/com/dwc/laf/ui/DwcProgressBarUI.java` -- progressbar rendering
- `/Users/beff/_lab/dwclaf/src/main/java/com/dwc/laf/painting/PaintUtils.java` -- shared painting utilities
- `/Users/beff/_lab/dwclaf/src/main/java/com/dwc/laf/defaults/UIDefaultsPopulator.java` -- token conversion logic
- `/Users/beff/_lab/dwclaf/src/main/java/com/dwc/laf/css/CssTokenParser.java` -- CSS flattening behavior

### Secondary (MEDIUM confidence)
- W3C relative luminance formula for contrast calculation: `L = 0.2126*R + 0.7152*G + 0.0722*B` with sRGB linearization -- standard, well-documented

## Metadata

**Confidence breakdown:**
- Token collision analysis: HIGH -- fully traced through source code, CSS, and mapping files
- Font weight mapping: HIGH -- verified from CSS values and Java Font API constraints
- ProgressBar contrast: HIGH -- standard W3C luminance algorithm
- Border crispness: MEDIUM -- rendering approach is correct; visual sharpness is runtime-dependent
- ComboBox rework scope: MEDIUM -- depends on user assessment of Phase 11 results
- TextField flatness: MEDIUM -- the "sunken" appearance may be subjective or resolved by border fix

**Research date:** 2026-02-11
**Valid until:** 2026-03-11 (stable codebase, no external dependency changes expected)
