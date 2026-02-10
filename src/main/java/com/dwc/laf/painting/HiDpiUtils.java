package com.dwc.laf.painting;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * HiDPI-aware utilities for scale detection and device-resolution image creation.
 *
 * <p>Scale factor is always read from the {@link Graphics2D} transform, never
 * from system properties or {@code Toolkit.getScreenResolution()}, which are
 * unreliable across platforms.</p>
 */
public final class HiDpiUtils {

    private HiDpiUtils() {
        // Non-instantiable utility class
    }

    /**
     * Returns the horizontal scale factor from the graphics context's transform.
     *
     * <p>Since Java 9, the JDK automatically applies a scale transform on HiDPI
     * displays. This method reads that transform to determine the device scale.</p>
     *
     * @param g the graphics context, or {@code null} for a default of 1.0
     * @return the scale factor (e.g., 1.0 for standard, 2.0 for Retina)
     */
    public static float getScaleFactor(Graphics2D g) {
        if (g == null) {
            return 1.0f;
        }
        return (float) g.getTransform().getScaleX();
    }

    /**
     * Creates a {@link BufferedImage} at device resolution for the given logical
     * dimensions. The image type is always {@code TYPE_INT_ARGB}.
     *
     * <p>For example, at 2x scale, a 100x50 logical image produces a 200x100
     * device-resolution image.</p>
     *
     * @param g             the graphics context (used to read the scale factor)
     * @param logicalWidth  the width in logical pixels
     * @param logicalHeight the height in logical pixels
     * @return a new ARGB image at device resolution; at least 1x1
     */
    public static BufferedImage createHiDpiImage(Graphics2D g,
            int logicalWidth, int logicalHeight) {
        if (logicalWidth <= 0 || logicalHeight <= 0) {
            return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        }
        float scale = getScaleFactor(g);
        int deviceWidth = Math.round(logicalWidth * scale);
        int deviceHeight = Math.round(logicalHeight * scale);
        return new BufferedImage(deviceWidth, deviceHeight, BufferedImage.TYPE_INT_ARGB);
    }
}
