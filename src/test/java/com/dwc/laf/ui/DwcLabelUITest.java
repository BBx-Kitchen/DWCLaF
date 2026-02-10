package com.dwc.laf.ui;

import com.dwc.laf.DwcLookAndFeel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DwcLabelUI} verifying per-component instances, UI installation,
 * opacity, paint smoke tests (enabled and disabled), and token-derived font/color.
 */
class DwcLabelUITest {

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
        JLabel l1 = new JLabel("A");
        JLabel l2 = new JLabel("B");
        ComponentUI ui1 = DwcLabelUI.createUI(l1);
        ComponentUI ui2 = DwcLabelUI.createUI(l2);

        assertNotSame(ui1, ui2,
                "Each JLabel should get its own DwcLabelUI instance");
    }

    @Test
    void label_hasCorrectUI() {
        JLabel label = new JLabel("Test");
        assertInstanceOf(DwcLabelUI.class, label.getUI(),
                "JLabel should have DwcLabelUI installed");
    }

    @Test
    void label_isNotOpaque() {
        JLabel label = new JLabel("Test");
        assertFalse(label.isOpaque(),
                "JLabel should not be opaque after L&F installation");
    }

    @Test
    void paintSmokeTest() {
        JLabel label = new JLabel("Hello");
        label.setSize(100, 30);

        BufferedImage img = new BufferedImage(100, 30, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> label.paint(g2),
                    "Painting a JLabel should not throw");

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
    void paintSmokeTest_disabled() {
        JLabel label = new JLabel("Disabled");
        label.setEnabled(false);
        label.setSize(100, 30);

        BufferedImage img = new BufferedImage(100, 30, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> label.paint(g2),
                    "Painting a disabled JLabel should not throw");
        } finally {
            g2.dispose();
        }
    }

    @Test
    void label_usesTokenFont() {
        JLabel label = new JLabel("Font Test");
        Font font = label.getFont();
        assertNotNull(font, "Label font should not be null");

        Font uiFont = UIManager.getFont("Label.font");
        assertNotNull(uiFont, "UIManager Label.font should not be null");
        assertEquals(uiFont, font,
                "Label font should match UIManager.getFont('Label.font')");
    }

    @Test
    void label_usesTokenForeground() {
        JLabel label = new JLabel("Color Test");
        Color fg = label.getForeground();
        assertNotNull(fg, "Label foreground should not be null");

        Color uiFg = UIManager.getColor("Label.foreground");
        assertNotNull(uiFg, "UIManager Label.foreground should not be null");
        assertEquals(uiFg, fg,
                "Label foreground should match UIManager.getColor('Label.foreground')");
    }
}
