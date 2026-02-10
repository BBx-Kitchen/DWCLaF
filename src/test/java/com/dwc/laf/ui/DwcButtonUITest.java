package com.dwc.laf.ui;

import com.dwc.laf.DwcLookAndFeel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DwcButtonUI} verifying UI installation, per-component instances,
 * property setup, preferred size enforcement, primary variant support, and paint
 * pipeline smoke tests.
 */
class DwcButtonUITest {

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
        JButton button = new JButton("Test");
        assertInstanceOf(DwcButtonUI.class, button.getUI(),
                "JButton should use DwcButtonUI when DwcLookAndFeel is active");
    }

    @Test
    void testPerComponentInstance() {
        JButton button1 = new JButton("One");
        JButton button2 = new JButton("Two");
        assertNotSame(button1.getUI(), button2.getUI(),
                "Each JButton should have its own DwcButtonUI instance");
    }

    @Test
    void testRolloverEnabled() {
        JButton button = new JButton("Test");
        assertTrue(button.isRolloverEnabled(),
                "Rollover should be enabled for hover state tracking");
    }

    @Test
    void testNotOpaque() {
        JButton button = new JButton("Test");
        assertFalse(button.isOpaque(),
                "Button should not be opaque (custom painting handles background)");
    }

    @Test
    void testBorderInstalled() {
        JButton button = new JButton("Test");
        assertInstanceOf(DwcButtonBorder.class, button.getBorder(),
                "Button border should be DwcButtonBorder");
    }

    @Test
    void testPreferredSizeMinimumWidth() {
        JButton button = new JButton("OK");
        Dimension pref = button.getPreferredSize();
        assertTrue(pref.width >= 72,
                "Preferred width should be at least 72px, but was " + pref.width);
    }

    @Test
    void testPreferredSizeMinimumHeight() {
        JButton button = new JButton("OK");
        Dimension pref = button.getPreferredSize();
        assertTrue(pref.height >= 36,
                "Preferred height should be at least 36px, but was " + pref.height);
    }

    @Test
    void testPrimaryVariant() {
        JButton button = new JButton("Primary");
        button.putClientProperty("dwc.buttonType", "primary");
        button.setSize(200, 40);

        // Smoke test: paint to BufferedImage should not throw
        BufferedImage img = new BufferedImage(200, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> button.paint(g2),
                    "Painting primary variant button should not throw");
        } finally {
            g2.dispose();
        }
    }

    @Test
    void testPaintNoError() {
        JButton button = new JButton("Test");
        button.setSize(200, 40);

        BufferedImage img = new BufferedImage(200, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> button.paint(g2),
                    "Painting enabled button should not throw");
        } finally {
            g2.dispose();
        }
    }

    @Test
    void testDisabledPaintNoError() {
        JButton button = new JButton("Test");
        button.setEnabled(false);
        button.setSize(200, 40);

        BufferedImage img = new BufferedImage(200, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> button.paint(g2),
                    "Painting disabled button should not throw");
        } finally {
            g2.dispose();
        }
    }
}
