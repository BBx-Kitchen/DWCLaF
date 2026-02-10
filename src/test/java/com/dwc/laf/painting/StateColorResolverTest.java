package com.dwc.laf.painting;

import org.junit.jupiter.api.Test;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JButton;
import javax.swing.JPanel;

import static org.junit.jupiter.api.Assertions.*;

class StateColorResolverTest {

    private static final Color ENABLED = Color.WHITE;
    private static final Color DISABLED = Color.GRAY;
    private static final Color FOCUSED = Color.CYAN;
    private static final Color HOVER = Color.YELLOW;
    private static final Color PRESSED = Color.RED;

    @Test
    void testResolve_disabledReturnsDisabledColor() {
        JButton btn = new JButton("Test");
        btn.setEnabled(false);
        assertEquals(DISABLED,
                StateColorResolver.resolve(btn, ENABLED, DISABLED, FOCUSED, HOVER, PRESSED));
    }

    @Test
    void testResolve_disabledFallsBackToEnabled() {
        JButton btn = new JButton("Test");
        btn.setEnabled(false);
        assertEquals(ENABLED,
                StateColorResolver.resolve(btn, ENABLED, null, FOCUSED, HOVER, PRESSED));
    }

    @Test
    void testResolve_pressedReturnsPressedColor() {
        JButton btn = new JButton("Test");
        btn.getModel().setArmed(true);
        btn.getModel().setPressed(true);
        assertEquals(PRESSED,
                StateColorResolver.resolve(btn, ENABLED, DISABLED, FOCUSED, HOVER, PRESSED));
    }

    @Test
    void testResolve_hoverReturnsHoverColor() {
        JButton btn = new JButton("Test");
        btn.getModel().setRollover(true);
        assertEquals(HOVER,
                StateColorResolver.resolve(btn, ENABLED, DISABLED, FOCUSED, HOVER, PRESSED));
    }

    @Test
    void testResolve_focusedReturnsFocusedColor() {
        // Override hasFocus() since we cannot get real focus in a headless test
        @SuppressWarnings("serial")
        JPanel panel = new JPanel() {
            @Override
            public boolean hasFocus() {
                return true;
            }
        };
        assertEquals(FOCUSED,
                StateColorResolver.resolve(panel, ENABLED, DISABLED, FOCUSED, HOVER, PRESSED));
    }

    @Test
    void testResolve_enabledReturnsEnabledColor() {
        JButton btn = new JButton("Test");
        assertEquals(ENABLED,
                StateColorResolver.resolve(btn, ENABLED, DISABLED, FOCUSED, HOVER, PRESSED));
    }

    @Test
    void testResolve_priorityDisabledOverPressed() {
        JButton btn = new JButton("Test");
        btn.setEnabled(false);
        btn.getModel().setArmed(true);
        btn.getModel().setPressed(true);
        assertEquals(DISABLED,
                StateColorResolver.resolve(btn, ENABLED, DISABLED, FOCUSED, HOVER, PRESSED));
    }

    @Test
    void testResolve_priorityPressedOverHover() {
        JButton btn = new JButton("Test");
        btn.getModel().setArmed(true);
        btn.getModel().setPressed(true);
        btn.getModel().setRollover(true);
        assertEquals(PRESSED,
                StateColorResolver.resolve(btn, ENABLED, DISABLED, FOCUSED, HOVER, PRESSED));
    }

    @Test
    void testPaintWithOpacity_changesComposite() {
        BufferedImage img = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        StateColorResolver.paintWithOpacity(g, 0.5f, () -> {
            g.setColor(Color.RED);
            g.fillRect(0, 0, 50, 50);
        });

        // Center pixel should have reduced alpha due to 0.5 opacity
        int centerArgb = img.getRGB(25, 25);
        int alpha = (centerArgb >> 24) & 0xFF;
        // Alpha at 0.5 opacity should be approximately 128 (half of 255)
        assertTrue(alpha > 100 && alpha < 160,
                "Alpha should be approximately 128 at 0.5 opacity, was: " + alpha);

        g.dispose();
    }

    @Test
    void testPaintWithOpacity_restoresComposite() {
        BufferedImage img = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        Composite before = g.getComposite();
        StateColorResolver.paintWithOpacity(g, 0.3f, () -> {
            // Paint something
            g.setColor(Color.BLUE);
            g.fillRect(0, 0, 10, 10);
        });
        Composite after = g.getComposite();

        assertEquals(before, after, "Composite should be restored after paintWithOpacity");
        g.dispose();
    }

    @Test
    void testPaintWithOpacity_clampsValues() {
        BufferedImage img = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Opacity > 1.0 should be clamped to 1.0 (no exception)
        assertDoesNotThrow(() ->
                StateColorResolver.paintWithOpacity(g, 2.0f, () -> {
                    g.setColor(Color.GREEN);
                    g.fillRect(0, 0, 50, 50);
                }));

        // Verify the paint happened at full opacity
        int alpha = (img.getRGB(25, 25) >> 24) & 0xFF;
        assertEquals(255, alpha, "Opacity > 1.0 should be clamped to 1.0 (full opaque)");

        // Create fresh image for negative test
        BufferedImage img2 = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img2.createGraphics();

        // Opacity < 0.0 should be clamped to 0.0 (no exception)
        assertDoesNotThrow(() ->
                StateColorResolver.paintWithOpacity(g2, -0.5f, () -> {
                    g2.setColor(Color.GREEN);
                    g2.fillRect(0, 0, 50, 50);
                }));

        // At 0.0 opacity, nothing should be visible
        int alpha2 = (img2.getRGB(25, 25) >> 24) & 0xFF;
        assertEquals(0, alpha2, "Opacity < 0.0 should be clamped to 0.0 (fully transparent)");

        g.dispose();
        g2.dispose();
    }
}
