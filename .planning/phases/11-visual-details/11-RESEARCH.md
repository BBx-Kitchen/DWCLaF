# Phase 11: Visual Details - Research

**Researched:** 2026-02-11
**Domain:** Swing L&F visual polish - four component rendering fixes
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **ProgressBar fill radius**: Fill (colored portion) must have fully rounded ends matching the track -- pill-shaped fill inside pill-shaped track. Match DWC rendering exactly -- reference DWC CSS source for exact radius values. Current issue: fill clips with square ends despite track having rounded corners.
- **ComboBox full rework**: Match DWC fully: chevron shape/size, separator line, border framing, overall proportions. DWC has a thin chevron (small, subtle) on the right side with a subtle separator line. Current Swing version has a wider arrow area with different coloring -- too prominent. Reference both DWC SCSS/CSS source files AND web screenshots for exact match.
- **TextField flat border**: Replace current 3D/inset border look with flat, single-pixel border. Rounded corners matching DWC input styling. No visible bevel, shadow, or 3D effect on the border.
- **Tree node icons**: Tree nodes must show BOTH the expand/collapse chevron AND a programmable icon. Default icon must be visible (not hidden) -- BBj renders a default icon when none is explicitly set. Current issue: when DWC L&F is applied, icons disappear entirely. Fix must preserve the ability for users to set custom icons per node.

### Claude's Discretion
- Exact implementation approach for each fix (which classes to modify, painting strategy)
- Order of fixes within the phase
- Whether to update gallery demo to better showcase fixed rendering

### Deferred Ideas (OUT OF SCOPE)
None
</user_constraints>

## Summary

Phase 11 is a focused polish phase addressing four specific visual rendering gaps between the Swing L&F and the DWC web components. All four fixes are localized to existing classes with well-understood painting pipelines. No new component delegates or architectural changes are needed.

The four fixes have distinct root causes: (1) ProgressBar fill uses `createRoundedShape` with a `--dwc-border-radius` arc (~4px) that is too small for the pill shape DWC uses (`--dwc-border-radius-xl` = 12px), and the fill shape itself has square left ends when partially filled; (2) ComboBox arrow button is oversized with no separator line; (3) TextField border already uses `PaintUtils.paintOutline` correctly but the border color and rendering may still look 3D due to BasicLookAndFeel insets or shadow; (4) Tree nodes lose their default icons entirely because `BasicLookAndFeel` does not set `Tree.leafIcon`, `Tree.openIcon`, or `Tree.closedIcon` and the L&F currently does not either.

**Primary recommendation:** Fix each component independently in its own plan step. Recommended order: ProgressBar (simplest), TextField (small border change), Tree icons (UIDefaults addition + renderer config), ComboBox (most complex rework). Update gallery last to showcase improvements.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Java Swing | 21+ | UI framework | Existing project foundation |
| BasicLookAndFeel | JDK 21 | Base L&F class | Project's chosen base |
| java.awt.geom | JDK 21 | Shape painting (RoundRectangle2D, Area, Path2D) | Already used throughout |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| PaintUtils | project | Rounded shapes, outlines, AA hints | All four fixes |
| DwcTextFieldBorder | project | Border painting for input-style components | TextField fix, ComboBox border |
| DwcLookAndFeel | project | UIDefaults initialization | Tree icon + ProgressBar arc changes |

### Alternatives Considered
None needed -- all work is within existing codebase classes.

## Architecture Patterns

### Existing Project Structure (unchanged)
```
src/main/java/com/dwc/laf/
  ui/
    DwcProgressBarUI.java     # Fix 1: fill arc
    DwcComboBoxUI.java        # Fix 2: full rework
    DwcTextFieldBorder.java   # Fix 3: flat border
    DwcTextFieldUI.java       # Fix 3: verify bg painting
    DwcTreeUI.java            # Fix 4: icon visibility
  DwcLookAndFeel.java         # Fix 4: UIDefaults for tree icons
  DwcComponentGallery.java    # Gallery updates
src/main/resources/com/dwc/laf/
  token-mapping.properties    # Possible ProgressBar arc token addition
```

