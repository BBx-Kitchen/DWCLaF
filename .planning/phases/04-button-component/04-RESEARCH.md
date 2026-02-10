# Phase 4: Button Component - Research

**Researched:** 2026-02-10
**Domain:** Custom Swing ButtonUI delegate painting with CSS design tokens (Java2D, BasicButtonUI, UIDefaults)
**Confidence:** HIGH

## Summary

This phase implements a custom `DwcButtonUI` extending `BasicButtonUI` that paints JButton with DWC CSS token-derived appearance. The existing infrastructure provides everything needed: `CssThemeLoader`/`CssTokenMap` for token access, `UIDefaultsPopulator` for populating UIDefaults, `PaintUtils` for rounded backgrounds and outlines, `StateColorResolver` for state-dependent color selection, `FocusRingPainter` for focus rings, and `ShadowPainter` for box shadows. The token-mapping.properties already maps the critical DWC button tokens (`--dwc-color-primary`, `--dwc-color-default`, `--dwc-button-background`, etc.) to Swing UIDefaults keys like `Button.background`, `Button.foreground`, `Button.default.background`, `Button.hoverBackground`, `Button.pressedBackground`, etc.

The primary technical challenges are: (1) implementing the complete painting pipeline (background, border, icon, text, focus ring) in the correct layered order, (2) implementing a custom `DwcButtonBorder` that includes focus ring width in its insets so the focus ring paints within the allocated component bounds, (3) supporting two button variants (default/standard with `--dwc-color-default` and primary with `--dwc-color-primary`) via a client property, and (4) correctly handling the five visual states (enabled, hover, pressed, focused, disabled) using `StateColorResolver`. The DWC CSS already defines all necessary tokens for these variants and states.

FlatLaf's `FlatButtonUI` architecture serves as the reference model. It extends `BasicButtonUI`, reads color/dimension properties from UIDefaults in `installDefaults()`, overrides `paint()` to fully control rendering, delegates icon/text layout to `SwingUtilities.layoutCompoundLabel()`, and uses a custom border (`FlatButtonBorder`) that includes `focusWidth` in its insets. This project follows the same architecture but adapted for DWC tokens.

**Primary recommendation:** Build `DwcButtonUI` (extends `BasicButtonUI`) + `DwcButtonBorder` (extends `AbstractBorder`), register via `initClassDefaults()` in `DwcLookAndFeel`, read all colors/dimensions from UIDefaults, use the Phase 3 painting utilities for all rendering, and support primary variant via `JButton.putClientProperty("dwc.buttonType", "primary")`.

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `javax.swing.plaf.basic.BasicButtonUI` | JDK 21 | Base class for button delegate | Standard Swing extension point; handles keyboard actions, accessibility, base layout |
| `javax.swing.border.AbstractBorder` | JDK 21 | Base class for custom button border | Standard Swing border extension point with `getBorderInsets()` and `paintBorder()` |
| `javax.swing.SwingUtilities` | JDK 21 | `layoutCompoundLabel()` for icon+text layout | The canonical way to position icon and text within button bounds |
| `com.dwc.laf.painting.PaintUtils` | Phase 3 | Rounded background, outline painting, antialiasing | Already built and tested |
| `com.dwc.laf.painting.StateColorResolver` | Phase 3 | State-based color resolution (disabled>pressed>hover>focused>enabled) | Already built and tested |
| `com.dwc.laf.painting.FocusRingPainter` | Phase 3 | Focus ring painting outside component shape | Already built and tested |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `com.dwc.laf.painting.HiDpiUtils` | Phase 3 | Scale factor detection | When creating device-resolution images for shadow caching |
| `com.dwc.laf.painting.ShadowPainter` | Phase 3 | Box shadow rendering | If button shadows are needed (not standard for DWC buttons, but available) |
| `javax.swing.plaf.ColorUIResource` | JDK 21 | UIResource contract compliance for colors | All colors installed from L&F must be wrapped |
| `javax.swing.plaf.InsetsUIResource` | JDK 21 | UIResource contract compliance for insets | Border insets returned from border must be UIResource |
| `javax.swing.BasicGraphicsUtils` | JDK 21 | `getPreferredButtonSize()` and mnemonics underline | Reuse existing size computation from BasicButtonUI |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Extending `BasicButtonUI` | Extending `MetalButtonUI` | Metal has visual opinions (gradient backgrounds, different pressed appearance) that would need overriding; Basic is cleaner |
| Custom `DwcButtonBorder` | `EmptyBorder` with hardcoded insets | Custom border can paint the border line and adjust insets based on focus width dynamically |
| Client property for variant | Separate `DwcPrimaryButtonUI` class | Client property is more flexible, follows FlatLaf pattern, single UI class handles both variants |

