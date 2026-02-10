# Domain Pitfalls: Custom Swing Look & Feel with CSS Integration

**Domain:** Swing Look & Feel Engine with CSS Custom Property Support
**Researched:** 2026-02-10
**Confidence:** MEDIUM (based on training data - web research tools unavailable)

## Critical Pitfalls

These mistakes cause rewrites, performance disasters, or fundamental architectural problems.

### Pitfall 1: Non-EDT Painting Operations
**What goes wrong:** Custom painting code executes outside the Event Dispatch Thread, causing race conditions, rendering artifacts, and intermittent crashes. Particularly dangerous when CSS parsing triggers component repaints.

**Why it happens:**
- CSS file watching triggers updates from background threads
- Asynchronous CSS loading completes on worker threads
- Developers call `repaint()` or modify UIDefaults from CSS parser callbacks
- Component state changes during paint cycle

**Consequences:**
- Random NullPointerExceptions in paint methods
- Visual corruption that's hard to reproduce
- Complete UI freezes when EDT deadlocks
- State inconsistencies between model and view

**Prevention:**
```java
// BAD - CSS update triggers direct repaint from parser thread
cssParser.onUpdate(styles -> {
    component.setBackground(styles.getColor());
    component.repaint(); // DANGER: not on EDT
});

// GOOD - Marshal to EDT first
cssParser.onUpdate(styles -> {
    SwingUtilities.invokeLater(() -> {
        component.setBackground(styles.getColor());
        component.repaint();
    });
});
```

**Detection:**
- Log warning when `SwingUtilities.isEventDispatchThread()` returns false during paint
- Enable EDT violation checking: `-Xcheck:edt` or `CheckThreadViolationRepaintManager`
- Intermittent exceptions in paint methods
- Artifacts appear/disappear based on timing

**Phase impact:** Must be addressed in Phase 1 (Core L&F architecture) - retrofitting thread safety is nearly impossible.

---

### Pitfall 2: CSS Variable Resolution Infinite Loops
**What goes wrong:** Circular `var()` references or deeply nested variable chains cause stack overflow or infinite parsing loops.

**Why it happens:**
```css
:root {
    --color-primary: var(--color-accent);
    --color-accent: var(--color-primary); /* Circular! */
}

.button {
    background: var(--a, var(--b, var(--c, var(--d, var(--e))))); /* Deep nesting */
}
```

**Consequences:**
- Parser hangs indefinitely consuming CPU
- StackOverflowError crashes the application
- Memory exhaustion from recursive parsing
- No components render because CSS never finishes parsing

**Prevention:**
- Implement cycle detection with visited set during resolution
- Limit maximum resolution depth (CSS spec suggests 10-20 levels)
- Track resolution chain and error on revisit
```java
Set<String> resolving = new HashSet<>();
String resolve(String varName) {
    if (!resolving.add(varName)) {
        throw new CSSException("Circular reference: " + varName);
    }
    try {
        // resolve variable
    } finally {
        resolving.remove(varName);
    }
}
```

**Detection:**
- Unit tests with circular CSS definitions
- Timeout on CSS parsing operations (>1 second is suspicious)
- Stack depth monitoring during variable resolution

**Phase impact:** Address in Phase 1 (CSS Parser) - parser architecture determines if cycles are possible.

---

### Pitfall 3: UIDefaults Race Conditions on Multi-Monitor Systems
**What goes wrong:** UIDefaults modifications aren't atomic. On multi-monitor setups (different DPI per screen), components read partially-updated UIDefaults during L&F initialization, causing mixed styling or crashes.

**Why it happens:**
- Window dragged between monitors with different scaling factors
- L&F updates UIDefaults in batches but components query mid-update
- Multiple threads read UIDefaults while CSS engine writes
- UIDefaults inheritance from parent L&F classes isn't copied atomically

**Consequences:**
- Components render with wrong DPI scaling (text too large, icons too small)
- ClassCastException when component expects Color but gets derived ColorUIResource mid-update
- Buttons inherit defaults from Metal while panels inherit from custom L&F
- Platform-specific crashes (macOS Retina vs Windows 125% scaling)

**Prevention:**
```java
// BAD - Components can read mid-update
for (String key : cssStyles.keySet()) {
    UIManager.put(key, cssStyles.get(key));
}

// GOOD - Atomic batch update
UIDefaults defaults = UIManager.getDefaults();
synchronized(defaults) {
    defaults.putAll(buildCompleteUIDefaultsMap());
    SwingUtilities.updateComponentTreeUI(rootFrame);
}
```

**Detection:**
- Test on multi-monitor with different DPI scaling
- Log UIDefaults access during L&F updates
- ClassCastException in component constructors
- Visual inconsistencies when dragging windows between screens

