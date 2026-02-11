package com.dwc.laf;

import com.dwc.laf.css.CssThemeLoader;
import com.dwc.laf.css.CssTokenMap;
import com.dwc.laf.css.CssValue;
import com.dwc.laf.defaults.TokenMappingConfig;
import com.dwc.laf.defaults.UIDefaultsPopulator;
import com.dwc.laf.ui.DwcButtonBorder;
import com.dwc.laf.ui.DwcCheckBoxIcon;
import com.dwc.laf.ui.DwcRadioButtonIcon;
import com.dwc.laf.ui.DwcTextFieldBorder;
import com.dwc.laf.ui.DwcToolTipBorder;
import com.dwc.laf.ui.DwcTreeExpandIcon;
import com.dwc.laf.ui.DwcTreeNodeIcon;

import javax.swing.UIDefaults;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.basic.BasicLookAndFeel;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A Swing Look and Feel derived from DWC CSS design tokens.
 *
 * <p>This is the main entry point for the DWC Look and Feel. Activate it with:
 * <pre>
 *   UIManager.setLookAndFeel(new DwcLookAndFeel());
 * </pre>
 *
 * <p>On activation, the L&amp;F loads the bundled default CSS theme (or an
 * external override specified via the {@code dwc.theme} system property),
 * parses CSS custom properties into typed values, and populates Swing
 * {@link UIDefaults} through a properties-driven mapping layer.</p>
 *
 * <p>Custom {@code ComponentUI} delegates for specific components (buttons,
 * text fields, etc.) are registered in later phases. This class provides
 * the foundation: correct colors, dimensions, and fonts from CSS tokens.</p>
 */
public class DwcLookAndFeel extends BasicLookAndFeel {

    private static final Logger LOG = Logger.getLogger(DwcLookAndFeel.class.getName());

    /**
     * The loaded CSS token map, available for downstream custom ComponentUI
     * delegates that need direct token access for painting.
     */
    private CssTokenMap tokenMap;

    // ---- BasicLookAndFeel abstract method overrides ----

    @Override
    public String getName() {
        return "DWC";
    }

    @Override
    public String getID() {
        return "DwcLaf";
    }

    @Override
    public String getDescription() {
        return "A Swing Look and Feel derived from DWC CSS design tokens";
    }

    @Override
    public boolean isNativeLookAndFeel() {
        return false;
    }

    @Override
    public boolean isSupportedLookAndFeel() {
        return true;
    }

    // ---- Initialization overrides ----

    @Override
    protected void initClassDefaults(UIDefaults table) {
        super.initClassDefaults(table);
        table.put("ButtonUI", "com.dwc.laf.ui.DwcButtonUI");
        table.put("TextFieldUI", "com.dwc.laf.ui.DwcTextFieldUI");
        table.put("CheckBoxUI", "com.dwc.laf.ui.DwcCheckBoxUI");
        table.put("RadioButtonUI", "com.dwc.laf.ui.DwcRadioButtonUI");
        table.put("ComboBoxUI", "com.dwc.laf.ui.DwcComboBoxUI");
        table.put("LabelUI", "com.dwc.laf.ui.DwcLabelUI");
        table.put("PanelUI", "com.dwc.laf.ui.DwcPanelUI");
        table.put("TabbedPaneUI", "com.dwc.laf.ui.DwcTabbedPaneUI");
        table.put("ToolTipUI", "com.dwc.laf.ui.DwcToolTipUI");
        table.put("ProgressBarUI", "com.dwc.laf.ui.DwcProgressBarUI");
        table.put("ScrollBarUI", "com.dwc.laf.ui.DwcScrollBarUI");
        table.put("TreeUI", "com.dwc.laf.ui.DwcTreeUI");
        table.put("TableUI", "com.dwc.laf.ui.DwcTableUI");
    }