## Architecture Patterns

### Recommended Project Structure

```
src/main/java/com/dwc/laf/
  ui/
    DwcButtonUI.java          # Custom ButtonUI delegate
    DwcButtonBorder.java      # Custom border with focus-width-aware insets
  DwcLookAndFeel.java         # Register DwcButtonUI in initClassDefaults()
```

### Pattern 1: ButtonUI Delegate Structure (FlatLaf-inspired)

**What:** A custom ButtonUI that fully controls rendering by overriding `paint()`, delegates to `BasicButtonUI` for keyboard/accessibility, and reads all visual properties from UIDefaults.
**When to use:** This is THE pattern for this phase.
**Example:**

```java
// Source: FlatLaf FlatButtonUI architecture + BasicButtonUI contract
public class DwcButtonUI extends BasicButtonUI {

    // Colors from UIDefaults (loaded in installDefaults)
    private Color background;
    private Color foreground;
    private Color hoverBackground;
    private Color pressedBackground;
    private Color focusedBackground;
    private Color disabledBackground;
    private Color disabledText;

    // Primary variant colors
    private Color defaultBackground;
    private Color defaultForeground;
    private Color defaultHoverBackground;
    private Color defaultPressedBackground;

    // Dimensions from UIDefaults
    private int arc;
    private int focusWidth;
    private int borderWidth;
    private int minimumWidth;
    private float disabledOpacity;

    // Factory method - each button gets its own UI instance
    public static ComponentUI createUI(JComponent c) {
        return new DwcButtonUI();
    }

    @Override
    protected void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        // Read UIDefaults populated from CSS tokens
        background = UIManager.getColor("Button.background");
        foreground = UIManager.getColor("Button.foreground");
        // ... etc
        b.setRolloverEnabled(true); // Enable rollover for hover state
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            // 1. Paint background (state-dependent)
            // 2. Paint border outline
            // 3. Layout icon + text via SwingUtilities.layoutCompoundLabel
            // 4. Paint icon
            // 5. Paint text
            // 6. Paint focus ring (if focused)
        } finally {
            g2.dispose();
        }
    }
}
```

### Pattern 2: Focus Width in Border Insets

**What:** The button border adds `focusWidth` to its insets so the focus ring paints within the component's allocated bounds, not outside them (which would get clipped).
**When to use:** Always -- this prevents focus ring clipping.
**Example:**

```java
// Source: FlatLaf FlatButtonBorder pattern
public class DwcButtonBorder extends AbstractBorder {

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        int focusWidth = UIManager.getInt("Component.focusWidth");
        int bw = UIManager.getInt("Component.borderWidth");
        Insets margin = (c instanceof AbstractButton ab) ? ab.getMargin() : new Insets(0,0,0,0);

        // Focus width is added to all sides
        insets.top = focusWidth + bw + margin.top;
        insets.left = focusWidth + bw + margin.left;
        insets.bottom = focusWidth + bw + margin.bottom;
        insets.right = focusWidth + bw + margin.right;
        return insets;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
        // Paint border outline within the focusWidth offset area
        int focusWidth = UIManager.getInt("Component.focusWidth");
        int arc = UIManager.getInt("Button.arc");
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Color borderColor = UIManager.getColor("Button.borderColor");
            if (borderColor != null) {
                Object[] saved = PaintUtils.setupPaintingHints(g2);
                g2.setColor(borderColor);
                PaintUtils.paintOutline(g2,
                    focusWidth, focusWidth,
                    w - focusWidth * 2, h - focusWidth * 2,
                    UIManager.getInt("Component.borderWidth"), arc);
                PaintUtils.restorePaintingHints(g2, saved);
            }
        } finally {
            g2.dispose();
        }
    }
}
```

