# Phase 10: More Controls - Research

**Researched:** 2026-02-10
**Domain:** Java Swing ComponentUI delegates for JTable, JTree, JScrollBar, JProgressBar, JToolTip
**Confidence:** HIGH

## Summary

Phase 10 extends the DWC Look & Feel with five additional Swing component UI delegates: JTable (row striping, header styling, selection highlighting), JTree (custom expand/collapse icons, selection styling), JScrollBar (thin modern track/thumb), JProgressBar (color variants), and JToolTip (rounded corners and shadow). These components are more complex than Phase 4-7 delegates because they involve data-driven rendering (table cells, tree nodes), composite sub-components (scrollbar buttons, table headers), and animation (progress bar indeterminate mode).

The codebase already has a mature, well-established pattern for UI delegates: extend `Basic*UI`, override `installDefaults()` to cache UIManager colors/dimensions, override `paint()` or specific paint methods for custom rendering, use `PaintUtils` / `FocusRingPainter` / `ShadowPainter` for shared effects, register in `DwcLookAndFeel.initClassDefaults()`, add token mappings to `token-mapping.properties`, and add initialization methods in `DwcLookAndFeel.initComponentDefaults()`. Each new component should follow this exact pattern. The existing `DwcTabbedPaneUI` (most complex existing delegate) and `DwcComboBoxUI` (composite sub-components) are the closest reference models.

FlatLaf (the leading modern Swing L&F) provides proven patterns for all five components. Key insights: table row striping uses `Table.alternateRowColor` in UIDefaults; tree expand/collapse icons are set via `Tree.expandedIcon` / `Tree.collapsedIcon` UIDefaults keys; scrollbar thin styling uses zero-size arrow buttons with `paintThumb()`/`paintTrack()` overrides; progress bar rounded rendering uses `RoundRectangle2D` with `Area` intersection; tooltip shadow is best done via a custom `Border` rather than in the UI delegate's `paint()`.

**Primary recommendation:** Follow the established per-component UI delegate pattern exactly. Implement in complexity order: JToolTip (simplest) -> JProgressBar -> JScrollBar -> JTree -> JTable (most complex). Each needs: token mappings, L&F init method, UI delegate class, unit tests, and gallery section.

## Standard Stack

### Core (zero external deps -- all JDK built-in)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `javax.swing.plaf.basic.BasicTableUI` | JDK 21+ | Base class for DwcTableUI | Standard Swing extension point for table L&F |
| `javax.swing.plaf.basic.BasicTreeUI` | JDK 21+ | Base class for DwcTreeUI | Standard Swing extension point for tree L&F |
| `javax.swing.plaf.basic.BasicScrollBarUI` | JDK 21+ | Base class for DwcScrollBarUI | Standard Swing extension point for scrollbar L&F |
| `javax.swing.plaf.basic.BasicProgressBarUI` | JDK 21+ | Base class for DwcProgressBarUI | Standard Swing extension point for progress bar L&F |
| `javax.swing.plaf.basic.BasicToolTipUI` | JDK 21+ | Base class for DwcToolTipUI | Standard Swing extension point for tooltip L&F |
| `javax.swing.table.DefaultTableCellRenderer` | JDK 21+ | Custom table cell renderer for row striping | Standard renderer base with proper L&F integration |
| `javax.swing.table.DefaultTableHeaderCellRenderer` | JDK 21+ | Custom header renderer | Not available -- use `JTableHeader.getDefaultRenderer()` pattern |

### Supporting (existing project infrastructure)

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `PaintUtils` | Phase 3 | Antialiased rendering, rounded shapes, outlines | All paint methods |
| `FocusRingPainter` | Phase 3 | Focus ring outside component bounds | Table focus, tree focus |
| `ShadowPainter` | Phase 3 | Cached Gaussian blur shadows | Tooltip shadow |
| `StateColorResolver` | Phase 3 | State-dependent color resolution + disabled opacity | All components |
| `CssTokenMap` | Phase 1 | Direct token access for complex values | Progress bar variants if needed |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Custom table cell renderer | Override BasicTableUI.paint() entirely | Cell renderer approach is more maintainable; paint() override is fragile across JDK versions |
| Custom tooltip Border for shadow | ShadowPainter in paint() | Border approach matches Swing conventions; paint() approach risks clipping |
| Animated scrollbar fade-in/out | Static thin scrollbar | Animation adds complexity; static thin scrollbar is sufficient for v1 |

