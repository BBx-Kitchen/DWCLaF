# Phase 3: Shared Painting Utilities - Research

**Researched:** 2026-02-10
**Domain:** Java2D painting utilities for Swing Look & Feel (antialiasing, rounded corners, focus rings, box shadows, HiDPI, state color resolution)
**Confidence:** HIGH

## Summary

This phase builds the shared painting utility layer that all future ComponentUI delegates (Phases 4-7) will consume. The domain is well-understood: Java2D's `Graphics2D` API with `RenderingHints`, `RoundRectangle2D.Float`, `Path2D`, `AlphaComposite`, `ConvolveOp`, and `BufferedImage` caching for shadows. FlatLaf's source code provides an excellent, production-proven reference architecture for exactly these utilities.

The key technical challenges are: (1) correctly implementing CSS `box-shadow` semantics (multi-layer, blur + spread + offset) in Java2D with performant caching, (2) painting focus rings outside component bounds (requiring awareness of insets and clip management), and (3) ensuring all float-coordinate painting works correctly on HiDPI displays where the Graphics2D transform includes a 2x scale. The DWC CSS tokens define specific values for focus ring width (3px), opacity (0.4), lightness (45%), and multi-layer shadow specifications that must be translated into Java2D painting operations.

The state color resolver is a simpler problem with a clear priority chain (disabled > pressed > hover > focused > enabled) modeled directly after FlatLaf's `buttonStateColor()` pattern. All utilities are pure Java with zero external dependencies, using only `java.awt` and `java.awt.image` classes.

**Primary recommendation:** Build 5 utility classes in `com.dwc.laf.painting` package -- `PaintUtils` (antialiasing + rounded rects), `FocusRingPainter`, `ShadowPainter`, `HiDpiUtils`, and `StateColorResolver` -- following FlatLaf's architecture as a reference but adapted for DWC's CSS token model.

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `java.awt.Graphics2D` | JDK 21 | All painting operations | Core Java2D rendering API; the only way to paint in Swing |
| `java.awt.RenderingHints` | JDK 21 | Antialiasing, stroke control, text rendering quality | Standard mechanism for rendering quality in Java2D |
| `java.awt.geom.RoundRectangle2D.Float` | JDK 21 | Rounded corner shapes | Built-in float-precision rounded rectangle |
| `java.awt.geom.Path2D.Float` | JDK 21 | Complex shapes, per-corner radii, outlines via even-odd fill | Necessary for shapes RoundRectangle2D cannot express |
| `java.awt.image.BufferedImage` | JDK 21 | Shadow image caching, off-screen painting | Standard off-screen image buffer for caching |
| `java.awt.image.ConvolveOp` | JDK 21 | Gaussian blur for shadow rendering | JDK-provided convolution operator |
| `java.awt.image.Kernel` | JDK 21 | Blur kernel definition | Companion to ConvolveOp |
| `java.awt.AlphaComposite` | JDK 21 | Opacity/transparency control for disabled states, shadows | Standard alpha blending API |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `java.awt.geom.Ellipse2D.Float` | JDK 21 | Circular shapes | When arc >= min(width, height) and width == height |
| `java.awt.geom.AffineTransform` | JDK 21 | HiDPI scale detection | Extracting scale factor from Graphics2D transform |
| `java.awt.BasicStroke` | JDK 21 | Border stroke rendering | Drawing component borders with specific width |
| `java.lang.ref.SoftReference` | JDK 21 | Shadow image cache entries | Memory-sensitive caching that GC can reclaim under pressure |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `ConvolveOp` Gaussian blur | Multi-rect approximation (stacked transparent rounded rects) | Simpler code, no caching needed, but less accurate shadow; acceptable for prototyping |
| `ConvolveOp` single 2D kernel | Two-pass separable blur (horizontal then vertical) | Better performance for large blur radii (O(2N) vs O(N^2) per pixel), but more complex setup |
| `RoundRectangle2D` | `Path2D` with cubic bezier arcs | Needed only if per-corner different radii are required; RoundRectangle2D handles uniform radii |
| Custom shadow caching | awt-painter library (MIT, archived) | Zero-dep constraint rules out runtime deps; reference its approach instead |

## Architecture Patterns

### Recommended Project Structure

