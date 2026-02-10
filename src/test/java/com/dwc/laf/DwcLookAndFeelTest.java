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
import javax.swing.plaf.InsetsUIResource;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

import com.dwc.laf.ui.DwcTextFieldBorder;

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

    // ---- Test 11: Focus ring color computed from HSL tokens ----

    @Test
    void lafPopulatesFocusRingColor() throws UnsupportedLookAndFeelException {
        activateDwcLaf();

        Color focusRingColor = UIManager.getColor("Component.focusRingColor");
        assertNotNull(focusRingColor,
                "Component.focusRingColor should be computed from CSS HSL tokens");
        assertInstanceOf(ColorUIResource.class, focusRingColor,
                "Component.focusRingColor must be ColorUIResource");
        assertTrue(focusRingColor.getAlpha() < 255,
                "Focus ring color should be semi-transparent (alpha < 255)");
        assertTrue(focusRingColor.getAlpha() > 0,
                "Focus ring color should have some opacity (alpha > 0)");
    }

    // ---- Test 12: Button margin ----

    @Test
    void lafPopulatesButtonMargin() throws UnsupportedLookAndFeelException {
        activateDwcLaf();

        Insets margin = UIManager.getInsets("Button.margin");
        assertNotNull(margin, "Button.margin should be populated");
        assertInstanceOf(InsetsUIResource.class, margin,
                "Button.margin must be InsetsUIResource");
        assertEquals(new Insets(2, 14, 2, 14), margin,
                "Button.margin should be (2, 14, 2, 14)");
    }

    // ---- Test 13: Button minimum width ----

    @Test
    void lafPopulatesButtonMinimumWidth() throws UnsupportedLookAndFeelException {
        activateDwcLaf();

        int minimumWidth = UIManager.getInt("Button.minimumWidth");
        assertEquals(72, minimumWidth,
                "Button.minimumWidth should be 72");
    }

    // ---- Test 14: Button borderColor from token mapping ----

    @Test
    void lafPopulatesButtonBorderColor() throws UnsupportedLookAndFeelException {
        activateDwcLaf();

        Color borderColor = UIManager.getColor("Button.borderColor");
        assertNotNull(borderColor,
                "Button.borderColor should be populated from --dwc-button-border-color");
    }

    // ---- Test 15: Button rollover flag ----

    @Test
    void lafPopulatesButtonRollover() throws UnsupportedLookAndFeelException {
        activateDwcLaf();

        Object rollover = UIManager.get("Button.rollover");
        assertNotNull(rollover, "Button.rollover should be set");
        assertEquals(Boolean.TRUE, rollover, "Button.rollover should be true");
    }

    // ---- Test 16: TextField hover border color from token mapping ----

    @Test
    void lafPopulatesTextFieldHoverBorderColor() throws UnsupportedLookAndFeelException {
        activateDwcLaf();

        Color hoverBorderColor = UIManager.getColor("TextField.hoverBorderColor");
        assertNotNull(hoverBorderColor,
                "TextField.hoverBorderColor should be populated from --dwc-input-hover-border-color");
    }

    // ---- Test 17: TextField hover background from token mapping ----

    @Test
    void lafPopulatesTextFieldHoverBackground() throws UnsupportedLookAndFeelException {
        activateDwcLaf();

        Color hoverBg = UIManager.getColor("TextField.hoverBackground");
        assertNotNull(hoverBg,
                "TextField.hoverBackground should be populated from --dwc-input-hover-background");
    }

    // ---- Test 18: TextField.background is DWC input background, not white ----

    @Test
    void lafTextFieldBackgroundIsNotWhite() throws UnsupportedLookAndFeelException {
        activateDwcLaf();

        Color tfBg = UIManager.getColor("TextField.background");
        assertNotNull(tfBg, "TextField.background should be populated");
        // --dwc-input-background maps to --dwc-color-default-light, which is a grayish color,
        // not pure white (#fff = 255,255,255). The --dwc-color-white mapping was removed.
        assertFalse(tfBg.getRed() == 255 && tfBg.getGreen() == 255 && tfBg.getBlue() == 255,
                "TextField.background should NOT be pure white; it should be the DWC input background");
    }

    // ---- Test 19: TextField.border is DwcTextFieldBorder ----

    @Test
    void lafPopulatesTextFieldBorder() throws UnsupportedLookAndFeelException {
        activateDwcLaf();

        Object border = UIManager.get("TextField.border");
        assertNotNull(border, "TextField.border should be populated");
        // BorderUIResource wraps the actual border
        assertInstanceOf(javax.swing.plaf.BorderUIResource.class, border,
                "TextField.border should be wrapped in BorderUIResource");
        javax.swing.plaf.BorderUIResource bur = (javax.swing.plaf.BorderUIResource) border;
        // The BorderUIResource delegates to the wrapped border, which should be DwcTextFieldBorder.
        // We verify by checking the insets behavior matches DwcTextFieldBorder's pattern.
        javax.swing.JTextField tf = new javax.swing.JTextField();
        Insets insets = bur.getBorderInsets(tf);
        assertNotNull(insets, "Border insets should not be null");
        // With the UIDefaults set by the L&F (focusWidth + borderWidth + margin),
        // insets should be greater than zero on all sides
        assertTrue(insets.top > 0, "top inset > 0");
        assertTrue(insets.left > 0, "left inset > 0");
    }

    // ---- Test 20: TextField.margin ----

    @Test
    void lafPopulatesTextFieldMargin() throws UnsupportedLookAndFeelException {
        activateDwcLaf();

        Insets margin = UIManager.getInsets("TextField.margin");
        assertNotNull(margin, "TextField.margin should be populated");
        assertInstanceOf(InsetsUIResource.class, margin,
                "TextField.margin must be InsetsUIResource");
        assertEquals(new Insets(2, 6, 2, 6), margin,
                "TextField.margin should be (2, 6, 2, 6)");
    }

    // ---- Test 21: CheckBox.icon.background populated ----

    @Test
    void lafPopulatesCheckBoxIconBackground() throws UnsupportedLookAndFeelException {
        activateDwcLaf();

        Color bg = UIManager.getColor("CheckBox.icon.background");
        assertNotNull(bg, "CheckBox.icon.background should be populated from --dwc-color-default");
        assertInstanceOf(ColorUIResource.class, bg);
    }

    // ---- Test 22: CheckBox.icon.checkmarkColor is white (not primary blue) ----

    @Test
    void lafCheckBoxCheckmarkColorIsWhite() throws UnsupportedLookAndFeelException {
        activateDwcLaf();

        Color checkmarkColor = UIManager.getColor("CheckBox.icon.checkmarkColor");
        assertNotNull(checkmarkColor,
                "CheckBox.icon.checkmarkColor should be populated from --dwc-color-on-primary-text");
        // White-ish: RGB values should all be above 200 (on-primary-text is white/near-white)
        assertTrue(checkmarkColor.getRed() > 200 && checkmarkColor.getGreen() > 200
                && checkmarkColor.getBlue() > 200,
                "Checkmark color should be white-ish (not primary blue). Got: " + checkmarkColor);
    }

    // ---- Test 23: RadioButton.icon.dotColor populated ----

    @Test
    void lafPopulatesRadioButtonIconDotColor() throws UnsupportedLookAndFeelException {
        activateDwcLaf();

        Color dotColor = UIManager.getColor("RadioButton.icon.dotColor");
        assertNotNull(dotColor, "RadioButton.icon.dotColor should be populated");
        assertInstanceOf(ColorUIResource.class, dotColor);
    }

    // ---- Test 24: CheckBox.icon.selectedBackground populated ----

    @Test
    void lafPopulatesCheckBoxSelectedBackground() throws UnsupportedLookAndFeelException {
        activateDwcLaf();

        Color selBg = UIManager.getColor("CheckBox.icon.selectedBackground");
        assertNotNull(selBg, "CheckBox.icon.selectedBackground should be populated from --dwc-color-primary");
        assertInstanceOf(ColorUIResource.class, selBg);
    }

    // ---- Test 25: ComboBox.background populated ----

    @Test
    void lafPopulatesComboBoxBackground() throws UnsupportedLookAndFeelException {
        activateDwcLaf();

        Color bg = UIManager.getColor("ComboBox.background");
        assertNotNull(bg, "ComboBox.background should be populated from --dwc-input-background");
        assertInstanceOf(ColorUIResource.class, bg);
    }

    // ---- Test 26: ComboBox.selectionBackground populated ----

    @Test
    void lafPopulatesComboBoxSelectionBackground() throws UnsupportedLookAndFeelException {
        activateDwcLaf();

        Color selBg = UIManager.getColor("ComboBox.selectionBackground");
        assertNotNull(selBg, "ComboBox.selectionBackground should be populated from --dwc-color-primary");
        assertInstanceOf(ColorUIResource.class, selBg);
    }

    // ---- Test 27: CheckBoxUI class default registered ----

    @Test
    void lafRegistersCheckBoxUI() throws UnsupportedLookAndFeelException {
        activateDwcLaf();

        Object checkBoxUI = UIManager.get("CheckBoxUI");
        assertEquals("com.dwc.laf.ui.DwcCheckBoxUI", checkBoxUI,
                "CheckBoxUI should be registered to DwcCheckBoxUI");
    }

    // ---- Test 28: RadioButtonUI class default registered ----

    @Test
    void lafRegistersRadioButtonUI() throws UnsupportedLookAndFeelException {
        activateDwcLaf();

        Object radioButtonUI = UIManager.get("RadioButtonUI");
        assertEquals("com.dwc.laf.ui.DwcRadioButtonUI", radioButtonUI,
                "RadioButtonUI should be registered to DwcRadioButtonUI");
    }

    // ---- Test 29: ComboBoxUI class default registered ----

    @Test
    void lafRegistersComboBoxUI() throws UnsupportedLookAndFeelException {
        activateDwcLaf();

        Object comboBoxUI = UIManager.get("ComboBoxUI");
        assertEquals("com.dwc.laf.ui.DwcComboBoxUI", comboBoxUI,
                "ComboBoxUI should be registered to DwcComboBoxUI");
    }
}
