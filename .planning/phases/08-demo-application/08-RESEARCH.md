# Phase 8: Demo Application - Research

**Researched:** 2026-02-10
**Domain:** Java Swing demo application layout, component gallery patterns, single-class runnable showcase
**Confidence:** HIGH

## Summary

This phase builds a polished demo application that showcases all 8 DWC-themed Swing components in a scrollable gallery, demonstrating every visual state (normal, hover, pressed, focused, disabled). The entire codebase is already complete: all 8 UI delegates are implemented and registered in `DwcLookAndFeel.initClassDefaults()`, the token-mapping pipeline populates UIDefaults from CSS, and a prototype `DwcQuickDemo.java` already exists at `src/main/java/com/dwc/laf/DwcQuickDemo.java` showing 6 of the 8 components (missing JLabel section heading styling and JPanel card-mode, plus missing JTabbedPane entirely). The demo phase replaces this prototype with a comprehensive gallery.

The demo must be a single runnable main class with zero framework dependencies beyond the existing project classes. Swing's standard layout managers (BoxLayout for vertical stacking, FlowLayout for horizontal rows, GridLayout for state grids) are sufficient. No external libraries are needed. The key challenge is organizing 8 components with 4-5 states each into a clean, scannable layout that makes visual comparison easy. The existing `DwcQuickDemo` pattern of section-label + component-row + vertical-strut is sound and should be extended.

The success criteria require "side-by-side" comparison capability (DEMO-03 criterion 4: "User can visually compare Swing components to DWC web equivalents side-by-side"). The goal.md suggests an optional JavaFX WebView approach as a stretch goal, but embedding JavaFX would violate the "no framework dependencies" requirement. Instead, the demo should present components in a clear gallery layout with state labels, and the user performs manual visual comparison against the DWC web component documentation or a browser. An alternative approach is to display labeled state columns so the user can screenshot and compare.

**Primary recommendation:** Replace the existing `DwcQuickDemo.java` with a comprehensive `DwcComponentGallery.java` that organizes all 8 components into labeled sections, each showing the component in all its states (normal, hover, pressed, focused, disabled), using BoxLayout vertical stacking with section headers, state labels, and a JScrollPane wrapping the entire content. Add `exec-maven-plugin` configuration to pom.xml for one-command execution: `mvn compile exec:java`.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `javax.swing.*` | JDK 21 | All UI components, layout managers, UIManager | The entire project's target framework |
| `java.awt.*` | JDK 21 | Colors, Fonts, FlowLayout, BorderLayout, GridLayout, Dimension | Standard AWT layout and rendering |
| `com.dwc.laf.DwcLookAndFeel` | Phase 2 | L&F activation via `UIManager.setLookAndFeel()` | Already complete, activates all 8 delegates |
| `javax.swing.BoxLayout` | JDK 21 | Vertical stacking of component sections | Natural fit for scrollable gallery |
| `javax.swing.JScrollPane` | JDK 21 | Scrollable content viewport | Required for gallery with many components |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `javax.swing.BorderFactory` | JDK 21 | Section spacing, titled borders | Section separation and padding |
| `javax.swing.Box` | JDK 21 | Rigid areas and vertical/horizontal struts | Spacing between component rows |
| `javax.swing.SwingUtilities` | JDK 21 | EDT invocation, safe L&F installation | Required for thread-safe Swing startup |
| `org.codehaus.mojo:exec-maven-plugin` | 3.6.3 | Run demo via `mvn exec:java` | One-command demo execution |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| BoxLayout vertical stack | GridBagLayout | GridBagLayout is more flexible but overkill for a simple vertical gallery; BoxLayout is simpler and sufficient |
| FlowLayout for state rows | GridLayout for state columns | FlowLayout wraps naturally when window is narrow; GridLayout forces fixed columns but ensures alignment -- **use GridLayout for state grids** |
| Single monolithic main class | Separate panel builder classes | Requirement DEMO-03 mandates single main class. Helper methods within the class are fine. |
| exec-maven-plugin in pom.xml | `java -cp target/classes com.dwc.laf.DwcComponentGallery` | Plugin is more convenient for `mvn exec:java`; both approaches work |