```
src/main/java/com/dwc/laf/
  painting/
    PaintUtils.java          # Static utility: antialiasing setup, rounded rect creation, outline painting
    FocusRingPainter.java    # Focus ring painting outside component bounds
    ShadowPainter.java       # Box shadow rendering with cached blurred images
    HiDpiUtils.java          # HiDPI scale detection and scale-aware painting
    StateColorResolver.java  # Picks correct color for component state
```

### Pattern 1: Static Utility Methods with Graphics State Save/Restore

**What:** All painting utilities are stateless classes with static methods that accept a `Graphics2D` and return it to its original state.
**When to use:** Every painting utility method.
**Why:** Swing reuses Graphics objects across paint calls; corrupting state causes cascading visual bugs.

```java
// Pattern: save hints, set ours, paint, restore
public static Object[] setupAntialiasing(Graphics2D g) {
    Object[] oldHints = {
        g.getRenderingHint(RenderingHints.KEY_ANTIALIASING),
        g.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL)
    };
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                       RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                       RenderingHints.VALUE_STROKE_NORMALIZE);
    return oldHints;
}

public static void restoreAntialiasing(Graphics2D g, Object[] oldHints) {
    if (oldHints[0] != null)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldHints[0]);
    if (oldHints[1] != null)
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, oldHints[1]);
}
```

### Pattern 2: Shape Factory with Smart Degeneration

**What:** A single `createRoundedShape()` factory that degrades gracefully: arc=0 returns `Rectangle2D`, arc >= min(w,h) on a square returns `Ellipse2D`, otherwise `RoundRectangle2D.Float`.
**When to use:** Everywhere a potentially-rounded shape is needed.
**Why:** Avoids unnecessary bezier computation for simple shapes; prevents arc from exceeding dimensions.

```java
// Source: FlatLaf FlatUIUtils.createComponentRectangle pattern
public static Shape createRoundedShape(float x, float y, float w, float h, float arc) {
    if (arc <= 0) return new Rectangle2D.Float(x, y, w, h);
    float clampedArc = Math.min(arc, Math.min(w, h));
    if (clampedArc >= Math.min(w, h) && w == h)
        return new Ellipse2D.Float(x, y, w, h);
    return new RoundRectangle2D.Float(x, y, w, h, clampedArc, clampedArc);
}
```

### Pattern 3: Outline via Even-Odd Fill Rule

**What:** Paint borders/focus rings by creating a `Path2D` with outer and inner shapes using `WIND_EVEN_ODD`, then filling. No `draw()` with stroke needed.
**When to use:** Focus rings, component borders where sub-pixel width matters.
**Why:** `draw()` with `BasicStroke` centers the stroke on the path, causing half to paint inside and half outside. Even-odd fill gives exact pixel control.

```java
// Source: FlatLaf FlatUIUtils.paintOutline pattern
public static void paintOutline(Graphics2D g, float x, float y,
        float w, float h, float lineWidth, float arc) {
    float innerArc = Math.max(arc - lineWidth, 0);
    Path2D path = new Path2D.Float(Path2D.WIND_EVEN_ODD);
    path.append(createRoundedShape(x, y, w, h, arc), false);
    path.append(createRoundedShape(
        x + lineWidth, y + lineWidth,
        w - lineWidth * 2, h - lineWidth * 2, innerArc), false);
    g.fill(path);
}
```

### Pattern 4: Shadow Image Caching with SoftReference

**What:** Render shadow into a `BufferedImage`, cache with `SoftReference` keyed by (width, height, arc, blurRadius, shadowColor). Invalidate on resize.
**When to use:** `ShadowPainter` for box-shadow rendering.
**Why:** Gaussian blur is expensive; components rarely resize, so caching eliminates per-frame blur cost.

```java
// Cache key record (immutable, hashCode/equals via record)
record ShadowCacheKey(int width, int height, float arc, float blurRadius,
                      int shadowColorRgb) {}

// Cache map
private static final Map<ShadowCacheKey, SoftReference<BufferedImage>> cache =
    new ConcurrentHashMap<>();
```

### Pattern 5: State Color Resolution with Priority Chain

**What:** A static method that accepts colors for all states and a component, returning the correct color based on current state.
**When to use:** Every ComponentUI delegate's `paint()` method to select background/foreground color.
**Why:** Centralizes state priority logic; prevents inconsistency across delegates.

