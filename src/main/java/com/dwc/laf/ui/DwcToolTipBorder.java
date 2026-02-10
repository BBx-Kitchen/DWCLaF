package com.dwc.laf.ui;

import com.dwc.laf.painting.PaintUtils;
import com.dwc.laf.painting.ShadowPainter;

import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;

/**
 * Custom border for DWC-styled tooltips that provides shadow rendering and
 * rounded outline painting.
 *
 * <p>The border insets reserve space for the drop shadow on all sides plus
 * inner padding for text content. The shadow is painted via
 * {@link ShadowPainter} and the rounded outline via {@link PaintUtils}.</p>
 */
public class DwcToolTipBorder extends AbstractBorder {

    /** Shadow extent on each side. */
    static final int SHADOW_SIZE = 6;

    /** Corner arc diameter for the tooltip shape. */
    static final int ARC = 8;

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(
                SHADOW_SIZE + 4,
                SHADOW_SIZE + 8,
                SHADOW_SIZE + 4,
                SHADOW_SIZE + 8
        );
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.top = SHADOW_SIZE + 4;
        insets.left = SHADOW_SIZE + 8;
        insets.bottom = SHADOW_SIZE + 4;
        insets.right = SHADOW_SIZE + 8;
        return insets;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            // Content area (inside shadow space)
            int cx = x + SHADOW_SIZE;
            int cy = y + SHADOW_SIZE;
            int cw = w - SHADOW_SIZE * 2;
            int ch = h - SHADOW_SIZE * 2;

            // Paint drop shadow
            Color shadowColor = UIManager.getColor("Panel.shadowColor");
            if (shadowColor == null) {
                shadowColor = new Color(0, 0, 0, 40);
            }
            ShadowPainter.paintShadow(g2, cx, cy, cw, ch, ARC, 4f, 0, 2, shadowColor);

            // Paint rounded border outline
            Color borderColor = UIManager.getColor("ToolTip.borderColor");
            if (borderColor == null) {
                borderColor = Color.GRAY;
            }
            Object[] saved = PaintUtils.setupPaintingHints(g2);
            g2.setColor(borderColor);
            PaintUtils.paintOutline(g2, cx, cy, cw, ch, 1f, ARC);
            PaintUtils.restorePaintingHints(g2, saved);
        } finally {
            g2.dispose();
        }
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }
}