    @Override
    protected void initComponentDefaults(UIDefaults table) {
        super.initComponentDefaults(table);

        // 1. Load CSS tokens from bundled theme (+ optional override)
        tokenMap = CssThemeLoader.load();

        // 2. Load token-to-UIDefaults mapping configuration
        TokenMappingConfig mapping = TokenMappingConfig.loadDefault();

        // 3. Populate UIDefaults from CSS tokens via mapping
        UIDefaultsPopulator.populate(table, tokenMap, mapping);

        // 4. Set up default font from mapped font properties
        initDefaultFont(table);

        // 5. Set up button-specific UIDefaults (focus ring color, margin, etc.)
        initButtonDefaults(table);

        // 6. Set up text field-specific UIDefaults (margin, border)
        initTextFieldDefaults(table);

        // 7. Set up checkbox-specific UIDefaults (icon, margin, rollover)
        initCheckBoxDefaults(table);

        // 8. Set up radio button-specific UIDefaults (icon, margin, rollover)
        initRadioButtonDefaults(table);

        // 9. Set up combobox-specific UIDefaults (border, padding)
        initComboBoxDefaults(table);

        // 10. Set up label-specific UIDefaults
        initLabelDefaults(table);

        // 11. Set up panel-specific UIDefaults (card mode shadow, arc)
        initPanelDefaults(table);

        // 12. Set up tabbed pane-specific UIDefaults (tab insets, underline height)
        initTabbedPaneDefaults(table);

        // 13. Set up tooltip-specific UIDefaults (border)
        initToolTipDefaults(table);

        // 14. Set up progress bar-specific UIDefaults (border, arc)
        initProgressBarDefaults(table);

        // 15. Set up scrollbar-specific UIDefaults (thumb arc, width)
        initScrollBarDefaults(table);

        // 16. Set up tree-specific UIDefaults (expand/collapse icons, row height)
        initTreeDefaults(table);

        // 17. Set up table-specific UIDefaults (alternate row color, borders)
        initTableDefaults(table);
    }

    // ---- Public API ----

    /**
     * Returns the loaded CSS token map.
     *
     * <p>Useful for custom {@code ComponentUI} delegates in future phases
     * that need direct token access for painting beyond what UIDefaults
     * provides.</p>
     *
     * @return the CSS token map, or null if the L&amp;F has not been initialized
     */
    public CssTokenMap getTokenMap() {
        return tokenMap;
    }

    // ---- Font resolution ----

    /**
     * Initializes the default font from mapped CSS token values.
     *
     * <p>Checks for {@code defaultFont.family}, {@code defaultFont.size}, and
     * {@code defaultFont.style} keys in UIDefaults (populated from CSS tokens
     * by the mapping layer). If the family is present, resolves it from a CSS
     * font stack to a platform-available font family name.</p>
     */
    private void initDefaultFont(UIDefaults table) {
        Object familyObj = table.get("defaultFont.family");
        Object sizeObj = table.get("defaultFont.size");

        if (familyObj == null && sizeObj == null) {
            LOG.fine("No defaultFont properties found in UIDefaults; skipping font setup");
            return;
        }

        // Resolve font family from CSS font stack
        String family = "SansSerif"; // fallback
        if (familyObj instanceof String fontStack) {
            family = resolveFontFamily(fontStack);
        }

        // Resolve font size (DimensionValue was already converted to int by populator)
        int size = 14; // fallback
        if (sizeObj instanceof Integer intSize) {
            size = intSize;
        } else if (sizeObj instanceof Number numSize) {
            size = numSize.intValue();
        }

        // Resolve font style
        int style = Font.PLAIN;
        Object styleObj = table.get("defaultFont.style");
        if (styleObj instanceof Integer intStyle) {
            // CSS font-weight: 400=PLAIN, 700=BOLD. Map >=600 to BOLD.
            style = intStyle >= 600 ? Font.BOLD : Font.PLAIN;
        }

        FontUIResource fontResource = new FontUIResource(family, style, size);
        table.put("defaultFont", fontResource);

        // Apply to common component font keys
        String[] componentFontKeys = {
            "Button.font", "Label.font", "TextField.font", "TextArea.font",
            "ComboBox.font", "CheckBox.font", "RadioButton.font",
            "TabbedPane.font", "List.font", "Table.font", "ToolTip.font",
            "MenuBar.font", "Menu.font", "MenuItem.font"
        };
        for (String key : componentFontKeys) {
            table.put(key, fontResource);
        }

        LOG.fine(() -> "Set default font: " + fontResource.getFamily()
                + " " + fontResource.getSize() + "pt style=" + fontResource.getStyle());
    }

