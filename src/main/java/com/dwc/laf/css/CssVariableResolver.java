package com.dwc.laf.css;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Pass 2 of the two-pass CSS token architecture: resolves all {@code var()}
 * references in the raw token map to produce a fully-resolved string map.
 *
 * <p>Given the raw token map from {@link CssTokenParser#parse(String)} (property
 * name to raw value string, potentially containing {@code var()} references),
 * this resolver expands every {@code var()} reference recursively, handles
 * fallbacks, and detects circular references via DFS graph coloring.</p>
 *
 * <p>Resolution is <strong>eager</strong>: all references are resolved upfront
 * in a single pass, catching all errors at load time. Tokens that cannot be
 * resolved (circular references, missing references with no fallback) are
 * excluded from the result map and logged as warnings.</p>
 *
 * <p>The returned map is immutable.</p>
 */
public final class CssVariableResolver {

    private static final Logger LOG = Logger.getLogger(CssVariableResolver.class.getName());

    private static final String VAR_PREFIX = "var(";

    private CssVariableResolver() {
        // utility class
    }

    /**
     * DFS visit states for cycle detection.
     */
    private enum State {
        UNVISITED, VISITING, VISITED
    }

    /**
     * Sentinel indicating that a value could not be resolved.
     * Used internally to distinguish "resolved to nothing" from "not resolved".
     */
    private static final String UNRESOLVABLE = "\0__UNRESOLVABLE__\0";

    /**
     * Resolves all {@code var()} references in the given raw token map.
     *
     * @param rawTokens the raw token map from {@link CssTokenParser#parse};
     *                  may be {@code null}
     * @return an unmodifiable map with all {@code var()} references expanded;
     *         tokens that cannot be resolved are excluded
     */
    public static Map<String, String> resolve(Map<String, String> rawTokens) {
        if (rawTokens == null || rawTokens.isEmpty()) {
            return Map.of();
        }

        // Resolved value cache (memoization)
        var resolved = new HashMap<String, String>(rawTokens.size());

        // DFS visit state per property
        var states = new HashMap<String, State>(rawTokens.size());
        for (String key : rawTokens.keySet()) {
            states.put(key, State.UNVISITED);
        }

        // Tracks variables that were detected as part of a cycle.
        // A variable with no fallback that participates in a cycle
        // should be excluded even if the cycle was broken elsewhere
        // by another variable's fallback.
        var cycleParticipants = new HashSet<String>();

        // Resolve each property
        var result = new LinkedHashMap<String, String>(rawTokens.size());
        for (var entry : rawTokens.entrySet()) {
            String name = entry.getKey();
            String value = resolveProperty(name, rawTokens, resolved, states, cycleParticipants);
            if (!UNRESOLVABLE.equals(value)) {
                result.put(name, value);
            }
        }

        // Post-process: exclude cycle participants whose raw value has
        // no fallback on the var() that caused the cycle. A variable
        // that was part of a cycle but resolved only because another
        // variable in the cycle had a fallback should still be excluded
        // if it has no fallback of its own.
        for (String cycleProp : cycleParticipants) {
            if (result.containsKey(cycleProp) && !hasFallback(rawTokens.get(cycleProp))) {
                result.remove(cycleProp);
                LOG.warning("Excluding cycle participant with no fallback: " + cycleProp);
            }
        }

        return Collections.unmodifiableMap(result);
    }