**Phase impact:** Must address in Phase 2 (Component Integration) - after parser works but before production use.

---

### Pitfall 4: Color Space Mismatches (CSS sRGB vs AWT Default RGB)
**What goes wrong:** CSS colors are defined in sRGB color space, but `java.awt.Color` uses device RGB by default. Perceived colors don't match web references, particularly for grays and saturated colors.

**Why it happens:**
- CSS: `color: rgb(128, 128, 128)` assumes sRGB
- Java: `new Color(128, 128, 128)` uses default color space (often not sRGB)
- Modern displays use wider gamuts (Display P3, Adobe RGB)
- Color conversion happens implicitly with wrong assumptions

**Consequences:**
- Grays appear tinted (blue/green cast) compared to web version
- Brand colors look "off" when comparing browser to Swing app
- Gradients have visible banding or unexpected hue shifts
- Transparent colors composite incorrectly

**Prevention:**
```java
// BAD - Assumes device color space matches CSS
Color fromCSS(int r, int g, int b) {
    return new Color(r, g, b);
}

// GOOD - Explicit sRGB color space
Color fromCSS(int r, int g, int b) {
    ColorSpace sRGB = ColorSpace.getInstance(ColorSpace.CS_sRGB);
    float[] components = new float[] {r/255f, g/255f, b/255f};
    return new Color(sRGB, components, 1.0f);
}
```

**Detection:**
- Side-by-side visual comparison with browser rendering same CSS
- Color picker shows different RGB values than CSS definition
- Screenshots show color drift when moving between web and Swing

**Phase impact:** Address in Phase 1 (CSS Parser) - color parsing must produce correct color space from start.

---

### Pitfall 5: Font Metrics Divergence Between Web and Java2D
**What goes wrong:** CSS `font-family` fallback chains don't map to Java font names, and text metrics differ even when "same" font is used. Causes layout breakage where text overflows or wraps differently than web.

**Why it happens:**
- CSS: `font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif`
- Java font names: "Dialog", "SansSerif" don't match system font stack
- Font hinting/antialiasing differences between browsers and Java2D
- Subpixel positioning in web vs integer positioning in Swing
- Line height calculations differ (CSS `line-height` vs Java2D font metrics)

**Consequences:**
- Button labels truncate with "..." when they fit in browser
- Multi-line text has different line breaks
- Vertical alignment off by several pixels
- Component minimum sizes don't match web component dimensions

**Prevention:**
- Map CSS generic families to platform-specific fonts explicitly:
```java
Map<String, String> fontMapping = Map.of(
    "-apple-system", "SF Pro Text",  // macOS
    "Segoe UI", "Segoe UI",          // Windows
    "sans-serif", System.getProperty("os.name").contains("Mac")
                  ? ".AppleSystemUIFont" : "SansSerif"
);
```
- Add fudge factors for text metrics (10-15% buffer for width calculations)
- Use `TextLayout` for accurate metrics instead of `FontMetrics`
- Test on all target platforms (font availability varies)

**Detection:**
- Unit tests comparing rendered text width between web and Swing
- Visual regression tests showing text overflow
- Layout tests with known text strings measuring bounds

**Phase impact:** Address in Phase 2 (Component Integration) - text rendering affects all components.

---

### Pitfall 6: Shadow/Elevation Rendering Performance Collapse
**What goes wrong:** Material Design elevation/CSS `box-shadow` naive implementations cause exponential performance degradation. Painting 100 shadowed components drops from 60fps to <5fps.

**Why it happens:**
- Shadow blur implemented as real-time Gaussian blur on every paint
- Multi-layer shadows (elevation-8 = 3-4 shadow layers) compound the cost
- Swing clips shadows outside component bounds, causing overdraw
- No caching of shadow bitmaps when properties unchanged

**Consequences:**
- UI becomes unusably laggy with >20 shadowed components
- Scrolling stutters badly
- Mouse cursor lags behind actual position
- Entire app feels "janky"

**Prevention:**
```java
// BAD - Real-time blur every paint
void paint(Graphics2D g2) {
    BufferedImage shadow = createShadow(width, height, blur);
    applyGaussianBlur(shadow, blurRadius); // SLOW!
    g2.drawImage(shadow, x, y, null);
}

// GOOD - Pre-rendered shadow cache
class ShadowCache {
    Map<ShadowKey, BufferedImage> cache = new LRUCache<>(100);

    BufferedImage getShadow(int elevation, int width, int height) {
        ShadowKey key = new ShadowKey(elevation, width, height);
        return cache.computeIfAbsent(key, k -> {
            return preRenderShadow(elevation, width, height);
        });
    }
}
```

**Detection:**
- Profiler shows >50% time in paint methods
- Frame rate drops below 30fps with moderate component count
- CPU spikes when scrolling
- YourKit/JProfiler shows blur methods as hotspot

