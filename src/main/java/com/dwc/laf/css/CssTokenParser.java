package com.dwc.laf.css;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Pass 1 of the two-pass CSS token architecture: extracts custom property
 * declarations from CSS text into a raw string map.
 *
 * <p>Parses CSS text and extracts all {@code --custom-property: value}
 * declarations from {@code :root} and component-level selectors, flattening
 * into a single map where later declarations override earlier ones for the
 * same property name. This gives component-level properties natural precedence
 * over {@code :root} properties when the component block follows the root block
 * (which is the standard CSS authoring pattern).</p>
 *
 * <p>var() references are NOT resolved here -- they are extracted as raw
 * strings. Resolution happens in Pass 2 ({@code CssVariableResolver}).</p>
 *
 * <p>Implementation: hand-written character-by-character parser. Strips CSS
 * block comments, finds selector blocks by matching braces with nesting
 * awareness, and within each block scans for {@code --} prefixed declarations.</p>
 */
public final class CssTokenParser {

    private static final Logger LOG = Logger.getLogger(CssTokenParser.class.getName());

    private CssTokenParser() {
        // utility class
    }

    /**
     * Parses CSS text and extracts all custom property declarations.
     *
     * @param cssText the CSS text to parse; may be {@code null} or empty
     * @return an unmodifiable map of property name to raw value string,
     *         preserving insertion order with later declarations overriding
     *         earlier ones for the same key
     */
    public static Map<String, String> parse(String cssText) {
        if (cssText == null || cssText.isBlank()) {
            return Map.of();
        }

        // Step 1: Strip all block comments (/* ... */)
        String stripped = stripComments(cssText);

        // Step 2: Walk the text finding selector blocks and extracting declarations
        var tokens = new LinkedHashMap<String, String>();
        parseBlocks(stripped, tokens);

        return Collections.unmodifiableMap(tokens);
    }

    // ================================================================
    // Comment stripping
    // ================================================================

