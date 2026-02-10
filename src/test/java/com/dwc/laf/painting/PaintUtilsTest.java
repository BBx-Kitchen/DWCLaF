package com.dwc.laf.painting;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class PaintUtilsTest {

    private Graphics2D createGraphics() {
        return new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB).createGraphics();
    }

    @Test
    void testSetupAndRestoreHints() {
        Graphics2D g = createGraphics();

        // Record original hints
        Object originalAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Object originalSC = g.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL);

        // Setup should change hints
        Object[] saved = PaintUtils.setupPaintingHints(g);
        assertEquals(RenderingHints.VALUE_ANTIALIAS_ON,
                g.getRenderingHint(RenderingHints.KEY_ANTIALIASING));
        assertEquals(RenderingHints.VALUE_STROKE_NORMALIZE,
                g.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL));

        // Restore should bring them back
        PaintUtils.restorePaintingHints(g, saved);
        assertEquals(originalAA, g.getRenderingHint(RenderingHints.KEY_ANTIALIASING));
        assertEquals(originalSC, g.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL));

        g.dispose();
    }

    @Test
    void testCreateRoundedShape_zeroArc() {
        Shape shape = PaintUtils.createRoundedShape(0, 0, 50, 30, 0);
        assertInstanceOf(Rectangle2D.Float.class, shape);
    }

    @Test
    void testCreateRoundedShape_negativeArc() {
        Shape shape = PaintUtils.createRoundedShape(0, 0, 50, 30, -5);
        assertInstanceOf(Rectangle2D.Float.class, shape);
    }

    @Test
    void testCreateRoundedShape_normalArc() {
        Shape shape = PaintUtils.createRoundedShape(0, 0, 50, 30, 10);
        assertInstanceOf(RoundRectangle2D.Float.class, shape);
    }

    @Test
    void testCreateRoundedShape_circleDegeneration() {
        // w == h and arc >= min(w, h) => Ellipse2D
        Shape shape = PaintUtils.createRoundedShape(0, 0, 40, 40, 40);
        assertInstanceOf(Ellipse2D.Float.class, shape);
    }

    @Test
    void testCreateRoundedShape_circleDegeneration_arcExceeds() {
        // w == h and arc > min(w, h) => still Ellipse2D
        Shape shape = PaintUtils.createRoundedShape(0, 0, 40, 40, 100);
        assertInstanceOf(Ellipse2D.Float.class, shape);
    }

    @Test
    void testCreateRoundedShape_arcClamping() {
        // arc > w but w != h, so it is a RoundRectangle2D with clamped arc
        Shape shape = PaintUtils.createRoundedShape(5, 5, 30, 50, 100);
        assertInstanceOf(RoundRectangle2D.Float.class, shape);
        // Verify bounds are correct (arc clamping does not affect bounds)
        Rectangle2D bounds = shape.getBounds2D();
        assertEquals(5, bounds.getX(), 0.01);
        assertEquals(5, bounds.getY(), 0.01);
        assertEquals(30, bounds.getWidth(), 0.01);
        assertEquals(50, bounds.getHeight(), 0.01);
    }

    @Test
    void testPaintOutline_createsEvenOddPath() {
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.RED);

        // Should not throw
        assertDoesNotThrow(() ->
                PaintUtils.paintOutline(g, 10, 10, 80, 80, 2, 8));

        // Verify that some pixels were painted (outline ring area)
        boolean foundPainted = false;
        for (int x = 10; x < 90 && !foundPainted; x++) {
            for (int y = 10; y < 90 && !foundPainted; y++) {
                if ((img.getRGB(x, y) & 0x00FF0000) != 0) {
                    foundPainted = true;
                }
            }
        }
        assertTrue(foundPainted, "Outline should paint some red pixels");

        g.dispose();
    }

    @Test
    void testPaintRoundedBackground_nullColor() {
        BufferedImage img = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Should not throw and should not paint anything
        assertDoesNotThrow(() ->
                PaintUtils.paintRoundedBackground(g, 0, 0, 50, 50, 5, null));

        // Verify no pixels were painted (all transparent)
        for (int x = 0; x < 50; x++) {
            for (int y = 0; y < 50; y++) {
                assertEquals(0, img.getRGB(x, y),
                        "No pixels should be painted with null color");
            }
        }

        g.dispose();
    }

    @Test
    void testPaintRoundedBackground_paintsPixels() {
        BufferedImage img = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        PaintUtils.paintRoundedBackground(g, 0, 0, 50, 50, 5, Color.BLUE);

        // Verify center pixel is blue
        int centerRgb = img.getRGB(25, 25);
        int blue = centerRgb & 0x000000FF;
        int alpha = (centerRgb >> 24) & 0xFF;
        assertTrue(blue > 200, "Center pixel should have strong blue component");
        assertTrue(alpha > 200, "Center pixel should be opaque");

        g.dispose();
    }
}
