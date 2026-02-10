package com.dwc.laf.ui;

import com.dwc.laf.painting.PaintUtils;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * A custom {@link javax.swing.plaf.ScrollBarUI} delegate that paints JScrollBar
 * components as thin modern scrollbars with no arrow buttons, rounded thumb with
 * hover color change, and subtle track background.
 *
 * <p>The scrollbar is rendered as a narrow (10px default) bar with:
 * <ul>
 *   <li>No decrease/increase arrow buttons (zero-size buttons)</li>
 *   <li>Flat track background colored from CSS tokens</li>
 *   <li>Rounded thumb inset from track edges for thin look</li>
 *   <li>Hover state: thumb changes to primary-light color on rollover</li>
 * </ul>
 *
 * <p>Each JScrollBar gets its own instance (not a shared singleton).</p>
 */
public class DwcScrollBarUI extends BasicScrollBarUI {

    private Color thumbColor;
    private Color hoverThumbColor;
    private Color trackColor;
    private int thumbArc;
    private int scrollBarWidth;

    /**
     * Creates a new {@code DwcScrollBarUI} instance for the given component.
     *
     * @param c the component (unused, required by L&F contract)
     * @return a new DwcScrollBarUI instance
     */
    public static ComponentUI createUI(JComponent c) {
        return new DwcScrollBarUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();

        thumbColor = UIManager.getColor("ScrollBar.thumbColor");
        hoverThumbColor = UIManager.getColor("ScrollBar.hoverThumbColor");
        trackColor = UIManager.getColor("ScrollBar.trackColor");

        int arcVal = UIManager.getInt("ScrollBar.thumbArc");
        thumbArc = arcVal > 0 ? arcVal : 8;

        int widthVal = UIManager.getInt("ScrollBar.width");
        scrollBarWidth = widthVal > 0 ? widthVal : 10;
    }

    // ---- Zero-size arrow buttons (FlatLaf pattern) ----

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return createZeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return createZeroButton();
    }

    private JButton createZeroButton() {
        JButton button = new JButton();
        Dimension zero = new Dimension(0, 0);
        button.setPreferredSize(zero);
        button.setMinimumSize(zero);
        button.setMaximumSize(zero);
        button.setFocusable(false);
        return button;
    }

    // ---- Size ----

    @Override
    protected Dimension getMinimumThumbSize() {
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            return new Dimension(scrollBarWidth, 20);
        } else {
            return new Dimension(20, scrollBarWidth);
        }
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension d = super.getPreferredSize(c);
        if (d == null) {
            return null;
        }
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            return new Dimension(scrollBarWidth, d.height);
        } else {
            return new Dimension(d.width, scrollBarWidth);
        }
    }

    // ---- Painting ----

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setColor(trackColor != null ? trackColor : c.getBackground());
            g2.fill(trackBounds);
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !c.isEnabled()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Object[] saved = PaintUtils.setupPaintingHints(g2);

            Color tc = isThumbRollover() ? hoverThumbColor : thumbColor;
            if (tc == null) {
                tc = Color.GRAY;
            }

            int inset = 3;
            g2.setColor(tc);
            g2.fill(PaintUtils.createRoundedShape(
                    thumbBounds.x + inset,
                    thumbBounds.y + inset,
                    thumbBounds.width - inset * 2,
                    thumbBounds.height - inset * 2,
                    thumbArc));

            PaintUtils.restorePaintingHints(g2, saved);
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintDecreaseHighlight(Graphics g) {
        // No-op: modern scrollbar has no highlight regions
    }

    @Override
    protected void paintIncreaseHighlight(Graphics g) {
        // No-op: modern scrollbar has no highlight regions
    }
}
