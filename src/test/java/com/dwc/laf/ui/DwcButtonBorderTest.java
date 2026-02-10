package com.dwc.laf.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.UIManager;
import javax.swing.plaf.InsetsUIResource;
import java.awt.Color;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DwcButtonBorder}.
 *
 * <p>Each test sets up UIDefaults with known values and verifies the border
 * behaves correctly for insets calculation and painting.</p>
 */
class DwcButtonBorderTest {

    private static final int FOCUS_WIDTH = 3;
    private static final int BORDER_WIDTH = 1;
    private static final int ARC = 6;

    private Object origFocusWidth;
    private Object origBorderWidth;
    private Object origArc;
    private Object origBorderColor;

    @BeforeEach
    void setUpUIDefaults() {
        // Save originals
        origFocusWidth = UIManager.get("Component.focusWidth");
        origBorderWidth = UIManager.get("Component.borderWidth");
        origArc = UIManager.get("Button.arc");
        origBorderColor = UIManager.get("Button.borderColor");

        // Set known values
        UIManager.put("Component.focusWidth", FOCUS_WIDTH);
        UIManager.put("Component.borderWidth", BORDER_WIDTH);
        UIManager.put("Button.arc", ARC);
        UIManager.put("Button.borderColor", new Color(200, 200, 200));
    }

    @AfterEach
    void restoreUIDefaults() {
        UIManager.put("Component.focusWidth", origFocusWidth);
        UIManager.put("Component.borderWidth", origBorderWidth);
        UIManager.put("Button.arc", origArc);
        UIManager.put("Button.borderColor", origBorderColor);
    }

    // ---- Insets tests ----

    @Test
    void insetsIncludeFocusWidthBorderWidthAndDefaultMargin() {
        DwcButtonBorder border = new DwcButtonBorder();
        JButton button = new JButton("Test");
        Insets insets = border.getBorderInsets(button, new Insets(0, 0, 0, 0));

        // focusWidth(3) + borderWidth(1) + margin.top(2) = 6
        assertEquals(FOCUS_WIDTH + BORDER_WIDTH + 2, insets.top, "top");
        // focusWidth(3) + borderWidth(1) + margin.left(14) = 18
        assertEquals(FOCUS_WIDTH + BORDER_WIDTH + 14, insets.left, "left");
        // focusWidth(3) + borderWidth(1) + margin.bottom(2) = 6
        assertEquals(FOCUS_WIDTH + BORDER_WIDTH + 2, insets.bottom, "bottom");
        // focusWidth(3) + borderWidth(1) + margin.right(14) = 18
        assertEquals(FOCUS_WIDTH + BORDER_WIDTH + 14, insets.right, "right");
    }

    @Test
    void applicationSetMarginOverridesDefault() {
        DwcButtonBorder border = new DwcButtonBorder();
        JButton button = new JButton("Test");
        // Set a non-UIResource margin (simulating application override)
        button.setMargin(new Insets(5, 10, 5, 10));

        Insets insets = border.getBorderInsets(button, new Insets(0, 0, 0, 0));

        // focusWidth(3) + borderWidth(1) + custom margin
        assertEquals(FOCUS_WIDTH + BORDER_WIDTH + 5, insets.top, "top");
        assertEquals(FOCUS_WIDTH + BORDER_WIDTH + 10, insets.left, "left");
        assertEquals(FOCUS_WIDTH + BORDER_WIDTH + 5, insets.bottom, "bottom");
        assertEquals(FOCUS_WIDTH + BORDER_WIDTH + 10, insets.right, "right");
    }

    @Test
    void uiResourceMarginDoesNotOverrideDefault() {
        DwcButtonBorder border = new DwcButtonBorder();
        JButton button = new JButton("Test");
        // Set a UIResource margin (simulating L&F-installed margin)
        button.setMargin(new InsetsUIResource(8, 20, 8, 20));

        Insets insets = border.getBorderInsets(button, new Insets(0, 0, 0, 0));

        // Should still use default margin (2, 14, 2, 14), not the UIResource one
        assertEquals(FOCUS_WIDTH + BORDER_WIDTH + 2, insets.top, "top");
        assertEquals(FOCUS_WIDTH + BORDER_WIDTH + 14, insets.left, "left");
        assertEquals(FOCUS_WIDTH + BORDER_WIDTH + 2, insets.bottom, "bottom");
        assertEquals(FOCUS_WIDTH + BORDER_WIDTH + 14, insets.right, "right");
    }

    @Test
    void insetsMutateProvidedObject() {
        DwcButtonBorder border = new DwcButtonBorder();
        JButton button = new JButton("Test");
        Insets original = new Insets(99, 99, 99, 99);
        Insets returned = border.getBorderInsets(button, original);

        assertSame(original, returned, "Should return the same Insets object");
        // Verify it was mutated
        assertEquals(FOCUS_WIDTH + BORDER_WIDTH + 2, original.top);
    }

    // ---- Paint tests ----

    @Test
    void paintBorderWithBorderColorDoesNotThrow() {
        DwcButtonBorder border = new DwcButtonBorder();
        JButton button = new JButton("Test");

        BufferedImage img = new BufferedImage(100, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        assertDoesNotThrow(() -> border.paintBorder(button, g, 0, 0, 100, 40));
        g.dispose();

        // Verify some pixels were painted in the border area
        boolean foundBorderPixel = false;
        // Check pixels in the outline area (between focusWidth offset and borderWidth)
        // Top edge at y = focusWidth (3)
        for (int x = FOCUS_WIDTH + ARC / 2; x < 100 - FOCUS_WIDTH - ARC / 2; x++) {
            int alpha = (img.getRGB(x, FOCUS_WIDTH) >> 24) & 0xFF;
            if (alpha > 0) {
                foundBorderPixel = true;
                break;
            }
        }
        assertTrue(foundBorderPixel, "Border pixels should be painted when borderColor is set");
    }

    @Test
    void paintBorderNullColorIsNoop() {
        UIManager.put("Button.borderColor", null);

        DwcButtonBorder border = new DwcButtonBorder();
        JButton button = new JButton("Test");

        BufferedImage img = new BufferedImage(100, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        border.paintBorder(button, g, 0, 0, 100, 40);
        g.dispose();

        // Verify no pixels were painted
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                assertEquals(0, (img.getRGB(x, y) >> 24) & 0xFF,
                        "No pixels should be painted when borderColor is null");
            }
        }
    }

    @Test
    void paintBorderSkipsWhenBorderPaintedFalse() {
        DwcButtonBorder border = new DwcButtonBorder();
        JButton button = new JButton("Test");
        button.setBorderPainted(false);

        BufferedImage img = new BufferedImage(100, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        border.paintBorder(button, g, 0, 0, 100, 40);
        g.dispose();

        // Verify no pixels were painted
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                assertEquals(0, (img.getRGB(x, y) >> 24) & 0xFF,
                        "No pixels should be painted when borderPainted is false");
            }
        }
    }
}
