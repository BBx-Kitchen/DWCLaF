package com.dwc.laf.ui;

import com.dwc.laf.painting.FocusRingPainter;
import com.dwc.laf.painting.PaintUtils;
import com.dwc.laf.painting.StateColorResolver;

import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;

/**
 * A custom {@link javax.swing.plaf.TabbedPaneUI} delegate that paints
 * JTabbedPane components with DWC-style underline tab indicators, hover
 * backgrounds, state-dependent text colors, and clean content borders.
 *
 * <p>Tab states:
 * <ul>
 *   <li><b>Normal:</b> transparent background, body-text foreground</li>
 *   <li><b>Hover:</b> subtle background tint, lighter text color</li>
 *   <li><b>Selected:</b> primary-colored underline indicator, primary foreground</li>
 *   <li><b>Disabled:</b> reduced opacity, muted underline color</li>
 * </ul>
 *
 * <p>Each JTabbedPane gets its own instance (not a shared singleton).</p>
 */
public class DwcTabbedPaneUI extends BasicTabbedPaneUI {

    // Tab text colors
    private Color foreground;
    private Color selectedForeground;
    private Color hoverForeground;
    private Color disabledForeground;

    // Underline indicator colors
    private Color underlineColor;
    private Color disabledUnderlineColor;

    // Background colors
    private Color hoverBackground;
    private Color contentAreaColor;

    // Focus
    private Color focusRingColor;

    // Dimensions
    private int underlineHeight;
    private int tabArc;

    // Opacity
    private float disabledOpacity;

