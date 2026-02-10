package com.dwc.laf.ui;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

/**
 * Custom table header cell renderer that provides bold text with a styled
 * bottom separator line.
 *
 * <p>The header text is rendered in bold with {@code TableHeader.foreground}
 * color. A bottom separator is painted using a compound border with
 * {@code TableHeader.bottomSeparatorColor} (mapped from
 * {@code --dwc-color-default-dark}).</p>
 */
public class DwcTableHeaderRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        Color fg = UIManager.getColor("TableHeader.foreground");
        setForeground(fg != null ? fg : table.getForeground());

        setFont(table.getFont().deriveFont(Font.BOLD));
        setHorizontalAlignment(SwingConstants.LEADING);

        Color separatorColor = UIManager.getColor("TableHeader.bottomSeparatorColor");
        if (separatorColor == null) {
            separatorColor = Color.GRAY;
        }
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, separatorColor),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));

        setBackground(UIManager.getColor("control"));

        return this;
    }
}
