package com.dwc.laf.ui;

import com.dwc.laf.painting.PaintUtils;
import com.dwc.laf.painting.ShadowPainter;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPanelUI;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * A custom {@link javax.swing.plaf.PanelUI} delegate that supports card-style
 * rendering with rounded corners and elevation shadow when activated via client
 * property.
 *
 * <p>Normal panels paint with standard opaque fill behavior via BasicPanelUI.
 * When the client property {@code "dwc.panelStyle"} is set to {@code "card"},
 * the panel renders with:
 * <ul>
 *   <li>A box shadow behind the component using {@link ShadowPainter}</li>
 *   <li>A rounded background fill using {@link PaintUtils#paintRoundedBackground}</li>
 *   <li>Non-opaque rendering so rounded corners show through</li>
 *   <li>Extra insets to accommodate the shadow extent</li>
 * </ul>
 *
 * <p>Each JPanel gets its own instance (not a shared singleton) for consistency
 * with the per-component pattern used by all DWC delegates.</p>
 */
public class DwcPanelUI extends BasicPanelUI {

    // Cached UIDefaults values
    private Color background;
    private int arc;
    private int focusWidth;
    private Color shadowColor;
    private int shadowBlurRadius;
    private int shadowOffsetY;

    /**
     * Creates a new per-component DwcPanelUI instance.
     *
     * @param c the component (unused, required by the L&F contract)
     * @return a new DwcPanelUI instance
     */
    public static ComponentUI createUI(JComponent c) {
        return new DwcPanelUI();
    }

    @Override
    protected void installDefaults(JPanel p) {
        super.installDefaults(p);

        // Cache Panel-specific UIDefaults
        background = UIManager.getColor("Panel.background");
        arc = UIManager.getInt("Panel.arc");
        if (arc <= 0) {
            arc = UIManager.getInt("Component.arc");
        }
        if (arc <= 0) {
            arc = 8;
        }

        focusWidth = UIManager.getInt("Component.focusWidth");

        shadowColor = UIManager.getColor("Panel.shadowColor");
        if (shadowColor == null) {
            shadowColor = new Color(0, 0, 0, 40);
        }

        shadowBlurRadius = UIManager.getInt("Panel.shadowBlurRadius");
        if (shadowBlurRadius <= 0) {
            shadowBlurRadius = 6;
        }

        shadowOffsetY = UIManager.getInt("Panel.shadowOffsetY");
        if (shadowOffsetY <= 0) {
            shadowOffsetY = 2;
        }
    }

    @Override
    public void update(Graphics g, JComponent c) {
        if (!isCardMode(c)) {
            // Normal mode: standard opaque fill + paint
            super.update(g, c);
            return;
        }

        // Card mode: non-opaque rounded background with shadow
        if (c.isOpaque()) {
            c.setOpaque(false);
        }

        int w = c.getWidth();
        int h = c.getHeight();
        int fw = focusWidth;

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            // 1. Paint shadow
            ShadowPainter.paintShadow(g2, fw, fw, w - fw * 2, h - fw * 2,
                    arc, shadowBlurRadius, 0, shadowOffsetY, shadowColor);

            // 2. Paint rounded background
            Color bg = c.getBackground() != null ? c.getBackground() : background;
            PaintUtils.paintRoundedBackground(g2, fw, fw,
                    w - fw * 2, h - fw * 2, arc, bg);
        } finally {
            g2.dispose();
        }

        // 3. Paint children (not super.update which would fill rectangular background)
        paint(g, c);
    }

    /**
     * Returns whether the component is in card mode.
     */
    private boolean isCardMode(JComponent c) {
        return "card".equals(c.getClientProperty("dwc.panelStyle"));
    }
}
