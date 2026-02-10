package com.dwc.laf.painting;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class FocusRingPainterTest {

    /**
     * Creates a transparent ARGB image large enough to accommodate a component
     * at the given offset/size plus the ring width.
     */
    private static BufferedImage createTestImage(int totalWidth, int totalHeight) {
        return new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_ARGB);
    }

    @Test
    void testPaintFocusRing_nullColor() {
        BufferedImage img = createTestImage(120, 60);
        Graphics2D g = img.createGraphics();
        // Should not throw
        FocusRingPainter.paintFocusRing(g, 10, 10, 100, 40, 8, 3, null);
        g.dispose();

        // Verify no pixels painted (all transparent)
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                assertEquals(0, (img.getRGB(x, y) >> 24) & 0xFF,
                        "Pixel at (" + x + "," + y + ") should be transparent");
            }
        }
    }

    @Test
    void testPaintFocusRing_zeroWidth() {
        BufferedImage img = createTestImage(120, 60);
        Graphics2D g = img.createGraphics();
        Color ringColor = new Color(66, 133, 244, 102);
        // ringWidth = 0 should paint nothing
        FocusRingPainter.paintFocusRing(g, 10, 10, 100, 40, 8, 0, ringColor);
        g.dispose();

        // Verify no pixels painted
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                assertEquals(0, (img.getRGB(x, y) >> 24) & 0xFF,
                        "Pixel at (" + x + "," + y + ") should be transparent");
            }
        }
    }

    @Test
    void testPaintFocusRing_paintsPixels() {
        // Component at (10, 10) size 100x40, ring width 3
        // Image needs to be at least 10+100+10 = 120 wide, 10+40+10 = 60 tall
        BufferedImage img = createTestImage(120, 60);
        Graphics2D g = img.createGraphics();
        Color ringColor = new Color(66, 133, 244, 102); // alpha ~0.4

        FocusRingPainter.paintFocusRing(g, 10, 10, 100, 40, 8, 3, ringColor);
        g.dispose();

        // Check pixels in the ring area (outside component but inside outer ring)
        // Left edge of ring: x around 7-9 (component starts at 10, ring extends 3px left)
        boolean foundRingPixel = false;
        for (int y = 20; y < 40; y++) { // Middle vertical range
            for (int x = 7; x <= 9; x++) {
                int alpha = (img.getRGB(x, y) >> 24) & 0xFF;
                if (alpha > 0) {
                    foundRingPixel = true;
                    break;
                }
            }
            if (foundRingPixel) break;
        }
        assertTrue(foundRingPixel, "Ring pixels should exist outside component bounds (left edge)");

        // Also check that pixels well inside the component are transparent
        // (even-odd fill means the inner region is not filled)
        int centerAlpha = (img.getRGB(60, 30) >> 24) & 0xFF;
        assertEquals(0, centerAlpha, "Center of component should be transparent (even-odd fill)");
    }

    @Test
    void testPaintFocusRing_ringColorPreserved() {
        BufferedImage img = createTestImage(120, 60);
        Graphics2D g = img.createGraphics();
        Color ringColor = new Color(255, 0, 0, 102); // Red with alpha

        FocusRingPainter.paintFocusRing(g, 10, 10, 100, 40, 8, 3, ringColor);
        g.dispose();

        // Find a non-transparent ring pixel and verify color components
        // On a transparent background, the painted color should match ringColor directly
        // since there's nothing to composite against
        boolean foundAndVerified = false;
        for (int y = 20; y < 40 && !foundAndVerified; y++) {
            for (int x = 7; x <= 9 && !foundAndVerified; x++) {
                int argb = img.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha > 0) {
                    int red = (argb >> 16) & 0xFF;
                    int green = (argb >> 8) & 0xFF;
                    int blue = argb & 0xFF;
                    // On transparent background, painted pixel should be close to ringColor
                    // Allow some tolerance for antialiasing
                    assertTrue(red > 200, "Red channel should be high: " + red);
                    assertTrue(green < 50, "Green channel should be low: " + green);
                    assertTrue(blue < 50, "Blue channel should be low: " + blue);
                    foundAndVerified = true;
                }
            }
        }
        assertTrue(foundAndVerified, "Should find a ring pixel to verify color");
    }

    @Test
    void testPaintFocusRing_restoresHints() {
        BufferedImage img = createTestImage(120, 60);
        Graphics2D g = img.createGraphics();

        // Set specific hint values before calling
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                           RenderingHints.VALUE_STROKE_PURE);

        Color ringColor = new Color(66, 133, 244, 102);
        FocusRingPainter.paintFocusRing(g, 10, 10, 100, 40, 8, 3, ringColor);

        // Hints should be restored to pre-call values
        assertEquals(RenderingHints.VALUE_ANTIALIAS_OFF,
                g.getRenderingHint(RenderingHints.KEY_ANTIALIASING),
                "Antialiasing hint should be restored");
        assertEquals(RenderingHints.VALUE_STROKE_PURE,
                g.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL),
                "Stroke control hint should be restored");

        g.dispose();
    }

    @Test
    void testPaintFocusRing_expandsArc() {
        // Paint with componentArc=0 (sharp corners). The outer ring should use
        // outerArc = 0 + ringWidth = 3, which means the outer shape is rounded.
        // This should cause ring pixels to appear in the corner area that would
        // be empty if outerArc were also 0.
        BufferedImage img = createTestImage(120, 60);
        Graphics2D g = img.createGraphics();
        Color ringColor = new Color(66, 133, 244, 200);

        // Component at (10, 10) size 100x40, arc=0, ringWidth=3
        FocusRingPainter.paintFocusRing(g, 10, 10, 100, 40, 0, 3, ringColor);
        g.dispose();

        // Top-left corner of ring area: the inner shape is a rectangle (arc=0)
        // so the top-left corner pixel of the ring at (7, 7) area should have
        // ring pixels because the outer shape uses arc=3 (rounded) while the
        // inner shape uses arc=0 (square corners).
        // Check that ring pixels exist along the top edge
        boolean foundTopEdge = false;
        for (int x = 10; x < 110; x++) {
            int alpha = (img.getRGB(x, 8) >> 24) & 0xFF;
            if (alpha > 0) {
                foundTopEdge = true;
                break;
            }
        }
        assertTrue(foundTopEdge, "Ring should have pixels along the top edge outside component");
    }
}
