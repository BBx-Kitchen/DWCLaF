package com.dwc.laf.ui;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;

/**
 * Custom table cell renderer that provides alternating row striping and
 * primary-colored selection highlighting for JTable.
 *
 * <p>Even rows use {@code Table.background}, odd rows use
 * {@code Table.alternateRowColor} (mapped from {@code --dwc-surface-3}).
 * Selected rows use {@code Table.selectionBackground} and
 * {@code Table.selectionForeground} (mapped from primary color tokens).</p>
 *
 * <p>The focus border is replaced with a simple empty border for a cleaner
 * modern appearance.</p>
 */
public class DwcTableCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (isSelected) {
            setBackground(UIManager.getColor("Table.selectionBackground"));
            setForeground(UIManager.getColor("Table.selectionForeground"));
        } else {
            Color bg = (row % 2 == 0)
                    ? UIManager.getColor("Table.background")
                    : UIManager.getColor("Table.alternateRowColor");
            setBackground(bg != null ? bg : table.getBackground());
            setForeground(table.getForeground());
        }

        // Remove focus border for cleaner look
        setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        return this;
    }
}
