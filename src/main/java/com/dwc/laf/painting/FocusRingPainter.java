package com.dwc.laf.painting;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;

/**
 * Paints a semi-transparent focus ring outside component bounds, matching the
 * DWC CSS focus style: {@code box-shadow: 0 0 0 3px hsla(H, S, 45%, 0.4)}.
 *
 * <p>The ring is rendered as the difference between an outer and inner rounded
 * shape using the even-odd fill rule. The outer arc is expanded by the ring
 * width so the ring follows the component's curvature smoothly.</p>
 *
 * <p>Graphics2D rendering hints are saved before painting and restored
 * afterward.</p>
 */
public final class FocusRingPainter {

    private FocusRingPainter() {
        // Non-instantiable utility class
    }

    /**
     * Paints a focus ring outside the specified component bounds.
     *
     * <p>The ring is painted as a filled region between an outer shape
     * (expanded by {@code ringWidth}) and the inner component shape, using
     * {@link Path2D#WIND_EVEN_ODD} fill. The {@code ringColor} should
     * already include the desired alpha (e.g., 0.4 opacity for DWC style).</p>
     *
     * @param g            the graphics context
     * @param x            the component x coordinate
     * @param y            the component y coordinate
     * @param width        the component width
     * @param height       the component height
     * @param componentArc the component's corner arc diameter
     * @param ringWidth    the ring thickness in pixels (e.g., 3 for DWC default)
     * @param ringColor    the ring color with alpha; if {@code null}, nothing is painted
     */
    public static void paintFocusRing(Graphics2D g, float x, float y,
            float width, float height, float componentArc,
            float ringWidth, Color ringColor) {
        if (ringColor == null || ringWidth <= 0) {
            return;
        }

        Object[] saved = PaintUtils.setupPaintingHints(g);
        try {
            g.setColor(ringColor);

            // Outer ring bounds: expand outward by ringWidth on all sides
            float rx = x - ringWidth;
            float ry = y - ringWidth;
            float rw = width + ringWidth * 2;
            float rh = height + ringWidth * 2;

            // Outer arc expands to follow the component curvature
            float outerArc = componentArc + ringWidth;

            // Even-odd fill: the ring is the area between outer and inner shapes
            Path2D.Float path = new Path2D.Float(Path2D.WIND_EVEN_ODD);
            path.append(PaintUtils.createRoundedShape(rx, ry, rw, rh, outerArc), false);
            path.append(PaintUtils.createRoundedShape(x, y, width, height, componentArc), false);

            g.fill(path);
        } finally {
            PaintUtils.restorePaintingHints(g, saved);
        }
    }
}
