# Phase 5: Text Input Components - Research

**Researched:** 2026-02-10
**Domain:** Custom Swing TextFieldUI delegate painting with CSS design tokens (BasicTextUI, BasicTextFieldUI, placeholder text, focus/hover listeners)
**Confidence:** HIGH

## Summary

This phase implements a custom `DwcTextFieldUI` extending `BasicTextFieldUI` that paints JTextField with DWC CSS token-derived appearance: rounded border, colored background, placeholder text, hover/focus state transitions, and a focus ring. The existing infrastructure from Phases 1-4 provides nearly everything needed: `PaintUtils` for rounded backgrounds and outlines, `FocusRingPainter` for focus rings, `StateColorResolver` for disabled opacity, and the token-mapping pipeline for populating UIDefaults from CSS tokens.

The key architectural difference from the button delegate is that `BasicTextFieldUI` extends `BasicTextUI`, which uses a fundamentally different painting pipeline than `BasicButtonUI`. Text components use `paintSafely()` -> `paintBackground()` -> view hierarchy -> caret. The `paint()` method in `BasicTextUI` is **final** and cannot be overridden; all custom painting must go through `paintSafely()` and `paintBackground()`. Additionally, JTextField is not an `AbstractButton`, so `StateColorResolver.resolve()` cannot detect hover/rollover state -- a `MouseListener` must be installed on the text field to track hover state manually.

The DWC CSS defines comprehensive input tokens: `--dwc-input-background` (default-light), `--dwc-input-border-color` (default-dark), `--dwc-input-placeholder-color` (gray-60), `--dwc-input-hover-background` (default-light), `--dwc-input-hover-border-color` (primary), plus the shared `--dwc-focus-ring-default` and `--dwc-border-radius`. Some of these are already mapped in `token-mapping.properties` but several are missing (hover background, hover border color). A custom `DwcTextFieldBorder` (analogous to `DwcButtonBorder`) will handle border insets including focus ring reservation and border painting.

**Primary recommendation:** Build `DwcTextFieldUI` (extends `BasicTextFieldUI`) + `DwcTextFieldBorder` (extends `AbstractBorder`), register via `initClassDefaults()`, override `paintSafely()` for background/placeholder/focus-ring painting, install a `MouseListener` for hover state tracking, use a client property `JTextField.placeholderText` for placeholder text support, and add missing token mappings to `token-mapping.properties`.

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `javax.swing.plaf.basic.BasicTextFieldUI` | JDK 21 | Base class for text field delegate | Standard Swing extension point; handles caret, selection, text layout, model interaction, thread safety |
| `javax.swing.plaf.basic.BasicTextUI` | JDK 21 | Parent class providing `paintSafely()` / `paintBackground()` pipeline | The paint pipeline for all text components; `paint()` is final |
| `javax.swing.border.AbstractBorder` | JDK 21 | Base class for custom text field border | Same pattern proven in Phase 4's `DwcButtonBorder` |
| `com.dwc.laf.painting.PaintUtils` | Phase 3 | Rounded background, outline painting, antialiasing | Already built and tested |
| `com.dwc.laf.painting.FocusRingPainter` | Phase 3 | Focus ring painting outside component shape | Already built and tested |
| `com.dwc.laf.painting.StateColorResolver` | Phase 3 | Disabled opacity via `paintWithOpacity()` | Already built and tested |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `java.awt.event.MouseAdapter` | JDK 21 | Hover state tracking via `mouseEntered`/`mouseExited` | For hover state detection (JTextField has no rollover model) |
| `java.awt.event.FocusListener` | JDK 21 | Focus state change notification for repaint | Already handled by `BasicTextUI`; may need explicit repaint trigger for border/focus-ring changes |
| `javax.swing.plaf.ColorUIResource` | JDK 21 | UIResource contract compliance | All colors installed by L&F must be wrapped |
| `javax.swing.plaf.InsetsUIResource` | JDK 21 | UIResource contract for border insets | Border insets from L&F border |
| `javax.swing.SwingUtilities` | JDK 21 | `getClippedString()` for placeholder text clipping | When placeholder text exceeds field width |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Extending `BasicTextFieldUI` | Starting from `TextUI` directly | Would lose all caret, selection, keyboard, and accessibility handling -- enormous amount of work |
| Custom `DwcTextFieldBorder` | Reusing `DwcButtonBorder` | Text fields have different margin defaults and may later need component-specific border behavior |
| MouseListener for hover | PropertyChangeListener on a "hover" client property | MouseListener is simpler, more direct, and what FlatLaf uses |
| Client property for placeholder | Subclass `JTextField` with a placeholder field | Client property is more flexible, follows FlatLaf/SwingX pattern, works with any existing JTextField |