```java
// Source: FlatLaf FlatButtonUI.buttonStateColor pattern
public static Color resolve(Component c, Color enabled, Color disabled,
        Color focused, Color hover, Color pressed) {
    if (!c.isEnabled()) return coalesce(disabled, enabled);
    // For AbstractButton, check ButtonModel
    if (c instanceof AbstractButton ab) {
        ButtonModel model = ab.getModel();
        if (model.isPressed()) return coalesce(pressed, enabled);
        if (model.isRollover()) return coalesce(hover, enabled);
    }
    if (c.isFocusOwner()) return coalesce(focused, enabled);
    return enabled;
}

private static Color coalesce(Color primary, Color fallback) {
    return primary != null ? primary : fallback;
}
```

### Anti-Patterns to Avoid

- **Calling `Graphics2D.draw()` for borders/outlines:** Stroke centers on the path boundary, causing half-pixel bleed. Use even-odd fill instead.
- **Using `int` coordinates for all painting:** Causes misalignment on HiDPI. Use `float` coordinates via `Float` variants of shapes.
- **Creating a new `BufferedImage` every paint call for shadows:** Devastates performance. Always cache.
- **Setting `RenderingHints` without restoring:** Corrupts rendering for subsequently-painted components.
- **Ignoring the Graphics2D transform for HiDPI:** The JDK scales Graphics2D automatically since Java 9; painting at "pixel" coordinates means logical pixels, not device pixels. Read the transform to get device scale.
- **Using `setTransform()` to reset scaling:** Overwrites the JDK's HiDPI transform. Use `transform()` (concatenate) instead, and save/restore.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Gaussian blur | Custom pixel-level convolution loop | `ConvolveOp` with `Kernel` (two-pass separable) | JDK's `ConvolveOp` is native-accelerated on many platforms; edge handling via `EDGE_ZERO_FILL` is tricky to get right |
| Rounded rectangle shape | Manual `Path2D` with arc segments for uniform radii | `RoundRectangle2D.Float` | Built-in, handles arc clamping, faster than manual bezier |
| Alpha blending for disabled states | Manual pixel manipulation | `AlphaComposite.getInstance(SRC_OVER, opacity)` | Handles all edge cases including transparent backgrounds |
| HiDPI scale detection | System property parsing (`sun.java2d.uiScale`) | `Graphics2D.getTransform().getScaleX()` | System property is platform-specific and may not exist; transform is always correct |
| Thread-safe shadow cache | Manual synchronization | `ConcurrentHashMap<Key, SoftReference<BufferedImage>>` | Correct without explicit locking; SoftReference allows GC under memory pressure |

**Key insight:** Java2D already solves most of these problems in its standard library. The complexity is in composition (combining blur + shape + offset + cache correctly), not in reimplementing primitives.

## Common Pitfalls

### Pitfall 1: Focus Ring Clipped by Parent Container

**What goes wrong:** Focus ring painted outside component bounds gets clipped by the parent's paint region.
**Why it happens:** Swing's default component painting clips to the component's bounds. A focus ring that extends 3px outside those bounds gets cut off.
**How to avoid:** Components with focus rings need extra insets/margin to accommodate the ring within their allocated bounds. The focus width should be factored into the component's preferred size and paint area. FlatLaf solves this by including `focusWidth` in the component's border insets.
**Warning signs:** Focus rings appear on left/top edges but get cut on right/bottom, or vice versa.

### Pitfall 2: Shadow Image Size Does Not Account for Blur Spread

**What goes wrong:** Shadow gets hard-clipped at edges, producing visible rectangular artifacts.
**Why it happens:** The `BufferedImage` for the shadow must be larger than the component by `blurRadius + spreadRadius` on each side, but developers size it to the component dimensions.
**How to avoid:** Calculate image size as `(width + 2 * (blurRadius + spreadRadius), height + 2 * (blurRadius + spreadRadius))`. Paint the shape centered in this enlarged image, apply blur, then paint the image offset so the shape aligns with the component.
**Warning signs:** Shadow appears to have sharp rectangular edges instead of soft falloff.

### Pitfall 3: ConvolveOp EDGE_NO_OP Creates Hard Edges