    // ---- Button defaults ----

    /**
     * Initializes button-specific UIDefaults entries that require computation
     * beyond simple token mapping.
     *
     * <p>Computes the focus ring color from CSS HSL tokens
     * ({@code --dwc-color-primary-h}, {@code --dwc-color-primary-s},
     * {@code --dwc-focus-ring-l}, {@code --dwc-focus-ring-a}) and stores it
     * as {@code Component.focusRingColor} for all components to share.</p>
     *
     * <p>Also installs button margin, minimum width, icon-text gap, and
     * rollover flag in UIDefaults.</p>
     */
    private void initButtonDefaults(UIDefaults table) {
        // 1. Focus ring color from CSS HSL tokens
        initFocusRingColor(table);

        // 1b. Per-variant focus ring colors (success, danger, warning, info)
        initVariantFocusRingColors(table);

        // 2. Button margin
        table.put("Button.margin", new InsetsUIResource(2, 14, 2, 14));

        // 3. Button minimum width
        table.put("Button.minimumWidth", 72);

        // 4. Button icon-text gap
        table.put("Button.iconTextGap", 4);

        // 5. Rollover enabled
        table.put("Button.rollover", Boolean.TRUE);

        // 6. Button border (standard L&F pattern for LookAndFeel.installBorder)
        table.put("Button.border", new BorderUIResource(new DwcButtonBorder()));

        // 7. Button font weight: DWC uses font-weight 500 (semibold).
        // Java Font has no semi-bold; use BOLD as nearest equivalent.
        FontUIResource defaultFont = (FontUIResource) table.get("defaultFont");
        if (defaultFont != null) {
            FontUIResource buttonFont = new FontUIResource(
                defaultFont.getFamily(), Font.BOLD, defaultFont.getSize());
            table.put("Button.font", buttonFont);
        }

        LOG.fine("Initialized button defaults (margin, minimumWidth, iconTextGap, rollover, border, font)");
    }

    // ---- TextField defaults ----

    /**
     * Initializes text field-specific UIDefaults entries.
     *
     * <p>Installs the text field margin and the custom {@link DwcTextFieldBorder}
     * that provides focus-width-aware insets and state-aware outline painting.</p>
     */
    private void initTextFieldDefaults(UIDefaults table) {
        // 1. TextField margin
        table.put("TextField.margin", new InsetsUIResource(2, 6, 2, 6));

        // 2. TextField border (standard L&F pattern for LookAndFeel.installBorder)
        table.put("TextField.border", new BorderUIResource(new DwcTextFieldBorder()));

        LOG.fine("Initialized text field defaults (margin, border)");
    }

    // ---- CheckBox defaults ----

    /**
     * Initializes checkbox-specific UIDefaults entries.
     *
     * <p>Installs the custom {@link DwcCheckBoxIcon} as the default icon for
     * all checkboxes, sets margins, and enables rollover for hover tracking.</p>
     */
    private void initCheckBoxDefaults(UIDefaults table) {
        table.put("CheckBox.icon", new DwcCheckBoxIcon());
        table.put("CheckBox.margin", new InsetsUIResource(2, 4, 2, 4));
        table.put("CheckBox.rollover", Boolean.TRUE);

        // Set CheckBox.background to match the panel background so that if the
        // component is opaque (BBj ties opaque→contentAreaFilled), the filled
        // background doesn't show as a grey rectangle.
        Color panelBg = table.getColor("Panel.background");
        if (panelBg != null) {
            table.put("CheckBox.background", new ColorUIResource(panelBg));
        }

        LOG.fine("Initialized checkbox defaults (icon, margin, rollover, background)");
    }

