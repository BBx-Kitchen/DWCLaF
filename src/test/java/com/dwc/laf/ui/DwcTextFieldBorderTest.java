package com.dwc.laf.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.plaf.InsetsUIResource;
import java.awt.Color;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DwcTextFieldBorder}.
 *
 * <p>Each test sets up UIDefaults with known values and verifies the border
 * behaves correctly for insets calculation and painting.</p>
 */
class DwcTextFieldBorderTest {

    private static final int FOCUS_WIDTH = 3;
    private static final int BORDER_WIDTH = 1;
    private static final int ARC = 6;
    private static final Color NORMAL_BORDER_COLOR = new Color(200, 200, 200);
    private static final Color HOVER_BORDER_COLOR = new Color(33, 150, 243);

    private Object origFocusWidth;
    private Object origBorderWidth;
    private Object origArc;
    private Object origBorderColor;
    private Object origHoverBorderColor;

    @BeforeEach
    void setUpUIDefaults() {
        // Save originals
        origFocusWidth = UIManager.get("Component.focusWidth");
        origBorderWidth = UIManager.get("Component.borderWidth");
        origArc = UIManager.get("TextField.arc");
        origBorderColor = UIManager.get("TextField.borderColor");
        origHoverBorderColor = UIManager.get("TextField.hoverBorderColor");

        // Set known values
        UIManager.put("Component.focusWidth", FOCUS_WIDTH);
        UIManager.put("Component.borderWidth", BORDER_WIDTH);
        UIManager.put("TextField.arc", ARC);
        UIManager.put("TextField.borderColor", NORMAL_BORDER_COLOR);
        UIManager.put("TextField.hoverBorderColor", HOVER_BORDER_COLOR);
    }

    @AfterEach
    void restoreUIDefaults() {
        UIManager.put("Component.focusWidth", origFocusWidth);
        UIManager.put("Component.borderWidth", origBorderWidth);
        UIManager.put("TextField.arc", origArc);
        UIManager.put("TextField.borderColor", origBorderColor);
        UIManager.put("TextField.hoverBorderColor", origHoverBorderColor);
    }

    // ---- Insets tests ----

    @Test
    void insetsIncludeFocusWidthBorderWidthAndDefaultMargin() {
        DwcTextFieldBorder border = new DwcTextFieldBorder();
        JTextField textField = new JTextField("Test");
        Insets insets = border.getBorderInsets(textField, new Insets(0, 0, 0, 0));

        // focusWidth(3) + borderWidth(1) + margin.top(2) = 6
        assertEquals(FOCUS_WIDTH + BORDER_WIDTH + 2, insets.top, "top");
        // focusWidth(3) + borderWidth(1) + margin.left(6) = 10
        assertEquals(FOCUS_WIDTH + BORDER_WIDTH + 6, insets.left, "left");
        // focusWidth(3) + borderWidth(1) + margin.bottom(2) = 6
        assertEquals(FOCUS_WIDTH + BORDER_WIDTH + 2, insets.bottom, "bottom");
        // focusWidth(3) + borderWidth(1) + margin.right(6) = 10
        assertEquals(FOCUS_WIDTH + BORDER_WIDTH + 6, insets.right, "right");
    }

    @Test
    void applicationSetMarginOverridesDefault() {
        DwcTextFieldBorder border = new DwcTextFieldBorder();
        JTextField textField = new JTextField("Test");
        // Set a non-UIResource margin (simulating application override)
        textField.setMargin(new Insets(5, 10, 5, 10));

        Insets insets = border.getBorderInsets(textField, new Insets(0, 0, 0, 0));

        // focusWidth(3) + borderWidth(1) + custom margin
        assertEquals(FOCUS_WIDTH + BORDER_WIDTH + 5, insets.top, "top");
        assertEquals(FOCUS_WIDTH + BORDER_WIDTH + 10, insets.left, "left");
        assertEquals(FOCUS_WIDTH + BORDER_WIDTH + 5, insets.bottom, "bottom");
        assertEquals(FOCUS_WIDTH + BORDER_WIDTH + 10, insets.right, "right");
    }