**What goes wrong:** Using `ConvolveOp.EDGE_NO_OP` leaves un-blurred pixels at image edges, creating a visible rectangular ghost.
**Why it happens:** `EDGE_NO_OP` copies source pixels unchanged where the kernel extends beyond the image boundary.
**How to avoid:** Use `ConvolveOp.EDGE_ZERO_FILL` for shadow images (transparent edges fade naturally). Alternatively, pad the image with extra transparent pixels beyond the blur radius.
**Warning signs:** A faint rectangle outline visible around the shadow.

### Pitfall 4: Antialiasing Hint Interaction with Stroke Control

**What goes wrong:** Shapes look blurry or have inconsistent line thickness even with antialiasing on.
**Why it happens:** `KEY_STROKE_CONTROL` defaults to `VALUE_STROKE_DEFAULT`, which may or may not normalize stroke positions. On some platforms, this causes sub-pixel shifts.
**How to avoid:** Always set `KEY_STROKE_CONTROL` to `VALUE_STROKE_NORMALIZE` alongside `KEY_ANTIALIASING = VALUE_ANTIALIAS_ON`. This normalizes strokes to consistent widths. FlatLaf does this in every paint call.
**Warning signs:** Lines appear thicker on some edges than others; 1px borders look inconsistent.

### Pitfall 5: HiDPI Double-Scaling

**What goes wrong:** Components appear twice as large on HiDPI displays.
**Why it happens:** Since Java 9, the JDK automatically applies a scale transform to `Graphics2D`. If code also manually scales (e.g., multiplying coordinates by `Toolkit.getScreenResolution() / 96`), the scaling doubles.
**How to avoid:** Do NOT manually scale coordinates for HiDPI. Paint in logical pixels. The JDK transform handles device scaling. Only read the transform scale when you need to create resolution-appropriate `BufferedImage` objects (shadow cache images should be at device resolution).
**Warning signs:** Components are gigantic on HiDPI but correct on 1x displays.

### Pitfall 6: Shadow Cache Key Missing Component of Visual Identity

**What goes wrong:** Shadows appear stale after theme changes or component resize.
**Why it happens:** Cache key does not include all visually-relevant parameters (e.g., forgot shadow color, or forgot arc radius).
**How to avoid:** Use a `record` as cache key with ALL visual parameters: width, height, arc, blurRadius, spreadRadius, offsetX, offsetY, shadowColor RGB.
**Warning signs:** Shadow color does not update when switching light/dark theme.

### Pitfall 7: Disabled State Only Changes Opacity, Not Cursor

**What goes wrong:** Disabled components look faded but still show pointer cursor, confusing users.
**Why it happens:** The painting utility applies `AlphaComposite` for opacity but does not set the cursor.
**How to avoid:** `StateColorResolver` should be purely about color resolution. Cursor changes happen in the ComponentUI delegate's `installDefaults()`. Document this boundary clearly.
**Warning signs:** Cursor changes to hand/pointer over disabled buttons.

## Code Examples

### Antialiasing Setup with Full Rendering Hints

```java
// Source: FlatLaf FlatUIUtils.setRenderingHints pattern + Oracle RenderingHints docs
public static Object[] setupPaintingHints(Graphics2D g) {
    Object[] saved = {
        g.getRenderingHint(RenderingHints.KEY_ANTIALIASING),
        g.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL)
    };
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                       RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                       RenderingHints.VALUE_STROKE_NORMALIZE);
    return saved;
}

public static void restorePaintingHints(Graphics2D g, Object[] saved) {
    if (saved[0] != null)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, saved[0]);
    if (saved[1] != null)
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, saved[1]);
}
```

### Rounded Background Fill

```java
// Source: FlatLaf FlatUIUtils.paintComponentBackground pattern
public static void paintRoundedBackground(Graphics2D g, int x, int y,
        int width, int height, float arc, Color background) {
    if (background == null) return;
    Object[] hints = setupPaintingHints(g);
    g.setColor(background);
    g.fill(createRoundedShape(x, y, width, height, arc));
    restorePaintingHints(g, hints);
}
```

### Focus Ring Painting (DWC Style)