## Architecture Patterns

### Recommended File Structure
```
src/main/java/com/dwc/laf/
  DwcComponentGallery.java    # NEW: comprehensive demo (replaces DwcQuickDemo)
  DwcQuickDemo.java           # KEEP: existing quick demo (for reference)
```

### Pattern 1: Section-Per-Component Gallery Layout
**What:** Each component type gets a titled section with a grid of states
**When to use:** Displaying multiple components with multiple states in a scannable format

```java
// Pattern: create a section for each component type
private static JPanel createButtonSection() {
    JPanel section = new JPanel();
    section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
    section.setBorder(BorderFactory.createTitledBorder("JButton"));
    section.setAlignmentX(Component.LEFT_ALIGNMENT);

    // State labels row
    JPanel labelRow = new JPanel(new GridLayout(1, 5, 8, 0));
    labelRow.add(new JLabel("Normal"));
    labelRow.add(new JLabel("Hover*"));
    labelRow.add(new JLabel("Focused"));
    labelRow.add(new JLabel("Pressed*"));
    labelRow.add(new JLabel("Disabled"));
    section.add(labelRow);

    // Default variant row
    JPanel defaultRow = new JPanel(new GridLayout(1, 5, 8, 0));
    JButton normal = new JButton("Default");
    JButton focused = new JButton("Focused");
    focused.requestFocusInWindow(); // will gain focus when shown
    JButton disabled = new JButton("Disabled");
    disabled.setEnabled(false);
    // ... add all to row
    section.add(defaultRow);

    return section;
}
```

### Pattern 2: Card Panel Demonstration
**What:** Show JPanel in both normal mode and card mode side-by-side
**When to use:** Demonstrating the `dwc.panelStyle=card` client property

```java
// Card panel with content
JPanel card = new JPanel(new BorderLayout());
card.putClientProperty("dwc.panelStyle", "card");
card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
card.add(new JLabel("Card Panel"), BorderLayout.CENTER);
```

### Pattern 3: TabbedPane State Demonstration
**What:** Show JTabbedPane with normal, disabled, and selected tab states
**When to use:** Demonstrating tab underline indicator and hover states

```java
JTabbedPane tabs = new JTabbedPane();
tabs.addTab("Active", new JLabel(" Active tab content "));
tabs.addTab("Normal", new JLabel(" Normal tab content "));
tabs.addTab("Disabled", new JLabel(" Disabled tab content "));
tabs.setEnabledAt(2, false);
```

### Pattern 4: Primary Button Variant Activation
**What:** Use client property to activate primary button styling
**When to use:** Showing button variants in the demo

```java
JButton primaryBtn = new JButton("Primary");
primaryBtn.putClientProperty("dwc.buttonType", "primary");
```

### Anti-Patterns to Avoid
- **Shared button group across sections:** Each RadioButton section needs its own ButtonGroup. Don't share ButtonGroups across demo sections.
- **Missing setAlignmentX on sections:** BoxLayout requires consistent alignment. All sections must call `setAlignmentX(Component.LEFT_ALIGNMENT)` or the layout breaks.
- **Opaque panels with card mode:** Card-mode panels set opaque=false automatically, but parent containers should not force opaque painting.
- **Hardcoded colors in demo:** Never use `new Color(...)` in the demo. All colors come from the L&F. The demo should not set any foreground/background colors directly -- the point is to show the L&F doing it.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Vertical scrollable layout | Custom scroll logic | `JScrollPane` wrapping a `BoxLayout.Y_AXIS` panel | JScrollPane handles all scroll behavior |
| Section headers with underline | Custom painting | `BorderFactory.createTitledBorder()` or styled JLabel with separator | Built-in Swing mechanism |
| Focus management for demo | Custom focus traversal | Default Swing focus traversal + tab key | Works automatically |
| Window centering | Manual coordinate math | `frame.setLocationRelativeTo(null)` | Standard Swing pattern |
| Maven execution | Shell script | `exec-maven-plugin` configuration | Cross-platform, Maven-integrated |