## Architecture Patterns

### Recommended Project Structure (additions)

```
src/main/java/com/dwc/laf/
  ui/
    DwcTableUI.java              # JTable delegate
    DwcTableHeaderUI.java        # JTableHeader delegate
    DwcTreeUI.java               # JTree delegate
    DwcTreeExpandIcon.java       # Custom expand/collapse Icon
    DwcScrollBarUI.java          # JScrollBar delegate
    DwcProgressBarUI.java        # JProgressBar delegate
    DwcToolTipUI.java            # JToolTip delegate
    DwcToolTipBorder.java        # Rounded border with shadow insets
src/test/java/com/dwc/laf/
  ui/
    DwcTableUITest.java
    DwcTreeUITest.java
    DwcScrollBarUITest.java
    DwcProgressBarUITest.java
    DwcToolTipUITest.java
```

### Pattern 1: Per-Component UI Delegate (established project pattern)

**What:** Each component gets its own `ComponentUI` subclass extending `Basic*UI`, with per-component instances (not shared singletons).
**When to use:** Every new component delegate.
**Example:**
```java
// Source: Established project pattern from DwcButtonUI, DwcTabbedPaneUI
public class DwcProgressBarUI extends BasicProgressBarUI {

    private Color foreground;  // progress fill color
    private Color background;  // track color
    private int arc;

    public static ComponentUI createUI(JComponent c) {
        return new DwcProgressBarUI();  // per-component instance
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        // Cache from UIManager -- not UIDefaults directly
        foreground = UIManager.getColor("ProgressBar.foreground");
        background = UIManager.getColor("ProgressBar.background");
        arc = UIManager.getInt("ProgressBar.arc");
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        // Full custom paint -- do NOT call super.paint()
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            // ... custom rendering with PaintUtils
        } finally {
            g2.dispose();
        }
    }
}
```

### Pattern 2: Token Mapping Pipeline (established project pattern)

**What:** CSS tokens map to UIDefaults keys via `token-mapping.properties`. The L&F init methods install computed defaults that require more than simple mapping.
**When to use:** Every new component.
**Example:**
```properties
# In token-mapping.properties
--dwc-color-primary = ..., color:ProgressBar.foreground
--dwc-color-default-90 = ..., color:Table.alternateRowColor  (approximate)
--dwc-border-radius = ..., int:ProgressBar.arc
```

### Pattern 3: Variant Support via Client Property (established project pattern)

**What:** JProgressBar color variants selected via `putClientProperty("dwc.progressType", "success")`.
**When to use:** ProgressBar variants (primary, success, danger, warning, info).
**Example:**
```java
// Source: Established pattern from DwcButtonUI variant support
JProgressBar pb = new JProgressBar(0, 100);
pb.setValue(60);
pb.putClientProperty("dwc.progressType", "success");  // green fill
```

### Pattern 4: Custom Icon as UIDefaults (BasicTreeUI pattern)

**What:** Tree expand/collapse icons are set via UIDefaults keys `Tree.expandedIcon` and `Tree.collapsedIcon`. BasicTreeUI reads these automatically.
**When to use:** Tree expand/collapse chevrons.
**Example:**
```java
// In DwcLookAndFeel.initTreeDefaults()
table.put("Tree.expandedIcon", new DwcTreeExpandIcon(true));
table.put("Tree.collapsedIcon", new DwcTreeExpandIcon(false));
```

### Pattern 5: Zero-Size Buttons for Scrollbar (FlatLaf pattern)