## Architecture Patterns

### Recommended Project Structure

```
src/main/java/com/dwc/laf/
  ui/
    DwcTextFieldUI.java          # Custom TextFieldUI delegate
    DwcTextFieldBorder.java      # Custom border with focus-width-aware insets
  DwcLookAndFeel.java            # Add "TextFieldUI" registration in initClassDefaults()
                                  # Add initTextFieldDefaults() in initComponentDefaults()
```

### Pattern 1: TextFieldUI Delegate Structure

**What:** A custom TextFieldUI that overrides `paintSafely()` (NOT `paint()`, which is final in BasicTextUI) to paint background, placeholder text, and focus ring. Delegates text/caret/highlight rendering to the parent `BasicTextUI.paintSafely()`.
**When to use:** This is THE pattern for this phase.
**Example:**

```java
// Source: FlatLaf FlatTextFieldUI architecture + BasicTextUI contract
public class DwcTextFieldUI extends BasicTextFieldUI {

    // Colors from UIDefaults
    private Color background;
    private Color foreground;
    private Color borderColor;
    private Color hoverBackground;
    private Color hoverBorderColor;
    private Color focusedBackground;
    private Color placeholderForeground;
    private Color focusRingColor;

    // Dimensions
    private int arc;
    private int focusWidth;
    private int borderWidth;
    private float disabledOpacity;

    // Hover tracking
    private boolean hover;
    private MouseListener hoverListener;
    private FocusListener focusRepaintListener;

    public static ComponentUI createUI(JComponent c) {
        return new DwcTextFieldUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        // Read UIDefaults populated from CSS tokens
        background = UIManager.getColor("TextField.background");
        // ... etc
    }

    @Override
    protected void paintSafely(Graphics g) {
        JTextComponent c = getComponent();
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            // 1. Paint rounded background (replaces paintBackground)
            paintDwcBackground(g2, c);

            // 2. Paint placeholder text (if empty and not focused)
            paintPlaceholder(g2, c);

            // 3. Let parent paint text, highlights, caret
            // (Must call parent paintSafely with the ORIGINAL graphics
            //  since parent does clip setup)
        } finally {
            g2.dispose();
        }

        // 4. Call super to paint text content (with original Graphics)
        super.paintSafely(g);

        // 5. Paint focus ring on top
        Graphics2D g2Focus = (Graphics2D) g.create();
        try {
            if (c.hasFocus()) {
                paintFocusRing(g2Focus, c);
            }
        } finally {
            g2Focus.dispose();
        }
    }
}
```

### Pattern 2: Hover State via MouseListener

**What:** JTextField does not have a rollover model like AbstractButton. Install a MouseListener that sets a boolean `hover` flag and triggers repaint on enter/exit.
**When to use:** Always -- this is required for hover state visual feedback.
**Example:**

```java
// Source: FlatLaf hover tracking pattern
@Override
protected void installListeners() {
    super.installListeners();

    hoverListener = new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
            hover = true;
            getComponent().repaint();
        }
        @Override
        public void mouseExited(MouseEvent e) {
            hover = false;
            getComponent().repaint();
        }
    };
    getComponent().addMouseListener(hoverListener);

    focusRepaintListener = new FocusListener() {
        @Override
        public void focusGained(FocusEvent e) {
            getComponent().repaint();
        }
        @Override
        public void focusLost(FocusEvent e) {
            getComponent().repaint();
        }
    };
    getComponent().addFocusListener(focusRepaintListener);
}

@Override
protected void uninstallListeners() {
    super.uninstallListeners();
    getComponent().removeMouseListener(hoverListener);
    getComponent().removeFocusListener(focusRepaintListener);
    hoverListener = null;
    focusRepaintListener = null;
}
```

