package com.dwc.laf.ui;

import com.dwc.laf.DwcLookAndFeel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JCheckBox;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DwcCheckBoxUI} verifying UI installation, per-component instances,
 * property setup, icon installation, and paint pipeline smoke tests.
 */
class DwcCheckBoxUITest {

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
        JCheckBox cb1 = new JCheckBox("One");
        JCheckBox cb2 = new JCheckBox("Two");
        assertNotSame(cb1.getUI(), cb2.getUI(),
                "Each JCheckBox should have its own DwcCheckBoxUI instance");
    }

    @Test
    void testUIInstalled() {
        JCheckBox cb = new JCheckBox("Test");
        assertInstanceOf(DwcCheckBoxUI.class, cb.getUI(),
                "JCheckBox should use DwcCheckBoxUI when DwcLookAndFeel is active");
    }

    @Test
    void testRolloverEnabled() {
        JCheckBox cb = new JCheckBox("Test");
        assertTrue(cb.isRolloverEnabled(),
                "Rollover should be enabled for hover state tracking");
    }

    @Test
    void testNotOpaque() {
        JCheckBox cb = new JCheckBox("Test");
        assertFalse(cb.isOpaque(),
                "Checkbox should not be opaque (custom icon handles painting)");
    }

    @Test
    void testIconInstalled() {
        JCheckBox cb = new JCheckBox("Test");
        // The default icon is stored in the UI delegate (BasicRadioButtonUI.icon field),
        // not on the AbstractButton. Access it via getDefaultIcon().
        DwcCheckBoxUI ui = (DwcCheckBoxUI) cb.getUI();
        assertInstanceOf(DwcCheckBoxIcon.class, ui.getDefaultIcon(),
                "CheckBox default icon should be DwcCheckBoxIcon");
    }

    @Test
    void testIconDimensionsIncludeFocusWidth() {
        DwcCheckBoxIcon icon = new DwcCheckBoxIcon();
        int focusWidth = UIManager.getInt("Component.focusWidth");
        // Icon dimensions must include focus width on both sides
        assertTrue(icon.getIconWidth() > 16,
                "Icon width should be > 16 (includes focus ring space). Got: " + icon.getIconWidth());
        assertEquals(icon.getIconWidth(), 16 + focusWidth * 2,
                "Icon width should be ICON_SIZE + focusWidth * 2");
        assertEquals(icon.getIconHeight(), 16 + focusWidth * 2,
                "Icon height should be ICON_SIZE + focusWidth * 2");
    }

    @Test
    void testPaintNoError() {
        JCheckBox cb = new JCheckBox("Test");
        cb.setSize(200, 30);

        BufferedImage img = new BufferedImage(200, 30, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> cb.paint(g2),
                    "Painting unchecked checkbox should not throw");
        } finally {
            g2.dispose();
        }
    }

    @Test
    void testPaintSelectedNoError() {
        JCheckBox cb = new JCheckBox("Test");
        cb.setSelected(true);
        cb.setSize(200, 30);

        BufferedImage img = new BufferedImage(200, 30, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> cb.paint(g2),
                    "Painting selected checkbox should not throw");
        } finally {
            g2.dispose();
        }
    }

    @Test
    void testPaintDisabledNoError() {
        JCheckBox cb = new JCheckBox("Test");
        cb.setEnabled(false);
        cb.setSize(200, 30);

        BufferedImage img = new BufferedImage(200, 30, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> cb.paint(g2),
                    "Painting disabled checkbox should not throw");
        } finally {
            g2.dispose();
        }
    }
}