**What:** Override `createDecreaseButton()` / `createIncreaseButton()` to return zero-dimension buttons, hiding arrow buttons for a minimal scrollbar.
**When to use:** DwcScrollBarUI.
**Example:**
```java
@Override
protected JButton createDecreaseButton(int orientation) {
    return createZeroButton();
}
@Override
protected JButton createIncreaseButton(int orientation) {
    return createZeroButton();
}
private JButton createZeroButton() {
    JButton btn = new JButton();
    btn.setPreferredSize(new Dimension(0, 0));
    btn.setMinimumSize(new Dimension(0, 0));
    btn.setMaximumSize(new Dimension(0, 0));
    btn.setFocusable(false);
    return btn;
}
```

### Anti-Patterns to Avoid

- **Calling super.paint() when doing full custom rendering:** Creates double-painting artifacts. Either do full custom paint OR selective super.paint() with specific method overrides.
- **Shared singleton UI delegates:** The project uses per-component instances. Do NOT use a shared instance pattern even if BasicUI defaults to it.
- **Hardcoded colors instead of UIManager lookups:** All colors must come from UIManager/UIDefaults so themes work.
- **Painting shadow in tooltip paint() instead of Border:** Shadow needs inset space. A custom Border provides proper insets; paint() alone causes clipping.
- **Using BasicTableUI.paint() override for row striping:** The paint() method is monolithic. Use a custom cell renderer or paintComponent on the viewport instead.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Antialiased rounded shapes | Manual Graphics2D setup | `PaintUtils.setupPaintingHints()`, `PaintUtils.createRoundedShape()`, `PaintUtils.paintRoundedBackground()` | Already handles hint save/restore, shape degeneration |
| Focus ring outside bounds | Manual path subtraction | `FocusRingPainter.paintFocusRing()` | Already handles outer arc expansion, even-odd fill |
| Box shadows | Manual blur code | `ShadowPainter.paintShadow()` | Already has Gaussian blur cache, HiDPI-aware |
| Disabled opacity rendering | Manual AlphaComposite | `StateColorResolver.paintWithOpacity()` | Already saves/restores composite |
| CSS token resolution | Direct CssTokenMap access | `token-mapping.properties` + `UIManager.getColor()` | Follows established mapping pipeline |
| Table cell rendering | Override BasicTableUI.paint() | `DefaultTableCellRenderer` subclass | Standard Swing pattern, avoids monolithic paint override |
| Table header rendering | Override BasicTableHeaderUI.paint() | Custom renderer via `JTableHeader.setDefaultRenderer()` | Renderer-based approach is standard |

**Key insight:** The project already has a complete painting utility stack (Phase 3). All five new components should use these utilities exclusively rather than duplicating any rendering logic. The token-mapping pipeline (Phase 2) handles most color/dimension injection without code changes.

## Common Pitfalls

### Pitfall 1: Table Row Striping vs. Viewport Background

**What goes wrong:** Row striping only paints for data rows, leaving empty space below the last row unstriped. This is visually jarring when the table is taller than its data.
**Why it happens:** The table UI delegate only paints cells that exist. Empty viewport space is painted by the viewport's background.
**How to avoid:** Override `paintComponent()` on the table itself to paint alternating stripes for the full viewport height, OR use `table.setFillsViewportHeight(true)` combined with a custom `prepareRenderer()` in a subclass. The simplest approach: set `table.setFillsViewportHeight(true)` and implement striping in the cell renderer.
**Warning signs:** Stripes stop at the last data row.

### Pitfall 2: Table Selection + Row Striping Interaction

**What goes wrong:** Selection highlight color overwrites row stripe color even when the selection is in the other row.
**Why it happens:** `DefaultTableCellRenderer` applies selection colors based on `isSelected` parameter, which overrides any stripe logic.
**How to avoid:** Custom renderer must check `isSelected` FIRST. If selected, use selection colors. If not selected, use alternating colors based on row index.
**Warning signs:** Selected rows show stripe color instead of selection color, or vice versa.

### Pitfall 3: JTableHeader is a Separate Component

**What goes wrong:** Registering only `TableUI` does not style the header.
**Why it happens:** `JTableHeader` has its own UI delegate class (`TableHeaderUI`), separate from `TableUI`.
**How to avoid:** Register BOTH `TableUI` and `TableHeaderUI` in `initClassDefaults()`. Style the header separately.
**Warning signs:** Table body looks themed but header looks like default Basic L&F.

