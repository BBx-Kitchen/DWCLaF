package com.dwc.laf.ui;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicRadioButtonUI;

/**
 * A custom {@link javax.swing.plaf.ButtonUI} delegate for JCheckBox that
 * registers the custom {@link DwcCheckBoxIcon} and enables rollover for
 * hover state tracking.
 *
 * <p>Extends {@link BasicRadioButtonUI} directly (same as FlatLaf) because
 * {@code BasicCheckBoxUI} is just {@code BasicRadioButtonUI} with only
 * {@code getPropertyPrefix()} changed. The icon/text layout and painting
 * are inherited; the custom {@link DwcCheckBoxIcon} handles indicator painting.</p>
 *
 * <p>Each JCheckBox gets its own instance (not a shared singleton) for
 * consistency with the per-component pattern used by {@link DwcButtonUI}.</p>
 */
public class DwcCheckBoxUI extends BasicRadioButtonUI {

    /**
     * Creates a new per-component DwcCheckBoxUI instance.
     *
     * @param c the component (unused, required by the L&F contract)
     * @return a new DwcCheckBoxUI instance
     */
    public static ComponentUI createUI(JComponent c) {
        return new DwcCheckBoxUI();
    }

    @Override
    public String getPropertyPrefix() {
        return "CheckBox.";
    }

    @Override
    protected void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        LookAndFeel.installProperty(b, "opaque", false);
        b.setRolloverEnabled(true);
    }
}
