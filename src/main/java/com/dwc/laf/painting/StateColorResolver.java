package com.dwc.laf.painting;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics2D;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;

/**
 * Resolves the correct color for a component based on its current state.
 *
 * <p>Priority chain (highest to lowest):
 * <ol>
 *   <li>Disabled</li>
 *   <li>Pressed (armed + pressed, for {@link AbstractButton} only)</li>
 *   <li>Hover (rollover, for {@link AbstractButton} only)</li>
 *   <li>Focused</li>
 *   <li>Enabled (default)</li>
 * </ol>
 *
 * <p>Each state color falls back to the enabled color if {@code null}.</p>
 */
public final class StateColorResolver {

    private StateColorResolver() {
        // Non-instantiable utility class
    }

    /**
     * Returns the appropriate color for the component's current state.
     *
     * @param c        the component to inspect
     * @param enabled  the color for the enabled (default) state
     * @param disabled the color for the disabled state, or {@code null} to fall back
     * @param focused  the color for the focused state, or {@code null} to fall back
     * @param hover    the color for the hover/rollover state, or {@code null} to fall back
     * @param pressed  the color for the pressed state, or {@code null} to fall back
     * @return the resolved color; never {@code null} if {@code enabled} is non-null
     */
    public static Color resolve(Component c, Color enabled, Color disabled,
            Color focused, Color hover, Color pressed) {
        if (!c.isEnabled()) {
            return coalesce(disabled, enabled);
        }

        if (c instanceof AbstractButton ab) {
            ButtonModel model = ab.getModel();
            if (model.isArmed() && model.isPressed()) {
                return coalesce(pressed, enabled);
            }
            if (model.isRollover()) {
                return coalesce(hover, enabled);
            }
        }

        if (c.hasFocus()) {
            return coalesce(focused, enabled);
        }

        return enabled;
    }

    /**
     * Executes a paint action with reduced opacity by temporarily replacing
     * the graphics context's composite.
     *
     * @param g           the graphics context
     * @param opacity     the opacity (0.0 = fully transparent, 1.0 = fully opaque);
     *                    clamped to [0.0, 1.0]
     * @param paintAction the painting logic to execute at the given opacity
     */
    public static void paintWithOpacity(Graphics2D g, float opacity,
            Runnable paintAction) {
        float clamped = Math.max(0f, Math.min(1f, opacity));
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clamped));
        paintAction.run();
        g.setComposite(oldComposite);
    }

    private static Color coalesce(Color primary, Color fallback) {
        return primary != null ? primary : fallback;
    }
}
