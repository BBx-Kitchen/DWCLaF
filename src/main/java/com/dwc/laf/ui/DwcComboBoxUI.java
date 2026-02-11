package com.dwc.laf.ui;

import com.dwc.laf.painting.FocusRingPainter;
import com.dwc.laf.painting.PaintUtils;
import com.dwc.laf.painting.StateColorResolver;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Path2D;

/**
 * A custom {@link javax.swing.plaf.ComboBoxUI} delegate that paints JComboBox
 * components with a custom chevron arrow button, themed popup list renderer,
 * hover/focus state tracking, and rounded background based on DWC CSS design tokens.
 *
 * <p>The paint pipeline in {@link #paint(Graphics, JComponent)} follows this order:
 * <ol>
 *   <li>Apply disabled opacity (if not enabled)</li>
 *   <li>Paint rounded background (inside focus ring reservation)</li>
 *   <li>Call {@code super.paint()} for current value text/icon</li>
 *   <li>Paint focus ring (when component has focus)</li>
 * </ol>
 *
 * <p>Hover state is tracked via a {@link MouseListener} that sets the
 * {@code "DwcTextFieldUI.hover"} client property, which is read by
 * {@link DwcTextFieldBorder} for border color resolution (same pattern as
 * Phase 5 TextField).</p>
 *
 * <p>Each JComboBox gets its own instance (not a shared singleton) to prevent
 * hover state leaking between combo boxes.</p>
 */
public class DwcComboBoxUI extends BasicComboBoxUI {

    // Colors cached from UIDefaults
    private Color background;
    private Color foreground;
    private Color borderColor;
    private Color hoverBorderColor;
    private Color selectionBackground;
    private Color selectionForeground;
    private Color popupBackground;
    private Color arrowColor;

    // Dimensions cached from UIDefaults
    private int arc;
    private int focusWidth;
    private int borderWidth;

    // Disabled opacity
    private float disabledOpacity;

    // State tracking
    private boolean hover;
    private MouseListener hoverListener;

    /**
     * Creates a new per-component DwcComboBoxUI instance.
     *
     * @param c the component (unused, required by the L&F contract)
     * @return a new DwcComboBoxUI instance
     */
    public static ComponentUI createUI(JComponent c) {
        return new DwcComboBoxUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();

        // Required for rounded corners; prevents rectangular background artifact.
        LookAndFeel.installProperty(comboBox, "opaque", false);

        // Cache all colors from UIManager
        background = UIManager.getColor("ComboBox.background");
        foreground = UIManager.getColor("ComboBox.foreground");
        borderColor = UIManager.getColor("ComboBox.borderColor");
        hoverBorderColor = UIManager.getColor("ComboBox.hoverBorderColor");
        selectionBackground = UIManager.getColor("ComboBox.selectionBackground");
        selectionForeground = UIManager.getColor("ComboBox.selectionForeground");
        popupBackground = UIManager.getColor("ComboBox.popupBackground");
        arrowColor = UIManager.getColor("ComboBox.buttonArrowColor");

        // Cache dimensions
        arc = UIManager.getInt("ComboBox.arc");
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

        // Install custom renderer for themed selection highlight
        comboBox.setRenderer(new DwcComboBoxRenderer());
    }

