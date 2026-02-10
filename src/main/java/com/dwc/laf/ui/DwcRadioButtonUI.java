package com.dwc.laf.ui;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicRadioButtonUI;

/**
 * A custom {@link javax.swing.plaf.ButtonUI} delegate for JRadioButton that
 * registers the custom {@link DwcRadioButtonIcon} and enables rollover for
 * hover state tracking.
 *
 * <p>Extends {@link BasicRadioButtonUI} directly. The icon/text layout and
 * painting are inherited; the custom {@link DwcRadioButtonIcon} handles
 * indicator painting.</p>
 *
 * <p>Each JRadioButton gets its own instance (not a shared singleton).</p>
 */
public class DwcRadioButtonUI extends BasicRadioButtonUI {

    /**
     * Creates a new per-component DwcRadioButtonUI instance.
     *
     * @param c the component (unused, required by the L&F contract)
     * @return a new DwcRadioButtonUI instance
     */
    public static ComponentUI createUI(JComponent c) {
        return new DwcRadioButtonUI();
    }

    @Override
    public String getPropertyPrefix() {
        return "RadioButton.";
    }

    @Override
    protected void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        LookAndFeel.installProperty(b, "opaque", false);
        b.setRolloverEnabled(true);
    }
}