### Pitfall 4: BasicTreeUI Icon Handling

**What goes wrong:** Custom expand/collapse icons don't appear, or appear alongside default icons.
**Why it happens:** `BasicTreeUI` reads `Tree.expandedIcon` and `Tree.collapsedIcon` from UIDefaults during `installDefaults()`. If you set them after UI installation, they are ignored. Also, node icons (leaf/open/closed) are different from expand/collapse icons.
**How to avoid:** Set icons in UIDefaults BEFORE the tree is created, or set them in `DwcTreeUI.installDefaults()` via `setExpandedIcon()` / `setCollapsedIcon()`.
**Warning signs:** Default triangle/folder icons appear instead of custom chevrons.

### Pitfall 5: ScrollBar Thumb Minimum Size

**What goes wrong:** Very thin scrollbar has invisible or unusable thumb.
**Why it happens:** `BasicScrollBarUI.getMinimumThumbSize()` returns a size based on button sizes. With zero-size buttons, the minimum can be too small.
**How to avoid:** Override `getMinimumThumbSize()` to return a sensible minimum (e.g., 20x20 for thin bars).
**Warning signs:** Thumb disappears or becomes a single pixel.

### Pitfall 6: ProgressBar Indeterminate Animation Lifecycle

**What goes wrong:** Indeterminate animation continues after component is removed, causing memory leaks.
**Why it happens:** `BasicProgressBarUI` uses a `Timer` for animation. If not properly stopped, it leaks.
**How to avoid:** Let `BasicProgressBarUI` manage the timer via `super.installUI()` / `super.uninstallUI()`. Do NOT create your own timer. Override `paintIndeterminate()` for custom look, but let the base class handle the animation index.
**Warning signs:** Memory grows over time, or animation stops unexpectedly.

### Pitfall 7: Tooltip Shadow Clipping

**What goes wrong:** Tooltip shadow is clipped because the tooltip component bounds don't include shadow space.
**Why it happens:** Swing tooltip positioning sizes the component to its preferred size. If the border doesn't account for shadow insets, the shadow paints outside the component and gets clipped.
**How to avoid:** Use a custom `Border` (like `DwcToolTipBorder`) that returns enlarged `Insets` to account for shadow blur radius. Paint the shadow inside the border's `paintBorder()` method. The content area is then inside the shadow insets.
**Warning signs:** Shadow appears cut off on edges, or tooltip content overlaps with shadow area.

### Pitfall 8: JTable Grid Lines with Custom Rendering

**What goes wrong:** Grid lines appear doubled, offset, or inconsistently colored.
**Why it happens:** BasicTableUI paints grid lines in its `paint()` method. If you override paint AND the table has `showGrid=true`, both paint grid lines.
**How to avoid:** Set `table.setShowGrid(false)` and `table.setIntercellSpacing(new Dimension(0, 0))` during `installDefaults()`. Paint custom separators in the cell renderer or in a targeted paint override.
**Warning signs:** Thick or doubled grid lines.

## Code Examples

Verified patterns from the existing codebase and official Swing API:

### DwcProgressBarUI - Rounded Determinate Bar

```java
// Source: FlatLaf FlatProgressBarUI pattern + existing DwcButtonUI paint pattern
@Override
protected void paintDeterminate(Graphics g, JComponent c) {
    JProgressBar pb = (JProgressBar) c;
    Insets insets = pb.getInsets();
    int x = insets.left;
    int y = insets.top;
    int width = pb.getWidth() - insets.left - insets.right;
    int height = pb.getHeight() - insets.top - insets.bottom;

    Graphics2D g2 = (Graphics2D) g.create();
    try {
        Object[] saved = PaintUtils.setupPaintingHints(g2);

        // Paint track (background)
        g2.setColor(background);
        g2.fill(PaintUtils.createRoundedShape(x, y, width, height, arc));

        // Paint progress fill
        int amountFull = getAmountFull(insets, width, height);
        if (amountFull > 0) {
            Color fillColor = resolveVariantColor(pb);
            g2.setColor(fillColor);
            // Clip to track shape to prevent overflow at corners
            Shape trackShape = PaintUtils.createRoundedShape(x, y, width, height, arc);
            Shape fillShape = PaintUtils.createRoundedShape(x, y, amountFull, height, arc);
            Area fillArea = new Area(fillShape);
            fillArea.intersect(new Area(trackShape));
            g2.fill(fillArea);
        }

        PaintUtils.restorePaintingHints(g2, saved);
    } finally {
        g2.dispose();
    }
}
```