**Key insight:** The demo application is pure Swing composition -- no custom painting, no custom layout managers, no custom event handling. Everything is standard Swing API calls with the DWC L&F installed.

## Common Pitfalls

### Pitfall 1: BoxLayout MaximumSize Issue
**What goes wrong:** Components in BoxLayout stretch to fill available width because BoxLayout uses getMaximumSize(). Some components (JTextField, JComboBox) have MAX_VALUE maximumSize and stretch enormously.
**Why it happens:** BoxLayout distributes extra space based on maximumSize, not preferredSize.
**How to avoid:** Set maximumSize on components that would otherwise stretch: `textField.setMaximumSize(new Dimension(200, textField.getPreferredSize().height))`. Or better: wrap component rows in `FlowLayout` or `GridLayout` panels, which constrain sizing naturally.
**Warning signs:** Components stretching across the full window width when they should be compact.

### Pitfall 2: ScrollPane Viewport Background
**What goes wrong:** The scroll pane viewport shows a different background color than the content panel.
**Why it happens:** JScrollPane's viewport has its own background. When the content panel doesn't fill the viewport, the viewport background shows through.
**How to avoid:** Set the viewport's background to match: `scrollPane.getViewport().setBackground(mainPanel.getBackground())`. Or set the main content panel to fill the viewport: `scrollPane.getViewport().setView(main)`.
**Warning signs:** White strips or color mismatch at the edges of the scroll area.

### Pitfall 3: Hover/Pressed States Not Demonstrable in Static Layout
**What goes wrong:** Hover and pressed states cannot be permanently shown -- they require mouse interaction. The demo can show normal, focused, and disabled statically, but hover/pressed require the user to interact.
**Why it happens:** Swing button model hover/pressed states are transient and driven by mouse events.
**How to avoid:** Add a note or label explaining that hover/pressed states are interactive: "Hover and press buttons to see state changes." The static layout shows normal/focused/disabled. The user demonstrates hover/pressed by mousing over components.
**Warning signs:** Trying to force hover state programmatically -- this requires hacking the button model and is fragile.

### Pitfall 4: EDT Thread Safety
**What goes wrong:** L&F installation or component creation happens off the EDT, causing intermittent rendering glitches.
**Why it happens:** The `main()` method runs on the main thread, not the EDT.
**How to avoid:** Wrap all Swing code in `SwingUtilities.invokeLater()`, including `UIManager.setLookAndFeel()` and all component creation. The existing `DwcQuickDemo` already does this correctly.
**Warning signs:** Intermittent visual artifacts, especially on first display.

### Pitfall 5: JTabbedPane Minimum Height
**What goes wrong:** TabbedPane content area collapses to zero height when content panels have no preferred size.
**Why it happens:** Empty content panels have zero preferred size.
**How to avoid:** Add padding or content to tab content panels: `contentPanel.setPreferredSize(new Dimension(400, 60))` or add visible content.
**Warning signs:** Tabs visible but content area invisible or extremely thin.

### Pitfall 6: Card Panel Needs Extra Insets for Shadow
**What goes wrong:** Card panel's shadow is clipped by its parent layout.
**Why it happens:** The DwcPanelUI card-mode shadow extends outside the component bounds. If the parent has tight insets, the shadow gets clipped.
**How to avoid:** Add EmptyBorder around card panels or ensure the parent has sufficient padding: `cardWrapper.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8))`.
**Warning signs:** Shadow appears cut off on one or more sides.

