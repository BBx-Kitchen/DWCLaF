package com.dwc.laf.ui;

import com.dwc.laf.painting.FocusRingPainter;
import com.dwc.laf.painting.PaintUtils;
import com.dwc.laf.painting.StateColorResolver;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

/**
 * Custom {@link javax.swing.Icon} implementation for JCheckBox indicators.
 *
 * <p>Paints a rounded-rectangle box with a checkmark path when selected,
 * using DWC CSS token-derived colors from UIDefaults. Handles four visual
 * states: normal, hover, selected, and disabled.</p>
 *
 * <p>The icon reserves space for the focus ring outside the indicator box.
 * The focus ring is painted when the checkbox has focus.</p>
 */
public class DwcCheckBoxIcon implements javax.swing.Icon {

    private static final int ICON_SIZE = 16;

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
        int arc = UIManager.getInt("CheckBox.arc");

        float bx = x + fw;
        float by = y + fw;

        // Resolve background color
        Color bgColor;
        if (!enabled) {
            bgColor = UIManager.getColor("CheckBox.icon.background");
        } else if (selected && hover) {
            bgColor = UIManager.getColor("CheckBox.icon.hoverSelectedBackground");
        } else if (selected) {
            bgColor = UIManager.getColor("CheckBox.icon.selectedBackground");
        } else if (hover || pressed) {
            bgColor = UIManager.getColor("CheckBox.icon.hoverBackground");
        } else {
            bgColor = UIManager.getColor("CheckBox.icon.background");
        }

        // Resolve border color
        Color borderColor;
        if (!enabled) {
            borderColor = UIManager.getColor("CheckBox.icon.borderColor");
        } else if (selected) {
            borderColor = UIManager.getColor("CheckBox.icon.selectedBorderColor");
        } else if (hover || pressed) {
            borderColor = UIManager.getColor("CheckBox.icon.hoverBorderColor");
        } else {
            borderColor = UIManager.getColor("CheckBox.icon.borderColor");
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            // Disabled opacity
            if (!enabled) {
                Object opacityObj = UIManager.get("Component.disabledOpacity");
                float opacity = opacityObj instanceof Number num ? num.floatValue() : 0.6f;
                StateColorResolver.paintWithOpacity(g2, opacity, () -> {
                    paintIndicator(g2, bx, by, arc, bw, bgColor, borderColor, selected);
                });
            } else {
                paintIndicator(g2, bx, by, arc, bw, bgColor, borderColor, selected);
            }

            // Focus ring
            if (focused && enabled) {
                Color focusRingColor = UIManager.getColor("Component.focusRingColor");
                FocusRingPainter.paintFocusRing(g2, bx, by, ICON_SIZE, ICON_SIZE,
                        arc, fw, focusRingColor);
            }
        } finally {
            g2.dispose();
        }
    }

    private void paintIndicator(Graphics2D g2, float bx, float by, int arc,
            int bw, Color bgColor, Color borderColor, boolean selected) {
        // Paint background
        PaintUtils.paintRoundedBackground(g2, bx, by, ICON_SIZE, ICON_SIZE, arc, bgColor);

        // Paint border
        Object[] saved = PaintUtils.setupPaintingHints(g2);
        if (borderColor != null) {
            g2.setColor(borderColor);
            PaintUtils.paintOutline(g2, bx, by, ICON_SIZE, ICON_SIZE, bw, arc);
        }
        PaintUtils.restorePaintingHints(g2, saved);

        // Paint checkmark when selected
        if (selected) {
            Color checkColor = UIManager.getColor("CheckBox.icon.checkmarkColor");
            g2.setColor(checkColor != null ? checkColor : Color.WHITE);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_NORMALIZE);

            Path2D.Float path = new Path2D.Float();
            path.moveTo(bx + 4.8f, by + 8.0f);
            path.lineTo(bx + 7.04f, by + 10.67f);
            path.lineTo(bx + 12.0f, by + 3.73f);

            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(path);
        }
    }
}
