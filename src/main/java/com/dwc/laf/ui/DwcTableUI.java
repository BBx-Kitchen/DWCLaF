package com.dwc.laf.ui;

import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTableUI;
import java.awt.Dimension;

/**
 * Custom UI delegate for JTable that provides a modern DWC-styled appearance.
 *
 * <p>This delegate disables grid lines for a clean look, sets a minimum 24px
 * row height, fills the viewport height, and installs custom renderers for
 * both cells ({@link DwcTableCellRenderer}) and headers
 * ({@link DwcTableHeaderRenderer}).</p>
 *
 * <p>No paint() override is needed -- all visual customization is handled
 * through the renderer-based approach for maintainability.</p>
 */
public class DwcTableUI extends BasicTableUI {

    public static ComponentUI createUI(JComponent c) {
        return new DwcTableUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();

        // Disable grid lines for modern clean look
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));

        // Minimum 24px row height for comfortable spacing
        table.setRowHeight(Math.max(table.getRowHeight(), 24));

        // Fill viewport even with few rows (important for consistent background)
        table.setFillsViewportHeight(true);

        // Install custom cell renderer for row striping and selection highlighting
        table.setDefaultRenderer(Object.class, new DwcTableCellRenderer());

        // Install custom header renderer with bottom separator
        if (table.getTableHeader() != null) {
            table.getTableHeader().setDefaultRenderer(new DwcTableHeaderRenderer());
        }

        // Set selection colors from UIDefaults
        table.setSelectionBackground(UIManager.getColor("Table.selectionBackground"));
        table.setSelectionForeground(UIManager.getColor("Table.selectionForeground"));
    }

    @Override
    protected void uninstallDefaults() {
        super.uninstallDefaults();
    }
}
