package com.dwc.laf.ui;

import com.dwc.laf.painting.PaintUtils;

import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicToolTipUI;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * A custom {@link javax.swing.plaf.ToolTipUI} delegate that paints JToolTip
 * components with a rounded background, themed colors from CSS tokens, and
 * a drop shadow via {@link DwcToolTipBorder}.
 *
 * <p>The tooltip is set to non-opaque so that {@link BasicToolTipUI#paint}
 * skips its default background fill. This class paints the rounded background
 * first, then delegates text rendering to {@code super.paint()}.</p>
 *
 * <p>Each JToolTip gets its own instance (not a shared singleton).</p>
 */
public class DwcToolTipUI extends BasicToolTipUI {

    private Color background;
    private Color foreground;
    private int arc = DwcToolTipBorder.ARC;

    /**
     * Creates a new {@code DwcToolTipUI} instance for the given component.
     *
     * @param c the component (unused, required by L&F contract)
     * @return a new DwcToolTipUI instance
     */
    public static ComponentUI createUI(JComponent c) {
        return new DwcToolTipUI();
    }

    @Override
    protected void installDefaults(JComponent c) {
        super.installDefaults(c);

        background = UIManager.getColor("ToolTip.background");
        foreground = UIManager.getColor("ToolTip.foreground");

        // Non-opaque: BasicToolTipUI.paint() will skip background fill
        LookAndFeel.installProperty(c, "opaque", false);

        // Install custom border if none or UIResource
        if (c.getBorder() == null || c.getBorder() instanceof UIResource) {
            c.setBorder(new DwcToolTipBorder());
        }
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            int shadowSize = DwcToolTipBorder.SHADOW_SIZE;

            // Inner rectangle (inside shadow area)
            float ix = shadowSize;
            float iy = shadowSize;
            float iw = c.getWidth() - shadowSize * 2;
            float ih = c.getHeight() - shadowSize * 2;

            // Paint rounded background
            PaintUtils.paintRoundedBackground(g2, ix, iy, iw, ih, arc, background);
        } finally {
            g2.dispose();
        }

        // Delegate text rendering to BasicToolTipUI (opaque=false, so no bg fill)
        super.paint(g, c);
    }
}
