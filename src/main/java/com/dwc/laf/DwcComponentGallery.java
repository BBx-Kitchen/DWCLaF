package com.dwc.laf;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Comprehensive demo gallery showcasing all 13 DWC-themed Swing components.
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
                    "All 13 themed components. Hover and press to see interactive states.");
            subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            main.add(subtitle);
            main.add(Box.createVerticalStrut(16));

            // Theme switcher
            JPanel themeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            themeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            themeRow.add(new JLabel("Theme:"));
            JComboBox<String> themeSelector = new JComboBox<>(
                    new String[]{"Default (bundled)", "Theme 1", "Theme 2"});
            // Guard against re-entrancy: updateComponentTreeUI re-fires ActionEvent
            boolean[] switching = {false};
            themeSelector.addActionListener((ActionEvent e) -> {
                if (switching[0]) return;
                switching[0] = true;
                try {
                    int idx = themeSelector.getSelectedIndex();
                    switch (idx) {
                        case 0 -> System.clearProperty("dwc.theme");
                        case 1 -> System.setProperty("dwc.theme", "css/theme1.css");
                        case 2 -> System.setProperty("dwc.theme", "css/theme2.css");
                    }
                    UIManager.setLookAndFeel(new DwcLookAndFeel());
                    SwingUtilities.updateComponentTreeUI(
                            SwingUtilities.getWindowAncestor(themeSelector));
                    themeSelector.setSelectedIndex(idx);
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    switching[0] = false;
                }
            });
            themeRow.add(themeSelector);
            main.add(themeRow);
            main.add(Box.createVerticalStrut(16));

            // 13 component sections
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
            main.add(Box.createVerticalStrut(24));
            main.add(createProgressBarSection());
            main.add(Box.createVerticalStrut(24));
            main.add(createScrollBarSection());
            main.add(Box.createVerticalStrut(24));
            main.add(createTreeSection());
            main.add(Box.createVerticalStrut(24));
            main.add(createTableSection());
            main.add(Box.createVerticalStrut(24));
            main.add(createToolTipSection());

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

    // ---- Section 9: JProgressBar ----

    private static JPanel createProgressBarSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder("JProgressBar"));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Default + disabled row
        JPanel defaultRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        defaultRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JProgressBar pbDefault = new JProgressBar(0, 100);
        pbDefault.setValue(60);
        pbDefault.setStringPainted(true);
        pbDefault.setPreferredSize(new Dimension(200, 12));
        defaultRow.add(new JLabel("Default:"));
        defaultRow.add(pbDefault);

        JProgressBar pbDisabled = new JProgressBar(0, 100);
        pbDisabled.setValue(40);
        pbDisabled.setEnabled(false);
        pbDisabled.setPreferredSize(new Dimension(200, 12));
        defaultRow.add(new JLabel("Disabled:"));
        defaultRow.add(pbDisabled);

        section.add(defaultRow);

        // Variant row
        JPanel variantRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        variantRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JProgressBar pbSuccess = new JProgressBar(0, 100);
        pbSuccess.setValue(75);
        pbSuccess.putClientProperty("dwc.progressType", "success");
        pbSuccess.setPreferredSize(new Dimension(120, 12));
        variantRow.add(new JLabel("Success:"));
        variantRow.add(pbSuccess);

        JProgressBar pbDanger = new JProgressBar(0, 100);
        pbDanger.setValue(30);
        pbDanger.putClientProperty("dwc.progressType", "danger");
        pbDanger.setPreferredSize(new Dimension(120, 12));
        variantRow.add(new JLabel("Danger:"));
        variantRow.add(pbDanger);

        JProgressBar pbWarning = new JProgressBar(0, 100);
        pbWarning.setValue(50);
        pbWarning.putClientProperty("dwc.progressType", "warning");
        pbWarning.setPreferredSize(new Dimension(120, 12));
        variantRow.add(new JLabel("Warning:"));
        variantRow.add(pbWarning);

        JProgressBar pbInfo = new JProgressBar(0, 100);
        pbInfo.setValue(90);
        pbInfo.putClientProperty("dwc.progressType", "info");
        pbInfo.setPreferredSize(new Dimension(120, 12));
        variantRow.add(new JLabel("Info:"));
        variantRow.add(pbInfo);

        section.add(variantRow);

        // Indeterminate row
        JPanel indeterminateRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        indeterminateRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JProgressBar pbIndeterminate = new JProgressBar(0, 100);
        pbIndeterminate.setIndeterminate(true);
        pbIndeterminate.setPreferredSize(new Dimension(200, 12));
        indeterminateRow.add(new JLabel("Indeterminate:"));
        indeterminateRow.add(pbIndeterminate);

        section.add(indeterminateRow);

        return section;
    }

    // ---- Section 10: JScrollBar ----

    private static JPanel createScrollBarSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder("JScrollBar"));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel note = new JLabel(
                "The gallery's own scroll bar is already styled. Below are standalone bars:");
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(note);
        section.add(Box.createVerticalStrut(8));

        JPanel barRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        barRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollBar vertical = new JScrollBar(JScrollBar.VERTICAL);
        vertical.setValues(50, 10, 0, 100);
        vertical.setPreferredSize(new Dimension(10, 100));
        barRow.add(vertical);

        JScrollBar horizontal = new JScrollBar(JScrollBar.HORIZONTAL);
        horizontal.setValues(50, 10, 0, 100);
        horizontal.setPreferredSize(new Dimension(200, 10));
        barRow.add(horizontal);

        section.add(barRow);

        return section;
    }

    // ---- Section 11: JTree ----

    private static JPanel createTreeSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder("JTree"));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Project");

        DefaultMutableTreeNode src = new DefaultMutableTreeNode("src");
        DefaultMutableTreeNode main = new DefaultMutableTreeNode("main");
        main.add(new DefaultMutableTreeNode("java"));
        main.add(new DefaultMutableTreeNode("resources"));
        src.add(main);
        root.add(src);

        DefaultMutableTreeNode tests = new DefaultMutableTreeNode("tests");
        tests.add(new DefaultMutableTreeNode("unit"));
        tests.add(new DefaultMutableTreeNode("integration"));
        root.add(tests);

        DefaultMutableTreeNode docs = new DefaultMutableTreeNode("docs");
        docs.add(new DefaultMutableTreeNode("README"));
        docs.add(new DefaultMutableTreeNode("CHANGELOG"));
        root.add(docs);

        JTree tree = new JTree(root);
        tree.setPreferredSize(new Dimension(300, 180));
        tree.expandRow(0);
        tree.expandRow(1);
        tree.setSelectionRow(3);
        tree.setAlignmentX(Component.LEFT_ALIGNMENT);

        section.add(tree);

        return section;
    }

    // ---- Section 12: JTable ----

    private static JPanel createTableSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder("JTable"));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        String[] columnNames = {"Name", "Type", "Size", "Modified"};
        Object[][] data = {
                {"README.md", "File", "2 KB", "2026-02-10"},
                {"src", "Directory", "4 KB", "2026-02-09"},
                {"pom.xml", "File", "3 KB", "2026-02-08"},
                {"build.gradle", "File", "1 KB", "2026-02-07"},
                {"LICENSE", "File", "1 KB", "2026-01-15"},
                {".gitignore", "File", "512 B", "2026-01-10"},
        };

        JTable table = new JTable(data, columnNames);
        table.setRowSelectionInterval(1, 1);

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(600, 180));
        tableScroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        section.add(tableScroll);

        return section;
    }

    // ---- Section 13: JToolTip ----

    private static JPanel createToolTipSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder("JToolTip"));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel instruction = new JLabel("Hover over the buttons below to see themed tooltips");
        instruction.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(instruction);
        section.add(Box.createVerticalStrut(8));

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton b1 = new JButton("Hover me");
        b1.setToolTipText("This is a themed tooltip with rounded corners and shadow");
        buttonRow.add(b1);

        JButton b2 = new JButton("Information");
        b2.setToolTipText("Tooltips use DWC CSS tokens for consistent styling");
        buttonRow.add(b2);

        JButton b3 = new JButton("Disabled");
        b3.setToolTipText("Even disabled buttons show styled tooltips");
        b3.setEnabled(false);
        buttonRow.add(b3);

        section.add(buttonRow);

        return section;
    }
}
