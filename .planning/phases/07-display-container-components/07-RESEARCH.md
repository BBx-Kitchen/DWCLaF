# Phase 7: Display & Container Components - Research

**Researched:** 2026-02-10
**Domain:** Custom Swing LabelUI, PanelUI, and TabbedPaneUI delegates with DWC CSS token-derived typography, card-style surfaces, and tab strip painting
**Confidence:** HIGH

## Summary

This phase implements three custom ComponentUI delegates -- `DwcLabelUI`, `DwcPanelUI`, and `DwcTabbedPaneUI` -- that paint JLabel, JPanel, and JTabbedPane with DWC CSS token-derived styling. These three components complete the 8-component set required for the Phase 8 demo. The existing infrastructure from Phases 1-5 provides all needed painting primitives: `PaintUtils` for rounded backgrounds and outlines, `ShadowPainter` for elevation shadows, `FocusRingPainter` for focus rings, `StateColorResolver` for state-based color resolution, plus the token-mapping pipeline and `DwcLookAndFeel` registration mechanism.

JLabel is the simplest component -- `BasicLabelUI` already handles icon/text layout, mnemonic painting, and accessibility. The custom delegate only needs to ensure typography tokens (font family, size, weight, color) are correctly applied from CSS and that disabled state paints with reduced opacity. JPanel requires a `DwcPanelUI` extending `BasicPanelUI` that paints a rounded background with optional elevation shadow when a `"dwc.panelStyle"` client property is set to `"card"`. The surface color derives from `--dwc-surface-3` (already mapped to `Panel.background`). JTabbedPane is the most complex: the delegate extends `BasicTabbedPaneUI` and overrides `paintTabBackground()`, `paintTabBorder()`, `paintFocusIndicator()`, and `paintContentBorder()` to render DWC-style tabs with an underline active indicator, hover effects, and distinct states (normal, hover, selected, disabled).

The DWC web component's tabbed pane uses a 2px underline indicator below the active tab (`--dwc-tabbed-pane-indicator-height: 2px`) colored with `--dwc-color-primary-text` (the primary theme text color). Tabs have transparent backgrounds by default, with hover showing `--dwc-tab-hover-color` for text, and active/selected tabs showing `--dwc-tab-active-color`. The tab strip is separated from the content area by a border using `--dwc-color-default`. FlatLaf's `FlatTabbedPaneUI` implements a similar underline approach with `paintTabSelection()` and tracks hover state via a mouse handler that calls `setRolloverTab()`.

**Primary recommendation:** Build three UI delegates: (1) `DwcLabelUI` extending `BasicLabelUI` -- minimal, focused on typography token application and disabled opacity, (2) `DwcPanelUI` extending `BasicPanelUI` -- card-style with rounded corners and shadow via client property, (3) `DwcTabbedPaneUI` extending `BasicTabbedPaneUI` -- override `paintTabBackground`, `paintTabBorder`, `paintFocusIndicator`, `paintContentBorder*` for underline indicator styling. Register all three via `initClassDefaults()`. Extend token-mapping.properties for new UIDefaults keys.

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `javax.swing.plaf.basic.BasicLabelUI` | JDK 21 | Base class for label delegate | Handles icon/text layout, mnemonic underline, accessibility; paint() calls paintEnabledText()/paintDisabledText() |
| `javax.swing.plaf.basic.BasicPanelUI` | JDK 21 | Base class for panel delegate | Minimal base class; installDefaults() applies background/font from UIDefaults |
| `javax.swing.plaf.basic.BasicTabbedPaneUI` | JDK 21 | Base class for tabbed pane delegate | Handles tab layout (wrap/scroll), keyboard navigation, tab run calculation, scroll buttons |
| `com.dwc.laf.painting.PaintUtils` | Phase 3 | Rounded backgrounds, outlines, antialiasing | Already built and tested; used for panel card background and tab backgrounds |
| `com.dwc.laf.painting.ShadowPainter` | Phase 3 | Gaussian-blurred box shadow | Already built and tested; used for panel card elevation shadow |
| `com.dwc.laf.painting.FocusRingPainter` | Phase 3 | Focus ring painting | Already built and tested; used for tab focus indicator |
| `com.dwc.laf.painting.StateColorResolver` | Phase 3 | State-based color resolution and disabled opacity | Already built and tested |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `java.awt.geom.Path2D` | JDK 21 | Even-odd fill for tab underline indicator | Drawing the underline selection mark below active tab |
| `java.awt.geom.Line2D` | JDK 21 | Tab separator line painting | Drawing the border between tab strip and content area |
| `java.awt.geom.RoundRectangle2D` | JDK 21 | Rounded tab backgrounds and underline | Via PaintUtils.createRoundedShape for tab backgrounds with rounded corners |
| `javax.swing.plaf.basic.BasicGraphicsUtils` | JDK 21 | Mnemonic underline rendering | Inherited via BasicLabelUI for label text painting |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Extending `BasicTabbedPaneUI` | Starting from `TabbedPaneUI` abstract class | Would lose tab layout, keyboard navigation, scroll tab infrastructure -- massive reimplementation |
| Client property `"dwc.panelStyle"` for card panels | Always painting rounded/shadow on all panels | Most JPanels are plain containers; card style is opt-in. Making all panels card-style would break layout assumptions and add unnecessary shadow painting overhead. |
| `DwcLabelUI` extending `BasicLabelUI` | No custom LabelUI at all (rely on UIDefaults font/color) | BasicLabelUI already reads `Label.font`, `Label.foreground` from UIDefaults, which we already populate. However, a custom delegate gives us control over disabled opacity rendering to match `--dwc-disabled-opacity`. Worth the minimal effort for consistency. |
| Overriding `paintTabBackground` + `paintTabBorder` individually | Full `paintTab()` override | Individual method overrides preserve BasicTabbedPaneUI's icon/text layout logic. Full paintTab override would require reimplementing layout. |