### Pattern 3: Placeholder Text Painting

**What:** Paint placeholder (hint) text when the text field is empty and not focused. Read from client property `JTextField.placeholderText`.
**When to use:** When `document.getLength() == 0` and (optionally) the field is not focused.
**Example:**

```java
// Source: FlatLaf placeholder painting pattern
private void paintPlaceholder(Graphics2D g2, JTextComponent c) {
    if (c.getDocument().getLength() > 0) {
        return; // has text, no placeholder
    }

    String placeholder = (String) c.getClientProperty("JTextField.placeholderText");
    if (placeholder == null || placeholder.isEmpty()) {
        return;
    }

    // Use placeholderForeground color (muted gray)
    g2.setColor(placeholderForeground != null
        ? placeholderForeground : Color.GRAY);
    g2.setFont(c.getFont());

    FontMetrics fm = g2.getFontMetrics();
    Insets insets = c.getInsets();

    // Position: same as where text would appear
    int x = insets.left;
    int y = insets.top + fm.getAscent();

    // Clip to available width
    int availWidth = c.getWidth() - insets.left - insets.right;
    String clipped = SwingUtilities.layoutCompoundLabel(
        fm, placeholder, null,
        SwingConstants.CENTER, SwingConstants.LEADING,
        SwingConstants.CENTER, SwingConstants.TRAILING,
        new Rectangle(x, insets.top, availWidth, c.getHeight() - insets.top - insets.bottom),
        new Rectangle(), new Rectangle(), 0);

    g2.drawString(clipped, x, y);
}
```

### Pattern 4: DwcTextFieldBorder (Focus Ring Reservation)

**What:** Same architecture as `DwcButtonBorder` -- reserves space for the focus ring in border insets and paints the border outline.
**When to use:** Always -- prevents focus ring clipping.
**Example:**

```java
public class DwcTextFieldBorder extends AbstractBorder {

    private static final Insets DEFAULT_MARGIN = new Insets(2, 6, 2, 6);

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        int focusWidth = UIManager.getInt("Component.focusWidth");
        int borderWidth = UIManager.getInt("Component.borderWidth");

        Insets margin = DEFAULT_MARGIN;
        if (c instanceof JTextComponent tc) {
            Insets m = tc.getMargin();
            if (m != null && !(m instanceof UIResource)) {
                margin = m;
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
        // Determine border color based on state
        Color borderColor = resolveBorderColor(c);
        if (borderColor == null) return;

        int focusWidth = UIManager.getInt("Component.focusWidth");
        int borderWidth = UIManager.getInt("Component.borderWidth");
        int arc = UIManager.getInt("TextField.arc");

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Object[] saved = PaintUtils.setupPaintingHints(g2);
            g2.setColor(borderColor);
            PaintUtils.paintOutline(g2,
                focusWidth, focusWidth,
                w - focusWidth * 2, h - focusWidth * 2,
                borderWidth, arc);
            PaintUtils.restorePaintingHints(g2, saved);
        } finally {
            g2.dispose();
        }
    }
}
```

### Pattern 5: Background Painting Override

**What:** Override `paintBackground()` (called by `paintSafely()`) to paint a rounded background instead of the rectangular fill from BasicTextUI.
**When to use:** Always -- the rounded background is required for DWC appearance.
**Critical nuance:** `BasicTextUI.paintSafely()` calls `paintBackground()` only if `isOpaque()` returns true. Since we set opaque=false (for rounded corners to work), we must paint the background ourselves in `paintSafely()` BEFORE calling `super.paintSafely()`.
**Example:**

```java
@Override
protected void paintBackground(Graphics g) {
    // Do nothing here -- background is painted in paintSafely()
    // before calling super.paintSafely().
    // BasicTextUI only calls this if isOpaque(), which we set to false.
}
```

### Anti-Patterns to Avoid