### Pattern 1: Area Intersection Clipping (ProgressBar)
**What:** The fill shape is created with full rounded corners, then intersected with the track shape via `java.awt.geom.Area` to clip the fill to the track boundary.
**When to use:** When a fill must have its own rounded ends but stay contained within a parent shape's rounded boundary.
**Current code (DwcProgressBarUI line 131-139):**
```java
Shape fillShape;
if (pb.getOrientation() == JProgressBar.HORIZONTAL) {
    fillShape = PaintUtils.createRoundedShape(x, y, amountFull, height, arc);
} else {
    fillShape = PaintUtils.createRoundedShape(x, y + height - amountFull,
            width, amountFull, arc);
}
Area fillArea = new Area(fillShape);
fillArea.intersect(new Area(trackShape));
g2.fill(fillArea);
```
**Issue:** The `arc` value comes from `ProgressBar.arc` which is mapped from `--dwc-border-radius` (4px). DWC progressbar uses `--dwc-border-radius-xl` (12px). The fill needs a much larger arc to be pill-shaped. Additionally, when the fill width is small (e.g., 10-20%), the rounded shape degenerates properly via `PaintUtils.createRoundedShape` clamping, but the low arc value makes the fill appear to have square left ends.
**Fix approach:** Use a dedicated pill arc for the progressbar (height-based: `arc = height` for fully rounded ends) or map to `--dwc-border-radius-xl`. The track and fill both need `border-radius-xl` per DWC SCSS.

### Pattern 2: Custom Arrow Button (ComboBox)
**What:** `DwcComboBoxArrowButton` extends `JButton` (not `BasicArrowButton`) and overrides `paintComponent` to draw a stroked chevron.
**When to use:** Complete rendering control over the dropdown arrow area.
**Current issue:** The arrow button is 32x32px with just a chevron. DWC web has: (a) a 1px vertical separator line (`[part='suffix-separator']` with `background-color: var(--dwc-color-default-dark)`, `width: 1px`, `margin: var(--dwc-space-xs) 0`), and (b) a smaller icon area with a subtle chevron.
**Fix approach:** Paint separator line in arrow button's `paintComponent` (left edge, inset top/bottom by ~4px), reduce button width to ~24px, ensure chevron is proportionally smaller and matches DWC's icon.

### Pattern 3: Border Painting via paintOutline (TextField)
**What:** `DwcTextFieldBorder.paintBorder` uses `PaintUtils.paintOutline` for the even-odd fill border.
**When to use:** Flat rounded border around input components.
**Current state analysis:** The border already uses `PaintUtils.paintOutline` which creates a flat outline. The `borderWidth` comes from `--dwc-border-width: 1px`. The border should already be flat since `paintOutline` draws a ring shape with even-odd fill. The 3D appearance may be coming from residual `BasicTextFieldUI` behavior or insufficient border offset. Need to verify: (a) the border is truly flat (no shadow/bevel), (b) rounded corners match DWC input-wrapper `border-radius: var(--dwc-border-radius)` = 4px.
**Potential issues:** If `BasicLookAndFeel` installs a default border that gets combined somehow, or if the bg painting creates a visual inset effect.

### Pattern 4: UIDefaults Tree Icon Installation
**What:** `DwcLookAndFeel.initTreeDefaults()` sets expand/collapse icons but not the node type icons.
**When to use:** When `DefaultTreeCellRenderer` needs pre-configured default icons.
**Current state:** `BasicLookAndFeel` does NOT install `Tree.leafIcon`, `Tree.openIcon`, or `Tree.closedIcon` -- those come from Metal, Windows, GTK, etc. Since DwcLookAndFeel extends BasicLookAndFeel directly, these are null, causing `DefaultTreeCellRenderer` to display no icons.
**Fix approach:** Create simple default tree node icons (folder for open/closed, document for leaf) and install them via `initTreeDefaults`. Options: (a) paint programmatic icons using the existing stroked-icon pattern from `DwcTreeExpandIcon`, (b) use unicode/font-rendered icons, (c) simple geometric shapes. Programmatic icons matching the project's stroke pattern are most consistent.

