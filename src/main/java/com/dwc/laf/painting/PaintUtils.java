package com.dwc.laf.painting;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Shared painting utilities for antialiased rendering, rounded shape creation,
 * outline painting via even-odd fill, and rounded background fill.
 *
 * <p>All methods are stateless static utilities that save and restore Graphics2D
 * state when they modify rendering hints. Callers of {@link #paintOutline} are
 * responsible for managing hints themselves.</p>
 */
public final class PaintUtils {

    private PaintUtils() {
        // Non-instantiable utility class
    }

    /**
     * Saves the current antialiasing and stroke control rendering hints, then
     * enables antialiasing and stroke normalization.
     *
     * @param g the graphics context to configure
     * @return a two-element array of the previous hint values (may contain nulls)
     */
    public static Object[] setupPaintingHints(Graphics2D g) {
        Object[] saved = {
            g.getRenderingHint(RenderingHints.KEY_ANTIALIASING),
            g.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL)
        };
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                           RenderingHints.VALUE_STROKE_NORMALIZE);
        return saved;
    }

    /**
     * Restores rendering hints previously saved by {@link #setupPaintingHints}.
     *
     * @param g     the graphics context to restore
     * @param saved the hint array returned by {@code setupPaintingHints}
     */
    public static void restorePaintingHints(Graphics2D g, Object[] saved) {
        if (saved[0] != null) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, saved[0]);
        }
        if (saved[1] != null) {
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, saved[1]);
        }
    }

    /**
     * Creates a rounded shape with smart degeneration.
     *
     * <ul>
     *   <li>{@code arc <= 0}: returns {@link Rectangle2D.Float}</li>
     *   <li>{@code arc >= min(w, h)} and {@code w == h}: returns {@link Ellipse2D.Float}</li>
     *   <li>Otherwise: returns {@link RoundRectangle2D.Float} with the arc clamped
     *       to {@code min(w, h)}</li>
     * </ul>
     *
     * @param x   the x coordinate
     * @param y   the y coordinate
     * @param w   the width
     * @param h   the height
     * @param arc the corner arc diameter
     * @return the appropriate shape
     */
    public static Shape createRoundedShape(float x, float y, float w, float h, float arc) {
        if (arc <= 0) {
            return new Rectangle2D.Float(x, y, w, h);
        }
        float clampedArc = Math.min(arc, Math.min(w, h));
        if (clampedArc >= Math.min(w, h) && w == h) {
            return new Ellipse2D.Float(x, y, w, h);
        }
        return new RoundRectangle2D.Float(x, y, w, h, clampedArc, clampedArc);
    }

    /**
     * Paints an outline (ring) using the even-odd fill rule. The outline is the
     * region between an outer shape and an inset inner shape.
     *
     * <p>Coordinates are snapped to integer pixel boundaries before constructing
     * the path. This prevents fractional coordinates from causing the 1px outline
     * to spread across 2 device pixels via subpixel anti-aliasing, producing
     * maximum border crispness. Rounded corners remain smooth because
     * {@code VALUE_ANTIALIAS_ON} is still applied by the caller.</p>
     *
     * <p>The caller is responsible for setting up rendering hints and the paint
     * color on {@code g} before calling this method.</p>
     *
     * @param x         the x coordinate
     * @param y         the y coordinate
     * @param w         the width
     * @param h         the height
     * @param lineWidth the outline thickness
     * @param arc       the corner arc diameter for the outer shape
     */
    public static void paintOutline(Graphics2D g, float x, float y,
            float w, float h, float lineWidth, float arc) {
        // Snap to integer pixel boundaries for maximum 1px border crispness
        x = Math.round(x);
        y = Math.round(y);
        w = Math.round(w);
        h = Math.round(h);
        lineWidth = Math.max(1, Math.round(lineWidth));

        float innerArc = Math.max(arc - lineWidth, 0);
        Path2D path = new Path2D.Float(Path2D.WIND_EVEN_ODD);
        path.append(createRoundedShape(x, y, w, h, arc), false);
        path.append(createRoundedShape(
                x + lineWidth, y + lineWidth,
                w - lineWidth * 2, h - lineWidth * 2, innerArc), false);
        g.fill(path);
    }

    /**
     * Paints a filled rounded background. Sets up antialiasing hints, fills the
     * shape, and restores hints.
     *
     * @param g   the graphics context
     * @param x   the x coordinate
     * @param y   the y coordinate
     * @param w   the width
     * @param h   the height
     * @param arc the corner arc diameter
     * @param bg  the background color; if {@code null}, nothing is painted
     */
    public static void paintRoundedBackground(Graphics2D g, float x, float y,
            float w, float h, float arc, Color bg) {
        if (bg == null) {
            return;
        }
        Object[] saved = setupPaintingHints(g);
        g.setColor(bg);
        g.fill(createRoundedShape(x, y, w, h, arc));
        restorePaintingHints(g, saved);
    }
}