    /**
     * Creates a new {@code DwcTabbedPaneUI} instance for the given component.
     * Returns a per-component instance (not a shared singleton).
     *
     * @param c the component (unused, but required by the L&F contract)
     * @return a new DwcTabbedPaneUI instance
     */
    public static ComponentUI createUI(JComponent c) {
        return new DwcTabbedPaneUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();

        // Cache all TabbedPane colors from UIManager
        foreground = UIManager.getColor("TabbedPane.foreground");
        selectedForeground = UIManager.getColor("TabbedPane.selectedForeground");
        hoverForeground = UIManager.getColor("TabbedPane.hoverForeground");
        disabledForeground = UIManager.getColor("TabbedPane.disabledForeground");
        underlineColor = UIManager.getColor("TabbedPane.underlineColor");
        disabledUnderlineColor = UIManager.getColor("TabbedPane.disabledUnderlineColor");
        hoverBackground = UIManager.getColor("TabbedPane.hoverBackground");
        contentAreaColor = UIManager.getColor("TabbedPane.contentAreaColor");
        focusRingColor = UIManager.getColor("Component.focusRingColor");

        // Disabled opacity
        Object opacityObj = UIManager.get("Component.disabledOpacity");
        if (opacityObj instanceof Number num) {
            disabledOpacity = num.floatValue();
        } else {
            disabledOpacity = 0.6f;
        }

        // Underline thickness
        underlineHeight = UIManager.getInt("TabbedPane.underlineHeight");
        if (underlineHeight <= 0) {
            underlineHeight = 3;
        }

        // Tab arc for hover background rounding
        tabArc = UIManager.getInt("Component.arc");

        // Tab insets for padding
        tabInsets = new Insets(8, 16, 8, 16);

        // Minimal content border (just separator + underline)
        contentBorderInsets = new Insets(0, 0, 0, 0);
    }

    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement,
            int tabIndex, int x, int y, int w, int h, boolean isSelected) {
        // Selected tabs: no background (shown via underline)
        if (isSelected) {
            return;
        }

        // Hovered tabs: paint rounded background tint
        if (getRolloverTab() == tabIndex) {
            if (hoverBackground != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    int inset = 2;
                    PaintUtils.paintRoundedBackground(g2,
                            x + inset, y + inset,
                            w - inset * 2, h - inset * 2,
                            tabArc, hoverBackground);
                } finally {
                    g2.dispose();
                }
            }
            return;
        }

        // Normal tabs: transparent (no background)
    }

    @Override
    protected void paintTabBorder(Graphics g, int tabPlacement,
            int tabIndex, int x, int y, int w, int h, boolean isSelected) {
        // No-op: DWC tabs have no individual tab borders
    }

    @Override
    protected void paintFocusIndicator(Graphics g, int tabPlacement,
            Rectangle[] rects, int tabIndex, Rectangle iconRect,
            Rectangle textRect, boolean isSelected) {
        if (tabPane.hasFocus() && tabIndex == getFocusIndex() && focusRingColor != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                Rectangle tabRect = rects[tabIndex];
                int inset = 2;
                FocusRingPainter.paintFocusRing(g2,
                        tabRect.x + inset, tabRect.y + inset,
                        tabRect.width - inset * 2, tabRect.height - inset * 2,
                        tabArc, 2, focusRingColor);
            } finally {
                g2.dispose();
            }
        }
    }

    @Override
    protected void paintContentBorderTopEdge(Graphics g, int tabPlacement,
            int selectedIndex, int x, int y, int w, int h) {
        if (tabPlacement == TOP) {
            paintContentEdgeWithUnderline(g, selectedIndex, x, y, w, true);
        } else {
            paintSeparatorLine(g, x, y, w, true);
        }
    }

    @Override
    protected void paintContentBorderBottomEdge(Graphics g, int tabPlacement,
            int selectedIndex, int x, int y, int w, int h) {
        if (tabPlacement == BOTTOM) {
            int edgeY = y + h - 1;
            paintContentEdgeWithUnderline(g, selectedIndex, x, edgeY, w, true);
        }
        // Non-matching edge: no-op for clean look
    }

    @Override
    protected void paintContentBorderLeftEdge(Graphics g, int tabPlacement,
            int selectedIndex, int x, int y, int w, int h) {
        if (tabPlacement == LEFT) {
            paintContentEdgeWithUnderline(g, selectedIndex, x, y, h, false);
        }
        // Non-matching edge: no-op for clean look
    }

    @Override
    protected void paintContentBorderRightEdge(Graphics g, int tabPlacement,
            int selectedIndex, int x, int y, int w, int h) {
        if (tabPlacement == RIGHT) {
            int edgeX = x + w - 1;
            paintContentEdgeWithUnderline(g, selectedIndex, edgeX, y, h, false);
        }
        // Non-matching edge: no-op for clean look
    }

    /**
     * Paints a content edge separator line and, if a tab is selected,
     * an underline indicator beneath/beside the selected tab.
     */
    private void paintContentEdgeWithUnderline(Graphics g, int selectedIndex,
            int edgeX, int edgeY, int length, boolean horizontal) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            // Paint 1px separator line
            if (contentAreaColor != null) {
                g2.setColor(contentAreaColor);
                if (horizontal) {
                    g2.fillRect(edgeX, edgeY, length, 1);
                } else {
                    g2.fillRect(edgeX, edgeY, 1, length);
                }
            }

            // Paint underline indicator for selected tab
            if (selectedIndex >= 0 && selectedIndex < rects.length) {
                Rectangle tabRect = rects[selectedIndex];
                boolean disabled = !tabPane.isEnabledAt(selectedIndex);
                Color ulColor = disabled ? disabledUnderlineColor : underlineColor;
                if (ulColor != null) {
                    g2.setColor(ulColor);
                    if (horizontal) {
                        g2.fillRect(tabRect.x, edgeY, tabRect.width, underlineHeight);
                    } else {
                        g2.fillRect(edgeX, tabRect.y, underlineHeight, tabRect.height);
                    }
                }
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Paints a simple 1px separator line (for non-matching edges).
     */
    private void paintSeparatorLine(Graphics g, int x, int y, int length,
            boolean horizontal) {
        if (contentAreaColor != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setColor(contentAreaColor);
                if (horizontal) {
                    g2.fillRect(x, y, length, 1);
                } else {
                    g2.fillRect(x, y, 1, length);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    @Override
    protected void paintText(Graphics g, int tabPlacement, Font font,
            FontMetrics metrics, int tabIndex, String title,
            Rectangle textRect, boolean isSelected) {
        // Resolve text color based on state
        Color textColor;
        boolean disabled = !tabPane.isEnabledAt(tabIndex);

        if (disabled) {
            textColor = disabledForeground != null ? disabledForeground : foreground;
        } else if (isSelected) {
            textColor = selectedForeground != null ? selectedForeground : foreground;
        } else if (getRolloverTab() == tabIndex) {
            textColor = hoverForeground != null ? hoverForeground : foreground;
        } else {
            textColor = foreground;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setFont(font);

            if (disabled) {
                g2.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, disabledOpacity));
            }

            if (textColor != null) {
                g2.setColor(textColor);
            }

            int mnemonicIndex = tabPane.getDisplayedMnemonicIndexAt(tabIndex);
            BasicGraphicsUtils.drawStringUnderlineCharAt(g2,
                    title, mnemonicIndex,
                    textRect.x, textRect.y + metrics.getAscent());
        } finally {
            g2.dispose();
        }
    }
}
