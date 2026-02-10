package com.dwc.laf.ui;

import com.dwc.laf.DwcLookAndFeel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JRadioButton;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DwcRadioButtonUI} verifying UI installation, per-component instances,
 * property setup, icon installation, and paint pipeline smoke tests.
 */
class DwcRadioButtonUITest {

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
    void testCreateUIReturnsPerComponentInstances() {
        JRadioButton rb1 = new JRadioButton("One");
        JRadioButton rb2 = new JRadioButton("Two");
        assertNotSame(rb1.getUI(), rb2.getUI(),
                "Each JRadioButton should have its own DwcRadioButtonUI instance");
    }

    @Test
    void testUIInstalled() {
        JRadioButton rb = new JRadioButton("Test");
        assertInstanceOf(DwcRadioButtonUI.class, rb.getUI(),
                "JRadioButton should use DwcRadioButtonUI when DwcLookAndFeel is active");
    }

    @Test
    void testRolloverEnabled() {
        JRadioButton rb = new JRadioButton("Test");
        assertTrue(rb.isRolloverEnabled(),
                "Rollover should be enabled for hover state tracking");
    }

    @Test
    void testNotOpaque() {
        JRadioButton rb = new JRadioButton("Test");
        assertFalse(rb.isOpaque(),
                "Radio button should not be opaque (custom icon handles painting)");
    }

    @Test
    void testIconInstalled() {
        JRadioButton rb = new JRadioButton("Test");
        // The default icon is stored in the UI delegate (BasicRadioButtonUI.icon field),
        // not on the AbstractButton. Access it via getDefaultIcon().
        DwcRadioButtonUI ui = (DwcRadioButtonUI) rb.getUI();
        assertInstanceOf(DwcRadioButtonIcon.class, ui.getDefaultIcon(),
                "RadioButton default icon should be DwcRadioButtonIcon");
    }

    @Test
    void testIconDimensionsIncludeFocusWidth() {
        DwcRadioButtonIcon icon = new DwcRadioButtonIcon();
        int focusWidth = UIManager.getInt("Component.focusWidth");
        assertTrue(icon.getIconWidth() > 16,
                "Icon width should be > 16 (includes focus ring space). Got: " + icon.getIconWidth());
        assertEquals(icon.getIconWidth(), 16 + focusWidth * 2,
                "Icon width should be ICON_SIZE + focusWidth * 2");
        assertEquals(icon.getIconHeight(), 16 + focusWidth * 2,
                "Icon height should be ICON_SIZE + focusWidth * 2");
    }

    @Test
    void testPaintNoError() {
        JRadioButton rb = new JRadioButton("Test");
        rb.setSize(200, 30);

        BufferedImage img = new BufferedImage(200, 30, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> rb.paint(g2),
                    "Painting unselected radio button should not throw");
        } finally {
            g2.dispose();
        }
    }

    @Test
    void testPaintSelectedNoError() {
        JRadioButton rb = new JRadioButton("Test");
        rb.setSelected(true);
        rb.setSize(200, 30);

        BufferedImage img = new BufferedImage(200, 30, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> rb.paint(g2),
                    "Painting selected radio button should not throw");
        } finally {
            g2.dispose();
        }
    }

    @Test
    void testPaintDisabledNoError() {
        JRadioButton rb = new JRadioButton("Test");
        rb.setEnabled(false);
        rb.setSize(200, 30);

        BufferedImage img = new BufferedImage(200, 30, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> rb.paint(g2),
                    "Painting disabled radio button should not throw");
        } finally {
            g2.dispose();
        }
    }
}