## Architecture Patterns

### Recommended Project Structure

```
src/main/java/com/dwc/laf/
  ui/
    DwcLabelUI.java            # Custom LabelUI delegate (extends BasicLabelUI)
    DwcPanelUI.java            # Custom PanelUI delegate (extends BasicPanelUI)
    DwcTabbedPaneUI.java       # Custom TabbedPaneUI delegate (extends BasicTabbedPaneUI)
  DwcLookAndFeel.java          # Add LabelUI, PanelUI, TabbedPaneUI in initClassDefaults()
                               # Add initLabelDefaults(), initPanelDefaults(), initTabbedPaneDefaults()
```

### Pattern 1: DwcLabelUI -- Typography and Disabled Opacity

**What:** A minimal UI delegate extending `BasicLabelUI` that ensures typography tokens are applied correctly and disabled labels render with DWC's `--dwc-disabled-opacity` rather than the default BasicLabelUI disabled text treatment (lighter/darker shadow).

**When to use:** For all JLabel instances.

**Key insight:** `BasicLabelUI` paints disabled text using `paintDisabledText()` which draws the text twice (lighter + darker offset) to create a chiseled effect. This is wrong for DWC styling -- DWC uses simple opacity reduction. Override `paintDisabledText()` to paint at reduced opacity instead.

**Example:**

```java
public class DwcLabelUI extends BasicLabelUI {

    private float disabledOpacity;

    public static ComponentUI createUI(JComponent c) {
        return new DwcLabelUI();
    }

    @Override
    protected void installDefaults(JLabel c) {
        super.installDefaults(c);
        // disabledOpacity from UIDefaults
        Object opacityObj = UIManager.get("Component.disabledOpacity");
        disabledOpacity = (opacityObj instanceof Number num) ? num.floatValue() : 0.6f;
    }

    @Override
    protected void paintDisabledText(JLabel l, Graphics g, String s,
            int textX, int textY) {
        // DWC uses opacity reduction, not chiseled text
        Graphics2D g2 = (Graphics2D) g;
        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.SrcOver.derive(disabledOpacity));
        // Paint with normal foreground at reduced opacity
        g2.setColor(l.getForeground());
        BasicGraphicsUtils.drawStringUnderlineCharAt(g2, s,
                l.getDisplayedMnemonicIndex(), textX, textY);
        g2.setComposite(old);
    }
}
```

### Pattern 2: DwcPanelUI -- Card-Style Panel with Shadow

**What:** A panel UI delegate extending `BasicPanelUI` that supports two rendering modes: (1) default mode (standard flat background, delegates to super), and (2) card mode (rounded corners + elevation shadow), activated via `panel.putClientProperty("dwc.panelStyle", "card")`.

**When to use:** For all JPanel instances. Card mode is opt-in.

**Key insight:** `BasicPanelUI.update()` fills the background when opaque=true, then calls `paint()`. For card mode, we override `update()` to paint a rounded background with shadow instead of the rectangular fill. The panel must be set non-opaque for rounded corners to look correct (no rectangular fill showing through).

**Example:**

```java
public class DwcPanelUI extends BasicPanelUI {

    private Color background;
    private Color shadowColor;
    private int arc;
    private float shadowBlurRadius;
    private float shadowOffsetY;

    public static ComponentUI createUI(JComponent c) {
        return new DwcPanelUI();
    }

    @Override
    protected void installDefaults(JPanel p) {
        super.installDefaults(p);
        background = UIManager.getColor("Panel.background");
        arc = UIManager.getInt("Panel.arc");
        // Shadow parameters for card style
        shadowColor = UIManager.getColor("Panel.shadowColor");
        shadowBlurRadius = 8f; // approximating --dwc-shadow-m
        shadowOffsetY = 2f;
    }

    @Override
    public void update(Graphics g, JComponent c) {
        if (isCardStyle(c)) {
            // Card mode: non-opaque, rounded background with shadow
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                Color bg = c.getBackground();
                if (shadowColor != null) {
                    ShadowPainter.paintShadow(g2, 0, 0,
                            c.getWidth(), c.getHeight(),
                            arc, shadowBlurRadius, 0, shadowOffsetY, shadowColor);
                }
                PaintUtils.paintRoundedBackground(g2, 0, 0,
                        c.getWidth(), c.getHeight(), arc, bg);
            } finally {
                g2.dispose();
            }
            paint(g, c);
        } else {
            // Default mode: standard BasicPanelUI behavior
            super.update(g, c);
        }
    }

    private boolean isCardStyle(JComponent c) {
        return "card".equals(c.getClientProperty("dwc.panelStyle"));
    }
}
```

### Pattern 3: DwcTabbedPaneUI -- Underline Indicator Tab Strip