    // ---- RadioButton defaults ----

    /**
     * Initializes radio button-specific UIDefaults entries.
     *
     * <p>Installs the custom {@link DwcRadioButtonIcon} as the default icon for
     * all radio buttons, sets margins, and enables rollover for hover tracking.</p>
     */
    private void initRadioButtonDefaults(UIDefaults table) {
        table.put("RadioButton.icon", new DwcRadioButtonIcon());
        table.put("RadioButton.margin", new InsetsUIResource(2, 4, 2, 4));
        table.put("RadioButton.rollover", Boolean.TRUE);

        // Same background fix as CheckBox — prevent grey rectangle in BBj
        Color panelBg = table.getColor("Panel.background");
        if (panelBg != null) {
            table.put("RadioButton.background", new ColorUIResource(panelBg));
        }

        LOG.fine("Initialized radio button defaults (icon, margin, rollover, background)");
    }

    // ---- ComboBox defaults ----

    /**
     * Initializes combobox-specific UIDefaults entries.
     *
     * <p>Reuses the {@link DwcTextFieldBorder} for the combobox border (same
     * {@code --dwc-input-*} token styling as text fields) and sets padding.</p>
     */
    private void initComboBoxDefaults(UIDefaults table) {
        table.put("ComboBox.border", new BorderUIResource(new DwcTextFieldBorder()));
        table.put("ComboBox.padding", new InsetsUIResource(2, 6, 2, 6));

        LOG.fine("Initialized combobox defaults (border, padding)");
    }

    // ---- Label defaults ----

    /**
     * Initializes label-specific UIDefaults entries.
     *
     * <p>Label colors and fonts are populated via the token mapping pipeline.
     * No additional computed defaults are needed.</p>
     */
    private void initLabelDefaults(UIDefaults table) {
        LOG.fine("Initialized label defaults (token-mapped only)");
    }

    // ---- Panel defaults ----

    /**
     * Initializes panel-specific UIDefaults entries for card-mode rendering.
     *
     * <p>Sets up shadow parameters (color, blur radius, offset) and inherits
     * {@code Panel.arc} from {@code Component.arc} if not already set by
     * the token mapping pipeline.</p>
     */
    private void initPanelDefaults(UIDefaults table) {
        // Inherit Panel.arc from Component.arc if not already set from token mapping
        if (table.get("Panel.arc") == null || table.getInt("Panel.arc") <= 0) {
            table.put("Panel.arc", table.getInt("Component.arc"));
        }

        // Semi-transparent black shadow (CSS box-shadow: 0 2px 6px rgba(0,0,0,0.15) approximation)
        table.put("Panel.shadowColor", new ColorUIResource(new Color(0, 0, 0, 40)));
        table.put("Panel.shadowBlurRadius", 6);
        table.put("Panel.shadowOffsetY", 2);

        LOG.fine("Initialized panel defaults (arc, shadowColor, shadowBlurRadius, shadowOffsetY)");
    }

    // ---- TabbedPane defaults ----

    /**
     * Initializes tabbed pane-specific UIDefaults entries.
     *
     * <p>Sets tab insets for padding, zero tab area and content border insets
     * for clean layout, and the underline indicator height.</p>
     */
    private void initTabbedPaneDefaults(UIDefaults table) {
        table.put("TabbedPane.tabInsets", new InsetsUIResource(8, 16, 8, 16));
        table.put("TabbedPane.tabAreaInsets", new InsetsUIResource(0, 0, 0, 0));
        table.put("TabbedPane.contentBorderInsets", new InsetsUIResource(0, 0, 0, 0));
        table.put("TabbedPane.underlineHeight", 3);

        LOG.fine("Initialized tabbed pane defaults (tabInsets, tabAreaInsets, contentBorderInsets, underlineHeight)");
    }