- **Overriding `paint()` on a TextUI:** `BasicTextUI.paint()` is `final`. Cannot be overridden. Use `paintSafely()` instead.
- **Setting opaque=true and relying on `paintBackground()`:** Causes a rectangular background behind the rounded fill. Set opaque=false, paint the rounded background explicitly.
- **Using `StateColorResolver.resolve()` for hover detection:** The current resolver only checks `AbstractButton.isRollover()` for hover. JTextField has no rollover model. Use a MouseListener to set a boolean flag.
- **Painting the focus ring in `paintBorder()`:** The border's `paintBorder()` is called in a different graphics context / clip than `paintSafely()`. Paint the focus ring in `paintSafely()` after the content, so it layers correctly.
- **Forgetting to repaint on focus change:** `BasicTextUI` repaints caret/selection on focus change, but NOT the border or background. A FocusListener must trigger repaint for the border color and focus ring to update.
- **Calling `super.paintSafely()` inside a `Graphics.create()` clone:** The parent `paintSafely()` accesses `getComponent()` and sets up clip regions based on the original graphics context. Pass the original `Graphics` to `super.paintSafely()`.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Rounded background fill | Manual `g2.fill(RoundRectangle2D)` | `PaintUtils.paintRoundedBackground()` | Handles antialiasing, null checks, shape degeneration |
| Focus ring painting | Manual Path2D even-odd construction | `FocusRingPainter.paintFocusRing()` | Already handles outer arc expansion, null/zero checks, hint management |
| Disabled opacity | Manual alpha color manipulation | `StateColorResolver.paintWithOpacity()` | Wraps composite save/restore correctly |
| Outline/border painting | `g2.draw()` with BasicStroke | `PaintUtils.paintOutline()` | Even-odd fill avoids stroke centering artifacts |
| Text rendering | Custom `g2.drawString()` for document content | `super.paintSafely()` / BasicTextUI view hierarchy | Handles bidi text, selection, highlights, caret, font metrics correctly |
| Caret painting | Custom caret painting | `BasicTextUI` caret/highlight system | Already handles blink rate, selection, drag selection |

**Key insight:** The text field delegate's job is painting background + border + placeholder + focus ring, then delegating ALL text content rendering to `BasicTextUI.paintSafely()`. Never hand-roll text rendering for the document content.

## Common Pitfalls

### Pitfall 1: `paint()` Is Final in BasicTextUI

**What goes wrong:** Compilation error when trying to override `paint(Graphics, JComponent)`.
**Why it happens:** `BasicTextUI.paint()` is declared `final` because it acquires a read-lock on the document model for thread safety. All custom painting must go through `paintSafely()`.
**How to avoid:** Override `paintSafely(Graphics g)` instead. It is called by `paint()` under the document lock.
**Warning signs:** Compiler error "cannot override final method."

### Pitfall 2: Opaque=true Causes Rectangular Background Artifact

**What goes wrong:** A white rectangle is visible behind the rounded-corner background.
**Why it happens:** When `isOpaque()` returns true, `BasicTextUI.paintSafely()` calls `paintBackground()` which fills the entire component bounds with a rectangular fill. This shows outside the rounded corners.
**How to avoid:** Set `opaque=false` via `LookAndFeel.installProperty()` in `installDefaults()`. Paint the rounded background explicitly in `paintSafely()`. Override `paintBackground()` to be a no-op.
**Warning signs:** White rectangles visible at the corners of text fields.

### Pitfall 3: Hover State Not Detected

**What goes wrong:** Text field never shows hover visual (changed border color, changed background).
**Why it happens:** JTextField has no rollover model. `StateColorResolver.resolve()` checks `AbstractButton.isRollover()` which doesn't apply to text components. Without a MouseListener, the UI has no way to know the mouse is over the field.
**How to avoid:** Install a `MouseAdapter` in `installListeners()` that sets a boolean `hover` field and calls `getComponent().repaint()`.
**Warning signs:** Border color never changes on mouse hover; always stays at the "normal" state color.

### Pitfall 4: Focus Repaint Not Triggered for Border/Focus-Ring

