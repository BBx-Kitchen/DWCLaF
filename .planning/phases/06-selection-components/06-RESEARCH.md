# Phase 6: Selection Components - Research

**Researched:** 2026-02-10
**Domain:** Custom Swing CheckBoxUI, RadioButtonUI, and ComboBoxUI delegates with DWC CSS token-derived custom icon/indicator painting
**Confidence:** HIGH

## Summary

This phase implements three custom ComponentUI delegates -- `DwcCheckBoxUI`, `DwcRadioButtonUI`, and `DwcComboBoxUI` -- that paint JCheckBox, JRadioButton, and JComboBox with DWC CSS token-derived styling. The existing infrastructure from Phases 1-5 provides all needed painting primitives: `PaintUtils` for rounded backgrounds and outlines, `FocusRingPainter` for focus rings, `StateColorResolver` for state-based color resolution and disabled opacity, plus the token-mapping pipeline and `DwcLookAndFeel` registration mechanism.

JCheckBox and JRadioButton both extend `AbstractButton`, so `StateColorResolver.resolve()` works directly for hover/pressed detection (rollover is already handled by `ButtonModel`). The key technical challenge is painting custom **icons** for the checkbox checkmark and radio dot indicator. In Swing's L&F architecture, checkbox and radio button appearance is controlled by a custom `Icon` implementation registered via `CheckBox.icon` / `RadioButton.icon` in UIDefaults. The UI delegate itself (`BasicRadioButtonUI`, which is the parent of both) handles the text/icon layout; the icon's `paintIcon()` method handles the visual indicator. FlatLaf uses this same architecture: `FlatCheckBoxIcon` paints the rounded-square box with checkmark path, and `FlatRadioButtonIcon` extends it to paint a circular indicator with center dot.

The ComboBox is more complex. `BasicComboBoxUI` provides popup management, keyboard navigation, and editor support. The custom delegate needs to: (1) paint a rounded border with styled dropdown arrow button, (2) provide a themed popup list (via custom `ListCellRenderer`), and (3) track hover/focus states. FlatLaf's `FlatComboBoxUI` overrides `createArrowButton()`, `paintCurrentValueBackground()`, and installs a custom popup with styled list. The DWC combobox is essentially a styled input field with a dropdown arrow and a popup listbox, reusing the `--dwc-input-*` tokens for the field and `--dwc-listbox-*` tokens for the popup.

**Primary recommendation:** Build three component pairs: (1) `DwcCheckBoxUI` extending `BasicRadioButtonUI` + `DwcCheckBoxIcon` implementing `Icon`, (2) `DwcRadioButtonUI` extending `BasicRadioButtonUI` + `DwcRadioButtonIcon` implementing `Icon`, and (3) `DwcComboBoxUI` extending `BasicComboBoxUI`. Register all three via `initClassDefaults()`. Use the established Phase 3 painting utilities for all rendering. Follow the FlatLaf architectural pattern of icon-based indicator painting for checkbox/radio and arrow-button customization for combobox.

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `javax.swing.plaf.basic.BasicRadioButtonUI` | JDK 21 | Base class for checkbox AND radio button delegates | Handles icon/text layout, keyboard mnemonics, accessibility; `BasicCheckBoxUI` extends this |
| `javax.swing.plaf.basic.BasicComboBoxUI` | JDK 21 | Base class for combobox delegate | Handles popup management, keyboard navigation, editor, ListCellRenderer integration |
| `javax.swing.Icon` | JDK 21 | Interface for custom checkbox/radio indicator painting | Standard Swing extension point; icon is painted by the UI delegate during layout |
| `com.dwc.laf.painting.PaintUtils` | Phase 3 | Rounded backgrounds, outlines, antialiasing | Already built and tested |
| `com.dwc.laf.painting.FocusRingPainter` | Phase 3 | Focus ring painting outside component shape | Already built and tested |
| `com.dwc.laf.painting.StateColorResolver` | Phase 3 | State-based color resolution and disabled opacity | Already built and tested; works with AbstractButton directly |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `java.awt.geom.Path2D` | JDK 21 | Checkmark path rendering | Drawing the checkmark shape inside the checkbox icon |
| `java.awt.geom.Ellipse2D` | JDK 21 | Radio dot indicator | Drawing the center circle in the radio button icon |
| `java.awt.BasicStroke` | JDK 21 | Checkmark stroke width | Rendering the checkmark path with appropriate line weight |
| `javax.swing.DefaultListCellRenderer` | JDK 21 | Base for themed popup list renderer | Styling the combobox dropdown list items |
| `javax.swing.plaf.basic.BasicComboPopup` | JDK 21 | Base class for styled popup | May be extended to customize popup border/background |
| `javax.swing.plaf.basic.BasicArrowButton` | JDK 21 | Reference for arrow button | Will be replaced with custom painted arrow button |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Custom `Icon` implementation for checkbox/radio | Override `paintIcon()` in the UI delegate | Custom Icon is the standard Swing L&F pattern; FlatLaf, Nimbus, and all modern L&Fs use it. Overriding paintIcon in the delegate is fragile. |
| Extending `BasicRadioButtonUI` for both | Extending `BasicCheckBoxUI` for checkbox | `BasicCheckBoxUI` literally extends `BasicRadioButtonUI` with only `getPropertyPrefix()` changed. Extending `BasicRadioButtonUI` directly is cleaner and is what FlatLaf does. |
| Custom `DwcComboBoxUI` extending `BasicComboBoxUI` | Starting from `ComboBoxUI` directly | Would lose popup management, keyboard navigation, editor support, and `ListCellRenderer` integration |