    @Test
    void uiResourceMarginDoesNotOverrideDefault() {
        DwcTextFieldBorder border = new DwcTextFieldBorder();
        JTextField textField = new JTextField("Test");
        // Set a UIResource margin (simulating L&F-installed margin)
        textField.setMargin(new InsetsUIResource(8, 20, 8, 20));

        Insets insets = border.getBorderInsets(textField, new Insets(0, 0, 0, 0));

        // Should still use default margin (2, 6, 2, 6), not the UIResource one
        assertEquals(FOCUS_WIDTH + BORDER_WIDTH + 2, insets.top, "top");
        assertEquals(FOCUS_WIDTH + BORDER_WIDTH + 6, insets.left, "left");
        assertEquals(FOCUS_WIDTH + BORDER_WIDTH + 2, insets.bottom, "bottom");
        assertEquals(FOCUS_WIDTH + BORDER_WIDTH + 6, insets.right, "right");
    }

    @Test
    void insetsMutateProvidedObject() {
        DwcTextFieldBorder border = new DwcTextFieldBorder();
        JTextField textField = new JTextField("Test");
        Insets original = new Insets(99, 99, 99, 99);
        Insets returned = border.getBorderInsets(textField, original);

        assertSame(original, returned, "Should return the same Insets object");
        // Verify it was mutated
        assertEquals(FOCUS_WIDTH + BORDER_WIDTH + 2, original.top);
    }

    // ---- Paint tests ----

    @Test
    void paintBorderDoesNotThrow() {
        DwcTextFieldBorder border = new DwcTextFieldBorder();
        JTextField textField = new JTextField("Test");

        BufferedImage img = new BufferedImage(200, 30, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        assertDoesNotThrow(() -> border.paintBorder(textField, g, 0, 0, 200, 30));
        g.dispose();
    }

    @Test
    void paintBorderUsesNormalColorByDefault() {
        DwcTextFieldBorder border = new DwcTextFieldBorder();
        JTextField textField = new JTextField("Test");

        BufferedImage img = new BufferedImage(200, 30, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        border.paintBorder(textField, g, 0, 0, 200, 30);
        g.dispose();

        // Verify border pixels were painted with the normal border color
        boolean foundBorderPixel = false;
        for (int x = FOCUS_WIDTH + ARC / 2; x < 200 - FOCUS_WIDTH - ARC / 2; x++) {
            int rgb = img.getRGB(x, FOCUS_WIDTH) & 0x00FFFFFF;
            int alpha = (img.getRGB(x, FOCUS_WIDTH) >> 24) & 0xFF;
            if (alpha > 0) {
                // Should be the normal border color (200, 200, 200)
                int expectedRgb = NORMAL_BORDER_COLOR.getRGB() & 0x00FFFFFF;
                assertEquals(expectedRgb, rgb,
                        "Normal state should use TextField.borderColor");
                foundBorderPixel = true;
                break;
            }
        }
        assertTrue(foundBorderPixel, "Border pixels should be painted");
    }

    @Test
    void paintBorderUsesHoverColorWhenClientPropertySet() {
        DwcTextFieldBorder border = new DwcTextFieldBorder();
        JTextField textField = new JTextField("Test");
        textField.putClientProperty("DwcTextFieldUI.hover", Boolean.TRUE);

        BufferedImage img = new BufferedImage(200, 30, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        border.paintBorder(textField, g, 0, 0, 200, 30);
        g.dispose();

        // Verify border pixels were painted with the hover border color
        boolean foundBorderPixel = false;
        for (int x = FOCUS_WIDTH + ARC / 2; x < 200 - FOCUS_WIDTH - ARC / 2; x++) {
            int rgb = img.getRGB(x, FOCUS_WIDTH) & 0x00FFFFFF;
            int alpha = (img.getRGB(x, FOCUS_WIDTH) >> 24) & 0xFF;
            if (alpha > 0) {
                // Should be the hover border color (33, 150, 243)
                int expectedRgb = HOVER_BORDER_COLOR.getRGB() & 0x00FFFFFF;
                assertEquals(expectedRgb, rgb,
                        "Hover state should use TextField.hoverBorderColor");
                foundBorderPixel = true;
                break;
            }
        }
        assertTrue(foundBorderPixel, "Border pixels should be painted in hover state");
    }

    @Test
    void paintBorderNullColorIsNoop() {
        UIManager.put("TextField.borderColor", null);

        DwcTextFieldBorder border = new DwcTextFieldBorder();
        JTextField textField = new JTextField("Test");

        BufferedImage img = new BufferedImage(200, 30, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        border.paintBorder(textField, g, 0, 0, 200, 30);
        g.dispose();

        // Verify no pixels were painted
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                assertEquals(0, (img.getRGB(x, y) >> 24) & 0xFF,
                        "No pixels should be painted when borderColor is null");
            }
        }
    }
}
