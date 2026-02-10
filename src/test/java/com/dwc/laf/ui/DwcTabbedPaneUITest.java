package com.dwc.laf.ui;

import com.dwc.laf.DwcLookAndFeel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DwcTabbedPaneUI} verifying per-component instances, UI
 * installation, tab state painting (normal, hover, selected, disabled),
 * underline indicator, and token-mapped UIDefaults colors.
 */
class DwcTabbedPaneUITest {

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

    // ---- Test 1: Per-component instances ----

    @Test
    void createUI_returnsPerComponentInstances() {
        JTabbedPane tp1 = new JTabbedPane();
        JTabbedPane tp2 = new JTabbedPane();
        ComponentUI ui1 = DwcTabbedPaneUI.createUI(tp1);
        ComponentUI ui2 = DwcTabbedPaneUI.createUI(tp2);

        assertNotSame(ui1, ui2,
                "Each JTabbedPane should get its own DwcTabbedPaneUI instance");
    }

    // ---- Test 2: Correct UI installed ----

    @Test
    void tabbedPane_hasCorrectUI() {
        JTabbedPane tp = new JTabbedPane();
        assertInstanceOf(DwcTabbedPaneUI.class, tp.getUI(),
                "JTabbedPane should have DwcTabbedPaneUI installed");
    }

    // ---- Test 3: Token foreground ----

    @Test
    void tabbedPane_hasTokenForeground() {
        JTabbedPane tp = new JTabbedPane();
        Color expectedFg = UIManager.getColor("TabbedPane.foreground");
        assertNotNull(expectedFg, "TabbedPane.foreground should be set in UIDefaults");
        assertEquals(expectedFg, tp.getForeground(),
                "JTabbedPane foreground should match UIManager TabbedPane.foreground");
    }

    // ---- Test 4: Paint smoke test with tabs ----

    @Test
    void paintSmokeTest_withTabs() {
        JTabbedPane tp = new JTabbedPane();
        tp.addTab("Tab A", new JPanel());
        tp.addTab("Tab B", new JPanel());
        tp.addTab("Tab C", new JPanel());
        tp.setSelectedIndex(0);
        tp.setSize(400, 300);
        tp.doLayout();

        BufferedImage img = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> tp.paint(g2),
                    "Painting a JTabbedPane with tabs should not throw");

            boolean hasContent = hasNonTransparentPixels(img);
            assertTrue(hasContent,
                    "Painted image should have at least one non-transparent pixel");
        } finally {
            g2.dispose();
        }
    }

    // ---- Test 5: Paint smoke test with no tabs ----

    @Test
    void paintSmokeTest_noTabs() {
        JTabbedPane tp = new JTabbedPane();
        tp.setSize(400, 300);
        tp.doLayout();

        BufferedImage img = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> tp.paint(g2),
                    "Painting an empty JTabbedPane should not throw");
        } finally {
            g2.dispose();
        }
    }

    // ---- Test 6: Paint smoke test with disabled tab ----

    @Test
    void paintSmokeTest_disabledTab() {
        JTabbedPane tp = new JTabbedPane();
        tp.addTab("Tab A", new JPanel());
        tp.addTab("Tab B", new JPanel());
        tp.setEnabledAt(1, false);
        tp.setSize(400, 300);
        tp.doLayout();

        BufferedImage img = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> tp.paint(g2),
                    "Painting a JTabbedPane with disabled tab should not throw");
        } finally {
            g2.dispose();
        }
    }

    // ---- Test 7: Paint smoke test with selected tab ----

    @Test
    void paintSmokeTest_selectedTab() {
        JTabbedPane tp = new JTabbedPane();
        tp.addTab("Tab A", new JPanel());
        tp.addTab("Tab B", new JPanel());
        tp.addTab("Tab C", new JPanel());
        tp.setSelectedIndex(2);
        tp.setSize(400, 300);
        tp.doLayout();

        BufferedImage img = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> tp.paint(g2),
                    "Painting a JTabbedPane with selected tab should not throw");

            boolean hasContent = hasNonTransparentPixels(img);
            assertTrue(hasContent,
                    "Painted image with selected tab should have content");
        } finally {
            g2.dispose();
        }
    }

    // ---- Test 8: Underline color from UIDefaults ----

    @Test
    void underlineColor_fromUIDefaults() {
        Color underlineColor = UIManager.getColor("TabbedPane.underlineColor");
        assertNotNull(underlineColor,
                "TabbedPane.underlineColor should be populated from token mapping");
        assertInstanceOf(ColorUIResource.class, underlineColor,
                "TabbedPane.underlineColor should be a ColorUIResource");
    }

    // ---- Test 9: Selected foreground from UIDefaults ----

    @Test
    void selectedForeground_fromUIDefaults() {
        Color selectedFg = UIManager.getColor("TabbedPane.selectedForeground");
        assertNotNull(selectedFg,
                "TabbedPane.selectedForeground should be populated from token mapping");
    }

    // ---- Test 10: Hover background from UIDefaults ----

    @Test
    void hoverBackground_fromUIDefaults() {
        Color hoverBg = UIManager.getColor("TabbedPane.hoverBackground");
        assertNotNull(hoverBg,
                "TabbedPane.hoverBackground should be populated from token mapping");
    }

    // ---- Test 11: Content area color from UIDefaults ----

    @Test
    void contentAreaColor_fromUIDefaults() {
        Color contentAreaColor = UIManager.getColor("TabbedPane.contentAreaColor");
        assertNotNull(contentAreaColor,
                "TabbedPane.contentAreaColor should be populated from token mapping");
    }

    // ---- Test 12: Tab insets from UIDefaults ----

    @Test
    void tabbedPane_tabInsets() {
        Insets tabInsets = UIManager.getInsets("TabbedPane.tabInsets");
        assertNotNull(tabInsets,
                "TabbedPane.tabInsets should be set in UIDefaults");
        assertEquals(new Insets(8, 16, 8, 16), tabInsets,
                "TabbedPane.tabInsets should be (8, 16, 8, 16)");
    }

    // ---- Helper ----

    private static boolean hasNonTransparentPixels(BufferedImage img) {
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                if (((img.getRGB(x, y) >> 24) & 0xFF) > 0) {
                    return true;
                }
            }
        }
        return false;
    }
}
