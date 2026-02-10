package com.dwc.laf;

import com.dwc.laf.css.CssThemeLoader;
import com.dwc.laf.css.CssTokenMap;
import com.dwc.laf.defaults.TokenMappingConfig;
import com.dwc.laf.defaults.UIDefaultsPopulator;

import javax.swing.UIDefaults;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicLookAndFeel;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
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
        // Custom ComponentUI delegates registered in Phases 4-7
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
