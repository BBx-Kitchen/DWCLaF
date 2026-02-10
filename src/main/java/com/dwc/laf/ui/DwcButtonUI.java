package com.dwc.laf.ui;

import com.dwc.laf.painting.FocusRingPainter;
import com.dwc.laf.painting.PaintUtils;
import com.dwc.laf.painting.StateColorResolver;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;

/**
 * A custom {@link javax.swing.plaf.ButtonUI} delegate that paints JButton
 * components with rounded backgrounds, borders, icons, text, and focus rings
 * based on DWC CSS design tokens.
 *
 * <p>Supports two visual variants:
 * <ul>
 *   <li><b>Default:</b> neutral background/foreground colors</li>
 *   <li><b>Primary:</b> accent-colored background/foreground, activated via
 *       {@code button.putClientProperty("dwc.buttonType", "primary")}</li>
 * </ul>
 *
 * <p>Five visual states are rendered: normal, hover, pressed, focused, and
 * disabled. Colors are resolved by {@link StateColorResolver} based on
 * the button's current state. Disabled state paints at reduced opacity.</p>
 *
 * <p>Each JButton gets its own instance (not a shared singleton) to allow
 * per-component state caching in future phases.</p>
 */
public class DwcButtonUI extends BasicButtonUI {

    // Colors loaded from UIDefaults
    private Color background;
    private Color foreground;
    private Color hoverBackground;
    private Color pressedBackground;
    private Color disabledText;

    // Primary variant colors
    private Color defaultBackground;
    private Color defaultForeground;
    private Color defaultHoverBackground;
    private Color defaultPressedBackground;

    // Focus ring color
    private Color focusRingColor;

    // Border color
    private Color borderColor;

    // Dimensions
    private int arc;
    private int focusWidth;
    private int borderWidth;
    private int minimumWidth;

    // Opacity
    private float disabledOpacity;

    /**
     * Creates a new {@code DwcButtonUI} instance for the given component.
     * Returns a per-component instance (not a shared singleton).
     *
     * @param c the component (unused, but required by the L&F contract)
     * @return a new DwcButtonUI instance
     */
    public static ComponentUI createUI(JComponent c) {
        return new DwcButtonUI();
    }

    @Override
    protected void installDefaults(AbstractButton b) {
        super.installDefaults(b);

        // Read colors from UIManager
        background = UIManager.getColor("Button.background");
        foreground = UIManager.getColor("Button.foreground");
        hoverBackground = UIManager.getColor("Button.hoverBackground");
        pressedBackground = UIManager.getColor("Button.pressedBackground");
        disabledText = UIManager.getColor("Button.disabledText");

        // Primary variant colors
        defaultBackground = UIManager.getColor("Button.default.background");
        defaultForeground = UIManager.getColor("Button.default.foreground");
        defaultHoverBackground = UIManager.getColor("Button.default.hoverBackground");
        defaultPressedBackground = UIManager.getColor("Button.default.pressedBackground");

        // Shared component colors
        focusRingColor = UIManager.getColor("Component.focusRingColor");
        borderColor = UIManager.getColor("Button.borderColor");

        // Dimensions
        arc = UIManager.getInt("Button.arc");
        focusWidth = UIManager.getInt("Component.focusWidth");
        borderWidth = UIManager.getInt("Component.borderWidth");
        minimumWidth = UIManager.getInt("Button.minimumWidth");
        if (minimumWidth <= 0) {
            minimumWidth = 72;
        }

        // Disabled opacity
        Object opacityObj = UIManager.get("Component.disabledOpacity");
        if (opacityObj instanceof Number num) {
            disabledOpacity = num.floatValue();
        } else {
            disabledOpacity = 0.6f;
        }

        // Enable rollover for hover state tracking
        b.setRolloverEnabled(true);

        // Set opaque to false (respects UIResource contract)
        LookAndFeel.installProperty(b, "opaque", false);

        // Install DwcButtonBorder if the current border is null or a UIResource
        if (b.getBorder() == null || b.getBorder() instanceof UIResource) {
            b.setBorder(new DwcButtonBorder());
        }
    }

