package com.dwc.laf.ui;

import com.dwc.laf.painting.PaintUtils;

import javax.swing.AbstractButton;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;

/**
 * Custom border for DWC-styled buttons that reserves space for the focus ring
 * and paints a rounded outline.
 *
 * <p>The border insets include three layers: the focus ring width (outermost),
 * the border width, and the button margin (innermost). This ensures that the
 * focus ring can be painted within the component's allocated bounds without
 * clipping by the parent container.</p>
 *
 * <p>The outline is painted in the content area (inside the focus ring space)
 * using {@link PaintUtils#paintOutline} with the even-odd fill rule for
 * sub-pixel precision.</p>
 *
 * <p>Application code can override the default margin by calling
 * {@link AbstractButton#setMargin(Insets)} with a non-{@link javax.swing.plaf.UIResource}
 * insets. The L&amp;F-set margin (which is a UIResource) will be used as the
 * default when no application override is present.</p>
 */
public class DwcButtonBorder extends AbstractBorder {

    private static final Insets DEFAULT_MARGIN = new Insets(2, 14, 2, 14);

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        int focusWidth = UIManager.getInt("Component.focusWidth");
        int borderWidth = UIManager.getInt("Component.borderWidth");

        Insets margin = DEFAULT_MARGIN;
        if (c instanceof AbstractButton ab) {
            Insets m = ab.getMargin();
            if (m != null && !(m instanceof javax.swing.plaf.UIResource)) {
                margin = m;
            }
        }

        insets.top = focusWidth + borderWidth + margin.top;
        insets.left = focusWidth + borderWidth + margin.left;
        insets.bottom = focusWidth + borderWidth + margin.bottom;
        insets.right = focusWidth + borderWidth + margin.right;
        return insets;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        int focusWidth = UIManager.getInt("Component.focusWidth");
        int borderWidth = UIManager.getInt("Component.borderWidth");
        int arc = UIManager.getInt("Button.arc");
        Color borderColor = UIManager.getColor("Button.borderColor");

        if (borderColor == null) {
            return;
        }

        if (c instanceof AbstractButton ab && !ab.isBorderPainted()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Object[] saved = PaintUtils.setupPaintingHints(g2);
            g2.setColor(borderColor);
            PaintUtils.paintOutline(g2,
                    focusWidth, focusWidth,
                    width - focusWidth * 2, height - focusWidth * 2,
                    borderWidth, arc);
            PaintUtils.restorePaintingHints(g2, saved);
        } finally {
            g2.dispose();
        }
    }
}