## Code Examples

### Complete Demo Main Class Pattern
```java
// Source: analysis of existing DwcQuickDemo.java + project architecture
public class DwcComponentGallery {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new DwcLookAndFeel());
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            JFrame frame = new JFrame("DWC Component Gallery");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(900, 800);

            JPanel main = new JPanel();
            main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
            main.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

            // Title
            JLabel title = new JLabel("DWC Look & Feel - Component Gallery");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            main.add(title);
            main.add(Box.createVerticalStrut(8));

            JLabel subtitle = new JLabel(
                "All 8 themed components. Hover and press to see interactive states.");
            subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            main.add(subtitle);
            main.add(Box.createVerticalStrut(24));

            // Add component sections
            main.add(createButtonSection());
            main.add(Box.createVerticalStrut(24));
            main.add(createTextFieldSection());
            main.add(Box.createVerticalStrut(24));
            main.add(createCheckBoxSection());
            // ... etc for all 8 components

            JScrollPane scrollPane = new JScrollPane(main);
            scrollPane.setBorder(null);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            frame.setContentPane(scrollPane);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
```

### Scroll Speed Fix
```java
// JScrollPane default scroll speed is 1px per click, very slow
scrollPane.getVerticalScrollBar().setUnitIncrement(16);
```

### All Client Properties Used by DWC L&F
```java
// Button primary variant
button.putClientProperty("dwc.buttonType", "primary");

// Panel card mode
panel.putClientProperty("dwc.panelStyle", "card");

// TextField placeholder text
textField.putClientProperty("JTextField.placeholderText", "Enter text...");
```

### Component Sections -- All 8
```java
// 1. JButton -- Default + Primary variants, disabled
// 2. JTextField -- Normal with placeholder, filled, disabled
// 3. JCheckBox -- Unchecked, checked, disabled unchecked, disabled checked
// 4. JRadioButton -- Unselected, selected, disabled (in ButtonGroup)
// 5. JComboBox -- Normal with items, disabled
// 6. JLabel -- Normal text, disabled
// 7. JPanel -- Normal panel vs card-mode panel
// 8. JTabbedPane -- Multiple tabs, one disabled
```

### Maven exec-maven-plugin Configuration
```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.6.3</version>
    <configuration>
        <mainClass>com.dwc.laf.DwcComponentGallery</mainClass>
    </configuration>
</plugin>
```

