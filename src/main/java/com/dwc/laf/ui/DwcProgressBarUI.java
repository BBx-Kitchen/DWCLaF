package com.dwc.laf.ui;

import com.dwc.laf.painting.PaintUtils;
import com.dwc.laf.painting.StateColorResolver;

import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.util.HashMap;
import java.util.Map;

/**
 * A custom {@link javax.swing.plaf.ProgressBarUI} delegate that paints JProgressBar
 * components with rounded track and fill bars, themed colors from CSS tokens,
 * and color variant support.
 *
 * <p>Supports five visual variants:
 * <ul>
 *   <li><b>Default:</b> primary-colored fill bar</li>
 *   <li><b>Success:</b> green fill via {@code putClientProperty("dwc.progressType", "success")}</li>
 *   <li><b>Danger:</b> red fill via {@code "danger"}</li>
 *   <li><b>Warning:</b> amber fill via {@code "warning"}</li>
 *   <li><b>Info:</b> info-colored fill via {@code "info"}</li>
 * </ul>
 *
 * <p>Both determinate and indeterminate modes are supported with rounded painting.
 * Indeterminate mode uses BasicProgressBarUI's animation timer for the bouncing bar.</p>
 *
 * <p>Each JProgressBar gets its own instance (not a shared singleton).</p>
 */
public class DwcProgressBarUI extends BasicProgressBarUI {

    private Color foreground;
    private Color background;
    private int arc;
    private float disabledOpacity;
    private Map<String, Color> variantForegrounds;

    /**
     * Creates a new {@code DwcProgressBarUI} instance for the given component.
     *
     * @param c the component (unused, required by L&F contract)
     * @return a new DwcProgressBarUI instance
     */
    public static ComponentUI createUI(JComponent c) {
        return new DwcProgressBarUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();

        foreground = UIManager.getColor("ProgressBar.foreground");
        background = UIManager.getColor("ProgressBar.background");
        arc = UIManager.getInt("ProgressBar.arc");

        // Disabled opacity
        Object opacityObj = UIManager.get("Component.disabledOpacity");
        if (opacityObj instanceof Number num) {
            disabledOpacity = num.floatValue();
        } else {
            disabledOpacity = 0.6f;
        }

        // Build variant foreground map
        variantForegrounds = new HashMap<>();
        String[] variants = {"success", "danger", "warning", "info"};
        for (String variant : variants) {
            Color c = UIManager.getColor("ProgressBar." + variant + ".foreground");
            if (c != null) {
                variantForegrounds.put(variant, c);
            }
        }
    }

