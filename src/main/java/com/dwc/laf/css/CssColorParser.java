package com.dwc.laf.css;

import java.awt.Color;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Parses CSS color strings into {@link java.awt.Color} instances.
 *
 * <p>Supports all CSS color formats: hsl/hsla, hex (3/4/6/8 digit),
 * rgb/rgba, and named colors. Both comma-separated and modern
 * space-separated syntax are supported.</p>
 *
 * <p>Returns {@link Optional#empty()} for unrecognized input. Never throws.</p>
 */
public final class CssColorParser {

    private static final Logger LOG = Logger.getLogger(CssColorParser.class.getName());

    private CssColorParser() {
        // utility class
    }

    /**
     * Parse a CSS color string into a {@link Color}.
     *
     * <p>Tries each format in order: hex, hsl/hsla, rgb/rgba, named color.
     * Returns the first successful parse, or empty if no format matches.</p>
     *
     * @param value the CSS color string (e.g., "hsl(211, 100%, 50%)", "#ff0000", "red")
     * @return an Optional containing the parsed Color, or empty if not a valid color
     */
    public static Optional<Color> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        String trimmed = value.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);

        // Try hex first (starts with #)
        if (lower.startsWith("#")) {
            return parseHex(trimmed.substring(1));
        }

        // Try hsl/hsla
        if (lower.startsWith("hsl")) {
            return parseHsl(lower);
        }

        // Try rgb/rgba
        if (lower.startsWith("rgb")) {
            return parseRgb(lower);
        }

        // Fall through to named colors
        return NamedCssColors.resolve(lower);
    }

    // ---- Hex parsing ----

    private static Optional<Color> parseHex(String hex) {
        int len = hex.length();
        try {
            return switch (len) {
                case 3 -> {
                    // #RGB -> #RRGGBB
                    int r = Integer.parseInt(hex.substring(0, 1), 16);
                    int g = Integer.parseInt(hex.substring(1, 2), 16);
                    int b = Integer.parseInt(hex.substring(2, 3), 16);
                    yield Optional.of(new Color(r * 17, g * 17, b * 17));
                }
                case 4 -> {
                    // #RGBA -> #RRGGBBAA
                    int r = Integer.parseInt(hex.substring(0, 1), 16);
                    int g = Integer.parseInt(hex.substring(1, 2), 16);
                    int b = Integer.parseInt(hex.substring(2, 3), 16);
                    int a = Integer.parseInt(hex.substring(3, 4), 16);
                    yield Optional.of(new Color(r * 17, g * 17, b * 17, a * 17));
                }
                case 6 -> {
                    int r = Integer.parseInt(hex.substring(0, 2), 16);
                    int g = Integer.parseInt(hex.substring(2, 4), 16);
                    int b = Integer.parseInt(hex.substring(4, 6), 16);
                    yield Optional.of(new Color(r, g, b));
                }
                case 8 -> {
                    int r = Integer.parseInt(hex.substring(0, 2), 16);
                    int g = Integer.parseInt(hex.substring(2, 4), 16);
                    int b = Integer.parseInt(hex.substring(4, 6), 16);
                    int a = Integer.parseInt(hex.substring(6, 8), 16);
                    yield Optional.of(new Color(r, g, b, a));
                }
                default -> {
                    LOG.warning("Invalid hex color length: #" + hex);
                    yield Optional.empty();
                }
            };
        } catch (NumberFormatException e) {
            LOG.warning("Invalid hex color: #" + hex);
            return Optional.empty();
        }
    }

    // ---- HSL/HSLA parsing ----

    private static Optional<Color> parseHsl(String lower) {
        // Extract content between parentheses
        int openParen = lower.indexOf('(');
        int closeParen = lower.lastIndexOf(')');
        if (openParen < 0 || closeParen < 0 || closeParen <= openParen) {
            LOG.warning("Malformed hsl() value: " + lower);
            return Optional.empty();
        }

        String content = lower.substring(openParen + 1, closeParen).trim();

        // Parse components: either comma-separated or space-separated with optional / alpha
        float h, s, l;
        float alpha = 1.0f;

        try {
            if (content.contains(",")) {
                // Comma-separated: hsl(H, S%, L%) or hsla(H, S%, L%, A)
                String[] parts = content.split(",");
                if (parts.length < 3 || parts.length > 4) {
                    return Optional.empty();
                }
                h = Float.parseFloat(parts[0].trim());
                s = parsePercent(parts[1].trim());
                l = parsePercent(parts[2].trim());
                if (parts.length == 4) {
                    alpha = parseAlpha(parts[3].trim());
                }
            } else {
                // Space-separated: hsl(H S% L%) or hsl(H S% L% / A)
                String mainPart;
                if (content.contains("/")) {
                    int slashIdx = content.indexOf('/');
                    mainPart = content.substring(0, slashIdx).trim();
                    alpha = parseAlpha(content.substring(slashIdx + 1).trim());
                } else {
                    mainPart = content;
                }
                String[] parts = mainPart.split("\\s+");
                if (parts.length != 3) {
                    return Optional.empty();
                }
                h = Float.parseFloat(parts[0].trim());
                s = parsePercent(parts[1].trim());
                l = parsePercent(parts[2].trim());
            }
        } catch (NumberFormatException e) {
            LOG.warning("Malformed hsl() components: " + lower);
            return Optional.empty();
        }

        // Normalize hue to 0-360
        h = ((h % 360) + 360) % 360;
        // s and l are already 0-1 from parsePercent

        Color color = hslToRgb(h, s, l, alpha);
        return Optional.of(color);
    }

    /**
     * Convert HSL to RGB. This is NOT Java's built-in HSB -- it's the CSS HSL algorithm.
     */
    private static Color hslToRgb(float h, float s, float l, float alpha) {
        float r, g, b;

        if (s == 0) {
            // Achromatic
            r = g = b = l;
        } else {
            float q = l < 0.5f ? l * (1 + s) : l + s - l * s;
            float p = 2 * l - q;
            float hNorm = h / 360.0f;
            r = hueToRgb(p, q, hNorm + 1.0f / 3.0f);
            g = hueToRgb(p, q, hNorm);
            b = hueToRgb(p, q, hNorm - 1.0f / 3.0f);
        }

        int ri = clamp(Math.round(r * 255));
        int gi = clamp(Math.round(g * 255));
        int bi = clamp(Math.round(b * 255));
        int ai = clamp(Math.round(alpha * 255));

        return new Color(ri, gi, bi, ai);
    }

    private static float hueToRgb(float p, float q, float t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1.0f / 6.0f) return p + (q - p) * 6 * t;
        if (t < 1.0f / 2.0f) return q;
        if (t < 2.0f / 3.0f) return p + (q - p) * (2.0f / 3.0f - t) * 6;
        return p;
    }

    // ---- RGB/RGBA parsing ----

    private static Optional<Color> parseRgb(String lower) {
        int openParen = lower.indexOf('(');
        int closeParen = lower.lastIndexOf(')');
        if (openParen < 0 || closeParen < 0 || closeParen <= openParen) {
            LOG.warning("Malformed rgb() value: " + lower);
            return Optional.empty();
        }

        String content = lower.substring(openParen + 1, closeParen).trim();

        int r, g, b;
        float alpha = 1.0f;

        try {
            if (content.contains(",")) {
                // Comma-separated: rgb(R, G, B) or rgba(R, G, B, A)
                String[] parts = content.split(",");
                if (parts.length < 3 || parts.length > 4) {
                    return Optional.empty();
                }
                r = clamp(Math.round(Float.parseFloat(parts[0].trim())));
                g = clamp(Math.round(Float.parseFloat(parts[1].trim())));
                b = clamp(Math.round(Float.parseFloat(parts[2].trim())));
                if (parts.length == 4) {
                    alpha = parseAlpha(parts[3].trim());
                }
            } else {
                // Space-separated: rgb(R G B) or rgb(R G B / A)
                String mainPart;
                if (content.contains("/")) {
                    int slashIdx = content.indexOf('/');
                    mainPart = content.substring(0, slashIdx).trim();
                    alpha = parseAlpha(content.substring(slashIdx + 1).trim());
                } else {
                    mainPart = content;
                }
                String[] parts = mainPart.split("\\s+");
                if (parts.length != 3) {
                    return Optional.empty();
                }
                r = clamp(Math.round(Float.parseFloat(parts[0].trim())));
                g = clamp(Math.round(Float.parseFloat(parts[1].trim())));
                b = clamp(Math.round(Float.parseFloat(parts[2].trim())));
            }
        } catch (NumberFormatException e) {
            LOG.warning("Malformed rgb() components: " + lower);
            return Optional.empty();
        }

        int ai = clamp(Math.round(alpha * 255));
        return Optional.of(new Color(r, g, b, ai));
    }

    // ---- Utility methods ----

    /**
     * Parse a percentage value like "100%" to 0.0-1.0 range.
     */
    private static float parsePercent(String s) {
        if (s.endsWith("%")) {
            return Float.parseFloat(s.substring(0, s.length() - 1)) / 100.0f;
        }
        return Float.parseFloat(s);
    }

    /**
     * Parse an alpha value: either 0.0-1.0 or a percentage like "50%".
     */
    private static float parseAlpha(String s) {
        if (s.endsWith("%")) {
            return Float.parseFloat(s.substring(0, s.length() - 1)) / 100.0f;
        }
        return Float.parseFloat(s);
    }

    /**
     * Clamp an integer to the 0-255 range.
     */
    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