**Phase impact:** Must optimize by Phase 3 (Performance) - acceptable to be slow in Phase 2 prototypes.

---

### Pitfall 7: State Management Across Component Hierarchy
**What goes wrong:** Hover/focus/pressed states don't propagate correctly through nested components. Parent component shows hover state but child button doesn't respond, or vice versa.

**Why it happens:**
- Swing event propagation is complex (mouse entered vs mouse moved)
- CSS `:hover` means "mouse over element or descendant"
- Swing components don't have built-in `:has()` selector equivalent
- Z-order and glass pane interfere with mouse event delivery
- Disabled components consume events without visual feedback

**Consequences:**
- Hover effects flicker or don't appear
- Focus rings appear on wrong component
- Buttons inside panels don't show pressed state
- Inconsistent with web version where hover works predictably

**Prevention:**
- Implement state manager tracking global mouse position:
```java
class ComponentStateManager {
    Map<JComponent, ComponentState> states = new WeakHashMap<>();

    void updateHoverState(MouseEvent e) {
        Component deepest = e.getComponent()
            .getComponentAt(e.getPoint());

        // Update all ancestors to "has-hover-descendant"
        for (Component c = deepest; c != null; c = c.getParent()) {
            if (c instanceof JComponent) {
                updateState((JComponent)c, State.HOVER_DESCENDANT);
            }
        }
    }
}
```
- Use AWTEventListener for global mouse tracking
- Test with nested interactive components (buttons in panels in dialogs)

**Detection:**
- Manual testing of hover states in nested layouts
- Automated tests simulating mouse movement over component trees
- Visual comparison with web version

**Phase impact:** Address in Phase 2 (Component Integration) - state management is per-component.

---

### Pitfall 8: BasicLookAndFeel Assumptions Breaking Subclass Behavior
**What goes wrong:** Overriding only `ComponentUI.paint()` isn't enough. BasicLookAndFeel has hidden assumptions in `installUI()`, `uninstallUI()`, and `createUI()` that break when not followed exactly.

**Why it happens:**
- BasicLookAndFeel installs default listeners that conflict with custom L&F
- UIDefaults keys have undocumented dependencies (e.g., "Button.margin" affects "Button.contentMargins")
- Some components check L&F class name and enable special behavior
- Border installation order matters but isn't documented

**Consequences:**
- Components lose keyboard navigation
- Focus traversal breaks
- Mnemonics stop working
- Layout managers calculate wrong preferred sizes
- Some components render completely blank

**Prevention:**
- Always call `super.installUI()` even if overriding everything
- Copy BasicLookAndFeel's `installDefaults()` pattern exactly:
```java
@Override
public void installUI(JComponent c) {
    super.installUI(c);
    installDefaults((JButton)c);
    installListeners((JButton)c);
    installKeyboardActions((JButton)c);
}
```
- Test each component type in isolation
- Compare UIDefaults with BasicLookAndFeel defaults

**Detection:**
- Keyboard navigation stops working
- Components don't respond to space/enter keys
- Tab focus skips components randomly
- Component throws exception during installation

**Phase impact:** Address in Phase 2 (Component Integration) - manifests when adding each component type.

## Moderate Pitfalls

These cause bugs or workarounds but don't require architectural changes.

### Pitfall 9: CSS `calc()` Expression Edge Cases
**What goes wrong:** CSS `calc()` expressions with mixed units or whitespace sensitivity break parser.

