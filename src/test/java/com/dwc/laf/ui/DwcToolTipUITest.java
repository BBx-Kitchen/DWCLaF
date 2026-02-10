package com.dwc.laf.ui;

import com.dwc.laf.DwcLookAndFeel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JToolTip;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DwcToolTipUI} verifying UI installation, per-component instances,
 * border setup, insets, and paint pipeline smoke test.
 */
class DwcToolTipUITest {

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
        JToolTip tooltip = new JToolTip();
        assertInstanceOf(DwcToolTipUI.class, tooltip.getUI(),
                "JToolTip should use DwcToolTipUI when DwcLookAndFeel is active");
    }

    @Test
    void testPerComponentInstance() {
        JToolTip tooltip1 = new JToolTip();
        JToolTip tooltip2 = new JToolTip();
        assertNotSame(tooltip1.getUI(), tooltip2.getUI(),
                "Each JToolTip should have its own DwcToolTipUI instance");
    }

    @Test
    void testNotOpaque() {
        JToolTip tooltip = new JToolTip();
        assertFalse(tooltip.isOpaque(),
                "Tooltip should not be opaque (custom painting handles background)");
    }

    @Test
    void testBorderInstalled() {
        JToolTip tooltip = new JToolTip();
        assertInstanceOf(DwcToolTipBorder.class, tooltip.getBorder(),
                "Tooltip border should be DwcToolTipBorder");
    }

    @Test
    void testBorderInsets() {
        JToolTip tooltip = new JToolTip();
        Insets insets = tooltip.getBorder().getBorderInsets(tooltip);
        int shadowSize = DwcToolTipBorder.SHADOW_SIZE;
        assertTrue(insets.top >= shadowSize,
                "Top inset should account for shadow, was " + insets.top);
        assertTrue(insets.left >= shadowSize,
                "Left inset should account for shadow, was " + insets.left);
        assertTrue(insets.bottom >= shadowSize,
                "Bottom inset should account for shadow, was " + insets.bottom);
        assertTrue(insets.right >= shadowSize,
                "Right inset should account for shadow, was " + insets.right);
    }

    @Test
    void testPaintNoError() {
        JToolTip tooltip = new JToolTip();
        tooltip.setTipText("Hello World");
        tooltip.setSize(200, 40);

        BufferedImage img = new BufferedImage(200, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> tooltip.paint(g2),
                    "Painting tooltip should not throw");
        } finally {
            g2.dispose();
        }
    }
}
