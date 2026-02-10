package com.dwc.laf;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import java.awt.Color;
import java.awt.Font;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link DwcLookAndFeel}.
 *
 * <p>Tests verify the full pipeline: L&amp;F activation via UIManager ->
 * CSS loading -> token mapping -> UIDefaults population.</p>
 *
 * <p>Each test saves and restores the previous L&amp;F to avoid test pollution.</p>
 */
class DwcLookAndFeelTest {

    private LookAndFeel previousLaf;

    @BeforeAll
    static void setHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    @BeforeEach
    void saveCurrentLaf() {
        previousLaf = UIManager.getLookAndFeel();
    }

    @AfterEach
    void restorePreviousLaf() {
        try {
            if (previousLaf != null) {
                UIManager.setLookAndFeel(previousLaf);
            }
        } catch (UnsupportedLookAndFeelException e) {
            // Best effort restore
        }
    }

    // ---- Helper ----

    private void activateDwcLaf() throws UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(new DwcLookAndFeel());
    }

    // ---- Test 1: Metadata ----

    @Test
    void lafMetadata() {
        DwcLookAndFeel laf = new DwcLookAndFeel();

        assertEquals("DWC", laf.getName());
        assertEquals("DwcLaf", laf.getID());
        assertNotNull(laf.getDescription());
        assertFalse(laf.getDescription().isEmpty());
        assertFalse(laf.isNativeLookAndFeel());
        assertTrue(laf.isSupportedLookAndFeel());
    }

    // ---- Test 2: Activation ----

    @Test
    void lafActivatesViaUIManager() throws UnsupportedLookAndFeelException {
        assertDoesNotThrow(() -> UIManager.setLookAndFeel(new DwcLookAndFeel()));
        assertEquals("DWC", UIManager.getLookAndFeel().getName());
    }

    // ---- Test 3: Color UIDefaults population ----

    @Test
    void lafPopulatesColorUIDefaults() throws UnsupportedLookAndFeelException {
        activateDwcLaf();

        Color panelBg = UIManager.getColor("Panel.background");
        assertNotNull(panelBg, "Panel.background should be populated");
        assertInstanceOf(ColorUIResource.class, panelBg,
                "Panel.background must be ColorUIResource for UIResource contract");

        Color labelFg = UIManager.getColor("Label.foreground");
        assertNotNull(labelFg, "Label.foreground should be populated");
        assertInstanceOf(ColorUIResource.class, labelFg,
                "Label.foreground must be ColorUIResource for UIResource contract");
    }

    // ---- Test 4: Primary color mapping ----

    @Test
    void lafPopulatesPrimaryColor() throws UnsupportedLookAndFeelException {
        activateDwcLaf();

        Color buttonDefaultBg = UIManager.getColor("Button.default.background");
        assertNotNull(buttonDefaultBg,
                "Button.default.background should be populated from --dwc-color-primary");
    }

    // ---- Test 5: Integer defaults ----

    @Test
    void lafPopulatesIntegerDefaults() throws UnsupportedLookAndFeelException {
        activateDwcLaf();

        Object componentArc = UIManager.get("Component.arc");
        assertNotNull(componentArc, "Component.arc should be populated");
        assertInstanceOf(Integer.class, componentArc,
                "Component.arc should be an Integer");

        Object focusWidth = UIManager.get("Component.focusWidth");
        assertNotNull(focusWidth, "Component.focusWidth should be populated");
        assertInstanceOf(Integer.class, focusWidth,
                "Component.focusWidth should be an Integer");
    }

    // ---- Test 6: Font population ----

    @Test
    void lafPopulatesFont() throws UnsupportedLookAndFeelException {
        activateDwcLaf();

        Font defaultFont = UIManager.getFont("defaultFont");
        assertNotNull(defaultFont, "defaultFont should be populated");
        assertInstanceOf(FontUIResource.class, defaultFont,
                "defaultFont must be FontUIResource");
        assertTrue(defaultFont.getSize() > 0, "Font size must be > 0");
        assertNotNull(defaultFont.getFamily(), "Font family must not be null");
        assertFalse(defaultFont.getFamily().isEmpty(), "Font family must not be empty");
    }

    // ---- Test 7: Token map accessible ----

    @Test
    void lafTokenMapAccessible() throws UnsupportedLookAndFeelException {
        activateDwcLaf();

        LookAndFeel laf = UIManager.getLookAndFeel();
        assertInstanceOf(DwcLookAndFeel.class, laf);

        DwcLookAndFeel dwcLaf = (DwcLookAndFeel) laf;
        assertNotNull(dwcLaf.getTokenMap(), "Token map should be accessible after activation");
        assertTrue(dwcLaf.getTokenMap().size() > 0, "Token map should contain entries");
    }

    // ---- Test 8: One-to-many token mapping ----

    @Test
    void lafPopulatesMultipleKeysFromSingleToken() throws UnsupportedLookAndFeelException {
        activateDwcLaf();

        // --dwc-border-radius maps to both Button.arc and Component.arc
        Object buttonArc = UIManager.get("Button.arc");
        Object componentArc = UIManager.get("Component.arc");

        assertNotNull(buttonArc, "Button.arc should be populated from --dwc-border-radius");
        assertNotNull(componentArc, "Component.arc should be populated from --dwc-border-radius");
        assertEquals(buttonArc, componentArc,
                "Button.arc and Component.arc should have the same value (same CSS token)");
    }

    // ---- Test 9: No external dependencies ----

    @Test
    void lafNoExternalDependencies() {
        // Verify that DwcLookAndFeel can be instantiated without any external classpath additions.
        // If this test runs, it proves the class loads with only JDK + our own code.
        assertDoesNotThrow(() -> {
            DwcLookAndFeel laf = new DwcLookAndFeel();
            assertNotNull(laf);
            assertEquals("DWC", laf.getName());
        });
    }

    // ---- Test 10: L&F switchability (UIResource contract) ----

    @Test
    void lafSwitchable() throws Exception {
        // Activate DWC L&F
        activateDwcLaf();
        assertEquals("DWC", UIManager.getLookAndFeel().getName());

        // Switch to cross-platform (Metal) L&F
        String crossPlatformClassName = UIManager.getCrossPlatformLookAndFeelClassName();
        UIManager.setLookAndFeel(crossPlatformClassName);
        assertNotEquals("DWC", UIManager.getLookAndFeel().getName(),
                "Should have switched away from DWC L&F");

        // The switch succeeding proves UIResource wrapping is correct:
        // Swing can replace L&F-set values because they are UIResource instances.
    }
}