### DwcScrollBarUI - Thin Modern Scrollbar

```java
// Source: FlatLaf FlatScrollBarUI pattern + BasicScrollBarUI API
@Override
protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
    if (thumbBounds.isEmpty()) return;

    Graphics2D g2 = (Graphics2D) g.create();
    try {
        Object[] saved = PaintUtils.setupPaintingHints(g2);

        Color thumbColor = isThumbRollover() ? hoverThumbColor : this.thumbColor;
        int inset = 3;  // padding from track edge
        g2.setColor(thumbColor);
        g2.fill(PaintUtils.createRoundedShape(
            thumbBounds.x + inset,
            thumbBounds.y + inset,
            thumbBounds.width - inset * 2,
            thumbBounds.height - inset * 2,
            thumbArc));

        PaintUtils.restorePaintingHints(g2, saved);
    } finally {
        g2.dispose();
    }
}
```

### DwcToolTipBorder - Rounded Border with Shadow Insets

```java
// Source: Existing DwcButtonBorder pattern + ShadowPainter
public class DwcToolTipBorder extends AbstractBorder {
    private static final int SHADOW_SIZE = 6;
    private static final int ARC = 8;

    @Override
    public Insets getBorderInsets(Component c) {
        // Account for shadow on all sides
        return new Insets(
            SHADOW_SIZE + 4,     // top
            SHADOW_SIZE + 8,     // left
            SHADOW_SIZE + 4,     // bottom
            SHADOW_SIZE + 8      // right
        );
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            // Paint shadow behind content area
            float cx = x + SHADOW_SIZE;
            float cy = y + SHADOW_SIZE;
            float cw = w - SHADOW_SIZE * 2;
            float ch = h - SHADOW_SIZE * 2;
            ShadowPainter.paintShadow(g2, cx, cy, cw, ch,
                ARC, 4f, 0, 2, shadowColor);

            // Paint rounded border outline
            Object[] saved = PaintUtils.setupPaintingHints(g2);
            g2.setColor(borderColor);
            PaintUtils.paintOutline(g2, cx, cy, cw, ch, 1f, ARC);
            PaintUtils.restorePaintingHints(g2, saved);
        } finally {
            g2.dispose();
        }
    }
}
```

### DwcTreeExpandIcon - Custom Chevron Icon

```java
// Source: Existing DwcCheckBoxIcon pattern + DwcComboBoxArrowButton chevron pattern
public class DwcTreeExpandIcon implements Icon {
    private final boolean expanded;
    private static final int SIZE = 12;

    public DwcTreeExpandIcon(boolean expanded) {
        this.expanded = expanded;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Object[] saved = PaintUtils.setupPaintingHints(g2);
            Color color = UIManager.getColor("Tree.expandedIcon.color");
            if (color == null) color = Color.DARK_GRAY;
            g2.setColor(color);

            float cx = x + SIZE / 2f;
            float cy = y + SIZE / 2f;
            float arrowSize = 5f;

            Path2D.Float arrow = new Path2D.Float();
            if (expanded) {
                // Downward chevron
                arrow.moveTo(cx - arrowSize / 2, cy - arrowSize / 4);
                arrow.lineTo(cx, cy + arrowSize / 4);
                arrow.lineTo(cx + arrowSize / 2, cy - arrowSize / 4);
            } else {
                // Rightward chevron
                arrow.moveTo(cx - arrowSize / 4, cy - arrowSize / 2);
                arrow.lineTo(cx + arrowSize / 4, cy);
                arrow.lineTo(cx - arrowSize / 4, cy + arrowSize / 2);
            }
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(arrow);

            PaintUtils.restorePaintingHints(g2, saved);
        } finally {
            g2.dispose();
        }
    }

    @Override
    public int getIconWidth() { return SIZE; }

    @Override
    public int getIconHeight() { return SIZE; }
}
```

