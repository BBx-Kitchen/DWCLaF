package com.dwc.laf.painting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class ShadowPainterTest {

    @AfterEach
    void tearDown() {
        ShadowPainter.clearCache();
    }

    @Test
    void testPaintShadow_nullColor() {
        BufferedImage img = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        // Should not throw
        assertDoesNotThrow(() ->
                ShadowPainter.paintShadow(g, 50, 30, 100, 40, 8, 5, 2, 2, null));
        g.dispose();
    }

    @Test
    void testPaintShadow_zeroBlur() {
        BufferedImage img = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Color shadowColor = new Color(0, 0, 0, 80);

        ShadowPainter.paintShadow(g, 50, 30, 100, 40, 8, 0, 2, 2, shadowColor);
        g.dispose();

        // Verify no pixels painted (blurRadius=0 should be a no-op)
        boolean anyPainted = false;
        for (int x = 0; x < img.getWidth() && !anyPainted; x++) {
            for (int y = 0; y < img.getHeight() && !anyPainted; y++) {
                if (((img.getRGB(x, y) >> 24) & 0xFF) > 0) {
                    anyPainted = true;
                }
            }
        }
        assertFalse(anyPainted, "No shadow pixels should be painted with zero blur radius");
    }

    @Test
    void testPaintShadow_paintsSoftPixels() {
        // Large enough image to hold shadow with blur spread
        BufferedImage img = new BufferedImage(250, 150, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Color shadowColor = new Color(0, 0, 0, 128);

        // Component at (50, 30), size 100x40, blur 5, offset (2, 2)
        ShadowPainter.paintShadow(g, 50, 30, 100, 40, 8, 5, 2, 2, shadowColor);
        g.dispose();

        // The shadow image should produce non-transparent pixels somewhere on the canvas.
        // Scan the entire image for any non-transparent pixel.
        assertTrue(hasNonTransparentPixel(img),
                "Shadow should paint non-transparent pixels on the canvas");

        // Verify that the center of the shadow region has visible alpha.
        // Shadow image drawn at (37, 17), size 130x70.
        // Center of shadow region on canvas: approximately (37+65, 17+35) = (102, 52)
        int centerAlpha = (img.getRGB(102, 52) >> 24) & 0xFF;
        assertTrue(centerAlpha > 0,
                "Center of shadow region should have non-zero alpha, got: " + centerAlpha);
    }

    @Test
    void testPaintShadow_caching() {
        BufferedImage img = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Color shadowColor = new Color(0, 0, 0, 80);

        // First call: cache miss, creates image
        ShadowPainter.paintShadow(g, 50, 30, 100, 40, 8, 5, 2, 2, shadowColor);

        // Clear the target image and paint again with same parameters
        BufferedImage img2 = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img2.createGraphics();
        ShadowPainter.paintShadow(g2, 50, 30, 100, 40, 8, 5, 2, 2, shadowColor);

        g.dispose();
        g2.dispose();

        // Both images should produce same visual result (cache hit on second call)
        // Verify by comparing a sample pixel
        int pixel1 = img.getRGB(100, 50);
        int pixel2 = img2.getRGB(100, 50);
        assertEquals(pixel1, pixel2, "Cached shadow should produce identical pixels");

        // After clearCache, a new call should still work (new cache entry created)
        ShadowPainter.clearCache();
        BufferedImage img3 = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g3 = img3.createGraphics();
        ShadowPainter.paintShadow(g3, 50, 30, 100, 40, 8, 5, 2, 2, shadowColor);
        g3.dispose();

        int pixel3 = img3.getRGB(100, 50);
        assertEquals(pixel1, pixel3, "Shadow after cache clear should match original");
    }

    @Test
    void testPaintShadow_cacheMissOnDifferentSize() {
        BufferedImage img = new BufferedImage(250, 150, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Color shadowColor = new Color(0, 0, 0, 80);

        // Paint with width=100
        ShadowPainter.paintShadow(g, 50, 30, 100, 40, 8, 5, 2, 2, shadowColor);

        // Paint with width=120 (different cache key)
        BufferedImage img2 = new BufferedImage(250, 150, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img2.createGraphics();
        ShadowPainter.paintShadow(g2, 50, 30, 120, 40, 8, 5, 2, 2, shadowColor);

        g.dispose();
        g2.dispose();

        // Both should produce some shadow pixels (both are valid shadows)
        boolean found1 = hasNonTransparentPixel(img);
        boolean found2 = hasNonTransparentPixel(img2);
        assertTrue(found1, "First shadow should have pixels");
        assertTrue(found2, "Second shadow (different size) should have pixels");
    }

    @Test
    void testPaintShadow_blurRadiusClamped() {
        BufferedImage img = new BufferedImage(300, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Color shadowColor = new Color(0, 0, 0, 80);

        // Extreme blur radius should be clamped to 50 and not OOM
        assertDoesNotThrow(() ->
                ShadowPainter.paintShadow(g, 50, 50, 100, 40, 8, 200, 0, 0, shadowColor));
        g.dispose();

        // Should still paint something
        assertTrue(hasNonTransparentPixel(img), "Clamped shadow should still paint pixels");
    }

    @Test
    void testGaussianBlur_producesBlurredImage() {
        // Create image with single opaque pixel in center
        int size = 51;
        BufferedImage src = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        int center = size / 2;
        src.setRGB(center, center, 0xFF000000); // Opaque black pixel

        BufferedImage blurred = ShadowPainter.applyGaussianBlur(src, 5f);

        // The center pixel should still have non-zero alpha
        int centerAlpha = (blurred.getRGB(center, center) >> 24) & 0xFF;
        assertTrue(centerAlpha > 0, "Center should still have alpha after blur");

        // Surrounding pixels should now have non-zero alpha (blur spread)
        boolean foundSpread = false;
        for (int dx = -3; dx <= 3 && !foundSpread; dx++) {
            for (int dy = -3; dy <= 3 && !foundSpread; dy++) {
                if (dx == 0 && dy == 0) continue;
                int alpha = (blurred.getRGB(center + dx, center + dy) >> 24) & 0xFF;
                if (alpha > 0) {
                    foundSpread = true;
                }
            }
        }
        assertTrue(foundSpread, "Blur should spread color to surrounding pixels");
    }

    @Test
    void testGaussianBlur_zeroRadius() {
        BufferedImage src = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        src.setRGB(5, 5, 0xFFFF0000); // Red pixel

        BufferedImage result = ShadowPainter.applyGaussianBlur(src, 0f);

        // Should return original image unchanged
        assertSame(src, result, "Zero radius should return same image instance");
        assertEquals(0xFFFF0000, result.getRGB(5, 5), "Pixel should be unchanged");
    }

    /**
     * Checks whether any pixel in the image has non-zero alpha.
     */
    private static boolean hasNonTransparentPixel(BufferedImage img) {
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                if (((img.getRGB(x, y) >> 24) & 0xFF) > 0) {
                    return true;
                }
            }
        }
        return false;
    }
}