### Pattern 3: Primary Variant via Client Property

**What:** Distinguish primary (accent-colored) vs default (neutral-colored) buttons using a client property.
**When to use:** When `JButton.putClientProperty("dwc.buttonType", "primary")` is set.
**Example:**

```java
// In DwcButtonUI.paint():
private boolean isPrimary(AbstractButton b) {
    return "primary".equals(b.getClientProperty("dwc.buttonType"));
}

private Color getEffectiveBackground(AbstractButton b) {
    if (isPrimary(b)) {
        return StateColorResolver.resolve(b,
            defaultBackground, null, null,
            defaultHoverBackground, defaultPressedBackground);
    }
    return StateColorResolver.resolve(b,
        background, null, null,
        hoverBackground, pressedBackground);
}
```

### Pattern 4: Disabled State with Opacity

**What:** Disabled buttons paint at reduced opacity (0.6 per DWC `--dwc-disabled-opacity`) using `StateColorResolver.paintWithOpacity()`.
**When to use:** When `!component.isEnabled()`.
**Example:**

```java
// In paint():
if (!b.isEnabled()) {
    float opacity = UIManager.getFloat("Component.disabledOpacity");
    StateColorResolver.paintWithOpacity(g2, opacity, () -> {
        paintButtonContent(g2, b, ...);
    });
} else {
    paintButtonContent(g2, b, ...);
}
```

### Pattern 5: L&F Registration

**What:** Register DwcButtonUI in `DwcLookAndFeel.initClassDefaults()`.
**When to use:** During L&F initialization.
**Example:**

```java
// In DwcLookAndFeel.initClassDefaults():
@Override
protected void initClassDefaults(UIDefaults table) {
    super.initClassDefaults(table);
    table.put("ButtonUI", "com.dwc.laf.ui.DwcButtonUI");
}
```

### Anti-Patterns to Avoid

- **Shared UI instance for stateful delegate:** `BasicButtonUI` caches a single instance per `AppContext`. A custom delegate that stores per-component state (colors resolved from UIDefaults) MUST NOT use shared instances. Return `new DwcButtonUI()` from `createUI()`.
- **Painting outside component bounds:** Focus ring painted outside bounds gets clipped. Include `focusWidth` in border insets instead.
- **Hardcoding colors instead of reading UIDefaults:** Defeats the CSS-token pipeline. All colors come from UIDefaults (populated by `UIDefaultsPopulator` from CSS tokens).
- **Forgetting `setRolloverEnabled(true)`:** Without this, `ButtonModel.isRollover()` never returns `true`, so hover states never activate. Must be set in `installDefaults()`.
- **Using `paintButtonPressed()` from BasicButtonUI:** This method fills the entire button area with a hardcoded dark color. Override `paint()` completely and do NOT call `super.paint()` for background.
- **Calling `super.paint()` for text/icon:** `BasicButtonUI.paint()` calls `paintButtonPressed()` which fights custom painting. Either override the entire `paint()` or call only the specific methods (`paintIcon()`, `paintText()`) from super.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| State color selection | Custom if/else chain in paint() | `StateColorResolver.resolve()` | Centralized priority chain, already tested, handles all 5 states |
| Rounded background fill | Manual `g2.fill(RoundRectangle2D)` with hint management | `PaintUtils.paintRoundedBackground()` | Handles antialiasing setup/restore, null checks, shape degeneration |
| Focus ring painting | Manual Path2D even-odd construction | `FocusRingPainter.paintFocusRing()` | Already handles outer arc expansion, null/zero checks, hint management |
| Icon+text positioning | Manual inset/size arithmetic | `SwingUtilities.layoutCompoundLabel()` | Handles all alignment combinations, text clipping, mnemonic index |
| Disabled opacity | Manual alpha color manipulation | `StateColorResolver.paintWithOpacity()` | Wraps composite save/restore correctly |
| Outline/border painting | `g2.draw()` with BasicStroke | `PaintUtils.paintOutline()` | Even-odd fill avoids stroke centering artifacts |

