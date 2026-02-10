package com.dwc.laf;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Comprehensive demo gallery showcasing all 8 DWC-themed Swing components.
 *
 * <p>Each component is displayed in a labeled section with normal, focused,
 * and disabled states shown statically. Hover and pressed states are
 * interactive -- move the mouse over components to see them.</p>
 *
 * <p>Run with: {@code mvn compile exec:java}</p>
 */
public class DwcComponentGallery {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new DwcLookAndFeel());
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            JFrame frame = new JFrame("DWC Component Gallery");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(900, 800);

            JPanel main = new JPanel();
            main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
            main.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

            // Title
            JLabel title = new JLabel("DWC Look & Feel - Component Gallery");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            main.add(title);
            main.add(Box.createVerticalStrut(8));

            // Subtitle
            JLabel subtitle = new JLabel(
                    "All 8 themed components. Hover and press to see interactive states.");
            subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            main.add(subtitle);
            main.add(Box.createVerticalStrut(16));

            // Theme switcher
            JPanel themeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            themeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            themeRow.add(new JLabel("Theme:"));
            JComboBox<String> themeSelector = new JComboBox<>(
                    new String[]{"Default (bundled)", "Theme 1", "Theme 2"});
            themeSelector.addActionListener((ActionEvent e) -> {
                int idx = themeSelector.getSelectedIndex();
                switch (idx) {
                    case 0 -> System.clearProperty("dwc.theme");
                    case 1 -> System.setProperty("dwc.theme", "css/theme1.css");
                    case 2 -> System.setProperty("dwc.theme", "css/theme2.css");
                }
                try {
                    int savedIdx = idx;
                    UIManager.setLookAndFeel(new DwcLookAndFeel());
                    SwingUtilities.updateComponentTreeUI(
                            SwingUtilities.getWindowAncestor(themeSelector));
                    SwingUtilities.invokeLater(() -> themeSelector.setSelectedIndex(savedIdx));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            themeRow.add(themeSelector);
            main.add(themeRow);
            main.add(Box.createVerticalStrut(16));

            // 8 component sections
            main.add(createButtonSection());
            main.add(Box.createVerticalStrut(24));
            main.add(createTextFieldSection());
            main.add(Box.createVerticalStrut(24));
            main.add(createCheckBoxSection());
            main.add(Box.createVerticalStrut(24));
            main.add(createRadioButtonSection());
            main.add(Box.createVerticalStrut(24));
            main.add(createComboBoxSection());
            main.add(Box.createVerticalStrut(24));
            main.add(createLabelSection());
            main.add(Box.createVerticalStrut(24));
            main.add(createPanelSection());
            main.add(Box.createVerticalStrut(24));
            main.add(createTabbedPaneSection());

            // Wrap in scroll pane
            JScrollPane scrollPane = new JScrollPane(main);
            scrollPane.setBorder(null);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);

            frame.setContentPane(scrollPane);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    // ---- Section 1: JButton ----

    private static JPanel createButtonSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder("JButton"));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        // State labels
        JPanel labelRow = new JPanel(new GridLayout(1, 3, 8, 0));
        labelRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        labelRow.add(new JLabel("Normal"));
        labelRow.add(new JLabel("Focused"));
        labelRow.add(new JLabel("Disabled"));
        section.add(labelRow);
        section.add(Box.createVerticalStrut(4));

        // Default variant row
        JPanel defaultLabel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        defaultLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel dlbl = new JLabel("Default variant:");
        dlbl.setFont(dlbl.getFont().deriveFont(Font.ITALIC));
        defaultLabel.add(dlbl);
        section.add(defaultLabel);
        section.add(Box.createVerticalStrut(4));

        JPanel defaultRow = new JPanel(new GridLayout(1, 3, 8, 0));
        defaultRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton defNormal = new JButton("Default");
        JButton defFocused = new JButton("Focused");
        // requestFocusInWindow will take effect after the frame is visible
        SwingUtilities.invokeLater(defFocused::requestFocusInWindow);
        JButton defDisabled = new JButton("Disabled");
        defDisabled.setEnabled(false);

        defaultRow.add(defNormal);
        defaultRow.add(defFocused);
        defaultRow.add(defDisabled);
        section.add(defaultRow);
        section.add(Box.createVerticalStrut(8));

        // Primary variant row
        JPanel primaryLabel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        primaryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel plbl = new JLabel("Primary variant:");
        plbl.setFont(plbl.getFont().deriveFont(Font.ITALIC));
        primaryLabel.add(plbl);
        section.add(primaryLabel);
        section.add(Box.createVerticalStrut(4));

        JPanel primaryRow = new JPanel(new GridLayout(1, 3, 8, 0));
        primaryRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton priNormal = new JButton("Primary");
        priNormal.putClientProperty("dwc.buttonType", "primary");
        JButton priFocused = new JButton("Focused");
        priFocused.putClientProperty("dwc.buttonType", "primary");
        JButton priDisabled = new JButton("Disabled");
        priDisabled.putClientProperty("dwc.buttonType", "primary");
        priDisabled.setEnabled(false);

        primaryRow.add(priNormal);
        primaryRow.add(priFocused);
        primaryRow.add(priDisabled);
        section.add(primaryRow);
        section.add(Box.createVerticalStrut(8));

        // Semantic variant rows
        addVariantRow(section, "success", "Success");
        addVariantRow(section, "danger", "Danger");
        addVariantRow(section, "warning", "Warning");
        addVariantRow(section, "info", "Info");

        return section;
    }

    private static void addVariantRow(JPanel section, String variant, String label) {
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        labelPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbl = new JLabel(label + " variant:");
        lbl.setFont(lbl.getFont().deriveFont(Font.ITALIC));
        labelPanel.add(lbl);
        section.add(labelPanel);
        section.add(Box.createVerticalStrut(4));

        JPanel row = new JPanel(new GridLayout(1, 3, 8, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton normal = new JButton(label);
        normal.putClientProperty("dwc.buttonType", variant);
        JButton disabled = new JButton("Disabled");
        disabled.putClientProperty("dwc.buttonType", variant);
        disabled.setEnabled(false);

        row.add(normal);
        row.add(new JLabel(""));
        row.add(disabled);
        section.add(row);
        section.add(Box.createVerticalStrut(8));
    }

    // ---- Section 2: JTextField ----

    private static JPanel createTextFieldSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder("JTextField"));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField placeholder = new JTextField(12);
        placeholder.putClientProperty("JTextField.placeholderText", "Placeholder...");
        JTextField filled = new JTextField("Filled text", 12);
        JTextField disabled = new JTextField("Disabled", 12);
        disabled.setEnabled(false);

        row.add(placeholder);
        row.add(filled);
        row.add(disabled);
        section.add(row);

        return section;
    }

    // ---- Section 3: JCheckBox ----

    private static JPanel createCheckBoxSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder("JCheckBox"));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JCheckBox unchecked = new JCheckBox("Unchecked");
        JCheckBox checked = new JCheckBox("Checked");
        checked.setSelected(true);
        JCheckBox disabledUnchecked = new JCheckBox("Disabled");
        disabledUnchecked.setEnabled(false);
        JCheckBox disabledChecked = new JCheckBox("Disabled Checked");
        disabledChecked.setSelected(true);
        disabledChecked.setEnabled(false);

        row.add(unchecked);
        row.add(checked);
        row.add(disabledUnchecked);
        row.add(disabledChecked);
        section.add(row);

        return section;
    }

    // ---- Section 4: JRadioButton ----

    private static JPanel createRadioButtonSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder("JRadioButton"));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        ButtonGroup bg = new ButtonGroup();
        JRadioButton unselected = new JRadioButton("Unselected");
        JRadioButton selected = new JRadioButton("Selected");
        selected.setSelected(true);
        JRadioButton disabled = new JRadioButton("Disabled");
        disabled.setEnabled(false);

        bg.add(unselected);
        bg.add(selected);
        bg.add(disabled);

        row.add(unselected);
        row.add(selected);
        row.add(disabled);
        section.add(row);

        return section;
    }

    // ---- Section 5: JComboBox ----

    private static JPanel createComboBoxSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder("JComboBox"));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JComboBox<String> normal = new JComboBox<>(
                new String[]{"Select...", "Apple", "Banana", "Cherry"});
        JComboBox<String> disabledCombo = new JComboBox<>(
                new String[]{"Disabled"});
        disabledCombo.setEnabled(false);

        row.add(normal);
        row.add(disabledCombo);
        section.add(row);

        return section;
    }

    // ---- Section 6: JLabel ----

    private static JPanel createLabelSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder("JLabel"));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel normalLabel = new JLabel("Normal label");
        JLabel disabledLabel = new JLabel("Disabled label");
        disabledLabel.setEnabled(false);

        row.add(normalLabel);
        row.add(disabledLabel);
        section.add(row);

        return section;
    }

    // ---- Section 7: JPanel ----

    private static JPanel createPanelSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder("JPanel"));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Normal panel
        JPanel normalPanel = new JPanel(new BorderLayout());
        normalPanel.setPreferredSize(new Dimension(200, 80));
        normalPanel.add(new JLabel("Normal Panel"), BorderLayout.CENTER);
        row.add(normalPanel);

        // Card-mode panel with shadow clipping protection
        JPanel cardWrapper = new JPanel(new BorderLayout());
        cardWrapper.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel cardPanel = new JPanel(new BorderLayout());
        cardPanel.putClientProperty("dwc.panelStyle", "card");
        cardPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        cardPanel.setPreferredSize(new Dimension(200, 80));
        cardPanel.add(new JLabel("Card Panel"), BorderLayout.CENTER);

        cardWrapper.add(cardPanel, BorderLayout.CENTER);
        row.add(cardWrapper);

        section.add(row);

        return section;
    }

    // ---- Section 8: JTabbedPane ----

    private static JPanel createTabbedPaneSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder("JTabbedPane"));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTabbedPane tabs = new JTabbedPane();

        JPanel activeContent = new JPanel(new BorderLayout());
        activeContent.setPreferredSize(new Dimension(400, 60));
        activeContent.add(new JLabel("  Active tab content"), BorderLayout.CENTER);
        tabs.addTab("Active", activeContent);

        JPanel normalContent = new JPanel(new BorderLayout());
        normalContent.setPreferredSize(new Dimension(400, 60));
        normalContent.add(new JLabel("  Normal tab content"), BorderLayout.CENTER);
        tabs.addTab("Normal", normalContent);

        JPanel disabledContent = new JPanel(new BorderLayout());
        disabledContent.setPreferredSize(new Dimension(400, 60));
        disabledContent.add(new JLabel("  Disabled tab content"), BorderLayout.CENTER);
        tabs.addTab("Disabled", disabledContent);
        tabs.setEnabledAt(2, false);

        // Prevent BoxLayout stretch
        tabs.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabs.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                tabs.getPreferredSize().height));

        section.add(tabs);

        return section;
    }
}