    // ---- ToolTip defaults ----

    /**
     * Initializes tooltip-specific UIDefaults entries.
     *
     * <p>Installs the custom {@link DwcToolTipBorder} that provides shadow
     * rendering and rounded outline painting.</p>
     */
    private void initToolTipDefaults(UIDefaults table) {
        table.put("ToolTip.border", new BorderUIResource(new DwcToolTipBorder()));

        LOG.fine("Initialized tooltip defaults (border)");
    }

    // ---- ProgressBar defaults ----

    /**
     * Initializes progress bar-specific UIDefaults entries.
     *
     * <p>Installs an empty border (no visible border) and inherits
     * {@code ProgressBar.arc} from {@code Component.arc} if not already set.</p>
     */
    private void initProgressBarDefaults(UIDefaults table) {
        table.put("ProgressBar.border",
                new BorderUIResource(javax.swing.BorderFactory.createEmptyBorder()));

        // Use pill-shaped fallback (999 -> clamps to min(width, height) in createRoundedShape)
        // if the token-mapped --dwc-border-radius-xl value is missing or zero.
        if (table.get("ProgressBar.arc") == null || table.getInt("ProgressBar.arc") <= 0) {
            table.put("ProgressBar.arc", 999);
        }

        LOG.fine("Initialized progress bar defaults (border, arc=" + table.getInt("ProgressBar.arc") + ")");
    }

    // ---- Tree defaults ----

    /**
     * Initializes tree-specific UIDefaults entries.
     *
     * <p>Installs custom {@link DwcTreeExpandIcon} chevron icons for expand/collapse,
     * sets 24px row height for comfortable spacing, enables root handles, and
     * disables connecting lines for a modern look.</p>
     */
    private void initTreeDefaults(UIDefaults table) {
        table.put("Tree.expandedIcon", new DwcTreeExpandIcon(true));
        table.put("Tree.collapsedIcon", new DwcTreeExpandIcon(false));
        table.put("Tree.openIcon", new DwcTreeNodeIcon(DwcTreeNodeIcon.Type.FOLDER_OPEN));
        table.put("Tree.closedIcon", new DwcTreeNodeIcon(DwcTreeNodeIcon.Type.FOLDER_CLOSED));
        table.put("Tree.leafIcon", new DwcTreeNodeIcon(DwcTreeNodeIcon.Type.FILE));
        table.put("Tree.rowHeight", 24);
        table.put("Tree.showsRootHandles", Boolean.TRUE);
        table.put("Tree.paintLines", Boolean.FALSE);

        // DefaultTreeCellRenderer uses these for non-selected node backgrounds.
        // Without setting them, it falls back to grey "control" color, creating
        // visible grey rectangles behind node text.
        Color treeBg = table.getColor("Tree.background");
        if (treeBg != null) {
            table.put("Tree.textBackground", treeBg);
        }

        // Set selection colors for DefaultTreeCellRenderer's own selection painting.
        // Our DwcTreeUI.paintRow() paints full-width selection behind the renderer,
        // but the renderer also paints selection — make them match.
        Color selBg = table.getColor("Tree.selectionBackground");
        Color selFg = table.getColor("Tree.selectionForeground");
        if (selBg != null) {
            table.put("Tree.textBackground", treeBg);
            table.put("Tree.selectionBackground", selBg);
        }
        if (selFg != null) {
            table.put("Tree.selectionForeground", selFg);
        }

        LOG.fine("Initialized tree defaults (expandedIcon, collapsedIcon, openIcon, closedIcon, leafIcon, rowHeight, showsRootHandles, paintLines, textBackground)");
    }