**Key insight:** Phase 3 built all the painting primitives this phase needs. The ButtonUI's job is composition -- calling the right utility in the right order with the right parameters from UIDefaults. Do not reimplement any painting primitive.

## Common Pitfalls

### Pitfall 1: Shared UI Instance Storing Per-Component State

**What goes wrong:** Colors loaded in `installDefaults()` are shared across all buttons, so all buttons look identical (e.g., all primary or all default).
**Why it happens:** `BasicButtonUI.createUI()` returns a cached singleton. If you store instance fields (colors, dimensions) on a shared instance, the last button to call `installDefaults()` wins.
**How to avoid:** Return `new DwcButtonUI()` from `createUI()` -- one instance per button. Alternatively, read colors from UIDefaults in `paint()` rather than `installDefaults()`, but per-instance is simpler.
**Warning signs:** All buttons have the same color regardless of variant client property.

### Pitfall 2: Focus Ring Clipped by Parent

**What goes wrong:** The focus ring is painted but invisible or partially cut off.
**Why it happens:** The focus ring extends outside the component's content area. If the border insets don't account for `focusWidth`, the ring paints in the region the parent container clips.
**How to avoid:** `DwcButtonBorder.getBorderInsets()` adds `focusWidth` to all sides. The background and content paint inside the focus-width-inset area, and the focus ring paints in the reserved outer area.
**Warning signs:** Focus ring visible on some edges but cut on others; or no focus ring visible at all.

### Pitfall 3: Rollover Not Enabled

**What goes wrong:** Hover state never activates. Buttons go straight from normal to pressed with no hover feedback.
**Why it happens:** `AbstractButton.setRolloverEnabled(false)` is the Swing default. Without `setRolloverEnabled(true)`, `ButtonModel.isRollover()` always returns `false`.
**How to avoid:** Call `b.setRolloverEnabled(true)` in `installDefaults()`. Or set `UIManager.put("Button.rollover", true)` in L&F component defaults.
**Warning signs:** `ButtonModel.isRollover()` never returns true when hovering.

### Pitfall 4: Armed vs Pressed Confusion

**What goes wrong:** Pressed state activates when mouse moves out of button while held down.
**Why it happens:** `ButtonModel.isPressed()` returns true even when the mouse has left the button (press started in button, dragged out). The button is only visually "pressed" when BOTH armed and pressed.
**How to avoid:** Check `model.isArmed() && model.isPressed()` (already implemented in `StateColorResolver.resolve()`). The existing `StateColorResolver` handles this correctly.
**Warning signs:** Button shows pressed color when mouse is pressed but cursor is outside the button.

### Pitfall 5: Not Respecting contentAreaFilled and borderPainted

**What goes wrong:** Application code that sets `button.setContentAreaFilled(false)` or `button.setBorderPainted(false)` has no effect.
**Why it happens:** The custom `paint()` method ignores these standard `AbstractButton` properties.
**How to avoid:** Check `b.isContentAreaFilled()` before painting background. Check `b.isBorderPainted()` before painting border. `BasicButtonUI` respects these in its default implementation.
**Warning signs:** Buttons styled as "transparent" or "text-only" still show background/border.

### Pitfall 6: Mnemonic Underline Missing