    @Override
    protected void paintDeterminate(Graphics g, JComponent c) {
        JProgressBar pb = (JProgressBar) c;
        Insets insets = pb.getInsets();
        int x = insets.left;
        int y = insets.top;
        int width = pb.getWidth() - insets.left - insets.right;
        int height = pb.getHeight() - insets.top - insets.bottom;

        if (width <= 0 || height <= 0) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            if (!pb.isEnabled()) {
                StateColorResolver.paintWithOpacity(g2, disabledOpacity, () -> {
                    paintDeterminateContent(g2, pb, x, y, width, height);
                });
            } else {
                paintDeterminateContent(g2, pb, x, y, width, height);
            }
        } finally {
            g2.dispose();
        }
    }

    private void paintDeterminateContent(Graphics2D g2, JProgressBar pb,
                                          int x, int y, int width, int height) {
        Object[] saved = PaintUtils.setupPaintingHints(g2);

        // Paint track background
        Shape trackShape = PaintUtils.createRoundedShape(x, y, width, height, arc);
        g2.setColor(background);
        g2.fill(trackShape);

        // Paint fill bar
        int amountFull = getAmountFull(pb.getInsets(), width, height);
        if (amountFull > 0) {
            Color fillColor = resolveVariantColor(pb);
            g2.setColor(fillColor);

            // Create fill shape and clip to track for rounded corners
            Shape fillShape;
            if (pb.getOrientation() == JProgressBar.HORIZONTAL) {
                fillShape = PaintUtils.createRoundedShape(x, y, amountFull, height, arc);
            } else {
                fillShape = PaintUtils.createRoundedShape(x, y + height - amountFull,
                        width, amountFull, arc);
            }
            Area fillArea = new Area(fillShape);
            fillArea.intersect(new Area(trackShape));
            g2.fill(fillArea);
        }

        PaintUtils.restorePaintingHints(g2, saved);

        // Paint percentage text if requested
        if (pb.isStringPainted()) {
            paintString(g2, pb, x, y, width, height, amountFull);
        }
    }

    @Override
    protected void paintIndeterminate(Graphics g, JComponent c) {
        JProgressBar pb = (JProgressBar) c;
        Insets insets = pb.getInsets();
        int x = insets.left;
        int y = insets.top;
        int width = pb.getWidth() - insets.left - insets.right;
        int height = pb.getHeight() - insets.top - insets.bottom;

        if (width <= 0 || height <= 0) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            if (!pb.isEnabled()) {
                StateColorResolver.paintWithOpacity(g2, disabledOpacity, () -> {
                    paintIndeterminateContent(g2, pb, x, y, width, height);
                });
            } else {
                paintIndeterminateContent(g2, pb, x, y, width, height);
            }
        } finally {
            g2.dispose();
        }
    }

    private void paintIndeterminateContent(Graphics2D g2, JProgressBar pb,
                                            int x, int y, int width, int height) {
        Object[] saved = PaintUtils.setupPaintingHints(g2);

        // Paint track background
        Shape trackShape = PaintUtils.createRoundedShape(x, y, width, height, arc);
        g2.setColor(background);
        g2.fill(trackShape);

        // Paint bouncing bar
        Rectangle boxRect = new Rectangle();
        Rectangle box = getBox(boxRect);
        if (box != null) {
            Color fillColor = resolveVariantColor(pb);
            g2.setColor(fillColor);

            Shape bouncingShape = PaintUtils.createRoundedShape(
                    box.x, box.y, box.width, box.height, arc);
            Area bounceArea = new Area(bouncingShape);
            bounceArea.intersect(new Area(trackShape));
            g2.fill(bounceArea);
        }

        PaintUtils.restorePaintingHints(g2, saved);

        // Paint percentage text if requested
        if (pb.isStringPainted()) {
            paintString(g2, pb, x, y, width, height, 0);
        }
    }

    /**
     * Paints the percentage/status string centered on the progress bar.
     *
     * <p>Text color is chosen dynamically based on the dominant background area's
     * luminance: white text on dark fills (primary blue, danger red, success green)
     * and black text on light fills (warning amber, track gray). The dominant area
     * is whichever of the fill or track covers more than half the bar.</p>
     */
    private void paintString(Graphics2D g2, JProgressBar pb,
                              int x, int y, int width, int height, int amountFull) {
        String text = pb.getString();
        if (text == null || text.isEmpty()) {
            return;
        }

        FontMetrics fm = g2.getFontMetrics(pb.getFont());
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent();

        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - fm.getHeight()) / 2 + textHeight;

        g2.setFont(pb.getFont());

        // Contrast-aware text color: pick black or white based on the dominant
        // background area's luminance (fill if > 50%, track otherwise)
        Color fillColor = resolveVariantColor(pb);
        boolean fillDominant = (pb.getOrientation() == JProgressBar.HORIZONTAL)
                ? amountFull > width / 2
                : amountFull > height / 2;
        Color bgForContrast = fillDominant ? fillColor : background;
        g2.setColor(contrastTextColor(bgForContrast));

        g2.drawString(text, textX, textY);
    }

    /**
     * Returns {@link Color#BLACK} or {@link Color#WHITE} for maximum contrast
     * against the given background color, using W3C relative luminance.
     *
     * <p>The threshold of 0.4 ensures white text on medium-dark fills (primary
     * blue L~0.13, danger red, success green) and black text on light fills
     * (warning amber L~0.74, track gray).</p>
     *
     * @param bg the background color to contrast against
     * @return black if the background is light, white if the background is dark
     */
    private Color contrastTextColor(Color bg) {
        double r = linearize(bg.getRed() / 255.0);
        double g = linearize(bg.getGreen() / 255.0);
        double b = linearize(bg.getBlue() / 255.0);
        double luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;
        return luminance > 0.4 ? Color.BLACK : Color.WHITE;
    }

    /**
     * Linearizes an sRGB channel value (0.0 to 1.0) to linear RGB.
     *
     * <p>Applies the standard sRGB transfer function inverse: values at or below
     * 0.04045 are divided by 12.92; values above use the power curve
     * {@code ((v + 0.055) / 1.055) ^ 2.4}.</p>
     *
     * @param srgb the sRGB channel value in [0.0, 1.0]
     * @return the linearized value
     */
    private double linearize(double srgb) {
        return srgb <= 0.04045
                ? srgb / 12.92
                : Math.pow((srgb + 0.055) / 1.055, 2.4);
    }

    /**
     * Resolves the fill color based on the {@code dwc.progressType} client property.
     */
    private Color resolveVariantColor(JProgressBar pb) {
        Object prop = pb.getClientProperty("dwc.progressType");
        if (prop instanceof String variant && variantForegrounds.containsKey(variant)) {
            return variantForegrounds.get(variant);
        }
        return foreground;
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension d = super.getPreferredSize(c);
        if (d == null) {
            return null;
        }

        // Ensure minimum height of 8px for thin bar look
        JProgressBar pb = (JProgressBar) c;
        if (pb.getOrientation() == JProgressBar.HORIZONTAL) {
            d.height = Math.max(d.height, 8);
        } else {
            d.width = Math.max(d.width, 8);
        }
        return d;
    }
}