**What:** A tabbed pane UI delegate extending `BasicTabbedPaneUI` that paints tabs with DWC styling: transparent default backgrounds, hover text color change, underline active indicator (2px thick primary-colored line below the selected tab), and a separator line between tabs and content.

**When to use:** For all JTabbedPane instances.

**Key architectural decisions:**
1. Override `paintTabBackground()` -- paint transparent by default, subtle hover background for rollover tab
2. Override `paintTabBorder()` -- no-op (DWC tabs don't have individual borders)
3. Override `paintFocusIndicator()` -- paint DWC-style focus ring instead of dashed rectangle
4. Override `paintContentBorderTopEdge()` (and other edges) -- paint the separator line + underline indicator
5. Track hover state via `getRolloverTab()` (inherited from BasicTabbedPaneUI)

**Example:**

```java
public class DwcTabbedPaneUI extends BasicTabbedPaneUI {

    private Color tabForeground;
    private Color tabHoverForeground;
    private Color tabSelectedForeground;
    private Color tabHoverBackground;
    private Color underlineColor;
    private Color disabledUnderlineColor;
    private Color contentBorderColor;
    private Color focusRingColor;
    private int underlineHeight;
    private float disabledOpacity;

    public static ComponentUI createUI(JComponent c) {
        return new DwcTabbedPaneUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        // Read DWC-specific UIDefaults
        tabForeground = UIManager.getColor("TabbedPane.foreground");
        tabHoverForeground = UIManager.getColor("TabbedPane.hoverForeground");
        tabSelectedForeground = UIManager.getColor("TabbedPane.selectedForeground");
        tabHoverBackground = UIManager.getColor("TabbedPane.hoverBackground");
        underlineColor = UIManager.getColor("TabbedPane.underlineColor");
        disabledUnderlineColor = UIManager.getColor("TabbedPane.disabledUnderlineColor");
        contentBorderColor = UIManager.getColor("TabbedPane.contentAreaColor");
        focusRingColor = UIManager.getColor("Component.focusRingColor");
        underlineHeight = UIManager.getInt("TabbedPane.underlineHeight");
        if (underlineHeight <= 0) underlineHeight = 2;
        // ... etc
    }

    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement,
            int tabIndex, int x, int y, int w, int h, boolean isSelected) {
        // DWC tabs have transparent background by default
        // Show subtle background only on hover
        if (getRolloverTab() == tabIndex && tabPane.isEnabledAt(tabIndex)) {
            Graphics2D g2 = (Graphics2D) g;
            if (tabHoverBackground != null) {
                PaintUtils.paintRoundedBackground(g2, x, y, w, h,
                        UIManager.getInt("TabbedPane.tabArc"), tabHoverBackground);
            }
        }
    }

    @Override
    protected void paintTabBorder(Graphics g, int tabPlacement,
            int tabIndex, int x, int y, int w, int h, boolean isSelected) {
        // No individual tab borders in DWC style -- no-op
    }

    @Override
    protected void paintFocusIndicator(Graphics g, int tabPlacement,
            Rectangle[] rects, int tabIndex, Rectangle iconRect,
            Rectangle textRect, boolean isSelected) {
        // Paint DWC-style focus ring instead of dashed rectangle
        if (tabPane.hasFocus() && isSelected) {
            Rectangle r = rects[tabIndex];
            Graphics2D g2 = (Graphics2D) g;
            int arc = UIManager.getInt("TabbedPane.tabArc");
            FocusRingPainter.paintFocusRing(g2, r.x, r.y, r.width, r.height,
                    arc, UIManager.getInt("Component.focusWidth"), focusRingColor);
        }
    }

    @Override
    protected void paintContentBorderTopEdge(Graphics g, int tabPlacement,
            int selectedIndex, int x, int y, int w, int h) {
        if (tabPlacement != TOP) return;
        Graphics2D g2 = (Graphics2D) g;
        // Paint separator line
        if (contentBorderColor != null) {
            g2.setColor(contentBorderColor);
            g2.drawLine(x, y, x + w - 1, y);
        }
        // Paint underline indicator below selected tab
        if (selectedIndex >= 0) {
            Rectangle selRect = rects[selectedIndex];
            Color uColor = tabPane.isEnabled() ? underlineColor : disabledUnderlineColor;
            if (uColor != null) {
                g2.setColor(uColor);
                g2.fillRect(selRect.x, y - underlineHeight, selRect.width, underlineHeight);
            }
        }
    }
}
```

### Pattern 4: Tab Text Color Resolution

**What:** Override `paintText()` in the TabbedPaneUI to apply DWC-style tab text colors: normal color for unselected tabs, hover color on rollover, selected/active color for the active tab, and disabled color for disabled tabs.

**When to use:** In DwcTabbedPaneUI.

**Key insight:** `BasicTabbedPaneUI.paintText()` calls `tabPane.getForegroundAt(tabIndex)`, which defaults to the `TabbedPane.foreground` UIDefault. For state-dependent text colors (hover, selected), we override `paintText()` and set the Graphics color before calling `super.paintText()` or painting text directly.

**Example:**

```java
@Override
protected void paintText(Graphics g, int tabPlacement, Font font,
        FontMetrics metrics, int tabIndex, String title,
        Rectangle textRect, boolean isSelected) {
    // Resolve text color based on tab state
    Color fg;
    if (!tabPane.isEnabledAt(tabIndex)) {
        fg = UIManager.getColor("TabbedPane.disabledForeground");
    } else if (isSelected) {
        fg = tabSelectedForeground != null ? tabSelectedForeground : tabForeground;
    } else if (getRolloverTab() == tabIndex) {
        fg = tabHoverForeground != null ? tabHoverForeground : tabForeground;
    } else {
        fg = tabForeground;
    }
    g.setColor(fg);
    super.paintText(g, tabPlacement, font, metrics, tabIndex, title,
            textRect, isSelected);
}
```

### Anti-Patterns to Avoid

- **Making all panels card-style by default:** JPanels are used extensively as layout containers. Adding shadows and rounded corners to every JPanel would create visual chaos and performance issues. Card style must be opt-in via client property.
- **Overriding `paintTab()` entirely:** This would require reimplementing icon/text layout, mnemonic painting, and close button handling. Instead, override the specific methods (`paintTabBackground`, `paintTabBorder`, `paintFocusIndicator`, `paintContentBorder*`).
- **Using `BasicLabelUI.labelUI` shared singleton pattern:** BasicLabelUI uses a single shared instance for all labels (static `labelUI` field). Our DwcLabelUI should follow the per-component pattern established in all prior phases for consistency, even though labels are stateless enough to share.
- **Painting the tab underline indicator inside `paintTabBackground`:** The underline should be painted in `paintContentBorderTopEdge` (or the appropriate edge method based on tab placement) because the indicator is positioned at the edge between the tab area and the content area, not within the tab rectangle itself.
- **Forgetting to handle all four tab placements (TOP, BOTTOM, LEFT, RIGHT):** The underline indicator position changes based on placement. TOP = bottom edge of tab area, BOTTOM = top edge, LEFT = right edge, RIGHT = left edge. Each content border edge method must handle its respective placement.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Rounded background fill | Manual `g2.fill(RoundRectangle2D)` | `PaintUtils.paintRoundedBackground()` | Handles antialiasing, null checks, shape degeneration |
| Box shadow for card panels | Manual multi-rect approximation | `ShadowPainter.paintShadow()` | Gaussian blur with caching already implemented and tested |
| Focus ring on tabs | Dashed rectangle (BasicTabbedPaneUI default) | `FocusRingPainter.paintFocusRing()` | DWC-style semi-transparent focus ring matching other components |
| Disabled opacity | Custom alpha blending | `StateColorResolver.paintWithOpacity()` | Wraps composite save/restore correctly |
| Tab icon/text layout | Manual position calculation | Inherited `BasicTabbedPaneUI` layout | Handles all tab placements, icon positions, mnemonic index, clipping |
| Tab keyboard navigation | Custom key bindings | Inherited `BasicTabbedPaneUI` input maps | Handles Ctrl+Tab, arrow keys, Home/End, mnemonic activation |
| Tab scroll buttons | Custom scroll implementation | Inherited `BasicTabbedPaneUI` SCROLL_TAB_LAYOUT | Handles scroll buttons, viewport clipping, mouse wheel |

**Key insight:** JLabel and JPanel are mostly about applying CSS tokens correctly through UIDefaults, not about complex custom painting. JTabbedPane is more involved but still heavily leverages BasicTabbedPaneUI's infrastructure for tab layout and keyboard navigation.

## Common Pitfalls

### Pitfall 1: Label Disabled Text Shows Chiseled Effect Instead of Opacity Reduction

**What goes wrong:** Disabled JLabels show the classic chiseled look (lighter text offset by one pixel) instead of DWC's simple opacity reduction.
**Why it happens:** `BasicLabelUI.paintDisabledText()` paints text twice with different colors to create an embossed effect. This is the default Basic L&F behavior.
**How to avoid:** Override `paintDisabledText()` to paint the text once at `--dwc-disabled-opacity` using `AlphaComposite`.
**Warning signs:** Disabled labels look different from disabled buttons/textfields (which already use opacity reduction).

### Pitfall 2: Card Panel Shadow Clipped by Parent Container

**What goes wrong:** The shadow around a card-style JPanel is cut off on one or more sides.
**Why it happens:** Shadow extends beyond the panel's bounds (blur radius + offset). If the parent container clips its children (which it does by default), the shadow is clipped.
**How to avoid:** Card-style panels must have extra margin/padding from their layout to accommodate the shadow extent. Alternatively, the shadow can be constrained to paint within bounds (less realistic but avoids clipping). Document this in the component's Javadoc: "Card-style panels need surrounding margin of at least [shadow radius] pixels."
**Warning signs:** Shadow visible on bottom/right but cut on top/left (or vice versa depending on offset).

### Pitfall 3: Panel Card Mode Shows Rectangular Fill Behind Rounded Corners

**What goes wrong:** A rectangular background is visible behind the rounded corners of a card-style panel.
**Why it happens:** The panel is still opaque=true. `BasicPanelUI.update()` fills the entire background rectangularly before the custom paint code draws the rounded rectangle.
**How to avoid:** When card mode is active, set the panel to non-opaque (either in `installDefaults` based on client property, or in `update()` before painting). Use `LookAndFeel.installProperty(p, "opaque", false)` when card mode is detected.
**Warning signs:** Gray/colored rectangles visible at the corners of card panels.

### Pitfall 4: Tab Underline Indicator Positioned Incorrectly

**What goes wrong:** The underline indicator appears at the wrong position -- too far from the tab, overlapping the content, or not visible at all.
**Why it happens:** The underline indicator position depends on which edge method it's painted in and the exact y-coordinate calculations. `paintContentBorderTopEdge()` is called with `y` at the top of the content border area. The underline needs to be painted relative to the selected tab's rectangle and the content border edge.
**How to avoid:** Carefully calculate the underline y-position from the `rects[selectedIndex]` rectangle. For TOP placement, the underline is at `rects[selectedIndex].y + rects[selectedIndex].height - underlineHeight` (bottom of the tab rect). Test with all four placements.
**Warning signs:** Underline floating in space or covering content area text.

### Pitfall 5: Tab Hover State Not Updating (No Repaint on Mouse Move)

**What goes wrong:** Hovering over different tabs doesn't show visual feedback, or the hover effect "sticks" on the wrong tab.
**Why it happens:** `BasicTabbedPaneUI` tracks the rollover tab internally via `getRolloverTab()` / `setRolloverTab()`, and this already works if rollover is enabled. However, the tab area may not repaint when the rollover tab changes if the UI delegate doesn't trigger repaints.
**How to avoid:** `BasicTabbedPaneUI` already handles mouse motion and calls `setRolloverTab()`, which calls `repaint()` on the tab area. Verify that `getRolloverTab()` returns the expected value. If repaints are not happening, install a mouse motion listener that forces repaint. FlatLaf uses `repaintRolloverLaterOnce()` for deferred repaints.
**Warning signs:** Hover color doesn't change when moving mouse between tabs, or hover persists on a tab after mouse leaves.

### Pitfall 6: Tab Content Area Background Doesn't Match Panel Background

**What goes wrong:** The content area below/beside the tabs has a different background color than surrounding panels.
**Why it happens:** `BasicTabbedPaneUI` paints the content area background with `tabPane.getBackground()`, which may not match `Panel.background` if `TabbedPane.background` is set to a different color.
**How to avoid:** Ensure `TabbedPane.contentAreaColor` and `TabbedPane.background` are consistent with `Panel.background` (or `--dwc-surface-3`). Override `paintContentBorder()` if needed to use the correct background color.
**Warning signs:** Visible color seam between tabbed pane content and surrounding panels.

### Pitfall 7: TabbedPane Tab Placement Variations Not Handled

**What goes wrong:** The underline indicator and separator line work for TOP placement but break for BOTTOM, LEFT, or RIGHT.
**Why it happens:** `BasicTabbedPaneUI` calls different edge methods for each placement: `paintContentBorderTopEdge()` for TOP, `paintContentBorderBottomEdge()` for BOTTOM, etc. If only the top edge is overridden, other placements get the default Basic L&F content border.
**How to avoid:** Override all four `paintContentBorder*Edge()` methods. The underline indicator changes orientation: horizontal for TOP/BOTTOM, vertical for LEFT/RIGHT.
**Warning signs:** Correct appearance with tabs on top, broken appearance with tabs on other sides.

### Pitfall 8: BasicTabbedPaneUI Protected Fields Overwritten

**What goes wrong:** Color or dimension values set in `installDefaults()` are overwritten by the parent class's values.
**Why it happens:** If `super.installDefaults()` is called last instead of first, it overwrites our custom values. If called first, it sets default colors that we then override.
**How to avoid:** Always call `super.installDefaults()` FIRST, then override specific values. BasicTabbedPaneUI sets protected fields like `highlight`, `shadow`, `darkShadow`, `lightHighlight`, `focus`, and reads `TabbedPane.*` keys from UIManager. Our custom values must be set after super.
**Warning signs:** Colors revert to default Basic L&F appearance despite being set in the custom delegate.

## Code Examples

### DWC CSS Token Values for Tabs (from dwc-tabbed-pane.scss, dwc-tab.scss)

```scss
// Tab strip separator:
--_dwc-tabs-border: 2px solid var(--dwc-color-default);   // hsl(211, 38%, 90%)

// Active tab indicator:
--_tabs-indicator-height: 2px;
--_tabs-indicator-border: solid 2px var(--dwc-color-primary-text);  // hsl(211, 100%, 35%)

// Individual tab:
// Default state:
background: transparent;
color: currentColor;
font-weight: var(--dwc-font-weight-semibold);  // 500
padding: var(--dwc-space-xs);                   // 0.25rem = 4px
border-radius: var(--dwc-border-radius);        // 0.25em ~ 4px
border: 1px solid transparent;

// Hover state:
background: transparent;  // --dwc-tab-hover-background defaults to transparent
color: var(--dwc-color-primary-text-light);     // hsl(211, 100%, 40%)

// Active/selected:
background: transparent;  // --dwc-tab-active-background defaults to transparent
color: var(--dwc-color-primary-text);           // hsl(211, 100%, 35%)

// Disabled:
opacity: var(--dwc-disabled-opacity);           // 0.6
cursor: not-allowed;

// Focus:
border-color: var(--dwc-border-color-default);  // = --dwc-color-primary
box-shadow: var(--dwc-focus-ring-default);       // focus ring
```

### DWC CSS Token Values for Panel Surface

```scss
// --dwc-surface-3 = hsl(0, 0%, 100%) = white (light theme)
// This is already mapped to Panel.background in token-mapping.properties

// For card-style panels, DWC uses shadow:
// --dwc-shadow-m: 0 100px 80px hsla(..., 0.07),
//                 0 22.33px 17.86px hsla(..., 0.04),
//                 0 6.65px 5.32px hsla(..., 0.02);
// Simplified for Swing: single shadow layer approximation
// blur ~8px, offset-y ~2px, color hsla(211, 3%, 15%, 0.15)

// --dwc-border-radius for card corners
// --dwc-border-radius: var(--dwc-border-radius-s) = 0.25em ~ 4px
// For cards, larger radius is appropriate: --dwc-border-radius-l = 0.5em ~ 8px
```

### Token-to-UIDefaults Mapping Additions for Phase 7

```properties
# ALREADY mapped:
# --dwc-color-body-text = color:Label.foreground     (from Phase 2)
# --dwc-surface-3 = color:Panel.background            (from Phase 2)
# --dwc-border-radius = int:Button.arc, int:Component.arc  (from Phase 2)

# NEW mappings needed for Phase 7:

# Panel card style
# --dwc-border-radius-l will need to be added to CSS and mapped
# For now, Panel.arc can reuse Component.arc (4px) or be set programmatically

# TabbedPane - tab text colors
--dwc-color-primary-text = color:TabbedPane.selectedForeground, color:TabbedPane.underlineColor
--dwc-color-primary-text-light = color:TabbedPane.hoverForeground
--dwc-color-body-text = color:TabbedPane.foreground
--dwc-color-default = color:TabbedPane.contentAreaColor

# TabbedPane - disabled
--dwc-color-default-dark = color:TabbedPane.disabledForeground, color:TabbedPane.disabledUnderlineColor
```

### UIDefaults Keys Required by Phase 7

```
# JLabel (most already set by existing font/color mappings)
Label.foreground             -> text color (already mapped from --dwc-color-body-text)
Label.font                   -> font (already set by defaultFont in DwcLookAndFeel)
Label.disabledForeground     -> disabled text color (optional, DWC uses opacity approach)
Component.disabledOpacity    -> disabled opacity (already in UIDefaults)

# JPanel
Panel.background             -> surface color (already mapped from --dwc-surface-3)
Panel.arc                    -> card corner arc (new, set programmatically in initPanelDefaults)
Panel.shadowColor            -> card shadow color (new, computed from --dwc-shadow-color tokens)
Panel.shadowBlurRadius       -> card shadow blur (new, set programmatically)
Panel.shadowOffsetY          -> card shadow Y offset (new, set programmatically)

# JTabbedPane - Colors
TabbedPane.foreground        -> normal tab text color
TabbedPane.selectedForeground -> selected tab text color (primary-text)
TabbedPane.hoverForeground   -> hover tab text color (primary-text-light)
TabbedPane.disabledForeground -> disabled tab text color
TabbedPane.underlineColor    -> active indicator color (primary-text)
TabbedPane.disabledUnderlineColor -> disabled indicator color
TabbedPane.contentAreaColor  -> separator line color (default gray)
TabbedPane.hoverBackground   -> subtle hover background (optional, transparent by default)
TabbedPane.background        -> tab area background (surface-3 for consistency)

# JTabbedPane - Dimensions
TabbedPane.underlineHeight   -> indicator thickness (2px from DWC)
TabbedPane.tabArc            -> tab background corner arc (4px from --dwc-border-radius)
TabbedPane.tabHeight         -> minimum tab height (36px to match --dwc-size-m)
TabbedPane.tabInsets         -> padding inside tabs (4,8,4,8 from DWC space-xs)
TabbedPane.tabAreaInsets     -> margin around tab area
TabbedPane.contentBorderInsets -> insets around content area

# Shared (already in UIDefaults)
Component.focusWidth         -> focus ring width (3px)
Component.focusRingColor     -> focus ring color (already computed)
Component.disabledOpacity    -> disabled opacity (0.6)
Component.arc                -> default arc (4px from --dwc-border-radius)
```

### L&F Registration

```java
// In DwcLookAndFeel.initClassDefaults():
@Override
protected void initClassDefaults(UIDefaults table) {
    super.initClassDefaults(table);
    table.put("ButtonUI", "com.dwc.laf.ui.DwcButtonUI");
    table.put("TextFieldUI", "com.dwc.laf.ui.DwcTextFieldUI");
    table.put("CheckBoxUI", "com.dwc.laf.ui.DwcCheckBoxUI");
    table.put("RadioButtonUI", "com.dwc.laf.ui.DwcRadioButtonUI");
    table.put("ComboBoxUI", "com.dwc.laf.ui.DwcComboBoxUI");
    table.put("LabelUI", "com.dwc.laf.ui.DwcLabelUI");       // NEW
    table.put("PanelUI", "com.dwc.laf.ui.DwcPanelUI");       // NEW
    table.put("TabbedPaneUI", "com.dwc.laf.ui.DwcTabbedPaneUI"); // NEW
}
```

### DwcLookAndFeel initComponentDefaults Additions

```java
// In DwcLookAndFeel.initComponentDefaults():

// 10. Set up label-specific UIDefaults
initLabelDefaults(table);

// 11. Set up panel-specific UIDefaults
initPanelDefaults(table);

// 12. Set up tabbed pane-specific UIDefaults
initTabbedPaneDefaults(table);

// ---- Label defaults ----
private void initLabelDefaults(UIDefaults table) {
    // Label.foreground already mapped from --dwc-color-body-text
    // Label.font already set by defaultFont
    // No additional setup needed; DwcLabelUI reads Component.disabledOpacity
    LOG.fine("Initialized label defaults");
}

// ---- Panel defaults ----
private void initPanelDefaults(UIDefaults table) {
    // Panel.background already mapped from --dwc-surface-3
    table.put("Panel.arc", 8); // --dwc-border-radius-l = 0.5em ~ 8px for cards
    // Shadow color computed from --dwc-shadow-color tokens
    if (tokenMap != null) {
        // --dwc-shadow-color: H, S%, L% (e.g., "211, 3%, 15%")
        // Approximate as a semi-transparent dark color
        table.put("Panel.shadowColor",
                new ColorUIResource(new Color(15, 20, 25, 38))); // ~15% alpha
    }
    LOG.fine("Initialized panel defaults");
}

// ---- TabbedPane defaults ----
private void initTabbedPaneDefaults(UIDefaults table) {
    // Colors computed from CSS tokens
    // --dwc-color-primary-text = hsl(211, 100%, 35%) => used for selected tab + underline
    // --dwc-color-primary-text-light = hsl(211, 100%, 40%) => hover text
    // --dwc-color-default = hsl(211, 38%, 90%) => separator line
    // These colors already exist as token-computed values or can be mapped

    table.put("TabbedPane.underlineHeight", 2);
    table.put("TabbedPane.tabArc", UIManager.getInt("Component.arc"));
    table.put("TabbedPane.tabHeight", 36);
    table.put("TabbedPane.tabInsets", new InsetsUIResource(4, 8, 4, 8));
    table.put("TabbedPane.tabAreaInsets", new InsetsUIResource(0, 0, 0, 0));
    table.put("TabbedPane.contentBorderInsets", new InsetsUIResource(4, 0, 0, 0));

    LOG.fine("Initialized tabbed pane defaults");
}
```

### Tab Underline Indicator Positioning (All Placements)

```
TOP placement (default):
  Tab strip at top, content below
  Separator line at y = tab area bottom
  Underline at bottom edge of selected tab rect

BOTTOM placement:
  Tab strip at bottom, content above
  Separator line at y = tab area top
  Underline at top edge of selected tab rect

LEFT placement:
  Tab strip at left, content to right
  Separator line at x = tab area right
  Underline at right edge of selected tab rect (vertical)

RIGHT placement:
  Tab strip at right, content to left
  Separator line at x = tab area left
  Underline at left edge of selected tab rect (vertical)
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| BasicLabelUI chiseled disabled text (lighter+darker) | Opacity reduction matching DWC `--dwc-disabled-opacity` | This phase | Consistent disabled appearance across all components |
| All JPanels opaque with flat background | Opt-in card style with rounded corners + shadow | This phase | Modern card UI pattern matching DWC web components |
| BasicTabbedPaneUI raised/lowered tab borders | Flat tabs with underline active indicator | This phase | Modern tab strip appearance matching DWC web tabbed-pane |
| Dashed focus rectangle on tabs | Semi-transparent focus ring matching DWC | This phase | Consistent focus visual across all components |
| BasicTabbedPaneUI content area border (3D raised/lowered) | Simple separator line with primary-colored underline | This phase | Clean, modern separator matching DWC CSS |

**Deprecated/outdated:**
- `BasicLabelUI`'s embossed disabled text: Replaced with opacity-based disabled rendering for DWC consistency
- `BasicTabbedPaneUI`'s 3D tab borders: Replaced with flat, borderless tabs matching modern web design
- `BasicTabbedPaneUI`'s content area 3D border: Replaced with a simple separator line

## Open Questions

1. **Panel card shadow complexity**
   - What we know: DWC `--dwc-shadow-m` uses three stacked shadow layers with different blur/opacity values. `ShadowPainter` supports single-layer Gaussian blur.
   - What's unclear: Whether a single-layer approximation is visually acceptable, or if multiple `ShadowPainter.paintShadow()` calls are needed.
   - Recommendation: Start with a single shadow layer (blur=8, offsetY=2, color with ~15% alpha). Visually test and add layers if the result looks flat. Multiple calls to `ShadowPainter.paintShadow()` with different parameters can stack.

2. **Panel card mode activation: client property vs border**
   - What we know: The current plan uses `putClientProperty("dwc.panelStyle", "card")` to activate card mode.
   - What's unclear: Whether a custom `DwcCardBorder` (similar to `DwcButtonBorder`) would be a better API -- applications could set the border instead of a client property.
   - Recommendation: Use the client property approach. It's consistent with the `"dwc.buttonType"` pattern from Phase 4 and doesn't conflict with application-set borders. The border approach would require the application to import a DWC-specific class.

3. **TabbedPane tab text color: override paintText vs set foreground**
   - What we know: `BasicTabbedPaneUI.paintText()` calls `tabPane.getForegroundAt(tabIndex)`. FlatLaf overrides `paintText()` to set colors.
   - What's unclear: Whether we can set per-tab foreground colors via UIDefaults without overriding `paintText()`.
   - Recommendation: Override `paintText()` to resolve the correct color based on hover/selected/disabled state. This is the approach FlatLaf uses and it gives full control.

4. **TabbedPane scroll tab layout mode**
   - What we know: `BasicTabbedPaneUI` supports both `WRAP_TAB_LAYOUT` and `SCROLL_TAB_LAYOUT`. DWC web component has scrolling tabs.
   - What's unclear: Whether SCROLL_TAB_LAYOUT requires additional customization for the scroll buttons.
   - Recommendation: Support both layout modes. The underline indicator logic works the same for both. If scroll buttons look wrong with default styling, override `createScrollButton(int direction)` to return styled buttons (like the combobox arrow button pattern). Defer scroll button styling to a follow-up if it proves complex.

5. **TabbedPane disabled tab appearance**
   - What we know: DWC uses `--dwc-disabled-opacity` on disabled tabs. BasicTabbedPaneUI handles disabled state via `tabPane.isEnabledAt(tabIndex)`.
   - What's unclear: Whether to paint the entire tab at reduced opacity or just change the text color.
   - Recommendation: Use opacity reduction for disabled tabs (consistent with all other DWC components). In `paintTab()` or the individual paint methods, check `isEnabledAt()` and wrap painting in `StateColorResolver.paintWithOpacity()`.

6. **Panel.arc value: 4px vs 8px for card style**
   - What we know: `--dwc-border-radius` (small) = 0.25em ~ 4px. `--dwc-border-radius-l` (large) = 0.5em ~ 8px. Cards in web UIs typically use larger radii.
   - What's unclear: Which radius best matches the DWC card appearance.
   - Recommendation: Use 8px (matching `--dwc-border-radius-l`). This is a constant in `initPanelDefaults()` that's easy to tune visually. Note: the `--dwc-border-radius-l` token is not currently in the CSS but could be added. For now, hardcode 8 in the init method.

## Sources

### Primary (HIGH confidence)
- DWC `dwc-tabbed-pane.scss` (local project) - Tab strip separator border, active indicator height (2px), indicator color (`--dwc-color-primary-text`), placement variants (top/bottom/left/right)
- DWC `dwc-tab.scss` (local project) - Individual tab styling: transparent default background, hover/active/disabled states, font-weight (semibold), padding (`--dwc-space-xs`), border-radius, focus ring
- DWC `dwc-tab-panel.scss` (local project) - Content panel styling: transparent border, focus ring on panel
- DWC `default-light.css` (local project) - All token values including surfaces (`--dwc-surface-3 = hsl(0,0%,100%)`), shadows (`--dwc-shadow-m`), typography tokens
- DWC `token-mapping.properties` (local project) - Existing mappings for Label.foreground, Panel.background
- Project Phase 3 utilities (local project) - PaintUtils, ShadowPainter, FocusRingPainter, StateColorResolver
- Project Phase 4-6 UI delegates (local project) - Established patterns for createUI, installDefaults, painting pipeline, hover tracking
- [BasicTabbedPaneUI JDK 21 API](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/swing/plaf/basic/BasicTabbedPaneUI.html) - paintTab, paintTabBackground, paintTabBorder, paintFocusIndicator, paintContentBorder*, installDefaults, WRAP/SCROLL layout
- [BasicLabelUI JDK 21 API](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/swing/plaf/basic/BasicLabelUI.html) - paint, paintEnabledText, paintDisabledText, installDefaults

### Secondary (MEDIUM confidence)
- [FlatLaf TabbedPane documentation](https://www.formdev.com/flatlaf/components/tabbedpane/) - Complete UIDefaults keys, underlineColor, tabSelectionHeight, tabArc, contentSeparatorHeight, hover/focus colors, tab type (underlined/card)
- [FlatLaf FlatTabbedPaneUI source](https://github.com/JFormDesigner/FlatLaf/blob/main/flatlaf-core/src/main/java/com/formdev/flatlaf/ui/FlatTabbedPaneUI.java) - Architectural approach: paintTabSelection for underline, hover via setRolloverTab, isTabbedPaneOrChildFocused for focus tracking, Path2D content border
- [FlatLaf Customizing documentation](https://www.formdev.com/flatlaf/customizing/) - General UIDefaults customization patterns

### Tertiary (LOW confidence)
- None -- all findings verified against source code or official documentation

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All classes are JDK standard library or already-implemented Phase 3 utilities; FlatLaf reference architecture is well-understood
- Architecture (Label): HIGH - Minimal delegate; BasicLabelUI contract is simple and stable
- Architecture (Panel): HIGH - BasicPanelUI is trivial; card-style is a straightforward opt-in pattern using existing ShadowPainter
- Architecture (TabbedPane): MEDIUM-HIGH - Most complex of the three. BasicTabbedPaneUI has many protected methods to override. FlatLaf validates the underline-indicator approach, and DWC SCSS clearly defines the visual spec. The protected method contracts (`paintTabBackground`, `paintTabBorder`, `paintContentBorder*`) are stable JDK APIs. Downgraded slightly from HIGH because tab placement variations (LEFT/RIGHT/BOTTOM) and scroll layout mode interaction need careful testing.
- Pitfalls: HIGH - Shadow clipping, chiseled text, rectangular fill behind rounded corners, underline positioning, and placement variations are all documented and verifiable
- Token mapping: HIGH - Most tokens already mapped; few new mappings needed for TabbedPane colors

**Research date:** 2026-02-10
**Valid until:** 2026-03-12 (stable domain; Swing component UI contracts do not change between JDK releases)