    // ---- ScrollBar defaults ----

    /**
     * Initializes scrollbar-specific UIDefaults entries.
     *
     * <p>Sets the thumb corner arc and scrollbar width for the thin modern look.</p>
     */
    private void initScrollBarDefaults(UIDefaults table) {
        table.put("ScrollBar.thumbArc", 8);
        table.put("ScrollBar.width", 10);

        LOG.fine("Initialized scrollbar defaults (thumbArc, width)");
    }

    // ---- Table defaults ----

    /**
     * Initializes table-specific UIDefaults entries.
     *
     * <p>Ensures the alternate row color is set (inherits from Panel.background
     * if not already populated by the token mapping pipeline), removes the
     * scroll pane border, and sets the focus cell highlight border to match
     * the cell renderer padding.</p>
     */
    private void initTableDefaults(UIDefaults table) {
        // Inherit alternate row color from Panel.background if not already set
        if (table.getColor("Table.alternateRowColor") == null) {
            table.put("Table.alternateRowColor", table.getColor("Panel.background"));
        }

        // No scroll pane border
        table.put("Table.scrollPaneBorder",
                new BorderUIResource(javax.swing.BorderFactory.createEmptyBorder()));

        // Match renderer border for focus cell highlight
        table.put("Table.focusCellHighlightBorder",
                javax.swing.BorderFactory.createEmptyBorder(2, 6, 2, 6));

        LOG.fine("Initialized table defaults (alternateRowColor, scrollPaneBorder, focusCellHighlightBorder)");
    }

    /**
     * Computes the focus ring color from CSS HSL tokens and stores it in UIDefaults.
     *
     * <p>The DWC focus ring color is {@code hsla(primary-h, primary-s, 45%, 0.4)}.
     * This method reads the individual HSL components from the token map, converts
     * to RGB, and stores the result as {@code Component.focusRingColor}.</p>
     */
    private void initFocusRingColor(UIDefaults table) {
        if (tokenMap == null) {
            LOG.warning("Token map not available; skipping focus ring color computation");
            return;
        }

        // Read hue: --dwc-color-primary-h (IntegerValue, e.g. 211)
        OptionalInt hueOpt = tokenMap.getInt("--dwc-color-primary-h");
        if (hueOpt.isEmpty()) {
            LOG.warning("Missing --dwc-color-primary-h token; skipping focus ring color");
            return;
        }
        float hue = hueOpt.getAsInt();

        // Read saturation: --dwc-color-primary-s (DimensionValue with %, e.g. 100%)
        float saturation = getDimensionPercent("--dwc-color-primary-s", -1f);
        if (saturation < 0) {
            LOG.warning("Missing --dwc-color-primary-s token; skipping focus ring color");
            return;
        }

        // Read lightness: --dwc-focus-ring-l (DimensionValue with %, e.g. 45%)
        float lightness = getDimensionPercent("--dwc-focus-ring-l", -1f);
        if (lightness < 0) {
            LOG.warning("Missing --dwc-focus-ring-l token; skipping focus ring color");
            return;
        }

        // Read alpha: --dwc-focus-ring-a (FloatValue, e.g. 0.4)
        Optional<Float> alphaOpt = tokenMap.getFloat("--dwc-focus-ring-a");
        if (alphaOpt.isEmpty()) {
            LOG.warning("Missing --dwc-focus-ring-a token; skipping focus ring color");
            return;
        }
        float alpha = alphaOpt.get();

        Color focusRingColor = hslToColor(hue, saturation, lightness, alpha);
        table.put("Component.focusRingColor", new ColorUIResource(focusRingColor));
        table.put("Component.focusRingColor.primary", new ColorUIResource(focusRingColor));

        LOG.fine(() -> "Computed focus ring color: hsla(" + hue + ", " + saturation
                + "%, " + lightness + "%, " + alpha + ") -> " + focusRingColor);
    }