## Architecture Patterns

### Recommended Project Structure

```
src/main/java/com/dwc/laf/
  ui/
    DwcCheckBoxUI.java          # Custom CheckBoxUI delegate (extends BasicRadioButtonUI)
    DwcRadioButtonUI.java       # Custom RadioButtonUI delegate (extends BasicRadioButtonUI)
    DwcCheckBoxIcon.java        # Custom Icon for checkbox indicator
    DwcRadioButtonIcon.java     # Custom Icon for radio indicator
    DwcComboBoxUI.java          # Custom ComboBoxUI delegate (extends BasicComboBoxUI)
  DwcLookAndFeel.java           # Add CheckBoxUI, RadioButtonUI, ComboBoxUI in initClassDefaults()
                                 # Add initCheckBoxDefaults(), initRadioButtonDefaults(), initComboBoxDefaults()
```

### Pattern 1: Custom Checkbox/Radio Icon Architecture

**What:** The checkbox/radio indicator is a custom `Icon` registered via UIDefaults (`CheckBox.icon`, `RadioButton.icon`). The UI delegate (extending `BasicRadioButtonUI`) handles text/icon layout. The icon's `paintIcon(Component, Graphics, int, int)` paints the visual indicator -- box + checkmark for checkbox, circle + dot for radio.

**When to use:** This is THE pattern for checkbox and radio button painting in Swing L&Fs.

**Why it works:** `BasicRadioButtonUI.paint()` calls `SwingUtilities.layoutCompoundLabel()` to position the icon and text, then calls `icon.paintIcon()` to render the indicator. By providing a custom Icon, we control the indicator appearance without reimplementing layout logic.

**Example:**

```java
// DwcCheckBoxIcon implements Icon
public class DwcCheckBoxIcon implements Icon {

    private static final int ICON_SIZE = 18; // --dwc-size-3xs = 1.125rem = 18px at 16px base

    @Override
    public int getIconWidth() {
        return ICON_SIZE + UIManager.getInt("Component.focusWidth") * 2;
    }

    @Override
    public int getIconHeight() {
        return ICON_SIZE + UIManager.getInt("Component.focusWidth") * 2;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        AbstractButton b = (AbstractButton) c;
        ButtonModel model = b.getModel();
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            int fw = UIManager.getInt("Component.focusWidth");
            int bw = UIManager.getInt("Component.borderWidth");
            int arc = UIManager.getInt("CheckBox.arc");

            // 1. Determine state
            boolean selected = model.isSelected();
            boolean hover = model.isRollover();
            boolean pressed = model.isArmed() && model.isPressed();
            boolean focused = b.hasFocus();
            boolean enabled = b.isEnabled();

            // 2. Resolve colors based on state
            Color bg = resolveBackground(selected, hover, pressed, focused, enabled);
            Color border = resolveBorderColor(selected, hover, pressed, focused, enabled);

            // 3. Paint box background (inside focus ring area)
            float bx = x + fw;
            float by = y + fw;
            float bw2 = ICON_SIZE;
            float bh = ICON_SIZE;
            PaintUtils.paintRoundedBackground(g2, bx, by, bw2, bh, arc, bg);

            // 4. Paint box border
            Object[] saved = PaintUtils.setupPaintingHints(g2);
            g2.setColor(border);
            PaintUtils.paintOutline(g2, bx, by, bw2, bh, bw, arc);
            PaintUtils.restorePaintingHints(g2, saved);

            // 5. Paint checkmark (only when selected)
            if (selected) {
                paintCheckmark(g2, bx, by, bw2, bh);
            }

            // 6. Paint focus ring
            if (focused && enabled) {
                FocusRingPainter.paintFocusRing(g2, bx, by, bw2, bh,
                    arc, fw, UIManager.getColor("Component.focusRingColor"));
            }
        } finally {
            g2.dispose();
        }
    }

    private void paintCheckmark(Graphics2D g2, float x, float y, float w, float h) {
        Color checkColor = UIManager.getColor("CheckBox.icon.checkmarkColor");
        g2.setColor(checkColor != null ? checkColor : Color.WHITE);
        Object[] saved = PaintUtils.setupPaintingHints(g2);

        // Checkmark path (relative to icon bounds)
        Path2D.Float path = new Path2D.Float();
        float scale = w / 15f; // FlatLaf uses 15px base; scale to our icon size
        path.moveTo(x + 4.5f * scale, y + 7.5f * scale);
        path.lineTo(x + 6.6f * scale, y + 10f * scale);
        path.lineTo(x + 11.25f * scale, y + 3.5f * scale);

        g2.setStroke(new BasicStroke(1.9f * scale, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(path);
        PaintUtils.restorePaintingHints(g2, saved);
    }
}
```