**What goes wrong:** Alt+key mnemonics work but the underline indicator is not visible.
**Why it happens:** Custom `paintText()` doesn't pass the mnemonic index to `BasicGraphicsUtils.drawStringUnderlineCharAt()`.
**How to avoid:** Get `b.getDisplayedMnemonicIndex()` and pass it to the text painting method.
**Warning signs:** Mnemonic keys work functionally but no character is underlined.

### Pitfall 7: Icon Gap Not Applied

**What goes wrong:** Icon and text overlap or have no gap.
**Why it happens:** `layoutCompoundLabel()` needs `iconTextGap` from the button. If the delegate doesn't pass `b.getIconTextGap()`, the layout defaults to 0.
**How to avoid:** Pass `b.getIconTextGap()` to `SwingUtilities.layoutCompoundLabel()`.
**Warning signs:** Icon butts up against text with no space.

## Code Examples

### Complete Paint Method Structure

```java
// Source: FlatLaf FlatButtonUI.paint pattern adapted for DWC
@Override
public void paint(Graphics g, JComponent c) {
    AbstractButton b = (AbstractButton) c;
    ButtonModel model = b.getModel();
    Graphics2D g2 = (Graphics2D) g.create();

    try {
        int width = c.getWidth();
        int height = c.getHeight();

        // Determine variant
        boolean primary = isPrimary(b);

        // Resolve focus and arc dimensions
        int fw = UIManager.getInt("Component.focusWidth"); // e.g. 3
        int bw = UIManager.getInt("Component.borderWidth"); // e.g. 1
        int arc = UIManager.getInt("Button.arc"); // e.g. 4

        // Content area (inside focus ring space)
        float cx = fw;
        float cy = fw;
        float cw = width - fw * 2;
        float ch = height - fw * 2;

        // 1. Paint background
        if (b.isContentAreaFilled()) {
            Color bg = resolveBackground(b, primary);
            if (!b.isEnabled()) {
                StateColorResolver.paintWithOpacity(g2,
                    UIManager.getFloat("Component.disabledOpacity"), () -> {
                        PaintUtils.paintRoundedBackground(g2, cx, cy, cw, ch, arc, bg);
                    });
            } else {
                PaintUtils.paintRoundedBackground(g2, cx, cy, cw, ch, arc, bg);
            }
        }

        // 2. Paint border (via DwcButtonBorder.paintBorder, not here)

        // 3. Layout icon + text
        FontMetrics fm = g2.getFontMetrics(b.getFont());
        Rectangle viewRect = new Rectangle();
        Rectangle iconRect = new Rectangle();
        Rectangle textRect = new Rectangle();

        Insets insets = b.getInsets();
        viewRect.x = insets.left;
        viewRect.y = insets.top;
        viewRect.width = width - (insets.left + insets.right);
        viewRect.height = height - (insets.top + insets.bottom);

        String text = SwingUtilities.layoutCompoundLabel(
            c, fm, b.getText(), b.getIcon(),
            b.getVerticalAlignment(), b.getHorizontalAlignment(),
            b.getVerticalTextPosition(), b.getHorizontalTextPosition(),
            viewRect, iconRect, textRect,
            b.getText() == null ? 0 : b.getIconTextGap()
        );

        // 4. Paint icon
        if (b.getIcon() != null) {
            Icon icon = getIcon(b);
            if (icon != null) {
                icon.paintIcon(c, g2, iconRect.x, iconRect.y);
            }
        }

        // 5. Paint text
        if (text != null && !text.isEmpty()) {
            Color fg = resolveForeground(b, primary);
            if (!b.isEnabled()) {
                fg = UIManager.getColor("Button.disabledText");
                if (fg == null) fg = foreground;
            }
            g2.setColor(fg);
            // Use BasicGraphicsUtils or direct drawString
            paintText(g2, b, textRect, text);
        }

        // 6. Paint focus ring
        if (b.isFocusPainted() && b.hasFocus()) {
            Color focusColor = UIManager.getColor("Button.focusRingColor");
            FocusRingPainter.paintFocusRing(g2,
                cx, cy, cw, ch, arc, fw, focusColor);
        }
    } finally {
        g2.dispose();
    }
}
```