    @Override
    protected void uninstallDefaults(AbstractButton b) {
        super.uninstallDefaults(b);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            int width = c.getWidth();
            int height = c.getHeight();
            boolean primary = isPrimary(b);

            int fw = focusWidth;
            int bw = borderWidth;

            // Content area (inside focus ring reservation)
            float cx = fw;
            float cy = fw;
            float cw = width - fw * 2;
            float ch = height - fw * 2;

            // 1. Paint background
            if (b.isContentAreaFilled()) {
                Color bg = resolveBackground(b, primary);
                if (!b.isEnabled()) {
                    StateColorResolver.paintWithOpacity(g2, disabledOpacity, () -> {
                        PaintUtils.paintRoundedBackground(g2, cx, cy, cw, ch, arc, bg);
                    });
                } else {
                    PaintUtils.paintRoundedBackground(g2, cx, cy, cw, ch, arc, bg);
                }
            }

            // 2. Border painted by DwcButtonBorder (not here)

            // 3. Layout icon + text
            g2.setFont(b.getFont());
            FontMetrics fm = g2.getFontMetrics();
            Rectangle viewRect = new Rectangle();
            Rectangle iconRect = new Rectangle();
            Rectangle textRect = new Rectangle();
            Insets insets = b.getInsets();
            viewRect.x = insets.left;
            viewRect.y = insets.top;
            viewRect.width = width - (insets.left + insets.right);
            viewRect.height = height - (insets.top + insets.bottom);

            String text = SwingUtilities.layoutCompoundLabel(
                    c, fm, b.getText(), b.getIcon(),
                    b.getVerticalAlignment(), b.getHorizontalAlignment(),
                    b.getVerticalTextPosition(), b.getHorizontalTextPosition(),
                    viewRect, iconRect, textRect,
                    b.getText() == null ? 0 : b.getIconTextGap()
            );

            // 4. Paint icon
            if (b.getIcon() != null) {
                Icon icon = getStateIcon(b);
                if (icon != null) {
                    if (!b.isEnabled()) {
                        StateColorResolver.paintWithOpacity(g2, disabledOpacity, () -> {
                            icon.paintIcon(c, g2, iconRect.x, iconRect.y);
                        });
                    } else {
                        icon.paintIcon(c, g2, iconRect.x, iconRect.y);
                    }
                }
            }

            // 5. Paint text
            if (text != null && !text.isEmpty()) {
                Color fg = resolveForeground(b, primary);
                if (!b.isEnabled()) {
                    fg = disabledText != null ? disabledText : foreground;
                    g2.setComposite(AlphaComposite.getInstance(
                            AlphaComposite.SRC_OVER, disabledOpacity));
                }
                g2.setColor(fg);
                BasicGraphicsUtils.drawStringUnderlineCharAt(g2, text,
                        b.getDisplayedMnemonicIndex(),
                        textRect.x, textRect.y + fm.getAscent());
            }

            // 6. Paint focus ring
            if (b.isFocusPainted() && b.hasFocus()) {
                Color ringColor = focusRingColor;
                if (ringColor != null) {
                    FocusRingPainter.paintFocusRing(g2, cx, cy, cw, ch,
                            arc, fw, ringColor);
                }
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Resolves the background color based on button state and variant.
     */
    private Color resolveBackground(AbstractButton b, boolean primary) {
        if (primary) {
            return StateColorResolver.resolve(b, defaultBackground, null, null,
                    defaultHoverBackground, defaultPressedBackground);
        }
        return StateColorResolver.resolve(b, background, null, null,
                hoverBackground, pressedBackground);
    }

    /**
     * Resolves the foreground color based on variant.
     */
    private Color resolveForeground(AbstractButton b, boolean primary) {
        if (primary) {
            return defaultForeground != null ? defaultForeground : foreground;
        }
        return foreground;
    }

    /**
     * Returns whether the button is a primary variant.
     */
    private boolean isPrimary(AbstractButton b) {
        return "primary".equals(b.getClientProperty("dwc.buttonType"));
    }

    /**
     * Selects the correct icon for the button's current state.
     */
    private Icon getStateIcon(AbstractButton b) {
        ButtonModel model = b.getModel();
        if (!model.isEnabled()) {
            Icon disabled = b.getDisabledIcon();
            return disabled != null ? disabled : b.getIcon();
        }
        if (model.isArmed() && model.isPressed()) {
            Icon pressed = b.getPressedIcon();
            return pressed != null ? pressed : b.getIcon();
        }
        if (model.isRollover()) {
            Icon rollover = b.getRolloverIcon();
            return rollover != null ? rollover : b.getIcon();
        }
        return b.getIcon();
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension d = super.getPreferredSize(c);
        if (d == null) {
            return null;
        }

        // Enforce minimum width only if the button has text (not icon-only)
        AbstractButton b = (AbstractButton) c;
        if (b.getText() != null && !b.getText().isEmpty()) {
            d.width = Math.max(d.width, minimumWidth);
        }

        // Enforce minimum height (DWC --dwc-size-m = 2.25rem = 36px)
        d.height = Math.max(d.height, 36);

        return d;
    }

    @Override
    public Dimension getMinimumSize(JComponent c) {
        return getPreferredSize(c);
    }
}