### Pattern 2: Radio Button Dot Indicator

**What:** Same architecture as checkbox, but the icon paints a circle (border-radius: 50%) instead of a rounded rectangle, and paints a filled center dot instead of a checkmark path.

**When to use:** For JRadioButton indicator painting.

**Example:**

```java
// DwcRadioButtonIcon - similar to DwcCheckBoxIcon but circular
public class DwcRadioButtonIcon implements Icon {

    private static final int ICON_SIZE = 18;

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        // ... same state resolution as checkbox ...

        // 3. Paint circular background (arc = ICON_SIZE for full circle)
        PaintUtils.paintRoundedBackground(g2, bx, by, ICON_SIZE, ICON_SIZE, ICON_SIZE, bg);

        // 4. Paint circular border
        PaintUtils.paintOutline(g2, bx, by, ICON_SIZE, ICON_SIZE, bw, ICON_SIZE);

        // 5. Paint center dot (only when selected)
        if (selected) {
            float dotDiameter = 8f; // proportional to icon size
            float dotX = bx + (ICON_SIZE - dotDiameter) / 2f;
            float dotY = by + (ICON_SIZE - dotDiameter) / 2f;
            g2.setColor(dotColor);
            g2.fill(new Ellipse2D.Float(dotX, dotY, dotDiameter, dotDiameter));
        }

        // 6. Focus ring (circular)
        if (focused && enabled) {
            FocusRingPainter.paintFocusRing(g2, bx, by, ICON_SIZE, ICON_SIZE,
                ICON_SIZE, fw, focusRingColor);
        }
    }
}
```

### Pattern 3: CheckBoxUI/RadioButtonUI Delegate Structure

**What:** The UI delegates extend `BasicRadioButtonUI` (same as FlatLaf), override `getPropertyPrefix()`, `installDefaults()`, and register the custom icon. The paint method is mostly inherited from `BasicRadioButtonUI` -- the custom icon handles the visual indicator.

**When to use:** For both DwcCheckBoxUI and DwcRadioButtonUI.

**Example:**

```java
public class DwcCheckBoxUI extends BasicRadioButtonUI {

    public static ComponentUI createUI(JComponent c) {
        return new DwcCheckBoxUI();
    }

    @Override
    public String getPropertyPrefix() {
        return "CheckBox.";
    }

    @Override
    protected void installDefaults(AbstractButton b) {
        super.installDefaults(b);

        // Set opaque=false for custom painting
        LookAndFeel.installProperty(b, "opaque", false);

        // Enable rollover for hover state detection
        b.setRolloverEnabled(true);

        // Install custom icon (if not already set by application)
        if (b.getIcon() == null) {
            b.setIcon(new DwcCheckBoxIcon());
        }
    }
}
```

### Pattern 4: ComboBox Delegate Structure

**What:** The ComboBox delegate extends `BasicComboBoxUI`, overrides `createArrowButton()` to provide a custom styled arrow button, overrides `paintCurrentValueBackground()` for rounded background, installs hover/focus listeners, and provides a themed popup. The field area reuses `--dwc-input-*` token styling patterns from Phase 5.

**When to use:** For the DwcComboBoxUI.

**Example:**

```java
public class DwcComboBoxUI extends BasicComboBoxUI {

    private Color background;
    private Color hoverBackground;
    private Color arrowColor;
    private Color hoverArrowColor;
    private int arc;
    private int focusWidth;
    private int borderWidth;
    private Color focusRingColor;
    private boolean hover;
    private MouseListener hoverListener;

    public static ComponentUI createUI(JComponent c) {
        return new DwcComboBoxUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        LookAndFeel.installProperty(comboBox, "opaque", false);

        // Read UIDefaults from CSS tokens
        background = UIManager.getColor("ComboBox.background");
        // ... etc

        comboBox.setRenderer(new DwcComboBoxRenderer());
    }

    @Override
    protected JButton createArrowButton() {
        // Return custom painted arrow button (not BasicArrowButton)
        return new DwcComboBoxArrowButton();
    }

    @Override
    public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
        // Paint rounded background matching DWC input styling
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            PaintUtils.paintRoundedBackground(g2, bounds.x, bounds.y,
                bounds.width, bounds.height, arc, background);
        } finally {
            g2.dispose();
        }
    }
}
```

### Pattern 5: ComboBox Arrow Button

**What:** A custom JButton subclass that paints just the dropdown arrow (chevron or triangle), without its own background/border (those are painted by the combobox UI delegate). FlatLaf uses `Component.arrowType` to choose between chevron and triangle styles.

**When to use:** Returned from `createArrowButton()` in DwcComboBoxUI.

**Example:**

