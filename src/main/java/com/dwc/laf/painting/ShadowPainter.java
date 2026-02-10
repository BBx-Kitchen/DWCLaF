package com.dwc.laf.painting;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Paints box shadows behind components using Gaussian-blurred cached images.
 *
 * <p>Shadow images are created once and cached using {@link SoftReference}
 * entries in a {@link ConcurrentHashMap}, keyed by all visual parameters.
 * This allows the GC to reclaim shadow images under memory pressure while
 * avoiding redundant blur computation during normal operation.</p>
 *
 * <p>The Gaussian blur uses a two-pass separable {@link ConvolveOp} with
 * {@code EDGE_ZERO_FILL} for soft shadow edges that fade to transparent.</p>
 */
public final class ShadowPainter {

    /** Maximum blur radius to prevent excessive memory allocation. */
    private static final float MAX_BLUR_RADIUS = 50f;

    private ShadowPainter() {
        // Non-instantiable utility class
    }

    /**
     * Cache key containing all visual parameters that affect the shadow image.
     * Uses record auto-generated {@code hashCode} and {@code equals}.
     */
    record ShadowCacheKey(int width, int height, float arc,
                          float blurRadius, int shadowColorRgb) {
    }

    /** Thread-safe shadow image cache with memory-sensitive entries. */
    private static final ConcurrentHashMap<ShadowCacheKey, SoftReference<BufferedImage>> cache =
            new ConcurrentHashMap<>();

    /**
     * Paints a box shadow behind the specified component bounds.
     *
     * <p>The shadow is rendered as a blurred rounded shape, offset by
     * {@code offsetX}/{@code offsetY} from the component position. A cached
     * image is reused when all visual parameters match a previous call.</p>
     *
     * @param g           the graphics context
     * @param x           the component x coordinate
     * @param y           the component y coordinate
     * @param width       the component width
     * @param height      the component height
     * @param arc         the corner arc diameter
     * @param blurRadius  the Gaussian blur radius (clamped to 50px max)
     * @param offsetX     the horizontal shadow offset
     * @param offsetY     the vertical shadow offset
     * @param shadowColor the shadow color; if {@code null}, nothing is painted
     */
    public static void paintShadow(Graphics2D g, float x, float y,
            float width, float height, float arc, float blurRadius,
            float offsetX, float offsetY, Color shadowColor) {
        if (shadowColor == null || blurRadius <= 0) {
            return;
        }

        // Clamp blur radius to practical desktop limit
        blurRadius = Math.min(blurRadius, MAX_BLUR_RADIUS);

        // Padding: blur kernel extends ~3 sigma on each side
        int padding = (int) Math.ceil(blurRadius * 3);
        int imageWidth = (int) width + padding * 2;
        int imageHeight = (int) height + padding * 2;

        // Build cache key from all visual parameters
        ShadowCacheKey key = new ShadowCacheKey(
                imageWidth, imageHeight, arc, blurRadius, shadowColor.getRGB());

        // Check cache
        BufferedImage shadowImage = null;
        SoftReference<BufferedImage> ref = cache.get(key);
        if (ref != null) {
            shadowImage = ref.get();
        }

        // Cache miss: create and blur the shadow image
        if (shadowImage == null) {
            shadowImage = new BufferedImage(imageWidth, imageHeight,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D ig = shadowImage.createGraphics();
            try {
                ig.setColor(shadowColor);
                ig.fill(PaintUtils.createRoundedShape(
                        padding, padding, width, height, arc));
            } finally {
                ig.dispose();
            }

            shadowImage = applyGaussianBlur(shadowImage, blurRadius);
            cache.put(key, new SoftReference<>(shadowImage));
        }

        // Draw at logical dimensions; Graphics2D transform handles HiDPI scaling
        g.drawImage(shadowImage,
                (int) (x + offsetX - padding),
                (int) (y + offsetY - padding),
                imageWidth, imageHeight, null);
    }

    /**
     * Applies a two-pass separable Gaussian blur to the source image.
     *
     * <p>The kernel covers 3 sigma in each direction, and
     * {@link ConvolveOp#EDGE_ZERO_FILL} ensures edges fade to transparent
     * rather than wrapping or clamping.</p>
     *
     * @param src    the source image
     * @param radius the blur radius (sigma)
     * @return a new blurred image
     */
    static BufferedImage applyGaussianBlur(BufferedImage src, float radius) {
        if (radius <= 0) {
            return src;
        }

        int kernelSize = (int) Math.ceil(radius * 3) * 2 + 1;
        float[] kernelData = createGaussianKernel(kernelSize, radius);

        // Horizontal pass
        Kernel hKernel = new Kernel(kernelSize, 1, kernelData);
        ConvolveOp hOp = new ConvolveOp(hKernel, ConvolveOp.EDGE_ZERO_FILL, null);
        BufferedImage temp = hOp.filter(src, null);

        // Vertical pass
        Kernel vKernel = new Kernel(1, kernelSize, kernelData);
        ConvolveOp vOp = new ConvolveOp(vKernel, ConvolveOp.EDGE_ZERO_FILL, null);
        return vOp.filter(temp, null);
    }

    /**
     * Creates a normalized 1D Gaussian kernel.
     *
     * @param size  the kernel size (must be odd)
     * @param sigma the standard deviation
     * @return a float array of kernel weights summing to 1.0
     */
    private static float[] createGaussianKernel(int size, float sigma) {
        float[] data = new float[size];
        int center = size / 2;
        float sum = 0;

        for (int i = 0; i < size; i++) {
            float dist = i - center;
            data[i] = (float) Math.exp(-(dist * dist) / (2 * sigma * sigma));
            sum += data[i];
        }

        // Normalize
        for (int i = 0; i < size; i++) {
            data[i] /= sum;
        }

        return data;
    }

    /**
     * Clears the shadow image cache. Package-private for testing.
     */
    static void clearCache() {
        cache.clear();
    }
}