### Table Row Striping via Custom Cell Renderer

```java
// Source: FlatLaf FlatTableUI pattern adapted to project conventions
public class DwcTableCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (isSelected) {
            setBackground(UIManager.getColor("Table.selectionBackground"));
            setForeground(UIManager.getColor("Table.selectionForeground"));
        } else {
            Color bg = (row % 2 == 0)
                ? UIManager.getColor("Table.background")
                : UIManager.getColor("Table.alternateRowColor");
            setBackground(bg != null ? bg : table.getBackground());
            setForeground(table.getForeground());
        }

        // Remove focus border for cleaner look
        setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        return this;
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Metal-based L&F with gradient fills | Flat L&F with solid colors and rounded shapes | ~2019 (FlatLaf 0.x) | Modern L&Fs use no gradients, thin borders, subtle shadows |
| Scrollbar with arrow buttons and 3D track | Thin minimal scrollbar, no arrow buttons, hover fade | ~2018 (macOS-style) | Modern scrollbars are invisible until hovered |
| Table with visible grid lines | Table with alternating rows, no grid lines | ~2020 | Cleaner data presentation, easier row tracking |
| Metal tree with folder/file icons | Minimal tree with chevron expand/collapse | ~2019 | Cleaner tree appearance, less visual noise |
| System tooltip with squared border | Rounded tooltip with subtle shadow | ~2020 | Consistent with modern design systems |

**Deprecated/outdated:**
- Metal L&F gradient painting: replaced by flat solid colors
- Default tree folder/document icons: replaced by minimal or no icons
- Thick scrollbar with arrow buttons: replaced by thin overlay-style

## Token Mapping Plan

### New CSS token mappings needed in `token-mapping.properties`:

```properties
# Table
--dwc-surface-2 = ..., color:Table.alternateRowColor
--dwc-color-primary = ..., color:Table.selectionBackground  (already partly mapped)
--dwc-color-on-primary-text = ..., color:Table.selectionForeground
--dwc-color-default-dark = ..., color:TableHeader.bottomSeparatorColor

# Tree
--dwc-color-default-dark = ..., color:Tree.expandedIcon.color
--dwc-color-primary = ..., color:Tree.selectionBackground  (need to add)
--dwc-color-on-primary-text = ..., color:Tree.selectionForeground (need to add)

# ScrollBar
--dwc-color-default-dark = ..., color:ScrollBar.thumbColor
--dwc-color-default = ..., color:ScrollBar.trackColor
--dwc-color-primary-light = ..., color:ScrollBar.hoverThumbColor

# ProgressBar
--dwc-color-primary = ..., color:ProgressBar.foreground (already mapped)
--dwc-color-default = ..., color:ProgressBar.background
--dwc-color-success = ..., color:ProgressBar.success.foreground
--dwc-color-danger = ..., color:ProgressBar.danger.foreground
--dwc-color-warning = ..., color:ProgressBar.warning.foreground
--dwc-color-info = ..., color:ProgressBar.info.foreground