```java
// Inner class or separate class
private static class DwcComboBoxArrowButton extends JButton {
    DwcComboBoxArrowButton() {
        setName("ComboBox.arrowButton");
        setRequestFocusEnabled(false);
        setFocusable(false);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder());
        setContentAreaFilled(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Object[] saved = PaintUtils.setupPaintingHints(g2);
            Color arrowColor = UIManager.getColor("ComboBox.buttonArrowColor");
            g2.setColor(arrowColor);

            int w = getWidth();
            int h = getHeight();
            // Paint chevron arrow centered in button
            paintChevronArrow(g2, w, h);
            PaintUtils.restorePaintingHints(g2, saved);
        } finally {
            g2.dispose();
        }
    }

    private void paintChevronArrow(Graphics2D g2, int w, int h) {
        float arrowSize = 8f;
        float cx = w / 2f;
        float cy = h / 2f;
        Path2D.Float arrow = new Path2D.Float();
        arrow.moveTo(cx - arrowSize / 2, cy - arrowSize / 4);
        arrow.lineTo(cx, cy + arrowSize / 4);
        arrow.lineTo(cx + arrowSize / 2, cy - arrowSize / 4);
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(arrow);
    }
}
```

### Pattern 6: ComboBox Popup List Styling

**What:** A custom `ListCellRenderer` that paints list items with DWC-themed selection highlight and hover colors. Installed during `installDefaults()`.

**When to use:** For the combobox dropdown popup.

**Example:**

```java
private static class DwcComboBoxRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);

        if (isSelected) {
            label.setBackground(UIManager.getColor("ComboBox.selectionBackground"));
            label.setForeground(UIManager.getColor("ComboBox.selectionForeground"));
        } else {
            label.setBackground(UIManager.getColor("ComboBox.popupBackground"));
            label.setForeground(UIManager.getColor("ComboBox.foreground"));
        }

        // Add padding
        label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        return label;
    }
}
```

### Anti-Patterns to Avoid

- **Overriding `paint()` in the checkbox/radio UI delegate to draw the indicator:** The indicator should be an `Icon`. `BasicRadioButtonUI.paint()` handles icon/text layout correctly. Override only if you need custom text rendering (which we do not).
- **Sharing a single Icon instance across all checkboxes:** The Icon's `paintIcon()` receives the `Component` parameter, so it CAN read state from the component. However, if the Icon caches per-component state internally, it must not be shared. Since our Icon reads state from the `Component` parameter each paint call, a single shared instance would work, but per-component instances are safer for the DWC architecture (consistent with `DwcButtonUI` pattern).
- **Using `BasicCheckBoxUI` as the parent:** `BasicCheckBoxUI` literally extends `BasicRadioButtonUI` with only `getPropertyPrefix()` changed to return "CheckBox.". Extending `BasicRadioButtonUI` directly and overriding `getPropertyPrefix()` is cleaner.
- **Forgetting `setRolloverEnabled(true)`:** Without this, `ButtonModel.isRollover()` never returns true, so hover states never activate on checkbox/radio buttons.
- **Painting the combobox popup background in `paintCurrentValueBackground()`:** This method paints the FIELD background only, not the popup. The popup is a separate `JPopupMenu` with its own painting.
- **Not installing a custom renderer on the combobox:** Without a custom `ListCellRenderer`, the popup list uses the default Metal/Basic styling, which clashes with DWC theming.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Rounded background fill | Manual `g2.fill(RoundRectangle2D)` | `PaintUtils.paintRoundedBackground()` | Handles antialiasing, null checks, shape degeneration |
| Focus ring painting | Manual Path2D construction | `FocusRingPainter.paintFocusRing()` | Already handles outer arc expansion, null checks, hint management |
| Disabled opacity | Manual alpha manipulation | `StateColorResolver.paintWithOpacity()` | Wraps composite save/restore correctly |
| Outline/border painting | `g2.draw()` with BasicStroke | `PaintUtils.paintOutline()` | Even-odd fill avoids stroke centering artifacts |
| State color resolution | Custom if/else chain | `StateColorResolver.resolve()` | Centralized priority chain already tested |
| Icon/text layout | Manual position calculation | `BasicRadioButtonUI.paint()` (inherited) | Handles all alignment, text clipping, mnemonic index |
| Popup keyboard navigation | Custom key listeners | `BasicComboBoxUI` popup infrastructure | Handles up/down, enter, escape, page up/down, type-ahead |

**Key insight:** CheckBox and RadioButton are mostly Icon painting problems. The UI delegate's primary job is registering the custom icon and ensuring properties are installed. The ComboBox is more involved but still heavily leverages `BasicComboBoxUI` infrastructure for popup/editor/navigation.

## Common Pitfalls

### Pitfall 1: Icon Not Installed (System Default Checkbox Appears)

**What goes wrong:** JCheckBox shows the platform's default checkbox indicator instead of the custom DWC-styled one.
**Why it happens:** The custom icon was not registered in UIDefaults or was not installed in `installDefaults()`.
**How to avoid:** Register the icon in `DwcLookAndFeel.initCheckBoxDefaults()` via `table.put("CheckBox.icon", new DwcCheckBoxIcon())`. Also install it in `DwcCheckBoxUI.installDefaults()` as a fallback.
**Warning signs:** Checkbox looks like the OS native checkbox or the Basic L&F checkbox.

