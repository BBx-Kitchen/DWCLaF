package com.dwc.laf.painting;

import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class HiDpiUtilsTest {

    private Graphics2D createGraphics() {
        return new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB).createGraphics();
    }

    @Test
    void testGetScaleFactor_defaultTransform() {
        Graphics2D g = createGraphics();
        assertEquals(1.0f, HiDpiUtils.getScaleFactor(g), 0.001f);
        g.dispose();
    }

    @Test
    void testGetScaleFactor_scaled() {
        Graphics2D g = createGraphics();
        g.setTransform(AffineTransform.getScaleInstance(2.0, 2.0));
        assertEquals(2.0f, HiDpiUtils.getScaleFactor(g), 0.001f);
        g.dispose();
    }

    @Test
    void testGetScaleFactor_nullGraphics() {
        assertEquals(1.0f, HiDpiUtils.getScaleFactor(null), 0.001f);
    }

    @Test
    void testCreateHiDpiImage_1x() {
        Graphics2D g = createGraphics();
        BufferedImage img = HiDpiUtils.createHiDpiImage(g, 80, 40);
        assertEquals(80, img.getWidth());
        assertEquals(40, img.getHeight());
        g.dispose();
    }

    @Test
    void testCreateHiDpiImage_2x() {
        Graphics2D g = createGraphics();
        g.setTransform(AffineTransform.getScaleInstance(2.0, 2.0));
        BufferedImage img = HiDpiUtils.createHiDpiImage(g, 80, 40);
        assertEquals(160, img.getWidth());
        assertEquals(80, img.getHeight());
        g.dispose();
    }

    @Test
    void testCreateHiDpiImage_typeArgb() {
        Graphics2D g = createGraphics();
        BufferedImage img = HiDpiUtils.createHiDpiImage(g, 50, 50);
        assertEquals(BufferedImage.TYPE_INT_ARGB, img.getType());
        g.dispose();
    }

    @Test
    void testCreateHiDpiImage_zeroDimension() {
        Graphics2D g = createGraphics();
        BufferedImage img = HiDpiUtils.createHiDpiImage(g, 0, 50);
        assertEquals(1, img.getWidth());
        assertEquals(1, img.getHeight());

        BufferedImage img2 = HiDpiUtils.createHiDpiImage(g, 50, -10);
        assertEquals(1, img2.getWidth());
        assertEquals(1, img2.getHeight());

        g.dispose();
    }
}