### Anti-Patterns to Avoid
- **Modifying PaintUtils for one component's needs:** If ProgressBar needs a different arc, pass it as a parameter rather than changing global utility behavior.
- **Hardcoding pixel values that should come from tokens:** Use UIDefaults or direct token values wherever DWC CSS defines the number. However, reasonable pixel constants for small details (separator line width = 1px) are acceptable.
- **Replacing DefaultTreeCellRenderer entirely:** The renderer is configurable; set its icons and colors rather than writing a custom renderer. Users may set their own renderer, and the fix must work with the default.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Rounded shape creation | Manual RoundRectangle2D construction | `PaintUtils.createRoundedShape` | Handles degeneration (rect, ellipse) automatically |
| Flat outline painting | BasicStroke + draw() for borders | `PaintUtils.paintOutline` (even-odd fill) | Pixel-perfect at all scales, no stroke alignment issues |
| Tree node icons | Loading icon images from disk | Programmatic Icon implementations (`DwcTreeExpandIcon` pattern) | Zero-dependency, resolution-independent, theme-color-aware |
| Focus ring | Custom focus painting | `FocusRingPainter.paintFocusRing` | Already consistent across all components |

**Key insight:** All painting patterns needed for these fixes already exist in the project. This phase is about using them correctly with the right parameters, not building new infrastructure.

## Common Pitfalls

### Pitfall 1: ProgressBar Fill Arc vs Track Arc Mismatch
**What goes wrong:** If the fill shape has a different arc than the track, the fill's rounded corners poke outside the track's rounded boundary, or the fill appears to have flat ends while the track is rounded.
**Why it happens:** Currently both track and fill use the same `arc` from `ProgressBar.arc`. DWC uses `--dwc-border-radius-xl` (12px / 0.75em) for both track and fill. The current mapping puts `--dwc-border-radius` (4px) into `ProgressBar.arc`.
**How to avoid:** Use a dedicated arc value for ProgressBar that matches `--dwc-border-radius-xl`. For pill-shaped ends, the fill arc should equal the bar height (guarantees fully rounded ends). The Area intersection clips correctly when the fill arc >= track arc.
**Warning signs:** Flat left edge on the fill bar; visible corners at the track boundary.

### Pitfall 2: ComboBox Arrow Button Layout Interference
**What goes wrong:** Changing the arrow button size can cause BasicComboBoxUI's layout logic to mis-position the editor/display area.
**Why it happens:** `BasicComboBoxUI.getDefaultSize()` and `getDisplaySize()` account for arrow button width. The display area is `comboBox.width - arrowButton.width - insets`.
**How to avoid:** After changing arrow button preferred size, verify that the display area text is not clipped. BasicComboBoxUI calls `arrowButton.getPreferredSize().width` to compute the current value display area width.
**Warning signs:** Selected value text getting clipped; excessive white space on the right.

### Pitfall 3: TextField Border Appearing 3D Despite Flat Implementation
**What goes wrong:** Even though the border paint code draws a flat outline, the visual result looks 3D or inset.
**Why it happens:** The text field background may be slightly different from the surrounding panel background, creating a visual "well" effect. Or the border color is too dark relative to the background, creating an inset illusion. DWC uses `--dwc-input-border-color: var(--dwc-color-default-dark)` which is a subtle gray, not a strong contrast.
**How to avoid:** Ensure the border color is the correct DWC token value. Verify that the background inside the border matches `--dwc-input-background` = `--dwc-color-default-light`. The combination of light bg + subtle border + rounded corners should look flat.
**Warning signs:** Border looks like it has a shadow or is recessed into the surface.