### Pitfall 2: Rollover Not Enabled on Checkbox/Radio

**What goes wrong:** Hover state never activates; no visual feedback on mouse hover.
**Why it happens:** `AbstractButton.setRolloverEnabled(false)` is the default. `BasicRadioButtonUI` does not enable it.
**How to avoid:** Call `b.setRolloverEnabled(true)` in `installDefaults()`.
**Warning signs:** `ButtonModel.isRollover()` always returns false when hovering over checkbox/radio.

### Pitfall 3: Focus Ring Clipped on Checkbox/Radio Icon

**What goes wrong:** The focus ring around the checkbox/radio indicator is partially cut off.
**Why it happens:** The icon's `getIconWidth()` / `getIconHeight()` do not include space for the focus ring. The layout allocates only the icon's reported size.
**How to avoid:** Icon dimensions must include `focusWidth * 2` (e.g., `ICON_SIZE + focusWidth * 2`). The focus ring paints in the reserved outer area, the indicator box/circle paints inside.
**Warning signs:** Focus ring visible on some sides but cut on others.

### Pitfall 4: Checkmark Not Visible on Selected Checkbox

**What goes wrong:** Checkbox shows the accent background but no visible checkmark.
**Why it happens:** The checkmark color is the same as the background, or the checkmark path coordinates are wrong.
**How to avoid:** Verify checkmark color contrasts with the selected background (DWC uses white `--dwc-color-on-primary-text` on primary blue background). Test with a visible stroke width.
**Warning signs:** Checkbox appears as a solid colored square when selected.

### Pitfall 5: ComboBox Arrow Button Has Wrong Size

**What goes wrong:** The arrow button is too wide/narrow, or the field text is clipped.
**Why it happens:** `BasicComboBoxUI` calculates the arrow button width from `createArrowButton()`. If the custom button doesn't return a proper preferred size, the layout breaks.
**How to avoid:** Override `getPreferredSize()` on the arrow button to return a reasonable size (e.g., 36x36 matching `--dwc-size-m`). Or let `BasicComboBoxUI` handle sizing.
**Warning signs:** Arrow button is tiny or takes up most of the combobox width.

### Pitfall 6: ComboBox Popup Styling Not Applied

**What goes wrong:** The popup list uses default styling (white background, blue highlight) instead of DWC theming.
**Why it happens:** The custom renderer was not installed, or the popup overrides the renderer.
**How to avoid:** Install the custom renderer in `installDefaults()` AND set `ComboBox.selectionBackground` / `ComboBox.selectionForeground` in UIDefaults so even unthemed renderers pick up the colors.
**Warning signs:** Popup looks like default Metal/Basic L&F.

### Pitfall 7: ComboBox Hover State Not Tracked

