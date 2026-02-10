package com.dwc.laf.ui;

import com.dwc.laf.DwcLookAndFeel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JScrollBar;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DwcScrollBarUI} verifying UI installation, per-component instances,
 * thin width, and paint pipeline smoke tests.
 */
class DwcScrollBarUITest {

    private LookAndFeel previousLaf;

    @BeforeEach
    void setUp() throws Exception {
        previousLaf = UIManager.getLookAndFeel();
        UIManager.setLookAndFeel(new DwcLookAndFeel());
    }

    @AfterEach
    void tearDown() throws Exception {
        UIManager.setLookAndFeel(previousLaf);
    }

    @Test
    void testUIInstalled() {
        JScrollBar sb = new JScrollBar();
        assertInstanceOf(DwcScrollBarUI.class, sb.getUI(),
                "JScrollBar should use DwcScrollBarUI when DwcLookAndFeel is active");
    }

    @Test
    void testPerComponentInstance() {
        JScrollBar sb1 = new JScrollBar();
        JScrollBar sb2 = new JScrollBar();
        assertNotSame(sb1.getUI(), sb2.getUI(),
                "Each JScrollBar should have its own DwcScrollBarUI instance");
    }

    @Test
    void testThinWidth() {
        JScrollBar sb = new JScrollBar(JScrollBar.VERTICAL);
        Dimension pref = sb.getPreferredSize();
        assertTrue(pref.width <= 10,
                "Vertical scrollbar preferred width should be <= 10px, but was " + pref.width);
    }

    @Test
    void testPaintNoError() {
        JScrollBar sb = new JScrollBar(JScrollBar.VERTICAL, 50, 10, 0, 100);
        sb.setSize(10, 200);

        BufferedImage img = new BufferedImage(10, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> sb.paint(g2),
                    "Painting vertical scrollbar should not throw");
        } finally {
            g2.dispose();
        }
    }

    @Test
    void testPaintHorizontalNoError() {
        JScrollBar sb = new JScrollBar(JScrollBar.HORIZONTAL, 50, 10, 0, 100);
        sb.setSize(200, 10);

        BufferedImage img = new BufferedImage(200, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> sb.paint(g2),
                    "Painting horizontal scrollbar should not throw");
        } finally {
            g2.dispose();
        }
    }

    @Test
    void testLargeRangeSmokeTest() {
        JScrollBar sb = new JScrollBar(JScrollBar.VERTICAL, 5000, 10, 0, 10000);
        sb.setSize(10, 200);

        BufferedImage img = new BufferedImage(10, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> sb.paint(g2),
                    "Painting scrollbar with large range should not throw");
        } finally {
            g2.dispose();
        }
    }
}
