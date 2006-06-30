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

import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.GUIUtils;

/**
 * Setup class for spectra visualizer
 */
public class SpectraSetup extends JDialog implements ActionListener {

    static final int MARGIN_SIZE = 5;

    private RawDataFile rawDataFile;

    // dialog components
    private JButton btnNumberShow, btnNumbersRangeShow, btnTimeRangeShow,
            btnCancel;
    private JTextField fieldScanNumber, fieldMinScanNumber, fieldMaxScanNumber,
            fieldMinScanTime, fieldMaxScanTime;
    private JComboBox numbersMSlevel, timeMSlevel;

    SpectraSetup(RawDataFile rawDataFile) {

        // Make dialog modal
        super(MainWindow.getInstance(), "Spectra visualizer parameters ("
                + rawDataFile + ")", true);

        this.rawDataFile = rawDataFile;

        Integer msLevels[] = CollectionUtils.toIntegerArray(rawDataFile.getMSLevels());

        BoxLayout layout = new BoxLayout(getContentPane(), BoxLayout.Y_AXIS);
        setLayout(layout);

        JPanel numberShowPanel = new JPanel(new GridLayout(1, 2, MARGIN_SIZE,
                MARGIN_SIZE));
        GUIUtils.addLabel(numberShowPanel, "Show scan by number");
        fieldScanNumber = new JTextField();
        numberShowPanel.add(fieldScanNumber);
        GUIUtils.addMargin(numberShowPanel, MARGIN_SIZE);
        add(numberShowPanel);

        btnNumberShow = GUIUtils.addButtonInPanel(this, "Show", this);

        GUIUtils.addSeparator(this, MARGIN_SIZE);

        GUIUtils.addLabelInPanel(this, "Show multiple scans by numbers range");

        JPanel numberRangeShowPanel = new JPanel(new GridLayout(3, 2,
                MARGIN_SIZE, MARGIN_SIZE));
        GUIUtils.addLabel(numberRangeShowPanel, "MS level");
        numbersMSlevel = new JComboBox(msLevels);
        numberRangeShowPanel.add(numbersMSlevel);
        GUIUtils.addLabel(numberRangeShowPanel, "Minimum scan number");
        fieldMinScanNumber = new JTextField();
        numberRangeShowPanel.add(fieldMinScanNumber);
        GUIUtils.addLabel(numberRangeShowPanel, "Maximum scan number");
        fieldMaxScanNumber = new JTextField();
        numberRangeShowPanel.add(fieldMaxScanNumber);
        GUIUtils.addMargin(numberRangeShowPanel, MARGIN_SIZE);
        add(numberRangeShowPanel);

        btnNumbersRangeShow = GUIUtils.addButtonInPanel(this, "Show", this);

        GUIUtils.addSeparator(this, MARGIN_SIZE);

        GUIUtils.addLabelInPanel(this,
                "Show multiple scans by retention time range");

        JPanel timeRangeShowPanel = new JPanel(new GridLayout(3, 2,
                MARGIN_SIZE, MARGIN_SIZE));
        GUIUtils.addLabel(timeRangeShowPanel, "MS level");
        timeMSlevel = new JComboBox(msLevels);
        timeRangeShowPanel.add(timeMSlevel);
        GUIUtils.addLabel(timeRangeShowPanel, "Minimum scan time");
        fieldMinScanTime = new JTextField();
        timeRangeShowPanel.add(fieldMinScanTime);
        GUIUtils.addLabel(timeRangeShowPanel, "Maximum scan time");
        fieldMaxScanTime = new JTextField();
        timeRangeShowPanel.add(fieldMaxScanTime);
        GUIUtils.addMargin(timeRangeShowPanel, MARGIN_SIZE);
        add(timeRangeShowPanel);

        btnTimeRangeShow = GUIUtils.addButtonInPanel(this, "Show", this);

        GUIUtils.addSeparator(this, MARGIN_SIZE);

        btnCancel = GUIUtils.addButtonInPanel(this, "Cancel", this);

        pack();

        setLocationRelativeTo(MainWindow.getInstance());

    }

    SpectraSetup(RawDataFile rawDataFile, int msLevel, int scanNumber) {

        this(rawDataFile);

        String scanNumberString = String.valueOf(scanNumber);
        String retentionTimeString = String.valueOf(rawDataFile.getRetentionTime(scanNumber));

        for (int i = 0; i < numbersMSlevel.getItemCount(); i++) {
            Object item = numbersMSlevel.getItemAt(i);
            if (item.equals(msLevel)) {
                numbersMSlevel.setSelectedIndex(i);
                timeMSlevel.setSelectedIndex(i);
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
     * Show a new setup dialog
     * 
     * @param rawDataFile
     * @param msLevel
     * @param scanNumber
     */
    public static void showSetupDialog(RawDataFile rawDataFile, int msLevel,
            int scanNumber) {
        SpectraSetup setupDialog = new SpectraSetup(rawDataFile, msLevel,
                scanNumber);
        setupDialog.setVisible(true);
    }

    /**
     * Show a new setup dialog
     * 
     * @param rawDataFile
     */
    public static void showSetupDialog(RawDataFile rawDataFile) {
        SpectraSetup setupDialog = new SpectraSetup(rawDataFile);
        setupDialog.setVisible(true);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(java.awt.event.ActionEvent ae) {

        Object src = ae.getSource();

        if (src == btnNumberShow) {
            try {
                int num = Integer.parseInt(fieldScanNumber.getText());
                new SpectraVisualizer(rawDataFile, num);
                dispose();
            } catch (Exception e) {
                MainWindow.getInstance().displayErrorMessage("Invalid input.");
            }
        }

        if (src == btnNumbersRangeShow) {
            try {
                int msLevel = (Integer) numbersMSlevel.getSelectedItem();
                int minNumber = Integer.parseInt(fieldMinScanNumber.getText());
                int maxNumber = Integer.parseInt(fieldMaxScanNumber.getText());
                int scanNums[] = rawDataFile.getScanNumbers(msLevel);
                ArrayList<Integer> eligibleScans = new ArrayList<Integer>();
                for (int i = 0; i < scanNums.length; i++) {
                    if ((scanNums[i] >= minNumber)
                            && (scanNums[i] <= maxNumber))
                        eligibleScans.add(i);
                }
                int eligibleScanNums[] = CollectionUtils.toArray(eligibleScans);
                new SpectraVisualizer(rawDataFile, eligibleScanNums);
                dispose();
            } catch (Exception e) {
                MainWindow.getInstance().displayErrorMessage("Invalid input.");
            }
        }

        if (src == btnTimeRangeShow) {
            try {
                int msLevel = (Integer) timeMSlevel.getSelectedItem();
                double minRT = Double.parseDouble(fieldMinScanTime.getText());
                double maxRT = Double.parseDouble(fieldMaxScanTime.getText());
                int scanNums[] = rawDataFile.getScanNumbers(msLevel, minRT,
                        maxRT);
                new SpectraVisualizer(rawDataFile, scanNums);
                dispose();
            } catch (Exception e) {
                MainWindow.getInstance().displayErrorMessage("Invalid input.");
            }
        }

        if (src == btnCancel) {
            dispose();
        }

    }
}