**What goes wrong:** ComboBox field never shows hover visual feedback.
**Why it happens:** JComboBox is not an `AbstractButton`, so `StateColorResolver.resolve()` cannot detect rollover. A MouseListener must be installed (same pattern as Phase 5's `DwcTextFieldUI`).
**How to avoid:** Install a `MouseAdapter` that sets a client property or boolean flag for hover state. Repaint on enter/exit.
**Warning signs:** ComboBox border/background never changes on mouse hover.

### Pitfall 8: ComboBox Border Painted Twice (by Border and UI Delegate)

**What goes wrong:** Double border lines visible on the combobox.
**Why it happens:** Both the custom border AND the UI delegate's `paint()` method draw the border outline.
**How to avoid:** Either paint the border in the border class only (like `DwcTextFieldBorder`) OR in the UI delegate only (not both). The established pattern is border-paints-border.
**Warning signs:** Thicker-than-expected border, or double outlines visible.

### Pitfall 9: RadioButton Border Radius Not 50%

**What goes wrong:** Radio button indicator looks like a rounded square instead of a circle.
**Why it happens:** Using `CheckBox.arc` (which is a small radius like 4px) instead of using the full icon diameter for the arc.
**How to avoid:** Radio button icon must use `arc = ICON_SIZE` (or `arc = min(width, height)`) to create a perfect circle. `PaintUtils.createRoundedShape()` automatically degenerates to `Ellipse2D` when `arc >= min(w, h)`.
**Warning signs:** Radio button has square-ish corners instead of being perfectly circular.

## Code Examples

### DWC CSS Token Values for Checkbox (from dwc-checkbox.scss)

```scss
// Unchecked state:
background: var(--dwc-checkbox-background, var(--dwc-color-default));         // hsl(211, 38%, 90%)
border-color: var(--dwc-checkbox-border-color, var(--dwc-color-default-dark)); // hsl(211, 38%, 85%)
border-radius: var(--dwc-border-radius);                                       // 0.25em ~ 4px

// Hover (unchecked):
background: var(--dwc-checkbox-hover-background, var(--dwc-color-default-light));  // hsl(211, 38%, 95%)
border-color: var(--dwc-checkbox-hover-border-color, var(--dwc-color-primary));    // hsl(211, 100%, 40%) BLUE

// Checked:
background: var(--dwc-checkbox-checked-background, var(--dwc-color-primary));      // hsl(211, 100%, 40%) BLUE
color: var(--dwc-checkbox-checked-color, var(--dwc-color-on-primary-text));        // white

// Checked + hover:
background: var(--dwc-checkbox-hover-checked-background, var(--dwc-color-primary-light));  // hsl(211, 100%, 45%)
border-color: var(--dwc-checkbox-hover-checked-border-color, var(--dwc-color-primary));    // hsl(211, 100%, 40%)

// Size:
--dwc-checkbox-size: var(--dwc-size-3xs); // 1.125rem = 18px at 16px base font

// Focus ring:
box-shadow: var(--dwc-checkbox-focus-ring, var(--dwc-focus-ring-default));
```

### DWC CSS Token Values for Radio (from dwc-radio.scss)

```scss
// Same color scheme as checkbox, but:
border-radius: 50%; // Always circular
--dwc-radio-size: var(--dwc-size-3xs); // 1.125rem = 18px

// Unchecked:
background: var(--dwc-radio-background, var(--dwc-color-default));
border-color: var(--dwc-radio-border-color, var(--dwc-color-default-dark));

// Checked:
background: var(--dwc-radio-checked-background, var(--dwc-color-primary));
border-color: var(--dwc-radio-checked-border-color, var(--dwc-color-primary));
color: var(--dwc-radio-checked-color, var(--dwc-color-on-primary-text)); // dot color
```

### Token-to-UIDefaults Mapping for Selection Components

```properties
# ALREADY in token-mapping.properties:
--dwc-color-primary = color:CheckBox.icon.checkmarkColor    # primary blue for checkmark
--dwc-border-radius = int:CheckBox.arc, int:ComboBox.arc    # already mapped

# NEED TO ADD for checkbox/radio:
--dwc-color-default = color:CheckBox.icon.background, color:RadioButton.icon.background
--dwc-color-default-dark = color:CheckBox.icon.borderColor, color:RadioButton.icon.borderColor
--dwc-color-default-light = color:CheckBox.icon.hoverBackground, color:RadioButton.icon.hoverBackground
--dwc-color-primary = color:CheckBox.icon.selectedBackground, color:RadioButton.icon.selectedBackground, color:CheckBox.icon.selectedBorderColor, color:RadioButton.icon.selectedBorderColor
--dwc-color-on-primary-text = color:CheckBox.icon.checkmarkColor, color:RadioButton.icon.dotColor
--dwc-color-primary-light = color:CheckBox.icon.hoverSelectedBackground, color:RadioButton.icon.hoverSelectedBackground

# NEED TO ADD for combobox:
--dwc-input-background = color:ComboBox.background
--dwc-input-border-color = color:ComboBox.borderColor
--dwc-input-hover-border-color = color:ComboBox.hoverBorderColor
--dwc-color-primary = color:ComboBox.selectionBackground
--dwc-color-on-primary-text = color:ComboBox.selectionForeground
--dwc-color-body-text = color:ComboBox.foreground
--dwc-color-default-dark = color:ComboBox.buttonArrowColor
--dwc-color-primary = color:ComboBox.buttonHoverArrowColor
```

### UIDefaults Keys Required by Phase 6

```
# CheckBox Icon Colors
CheckBox.icon.background              -> unchecked background (default gray)
CheckBox.icon.borderColor             -> unchecked border color
CheckBox.icon.hoverBackground         -> unchecked hover background
CheckBox.icon.hoverBorderColor        -> unchecked hover border color (primary blue)
CheckBox.icon.selectedBackground      -> checked background (primary blue)
CheckBox.icon.selectedBorderColor     -> checked border color (primary blue)
CheckBox.icon.checkmarkColor          -> checkmark color (white)
CheckBox.icon.hoverSelectedBackground -> checked+hover background (primary light)
CheckBox.icon.focusColor              -> focus ring color (= Component.focusRingColor)

# CheckBox Dimensions (already in UIDefaults)
CheckBox.arc                          -> icon corner arc (from --dwc-border-radius)
Component.focusWidth                  -> focus ring width
Component.borderWidth                 -> border thickness
Component.disabledOpacity             -> disabled opacity

# RadioButton Icon Colors (same scheme, different keys)
RadioButton.icon.background           -> unchecked background
RadioButton.icon.borderColor          -> unchecked border color
RadioButton.icon.selectedBackground   -> checked background
RadioButton.icon.selectedBorderColor  -> checked border color
RadioButton.icon.dotColor             -> center dot color (white)
RadioButton.icon.centerDiameter       -> dot diameter (8px default)

# ComboBox Colors
ComboBox.background                   -> field background
ComboBox.foreground                   -> text color
ComboBox.borderColor                  -> field border color
ComboBox.hoverBorderColor             -> hover/focus border color
ComboBox.buttonArrowColor             -> arrow color
ComboBox.buttonHoverArrowColor        -> arrow hover color
ComboBox.selectionBackground          -> popup selected item background
ComboBox.selectionForeground          -> popup selected item text
ComboBox.popupBackground              -> popup list background
ComboBox.focusRingColor               -> focus ring (= Component.focusRingColor)

# ComboBox Dimensions
ComboBox.arc                          -> corner arc (from --dwc-border-radius)
ComboBox.padding                      -> text padding (Insets)
```

### L&F Registration

```java
// In DwcLookAndFeel.initClassDefaults():
@Override
protected void initClassDefaults(UIDefaults table) {
    super.initClassDefaults(table);
    table.put("ButtonUI", "com.dwc.laf.ui.DwcButtonUI");
    table.put("TextFieldUI", "com.dwc.laf.ui.DwcTextFieldUI");
    table.put("CheckBoxUI", "com.dwc.laf.ui.DwcCheckBoxUI");       // NEW
    table.put("RadioButtonUI", "com.dwc.laf.ui.DwcRadioButtonUI"); // NEW
    table.put("ComboBoxUI", "com.dwc.laf.ui.DwcComboBoxUI");       // NEW
}
```

### CheckBox Icon Size Derivation

```
DWC --dwc-checkbox-size = var(--dwc-size-3xs) = 1.125rem
At 16px base font: 1.125 * 16 = 18px

FlatLaf ICON_SIZE = 15px

Recommendation: Use 16px as the icon indicator size (content area).
With focus ring: 16 + 3*2 = 22px total icon dimension.

Rationale: 18px is the DWC web component size but includes padding.
The actual checkbox square in the DWC web component (excluding padding/focus-ring)
is closer to 16px. This also aligns better with standard desktop sizes.
However, this can be tuned during implementation -- the exact size should be
visually tested and adjusted to match DWC web component appearance.
```

### Checkmark Path Geometry

```
FlatLaf checkmark path (in 15px coordinate space):
  moveTo(4.5, 7.5)
  lineTo(6.6, 10.0)
  lineTo(11.25, 3.5)
  strokeWidth = 1.9, CAP_ROUND, JOIN_ROUND

For a 16px icon, scale factor = 16/15 = 1.067:
  moveTo(4.8, 8.0)
  lineTo(7.04, 10.67)
  lineTo(12.0, 3.73)
  strokeWidth = 2.0

These are approximate starting points. The exact coordinates should be
visually tuned to match the DWC web component's SVG checkmark.
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| System default checkbox appearance | Custom `Icon` painting checkmark with accent colors | FlatLaf/modern L&F pattern | Web-matching checkbox appearance |
| `BasicRadioButtonUI` default icon | Custom `Icon` with circular indicator + dot | FlatLaf pattern | Consistent with DWC web radio styling |
| `BasicArrowButton` for combobox dropdown | Custom arrow button with chevron/triangle path | FlatLaf pattern | Clean, themed dropdown arrow |
| Default `ListCellRenderer` for popup | Themed renderer with selection highlight colors | Standard L&F practice | Popup matches overall theme |
| No hover state on non-editable combobox | MouseListener for hover tracking (client property pattern) | Phase 5 pattern (carried forward) | Visual feedback on hover |

**Deprecated/outdated:**
- `BasicCheckBoxUI` as parent class: FlatLaf extends `BasicRadioButtonUI` directly for both checkbox and radio, overriding only `getPropertyPrefix()`.
- Dashed focus rectangle on checkbox/radio: Replace with semi-transparent focus ring matching DWC pattern.
- System-painted checkbox/radio indicators: Replace with custom Icon painting for full control.

## Open Questions

1. **Checkbox indicator size (16px vs 18px vs 15px)**
   - What we know: DWC uses `--dwc-size-3xs = 1.125rem = 18px`. FlatLaf uses 15px. Standard desktop checkboxes are typically 13-16px.
   - What's unclear: Whether 18px matches the visual appearance of the DWC web checkbox well on desktop, or whether it's too large.
   - Recommendation: Start with 16px indicator (content area, excluding focus ring). Visually test and adjust. The icon size is a single constant that's easy to change.

2. **ComboBox border: new DwcComboBoxBorder or reuse DwcTextFieldBorder**
   - What we know: ComboBox field styling matches `--dwc-input-*` tokens (same as TextField). `DwcTextFieldBorder` already handles focus-width reservation, border outline, and hover/focus state colors.
   - What's unclear: Whether `DwcTextFieldBorder` can be reused directly or needs adaptation for the arrow button area.
   - Recommendation: Reuse `DwcTextFieldBorder` for the combobox. The border paints around the entire component (including the arrow button area). If the arrow button needs a separator line, paint it in the UI delegate or arrow button, not in the border. This avoids creating a third nearly-identical border class.

3. **ComboBox popup border radius**
   - What we know: DWC uses `--dwc-border-radius` for the popup. Swing's `BasicComboPopup` uses a `JScrollPane` with a `Border`.
   - What's unclear: Whether to paint a rounded border on the popup or keep it rectangular. Rounded popup borders in Swing are complex (require heavy-weight popup for transparency).
   - Recommendation: Keep the popup rectangular with a simple `LineBorder` using the DWC border color. Rounded popup corners are a cosmetic enhancement that can be deferred -- they require non-trivial popup component wrapping.

4. **Indeterminate checkbox state**
   - What we know: DWC supports an indeterminate state (horizontal line instead of checkmark). Swing's `JCheckBox` does not natively support tri-state.
   - What's unclear: Whether to support indeterminate state now.
   - Recommendation: Defer indeterminate state to a later phase. The success criteria only mention checked/unchecked. If needed, a client property `"dwc.indeterminate"` can be checked by the icon and a horizontal line painted instead of a checkmark.

5. **Token mapping conflicts**
   - What we know: `--dwc-color-primary` is already mapped to `CheckBox.icon.checkmarkColor` in token-mapping.properties. But DWC uses `--dwc-color-on-primary-text` (white) for the checkmark color, not `--dwc-color-primary` (blue).
   - What's unclear: Whether the existing mapping is intentional or a mistake.
   - Recommendation: The checkmark color on a selected checkbox should be white (contrasting with the blue background). Change the mapping: `--dwc-color-on-primary-text = color:CheckBox.icon.checkmarkColor`. The existing `--dwc-color-primary` mapping for this key is incorrect -- primary blue on primary blue background would be invisible.

6. **ComboBox editable vs non-editable**
   - What we know: DWC combobox supports both modes. Swing distinguishes editable (shows text field) vs non-editable (shows label). `BasicComboBoxUI` handles this.
   - What's unclear: Whether to differentiate styling for editable mode in this phase.
   - Recommendation: Support both modes but do not differentiate styling. The field background and border are the same regardless. Editable mode's text field will use the combobox's font and colors, handled by `BasicComboBoxUI.createEditor()`.

## Sources

### Primary (HIGH confidence)
- FlatLaf [CheckBox documentation](https://www.formdev.com/flatlaf/components/checkbox/) - UIDefaults keys, icon properties
- FlatLaf [CheckBox Icon documentation](https://www.formdev.com/flatlaf/components/icons/#checkboxicon) - Complete icon UIDefaults keys for all states
- FlatLaf [ComboBox documentation](https://www.formdev.com/flatlaf/components/combobox/) - UIDefaults keys, popup styling, arrow button
- FlatLaf `FlatCheckBoxUI` source (GitHub main) - Architecture: extends `FlatRadioButtonUI` which extends `BasicRadioButtonUI`
- FlatLaf `FlatCheckBoxIcon` source (GitHub main) - Checkmark path coordinates (4.5,7.5 -> 6.6,10 -> 11.25,3.5), ICON_SIZE=15, stroke width 1.9
- FlatLaf `FlatRadioButtonIcon` source (GitHub main) - Center dot: `Ellipse2D`, centerDiameter=8
- DWC `dwc-checkbox.scss` (local project) - All checkbox CSS custom properties and states
- DWC `dwc-radio.scss` (local project) - All radio CSS custom properties and states (circular, 50% border-radius)
- DWC `dwc-combobox.scss` (local project) - Combobox structure (field + suffix icon + separator)
- DWC `dwc-listbox.scss` (local project) - Listbox item styling, selection colors, hover colors
- Project Phase 3 utilities (local project) - PaintUtils, FocusRingPainter, StateColorResolver
- Project Phase 4 DwcButtonUI (local project) - Proven per-component UI delegate pattern
- Project Phase 5 DwcTextFieldUI (local project) - Hover tracking via MouseListener, border reuse pattern
- DWC `default-light.css` (local project) - All token values
- DWC `token-mapping.properties` (local project) - Existing mappings

### Secondary (MEDIUM confidence)
- OpenJDK `BasicRadioButtonUI` JDK 21 - Paint pipeline, icon/text layout, installDefaults contract
- OpenJDK `BasicComboBoxUI` JDK 21 - Popup management, createArrowButton, createRenderer, editor lifecycle
- FlatLaf [Customizing documentation](https://www.formdev.com/flatlaf/customizing/) - General UIDefaults customization patterns

### Tertiary (LOW confidence)
- None -- all findings verified against source code or official documentation

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All classes are JDK standard library or already-implemented Phase 3/4/5 utilities; FlatLaf reference architecture is well-understood
- Architecture (CheckBox/Radio): HIGH - FlatLaf's Icon-based approach is production-proven; BasicRadioButtonUI contract is stable; DWC SCSS styling clearly maps to Icon states
- Architecture (ComboBox): MEDIUM - More moving parts (popup, arrow button, renderer, editor); BasicComboBoxUI has many hooks. FlatLaf validates the approach but ComboBox UI delegates are inherently more complex
- Pitfalls: HIGH - Icon sizing/clipping, rollover enabling, checkmark color contrast, and popup styling issues are all documented and verifiable
- Token mapping: HIGH - Complete analysis of existing and needed mappings; one incorrect mapping identified (checkmark color)

**Research date:** 2026-02-10
**Valid until:** 2026-03-12 (stable domain; Swing component UI contracts do not change between JDK releases)
