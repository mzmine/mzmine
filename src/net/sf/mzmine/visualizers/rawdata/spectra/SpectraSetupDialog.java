/*
 * Copyright 2006 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.visualizers.rawdata.spectra;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.GUIUtils;

/**
 * Setup dialog for spectra visualizer
 */
public class SpectraSetupDialog extends JDialog implements ActionListener {

    static final int PADDING_SIZE = 5;

    // dialog components
    private JButton btnNumberShow, btnNumbersRangeShow, btnTimeRangeShow,
            btnCancel;
    private JTextField fieldScanNumber, fieldMinScanNumber, fieldMaxScanNumber,
            fieldMinScanTime, fieldMaxScanTime;
    private JComboBox comboRawDataFile, comboNumbersMSlevel, comboTimeMSlevel;

    public SpectraSetupDialog() {

        // Make dialog modal
        super(MainWindow.getInstance(), "Spectra visualizer parameters", true);

        GridBagConstraints constraints = new GridBagConstraints();

        // set default layout constraints
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(PADDING_SIZE, PADDING_SIZE,
                PADDING_SIZE, PADDING_SIZE);

        JComponent comp;
        GridBagLayout layout = new GridBagLayout();

        JPanel components = new JPanel(layout);

        comp = GUIUtils.addLabel(components, "Raw data file");
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        comboRawDataFile = new JComboBox();
        comboRawDataFile.addActionListener(this);
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        components.add(comboRawDataFile, constraints);
        constraints.fill = GridBagConstraints.HORIZONTAL;

        comp = GUIUtils.addSeparator(components, PADDING_SIZE);
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        comp = GUIUtils.addLabel(components, "Show scan by number");
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        fieldScanNumber = new JTextField();
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        components.add(fieldScanNumber, constraints);

        btnNumberShow = GUIUtils.addButton(components, "Show", null, this);
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        layout.setConstraints(btnNumberShow, constraints);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;

        comp = GUIUtils.addSeparator(components, PADDING_SIZE);
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        comp = GUIUtils.addLabel(components,
                "Show multiple scans by numbers range");
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        comp = GUIUtils.addLabel(components, "MS level");
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        comboNumbersMSlevel = new JComboBox();
        comboNumbersMSlevel.addActionListener(this);
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 6;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        components.add(comboNumbersMSlevel, constraints);
        constraints.fill = GridBagConstraints.HORIZONTAL;

        comp = GUIUtils.addLabel(components, "Minimum scan number");
        constraints.gridx = 0;
        constraints.gridy = 7;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        fieldMinScanNumber = new JTextField();
        constraints.gridx = 1;
        constraints.gridy = 7;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        components.add(fieldMinScanNumber, constraints);

        comp = GUIUtils.addLabel(components, "Maximum scan number");
        constraints.gridx = 0;
        constraints.gridy = 8;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        fieldMaxScanNumber = new JTextField();
        constraints.gridx = 1;
        constraints.gridy = 8;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        components.add(fieldMaxScanNumber, constraints);

        btnNumbersRangeShow = GUIUtils.addButton(components, "Show", null, this);
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.gridx = 0;
        constraints.gridy = 9;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        layout.setConstraints(btnNumbersRangeShow, constraints);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;

        comp = GUIUtils.addSeparator(components, PADDING_SIZE);
        constraints.gridx = 0;
        constraints.gridy = 10;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        comp = GUIUtils.addLabel(components,
                "Show multiple scans by retention time range");
        constraints.gridx = 0;
        constraints.gridy = 11;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        comp = GUIUtils.addLabel(components, "MS level");
        constraints.gridx = 0;
        constraints.gridy = 12;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        comboTimeMSlevel = new JComboBox();
        comboTimeMSlevel.addActionListener(this);
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 12;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        components.add(comboTimeMSlevel, constraints);
        constraints.fill = GridBagConstraints.HORIZONTAL;

        comp = GUIUtils.addLabel(components, "Minimum retention time");
        constraints.gridx = 0;
        constraints.gridy = 13;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        fieldMinScanTime = new JTextField();
        constraints.gridx = 1;
        constraints.gridy = 13;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        components.add(fieldMinScanTime, constraints);

        comp = GUIUtils.addLabel(components, "Maximum retention time");
        constraints.gridx = 0;
        constraints.gridy = 14;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        fieldMaxScanTime = new JTextField();
        constraints.gridx = 1;
        constraints.gridy = 14;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        components.add(fieldMaxScanTime, constraints);

        btnTimeRangeShow = GUIUtils.addButton(components, "Show", null, this);
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.gridx = 0;
        constraints.gridy = 15;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        layout.setConstraints(btnTimeRangeShow, constraints);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;

        comp = GUIUtils.addSeparator(components, PADDING_SIZE);
        constraints.gridx = 0;
        constraints.gridy = 16;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        btnCancel = GUIUtils.addButton(components, "Cancel", null, this);
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.gridx = 0;
        constraints.gridy = 17;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        layout.setConstraints(btnCancel, constraints);

        GUIUtils.addMargin(components, PADDING_SIZE);
        add(components);

        // add data files to the combo box
        RawDataFile files[] = MainWindow.getInstance().getItemSelector().getRawDataFiles();
        DefaultComboBoxModel fileItems = new DefaultComboBoxModel(files);
        comboRawDataFile.setModel(fileItems);

        // finalize the dialog
        pack();
        setLocationRelativeTo(MainWindow.getInstance());
        setResizable(false);

    }

