package com.dwc.laf.css;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Pass 2.5 of the CSS token pipeline: evaluates {@code calc()} expressions
 * in resolved string values, replacing them with computed numeric results.
 *
 * <p>Runs after {@link CssVariableResolver} (all var() references expanded)
 * and before {@link CssValueTyper} (which needs plain numeric values to
 * classify colors, dimensions, etc.).</p>
 *
 * <p>Example: {@code hsl(0, 0%, calc((40 - 50) * -100%))} becomes
 * {@code hsl(0, 0%, 100%)} (percentage clamped to 0–100).</p>
 *
 * <p>The evaluator uses a small recursive-descent parser that supports
 * {@code +}, {@code -}, {@code *}, {@code /}, parentheses, unary minus,
 * and CSS units ({@code %}, {@code px}, {@code rem}, etc.).</p>
 *
 * <p>Non-calc values pass through unchanged. Unparseable calc() expressions
 * are left as-is and logged as warnings. The returned map is immutable.</p>
 */
public final class CssCalcEvaluator {

    private static final Logger LOG = Logger.getLogger(CssCalcEvaluator.class.getName());

    private CssCalcEvaluator() {
        // utility class
    }

    /**
     * Evaluates all {@code calc()} expressions in the given resolved token map.
     *
     * @param resolved the resolved token map from {@link CssVariableResolver#resolve};
     *                 may be {@code null}
     * @return an unmodifiable map with all {@code calc()} expressions replaced
     *         by computed numeric results
     */
    public static Map<String, String> evaluate(Map<String, String> resolved) {
        if (resolved == null || resolved.isEmpty()) {
            return Map.of();
        }

        var result = new LinkedHashMap<String, String>(resolved.size());

        for (var entry : resolved.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            result.put(name, evaluateValue(value));
        }

        return Collections.unmodifiableMap(result);
    }

    /**
     * Evaluates all calc() substrings within a single value string.
     */
    static String evaluateValue(String value) {
        if (value == null || value.isEmpty()) {
            return value == null ? "" : value;
        }

        String lower = value.toLowerCase(Locale.ROOT);
        if (!lower.contains("calc(")) {
            return value;
        }

        var sb = new StringBuilder(value.length());
        int i = 0;

        while (i < value.length()) {
            int calcStart = value.toLowerCase(Locale.ROOT).indexOf("calc(", i);
            if (calcStart == -1) {
                sb.append(value, i, value.length());
                break;
            }

            // Append text before calc(
            sb.append(value, i, calcStart);

            // Find matching closing paren
            int openParen = calcStart + 4; // position of '('
            int closeParen = findMatchingParen(value, openParen);
            if (closeParen == -1) {
                // Malformed: no closing paren, keep as-is
                LOG.warning("Malformed calc() expression (no closing paren): " + value);
                sb.append(value, calcStart, value.length());
                i = value.length();
                break;
            }

            String expr = value.substring(openParen + 1, closeParen);

            try {
                CalcResult computed = parseExpression(expr);
                String formatted = formatResult(computed);
                sb.append(formatted);
            } catch (CalcParseException e) {
                LOG.warning("Could not evaluate calc(" + expr + "): " + e.getMessage());
                sb.append(value, calcStart, closeParen + 1);
            }

            i = closeParen + 1;
        }

        return sb.toString();
    }

    // ================================================================
    // Recursive-descent parser
    // ================================================================

    /**
     * Parses and evaluates a calc() expression string.
     *
     * <pre>
     * expr     → term (('+' | '-') term)*
     * term     → unary (('*' | '/') unary)*
     * unary    → '-' unary | primary
     * primary  → NUMBER [UNIT] | '(' expr ')' [UNIT]
     * </pre>
     */
    static CalcResult parseExpression(String expr) {
        Parser parser = new Parser(expr.trim());
        CalcResult result = parser.parseExpr();
        parser.skipWhitespace();
        if (parser.pos < parser.input.length()) {
            throw new CalcParseException("Unexpected trailing content: '"
                    + parser.input.substring(parser.pos) + "'");
        }
        return result;
    }

    /**
     * Holds the numeric value and optional CSS unit from a calc() evaluation.
     */
    static final class CalcResult {
        final double value;
        final String unit; // empty string if unitless

        CalcResult(double value, String unit) {
            this.value = value;
            this.unit = unit == null ? "" : unit;
        }
    }

    /**
     * Formats a CalcResult as a string, applying clamping for percentages.
     */
    private static String formatResult(CalcResult result) {
        double value = result.value;

        // Clamp percentage results to 0-100
        if ("%".equals(result.unit)) {
            value = Math.max(0, Math.min(100, value));
        }

        // Format: avoid trailing ".0" for whole numbers
        String formatted;
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            formatted = String.valueOf((long) value);
        } else {
            formatted = String.valueOf(value);
            // Remove trailing zeros after decimal point but keep at least one decimal
            if (formatted.contains(".")) {
                formatted = formatted.replaceAll("0+$", "");
                if (formatted.endsWith(".")) {
                    formatted = formatted.substring(0, formatted.length() - 1);
                }
            }
        }