```java
// Source: DWC CSS focus ring tokens + FlatLaf paintOutline pattern
// DWC focus ring = box-shadow: 0 0 0 3px hsla(H, S, 45%, 0.4)
// Equivalent: a 3px outline outside component bounds, semi-transparent, following component arc
public static void paintFocusRing(Graphics2D g, int x, int y,
        int width, int height, float componentArc, float ringWidth,
        Color ringColor) {
    if (ringColor == null || ringWidth <= 0) return;
    Object[] hints = setupPaintingHints(g);
    g.setColor(ringColor);

    // Ring is OUTSIDE the component, so expand outward
    float rx = x - ringWidth;
    float ry = y - ringWidth;
    float rw = width + ringWidth * 2;
    float rh = height + ringWidth * 2;
    float outerArc = componentArc + ringWidth;

    // Even-odd fill: outer ring shape minus inner component shape
    Path2D ring = new Path2D.Float(Path2D.WIND_EVEN_ODD);
    ring.append(createRoundedShape(rx, ry, rw, rh, outerArc), false);
    ring.append(createRoundedShape(x, y, width, height, componentArc), false);
    g.fill(ring);

    restorePaintingHints(g, hints);
}
```

### Focus Ring Color from DWC Tokens

```java
// DWC CSS: hsla(var(--dwc-color-primary-h), var(--dwc-color-primary-s),
//               var(--dwc-focus-ring-l), var(--dwc-focus-ring-a))
// = hsla(211, 100%, 45%, 0.4) for default primary theme
public static Color createFocusRingColor(Color baseColor, float lightness, float alpha) {
    // Extract hue and saturation from base color, override lightness and alpha
    float[] hsb = Color.RGBtoHSB(baseColor.getRed(), baseColor.getGreen(),
                                  baseColor.getBlue(), null);
    // HSB brightness != HSL lightness, so convert properly
    // HSL to RGB conversion for correct focus ring color
    float hue = hsb[0] * 360;
    float sat = hsb[1]; // approximate: reuse base saturation
    Color hslColor = hslToRgb(hue, sat, lightness);
    return new Color(hslColor.getRed(), hslColor.getGreen(),
                     hslColor.getBlue(), Math.round(alpha * 255));
}
```

### Gaussian Blur for Shadow (Separable Two-Pass)

```java
// Source: ConvolveOp JDK 21 docs + separable Gaussian kernel technique
public static BufferedImage applyGaussianBlur(BufferedImage src, float radius) {
    if (radius <= 0) return src;
    int size = (int) Math.ceil(radius * 3) * 2 + 1; // kernel covers 3 sigma
    float[] kernelData = createGaussianKernel(size, radius);

    // Horizontal pass
    Kernel hKernel = new Kernel(size, 1, kernelData);
    ConvolveOp hOp = new ConvolveOp(hKernel, ConvolveOp.EDGE_ZERO_FILL, null);
    BufferedImage temp = hOp.filter(src, null);

    // Vertical pass
    Kernel vKernel = new Kernel(1, size, kernelData);
    ConvolveOp vOp = new ConvolveOp(vKernel, ConvolveOp.EDGE_ZERO_FILL, null);
    return vOp.filter(temp, null);
}

private static float[] createGaussianKernel(int size, float radius) {
    float[] data = new float[size];
    float sigma = radius;
    float twoSigmaSq = 2 * sigma * sigma;
    float sum = 0;
    int center = size / 2;
    for (int i = 0; i < size; i++) {
        float dist = i - center;
        data[i] = (float) Math.exp(-(dist * dist) / twoSigmaSq);
        sum += data[i];
    }
    // Normalize
    for (int i = 0; i < size; i++) data[i] /= sum;
    return data;
}
```

### HiDPI Scale Detection

```java
// Source: FlatLaf HiDPIUtils + JDK Graphics2D transform docs
public static float getScaleFactor(Graphics2D g) {
    AffineTransform tx = g.getTransform();
    // For non-rotated transforms, scaleX == scaleY
    return (float) tx.getScaleX();
}

public static BufferedImage createHiDpiImage(Graphics2D g,
        int logicalWidth, int logicalHeight) {
    float scale = getScaleFactor(g);
    int deviceWidth = Math.round(logicalWidth * scale);
    int deviceHeight = Math.round(logicalHeight * scale);
    return new BufferedImage(deviceWidth, deviceHeight,
                             BufferedImage.TYPE_INT_ARGB);
}
```

### State Color Resolver