    public SpectraSetupDialog(RawDataFile rawDataFile) {

        this();

        if (rawDataFile != null)
            comboRawDataFile.setSelectedItem(rawDataFile);

    }

    public SpectraSetupDialog(RawDataFile rawDataFile, int msLevel,
            int scanNumber) {

        this(rawDataFile);

        String scanNumberString = String.valueOf(scanNumber);
        String retentionTimeString = String.valueOf(rawDataFile.getRetentionTime(scanNumber));

        for (int i = 0; i < comboNumbersMSlevel.getItemCount(); i++) {
            Object item = comboNumbersMSlevel.getItemAt(i);
            if (item.equals(msLevel)) {
                comboNumbersMSlevel.setSelectedIndex(i);
                comboTimeMSlevel.setSelectedIndex(i);
                break;
            }
        }

        fieldScanNumber.setText(scanNumberString);
        fieldMinScanNumber.setText(scanNumberString);
        fieldMaxScanNumber.setText(scanNumberString);
        fieldMinScanTime.setText(retentionTimeString);
        fieldMaxScanTime.setText(retentionTimeString);

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent ae) {

        Object src = ae.getSource();

        // raw data selection changed
        if (src == comboRawDataFile) {
            RawDataFile selectedFile = (RawDataFile) comboRawDataFile.getSelectedItem();
            Integer msLevels[] = CollectionUtils.toIntegerArray(selectedFile.getMSLevels());

            DefaultComboBoxModel msLevelItems = new DefaultComboBoxModel(
                    msLevels);
            comboNumbersMSlevel.setModel(msLevelItems);
            comboTimeMSlevel.setModel(msLevelItems);
        }

        if (src == btnNumberShow) {
            try {
                RawDataFile selectedFile = (RawDataFile) comboRawDataFile.getSelectedItem();
                int num = Integer.parseInt(fieldScanNumber.getText());
                new SpectraVisualizer(selectedFile, num);
                dispose();
            } catch (Exception e) {
                MainWindow.getInstance().displayErrorMessage("Invalid input");
            }
        }

        if (src == btnNumbersRangeShow) {
            try {
                RawDataFile selectedFile = (RawDataFile) comboRawDataFile.getSelectedItem();
                int msLevel = (Integer) comboNumbersMSlevel.getSelectedItem();
                int minNumber = Integer.parseInt(fieldMinScanNumber.getText());
                int maxNumber = Integer.parseInt(fieldMaxScanNumber.getText());
                int scanNums[] = selectedFile.getScanNumbers(msLevel);
                ArrayList<Integer> eligibleScans = new ArrayList<Integer>();
                for (int i = 0; i < scanNums.length; i++) {
                    if ((scanNums[i] >= minNumber)
                            && (scanNums[i] <= maxNumber))
                        eligibleScans.add(i);
                }
                int eligibleScanNums[] = CollectionUtils.toIntArray(eligibleScans);
                if (eligibleScanNums.length == 0) {
                    MainWindow.getInstance().displayErrorMessage("No scans found at MS level " + msLevel + " within given number range.");
                    return;
                }
                new SpectraVisualizer(selectedFile, eligibleScanNums);
                dispose();
            } catch (Exception e) {
                MainWindow.getInstance().displayErrorMessage("Invalid input");
            }
        }

        if (src == btnTimeRangeShow) {
            try {
                RawDataFile selectedFile = (RawDataFile) comboRawDataFile.getSelectedItem();
                int msLevel = (Integer) comboTimeMSlevel.getSelectedItem();
                double minRT = Double.parseDouble(fieldMinScanTime.getText());
                double maxRT = Double.parseDouble(fieldMaxScanTime.getText());
                int scanNums[] = selectedFile.getScanNumbers(msLevel, minRT, maxRT);
                if (scanNums.length == 0) {
                    MainWindow.getInstance().displayErrorMessage("No scans found at MS level " + msLevel + " within given retention time range.");
                    return;
                }
                new SpectraVisualizer(selectedFile, scanNums);
                dispose();
            } catch (Exception e) {
                MainWindow.getInstance().displayErrorMessage("Invalid input");
            }
        }

        if (src == btnCancel) {
            dispose();
        }

    }
}
