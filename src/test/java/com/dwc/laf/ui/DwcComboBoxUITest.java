package com.dwc.laf.ui;

import com.dwc.laf.DwcLookAndFeel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicArrowButton;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DwcComboBoxUI} verifying per-component instances, UI installation,
 * opacity, border type, custom renderer, custom arrow button, paint smoke tests,
 * minimum height, and hover client property tracking.
 */
class DwcComboBoxUITest {

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
    void createUI_returnsPerComponentInstances() {
        JComboBox<String> cb1 = new JComboBox<>();
        JComboBox<String> cb2 = new JComboBox<>();
        ComponentUI ui1 = DwcComboBoxUI.createUI(cb1);
        ComponentUI ui2 = DwcComboBoxUI.createUI(cb2);

        assertNotSame(ui1, ui2,
                "Each JComboBox should get its own DwcComboBoxUI instance");
    }

    @Test
    void comboBox_hasCorrectUI() {
        JComboBox<String> cb = new JComboBox<>();
        assertInstanceOf(DwcComboBoxUI.class, cb.getUI(),
                "JComboBox should have DwcComboBoxUI installed");
    }

    @Test
    void comboBox_isNotOpaque() {
        JComboBox<String> cb = new JComboBox<>();
        assertFalse(cb.isOpaque(),
                "JComboBox should not be opaque after L&F installation");
    }

    @Test
    void comboBox_hasDwcTextFieldBorder() {
        JComboBox<String> cb = new JComboBox<>();
        assertNotNull(cb.getBorder(), "JComboBox should have a border installed");

        // The border may be wrapped in a BorderUIResource, so check the underlying type
        javax.swing.border.Border border = cb.getBorder();
        if (border instanceof javax.swing.plaf.BorderUIResource bur) {
            // BorderUIResource wraps another border; check via class name since
            // getBorderInsets delegates correctly regardless
            // We verify the insets include focusWidth (characteristic of DwcTextFieldBorder)
            java.awt.Insets insets = bur.getBorderInsets(cb);
            assertTrue(insets.left > 6,
                    "Border insets should include focusWidth, was " + insets.left);
        } else {
            assertInstanceOf(DwcTextFieldBorder.class, border,
                    "Border should be DwcTextFieldBorder");
        }
    }

    @Test
    void comboBox_hasCustomRenderer() {
        JComboBox<String> cb = new JComboBox<>(new String[]{"A", "B"});
        assertNotNull(cb.getRenderer(), "Renderer should not be null");

        // The renderer should be the custom DwcComboBoxRenderer (private inner class)
        // Verify by checking it's not the default Metal/Basic renderer class name
        String rendererClassName = cb.getRenderer().getClass().getName();
        assertTrue(rendererClassName.contains("DwcComboBoxRenderer"),
                "Renderer should be DwcComboBoxRenderer, but was " + rendererClassName);
    }

    @Test
    void arrowButton_isCustom() {
        JComboBox<String> cb = new JComboBox<>(new String[]{"A", "B"});
        cb.setSize(200, 40);
        cb.doLayout();

        // The arrow button is typically the first component added by BasicComboBoxUI
        Component arrowButton = null;
        for (int i = 0; i < cb.getComponentCount(); i++) {
            Component child = cb.getComponent(i);
            if (child instanceof javax.swing.JButton) {
                arrowButton = child;
                break;
            }
        }

        assertNotNull(arrowButton, "ComboBox should have an arrow button component");
        assertFalse(arrowButton instanceof BasicArrowButton,
                "Arrow button should NOT be a BasicArrowButton");
        assertEquals("ComboBox.arrowButton", arrowButton.getName(),
                "Arrow button should be named 'ComboBox.arrowButton'");
    }

    @Test
    void paintSmokeTest() {
        JComboBox<String> cb = new JComboBox<>(new String[]{"Alpha", "Beta", "Gamma"});
        cb.setSize(200, 40);
        cb.doLayout();

        BufferedImage img = new BufferedImage(200, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> cb.paint(g2),
                    "Painting a JComboBox should not throw");

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
            assertTrue(hasContent, "Painted image should have at least one non-transparent pixel");
        } finally {
            g2.dispose();
        }
    }

    @Test
    void paintSmokeTest_withSelectedItem() {
        JComboBox<String> cb = new JComboBox<>(new String[]{"Alpha", "Beta", "Gamma"});
        cb.setSelectedIndex(1);
        cb.setSize(200, 40);
        cb.doLayout();

        BufferedImage img = new BufferedImage(200, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> cb.paint(g2),
                    "Painting a JComboBox with selected item should not throw");

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
            assertTrue(hasContent, "Painted image with selected item should have content");
        } finally {
            g2.dispose();
        }
    }

    @Test
    void preferredSize_hasMinimumHeight() {
        JComboBox<String> cb = new JComboBox<>(new String[]{"Short"});
        Dimension pref = cb.getPreferredSize();
        assertTrue(pref.height >= 36,
                "Preferred height should be at least 36px, but was " + pref.height);
    }

    @Test
    void comboBox_hoverClientProperty() {
        JComboBox<String> cb = new JComboBox<>(new String[]{"A", "B"});
        cb.setSize(200, 40);
        cb.doLayout();

        // Verify initial state: no hover
        assertNull(cb.getClientProperty("DwcTextFieldUI.hover"),
                "Hover should be null initially");

        // Simulate mouse entered
        MouseEvent entered = new MouseEvent(cb, MouseEvent.MOUSE_ENTERED,
                System.currentTimeMillis(), 0, 10, 10, 0, false);
        cb.dispatchEvent(entered);
        assertEquals(Boolean.TRUE, cb.getClientProperty("DwcTextFieldUI.hover"),
                "Hover should be TRUE after MOUSE_ENTERED");

        // Simulate mouse exited
        MouseEvent exited = new MouseEvent(cb, MouseEvent.MOUSE_EXITED,
                System.currentTimeMillis(), 0, -1, -1, 0, false);
        cb.dispatchEvent(exited);
        assertNull(cb.getClientProperty("DwcTextFieldUI.hover"),
                "Hover should be null after MOUSE_EXITED");
    }
}
