package com.dwc.laf.ui;

import com.dwc.laf.painting.FocusRingPainter;
import com.dwc.laf.painting.PaintUtils;

import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.text.JTextComponent;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * A custom {@link javax.swing.plaf.TextUI} delegate that paints JTextField
 * components with rounded backgrounds, hover/focus state tracking, placeholder
 * text, and focus ring rendering based on DWC CSS design tokens.
 *
 * <p>The paint pipeline in {@link #paintSafely(Graphics)} follows this order:
 * <ol>
 *   <li>Set disabled opacity composite (if not enabled)</li>
 *   <li>Paint rounded background (inside focus ring reservation)</li>
 *   <li>Call {@code super.paintSafely(g)} for text/caret/selection</li>
 *   <li>Paint placeholder text (when document is empty)</li>
 *   <li>Paint focus ring (when component has focus)</li>
 *   <li>Restore original composite (if disabled)</li>
 * </ol>
 *
 * <p>Hover state is tracked via a {@link MouseListener} that sets the
 * {@code "DwcTextFieldUI.hover"} client property, which is read by
 * {@link DwcTextFieldBorder} for border color resolution.</p>
 *
 * <p>Each JTextField gets its own instance (not a shared singleton) to prevent
 * hover state leaking between text fields.</p>
 */
public class DwcTextFieldUI extends BasicTextFieldUI {

    // Colors cached from UIDefaults
    private Color background;
    private Color foreground;
    private Color hoverBackground;
    private Color hoverBorderColor;
    private Color placeholderForeground;
    private Color focusRingColor;

    // Dimensions cached from UIDefaults
    private int arc;
    private int focusWidth;
    private int borderWidth;

    // Disabled opacity
    private float disabledOpacity;

    // State tracking
    private boolean hover;
    private MouseListener hoverListener;
    private FocusListener focusRepaintListener;

    /**
     * Creates a new {@code DwcTextFieldUI} instance for the given component.
     * Returns a per-component instance (not a shared singleton).
     *
     * @param c the component (unused, but required by the L&F contract)
     * @return a new DwcTextFieldUI instance
     */
    public static ComponentUI createUI(JComponent c) {
        return new DwcTextFieldUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();

        // Required for rounded corners; prevents rectangular background artifact.
        // Use installProperty so application-set values are preserved (UIResource contract).
        LookAndFeel.installProperty(getComponent(), "opaque", false);

        // Cache all colors from UIManager
        background = UIManager.getColor("TextField.background");
        foreground = UIManager.getColor("TextField.foreground");
        hoverBackground = UIManager.getColor("TextField.hoverBackground");
        hoverBorderColor = UIManager.getColor("TextField.hoverBorderColor");
        placeholderForeground = UIManager.getColor("TextField.placeholderForeground");
        focusRingColor = UIManager.getColor("Component.focusRingColor");

        // Cache dimensions
        arc = UIManager.getInt("TextField.arc");
        focusWidth = UIManager.getInt("Component.focusWidth");
        borderWidth = UIManager.getInt("Component.borderWidth");

        // Disabled opacity -- UIManager.getFloat returns 0 if key missing
        Object opacityObj = UIManager.get("Component.disabledOpacity");
        if (opacityObj instanceof Number num) {
            disabledOpacity = num.floatValue();
        } else {
            disabledOpacity = 0.4f;
        }
        if (disabledOpacity == 0f) {
            disabledOpacity = 0.4f;
        }
    }

    @Override
    protected void installListeners() {
        super.installListeners();

        JTextComponent c = getComponent();

        // Hover listener: sets client property for DwcTextFieldBorder to read
        hoverListener = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                c.putClientProperty("DwcTextFieldUI.hover", Boolean.TRUE);
                c.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                c.putClientProperty("DwcTextFieldUI.hover", Boolean.FALSE);
                c.repaint();
            }
        };
        c.addMouseListener(hoverListener);

        // Focus repaint listener: triggers border color update + focus ring paint
        focusRepaintListener = new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                c.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                c.repaint();
            }
        };
        c.addFocusListener(focusRepaintListener);
    }

    @Override
    protected void uninstallListeners() {
        super.uninstallListeners();

        JTextComponent c = getComponent();
        if (hoverListener != null) {
            c.removeMouseListener(hoverListener);
            hoverListener = null;
        }
        if (focusRepaintListener != null) {
            c.removeFocusListener(focusRepaintListener);
            focusRepaintListener = null;
        }
    }

    /**
     * Overridden as a no-op. Since opaque=false, {@code BasicTextUI.paintSafely()}
     * will not call this. But override defensively -- if something else calls it,
     * we don't want a rectangular fill.
     */
    @Override
    protected void paintBackground(Graphics g) {
        // No-op: background painting is handled in paintSafely()
    }

    @Override
    protected void paintSafely(Graphics g) {
        JTextComponent c = getComponent();

        // Handle disabled state: wrap entire paint in reduced opacity
        Composite oldComposite = null;
        if (!c.isEnabled() && g instanceof Graphics2D g2d) {
            oldComposite = g2d.getComposite();
            g2d.setComposite(AlphaComposite.SrcOver.derive(disabledOpacity));
        }

        // Step 1: Paint rounded background (before super, since opaque=false skips paintBackground)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                Object[] saved = PaintUtils.setupPaintingHints(g2);
                Color bg = resolveBackground(c);
                PaintUtils.paintRoundedBackground(g2, focusWidth, focusWidth,
                        c.getWidth() - focusWidth * 2, c.getHeight() - focusWidth * 2,
                        arc, bg);
                PaintUtils.restorePaintingHints(g2, saved);
            } finally {
                g2.dispose();
            }
        }

        // Step 2: Call super.paintSafely with the ORIGINAL Graphics object.
        // CRITICAL: Do NOT pass a g.create() clone to super. BasicTextUI.paintSafely()
        // sets up clip regions and accesses the component -- it needs the original context.
        super.paintSafely(g);

        // Step 3: Paint placeholder text (after super, layers correctly when document is empty)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                paintPlaceholder(g2, c);
            } finally {
                g2.dispose();
            }
        }

        // Step 4: Paint focus ring (on top of everything)
        if (c.hasFocus()) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                float fx = focusWidth;
                float fy = focusWidth;
                float fw = c.getWidth() - focusWidth * 2;
                float fh = c.getHeight() - focusWidth * 2;
                FocusRingPainter.paintFocusRing(g2, fx, fy, fw, fh,
                        arc, focusWidth, focusRingColor);
            } finally {
                g2.dispose();
            }
        }

        // Restore composite if we changed it for disabled state
        if (oldComposite != null && g instanceof Graphics2D g2d) {
            g2d.setComposite(oldComposite);
        }
    }

    /**
     * Resolves the correct background color based on component state.
     *
     * @param c the text component
     * @return the background color for the current state
     */
    private Color resolveBackground(JTextComponent c) {
        if (hover && !c.hasFocus()) {
            return hoverBackground != null ? hoverBackground : background;
        }
        return background;
    }

    /**
     * Paints placeholder text when the document is empty.
     *
     * @param g2 the graphics context
     * @param c  the text component
     */
    private void paintPlaceholder(Graphics2D g2, JTextComponent c) {
        if (c.getDocument().getLength() > 0) {
            return;
        }

        String placeholder = (String) c.getClientProperty("JTextField.placeholderText");
        if (placeholder == null || placeholder.isEmpty()) {
            return;
        }

        // Set color
        Color color = placeholderForeground != null ? placeholderForeground : Color.GRAY;
        g2.setColor(color);

        // Set font
        g2.setFont(c.getFont());

        // Enable text antialiasing
        Object desktopHints = c.getToolkit().getDesktopProperty("awt.font.desktophints");
        if (desktopHints instanceof java.util.Map<?, ?> hints) {
            g2.addRenderingHints((java.util.Map<?, ?>) hints);
        } else {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        }

        // Calculate position
        FontMetrics fm = g2.getFontMetrics();
        Insets insets = c.getInsets();
        int x = insets.left;
        int y = insets.top + fm.getAscent();

        // Clip to available width
        int availWidth = c.getWidth() - insets.left - insets.right;
        if (fm.stringWidth(placeholder) > availWidth && availWidth > 0) {
            // Truncate placeholder with ellipsis to fit available width
            String ellipsis = "...";
            int ellipsisWidth = fm.stringWidth(ellipsis);
            if (ellipsisWidth < availWidth) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < placeholder.length(); i++) {
                    if (fm.stringWidth(sb.toString() + placeholder.charAt(i)) + ellipsisWidth > availWidth) {
                        break;
                    }
                    sb.append(placeholder.charAt(i));
                }
                placeholder = sb + ellipsis;
            }
        }

        g2.drawString(placeholder, x, y);
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension size = super.getPreferredSize(c);
        if (size != null) {
            size.height = Math.max(size.height, 36);
        }
        return size;
    }
}
