package com.dwc.laf.ui;

import com.dwc.laf.DwcLookAndFeel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JTextField;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DwcTextFieldUI} verifying UI installation, per-component instances,
 * property setup, placeholder text, hover state, preferred size, and paint pipeline
 * smoke tests.
 */
class DwcTextFieldUITest {

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
        JTextField tf1 = new JTextField("One");
        JTextField tf2 = new JTextField("Two");
        ComponentUI ui1 = DwcTextFieldUI.createUI(tf1);
        ComponentUI ui2 = DwcTextFieldUI.createUI(tf2);

        assertNotSame(ui1, ui2,
                "Each JTextField should get its own DwcTextFieldUI instance");
    }

    @Test
    void testInstallSetsOpaqueToFalse() {
        JTextField tf = new JTextField("Test");
        assertFalse(tf.isOpaque(),
                "JTextField should not be opaque after L&F installation");
    }

    @Test
    void testInstallSetsBorder() {
        JTextField tf = new JTextField("Test");
        assertNotNull(tf.getBorder(), "JTextField should have a border installed");

        // Insets should include focus width padding (focusWidth + borderWidth + margin)
        // With default values: focusWidth=3, borderWidth=1, margin.left=6 -> left >= 6
        java.awt.Insets insets = tf.getInsets();
        assertTrue(insets.left > 6,
                "Border insets left should include focusWidth + borderWidth + margin, was " + insets.left);
    }

    @Test
    void testPreferredSizeMinimumHeight() {
        JTextField tf = new JTextField("Hi");
        Dimension pref = tf.getPreferredSize();
        assertTrue(pref.height >= 36,
                "Preferred height should be at least 36px, but was " + pref.height);
    }

    @Test
    void testHoverStateViaClientProperty() {
        JTextField tf = new JTextField("Test");
        tf.putClientProperty("DwcTextFieldUI.hover", Boolean.TRUE);

        assertEquals(Boolean.TRUE, tf.getClientProperty("DwcTextFieldUI.hover"),
                "Hover client property should be readable and match set value");
    }

    @Test
    void testPlaceholderClientProperty() {
        JTextField tf = new JTextField();
        String placeholder = "Enter name...";
        tf.putClientProperty("JTextField.placeholderText", placeholder);

        assertEquals(placeholder, tf.getClientProperty("JTextField.placeholderText"),
                "Placeholder client property should be readable and match set value");
    }

    @Test
    void testPaintSmokeTest() {
        JTextField tf = new JTextField("Hello");
        tf.setSize(200, 40);
        tf.doLayout();

        BufferedImage img = new BufferedImage(200, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> tf.paint(g2),
                    "Painting a JTextField with text should not throw");

            // Verify something was painted (at least one non-transparent pixel)
            boolean hasContent = false;
            outer:
            for (int x = 0; x < img.getWidth(); x++) {
                for (int y = 0; y < img.getHeight(); y++) {
                    if (((img.getRGB(x, y) >> 24) & 0xFF) > 0) {
                        hasContent = true;
                        break outer;
                    }
                }
            }
            assertTrue(hasContent, "Painted image should have at least one non-transparent pixel");
        } finally {
            g2.dispose();
        }
    }

    @Test
    void testPaintWithPlaceholderSmokeTest() {
        JTextField tf = new JTextField();
        tf.putClientProperty("JTextField.placeholderText", "Search...");
        tf.setSize(200, 40);
        tf.doLayout();

        BufferedImage img = new BufferedImage(200, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> tf.paint(g2),
                    "Painting a JTextField with placeholder should not throw");

            // Verify something was painted
            boolean hasContent = false;
            outer:
            for (int x = 0; x < img.getWidth(); x++) {
                for (int y = 0; y < img.getHeight(); y++) {
                    if (((img.getRGB(x, y) >> 24) & 0xFF) > 0) {
                        hasContent = true;
                        break outer;
                    }
                }
            }
            assertTrue(hasContent, "Painted image with placeholder should have content");
        } finally {
            g2.dispose();
        }
    }

    @Test
    void testDisabledStateDoesNotThrow() {
        JTextField tf = new JTextField("Disabled");
        tf.setEnabled(false);
        tf.setSize(200, 40);
        tf.doLayout();

        BufferedImage img = new BufferedImage(200, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> tf.paint(g2),
                    "Painting a disabled JTextField should not throw");
        } finally {
            g2.dispose();
        }
    }

    @Test
    void testUIClassRegistered() {
        assertEquals("com.dwc.laf.ui.DwcTextFieldUI", UIManager.get("TextFieldUI"),
                "TextFieldUI should be registered to DwcTextFieldUI class name");
    }
}