    @Override
    protected void installListeners() {
        super.installListeners();

        // Hover listener: sets client property for DwcTextFieldBorder to read
        hoverListener = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                comboBox.putClientProperty("DwcTextFieldUI.hover", Boolean.TRUE);
                comboBox.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                comboBox.putClientProperty("DwcTextFieldUI.hover", null);
                comboBox.repaint();
            }
        };
        comboBox.addMouseListener(hoverListener);
    }

    @Override
    protected void uninstallListeners() {
        if (hoverListener != null) {
            comboBox.removeMouseListener(hoverListener);
            hoverListener = null;
        }
        super.uninstallListeners();
    }

    @Override
    protected JButton createArrowButton() {
        return new DwcComboBoxArrowButton();
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            float px = focusWidth;
            float py = focusWidth;
            float pw = c.getWidth() - focusWidth * 2f;
            float ph = c.getHeight() - focusWidth * 2f;

            if (!c.isEnabled()) {
                StateColorResolver.paintWithOpacity(g2, disabledOpacity, () -> {
                    PaintUtils.paintRoundedBackground(g2, px, py, pw, ph, arc, background);
                });
            } else {
                // Paint rounded background
                PaintUtils.paintRoundedBackground(g2, px, py, pw, ph, arc, background);
            }

            // Let BasicComboBoxUI paint current value (text/icon)
            super.paint(g2, c);

            // Paint focus ring on top
            if (comboBox.hasFocus() && comboBox.isEnabled()) {
                Color focusRingColor = UIManager.getColor("Component.focusRingColor");
                FocusRingPainter.paintFocusRing(g2, px, py, pw, ph, arc,
                        focusWidth, focusRingColor);
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
        // Override to prevent BasicComboBoxUI from setting foreground to
        // selectionForeground (white) when focused — the display area has no
        // selection highlight background so white text is unreadable.
        ListCellRenderer<Object> renderer = comboBox.getRenderer();
        Component c = renderer.getListCellRendererComponent(
                listBox, comboBox.getSelectedItem(), -1, false, false);
        c.setFont(comboBox.getFont());
        c.setForeground(comboBox.isEnabled()
                ? comboBox.getForeground()
                : UIManager.getColor("ComboBox.disabledForeground"));
        c.setBackground(comboBox.getBackground());
        boolean shouldValidate = c instanceof JPanel;
        currentValuePane.paintComponent(g, c, comboBox,
                bounds.x, bounds.y, bounds.width, bounds.height, shouldValidate);
    }

    @Override
    public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
        // No-op: background painted in paint() method above
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension d = super.getPreferredSize(c);
        if (d != null) {
            d.height = Math.max(d.height, 36);
        }
        return d;
    }

    @Override
    public Dimension getMinimumSize(JComponent c) {
        Dimension d = super.getMinimumSize(c);
        if (d != null) {
            d.height = Math.max(d.height, 36);
        }
        return d;
    }

    // ---- Inner classes ----

    /**
     * Custom arrow button that paints a chevron (downward-pointing) with a
     * 1px vertical separator line on its left edge, matching the DWC web
     * combobox suffix area appearance.
     *
     * <p>The separator line mirrors the DWC SCSS {@code [part='suffix-separator']}
     * rule: 1px wide, colored with {@code --dwc-color-default-dark}, inset
     * 4px from top and bottom ({@code margin: var(--dwc-space-xs) 0}).</p>
     *
     * <p>The chevron is drawn as a stroked {@link Path2D} path with rounded
     * caps and joins for a smooth appearance. Arrow size is 6px with 1.2f
     * stroke width for a compact, subtle look matching DWC web proportions.</p>
     */
    private static class DwcComboBoxArrowButton extends JButton {

        DwcComboBoxArrowButton() {
            setName("ComboBox.arrowButton");
            setRequestFocusEnabled(false);
            setFocusable(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder());
            setContentAreaFilled(false);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(24, 24);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                Object[] saved = PaintUtils.setupPaintingHints(g2);

                int w = getWidth();
                int h = getHeight();

                // 1px separator line on the left edge, inset 4px top/bottom
                // Mirrors DWC [part='suffix-separator']: width 1px,
                // background-color var(--dwc-color-default-dark),
                // margin var(--dwc-space-xs) 0
                Color separatorColor = UIManager.getColor("ComboBox.buttonArrowColor");
                if (separatorColor == null) {
                    separatorColor = Color.DARK_GRAY;
                }
                g2.setColor(separatorColor);
                g2.fillRect(0, 4, 1, h - 8);

                // Chevron arrow (downward-pointing)
                Color chevronColor = separatorColor;
                g2.setColor(chevronColor);

                float arrowSize = 6f;
                float cx = w / 2f;
                float cy = h / 2f;
                Path2D.Float arrow = new Path2D.Float();
                arrow.moveTo(cx - arrowSize / 2, cy - arrowSize / 4);
                arrow.lineTo(cx, cy + arrowSize / 4);
                arrow.lineTo(cx + arrowSize / 2, cy - arrowSize / 4);
                g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                g2.draw(arrow);

                PaintUtils.restorePaintingHints(g2, saved);
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * Custom list cell renderer for the combobox popup that applies DWC-themed
     * selection highlight colors (primary blue background, white text for
     * selected items).
     */
    private static class DwcComboBoxRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            // Let super handle the basics (text, icon, etc.)
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            // index == -1 means the display area (not popup list).
            // Must be non-opaque so the renderer doesn't paint its own
            // background rectangle over the custom rounded background.
            // Also use normal foreground (not selection foreground) since the
            // display area has no selection highlight background.
            setOpaque(index != -1);

            if (index == -1) {
                Color fg = UIManager.getColor("ComboBox.foreground");
                setForeground(fg != null ? fg : list.getForeground());
                setBackground(list.getBackground());
            } else if (isSelected) {
                Color selBg = UIManager.getColor("ComboBox.selectionBackground");
                Color selFg = UIManager.getColor("ComboBox.selectionForeground");
                setBackground(selBg != null ? selBg : list.getSelectionBackground());
                setForeground(selFg != null ? selFg : list.getSelectionForeground());
            } else {
                Color popBg = UIManager.getColor("ComboBox.popupBackground");
                Color fg = UIManager.getColor("ComboBox.foreground");
                setBackground(popBg != null ? popBg : list.getBackground());
                setForeground(fg != null ? fg : list.getForeground());
            }

            // Padding for list items
            setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            return this;
        }
    }
}
