/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */
package io.github.mzmine.parameters.parametertypes.selectors;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.GUIUtils;
import io.github.mzmine.util.components.GridBagPanel;
import io.github.mzmine.util.components.MultipleSelectionComponent;

/**
 * @author akshaj Shows the dialog to select the features and add them to the
 *         parameter setup dialog of the Fx3DVisualizer.
 */
public class FeaturesSelectionDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;
    private Logger LOG = Logger.getLogger(this.getClass().getName());
    private MultipleSelectionComponent<Feature> featuresSelectionBox;
    private JComboBox<RawDataFile> rawDataFileComboBox;
    private JComboBox<PeakList> peakListComboBox;
    private JPanel buttonPane;
    private JButton btnOk;
    private JButton btnCancel;
    private boolean returnState = true;
    private PeakList[] allPeakLists;
    private GridBagPanel mainPanel;
    private int selectedIndex = 0;
    private JPanel panel00;
    private JPanel panel01;
    private JPanel panel02;
    private JPanel panel10;
    private JPanel panel11;
    private JPanel panel12;

    public FeaturesSelectionDialog() {
        mainPanel = new GridBagPanel();
        panel00 = new JPanel(new BorderLayout());
        panel01 = new JPanel(new BorderLayout());
        panel02 = new JPanel(new BorderLayout());
        panel10 = new JPanel(new BorderLayout());
        panel11 = new JPanel(new BorderLayout());
        panel12 = new JPanel(new BorderLayout());
        mainPanel.add(panel00, 0, 0);
        mainPanel.add(panel01, 0, 1);
        mainPanel.add(panel02, 0, 2);
        mainPanel.add(panel10, 1, 0);
        mainPanel.add(panel11, 1, 1);
        mainPanel.add(panel12, 1, 2);
        this.add(mainPanel);

        allPeakLists = MZmineCore.getProjectManager().getCurrentProject()
                .getPeakLists();
        peakListComboBox = new JComboBox<PeakList>(allPeakLists);
        peakListComboBox.setToolTipText("Feature list selection");
        peakListComboBox.addActionListener(this);
        JLabel peakListsLabel = new JLabel("Feature list");

        panel00.add(peakListsLabel, BorderLayout.CENTER);
        panel10.add(peakListComboBox, BorderLayout.CENTER);

        RawDataFile[] rawDataFiles = allPeakLists[0].getRawDataFiles();
        rawDataFileComboBox = new JComboBox<RawDataFile>(rawDataFiles);
        rawDataFileComboBox.setToolTipText("Raw data file selection");
        rawDataFileComboBox.addActionListener(this);
        JLabel rawDataFilesLabel = new JLabel("Raw data file");
        panel01.add(rawDataFilesLabel, BorderLayout.CENTER);
        panel11.add(rawDataFileComboBox, BorderLayout.CENTER);

        RawDataFile datafile = allPeakLists[0].getRawDataFile(0);
        Feature[] features = allPeakLists[0].getPeaks(datafile);
        featuresSelectionBox = new MultipleSelectionComponent<Feature>(
                features);
        featuresSelectionBox.setToolTipText("Features selection");
        JLabel featuresLabel = new JLabel("Features");
        featuresSelectionBox.setSize(50, 30);
        panel02.add(featuresLabel, BorderLayout.WEST);
        panel12.add(featuresSelectionBox, BorderLayout.CENTER);

        buttonPane = new JPanel();
        buttonPane.setLayout(new FlowLayout());
        btnOk = GUIUtils.addButton(buttonPane, "OK", null, this);
        btnCancel = GUIUtils.addButton(buttonPane, "Cancel", null, this);
        this.add(buttonPane, BorderLayout.SOUTH);
        this.pack();
        this.setSize(670, 400);
        this.setLocationRelativeTo(null);
    }

    public List<Feature> getSelectedFeatures() {
        return featuresSelectionBox.getSelectedValues();
    }

    public PeakList getSelectedPeakList() {
        return (PeakList) peakListComboBox.getSelectedItem();
    }

    public RawDataFile getSelectedRawDataFile() {
        return (RawDataFile) rawDataFileComboBox.getSelectedItem();
    }

    public boolean getReturnState() {
        return returnState;
    }

    /*
     *
     * 
     * @see
     * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        Object src = event.getSource();
        if (src == btnOk) {
            returnState = true;
            this.dispose();
        }
        if (src == btnCancel) {
            returnState = false;
            this.dispose();
        }
        if (src == rawDataFileComboBox) {
            panel12.removeAll();
            RawDataFile dataFile = (RawDataFile) rawDataFileComboBox
                    .getSelectedItem();
            Feature[] features = allPeakLists[selectedIndex].getPeaks(dataFile);
            featuresSelectionBox = new MultipleSelectionComponent<Feature>(
                    features);
            featuresSelectionBox.setToolTipText("Features Selection Box");
            panel12.add(featuresSelectionBox, BorderLayout.CENTER);
            panel12.revalidate();
        }
        if (src == peakListComboBox) {
            PeakList peakList = (PeakList) peakListComboBox.getSelectedItem();
            panel11.removeAll();
            panel12.removeAll();

            for (int j = 0; j < allPeakLists.length; j++) {
                if (peakList.equals(allPeakLists[j])) {
                    RawDataFile[] rawDataFiles = allPeakLists[j]
                            .getRawDataFiles();
                    rawDataFileComboBox = new JComboBox<RawDataFile>(
                            rawDataFiles);
                    rawDataFileComboBox
                            .setToolTipText("Raw data files Selection Box");
                    rawDataFileComboBox.addActionListener(this);
                    panel11.add(rawDataFileComboBox, BorderLayout.CENTER);

                    this.setSize(670, 400);
                    LOG.finest("PeakListRowComboBox is Added");

                    selectedIndex = j;
                    RawDataFile datafile = allPeakLists[j].getRawDataFile(0);
                    Feature[] features = allPeakLists[j].getPeaks(datafile);
                    featuresSelectionBox = new MultipleSelectionComponent<Feature>(
                            features);
                    featuresSelectionBox
                            .setToolTipText("Features Selection Box");
                    panel12.add(featuresSelectionBox, BorderLayout.CENTER);
                }
                panel11.revalidate();
                panel12.revalidate();
            }
        }
    }
}
