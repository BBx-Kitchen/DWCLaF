package com.dwc.laf.ui;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicLabelUI;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * A custom {@link javax.swing.plaf.LabelUI} delegate that renders JLabel
 * with proper disabled-state opacity reduction instead of the default
 * chiseled light/dark text effect used by BasicLabelUI.
 *
 * <p>BasicLabelUI.paintDisabledText paints two offset copies of the text
 * (light + dark) to create a "chiseled" appearance. This does not match
 * the DWC web convention of rendering disabled text at reduced opacity.
 * This delegate overrides only the disabled text painting; all other
 * layout, icon, and mnemonic handling is inherited from BasicLabelUI.</p>
 *
 * <p>Each JLabel gets its own instance (not a shared singleton) for
 * consistency with the per-component pattern used by all DWC delegates.</p>
 */
public class DwcLabelUI extends BasicLabelUI {

    /** Cached disabled opacity from UIDefaults. */
    private float disabledOpacity;

    /**
     * Creates a new per-component DwcLabelUI instance.
     *
     * @param c the component (unused, required by the L&F contract)
     * @return a new DwcLabelUI instance
     */
    public static ComponentUI createUI(JComponent c) {
        return new DwcLabelUI();
    }

    @Override
    protected void installDefaults(JLabel l) {
        super.installDefaults(l);
        LookAndFeel.installProperty(l, "opaque", false);

        // Cache disabled opacity
        Object opacityObj = UIManager.get("Component.disabledOpacity");
        if (opacityObj instanceof Number num) {
            disabledOpacity = num.floatValue();
        } else {
            disabledOpacity = 0.6f;
        }
    }

    /**
     * Paints disabled text at reduced opacity instead of the default chiseled
     * light/dark text effect. Uses the label's normal foreground color with
     * an alpha composite applied.
     */
    @Override
    protected void paintDisabledText(JLabel l, Graphics g, String s,
            int textX, int textY) {
        Graphics2D g2 = (Graphics2D) g;
        java.awt.Composite oldComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, disabledOpacity));

        // Use the label's foreground (not a separate disabled color)
        Color fg = l.getForeground();
        g2.setColor(fg);
        javax.swing.plaf.basic.BasicGraphicsUtils.drawStringUnderlineCharAt(
                g2, s, l.getDisplayedMnemonicIndex(), textX, textY);

        g2.setComposite(oldComposite);
    }
}