### Running the Demo
```bash
# Option A: Maven exec plugin
mvn compile exec:java

# Option B: Direct java command
mvn compile
java -cp target/classes com.dwc.laf.DwcComponentGallery
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Separate Demo + ComponentGallery classes (goal.md suggestion) | Single `DwcComponentGallery` main class | Phase 8 requirements | DEMO-03 mandates single runnable class |
| JavaFX WebView for side-by-side (goal.md stretch goal) | Manual visual comparison via gallery layout | Phase 8 constraints | No framework dependencies allowed |
| `DwcQuickDemo` prototype (6 components) | `DwcComponentGallery` comprehensive (8 components, all states) | Phase 8 | Covers all requirements |

**Deprecated/outdated:**
- `DwcQuickDemo.java` -- exists as a quick prototype from development. The comprehensive gallery supersedes it but does not need to delete it.

## Existing Code Analysis

### What DwcQuickDemo Already Has (to preserve/extend)
1. Correct EDT pattern: `SwingUtilities.invokeLater()`
2. L&F activation: `UIManager.setLookAndFeel(new DwcLookAndFeel())`
3. BoxLayout vertical stacking with struts
4. Button section: Default + Primary + Disabled
5. TextField section: Placeholder + Filled + Disabled
6. CheckBox section: Unchecked + Checked + Disabled + Disabled-Checked
7. RadioButton section: ButtonGroup with options + Disabled
8. ComboBox section: Normal + Disabled

### What DwcQuickDemo Is Missing (must add)
1. **JLabel section** -- currently used as section headers but not showcased as a component with its own disabled/styled states
2. **JPanel section** -- card-mode panels not demonstrated
3. **JTabbedPane section** -- not present at all
4. **State completeness** -- no focused-state demonstration
5. **Section titles** -- currently just JLabels, no visual separation
6. **Scroll increment** -- default is 1px (very slow scrolling)
7. **Window sizing** -- 520x520 is too small for 8 components
8. **Instructional text** -- no explanation of hover/press interactivity

### Component Client Properties Summary
| Component | Client Property | Values | Purpose |
|-----------|----------------|--------|---------|
| JButton | `dwc.buttonType` | `"primary"` | Activates primary accent-colored variant |
| JPanel | `dwc.panelStyle` | `"card"` | Activates rounded corners + shadow |
| JTextField | `JTextField.placeholderText` | any string | Shows placeholder when empty |

## Open Questions

1. **Should DwcQuickDemo.java be deleted or kept?**
   - What we know: It exists as a quick prototype showing 6 of 8 components
   - What's unclear: Whether keeping both demo files causes confusion
   - Recommendation: Keep both. `DwcQuickDemo` is a minimal sanity check; `DwcComponentGallery` is the comprehensive showcase. They serve different purposes.

2. **How to demonstrate hover/pressed states in a static gallery?**
   - What we know: Hover and pressed states are transient -- they require mouse interaction
   - What's unclear: Whether the requirements expect these to be shown statically
   - Recommendation: Include instructional text ("Hover and press components to see state changes"). The gallery shows normal, focused (via requestFocusInWindow on one component per section), and disabled statically. Hover/pressed are demonstrated by the user interacting.

3. **Side-by-side comparison (DEMO-03 criterion 4)**
   - What we know: The requirement says "User can visually compare Swing components to DWC web equivalents side-by-side"
   - What's unclear: Whether "side-by-side" means literal split-screen or just organized for easy comparison
   - Recommendation: Organize the gallery with clear section labels and state labels so the user can open a browser with DWC web components and compare visually. No JavaFX WebView needed (would violate zero-framework-dependency requirement).

## Sources

### Primary (HIGH confidence)
- Codebase analysis of all 8 UI delegate classes (DwcButtonUI, DwcTextFieldUI, DwcCheckBoxUI, DwcRadioButtonUI, DwcComboBoxUI, DwcLabelUI, DwcPanelUI, DwcTabbedPaneUI)
- Existing `DwcQuickDemo.java` -- current prototype demo
- `DwcLookAndFeel.java` -- L&F activation and class defaults registration
- `pom.xml` -- build configuration (Java 21, zero runtime deps)
- `goal.md` -- project architecture and requirements
- `ROADMAP.md` -- phase 8 requirements and success criteria
- `token-mapping.properties` -- all CSS-to-UIDefaults mappings

### Secondary (MEDIUM confidence)
- [Oracle Swing Layout Guide](https://docs.oracle.com/javase/tutorial/uiswing/layout/visual.html) -- layout manager patterns
- [Oracle BoxLayout Guide](https://docs.oracle.com/javase/tutorial/uiswing/layout/box.html) -- BoxLayout usage
- [Maven exec-maven-plugin](https://www.mojohaus.org/exec-maven-plugin/usage.html) -- plugin configuration and version 3.6.3
- [Baeldung Maven exec](https://www.baeldung.com/maven-java-main-method) -- running Java main class from Maven

### Tertiary (LOW confidence)
- None. All findings are from codebase analysis and official documentation.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - all components already built and tested; only Swing standard APIs needed
- Architecture: HIGH - extending the established DwcQuickDemo pattern with additional sections
- Pitfalls: HIGH - based on direct codebase analysis of component behaviors and Swing layout experience

**Research date:** 2026-02-10
**Valid until:** 2026-03-10 (stable -- Swing API does not change)
