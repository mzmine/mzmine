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

package net.sf.mzmine.modules.visualization.rawdata.spectra;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.GUIUtils;

/**
 * Setup dialog for spectra visualizer
 */
public class SpectraSetupDialog extends JDialog implements ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    static final int PADDING_SIZE = 5;

    static final String DEFAULT_MZ_BIN_SIZE = "0.1";

    // dialog components
    private JButton btnNumberShow, btnNumbersRangeShow, btnTimeRangeShow,
            btnCancel;
    private JTextField fieldScanNumber, fieldMinScanNumber, fieldMaxScanNumber,
            fieldMinScanTime, fieldMaxScanTime, fieldNumberBinSize,
            fieldTimeBinSize;
    private JComboBox comboNumbersMSlevel, comboTimeMSlevel;

    private static final NumberFormat format = NumberFormat.getNumberInstance();

    private TaskController taskController;
    private Desktop desktop;
    private OpenedRawDataFile dataFile;

    public SpectraSetupDialog(TaskController taskController, Desktop desktop,
            OpenedRawDataFile dataFile) {

        // Make dialog modal
        super(desktop.getMainFrame(), "Spectra visualizer parameters", true);

        this.taskController = taskController;
        this.desktop = desktop;
        this.dataFile = dataFile;
        RawDataFile rawDataFile = dataFile.getCurrentFile();

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

        comp = GUIUtils.addLabel(components, dataFile.toString(),
                JLabel.LEFT);
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        comp = GUIUtils.addSeparator(components, PADDING_SIZE);
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        comp = GUIUtils.addLabel(components, "Show scan by number");
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        fieldScanNumber = new JFormattedTextField(format);
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        components.add(fieldScanNumber, constraints);

        constraints.weightx = 0;

        btnNumberShow = GUIUtils.addButton(components, "Show", null, this);
        constraints.gridx = 2;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(btnNumberShow, constraints);

        comp = GUIUtils.addSeparator(components, PADDING_SIZE);
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        comp = GUIUtils.addLabel(components,
                "Show multiple scans by numbers range");
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        comp = GUIUtils.addLabel(components, "MS level");
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        Integer msLevels[] = CollectionUtils.toIntegerArray(rawDataFile.getMSLevels());

        comboNumbersMSlevel = new JComboBox(msLevels);
        comboNumbersMSlevel.addActionListener(this);
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 5;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        components.add(comboNumbersMSlevel, constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;

        comp = GUIUtils.addLabel(components, "Minimum scan number");
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        fieldMinScanNumber = new JFormattedTextField(format);
        constraints.gridx = 1;
        constraints.gridy = 6;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        components.add(fieldMinScanNumber, constraints);

        comp = GUIUtils.addLabel(components, "Maximum scan number");
        constraints.gridx = 0;
        constraints.gridy = 7;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        fieldMaxScanNumber = new JFormattedTextField(format);
        constraints.gridx = 1;
        constraints.gridy = 7;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        components.add(fieldMaxScanNumber, constraints);

        comp = GUIUtils.addLabel(components, "m/z bin size");
        constraints.gridx = 0;
        constraints.gridy = 8;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        fieldNumberBinSize = new JFormattedTextField(format);
        fieldNumberBinSize.setText(DEFAULT_MZ_BIN_SIZE);
        constraints.gridx = 1;
        constraints.gridy = 8;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        components.add(fieldNumberBinSize, constraints);

        comp = GUIUtils.addLabel(components, "m/q (Th)");
        constraints.gridx = 2;
        constraints.gridy = 8;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        btnNumbersRangeShow = GUIUtils.addButton(components, "Show", null, this);
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.gridx = 0;
        constraints.gridy = 9;
        constraints.gridwidth = 3;
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

        comboTimeMSlevel = new JComboBox(msLevels);
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

        fieldMinScanTime = new JFormattedTextField(format);
        constraints.gridx = 1;
        constraints.gridy = 13;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        components.add(fieldMinScanTime, constraints);

        comp = GUIUtils.addLabel(components, "s");
        constraints.gridx = 2;
        constraints.gridy = 13;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        comp = GUIUtils.addLabel(components, "Maximum retention time");
        constraints.gridx = 0;
        constraints.gridy = 14;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        fieldMaxScanTime = new JFormattedTextField(format);
        constraints.gridx = 1;
        constraints.gridy = 14;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        components.add(fieldMaxScanTime, constraints);

        comp = GUIUtils.addLabel(components, "s");
        constraints.gridx = 2;
        constraints.gridy = 14;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        comp = GUIUtils.addLabel(components, "m/z bin size");
        constraints.gridx = 0;
        constraints.gridy = 15;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        fieldTimeBinSize = new JFormattedTextField(format);
        fieldTimeBinSize.setText(DEFAULT_MZ_BIN_SIZE);
        constraints.gridx = 1;
        constraints.gridy = 15;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        components.add(fieldTimeBinSize, constraints);

        comp = GUIUtils.addLabel(components, "m/q (Th)");
        constraints.gridx = 2;
        constraints.gridy = 15;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        btnTimeRangeShow = GUIUtils.addButton(components, "Show", null, this);
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.gridx = 0;
        constraints.gridy = 16;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        layout.setConstraints(btnTimeRangeShow, constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;

        comp = GUIUtils.addSeparator(components, PADDING_SIZE);
        constraints.gridx = 0;
        constraints.gridy = 17;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        btnCancel = GUIUtils.addButton(components, "Cancel", null, this);
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.gridx = 0;
        constraints.gridy = 18;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        layout.setConstraints(btnCancel, constraints);

        GUIUtils.addMargin(components, PADDING_SIZE);
        add(components);

        // finalize the dialog
        pack();
        setLocationRelativeTo(desktop.getMainFrame());
        setResizable(false);

    }

    public SpectraSetupDialog(TaskController taskController, Desktop desktop,
            OpenedRawDataFile dataFile, int msLevel, int scanNumber) {

        this(taskController, desktop, dataFile);

        String scanNumberString = String.valueOf(scanNumber);
        String retentionTimeString = String.valueOf(dataFile.getCurrentFile().getRetentionTime(scanNumber));

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

        if (src == btnNumberShow) {
            try {
                int num = format.parse(fieldScanNumber.getText()).intValue();
                new SpectraVisualizerWindow(taskController, desktop,
                        dataFile, num);
                dispose();
            } catch (Exception e) {
                logger.log(Level.FINE, "Invalid input", e);
                desktop.displayErrorMessage("Invalid input");
            }
        }

        if (src == btnNumbersRangeShow) {
            try {
                int msLevel = (Integer) comboNumbersMSlevel.getSelectedItem();
                int minNumber = format.parse(fieldMinScanNumber.getText()).intValue();
                int maxNumber = format.parse(fieldMaxScanNumber.getText()).intValue();
                double mzBinSize = Double.parseDouble(fieldNumberBinSize.getText());
                if (mzBinSize <= 0) {
                    desktop.displayErrorMessage("Invalid bin size " + mzBinSize);
                    return;
                }
                int scanNums[] = dataFile.getCurrentFile().getScanNumbers(msLevel);
                ArrayList<Integer> eligibleScans = new ArrayList<Integer>();
                for (int i = 0; i < scanNums.length; i++) {
                    if ((scanNums[i] >= minNumber)
                            && (scanNums[i] <= maxNumber))
                        eligibleScans.add(scanNums[i]);
                }
                int eligibleScanNums[] = CollectionUtils.toIntArray(eligibleScans);
                if (eligibleScanNums.length == 0) {
                    desktop.displayErrorMessage("No scans found at MS level "
                            + msLevel + " within given number range.");
                    return;
                }
                new SpectraVisualizerWindow(taskController, desktop,
                        dataFile, eligibleScanNums, mzBinSize);
                dispose();
            } catch (Exception e) {
                desktop.displayErrorMessage("Invalid input");
            }
        }

        if (src == btnTimeRangeShow) {
            try {
                int msLevel = (Integer) comboTimeMSlevel.getSelectedItem();
                double minRT = Double.parseDouble(fieldMinScanTime.getText());
                double maxRT = Double.parseDouble(fieldMaxScanTime.getText());
                double mzBinSize = Double.parseDouble(fieldTimeBinSize.getText());
                if (mzBinSize <= 0) {
                    desktop.displayErrorMessage("Invalid bin size " + mzBinSize);
                    return;
                }
                int scanNums[] = dataFile.getCurrentFile().getScanNumbers(msLevel, minRT,
                        maxRT);
                if (scanNums.length == 0) {
                    desktop.displayErrorMessage("No scans found at MS level "
                            + msLevel + " within given retention time range.");
                    return;
                }
                new SpectraVisualizerWindow(taskController, desktop,
                        dataFile, scanNums, mzBinSize);
                dispose();
            } catch (Exception e) {
                desktop.displayErrorMessage("Invalid input");
            }
        }

        if (src == btnCancel) {
            dispose();
        }

    }
}