### Pitfall 4: Tree Icons Null After Theme Switch
**What goes wrong:** After `updateComponentTreeUI()`, tree icons disappear again.
**Why it happens:** If icons are set on the renderer instance directly (not via UIDefaults), theme switching creates a new `DefaultTreeCellRenderer` that reads icons from UIDefaults. If UIDefaults has null icons, the new renderer has no icons.
**How to avoid:** Install icons in UIDefaults (`Tree.leafIcon`, `Tree.openIcon`, `Tree.closedIcon`) in `DwcLookAndFeel.initTreeDefaults()`. This ensures any new renderer created during theme switching picks them up.
**Warning signs:** Icons visible initially but disappear after switching themes via the gallery.

### Pitfall 5: DefaultTreeCellRenderer Icon Override
**What goes wrong:** User-set custom icons per node are lost when the L&F installs default icons.
**Why it happens:** `DefaultTreeCellRenderer.setLeafIcon/setOpenIcon/setClosedIcon` override the UIDefaults-provided icons. But if the L&F sets icons in UIDefaults AND the renderer copies them on install, custom per-node icons set before L&F initialization may be overwritten.
**How to avoid:** Only set icons in UIDefaults (not directly on the renderer). DefaultTreeCellRenderer's constructor reads from UIDefaults but respects explicit calls to `setLeafIcon()` etc. The order is: UIDefaults provides defaults, user code overrides per-node via renderer methods.
**Warning signs:** Custom icons set by the application are replaced by default icons.

## Code Examples

### Example 1: ProgressBar Pill-Shaped Fill
```java
// In DwcProgressBarUI.paintDeterminateContent():
// DWC uses border-radius-xl (12px) for both track and fill
// For pill shape, arc should equal the bar height (fully rounded ends)
int progressArc = Math.min(arc, Math.min(width, height));
// Or use a dedicated ProgressBar.arc mapped from --dwc-border-radius-xl

Shape trackShape = PaintUtils.createRoundedShape(x, y, width, height, progressArc);
g2.setColor(background);
g2.fill(trackShape);

if (amountFull > 0) {
    // Fill with same arc -- createRoundedShape clamps to min(w,h)
    // so narrow fills automatically become pill-shaped
    Shape fillShape = PaintUtils.createRoundedShape(x, y, amountFull, height, progressArc);
    Area fillArea = new Area(fillShape);
    fillArea.intersect(new Area(trackShape));
    g2.fill(fillArea);
}
```

### Example 2: ComboBox Separator Line in Arrow Button
```java
// In DwcComboBoxArrowButton.paintComponent():
// DWC combobox has [part='suffix-separator']:
//   background-color: var(--dwc-color-default-dark)
//   width: 1px
//   margin: var(--dwc-space-xs) 0   (4px top/bottom margin)
Color separatorColor = UIManager.getColor("ComboBox.buttonArrowColor");
if (separatorColor == null) separatorColor = Color.DARK_GRAY;

int h = getHeight();
int margin = 4; // --dwc-space-xs = 0.25rem = 4px
g2.setColor(separatorColor);
g2.drawLine(0, margin, 0, h - margin); // 1px line on left edge

// Then draw chevron to the right of separator
```

### Example 3: Installing Default Tree Icons
```java
// In DwcLookAndFeel.initTreeDefaults():
// Create simple programmatic icons matching DwcTreeExpandIcon's stroke style
table.put("Tree.openIcon", new DwcTreeNodeIcon(DwcTreeNodeIcon.Type.FOLDER_OPEN));
table.put("Tree.closedIcon", new DwcTreeNodeIcon(DwcTreeNodeIcon.Type.FOLDER_CLOSED));
table.put("Tree.leafIcon", new DwcTreeNodeIcon(DwcTreeNodeIcon.Type.FILE));
```