### Icon Selection by State

```java
// Source: BasicButtonUI pattern
private Icon getIcon(AbstractButton b) {
    ButtonModel model = b.getModel();
    Icon icon = b.getIcon();

    if (!model.isEnabled()) {
        Icon disabledIcon = b.getDisabledIcon();
        return disabledIcon != null ? disabledIcon : icon;
    }
    if (model.isPressed() && model.isArmed()) {
        Icon pressedIcon = b.getPressedIcon();
        return pressedIcon != null ? pressedIcon : icon;
    }
    if (model.isRollover()) {
        Icon rolloverIcon = b.getRolloverIcon();
        return rolloverIcon != null ? rolloverIcon : icon;
    }
    return icon;
}
```

### Token-to-UIDefaults Mapping for Button

The existing `token-mapping.properties` already maps these tokens:

```properties
# Already in token-mapping.properties:
--dwc-color-primary        = color:Button.default.background
--dwc-color-on-primary-text = color:Button.default.foreground
--dwc-color-default        = color:Button.background
--dwc-color-on-default-text = color:Button.foreground
--dwc-border-radius        = int:Button.arc
--dwc-focus-ring-width     = int:Component.focusWidth
--dwc-disabled-opacity     = float:Component.disabledOpacity
--dwc-border-width         = int:Component.borderWidth
--dwc-button-hover-background = color:Button.hoverBackground
--dwc-color-primary-light  = color:Button.default.hoverBackground
--dwc-color-primary-dark   = color:Button.default.pressedBackground
--dwc-color-default-dark   = color:Button.pressedBackground
--dwc-color-default-light  = color:Button.hoverBackground

# Additional mappings needed for Phase 4:
--dwc-button-font-weight   = int:Button.font.style   # already mapped
# New mappings to add:
# --dwc-button-border-color -> color:Button.borderColor
# --dwc-button-selected-background -> color:Button.pressedBackground (already covered by --dwc-color-default-dark)
# Button.default.pressedBackground (already mapped via --dwc-color-primary-dark)
# Button.disabledText -> can derive from Button.foreground with opacity
# Button.margin -> hardcode in DwcButtonBorder or add as DWC token mapping
# Button.minimumWidth -> hardcode from DWC size token
```

### UIDefaults Keys Required by DwcButtonUI

```
# Colors (from CSS tokens via mapping)
Button.background           -> default variant background
Button.foreground           -> default variant text color
Button.hoverBackground      -> default variant hover background
Button.pressedBackground    -> default variant pressed background
Button.default.background   -> primary variant background
Button.default.foreground   -> primary variant text color
Button.default.hoverBackground -> primary variant hover
Button.default.pressedBackground -> primary variant pressed
Button.disabledText         -> disabled text color (can be derived)
Button.focusRingColor       -> focus ring color (need to add mapping)

# Dimensions (from CSS tokens via mapping)
Button.arc                  -> corner arc diameter (from --dwc-border-radius)
Component.focusWidth        -> focus ring width (from --dwc-focus-ring-width)
Component.borderWidth       -> border thickness (from --dwc-border-width)
Component.disabledOpacity   -> disabled opacity (from --dwc-disabled-opacity)

# Layout (hardcoded or from tokens)
Button.margin               -> Insets (2,14,2,14 following FlatLaf)
Button.minimumWidth         -> int (72 following FlatLaf)
Button.iconTextGap          -> int (4 standard)
```

### DwcButtonBorder Complete Implementation Pattern

