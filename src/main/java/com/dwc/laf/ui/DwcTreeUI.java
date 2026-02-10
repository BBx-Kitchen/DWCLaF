package com.dwc.laf.ui;

import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.TreePath;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;

/**
 * A custom {@link javax.swing.plaf.TreeUI} delegate that paints JTree components
 * with themed selection highlighting, custom chevron expand/collapse icons, and
 * modern styling (no connecting lines, 24px row height).
 *
 * <p>Key visual features:
 * <ul>
 *   <li>Full-width primary-colored selection highlight for selected nodes</li>
 *   <li>Custom {@link DwcTreeExpandIcon} chevron icons for expand/collapse</li>
 *   <li>24px row height for comfortable spacing</li>
 *   <li>No connecting lines between nodes for clean modern look</li>
 * </ul>
 *
 * <p>Each JTree gets its own instance (not a shared singleton).</p>
 */
public class DwcTreeUI extends BasicTreeUI {

    private Color selectionBackground;
    private Color selectionForeground;
    private Color foreground;
    private Color background;

    /**
     * Creates a new {@code DwcTreeUI} instance for the given component.
     *
     * @param c the component (unused, required by L&F contract)
     * @return a new DwcTreeUI instance
     */
    public static ComponentUI createUI(JComponent c) {
        return new DwcTreeUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();

        selectionBackground = UIManager.getColor("Tree.selectionBackground");
        selectionForeground = UIManager.getColor("Tree.selectionForeground");
        foreground = UIManager.getColor("Tree.foreground");
        background = UIManager.getColor("Tree.background");

        // Install custom chevron icons
        setExpandedIcon(new DwcTreeExpandIcon(true));
        setCollapsedIcon(new DwcTreeExpandIcon(false));

        // Set row height
        int rowHeight = UIManager.getInt("Tree.rowHeight");
        tree.setRowHeight(rowHeight > 0 ? rowHeight : 24);
    }

    @Override
    protected void paintRow(Graphics g, Rectangle clipBounds, Insets insets,
                             Rectangle bounds, TreePath path, int row,
                             boolean isExpanded, boolean hasBeenExpanded, boolean isLeaf) {
        // Paint full-width selection background for selected nodes
        if (tree.isPathSelected(path)) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setColor(selectionBackground != null
                        ? selectionBackground
                        : UIManager.getColor("Tree.selectionBackground"));
                g2.fillRect(0, bounds.y, tree.getWidth(), bounds.height);
            } finally {
                g2.dispose();
            }
        }

        // Let BasicTreeUI paint the node content on top of our selection background
        super.paintRow(g, clipBounds, insets, bounds, path, row,
                isExpanded, hasBeenExpanded, isLeaf);
    }
}