    /**
     * Computes per-variant focus ring colors from CSS HSL tokens and stores them
     * in UIDefaults.
     *
     * <p>Each variant (success, danger, warning, info) has its own hue and saturation
     * tokens ({@code --dwc-color-{variant}-h} and {@code --dwc-color-{variant}-s}).
     * The lightness and alpha are shared with the primary focus ring
     * ({@code --dwc-focus-ring-l} and {@code --dwc-focus-ring-a}).</p>
     *
     * <p>Results are stored as {@code Component.focusRingColor.{variant}} in UIDefaults.</p>
     */
    private void initVariantFocusRingColors(UIDefaults table) {
        if (tokenMap == null) {
            return;
        }

        // Shared lightness and alpha (same as primary focus ring)
        float lightness = getDimensionPercent("--dwc-focus-ring-l", -1f);
        if (lightness < 0) {
            return;
        }
        Optional<Float> alphaOpt = tokenMap.getFloat("--dwc-focus-ring-a");
        if (alphaOpt.isEmpty()) {
            return;
        }
        float alpha = alphaOpt.get();

        String[] variants = {"success", "danger", "warning", "info"};
        for (String variant : variants) {
            OptionalInt hueOpt = tokenMap.getInt("--dwc-color-" + variant + "-h");
            if (hueOpt.isEmpty()) {
                LOG.fine(() -> "Missing --dwc-color-" + variant + "-h token; skipping variant focus ring");
                continue;
            }
            float hue = hueOpt.getAsInt();

            float saturation = getDimensionPercent("--dwc-color-" + variant + "-s", -1f);
            if (saturation < 0) {
                LOG.fine(() -> "Missing --dwc-color-" + variant + "-s token; skipping variant focus ring");
                continue;
            }

            Color variantFocusRing = hslToColor(hue, saturation, lightness, alpha);
            table.put("Component.focusRingColor." + variant, new ColorUIResource(variantFocusRing));

            LOG.fine(() -> "Computed " + variant + " focus ring color: hsla(" + hue + ", "
                    + saturation + "%, " + lightness + "%, " + alpha + ") -> " + variantFocusRing);
        }
    }

    /**
     * Reads a DimensionValue with "%" unit from the token map and returns the
     * numeric part as a float (0-100 scale).
     *
     * @param propertyName the CSS custom property name
     * @param defaultValue the value to return if the token is missing or not a dimension
     * @return the percentage value (0-100), or defaultValue
     */
    private float getDimensionPercent(String propertyName, float defaultValue) {
        Optional<CssValue> valueOpt = tokenMap.get(propertyName);
        if (valueOpt.isPresent() && valueOpt.get() instanceof CssValue.DimensionValue dv) {
            return dv.value();
        }
        return defaultValue;
    }

    /**
     * Converts HSL color components to a Java {@link Color}.
     *
     * <p>Uses the same HSL-to-RGB algorithm as the CSS color spec and
     * {@code CssColorParser}. The hue is in degrees (0-360), saturation
     * and lightness are percentages (0-100), alpha is 0.0-1.0.</p>
     *
     * @param hue        the hue in degrees (0-360)
     * @param saturation the saturation as a percentage (0-100)
     * @param lightness  the lightness as a percentage (0-100)
     * @param alpha      the alpha (0.0 = transparent, 1.0 = opaque)
     * @return the computed Color
     */
    private static Color hslToColor(float hue, float saturation, float lightness, float alpha) {
        float h = hue / 360f;
        float s = saturation / 100f;
        float l = lightness / 100f;

        float q = l < 0.5f ? l * (1 + s) : l + s - l * s;
        float p = 2 * l - q;

        float r = hueToRgb(p, q, h + 1f / 3);
        float g = hueToRgb(p, q, h);
        float b = hueToRgb(p, q, h - 1f / 3);

        int ri = Math.round(Math.max(0, Math.min(1, r)) * 255);
        int gi = Math.round(Math.max(0, Math.min(1, g)) * 255);
        int bi = Math.round(Math.max(0, Math.min(1, b)) * 255);
        int ai = Math.round(Math.max(0, Math.min(1, alpha)) * 255);

        return new Color(ri, gi, bi, ai);
    }

