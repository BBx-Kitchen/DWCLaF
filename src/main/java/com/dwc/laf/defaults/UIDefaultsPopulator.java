package com.dwc.laf.defaults;

import com.dwc.laf.css.CssTokenMap;
import com.dwc.laf.css.CssValue;
import com.dwc.laf.css.CssValue.ColorValue;
import com.dwc.laf.css.CssValue.DimensionValue;
import com.dwc.laf.css.CssValue.FloatValue;
import com.dwc.laf.css.CssValue.IntegerValue;
import com.dwc.laf.css.CssValue.RawValue;
import com.dwc.laf.css.CssValue.StringValue;

import javax.swing.UIDefaults;
import javax.swing.plaf.ColorUIResource;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Populates a {@link UIDefaults} table from a {@link CssTokenMap} using
 * a {@link TokenMappingConfig} to drive the conversion.
 *
 * <p>For each mapping entry, the corresponding CSS token is looked up in the
 * token map. If found, its value is converted to the appropriate Java type
 * (respecting UIResource contracts) and placed in UIDefaults under each
 * target key.</p>
 *
 * <p>Color values are wrapped in {@link ColorUIResource} per the Swing
 * UIResource contract -- without this, theme switching and UI delegation
 * break because Swing uses UIResource checks to decide whether a value
 * was set by the L&amp;F or by application code.</p>
 */
public final class UIDefaultsPopulator {

    private static final Logger LOG = Logger.getLogger(UIDefaultsPopulator.class.getName());

    /**
     * Default base font size in pixels for rem/em to pixel conversion.
     * Matches the standard browser default (16px = 1rem).
     */
    private static final int DEFAULT_BASE_FONT_SIZE_PX = 16;

    private UIDefaultsPopulator() {
        // utility class
    }

    /**
     * Populates the given UIDefaults table from CSS tokens using the mapping config.
     *
     * <p>For each entry in the mapping, the CSS token is looked up. If present,
     * each mapping target receives the converted value. Missing tokens and
     * unconvertible values are skipped with FINE-level log messages.</p>
     *
     * @param table   the UIDefaults table to populate
     * @param tokens  the CSS token map (from CssThemeLoader)
     * @param mapping the token mapping configuration
     */
    public static void populate(UIDefaults table, CssTokenMap tokens, TokenMappingConfig mapping) {
        for (MappingEntry entry : mapping.entries()) {
            Optional<CssValue> value = tokens.get(entry.cssTokenName());
            if (value.isEmpty()) {
                LOG.fine(() -> "CSS token not found, skipping: " + entry.cssTokenName());
                continue;
            }

            CssValue cssValue = value.get();
            for (MappingTarget target : entry.targets()) {
                Object converted = convertValue(cssValue, target.type());
                if (converted != null) {
                    table.put(target.key(), converted);
                } else {
                    LOG.fine(() -> "Could not convert " + cssValue.getClass().getSimpleName()
                            + " to " + target.type() + " for key: " + target.key());
                }
            }
        }
    }

    /**
     * Converts a CssValue to a Java object based on the target MappingType.
     *
     * @param value the CSS value to convert
     * @param type  the desired target type
     * @return the converted Java object, or null if conversion is not possible
     */
    private static Object convertValue(CssValue value, MappingType type) {
        return switch (type) {
            case COLOR -> convertColor(value);
            case INT -> convertInt(value);
            case FLOAT -> convertFloat(value);
            case STRING -> convertString(value);
            case INSETS -> convertInsets(value);
            case AUTO -> convertAuto(value);
        };
    }

    private static Object convertColor(CssValue value) {
        if (value instanceof ColorValue cv) {
            return new ColorUIResource(cv.color());
        }
        return null;
    }

    private static Object convertInt(CssValue value) {
        if (value instanceof IntegerValue iv) {
            return iv.value();
        }
        if (value instanceof DimensionValue dv) {
            return dimensionToPixels(dv, DEFAULT_BASE_FONT_SIZE_PX);
        }
        if (value instanceof FloatValue fv) {
            return Math.round(fv.value());
        }
        return null;
    }

    private static Object convertFloat(CssValue value) {
        if (value instanceof FloatValue fv) {
            return fv.value();
        }
        if (value instanceof IntegerValue iv) {
            return (float) iv.value();
        }
        return null;
    }

    private static Object convertString(CssValue value) {
        if (value instanceof StringValue sv) {
            return sv.value();
        }
        if (value instanceof RawValue rv) {
            return rv.raw();
        }
        return null;
    }

    private static Object convertInsets(CssValue value) {
        LOG.fine("Insets mapping not yet implemented");
        return null;
    }

    private static Object convertAuto(CssValue value) {
        return switch (value) {
            case ColorValue cv -> new ColorUIResource(cv.color());
            case IntegerValue iv -> iv.value();
            case FloatValue fv -> fv.value();
            case DimensionValue dv -> dimensionToPixels(dv, DEFAULT_BASE_FONT_SIZE_PX);
            case StringValue sv -> sv.value();
            case RawValue rv -> {
                LOG.fine(() -> "Skipping RawValue in AUTO mode: " + rv.raw());
                yield null;
            }
        };
    }

    /**
     * Converts a CSS DimensionValue to pixels.
     *
     * @param dim             the dimension value
     * @param baseFontSizePx  the base font size in pixels (for rem/em conversion)
     * @return the pixel value as an integer
     */
    private static int dimensionToPixels(DimensionValue dim, int baseFontSizePx) {
        return switch (dim.unit()) {
            case "px" -> dim.intValue();
            case "rem", "em" -> Math.round(dim.value() * baseFontSizePx);
            default -> dim.intValue();
        };
    }
}