# ToolTip
--dwc-surface-3 = ..., color:ToolTip.background
--dwc-color-body-text = ..., color:ToolTip.foreground
--dwc-color-default-dark = ..., color:ToolTip.borderColor
```

### New UIDefaults keys needed (set in L&F init methods):

| Key | Type | Source | Purpose |
|-----|------|--------|---------|
| `Table.alternateRowColor` | Color | `--dwc-surface-2` | Even/odd row striping |
| `Table.selectionBackground` | Color | `--dwc-color-primary` | Selected row highlight |
| `Table.selectionForeground` | Color | `--dwc-color-on-primary-text` | Selected row text |
| `TableHeader.bottomSeparatorColor` | Color | `--dwc-color-default-dark` | Header bottom border |
| `Tree.expandedIcon` | Icon | computed | Downward chevron |
| `Tree.collapsedIcon` | Icon | computed | Rightward chevron |
| `Tree.selectionBackground` | Color | `--dwc-color-primary` | Selected node highlight |
| `Tree.selectionForeground` | Color | `--dwc-color-on-primary-text` | Selected node text |
| `Tree.rowHeight` | int | 24 (computed) | Row height for tree nodes |
| `ScrollBar.thumbColor` | Color | `--dwc-color-default-dark` | Scrollbar thumb |
| `ScrollBar.trackColor` | Color | `--dwc-color-default` | Scrollbar track |
| `ScrollBar.hoverThumbColor` | Color | `--dwc-color-primary-light` | Thumb on hover |
| `ScrollBar.thumbArc` | int | 8 (computed) | Rounded thumb corners |
| `ScrollBar.width` | int | 10 (computed) | Thin scrollbar width |
| `ProgressBar.arc` | int | `--dwc-border-radius` | Rounded progress bar |
| `ProgressBar.foreground` | Color | `--dwc-color-primary` | Default fill (already mapped) |
| `ProgressBar.background` | Color | `--dwc-color-default` | Track background |
| `ToolTip.background` | Color | `--dwc-surface-3` | Tooltip background |
| `ToolTip.foreground` | Color | `--dwc-color-body-text` | Tooltip text |
| `ToolTip.border` | Border | computed | DwcToolTipBorder with shadow |
| `ToolTip.borderColor` | Color | `--dwc-color-default-dark` | Border outline |

## Implementation Order (recommended)

1. **JToolTip** (simplest -- just border, background, rounded corners, shadow)
2. **JProgressBar** (moderate -- custom paint, color variants via client property)
3. **JScrollBar** (moderate -- paintThumb/paintTrack, zero-size buttons)
4. **JTree** (complex -- custom icons, selection styling, cell renderer)
5. **JTable + JTableHeader** (most complex -- row striping, header, selection, renderer)

Each component follows the same implementation sequence:
1. Token mappings in `token-mapping.properties`
2. Init method in `DwcLookAndFeel`
3. Register in `initClassDefaults()`
4. UI delegate class
5. Supporting classes (Icon, Border, Renderer)
6. Unit tests
7. Gallery section in `DwcComponentGallery`

## L&F Registration Pattern

```java
// In DwcLookAndFeel.initClassDefaults()
table.put("TableUI", "com.dwc.laf.ui.DwcTableUI");
table.put("TableHeaderUI", "com.dwc.laf.ui.DwcTableHeaderUI");
table.put("TreeUI", "com.dwc.laf.ui.DwcTreeUI");
table.put("ScrollBarUI", "com.dwc.laf.ui.DwcScrollBarUI");
table.put("ProgressBarUI", "com.dwc.laf.ui.DwcProgressBarUI");
table.put("ToolTipUI", "com.dwc.laf.ui.DwcToolTipUI");
```

## Open Questions

1. **Table header: separate UI delegate or renderer-only?**
   - What we know: FlatLaf uses both `FlatTableHeaderUI` and a custom renderer. BasicTableHeaderUI is a separate delegate class.
   - What's unclear: Whether a full `DwcTableHeaderUI` is needed or if a custom renderer installed via UIDefaults is sufficient for the Phase 10 requirements (which only say "header styling").
   - Recommendation: Start with a custom header renderer installed via `initTableDefaults()`. If that is insufficient (e.g., need custom bottom separator painting), add `DwcTableHeaderUI`. This is lower risk.

2. **ProgressBar indeterminate mode styling**
   - What we know: XTND-04 specifies color variants but does not mention indeterminate mode.
   - What's unclear: Whether indeterminate mode needs custom painting or if BasicProgressBarUI's default is acceptable.
   - Recommendation: Override `paintIndeterminate()` with a simple bouncing rounded rect using the variant color. Low effort, high visual consistency.

3. **ScrollBar: static or animated hover?**
   - What we know: Modern scrollbars often fade in on hover. FlatLaf supports both static and animated styles.
   - What's unclear: Whether the DWC web components use animated scrollbar hover.
   - Recommendation: Start with static hover color change. Animation can be added later if needed.

4. **Tree: show default node icons?**
   - What we know: Modern UIs often hide default folder/leaf icons. FlatLaf has a `showDefaultIcons` property.
   - What's unclear: Whether DWC trees show node icons.
   - Recommendation: Keep default node icons for now (they're set by the application's renderer, not the L&F). Focus on expand/collapse icons and selection styling per XTND-02.

## Sources

### Primary (HIGH confidence)
- Existing codebase: `DwcButtonUI.java`, `DwcTabbedPaneUI.java`, `DwcComboBoxUI.java` -- established patterns
- Existing codebase: `token-mapping.properties`, `DwcLookAndFeel.java` -- mapping pipeline
- Existing codebase: `PaintUtils.java`, `FocusRingPainter.java`, `ShadowPainter.java` -- painting utilities
- [Oracle Java SE 22 BasicTableUI](https://docs.oracle.com/en/java/javase/22/docs/api/java.desktop/javax/swing/plaf/basic/BasicTableUI.html) -- overridable methods
- [Oracle Java SE 22 BasicTreeUI](https://docs.oracle.com/en/java/javase/22/docs/api/java.desktop/javax/swing/plaf/basic/BasicTreeUI.html) -- expand/collapse icon API
- [Oracle Java SE 22 BasicScrollBarUI](https://docs.oracle.com/en/java/javase/22/docs/api/java.desktop/javax/swing/plaf/basic/BasicScrollBarUI.html) -- thumb/track/button API
- [Oracle Java SE 22 BasicProgressBarUI](https://docs.oracle.com/en/java/javase/22/docs/api/java.desktop/javax/swing/plaf/basic/BasicProgressBarUI.html) -- paint determinate/indeterminate API
- [Oracle Java SE 22 BasicToolTipUI](https://docs.oracle.com/en/java/javase/22/docs/api/java.desktop/javax/swing/plaf/basic/BasicToolTipUI.html) -- paint/installDefaults API

### Secondary (MEDIUM confidence)
- [FlatLaf FlatTableUI](https://github.com/JFormDesigner/FlatLaf/blob/main/flatlaf-core/src/main/java/com/formdev/flatlaf/ui/FlatTableUI.java) -- row striping, selection arc, alternateRowColor pattern
- [FlatLaf FlatScrollBarUI](https://github.com/JFormDesigner/FlatLaf/blob/main/flatlaf-core/src/main/java/com/formdev/flatlaf/ui/FlatScrollBarUI.java) -- thin scrollbar, thumb insets, hover colors
- [FlatLaf FlatProgressBarUI](https://github.com/JFormDesigner/FlatLaf/blob/main/flatlaf-core/src/main/java/com/formdev/flatlaf/ui/FlatProgressBarUI.java) -- rounded track, Area intersection pattern
- [FlatLaf FlatTreeUI](https://github.com/JFormDesigner/FlatLaf/blob/main/flatlaf-core/src/main/java/com/formdev/flatlaf/ui/FlatTreeUI.java) -- icon management, wide selection
- [FlatLaf FlatToolTipUI](https://github.com/JFormDesigner/FlatLaf/blob/main/flatlaf-core/src/main/java/com/formdev/flatlaf/ui/FlatToolTipUI.java) -- border-based shadow approach
- [FlatLaf FlatTableHeaderUI](https://github.com/JFormDesigner/FlatLaf/blob/main/flatlaf-core/src/main/java/com/formdev/flatlaf/ui/FlatTableHeaderUI.java) -- header separator, hover

### Tertiary (LOW confidence)
- None -- all findings verified against official API docs and established codebase patterns

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- all JDK built-in, zero external deps, verified against Java SE 22 docs
- Architecture: HIGH -- follows established project patterns from Phases 3-9, verified in codebase
- Pitfalls: HIGH -- verified against FlatLaf reference implementation and official Swing API docs
- Code examples: HIGH -- based on existing codebase patterns (DwcButtonUI, DwcTabbedPaneUI, DwcComboBoxUI)

**Research date:** 2026-02-10
**Valid until:** 2026-03-10 (stable domain -- Swing API is mature and unchanging)