    /**
     * Checks if a raw value string contains a top-level var() with a fallback.
     * This is a simple heuristic: if the entire value is a single var() with
     * a fallback, the variable has its own escape from cycles.
     */
    private static boolean hasFallback(String rawValue) {
        if (rawValue == null || !rawValue.contains(VAR_PREFIX)) {
            return false;
        }
        // Find the first var( and check if it has a comma (fallback) at top level
        int varStart = rawValue.indexOf(VAR_PREFIX);
        int openParen = varStart + VAR_PREFIX.length() - 1;
        int closeParen = findMatchingParen(rawValue, openParen);
        if (closeParen == -1) return false;

        String content = rawValue.substring(openParen + 1, closeParen);
        // Check for comma at depth 0
        int depth = 0;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == ',' && depth == 0) return true;
        }
        return false;
    }

    /**
     * Resolves a single property's value, triggering recursive resolution
     * of any var() references. Uses DFS coloring for cycle detection.
     *
     * @return the resolved value, or {@link #UNRESOLVABLE} if it cannot be resolved
     */
    private static String resolveProperty(String name,
                                           Map<String, String> rawTokens,
                                           Map<String, String> resolved,
                                           Map<String, State> states,
                                           Set<String> cycleParticipants) {
        // Already resolved (memoized)?
        if (resolved.containsKey(name)) {
            return resolved.get(name);
        }

        // Cycle detection: if we're currently visiting this node, it's circular
        State state = states.getOrDefault(name, State.UNVISITED);
        if (state == State.VISITING) {
            // Circular reference detected -- record this variable as a cycle participant
            cycleParticipants.add(name);
            return UNRESOLVABLE;
        }

        String rawValue = rawTokens.get(name);
        if (rawValue == null) {
            // Not in the raw map at all (missing reference)
            return UNRESOLVABLE;
        }

        // Mark as visiting (in-progress DFS)
        states.put(name, State.VISITING);

        // Resolve the value string (which may contain var() references)
        String result = resolveValue(rawValue, rawTokens, resolved, states, cycleParticipants);

        if (UNRESOLVABLE.equals(result)) {
            states.put(name, State.VISITED);
            resolved.put(name, UNRESOLVABLE);
            LOG.warning("Could not resolve CSS variable: " + name);
            return UNRESOLVABLE;
        }

        // Mark as fully resolved
        states.put(name, State.VISITED);
        resolved.put(name, result);
        return result;
    }

    /**
     * Resolves a value string that may contain zero or more {@code var()}
     * references embedded within it.
     *
     * <p>Scans the string for {@code var(} occurrences, resolves each one,
     * and replaces it with the resolved value. If any embedded var() cannot
     * be resolved, the entire value is unresolvable.</p>
     *
     * @return the resolved string, or {@link #UNRESOLVABLE} if any part fails
     */
    private static String resolveValue(String value,
                                        Map<String, String> rawTokens,
                                        Map<String, String> resolved,
                                        Map<String, State> states,
                                        Set<String> cycleParticipants) {
        if (value == null || value.isEmpty()) {
            return value == null ? UNRESOLVABLE : value;
        }

        // Fast path: no var() references at all
        if (!value.contains(VAR_PREFIX)) {
            return value;
        }

        var sb = new StringBuilder(value.length());
        int i = 0;

        while (i < value.length()) {
            int varStart = value.indexOf(VAR_PREFIX, i);
            if (varStart == -1) {
                // No more var() -- append rest of string
                sb.append(value, i, value.length());
                break;
            }

            // Append the literal text before the var(
            sb.append(value, i, varStart);

            // Find the matching closing parenthesis
            int closePos = findMatchingParen(value, varStart + VAR_PREFIX.length() - 1);
            if (closePos == -1) {
                // Malformed var() -- no closing paren. Keep raw text.
                LOG.warning("Malformed var() expression (no closing paren): " + value);
                sb.append(value, varStart, value.length());
                i = value.length();
                break;
            }

            // Extract content inside var(...)
            String varContent = value.substring(varStart + VAR_PREFIX.length(), closePos);

            // Resolve this var() expression
            String resolvedVar = resolveVarExpression(varContent, rawTokens, resolved, states, cycleParticipants);
            if (UNRESOLVABLE.equals(resolvedVar)) {
                return UNRESOLVABLE;
            }

            sb.append(resolvedVar);
            i = closePos + 1; // move past the ')'
        }

        return sb.toString();
    }

    /**
     * Resolves the content of a single {@code var()} expression.
     *
     * <p>The content is everything between {@code var(} and its matching
     * {@code )}. It has the form: {@code variable-name} or
     * {@code variable-name, fallback-value}.</p>
     *
     * @param varContent the content inside var(...), e.g. "--a" or "--a, blue"
     * @return the resolved value, or {@link #UNRESOLVABLE}
     */
    private static String resolveVarExpression(String varContent,
                                                Map<String, String> rawTokens,
                                                Map<String, String> resolved,
                                                Map<String, State> states,
                                                Set<String> cycleParticipants) {
        // Split into variable name and optional fallback
        // Must respect nested parens: var(--a, var(--b, green))
        //   -> name="--a", fallback="var(--b, green)"
        String[] parts = splitVarArgs(varContent);
        String varName = parts[0].trim();
        String fallback = parts.length > 1 ? parts[1].trim() : null;

        // Try to resolve the referenced variable
        String value = resolveProperty(varName, rawTokens, resolved, states, cycleParticipants);

        if (!UNRESOLVABLE.equals(value)) {
            return value;
        }

        // Primary reference failed -- try fallback
        if (fallback != null) {
            // The fallback itself may contain var() references
            return resolveValue(fallback, rawTokens, resolved, states, cycleParticipants);
        }

        // No fallback, reference missing or circular
        return UNRESOLVABLE;
    }

    /**
     * Splits the content of a var() expression into [variable-name, fallback].
     * Respects nested parentheses so that a comma inside a nested var() is not
     * treated as the separator.
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>{@code "--a"} -> {@code ["--a"]}</li>
     *   <li>{@code "--a, blue"} -> {@code ["--a", " blue"]}</li>
     *   <li>{@code "--a, var(--b, green)"} -> {@code ["--a", " var(--b, green)"]}</li>
     * </ul>
     *
     * @return array of 1 element (name only) or 2 elements (name + fallback)
     */
    private static String[] splitVarArgs(String varContent) {
        int depth = 0;
        for (int i = 0; i < varContent.length(); i++) {
            char c = varContent.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                // Found the separator
                return new String[]{
                        varContent.substring(0, i),
                        varContent.substring(i + 1)
                };
            }
        }
        // No comma at top level -- name only, no fallback
        return new String[]{varContent};
    }

    /**
     * Finds the matching closing parenthesis for an opening parenthesis.
     *
     * @param value the string
     * @param openPos the position of the opening '('
     * @return the position of the matching ')', or -1 if not found
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
}