**What goes wrong:** The focus ring appears delayed or not at all. Border color doesn't change on focus.
**Why it happens:** `BasicTextUI` handles caret visibility on focus change, but does NOT repaint the entire component. If the focus ring or border color depends on focus state, the component must be repainted.
**How to avoid:** Install a `FocusListener` that calls `getComponent().repaint()` on both `focusGained` and `focusLost`.
**Warning signs:** Focus ring only appears after the next repaint event (e.g., mouse move), or doesn't appear at all.

### Pitfall 5: Placeholder Text Overlaps Actual Text

**What goes wrong:** Placeholder text is visible even when the user has typed content.
**Why it happens:** The placeholder painting logic doesn't check `document.getLength()`.
**How to avoid:** Always check `c.getDocument().getLength() == 0` before painting placeholder text. Also check that placeholder text is not null/empty.
**Warning signs:** Both placeholder and user text are visible simultaneously.

### Pitfall 6: super.paintSafely() Called With Wrong Graphics Context

**What goes wrong:** Text content doesn't render, or renders at wrong position, or selection highlights break.
**Why it happens:** `paintSafely()` in BasicTextUI accesses the component via `getComponent()` and sets up clip regions. If called with a `Graphics2D` created via `g.create()`, the clip/transform state may not match what the view hierarchy expects.
**How to avoid:** Call `super.paintSafely(g)` with the ORIGINAL `Graphics` parameter received by your `paintSafely()` override. Use `g.create()` only for your own painting (background, placeholder, focus ring), then dispose it, then pass the original `g` to `super.paintSafely()`.
**Warning signs:** Text is invisible, misaligned, or selection highlighting is broken.

### Pitfall 7: `currentColor` CSS Token Not Resolvable

**What goes wrong:** The `--dwc-input-color` token maps to `TextField.foreground` but its CSS value is `currentColor`, which is not a parseable color.
**Why it happens:** `currentColor` is a CSS keyword meaning "inherit from parent element's color property." It has no fixed color value outside a browser context. The `CssColorParser` will not parse it.
**How to avoid:** The token mapping `--dwc-input-color = color:TextField.foreground` will silently fail because `currentColor` can't be parsed to a `ColorValue`. Instead, rely on the existing mapping `--dwc-color-black = color:TextField.foreground` which already sets `TextField.foreground` to black (the body text color). Do NOT add a new mapping for `--dwc-input-color` -- it would overwrite the working black color with a null.
**Warning signs:** `TextField.foreground` is null in UIDefaults.

### Pitfall 8: Shared UI Instance Across Text Fields

**What goes wrong:** Hover state on one text field affects another's appearance.
**Why it happens:** If `createUI()` returns a shared singleton (as `BasicTextFieldUI.createUI()` does), the `hover` boolean is shared across all text fields.
**How to avoid:** Return `new DwcTextFieldUI()` from `createUI()` -- one instance per text field (same pattern as `DwcButtonUI`).
**Warning signs:** Hovering over one text field causes a different text field to show hover state.

## Code Examples

### DWC CSS Input Token Values (from default-light.css)

```css
/* The actual resolved values for the default light theme: */
.dwc-input {
  --dwc-input-background: var(--dwc-color-default-light);       /* = hsl(211, 38%, 95%) */
  --dwc-input-border-color: var(--dwc-color-default-dark);      /* = hsl(211, 38%, 85%) */
  --dwc-input-border-width: var(--dwc-border-width);            /* = 1px */
  --dwc-input-color: currentColor;                               /* = parent text color (black) */
  --dwc-input-placeholder-color: var(--dwc-color-gray-60);      /* = hsl(0, 0%, 60%) */
  --dwc-input-hover-background: var(--dwc-color-default-light); /* = same as background */
  --dwc-input-hover-border-color: var(--dwc-color-primary);     /* = hsl(211, 100%, 40%) -- BLUE */
  --dwc-input-hover-color: var(--dwc-color-on-default-text-light);
  --dwc-input-focus-ring: var(--dwc-focus-ring-default);        /* = 0 0 0 3px hsla(211, 100%, 45%, 0.4) */
}
```

### Token-to-UIDefaults Mapping Analysis