```java
public class DwcButtonBorder extends AbstractBorder {

    private static final Insets DEFAULT_MARGIN = new Insets(2, 14, 2, 14);

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        int focusWidth = UIManager.getInt("Component.focusWidth");
        int borderWidth = UIManager.getInt("Component.borderWidth");

        Insets margin = DEFAULT_MARGIN;
        if (c instanceof AbstractButton ab) {
            Insets m = ab.getMargin();
            if (m != null && !(m instanceof javax.swing.plaf.UIResource)) {
                margin = m; // Application-set margin takes precedence
            }
        }

        insets.top = focusWidth + borderWidth + margin.top;
        insets.left = focusWidth + borderWidth + margin.left;
        insets.bottom = focusWidth + borderWidth + margin.bottom;
        insets.right = focusWidth + borderWidth + margin.right;
        return insets;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
        // Border painting happens here for the outline
        // Background + focus ring painting happens in DwcButtonUI.paint()
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `BasicButtonUI` shared singleton via AppContext | Per-component UI instances for stateful delegates | FlatLaf pattern, widely adopted | Each button can have different state without interference |
| `paintButtonPressed()` fills entire bounds | State-dependent color via `StateColorResolver` + `paintRoundedBackground` | Modern L&F pattern | Clean rounded backgrounds per state |
| Focus painted via `paintFocus()` with dashes | Focus ring painted as semi-transparent outline outside content | FlatLaf/DWC pattern | Modern focus indicator matching web components |
| `Graphics.drawRoundRect()` for borders | Even-odd fill via `PaintUtils.paintOutline()` | FlatLaf pattern | Sub-pixel precision, no stroke bleed |
| `button.setOpaque(true)` always | `button.setOpaque(false)` + custom background paint | Modern L&F requirement | Allows rounded corners without rectangular artifacts |

**Deprecated/outdated:**
- `BasicButtonUI.paintButtonPressed()`: Fills with hardcoded dark color. Override completely.
- Shared UI singleton for custom delegates: Causes state bleeding between components.
- `drawDashedRect()` for focus: Ugly dashed rectangle. Use semi-transparent focus ring instead.

## UIDefaults Mapping Gap Analysis

The existing `token-mapping.properties` covers most of what's needed. Additional mappings required:

| Missing Mapping | CSS Token | UIDefaults Key | Priority |
|-----------------|-----------|---------------|----------|
| Focus ring color | Computed from `--dwc-color-primary-h/s` + `--dwc-focus-ring-l/a` | `Button.focusRingColor` | HIGH - needed for focus ring |
| Border color (default) | `--dwc-button-border-color` (= `--dwc-color-default`) | `Button.borderColor` | MEDIUM - for border outline |
| Border color (primary) | `--dwc-color-primary` | `Button.default.borderColor` | MEDIUM - for primary border |
| Disabled text | Can derive from foreground | `Button.disabledText` | LOW - can use opacity approach |
| Button margin | `--dwc-space-s` (padding) | `Button.margin` | LOW - hardcode initially |
| Button minimum height | `--dwc-size-m` (2.25rem = 36px) | `Button.minimumHeight` | LOW - hardcode initially |

**Recommendation:** For the focus ring color, since it requires HSL computation from multiple tokens (`--dwc-color-primary-h`, `--dwc-color-primary-s`, `--dwc-focus-ring-l`, `--dwc-focus-ring-a`), compute it programmatically in `DwcButtonUI.installDefaults()` or `DwcLookAndFeel.initComponentDefaults()` rather than trying to express it in the properties mapping.

## Open Questions

1. **Focus ring color computation**
   - What we know: DWC focus ring is `hsla(primary-h, primary-s, 45%, 0.4)`. The tokens `--dwc-color-primary-h=211`, `--dwc-color-primary-s=100%`, `--dwc-focus-ring-l=45%`, `--dwc-focus-ring-a=0.4` are all available.
   - What's unclear: Whether to compute this in the L&F initialization and store in UIDefaults, or compute it per-paint in the ButtonUI.
   - Recommendation: Compute once in `DwcLookAndFeel.initComponentDefaults()` after CSS token loading, store as `Button.focusRingColor` in UIDefaults. This is cheaper and consistent with the UIDefaults-driven architecture.

2. **DWC button border-color vs background**
   - What we know: DWC CSS sets `--dwc-button-border-color: var(--dwc-color-default)` which is the same as the background color for default buttons. The border is visually subtle.
   - What's unclear: Whether to paint a visible border or rely on the background fill alone (since border-color == background-color makes the border invisible).
   - Recommendation: Paint the border for correctness (primary variant has different border behavior, e.g., hover border color differs). The border mechanism is needed for future components anyway.

3. **`button.setOpaque(false)` timing**
   - What we know: For rounded corners to look correct, the button must be non-opaque (otherwise the parent paints a rectangular background behind the rounded fill). FlatLaf sets `button.setOpaque(false)` during `installDefaults()`.
   - What's unclear: Whether to also handle the case where application code sets `button.setOpaque(true)` explicitly.
   - Recommendation: Set `b.setOpaque(false)` in `installDefaults()` only if the current opaque value is a `UIResource` (i.e., not explicitly set by app code). This respects application overrides.

4. **Minimum button height from DWC tokens**
   - What we know: DWC `--dwc-size-m` is `2.25rem` (36px at 16px base), which is the standard component height.
   - What's unclear: Whether to enforce this as a minimum height or let Swing's preferred size calculation handle it.
   - Recommendation: Override `getPreferredSize()` to enforce minimum width (72px, like FlatLaf) and minimum height (36px, from DWC `--dwc-size-m`). This ensures buttons match DWC web component sizing.

## Sources

### Primary (HIGH confidence)
- FlatLaf `FlatButtonUI.java` source (GitHub main branch) - Complete ButtonUI architecture, paint order, state color handling, installDefaults pattern
- FlatLaf `FlatLaf.properties` (GitHub main branch) - Default values for Button.arc (6), Button.margin (2,14,2,14), Button.minimumWidth (72), Component.focusWidth (0)
- FlatLaf Button documentation (formdev.com/flatlaf/components/button/) - UIDefaults keys for button colors and dimensions
- FlatLaf Customizing documentation (formdev.com/flatlaf/customizing/) - Client property pattern for button variants
- OpenJDK `BasicButtonUI.java` source - Base class contract, paint method order, installUI/installDefaults lifecycle
- DWC `default-light.css` (local project) - All DWC design tokens for button, focus ring, disabled state
- DWC `token-mapping.properties` (local project) - Existing CSS-to-UIDefaults mappings
- Project Phase 3 utilities (local project) - PaintUtils, StateColorResolver, FocusRingPainter, ShadowPainter, HiDpiUtils

### Secondary (MEDIUM confidence)
- [FlatLaf project page](https://www.formdev.com/flatlaf/) - Architecture overview, UI defaults structure
- [FlatLaf GitHub](https://github.com/JFormDesigner/FlatLaf) - Active development, reference implementation
- [Oracle BasicButtonUI JDK docs](https://docs.oracle.com/en/java/javase/18/docs/api/java.desktop/javax/swing/plaf/basic/BasicButtonUI.html) - Public API contract
- [Oracle AbstractButton JDK 21 docs](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/swing/AbstractButton.html) - contentAreaFilled, borderPainted, rolloverEnabled properties

### Tertiary (LOW confidence)
- None -- all findings verified against source code or official documentation

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All classes are JDK standard library or already-implemented Phase 3 utilities; FlatLaf reference architecture is well-understood
- Architecture: HIGH - FlatLaf's ButtonUI pattern is production-proven and directly applicable; BasicButtonUI contract is stable since Java 1.2
- Pitfalls: HIGH - Documented from FlatLaf source code patterns, verified against BasicButtonUI contract, and confirmed by existing StateColorResolver implementation
- Token mapping: HIGH - Existing token-mapping.properties already covers most button tokens; gap analysis identifies only 3-4 additional mappings
- Icon handling: MEDIUM - Standard Swing icon API is well-understood, but interaction with DWC-specific icon styling (tinting, sizing) may need exploration in later phases

**Research date:** 2026-02-10
**Valid until:** 2026-03-12 (stable domain; Swing ButtonUI contract does not change between JDK releases)