    /**
     * Helper for HSL-to-RGB conversion. Converts a single color channel.
     */
    private static float hueToRgb(float p, float q, float t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1f / 6) return p + (q - p) * 6 * t;
        if (t < 1f / 2) return q;
        if (t < 2f / 3) return p + (q - p) * (2f / 3 - t) * 6;
        return p;
    }

    /**
     * Resolves a CSS font-family stack to a platform-available Java font family name.
     *
     * <p>Splits the CSS font stack on commas, trims quotes from each candidate,
     * maps CSS generic names and platform-specific aliases to Java logical names,
     * then checks {@link GraphicsEnvironment} for the first available match.</p>
     *
     * @param cssFontStack the CSS font-family value (e.g., "-apple-system, BlinkMacSystemFont, 'Roboto', sans-serif")
     * @return the resolved Java font family name, or "SansSerif" as ultimate fallback
     */
    private String resolveFontFamily(String cssFontStack) {
        // CSS generic name -> Java logical font name
        // Java logical fonts are guaranteed available: Dialog, DialogInput, Monospaced, Serif, SansSerif
        String[] candidates = cssFontStack.split(",");

        // Get available system fonts (cached by JRE)
        Set<String> availableFonts;
        try {
            availableFonts = Set.of(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()
            );
        } catch (Exception e) {
            // Headless or other environment issue -- use fallback
            LOG.fine("Could not query available fonts: " + e.getMessage());
            return "SansSerif";
        }

        for (String candidate : candidates) {
            String name = candidate.strip();
            // Remove surrounding quotes (single or double)
            if ((name.startsWith("'") && name.endsWith("'"))
                    || (name.startsWith("\"") && name.endsWith("\""))) {
                name = name.substring(1, name.length() - 1);
            }

            if (name.isEmpty()) {
                continue;
            }

            // Map CSS generic names to Java logical font names
            String mapped = mapCssGenericToJava(name);
            if (mapped != null) {
                return mapped;
            }

            // Map platform-specific aliases
            String platformMapped = mapPlatformAlias(name);
            if (platformMapped != null) {
                // Check if the platform-mapped name is available
                if (availableFonts.contains(platformMapped)) {
                    return platformMapped;
                }
                // Fall through to check original name
            }

            // Check if this exact font family is available on the system
            if (availableFonts.contains(name)) {
                return name;
            }
        }

        // Ultimate fallback
        return "SansSerif";
    }

    /**
     * Maps CSS generic font family names to Java logical font names.
     *
     * @return the Java logical font name, or null if not a CSS generic name
     */
    private static String mapCssGenericToJava(String name) {
        return switch (name.toLowerCase()) {
            case "sans-serif" -> "SansSerif";
            case "serif" -> "Serif";
            case "monospace" -> "Monospaced";
            case "cursive" -> "SansSerif";
            case "fantasy" -> "SansSerif";
            case "system-ui" -> "SansSerif";
            case "ui-monospace" -> "Monospaced";
            case "ui-serif" -> "Serif";
            case "ui-sans-serif" -> "SansSerif";
            default -> null;
        };
    }

    /**
     * Maps platform-specific font aliases to common font family names.
     *
     * @return the mapped font family name, or null if not a known alias
     */
    private static String mapPlatformAlias(String name) {
        return switch (name) {
            case "-apple-system", "BlinkMacSystemFont" -> ".AppleSystemUIFont";
            case "Segoe UI" -> "Segoe UI";
            default -> null;
        };
    }
}
