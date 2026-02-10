package com.dwc.laf.ui;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicComboBoxUI;

/**
 * A custom {@link javax.swing.plaf.ComboBoxUI} delegate for JComboBox.
 *
 * <p>This is a placeholder class registered in {@code DwcLookAndFeel.initClassDefaults()}
 * for Phase 6 Plan 02 implementation. Currently delegates all behavior to
 * {@link BasicComboBoxUI}. The UIDefaults color mappings (ComboBox.background,
 * ComboBox.borderColor, etc.) are already populated by the token mapping pipeline.</p>
 */
public class DwcComboBoxUI extends BasicComboBoxUI {

    /**
     * Creates a new per-component DwcComboBoxUI instance.
     *
     * @param c the component (unused, required by the L&F contract)
     * @return a new DwcComboBoxUI instance
     */
    public static ComponentUI createUI(JComponent c) {
        return new DwcComboBoxUI();
    }
}
