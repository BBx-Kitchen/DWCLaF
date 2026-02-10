package com.dwc.laf.ui;

import com.dwc.laf.painting.PaintUtils;

import javax.swing.Icon;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;

/**
 * A custom expand/collapse {@link Icon} for JTree nodes that renders
 * stroked chevron arrows instead of the default filled triangles.
 *
 * <p>When expanded, draws a downward-pointing chevron ({@code v}).
 * When collapsed, draws a rightward-pointing chevron ({@code >}).</p>
 *
 * <p>The icon color is read from {@code UIManager.getColor("Tree.expandedIcon.color")}
 * with a fallback to {@link Color#DARK_GRAY}.</p>
 */
public class DwcTreeExpandIcon implements Icon {

    private static final int SIZE = 12;

    private final boolean expanded;

    /**
     * Creates a new expand/collapse icon.
     *
     * @param expanded true for expanded (downward chevron), false for collapsed (rightward chevron)
     */
    public DwcTreeExpandIcon(boolean expanded) {
        this.expanded = expanded;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Object[] saved = PaintUtils.setupPaintingHints(g2);

            Color color = UIManager.getColor("Tree.expandedIcon.color");
            if (color == null) {
                color = Color.DARK_GRAY;
            }
            g2.setColor(color);

            float cx = x + SIZE / 2f;
            float cy = y + SIZE / 2f;
            float arrowSize = 5f;

            Path2D.Float arrow = new Path2D.Float();
            if (expanded) {
                // Downward chevron (v)
                arrow.moveTo(cx - arrowSize / 2, cy - arrowSize / 4);
                arrow.lineTo(cx, cy + arrowSize / 4);
                arrow.lineTo(cx + arrowSize / 2, cy - arrowSize / 4);
            } else {
                // Rightward chevron (>)
                arrow.moveTo(cx - arrowSize / 4, cy - arrowSize / 2);
                arrow.lineTo(cx + arrowSize / 4, cy);
                arrow.lineTo(cx - arrowSize / 4, cy + arrowSize / 2);
            }

            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(arrow);

            PaintUtils.restorePaintingHints(g2, saved);
        } finally {
            g2.dispose();
        }
    }

    @Override
    public int getIconWidth() {
        return SIZE;
    }

    @Override
    public int getIconHeight() {
        return SIZE;
    }
}