### Example 4: DwcTreeNodeIcon Implementation Pattern
```java
// Following DwcTreeExpandIcon's pattern: implements Icon, programmatic paint
public class DwcTreeNodeIcon implements Icon {
    enum Type { FOLDER_OPEN, FOLDER_CLOSED, FILE }

    private static final int SIZE = 16;
    private final Type type;

    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        Object[] saved = PaintUtils.setupPaintingHints(g2);
        Color color = UIManager.getColor("Tree.expandedIcon.color");
        // Paint simple geometric shapes:
        // FOLDER: rectangle with tab
        // FILE: rectangle with folded corner
        // Use stroked outlines, not filled shapes
        PaintUtils.restorePaintingHints(g2, saved);
        g2.dispose();
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `--dwc-border-radius` (4px) for ProgressBar | `--dwc-border-radius-xl` (12px) per DWC SCSS | Phase 11 fix | Pill-shaped track/fill |
| 32px arrow button, no separator | ~24px arrow button with 1px separator line | Phase 11 fix | Matches DWC combobox suffix pattern |
| paintOutline with correct tokens (already flat) | Verify/fix any residual 3D appearance | Phase 11 fix | Clean flat input border |
| No tree node icons (null from BasicLookAndFeel) | Programmatic icons installed in UIDefaults | Phase 11 fix | Visible default folder/file icons |

**Deprecated/outdated:** None -- all changes build on existing patterns.

## Research Findings Detail

### Finding 1: ProgressBar DWC SCSS Analysis (HIGH confidence)
**Source:** `dwc/src/components/dwc-progressbar/dwc-progressbar.scss` lines 102-106
```scss
[part~='front'],
[part~='back'] {
  border: var(--dwc-border-width) var(--dwc-border-style) var(--dwc-progressbar-border-color, var(--dwc-color-default-dark));
  border-radius: var(--dwc-border-radius-xl);
}
```
DWC uses `--dwc-border-radius-xl` = `0.75em` = **12px** (at 16px base font) for BOTH the front (overlay/unfilled) and back (progress fill) parts. This is significantly larger than the current `ProgressBar.arc` which inherits `--dwc-border-radius` = `--dwc-border-radius-s` = `0.25em` = **4px**.

**Action needed:** Either map a new token `--dwc-border-radius-xl` to `ProgressBar.arc` in `token-mapping.properties`, or hardcode `ProgressBar.arc = height` in `initProgressBarDefaults()` for a pill shape. The token approach is preferred for consistency.

### Finding 2: ComboBox DWC SCSS Analysis (HIGH confidence)
**Source:** `dwc/src/components/dwc-combobox/dwc-combobox.scss` lines 56-69
```scss
[part='suffix-icon'] {
  box-sizing: border-box !important;
  display: flex;
  padding: var(--dwc-space-xs);         // 4px
  transform-origin: center;
}

