package com.dwc.laf.ui;

import com.dwc.laf.painting.FocusRingPainter;
import com.dwc.laf.painting.PaintUtils;
import com.dwc.laf.painting.StateColorResolver;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;

/**
 * Custom {@link javax.swing.Icon} implementation for JRadioButton indicators.
 *
 * <p>Paints a circular indicator with a center dot when selected, using DWC
 * CSS token-derived colors from UIDefaults. Handles four visual states:
 * normal, hover, selected, and disabled.</p>
 *
 * <p>The icon reserves space for the focus ring outside the circular indicator.
 * The focus ring is painted as a circular ring when the radio button has focus.</p>
 */
public class DwcRadioButtonIcon implements javax.swing.Icon {

    private static final int ICON_SIZE = 16;
    private static final float DOT_DIAMETER = 8f;

    @Override
    public int getIconWidth() {
        return ICON_SIZE + UIManager.getInt("Component.focusWidth") * 2;
    }

    @Override
    public int getIconHeight() {
        return ICON_SIZE + UIManager.getInt("Component.focusWidth") * 2;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        AbstractButton b = (AbstractButton) c;
        ButtonModel model = b.getModel();

        boolean selected = model.isSelected();
        boolean hover = model.isRollover();
        boolean pressed = model.isArmed() && model.isPressed();
        boolean focused = b.hasFocus();
        boolean enabled = b.isEnabled();

        int fw = UIManager.getInt("Component.focusWidth");
        int bw = UIManager.getInt("Component.borderWidth");

        float bx = x + fw;
        float by = y + fw;

        // Resolve background color
        Color bgColor;
        if (!enabled) {
            bgColor = UIManager.getColor("RadioButton.icon.background");
        } else if (selected && hover) {
            bgColor = UIManager.getColor("RadioButton.icon.hoverSelectedBackground");
        } else if (selected) {
            bgColor = UIManager.getColor("RadioButton.icon.selectedBackground");
        } else if (hover || pressed) {
            bgColor = UIManager.getColor("RadioButton.icon.hoverBackground");
        } else {
            bgColor = UIManager.getColor("RadioButton.icon.background");
        }

        // Resolve border color
        Color borderColor;
        if (!enabled) {
            borderColor = UIManager.getColor("RadioButton.icon.borderColor");
        } else if (selected) {
            borderColor = UIManager.getColor("RadioButton.icon.selectedBorderColor");
        } else if (hover || pressed) {
            borderColor = UIManager.getColor("RadioButton.icon.hoverBorderColor");
        } else {
            borderColor = UIManager.getColor("RadioButton.icon.borderColor");
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            // Disabled opacity
            if (!enabled) {
                Object opacityObj = UIManager.get("Component.disabledOpacity");
                float opacity = opacityObj instanceof Number num ? num.floatValue() : 0.6f;
                StateColorResolver.paintWithOpacity(g2, opacity, () -> {
                    paintIndicator(g2, bx, by, bw, bgColor, borderColor, selected);
                });
            } else {
                paintIndicator(g2, bx, by, bw, bgColor, borderColor, selected);
            }

            // Focus ring (circular)
            if (focused && enabled) {
                Color focusRingColor = UIManager.getColor("Component.focusRingColor");
                FocusRingPainter.paintFocusRing(g2, bx, by, ICON_SIZE, ICON_SIZE,
                        ICON_SIZE, fw, focusRingColor);
            }
        } finally {
            g2.dispose();
        }
    }

    private void paintIndicator(Graphics2D g2, float bx, float by, int bw,
            Color bgColor, Color borderColor, boolean selected) {
        // Paint circular background (arc = ICON_SIZE for full circle)
        PaintUtils.paintRoundedBackground(g2, bx, by, ICON_SIZE, ICON_SIZE, ICON_SIZE, bgColor);

        // Paint circular border
        Object[] saved = PaintUtils.setupPaintingHints(g2);
        if (borderColor != null) {
            g2.setColor(borderColor);
            PaintUtils.paintOutline(g2, bx, by, ICON_SIZE, ICON_SIZE, bw, ICON_SIZE);
        }
        PaintUtils.restorePaintingHints(g2, saved);

        // Paint center dot when selected
        if (selected) {
            Color dotColor = UIManager.getColor("RadioButton.icon.dotColor");
            g2.setColor(dotColor != null ? dotColor : Color.WHITE);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            float dotX = bx + (ICON_SIZE - DOT_DIAMETER) / 2f;
            float dotY = by + (ICON_SIZE - DOT_DIAMETER) / 2f;
            g2.fill(new Ellipse2D.Float(dotX, dotY, DOT_DIAMETER, DOT_DIAMETER));
        }
    }
}
