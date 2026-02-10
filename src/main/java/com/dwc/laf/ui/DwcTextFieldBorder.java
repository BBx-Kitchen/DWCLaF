package com.dwc.laf.ui;

import com.dwc.laf.painting.PaintUtils;

import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;

/**
 * Custom border for DWC-styled text fields that reserves space for the focus ring
 * and paints a state-aware rounded outline.
 *
 * <p>The border insets include three layers: the focus ring width (outermost),
 * the border width, and the text field margin (innermost). This ensures that the
 * focus ring can be painted within the component's allocated bounds without
 * clipping by the parent container.</p>
 *
 * <p>The outline color varies based on component state:
 * <ul>
 *   <li>Focused or hovered: uses {@code TextField.hoverBorderColor} (the DWC
 *       accent/primary color for interactive states)</li>
 *   <li>Normal: uses {@code TextField.borderColor}</li>
 * </ul>
 *
 * <p>Hover state is communicated via a client property
 * {@code "DwcTextFieldUI.hover"} (Boolean) set by the {@code DwcTextFieldUI}
 * delegate's mouse listener, since {@link JTextComponent} has no rollover model.</p>
 *
 * <p>Application code can override the default margin by calling
 * {@link JTextComponent#setMargin(Insets)} with a non-{@link javax.swing.plaf.UIResource}
 * insets. The L&amp;F-set margin (which is a UIResource) will be used as the
 * default when no application override is present.</p>
 */
public class DwcTextFieldBorder extends AbstractBorder {

    private static final Insets DEFAULT_MARGIN = new Insets(2, 6, 2, 6);

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        int focusWidth = UIManager.getInt("Component.focusWidth");
        int borderWidth = UIManager.getInt("Component.borderWidth");

        Insets margin = DEFAULT_MARGIN;
        if (c instanceof JTextComponent tc) {
            Insets m = tc.getMargin();
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
        int arc = UIManager.getInt("TextField.arc");

        // Determine border color based on component state
        Color borderColor;
        if (!c.isEnabled()) {
            borderColor = UIManager.getColor("TextField.borderColor");
        } else if (c.hasFocus()) {
            borderColor = UIManager.getColor("TextField.hoverBorderColor");
        } else if (c instanceof JComponent jc
                && Boolean.TRUE.equals(jc.getClientProperty("DwcTextFieldUI.hover"))) {
            borderColor = UIManager.getColor("TextField.hoverBorderColor");
        } else {
            borderColor = UIManager.getColor("TextField.borderColor");
        }

        if (borderColor == null) {
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
