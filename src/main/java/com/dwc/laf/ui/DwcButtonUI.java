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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A custom {@link javax.swing.plaf.ButtonUI} delegate that paints JButton
 * components with rounded backgrounds, borders, icons, text, and focus rings
 * based on DWC CSS design tokens.
 *
 * <p>Supports six visual variants:
 * <ul>
 *   <li><b>Default:</b> neutral background/foreground colors</li>
 *   <li><b>Primary:</b> accent-colored background/foreground, activated via
 *       {@code button.putClientProperty("dwc.buttonType", "primary")}</li>
 *   <li><b>Success:</b> green background from {@code --dwc-color-success}</li>
 *   <li><b>Danger:</b> red background from {@code --dwc-color-danger}</li>
 *   <li><b>Warning:</b> amber background from {@code --dwc-color-warning}</li>
 *   <li><b>Info:</b> info-colored background from {@code --dwc-color-info}</li>
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

    /**
     * Encapsulates all colors for one button variant.
     */
    private record VariantColors(
            Color background,
            Color foreground,
            Color hoverBackground,
            Color pressedBackground,
            Color focusRingColor
    ) {}

    // Variant color map keyed by variant name
    private Map<String, VariantColors> variantColors;

    // Disabled text color
    private Color disabledText;

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

        // Build variant color map
        variantColors = new HashMap<>();
        List<String> variants = List.of("default", "primary", "success", "danger", "warning", "info");
        for (String variant : variants) {
            // UIDefaults prefix: "default" variant uses "Button" (no suffix),
            // "primary" uses "Button.default" (Swing convention: default button = primary),
            // others use "Button.{variant}"
            String prefix = switch (variant) {
                case "default" -> "Button";
                case "primary" -> "Button.default";
                default -> "Button." + variant;
            };

            Color bg = UIManager.getColor(prefix + ".background");
            Color fg = UIManager.getColor(prefix + ".foreground");
            Color hoverBg = UIManager.getColor(prefix + ".hoverBackground");
            Color pressedBg = UIManager.getColor(prefix + ".pressedBackground");

            // Focus ring: "default" uses global, others use variant-specific
            Color focusRing = switch (variant) {
                case "default" -> UIManager.getColor("Component.focusRingColor");
                case "primary" -> {
                    Color c2 = UIManager.getColor("Component.focusRingColor.primary");
                    yield c2 != null ? c2 : UIManager.getColor("Component.focusRingColor");
                }
                default -> {
                    Color c2 = UIManager.getColor("Component.focusRingColor." + variant);
                    yield c2 != null ? c2 : UIManager.getColor("Component.focusRingColor");
                }
            };

            variantColors.put(variant, new VariantColors(bg, fg, hoverBg, pressedBg, focusRing));
        }

        // Disabled text
        disabledText = UIManager.getColor("Button.disabledText");

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
            String variant = getVariant(b);
            VariantColors vc = variantColors.getOrDefault(variant, variantColors.get("default"));

            int fw = focusWidth;

            // Content area (inside focus ring reservation)
            float cx = fw;
            float cy = fw;
            float cw = width - fw * 2;
            float ch = height - fw * 2;

            // 1. Paint background
            // Always paint: opaque/contentAreaFilled is set to false so that
            // Swing's default rectangular fill doesn't paint under our rounded
            // corners. BBj ties opaque→contentAreaFilled, so checking that
            // flag here would skip our own background painting.
            Color bg = resolveBackground(b);
            if (!b.isEnabled()) {
                StateColorResolver.paintWithOpacity(g2, disabledOpacity, () -> {
                    PaintUtils.paintRoundedBackground(g2, cx, cy, cw, ch, arc, bg);
                });
            } else {
                PaintUtils.paintRoundedBackground(g2, cx, cy, cw, ch, arc, bg);
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
                Color fg = resolveForeground(b);
                if (!b.isEnabled()) {
                    VariantColors defVc = variantColors.get("default");
                    fg = disabledText != null ? disabledText
                            : (defVc != null ? defVc.foreground() : fg);
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
                Color ringColor = vc != null ? vc.focusRingColor() : null;
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
    private Color resolveBackground(AbstractButton b) {
        String variant = getVariant(b);
        VariantColors vc = variantColors.getOrDefault(variant, variantColors.get("default"));
        return StateColorResolver.resolve(b, vc.background(), null, null,
                vc.hoverBackground(), vc.pressedBackground());
    }

    /**
     * Resolves the foreground color based on variant.
     */
    private Color resolveForeground(AbstractButton b) {
        String variant = getVariant(b);
        VariantColors vc = variantColors.getOrDefault(variant, variantColors.get("default"));
        VariantColors defVc = variantColors.get("default");
        Color fg = vc.foreground();
        return fg != null ? fg : (defVc != null ? defVc.foreground() : null);
    }

    /**
     * Returns the variant name for the button based on the {@code dwc.buttonType}
     * client property. Returns {@code "default"} if the property is null or unrecognized.
     */
    private String getVariant(AbstractButton b) {
        Object prop = b.getClientProperty("dwc.buttonType");
        if (prop instanceof String s && variantColors.containsKey(s)) {
            return s;
        }
        return "default";
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