**Prevention:**
- Test with: `calc(100% - 20px)`, `calc(var(--size) * 2)`, `calc(1em + 5px)`
- CSS spec requires whitespace around `+` and `-` operators
- Maintain unit through calculation (can't multiply `px * px`)

---

### Pitfall 10: Transparent Colors with Insets/Borders
**What goes wrong:** Component insets/borders with semi-transparent colors composite wrong when component is opaque.

**Prevention:**
- Set component opaque=false when using alpha in backgrounds/borders
- Use `AlphaComposite.SrcOver` explicitly
- Clear background before painting transparent elements

---

### Pitfall 11: UIDefaults Key Naming Inconsistencies
**What goes wrong:** UIDefaults keys vary between Swing versions and components don't recognize custom L&F properties.

**Why it happens:**
- Java 8: "Button.background"
- Java 11+: "Button.background" and "Button[Enabled].background"
- Third-party components use different conventions

**Prevention:**
- Register keys for all known variants
- Test on minimum and maximum supported Java versions
- Log warnings for unrecognized CSS properties

---

### Pitfall 12: CSS Custom Property Fallback Chain Complexity
**What goes wrong:** CSS fallback syntax `var(--primary, var(--default, blue))` requires recursive parsing that's easy to get wrong.

**Prevention:**
- Parse fallbacks lazily (only when primary var undefined)
- Unit test with 5+ levels of fallback nesting
- Handle fallback values that are themselves expressions

---

### Pitfall 13: HiDPI Image Scaling Artifacts
**What goes wrong:** Icons and images scale poorly on high-DPI displays, appearing blurry or pixelated.

**Prevention:**
- Use `MultiResolutionImage` for icons (provide 1x, 2x, 3x variants)
- Enable `RenderingHints.VALUE_INTERPOLATION_BICUBIC`
- Test on actual HiDPI displays (not just scaled VMs)

---

### Pitfall 14: CSS Selector Specificity Calculation Errors
**What goes wrong:** CSS specificity rules are complex and errors cause wrong styles to apply.

**Prevention:**
- Implement specificity as 3-tuple: (id, class, element)
- Test with specificity conflicts: `#id .class` vs `.class.class`
- Match browser behavior exactly (not "close enough")

## Minor Pitfalls

Small issues with straightforward fixes.

### Pitfall 15: Forgetting to Dispose of Graphics Contexts
**What goes wrong:** Creating `Graphics2D` copies without disposing causes memory leaks.

**Prevention:**
```java
Graphics2D g2 = (Graphics2D) g.create();
try {
    // paint
} finally {
    g2.dispose();
}
```

---

### Pitfall 16: CSS Color Function Format Variations
**What goes wrong:** CSS supports `rgb(255, 0, 0)`, `rgb(255 0 0)`, `rgba(255, 0, 0, 0.5)`, and `rgb(255 0 0 / 0.5)`.

**Prevention:**
- Support all syntax variants in parser
- Test with actual browser-generated CSS output

---

### Pitfall 17: Line Separator Differences in CSS Files
**What goes wrong:** CSS files with `\r\n` (Windows) vs `\n` (Unix) vs `\r` (old Mac) break naive parsers.

**Prevention:**
- Normalize line endings during file read
- Use universal line separator regex `\r\n|\r|\n`

---

### Pitfall 18: Component Repaint Optimization Breaking Change Detection
**What goes wrong:** Swing optimizes repaints by checking if properties actually changed. CSS updates might not trigger repaint if value is "equal" but styled differently.

**Prevention:**
- Fire property change events even when value equals previous
- Use `RepaintManager.markCompletelyDirty()` for L&F updates
- Force full repaint after CSS reload

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|---------------|------------|
| Phase 1: CSS Parser | Variable resolution cycles (#2) | Implement cycle detection from start |
| Phase 1: CSS Parser | Color space mismatches (#4) | Use sRGB ColorSpace explicitly |
| Phase 1: Architecture | EDT violations (#1) | Design with thread safety first, not later |
| Phase 2: Component Integration | Font metrics divergence (#5) | Budget extra time for text layout testing |
| Phase 2: Component Integration | State management (#7) | Build global state tracker early |
| Phase 2: Component Integration | BasicLookAndFeel quirks (#8) | Test each component type thoroughly |
| Phase 3: Performance | Shadow rendering (#6) | Profile early, cache aggressively |
| Phase 3: Multi-Monitor | UIDefaults races (#3) | Test on actual multi-DPI setup |
| Phase 4: Edge Cases | CSS calc() expressions (#9) | Unit tests with complex calc() cases |

## Testing Recommendations

To catch these pitfalls early:

1. **Thread Safety Tests:** Run with `-Xcheck:edt` and `CheckThreadViolationRepaintManager`
2. **CSS Edge Cases:** Maintain corpus of pathological CSS (circular vars, deep nesting, mixed units)
3. **Multi-Monitor:** Automated tests moving windows between virtual displays with different DPI
4. **Visual Regression:** Screenshot comparison between browser and Swing for each component
5. **Performance Benchmarks:** Paint 1000 components, measure fps drop threshold
6. **Platform Coverage:** Test on Windows 10/11, macOS 12+, Linux with different DEs

## Confidence Notes

**MEDIUM confidence overall** - based on training data knowledge of Swing and CSS specifications. Web research tools were unavailable, so could not verify:
- Current best practices (2025-2026) for Swing L&F development
- Recent JDK changes affecting UIDefaults or painting
- Modern CSS parser library comparisons
- Community post-mortems from similar projects

**Recommend verification:**
- Official Java documentation for UIDefaults thread safety guarantees
- CSS Color Module Level 4 specification for color space handling
- Swing painting architecture documentation for EDT requirements
- Performance benchmarking real shadow rendering approaches

## Sources

Based on training data (January 2025 knowledge cutoff):
- Java Swing documentation (Oracle)
- CSS Specifications (W3C)
- Java2D rendering documentation
- Swing Look & Feel design patterns

**Note:** Unable to verify with current sources due to tool restrictions. Treat as starting point requiring validation against official documentation and real-world testing.