        return formatted + result.unit;
    }

    /**
     * Recursive-descent parser for calc() expressions.
     */
    private static final class Parser {
        final String input;
        int pos;

        Parser(String input) {
            this.input = input;
            this.pos = 0;
        }

        void skipWhitespace() {
            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
                pos++;
            }
        }

        char peek() {
            skipWhitespace();
            if (pos >= input.length()) return '\0';
            return input.charAt(pos);
        }

        /**
         * expr → term (('+' | '-') term)*
         *
         * Per CSS spec, + and - in calc() must be surrounded by whitespace.
         * We're lenient here since we're evaluating post-resolution values.
         */
        CalcResult parseExpr() {
            CalcResult left = parseTerm();

            while (true) {
                skipWhitespace();
                if (pos >= input.length()) break;
                char op = input.charAt(pos);
                if (op != '+' && op != '-') break;

                // Distinguish binary +/- from unary - in a number.
                // If preceded by whitespace and followed by a digit without whitespace,
                // that's still a binary op in calc() context when we've already parsed
                // a left operand. But we also need to handle the case where there's
                // no whitespace before the operator -- be lenient.
                pos++;
                CalcResult right = parseTerm();
                String unit = mergeUnits(left.unit, right.unit);

                if (op == '+') {
                    left = new CalcResult(left.value + right.value, unit);
                } else {
                    left = new CalcResult(left.value - right.value, unit);
                }
            }

            return left;
        }

        /**
         * term → unary (('*' | '/') unary)*
         */
        CalcResult parseTerm() {
            CalcResult left = parseUnary();

            while (true) {
                skipWhitespace();
                if (pos >= input.length()) break;
                char op = input.charAt(pos);
                if (op != '*' && op != '/') break;

                pos++;
                CalcResult right = parseUnary();

                if (op == '*') {
                    // unit: one side should be unitless
                    String unit = left.unit.isEmpty() ? right.unit : left.unit;
                    left = new CalcResult(left.value * right.value, unit);
                } else {
                    if (right.value == 0) {
                        throw new CalcParseException("Division by zero");
                    }
                    // unit: if dividing by unitless, preserve left unit
                    String unit = right.unit.isEmpty() ? left.unit : "";
                    left = new CalcResult(left.value / right.value, unit);
                }
            }

            return left;
        }

        /**
         * unary → '-' unary | '+' unary | primary
         */
        CalcResult parseUnary() {
            skipWhitespace();
            if (pos < input.length() && input.charAt(pos) == '-') {
                pos++;
                CalcResult inner = parseUnary();
                return new CalcResult(-inner.value, inner.unit);
            }
            if (pos < input.length() && input.charAt(pos) == '+') {
                pos++;
                return parseUnary();
            }
            return parsePrimary();
        }

        /**
         * primary → NUMBER [UNIT] | '(' expr ')' [UNIT]
         */
        CalcResult parsePrimary() {
            skipWhitespace();

            if (pos >= input.length()) {
                throw new CalcParseException("Unexpected end of expression");
            }

            char c = input.charAt(pos);

            // Parenthesized sub-expression
            if (c == '(') {
                pos++; // skip '('
                CalcResult inner = parseExpr();
                skipWhitespace();
                if (pos >= input.length() || input.charAt(pos) != ')') {
                    throw new CalcParseException("Expected closing ')'");
                }
                pos++; // skip ')'

                // Check for unit suffix after closing paren, e.g. (40 - 50)%
                // This is not standard CSS but appears in some shorthand patterns
                String trailingUnit = tryParseUnit();
                if (!trailingUnit.isEmpty()) {
                    String unit = inner.unit.isEmpty() ? trailingUnit : inner.unit;
                    return new CalcResult(inner.value, unit);
                }

                return inner;
            }

            // Number literal
            if (c == '.' || (c >= '0' && c <= '9')) {
                return parseNumber();
            }

            throw new CalcParseException("Unexpected character: '" + c + "' at position " + pos);
        }

        /**
         * Parses a number literal with optional unit suffix.
         */
        CalcResult parseNumber() {
            int start = pos;

            // Integer or decimal part
            while (pos < input.length() && (input.charAt(pos) >= '0' && input.charAt(pos) <= '9')) {
                pos++;
            }
            if (pos < input.length() && input.charAt(pos) == '.') {
                pos++;
                while (pos < input.length() && (input.charAt(pos) >= '0' && input.charAt(pos) <= '9')) {
                    pos++;
                }
            }

            if (pos == start) {
                throw new CalcParseException("Expected number at position " + pos);
            }

            double value = Double.parseDouble(input.substring(start, pos));
            String unit = tryParseUnit();

            return new CalcResult(value, unit);
        }

        /**
         * Tries to parse a CSS unit suffix (%, px, rem, em, etc.).
         * Returns empty string if no unit found.
         */
        String tryParseUnit() {
            if (pos >= input.length()) return "";

            // Check for % first
            if (input.charAt(pos) == '%') {
                pos++;
                return "%";
            }

            // Check for alphabetic units (px, rem, em, vw, vh, etc.)
            int start = pos;
            while (pos < input.length() && Character.isLetter(input.charAt(pos))) {
                pos++;
            }

            if (pos > start) {
                return input.substring(start, pos);
            }

            return "";
        }
    }

    /**
     * Merges units from two operands in an addition/subtraction.
     * If both have units, they should match (we take the first).
     * If one is unitless, the other's unit wins.
     */
    private static String mergeUnits(String left, String right) {
        if (left.isEmpty()) return right;
        if (right.isEmpty()) return left;
        // Both have units -- take the left (they should match per CSS spec)
        return left;
    }

    /**
     * Finds the matching closing parenthesis for an opening parenthesis.
     */
    private static int findMatchingParen(String value, int openPos) {
        int depth = 1;
        for (int i = openPos + 1; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Exception for calc() parse errors.
     */
    static final class CalcParseException extends RuntimeException {
        CalcParseException(String message) {
            super(message);
        }
    }
}