[part='suffix-separator'] {
  align-self: stretch;
  background-color: var(--dwc-color-default-dark);
  box-sizing: border-box;
  margin: var(--dwc-space-xs) 0;        // 4px top/bottom
  width: 1px;
}
```
DWC combobox has three visual elements in the suffix area: (1) a 1px separator line that is inset 4px from top/bottom, colored `--dwc-color-default-dark`, (2) a chevron icon with 4px padding, (3) the separator sits between the display area and the chevron.

The current Swing implementation has a 32x32 arrow button with just a chevron and no separator. The button also lacks the separator coloring.

**Also from dwc-choicebox:** `[part='suffix-separator']` has the same pattern: `background-color: var(--dwc-choicebox-separator-color, var(--dwc-color-default-dark))`, `width: 1px`, `margin: var(--dwc-space-xs) 0`.

### Finding 3: TextField Border State Analysis (HIGH confidence)
**Source:** `DwcTextFieldBorder.java` (current implementation)
The current border uses `PaintUtils.paintOutline` which creates a mathematically flat border ring using the even-odd fill rule. The border color is `TextField.borderColor` which maps from `--dwc-input-border-color` = `--dwc-color-default-dark` = `hsl(211, 38%, 85%)` -- a light gray.

The DWC input-base SCSS confirms the web styling:
```scss
[part='input-wrapper'] {
  border: var(--dwc-input-border-width, var(--dwc-border-width)) var(--dwc-input-border-style, var(--dwc-border-style)) var(--dwc-input-border-color, var(--dwc-color-default-dark));
  border-radius: var(--dwc-border-radius);
}
```
This is: `1px solid <default-dark>` with `border-radius: var(--dwc-border-radius)` = 4px.

The Swing implementation should already match this. The "3D/inset" appearance the user sees may come from:
- The `TextField.background` (`--dwc-input-background` = `--dwc-color-default-light` = `hsl(211, 38%, 95%)`) being slightly darker than the panel background (`--dwc-surface-3` = `hsl(0, 0%, 100%)`), creating a subtle well effect
- This is actually correct DWC behavior -- the input wrapper has a slightly tinted background

**Action needed:** Verify the current rendering is actually already flat. If the "3D" effect is just the background color difference (which IS correct per DWC), the fix may be minimal or just a confirmation. If there's an actual bevel/shadow being painted, identify its source.

### Finding 4: Tree Node Icon Analysis (HIGH confidence)
**Source:** `DwcLookAndFeel.java` `initTreeDefaults()` method and JDK `BasicLookAndFeel` source.

`BasicLookAndFeel.initComponentDefaults()` does NOT install `Tree.leafIcon`, `Tree.openIcon`, or `Tree.closedIcon`. These are installed by platform-specific L&Fs:
- **MetalLookAndFeel:** Installs `MetalIconFactory.TreeLeafIcon`, `TreeFolderIcon`, etc.
- **WindowsLookAndFeel:** Installs icons from the native Windows theme
- **GTKLookAndFeel:** Installs GTK-themed tree icons

Since `DwcLookAndFeel` extends `BasicLookAndFeel` directly and does not install these icons, `DefaultTreeCellRenderer` gets `null` for all three icon types, rendering no icons.

DWC tree-node SCSS shows the web component has:
```scss
[part='icons'] {
  --dwc-icon-size: 1.5em;           // ~24px at base 16px
}
[part~='icon'] {
  // Shows folder/file icons with configurable colors
}
```
The DWC web tree uses folder-open, folder-closed, and file icons by default.

**Action needed:** Create a `DwcTreeNodeIcon` class (following `DwcTreeExpandIcon` pattern) that paints simple folder/file icons. Install in UIDefaults via `initTreeDefaults()`. The icons should be 16x16 (standard Swing icon size for trees) and use `Tree.expandedIcon.color` for consistency.

### Finding 5: ProgressBar Arc Token Value Resolution (HIGH confidence)
**Source:** `token-mapping.properties` line 25 and `default-light.css`

Current mapping:
```properties
--dwc-border-radius = int:Button.arc, int:Component.arc, int:CheckBox.arc, int:ComboBox.arc, int:TextField.arc, int:Panel.arc, int:ProgressBar.arc
```

`--dwc-border-radius` resolves to `var(--dwc-border-radius-s)` = `0.25em`. At 16px base, this is **4px**.

DWC progressbar SCSS uses `--dwc-border-radius-xl` = `0.75em` = **12px**.

Options:
1. Add `--dwc-border-radius-xl` to token-mapping with `int:ProgressBar.arc` (removes it from the `--dwc-border-radius` mapping)
2. Hardcode `ProgressBar.arc` in `initProgressBarDefaults()` to use height-based pill value

Option 1 is cleaner because it stays token-driven. The `--dwc-border-radius-xl` token resolves to `0.75em` which the dimension parser converts to 12px.

## Open Questions

1. **TextField "3D" appearance specifics**
   - What we know: The border paint code is mathematically flat. The background color difference between input and panel is intentional DWC design.
   - What's unclear: Whether the user sees the background color difference as "3D" or if there's an actual bevel being painted somewhere. Need visual verification.
   - Recommendation: Run the gallery and visually inspect. If the border is truly flat (just the outline ring), the fix may be adjusting the background to be fully white, or the user may accept that the subtle tint is correct DWC behavior. If there's an actual bevel, find its source (likely a residual BasicTextFieldUI behavior).

2. **ComboBox border sharing with TextField**
   - What we know: ComboBox currently uses `DwcTextFieldBorder` for its border. ComboBox has its own `paint()` method that paints rounded background.
   - What's unclear: Whether the combobox rework needs its own border class or if `DwcTextFieldBorder` still works after the visual changes.
   - Recommendation: Keep sharing `DwcTextFieldBorder` -- the border/bg treatment should be identical between combobox and textfield per DWC input-base styling. The rework is about the arrow button area, not the border.

3. **Tree icon style preference**
   - What we know: DWC web uses SVG icons (folder/file). Swing needs programmatic Java2D-painted icons.
   - What's unclear: Exact icon shapes that best match DWC's folder/file SVGs.
   - Recommendation: Use simple recognizable shapes: folder = rectangle with small tab flap on top, file = rectangle with folded corner. Stroked outlines (not filled) for consistency with `DwcTreeExpandIcon`'s visual style. Color from `Tree.expandedIcon.color` (mapped from `--dwc-color-default-dark`).

## Sources

### Primary (HIGH confidence)
- `dwc/src/components/dwc-progressbar/dwc-progressbar.scss` -- border-radius-xl for track and fill
- `dwc/src/components/dwc-combobox/dwc-combobox.scss` -- suffix-separator and suffix-icon patterns
- `dwc/src/components/dwc-choicebox/dwc-choicebox.scss` -- choicebox separator pattern (same as combobox)
- `dwc/src/styles/components/_input-base.scss` -- input-wrapper border styling
- `dwc/src/styles/mixins/props/_borders.scss` -- all border-radius values
- `dwc/src/components/dwc-tree-node/dwc-tree-node.scss` -- tree node icon parts and styling
- `src/main/java/com/dwc/laf/ui/DwcProgressBarUI.java` -- current fill painting logic
- `src/main/java/com/dwc/laf/ui/DwcComboBoxUI.java` -- current arrow button implementation
- `src/main/java/com/dwc/laf/ui/DwcTextFieldBorder.java` -- current border painting
- `src/main/java/com/dwc/laf/ui/DwcTreeUI.java` -- current tree UI with no icon setup
- `src/main/java/com/dwc/laf/DwcLookAndFeel.java` -- initTreeDefaults, initProgressBarDefaults
- `src/main/resources/com/dwc/laf/token-mapping.properties` -- current token mappings
- `src/main/resources/com/dwc/laf/themes/default-light.css` -- resolved token values

### Secondary (MEDIUM confidence)
- JDK BasicLookAndFeel source -- confirmation that Tree.leafIcon/openIcon/closedIcon are NOT set by BasicLookAndFeel (verified by Grep: no matches for these keys in project source)

## Metadata

**Confidence breakdown:**
- ProgressBar fix: HIGH -- DWC SCSS clearly specifies border-radius-xl; fix is straightforward arc value change
- ComboBox rework: HIGH -- DWC SCSS shows exact separator/icon structure; implementation approach is clear
- TextField border: MEDIUM -- Current implementation may already be flat; need visual verification to confirm root cause
- Tree icons: HIGH -- Root cause clear (null icons from BasicLookAndFeel); fix pattern established by DwcTreeExpandIcon

**Research date:** 2026-02-11
**Valid until:** 2026-03-11 (stable domain, no external dependency changes expected)
