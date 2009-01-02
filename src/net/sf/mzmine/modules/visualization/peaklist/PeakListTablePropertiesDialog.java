/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.peaklist;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.peaklist.table.CommonColumnType;
import net.sf.mzmine.modules.visualization.peaklist.table.DataFileColumnType;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.dialogs.ExitCode;

public class PeakListTablePropertiesDialog extends JDialog implements
        ActionListener {

    private ExitCode exitCode = ExitCode.CANCEL;

    private Hashtable<CommonColumnType, JCheckBox> commonColumnCheckBoxes;
    private Hashtable<DataFileColumnType, JCheckBox> rawDataColumnCheckBoxes;

    private JButton btnOk, btnCancel;
    private JComboBox peakShapeMaxCombo;
    private JTextField rowHeightField;

    private PeakListTableParameters parameters;

    public PeakListTablePropertiesDialog(PeakListTableParameters parameters) {

        // Make dialog modal
        super(MZmineCore.getDesktop().getMainFrame(), true);

        this.parameters = parameters;

        setTitle("Peak list table properties");

        // Generate label and check box for each possible common column
        JPanel pnlCommon = new JPanel();
        pnlCommon.setLayout(new BoxLayout(pnlCommon, BoxLayout.Y_AXIS));

        commonColumnCheckBoxes = new Hashtable<CommonColumnType, JCheckBox>();

        JLabel commonColsTitle = new JLabel("Common columns");
        pnlCommon.add(commonColsTitle);

        pnlCommon.add(Box.createRigidArea(new Dimension(0, 5)));

        for (CommonColumnType c : CommonColumnType.values()) {

            JCheckBox commonColumnCheckBox = new JCheckBox();
            commonColumnCheckBox.setText(c.getColumnName());
            commonColumnCheckBoxes.put(c, commonColumnCheckBox);
            commonColumnCheckBox.setSelected(parameters.isColumnVisible(c));
            commonColumnCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            pnlCommon.add(commonColumnCheckBox);

        }

        pnlCommon.add(Box.createVerticalGlue());

        // Generate label and check box for each possible raw data column
        JPanel pnlRaw = new JPanel();
        pnlRaw.setLayout(new BoxLayout(pnlRaw, BoxLayout.Y_AXIS));

        rawDataColumnCheckBoxes = new Hashtable<DataFileColumnType, JCheckBox>();

        JLabel rawDataColsTitle = new JLabel("Data file columns");
        pnlRaw.add(rawDataColsTitle);

        for (DataFileColumnType c : DataFileColumnType.values()) {

            JCheckBox rawDataColumnCheckBox = new JCheckBox();
            rawDataColumnCheckBox.setText(c.getColumnName());
            rawDataColumnCheckBoxes.put(c, rawDataColumnCheckBox);
            rawDataColumnCheckBox.setSelected(parameters.isColumnVisible(c));
            rawDataColumnCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            pnlRaw.add(rawDataColumnCheckBox);

        }

        pnlRaw.add(Box.createVerticalGlue());

        // Create and add buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        buttonPanel.add(Box.createHorizontalGlue());
        btnOk = GUIUtils.addButton(buttonPanel, "OK", null, this);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        btnCancel = GUIUtils.addButton(buttonPanel, "Cancel", null, this);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        mainPanel.add(pnlCommon);
        mainPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        mainPanel.add(pnlRaw);

        JPanel propertiesPanel = new JPanel();
        propertiesPanel.setLayout(new GridLayout(2, 2, 5, 5));

        GUIUtils.addLabel(propertiesPanel, "Peak shape normalization");
        peakShapeMaxCombo = new JComboBox(PeakShapeNormalization.values());
        peakShapeMaxCombo.setSelectedItem(parameters.getPeakShapeNormalization());
        propertiesPanel.add(peakShapeMaxCombo);
        GUIUtils.addLabel(propertiesPanel, "Row height");
        rowHeightField = new JTextField();
        rowHeightField.setText(String.valueOf(parameters.getRowHeight()));
        propertiesPanel.add(rowHeightField);

        JPanel allPanel = new JPanel();
        allPanel.setLayout(new BoxLayout(allPanel, BoxLayout.Y_AXIS));

        GUIUtils.addMargin(mainPanel, 10);
        GUIUtils.addMargin(propertiesPanel, 10);
        GUIUtils.addMargin(buttonPanel, 10);

        allPanel.add(mainPanel);
        GUIUtils.addSeparator(allPanel);
        allPanel.add(propertiesPanel);
        GUIUtils.addSeparator(allPanel);
        allPanel.add(buttonPanel);

        add(allPanel);

        setResizable(false);
        pack();
        setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());

    }

    public void actionPerformed(ActionEvent e) {

        Object src = e.getSource();

        if (src == btnOk) {

            Enumeration<CommonColumnType> ccEnum = commonColumnCheckBoxes.keys();
            while (ccEnum.hasMoreElements()) {
                CommonColumnType ccType = ccEnum.nextElement();
                JCheckBox ccBox = commonColumnCheckBoxes.get(ccType);
                parameters.setColumnVisible(ccType, ccBox.isSelected());
            }

            Enumeration<DataFileColumnType> rcEnum = rawDataColumnCheckBoxes.keys();
            while (rcEnum.hasMoreElements()) {
                DataFileColumnType rcType = rcEnum.nextElement();
                JCheckBox rcBox = rawDataColumnCheckBoxes.get(rcType);
                parameters.setColumnVisible(rcType, rcBox.isSelected());
            }

            PeakShapeNormalization maxSelected = (PeakShapeNormalization) peakShapeMaxCombo.getSelectedItem();
            if (maxSelected != null)
                parameters.setPeakShapeNormalization(maxSelected);

            int newRowHeight = Integer.parseInt(rowHeightField.getText());
            if (newRowHeight > 0)
                parameters.setRowHeight(newRowHeight);

            exitCode = ExitCode.OK;
            dispose();

        }

        if (src == btnCancel) {
            exitCode = ExitCode.CANCEL;
            dispose();
        }

    }

    /**
     * Method for reading exit code
     * 
     */
    public ExitCode getExitCode() {
        return exitCode;
    }

}