```properties
# ALREADY in token-mapping.properties:
--dwc-input-background        = color:TextField.background        # CONFLICT: also mapped from --dwc-color-white
--dwc-input-border-color       = color:TextField.borderColor
--dwc-input-placeholder-color  = color:TextField.placeholderForeground
--dwc-input-color              = color:TextField.foreground        # WARNING: currentColor, won't parse
--dwc-border-radius            = int:TextField.arc
--dwc-color-black              = color:TextField.foreground        # This one WORKS (gives black)
--dwc-color-white              = color:TextField.background        # CONFLICT with --dwc-input-background

# NEED TO ADD for hover/focus states:
--dwc-input-hover-border-color = color:TextField.hoverBorderColor  # NEW KEY
--dwc-input-hover-background   = color:TextField.hoverBackground   # NEW KEY

# ALREADY handled in DwcLookAndFeel:
# Component.focusRingColor   -- computed from HSL tokens (shared across all components)
# Component.focusWidth       -- from --dwc-focus-ring-width
# Component.borderWidth      -- from --dwc-border-width
```

### Mapping Conflict Resolution

The CSS has conflicting mappings for `TextField.background`:
1. `--dwc-color-white = color:TextField.background` (maps to `#fff` = white)
2. `--dwc-input-background = color:TextField.background` (maps to `--dwc-color-default-light` = light grayish blue)

The DWC input background should win, as the `.dwc-input` component token overrides the global default. In the properties file, the LAST mapping for a given UIDefaults key wins when `UIDefaultsPopulator` processes entries (since it calls `table.put()` sequentially). So the mapping order matters.

**Recommendation:** Remove the `TextField.background` target from the `--dwc-color-white` line in token-mapping.properties, and keep only the `--dwc-input-background` mapping for it. This matches DWC behavior where the component-level token overrides the global token.

