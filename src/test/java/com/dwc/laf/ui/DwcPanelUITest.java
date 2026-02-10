package com.dwc.laf.ui;

import com.dwc.laf.DwcLookAndFeel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DwcPanelUI} verifying per-component instances, UI installation,
 * background color, paint smoke tests (default and card mode), and UIDefaults entries.
 */
class DwcPanelUITest {

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
        JPanel p1 = new JPanel();
        JPanel p2 = new JPanel();
        ComponentUI ui1 = DwcPanelUI.createUI(p1);
        ComponentUI ui2 = DwcPanelUI.createUI(p2);

        assertNotSame(ui1, ui2,
                "Each JPanel should get its own DwcPanelUI instance");
    }

    @Test
    void panel_hasCorrectUI() {
        JPanel panel = new JPanel();
        assertInstanceOf(DwcPanelUI.class, panel.getUI(),
                "JPanel should have DwcPanelUI installed");
    }

    @Test
    void panel_backgroundFromToken() {
        JPanel panel = new JPanel();
        Color bg = panel.getBackground();
        assertNotNull(bg, "Panel background should not be null");

        Color uiBg = UIManager.getColor("Panel.background");
        assertNotNull(uiBg, "UIManager Panel.background should not be null");
        assertInstanceOf(ColorUIResource.class, uiBg,
                "Panel.background should be a ColorUIResource");
        assertEquals(uiBg, bg,
                "Panel background should match UIManager.getColor('Panel.background')");
    }

    @Test
    void paintSmokeTest_defaultMode() {
        JPanel panel = new JPanel();
        panel.setSize(200, 200);

        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> panel.paint(g2),
                    "Painting a JPanel in default mode should not throw");
        } finally {
            g2.dispose();
        }
    }

    @Test
    void paintSmokeTest_cardMode() {
        JPanel panel = new JPanel();
        panel.putClientProperty("dwc.panelStyle", "card");
        panel.setSize(200, 200);

        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> panel.paint(g2),
                    "Painting a JPanel in card mode should not throw");

            // Verify something was painted (shadow + rounded background)
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
            assertTrue(hasContent,
                    "Card mode painted image should have at least one non-transparent pixel");
        } finally {
            g2.dispose();
        }
    }

    @Test
    void cardMode_isNotOpaque() {
        JPanel panel = new JPanel();
        panel.putClientProperty("dwc.panelStyle", "card");
        panel.setSize(200, 200);

        // Trigger update() which sets opaque to false in card mode
        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            panel.paint(g2);
        } finally {
            g2.dispose();
        }

        assertFalse(panel.isOpaque(),
                "JPanel in card mode should not be opaque after painting");
    }

    @Test
    void panel_arcFromUIDefaults() {
        int arc = UIManager.getInt("Panel.arc");
        assertTrue(arc > 0,
                "Panel.arc should be > 0, but was " + arc);
    }

    @Test
    void panel_shadowColorFromUIDefaults() {
        Color shadowColor = UIManager.getColor("Panel.shadowColor");
        assertNotNull(shadowColor,
                "Panel.shadowColor should not be null");
    }
}