```java
// Source: FlatLaf FlatButtonUI.buttonStateColor pattern
public static Color resolveBackground(Component c, Color enabled,
        Color disabled, Color focused, Color hover, Color pressed) {
    if (!c.isEnabled())
        return disabled != null ? disabled : enabled;

    if (c instanceof AbstractButton ab) {
        ButtonModel m = ab.getModel();
        if (m.isArmed() && m.isPressed())
            return pressed != null ? pressed : enabled;
        if (m.isRollover())
            return hover != null ? hover : enabled;
    }

    if (c.hasFocus())
        return focused != null ? focused : enabled;

    return enabled;
}
```

### Disabled State Painting with Opacity

```java
// Source: DWC CSS --dwc-disabled-opacity: 0.6
public static void paintWithOpacity(Graphics2D g, float opacity,
        Runnable paintAction) {
    Composite oldComposite = g.getComposite();
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
    paintAction.run();
    g.setComposite(oldComposite);
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `Graphics.drawRoundRect(int...)` | `Graphics2D.fill(RoundRectangle2D.Float)` | Java 1.2 | Float precision essential for HiDPI; int causes misalignment |
| Manual HiDPI scale via system properties | JDK auto-scales Graphics2D transform | Java 9 (2017) | No manual scaling needed; read `getTransform()` for device resolution |
| `Graphics2D.draw()` with `BasicStroke` for outlines | `Path2D.WIND_EVEN_ODD` fill with outer-inner shapes | FlatLaf pattern, widely adopted | Exact pixel control, no half-pixel stroke bleed |
| Single-pass 2D ConvolveOp for blur | Two-pass separable (horizontal + vertical) ConvolveOp | Performance optimization standard | O(2N) vs O(N*N) per pixel for kernel size N |
| `VALUE_STROKE_DEFAULT` | `VALUE_STROKE_NORMALIZE` | Best practice since Java 1.5 | Consistent 1px lines across platforms |

**Deprecated/outdated:**
- `Graphics.drawRoundRect(int, int, int, int, int, int)`: Uses int coordinates only. Use `Graphics2D.fill/draw(RoundRectangle2D.Float)` instead.
- Manual `sun.java2d.uiScale` property reading: Not portable. Use `Graphics2D.getTransform().getScaleX()`.
- `Toolkit.getScreenResolution()`: Returns 72 or 96 depending on platform, not actual DPI. Unreliable for HiDPI detection.

## DWC-Specific Token Integration Notes

### Focus Ring Tokens (from default-light.css)

```
--dwc-focus-ring-l: 45%           -> lightness for HSL color
--dwc-focus-ring-a: 0.4           -> alpha transparency
--dwc-focus-ring-width: 3px       -> ring width in pixels
```

The focus ring color formula: `hsla(component-hue, component-saturation, 45%, 0.4)`. Each color theme (primary, success, warning, danger, etc.) uses its own hue/saturation with the shared lightness and alpha.

### Shadow Tokens (from default-light.css)

Shadows are multi-layer, each layer defined as:
```
offset-x offset-y blur-radius color
```

Example `--dwc-shadow-m` has 3 layers:
- `0 100px 80px hsla(color, 0.07)` -- deep ambient
- `0 22.33px 17.86px hsla(color, 0.04)` -- mid spread
- `0 6.65px 5.32px hsla(color, 0.02)` -- close contact

**Important:** The DWC shadows use very large blur radii (up to 100px). For Swing components (typically 30-40px tall), this means the shadow extends far beyond the component. For practical implementation, consider:
1. Clamping the maximum blur radius to a reasonable value for desktop painting
2. Only rendering the innermost 1-2 shadow layers for most components
3. Using full multi-layer shadows only for elevated panels/windows

### State Color Pattern (from DWC button CSS)

```
Normal:   --dwc-button-background       (--dwc-color-default)
Hover:    --dwc-button-hover-background  (--dwc-color-default-light)
Pressed:  --dwc-button-selected-background (--dwc-color-default-dark)
Focus:    Focus ring only (background same as hover)
Disabled: Reduce opacity to 0.6
```

### Border Radius Token

```
--dwc-border-radius: var(--dwc-border-radius-s) = 0.25em
```

At 16px base font = 4px border radius. Already mapped to `Button.arc`, `Component.arc`, etc. in token-mapping.properties.

## Open Questions

1. **Shadow parsing from CSS token values**
   - What we know: Shadow tokens are complex multi-layer `box-shadow` shorthand strings (e.g., `0 100px 80px hsla(...), 0 22.33px 17.86px hsla(...)`)
   - What's unclear: The current `CssValueTyper` likely classifies these as `RawValue` since they cannot be typed as a single color, dimension, or number. A shadow parser may be needed to decompose these strings into structured `ShadowSpec` records.
   - Recommendation: Create a `ShadowSpec` record with `offsetX, offsetY, blurRadius, spreadRadius, color` fields, and a parser that handles the multi-layer comma-separated format. This parser can be part of Phase 3 or deferred to Phase 4 when button/panel delegates first need it. For Phase 3, the `ShadowPainter` API should accept pre-parsed `ShadowSpec` values.

2. **Focus ring vs. component bounds interaction**
   - What we know: FlatLaf includes focus width in component border insets, which means the component itself allocates space for the ring.
   - What's unclear: Whether DWC-style focus ring (which is a `box-shadow` that paints outside bounds) should be handled the same way, or if delegates should temporarily expand the clip region.
   - Recommendation: Follow FlatLaf's approach -- include `focusWidth` in the component border insets. This is simpler and avoids clip management issues. The painting area is within bounds but the visual ring appears to be "outside" the content area.

3. **BufferedImage type for shadow cache on varying color models**
   - What we know: `TYPE_INT_ARGB` is standard for transparency support.
   - What's unclear: Whether some display configurations (8-bit, remote desktop) would benefit from `TYPE_INT_ARGB_PRE` (pre-multiplied alpha) for faster compositing.
   - Recommendation: Use `TYPE_INT_ARGB` universally. Pre-multiplied alpha adds complexity with negligible benefit for the small images involved in shadow caching.

## Sources

### Primary (HIGH confidence)
- [Oracle RenderingHints JDK 21 API](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/java/awt/RenderingHints.html) - All KEY_/VALUE_ constants verified
- [Oracle RoundRectangle2D JDK 21 API](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/java/awt/geom/RoundRectangle2D.html) - Float variant API verified
- [Oracle ConvolveOp JDK 17 API](https://docs.oracle.com/en/java/javase/17/docs/api/java.desktop/java/awt/image/ConvolveOp.html) - Edge handling modes verified
- FlatLaf `FlatUIUtils.java` source (GitHub main branch) - Rounded shape creation, outline painting, antialiasing setup patterns
- FlatLaf `HiDPIUtils.java` source (GitHub main branch) - Scale detection, paintAtScale1x pattern
- FlatLaf `FlatButtonUI.java` source (GitHub main branch) - `buttonStateColor()` priority chain pattern
- DWC `default-light.css` (local project) - Focus ring tokens, shadow tokens, border radius tokens, disabled opacity
- DWC `dwc-button.scss` (local project) - Focus ring as box-shadow, state color resolution order

### Secondary (MEDIUM confidence)
- [FlatLaf project page](https://www.formdev.com/flatlaf/) - Architecture overview, client properties, customization patterns
- [FlatLaf GitHub](https://github.com/JFormDesigner/FlatLaf) - Active development through 2026
- [awt-painter library](https://github.com/smikhalevski/awt-painter) - CSS box-shadow to Java2D reference implementation (MIT, archived 2021)
- [Oracle Java Tutorials - Controlling Rendering Quality](https://docs.oracle.com/javase/tutorial/2d/advanced/quality.html) - General rendering hints guidance

### Tertiary (LOW confidence)
- Gaussian blur separable kernel optimization: Well-established mathematical property, confirmed across multiple sources but not verified in JDK-specific benchmarks for this project's image sizes

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All classes are JDK standard library (`java.awt`, `java.awt.geom`, `java.awt.image`), zero external dependencies, well-documented
- Architecture: HIGH - FlatLaf's painting utility architecture is production-proven, open source, directly inspectable; DWC CSS tokens are in the local project
- Pitfalls: HIGH - Documented from FlatLaf source code patterns, JDK API docs, and well-known Java2D gotchas
- Shadow implementation: MEDIUM - Two-pass separable ConvolveOp is standard technique but performance characteristics for this specific use case (small images, frequent caching) are not benchmarked
- HiDPI: MEDIUM - JDK auto-scaling since Java 9 is well-documented, but fractional scale factors (125%, 150%) may need testing on Windows

**Research date:** 2026-02-10
**Valid until:** 2026-03-12 (stable domain; Java2D API does not change between JDK releases)