Similarly for `TextField.foreground`:
1. `--dwc-color-black = color:TextField.foreground` (gives black)
2. `--dwc-input-color = color:TextField.foreground` (`currentColor` won't parse, silently skipped)

The `--dwc-color-black` mapping provides the correct black foreground color. The `--dwc-input-color` mapping should be removed since `currentColor` cannot be parsed.

### Painting Order in paintSafely()

```java
@Override
protected void paintSafely(Graphics g) {
    JTextComponent c = getComponent();

    // 1. Paint rounded background (BEFORE super, since opaque=false means
    //    super won't call paintBackground)
    {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            paintDwcBackground(g2, c);
        } finally {
            g2.dispose();
        }
    }

    // 2. Call super.paintSafely() to render text content, highlights, caret
    //    MUST use original Graphics object (not g.create())
    super.paintSafely(g);

    // 3. Paint placeholder text on top of background, below caret
    //    (Actually, placeholder should be painted AFTER super too, since we
    //     don't want it obscured by anything. But if super paints background
    //     it would overwrite. Since opaque=false, super skips background.)
    {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            paintPlaceholder(g2, c);
        } finally {
            g2.dispose();
        }
    }

    // 4. Paint focus ring on top of everything
    if (c.hasFocus()) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            paintFocusRing(g2, c);
        } finally {
            g2.dispose();
        }
    }
}
```

**NOTE on ordering:** Placeholder text is painted AFTER `super.paintSafely()` so that if there IS text in the document, the text renders on top. But since we only paint placeholder when document is empty, the order relative to super doesn't matter functionally. However, painting it AFTER super ensures the placeholder renders with our chosen color regardless of any background painting super might do.

### UIDefaults Keys Required by DwcTextFieldUI

```
# Colors (from CSS tokens via mapping)
TextField.background             -> normal state background (light grayish blue)
TextField.foreground             -> text color (black)
TextField.borderColor            -> normal state border color
TextField.hoverBackground        -> hover state background (NEW)
TextField.hoverBorderColor       -> hover state border color (NEW)
TextField.placeholderForeground  -> placeholder text color (muted gray)
TextField.disabledBackground     -> disabled background (OPTIONAL, can use opacity)
TextField.inactiveBackground     -> non-editable background (OPTIONAL for later)

# Shared component colors (already in UIDefaults)
Component.focusRingColor         -> focus ring color (semi-transparent blue)

# Dimensions (already in UIDefaults)
TextField.arc                    -> corner arc diameter (from --dwc-border-radius)
Component.focusWidth             -> focus ring width (from --dwc-focus-ring-width)
Component.borderWidth            -> border thickness (from --dwc-border-width)
Component.disabledOpacity        -> disabled opacity (from --dwc-disabled-opacity)

# Layout
TextField.margin                 -> Insets (2, 6, 2, 6) -- text padding inside the field
```

### L&F Registration and Initialization

```java
// In DwcLookAndFeel.initClassDefaults():
@Override
protected void initClassDefaults(UIDefaults table) {
    super.initClassDefaults(table);
    table.put("ButtonUI", "com.dwc.laf.ui.DwcButtonUI");
    table.put("TextFieldUI", "com.dwc.laf.ui.DwcTextFieldUI");  // NEW
}

// In DwcLookAndFeel.initComponentDefaults():
private void initTextFieldDefaults(UIDefaults table) {
    // 1. Text field margin
    table.put("TextField.margin", new InsetsUIResource(2, 6, 2, 6));

    // 2. Text field border
    table.put("TextField.border",
        new BorderUIResource(new DwcTextFieldBorder()));
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `paintBackground()` fills rectangular bounds | Override `paintSafely()` to paint rounded background | Modern L&F pattern (FlatLaf) | Rounded corners match web component appearance |
| No hover state on text fields | MouseListener tracking with boolean flag | FlatLaf pattern | Visual feedback on hover matching DWC web input |
| No placeholder text support | Client property `JTextField.placeholderText` | FlatLaf/SwingX pattern | Standard approach adopted by modern L&Fs |
| `setOpaque(true)` for text fields | `setOpaque(false)` + custom background paint | FlatLaf pattern | Required for rounded corners |
| Focus shown only by caret appearance | Focus ring painted outside component bounds | DWC/modern L&F pattern | Consistent focus indicator with buttons |

**Deprecated/outdated:**
- `paintBackground()` for rounded fields: The default implementation fills the entire bounds rectangularly. Override it as a no-op and paint rounded background in `paintSafely()`.
- Swing `JTextField.setHint()`: Does not exist in standard Swing. Must be implemented via client property + custom painting.
- `AbstractButton.isRollover()` for text components: Does not apply. Use MouseListener.

## Token Mapping Updates Required

| Current Mapping | Issue | Fix |
|-----------------|-------|-----|
| `--dwc-color-white = color:TextField.background` | Overridden by `--dwc-input-background` mapping; `#fff` is wrong for DWC input bg | Remove `TextField.background` from this line |
| `--dwc-input-color = color:TextField.foreground` | `currentColor` is not parseable | Remove this mapping entirely; `--dwc-color-black` already provides the correct foreground |
| (missing) `--dwc-input-hover-border-color` | Not mapped | Add: `--dwc-input-hover-border-color = color:TextField.hoverBorderColor` |
| (missing) `--dwc-input-hover-background` | Not mapped | Add: `--dwc-input-hover-background = color:TextField.hoverBackground` |

## Open Questions

1. **Placeholder visibility on focus**
   - What we know: Some implementations hide placeholder when focused, others hide only when text is present. DWC web input hides placeholder only when text is present (standard HTML `<input placeholder="...">` behavior).
   - What's unclear: Whether to hide placeholder on focus (even when field is empty) or only when text is entered.
   - Recommendation: Follow standard HTML behavior -- show placeholder when field is empty regardless of focus. Only hide when `document.getLength() > 0`. This matches user expectations from web.

2. **`update()` method override**
   - What we know: `BasicTextUI.update()` is overridden to simply call `paint()` without painting the component background (avoiding double-painting). FlatLaf overrides `update()` to handle parent background painting when the component has rounded corners.
   - What's unclear: Whether we need to override `update()` to paint the parent's background behind the rounded field.
   - Recommendation: Override `update()` to fill the component's parent background color in the rounded-corner gap area, then call `paint()`. This prevents visual artifacts where the parent color bleeds through. If the parent is opaque, Swing handles this automatically. Start without the override and add it if visual artifacts appear.

3. **Text selection colors**
   - What we know: DWC defines `--dwc-input-selection-background` and `--dwc-input-selection-color` for text selection highlighting.
   - What's unclear: Whether to map these now or defer to a later phase.
   - Recommendation: Add the mappings now since they're simple and improve the experience: `--dwc-input-selection-background -> color:TextField.selectionBackground` and `--dwc-input-selection-color -> color:TextField.selectionForeground`. But do not make them blockers for the phase success criteria.

4. **Caret color**
   - What we know: The default caret color is typically the foreground color. DWC doesn't define a specific caret color token.
   - What's unclear: Whether to explicitly set caret color or let it inherit.
   - Recommendation: Let the default behavior handle it (caret uses foreground color). Only override if visual testing shows an issue.

5. **Shared DwcTextFieldBorder vs DwcButtonBorder**
   - What we know: Both borders follow the same structural pattern (focusWidth + borderWidth + margin). They differ in default margins and may differ in border color resolution logic.
   - What's unclear: Whether to extract a common base class or keep them separate.
   - Recommendation: Keep separate classes for now. The implementations are small (~50 lines each) and may diverge as more features are added. Extract a common base only if a clear pattern emerges after Phase 6.

## Sources

### Primary (HIGH confidence)
- OpenJDK `BasicTextUI` JDK 21 docs - Paint pipeline (`paint()` is final, `paintSafely()`, `paintBackground()`), installation lifecycle
- OpenJDK `BasicTextFieldUI` JDK 21 docs - Extends BasicTextUI, getPropertyPrefix() returns "TextField"
- [FlatLaf TextField documentation](https://www.formdev.com/flatlaf/components/textfield/) - UIDefaults keys, client properties, placeholder support
- [FlatLaf FlatTextFieldUI source](https://github.com/JFormDesigner/FlatLaf/blob/main/flatlaf-core/src/main/java/com/formdev/flatlaf/ui/FlatTextFieldUI.java) - Architecture, paintSafely override, hover tracking, placeholder painting
- [FlatLaf default properties](https://github.com/JFormDesigner/FlatLaf/blob/main/flatlaf-core/src/main/resources/com/formdev/flatlaf/FlatLaf.properties) - Default values for TextField.margin, Component.focusWidth, etc.
- DWC `default-light.css` (local project) - All `--dwc-input-*` tokens
- DWC `_input-base.scss` (local project) - Input styling: background, border, hover, focus, placeholder
- DWC `token-mapping.properties` (local project) - Existing and missing TextField mappings
- Project Phase 3 utilities (local project) - PaintUtils, FocusRingPainter, StateColorResolver
- Project Phase 4 DwcButtonUI/DwcButtonBorder (local project) - Proven architecture pattern for custom UI delegate

### Secondary (MEDIUM confidence)
- [BasicTextUI JDK 17 docs](https://docs.oracle.com/en/java/javase/17/docs/api/java.desktop/javax/swing/plaf/basic/BasicTextUI.html) - Complete method listing and paint pipeline documentation
- [FlatLaf project](https://www.formdev.com/flatlaf/) - Reference implementation for modern Swing L&F
- [FlatLaf GitHub](https://github.com/JFormDesigner/FlatLaf) - Active development, FlatTextFieldUI source

### Tertiary (LOW confidence)
- Community placeholder implementations (blog posts, StackOverflow) - General approach validation, but implementation details vary

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All classes are JDK standard library or already-implemented Phase 3/4 utilities; FlatLaf reference architecture is well-understood
- Architecture: HIGH - BasicTextUI/BasicTextFieldUI paint pipeline is documented in JDK docs; FlatLaf's FlatTextFieldUI validates the approach; DwcButtonUI proves the per-component instance + custom border pattern works
- Pitfalls: HIGH - `paint()` finality, opaque flag, hover state detection, and Graphics context issues are all documented in official sources and verified against the codebase
- Token mapping: HIGH - Complete analysis of existing mappings, conflicts identified, resolution strategy clear
- Placeholder text: HIGH - FlatLaf client property approach is production-proven and simple to implement

**Research date:** 2026-02-10
**Valid until:** 2026-03-12 (stable domain; Swing TextUI contract does not change between JDK releases)
