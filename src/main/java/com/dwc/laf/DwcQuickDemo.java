package com.dwc.laf;

import javax.swing.*;
import java.awt.*;


public class DwcQuickDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new DwcLookAndFeel());
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            JFrame frame = new JFrame("DWC L&F — Component Preview");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(520, 520);

            JPanel main = new JPanel();
            main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
            main.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

            // -- Buttons --
            JLabel btnLabel = new JLabel("Buttons");
            btnLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            main.add(btnLabel);
            main.add(Box.createVerticalStrut(6));

            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            JButton defaultBtn = new JButton("Default");
            JButton primaryBtn = new JButton("Primary");
            primaryBtn.putClientProperty("JButton.buttonType", "primary");
            JButton disabledBtn = new JButton("Disabled");
            disabledBtn.setEnabled(false);
            btnRow.add(defaultBtn);
            btnRow.add(primaryBtn);
            btnRow.add(disabledBtn);
            main.add(btnRow);
            main.add(Box.createVerticalStrut(16));

            // -- Text Fields --
            JLabel tfLabel = new JLabel("Text Fields");
            tfLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            main.add(tfLabel);
            main.add(Box.createVerticalStrut(6));

            JPanel tfRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            tfRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            JTextField normalTf = new JTextField(12);
            normalTf.putClientProperty("JTextField.placeholderText", "Placeholder...");
            JTextField filledTf = new JTextField("Filled text", 12);
            JTextField disabledTf = new JTextField("Disabled", 12);
            disabledTf.setEnabled(false);
            tfRow.add(normalTf);
            tfRow.add(filledTf);
            tfRow.add(disabledTf);
            main.add(tfRow);
            main.add(Box.createVerticalStrut(16));

            // -- CheckBoxes --
            JLabel cbLabel = new JLabel("CheckBoxes");
            cbLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            main.add(cbLabel);
            main.add(Box.createVerticalStrut(6));

            JPanel cbRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            cbRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            JCheckBox cb1 = new JCheckBox("Unchecked");
            JCheckBox cb2 = new JCheckBox("Checked");
            cb2.setSelected(true);
            JCheckBox cb3 = new JCheckBox("Disabled");
            cb3.setEnabled(false);
            JCheckBox cb4 = new JCheckBox("Disabled Checked");
            cb4.setSelected(true);
            cb4.setEnabled(false);
            cbRow.add(cb1);
            cbRow.add(cb2);
            cbRow.add(cb3);
            cbRow.add(cb4);
            main.add(cbRow);
            main.add(Box.createVerticalStrut(16));

            // -- RadioButtons --
            JLabel rbLabel = new JLabel("RadioButtons");
            rbLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            main.add(rbLabel);
            main.add(Box.createVerticalStrut(6));

            JPanel rbRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            rbRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            ButtonGroup bg = new ButtonGroup();
            JRadioButton rb1 = new JRadioButton("Option A");
            JRadioButton rb2 = new JRadioButton("Option B");
            rb2.setSelected(true);
            JRadioButton rb3 = new JRadioButton("Disabled");
            rb3.setEnabled(false);
            bg.add(rb1);
            bg.add(rb2);
            bg.add(rb3);
            rbRow.add(rb1);
            rbRow.add(rb2);
            rbRow.add(rb3);
            main.add(rbRow);
            main.add(Box.createVerticalStrut(16));

            // -- ComboBoxes --
            JLabel cmbLabel = new JLabel("ComboBoxes");
            cmbLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            main.add(cmbLabel);
            main.add(Box.createVerticalStrut(6));

            JPanel cmbRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            cmbRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            JComboBox<String> cmb1 = new JComboBox<>(new String[]{"Select...", "Apple", "Banana", "Cherry"});
            JComboBox<String> cmb2 = new JComboBox<>(new String[]{"Disabled"});
            cmb2.setEnabled(false);
            cmbRow.add(cmb1);
            cmbRow.add(cmb2);
            main.add(cmbRow);

            frame.setContentPane(new JScrollPane(main));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