    /**
     * Removes all CSS block comments ({@code /* ... * /}) from the input.
     * Handles nested comment-like sequences inside strings gracefully by
     * simply scanning for {@code /*} and {@code * /} pairs.
     */
    static String stripComments(String css) {
        var sb = new StringBuilder(css.length());
        int i = 0;
        while (i < css.length()) {
            if (i + 1 < css.length() && css.charAt(i) == '/' && css.charAt(i + 1) == '*') {
                // Find closing */
                int end = css.indexOf("*/", i + 2);
                if (end == -1) {
                    // Unclosed comment -- skip rest of file
                    LOG.warning("Unclosed CSS comment starting at position " + i);
                    break;
                }
                i = end + 2;
            } else {
                sb.append(css.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    // ================================================================
    // Block parsing
    // ================================================================

    /**
     * Walks the CSS text finding selector blocks (delimited by {@code { }})
     * and extracts custom property declarations from each block.
     */
    private static void parseBlocks(String css, Map<String, String> tokens) {
        int i = 0;
        int len = css.length();

        while (i < len) {
            // Find the next opening brace
            int openBrace = css.indexOf('{', i);
            if (openBrace == -1) {
                break; // no more blocks
            }

            // Find the matching closing brace (handling nesting)
            int closeBrace = findMatchingBrace(css, openBrace);
            if (closeBrace == -1) {
                LOG.warning("Unmatched opening brace at position " + openBrace);
                break;
            }

            // Extract the block content (between braces)
            String blockContent = css.substring(openBrace + 1, closeBrace);

            // Extract custom property declarations from this block
            extractDeclarations(blockContent, tokens);

            // Move past the closing brace
            i = closeBrace + 1;
        }
    }

    /**
     * Finds the matching closing brace for an opening brace, handling nesting.
     *
     * @param css the CSS text
     * @param openBrace position of the opening brace
     * @return position of the matching closing brace, or -1 if not found
     */
    private static int findMatchingBrace(String css, int openBrace) {
        int depth = 1;
        int i = openBrace + 1;
        while (i < css.length() && depth > 0) {
            char c = css.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            }
            if (depth > 0) {
                i++;
            }
        }
        return depth == 0 ? i : -1;
    }

    // ================================================================
    // Declaration extraction
    // ================================================================

    /**
     * Extracts custom property declarations (those starting with {@code --})
     * from a CSS block's content string.
     *
     * <p>Scans for declaration starts ({@code --} at the beginning of a
     * declaration), reads the property name until {@code :}, and reads the
     * value until {@code ;} or end of block content. Values within
     * parenthesized expressions (like {@code var()}, {@code rgb()},
     * {@code calc()}) are handled by tracking parenthesis depth so that
     * semicolons or commas inside function calls don't prematurely terminate
     * the value.</p>
     */
    private static void extractDeclarations(String block, Map<String, String> tokens) {
        int i = 0;
        int len = block.length();

        while (i < len) {
            // Skip whitespace
            i = skipWhitespace(block, i);
            if (i >= len) break;

            // Check if this is a custom property declaration (starts with --)
            if (i + 1 < len && block.charAt(i) == '-' && block.charAt(i + 1) == '-') {
                // Read property name until ':'
                int colonPos = findColon(block, i);
                if (colonPos == -1) {
                    // Malformed: no colon found. Skip to next ; or end
                    LOG.warning("Malformed custom property declaration (no colon): "
                            + excerptFrom(block, i));
                    i = skipToNextDeclaration(block, i);
                    continue;
                }

                String name = block.substring(i, colonPos).trim();

                // Read value until ';' or end of block, respecting parenthesis nesting
                int valueStart = colonPos + 1;
                int valueEnd = findValueEnd(block, valueStart);
                String rawValue = block.substring(valueStart, valueEnd).trim();

                // Normalize internal whitespace (collapse multi-line / multi-space)
                rawValue = normalizeWhitespace(rawValue);

                if (!name.isEmpty()) {
                    tokens.put(name, rawValue);
                }

                // Move past the semicolon if present
                i = valueEnd;
                if (i < len && block.charAt(i) == ';') {
                    i++;
                }
            } else {
                // Not a custom property -- skip to next semicolon or end
                i = skipToNextDeclaration(block, i);
            }
        }
    }

    /**
     * Finds the colon separating property name from value, but only at the
     * top level (not inside parentheses or brackets).
     */
    private static int findColon(String block, int start) {
        for (int i = start; i < block.length(); i++) {
            char c = block.charAt(i);
            if (c == ':') return i;
            if (c == ';' || c == '}') return -1; // hit end of declaration without colon
        }
        return -1;
    }

    /**
     * Finds the end of a CSS value, respecting parenthesis nesting so that
     * semicolons inside {@code var()}, {@code rgb()}, etc. don't terminate
     * the value prematurely. The value ends at an unparenthesized {@code ;}
     * or at the end of the block content.
     */
    private static int findValueEnd(String block, int start) {
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = start; i < block.length(); i++) {
            char c = block.charAt(i);

            // Track string literals
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }
            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }
            if (inSingleQuote || inDoubleQuote) {
                continue;
            }

            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth < 0) depth = 0; // safety
            } else if (c == ';' && depth == 0) {
                return i;
            }
        }
        // Reached end of block content -- value terminates at end
        return block.length();
    }

    // ================================================================
    // Utility methods
    // ================================================================

    private static int skipWhitespace(String s, int pos) {
        while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
            pos++;
        }
        return pos;
    }

    /**
     * Skips to the next declaration start: past the next {@code ;} or to end.
     */
    private static int skipToNextDeclaration(String s, int pos) {
        for (int i = pos; i < s.length(); i++) {
            if (s.charAt(i) == ';') {
                return i + 1;
            }
        }
        return s.length();
    }

    /**
     * Returns a short excerpt of the string from the given position, for log messages.
     */
    private static String excerptFrom(String s, int pos) {
        int end = Math.min(pos + 40, s.length());
        String excerpt = s.substring(pos, end).replace('\n', ' ').replace('\r', ' ');
        return excerpt + (end < s.length() ? "..." : "");
    }

    /**
     * Normalizes whitespace in a CSS value: collapses runs of whitespace
     * (including newlines) into single spaces, and trims the result.
     */
    static String normalizeWhitespace(String value) {
        if (value.indexOf('\n') == -1 && value.indexOf('\r') == -1) {
            // Fast path: no newlines, just trim
            return value.trim();
        }
        // Replace runs of whitespace with single space
        var sb = new StringBuilder(value.length());
        boolean lastWasSpace = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isWhitespace(c)) {
                if (!lastWasSpace) {
                    sb.append(' ');
                    lastWasSpace = true;
                }
            } else {
                sb.append(c);
                lastWasSpace = false;
            }
        }
        return sb.toString().trim();
    }
}
