/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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
package net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;
import net.sf.mzmine.util.components.ExtendedCheckBox;
import net.sf.mzmine.util.components.HelpButton;
import net.sf.mzmine.util.dialogs.ExitCode;

public class ClusteringSetupDialog extends JDialog implements
        ActionListener {

        ClusteringParameters parameterSet;
        static final int PADDING_SIZE = 5;
        private ExitCode exitCode = ExitCode.CANCEL;
        private Desktop desktop;
        private PeakList alignedPeakList;
        // dialog components    
        private JComboBox comboPeakMeasuringMethod;
        private JComboBox comboClusteringAlgorithm;
        private JComboBox comboClusteringVisualization;
        private JTextField textFieldClusteringNumberGroups;
        private Vector<ExtendedCheckBox<RawDataFile>> rawDataFileCheckBoxes;
        private Vector<ExtendedCheckBox<PeakListRow>> peakCheckBoxes;
        private JButton btnSelectAllFiles, btnDeselectAllFiles, btnSelectAllPeaks,
                btnDeselectAllPeaks, btnOK, btnCancel, btnHelp;

        public ClusteringSetupDialog(PeakList peakList,
                ClusteringParameters parameterSet, boolean forceXYComponents, String helpID) {
                // make dialog modal
                super(MZmineCore.getDesktop().getMainFrame(), "Projection plot setup",
                        true);
                this.parameterSet = parameterSet;

                this.desktop = MZmineCore.getDesktop();
                this.alignedPeakList = peakList;
                this.parameterSet = parameterSet;

                List<RawDataFile> selectedDataFiles = Arrays.asList(parameterSet.getSelectedDataFiles());
                List<PeakListRow> selectedRows = Arrays.asList(parameterSet.getSelectedRows());

                GridBagConstraints constraints = new GridBagConstraints();

                // set default layout constraints
                constraints.fill = GridBagConstraints.BOTH;
                constraints.anchor = GridBagConstraints.WEST;
                constraints.insets = new Insets(PADDING_SIZE, PADDING_SIZE,
                        PADDING_SIZE, PADDING_SIZE);

                constraints.gridwidth = 1;
                constraints.gridheight = 1;

                JComponent comp;
                GridBagLayout layout = new GridBagLayout();

                JPanel components = new JPanel(layout);

                comp = GUIUtils.addLabel(components, "Data files");
                constraints.gridx = 0;
                constraints.gridy = 0;
                layout.setConstraints(comp, constraints);

                JPanel dataFileCheckBoxesPanel = new JPanel();
                dataFileCheckBoxesPanel.setBackground(Color.white);
                dataFileCheckBoxesPanel.setLayout(new BoxLayout(
                        dataFileCheckBoxesPanel, BoxLayout.Y_AXIS));
                rawDataFileCheckBoxes = new Vector<ExtendedCheckBox<RawDataFile>>();
                int minimumHorizSize = 0;
                RawDataFile files[] = alignedPeakList.getRawDataFiles();
                for (int i = 0; i < files.length; i++) {
                        ExtendedCheckBox<RawDataFile> ecb = new ExtendedCheckBox<RawDataFile>(
                                files[i], selectedDataFiles.contains(files[i]));
                        rawDataFileCheckBoxes.add(ecb);
                        minimumHorizSize = Math.max(minimumHorizSize,
                                ecb.getPreferredWidth());
                        dataFileCheckBoxesPanel.add(ecb);
                }
                int minimumVertSize = (int) rawDataFileCheckBoxes.get(0).getPreferredSize().getHeight() * 3;
                JScrollPane dataFilePanelScroll = new JScrollPane(
                        dataFileCheckBoxesPanel,
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                dataFilePanelScroll.setPreferredSize(new Dimension(minimumHorizSize,
                        minimumVertSize));
                constraints.gridx = 1;
                components.add(dataFilePanelScroll, constraints);

                JPanel dataFileButtonsPanel = new JPanel();
                dataFileButtonsPanel.setLayout(new BoxLayout(dataFileButtonsPanel,
                        BoxLayout.Y_AXIS));
                btnSelectAllFiles = GUIUtils.addButton(dataFileButtonsPanel,
                        "Select all", null, this);
                btnDeselectAllFiles = GUIUtils.addButton(dataFileButtonsPanel,
                        "Deselect all", null, this);
                dataFileButtonsPanel.add(Box.createGlue());
                constraints.gridx = 2;
                components.add(dataFileButtonsPanel);
                layout.setConstraints(dataFileButtonsPanel, constraints);

                Parameter projectParameters[] = MZmineCore.getCurrentProject().getParameters();


                comp = GUIUtils.addLabel(components, "Peak measuring approach");
                constraints.gridx = 0;
                constraints.gridy = 1;
                layout.setConstraints(comp, constraints);

                String availablePeakMeasuringStyles[] = new String[]{
                        ClusteringParameters.PeakMeasurementTypeHeight,
                        ClusteringParameters.PeakMeasurementTypeArea};
                comboPeakMeasuringMethod = new JComboBox(availablePeakMeasuringStyles);
                if (parameterSet.getParameterValue(ClusteringParameters.peakMeasurementType) == ClusteringParameters.PeakMeasurementTypeHeight) {
                        comboPeakMeasuringMethod.setSelectedItem(ClusteringParameters.PeakMeasurementTypeHeight);
                }
                if (parameterSet.getParameterValue(ClusteringParameters.peakMeasurementType) == ClusteringParameters.PeakMeasurementTypeArea) {
                        comboPeakMeasuringMethod.setSelectedItem(ClusteringParameters.PeakMeasurementTypeArea);
                }

                constraints.gridx = 1;
                components.add(comboPeakMeasuringMethod, constraints);

                comp = GUIUtils.addLabel(components, "Peaks");
                constraints.gridx = 0;
                constraints.gridy = 2;
                layout.setConstraints(comp, constraints);

                JPanel peakCheckBoxesPanel = new JPanel();
                peakCheckBoxesPanel.setBackground(Color.white);
                peakCheckBoxesPanel.setLayout(new BoxLayout(peakCheckBoxesPanel,
                        BoxLayout.Y_AXIS));
                peakCheckBoxes = new Vector<ExtendedCheckBox<PeakListRow>>();
                minimumHorizSize = 0;
                PeakListRow rows[] = alignedPeakList.getRows();
                Arrays.sort(rows, new PeakListRowSorter(SortingProperty.MZ,
                        SortingDirection.Ascending));
                for (int i = 0; i < rows.length; i++) {
                        ExtendedCheckBox<PeakListRow> ecb = new ExtendedCheckBox<PeakListRow>(
                                rows[i], selectedRows.contains(rows[i]));
                        peakCheckBoxes.add(ecb);
                        minimumHorizSize = Math.max(minimumHorizSize,
                                ecb.getPreferredWidth());
                        peakCheckBoxesPanel.add(ecb);
                }
                minimumVertSize = (int) peakCheckBoxes.get(0).getPreferredSize().getHeight() * 6;
                JScrollPane peakPanelScroll = new JScrollPane(peakCheckBoxesPanel,
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                peakPanelScroll.setPreferredSize(new Dimension(minimumHorizSize,
                        minimumVertSize));
                constraints.gridx = 1;
                constraints.weightx = 1;
                constraints.weighty = 1;
                components.add(peakPanelScroll, constraints);

                JPanel peakButtonsPanel = new JPanel();
                peakButtonsPanel.setLayout(new BoxLayout(peakButtonsPanel,
                        BoxLayout.Y_AXIS));
                btnSelectAllPeaks = GUIUtils.addButton(peakButtonsPanel, "Select all",
                        null, this);
                btnDeselectAllPeaks = GUIUtils.addButton(peakButtonsPanel,
                        "Deselect all", null, this);
                peakButtonsPanel.add(Box.createGlue());
                constraints.gridx = 2;
                components.add(peakButtonsPanel);
                layout.setConstraints(peakButtonsPanel, constraints);

                comp = GUIUtils.addLabel(components, "Algorithm");
                constraints.gridx = 0;
                constraints.gridy = 3;
                layout.setConstraints(comp, constraints);


                ClusteringAlgorithmsEnum[] algorithms = ClusteringAlgorithmsEnum.values();
                String[] algorithmsString = new String[algorithms.length];
                for (int i = 0; i < algorithms.length; i++) {
                        algorithmsString[i] = algorithms[i].getName();
                }
                comboClusteringAlgorithm = new JComboBox(algorithmsString);
                textFieldClusteringNumberGroups = new JTextField(String.valueOf(parameterSet.getParameterValue(ClusteringParameters.numberOfGroups)));
               
                comboClusteringAlgorithm.addActionListener(this);
                for (ClusteringAlgorithmsEnum algorithmsEnum : ClusteringAlgorithmsEnum.values()) {
                        if (parameterSet.getParameterValue(ClusteringParameters.clusteringAlgorithm) == algorithmsEnum) {
                                comboClusteringAlgorithm.setSelectedItem(algorithmsEnum.getName());
                        }
                }

                constraints.gridx = 1;
                components.add(comboClusteringAlgorithm, constraints);
                comp = GUIUtils.addLabel(components, "Number of groups");
                constraints.gridx = 0;
                constraints.gridy = 4;
                layout.setConstraints(comp, constraints);
                
                if (parameterSet.getParameterValue(ClusteringParameters.clusteringAlgorithm) != ClusteringAlgorithmsEnum.FARTHESTFIRST &&
                        parameterSet.getParameterValue(ClusteringParameters.clusteringAlgorithm) != ClusteringAlgorithmsEnum.SIMPLEKMEANS) {
                        textFieldClusteringNumberGroups.setEnabled(false);
                }
                constraints.gridx = 1;
                components.add(textFieldClusteringNumberGroups, constraints);

                comp = GUIUtils.addLabel(components, "Visualization");
                constraints.gridx = 0;
                constraints.gridy = 5;
                layout.setConstraints(comp, constraints);
                String[] visualizationType = {ClusteringParameters.visualizationPCA, ClusteringParameters.visualizationSammon};
                comboClusteringVisualization = new JComboBox(visualizationType);
                if (parameterSet.getParameterValue(ClusteringParameters.visualization) == ClusteringParameters.visualizationPCA) {
                        comboClusteringVisualization.setSelectedItem(ClusteringParameters.visualizationPCA);
                }
                if (parameterSet.getParameterValue(ClusteringParameters.visualization) == ClusteringParameters.visualizationSammon) {
                        comboClusteringVisualization.setSelectedItem(ClusteringParameters.visualizationSammon);
                }
                constraints.gridx = 1;
                components.add(comboClusteringVisualization, constraints);


                comp = GUIUtils.addSeparator(components, PADDING_SIZE);
                constraints.gridx = 0;
                constraints.gridy = 6;
                constraints.weightx = 0;
                constraints.weighty = 0;
                constraints.gridheight = 1;
                constraints.gridwidth = 3;
                layout.setConstraints(comp, constraints);

                JPanel buttonsPanel = new JPanel();
                btnOK = GUIUtils.addButton(buttonsPanel, "OK", null, this);
                btnCancel = GUIUtils.addButton(buttonsPanel, "Cancel", null, this);

                if (helpID != null) {
                        btnHelp = new HelpButton(helpID);
                        buttonsPanel.add(btnHelp);
                }
                constraints.gridx = 0;
                constraints.gridy = 7;
                constraints.gridwidth = 3;
                components.add(buttonsPanel, constraints);

                GUIUtils.addMargin(components, PADDING_SIZE);
                add(components);

                // finalize the dialog
                pack();
                setLocationRelativeTo(desktop.getMainFrame());
                setResizable(true);

        }

        /**
         * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
         */
        public void actionPerformed(ActionEvent event) {

                Object src = event.getSource();

                if (src == btnOK) {

                        Vector<RawDataFile> selectedFiles = new Vector<RawDataFile>();
                        Vector<PeakListRow> selectedPeaks = new Vector<PeakListRow>();

                        for (ExtendedCheckBox<RawDataFile> box : rawDataFileCheckBoxes) {
                                if (box.isSelected()) {
                                        selectedFiles.add(box.getObject());
                                }
                        }


                        for (ExtendedCheckBox<PeakListRow> box : peakCheckBoxes) {
                                if (box.isSelected()) {
                                        selectedPeaks.add(box.getObject());
                                }
                        }

                        if (selectedFiles.size() == 0) {
                                desktop.displayErrorMessage("Please select at least one data file");
                                return;
                        }

                        if (selectedPeaks.size() == 0) {
                                desktop.displayErrorMessage("Please select at least one peak");
                                return;
                        }

                        parameterSet.setSourcePeakList(alignedPeakList);
                        parameterSet.setSelectedDataFiles(selectedFiles.toArray(new RawDataFile[0]));
                        parameterSet.setSelectedRows(selectedPeaks.toArray(new PeakListRow[0]));

                        parameterSet.setParameterValue(
                                ClusteringParameters.peakMeasurementType,
                                comboPeakMeasuringMethod.getSelectedItem());

                        String algorithm = (String) comboClusteringAlgorithm.getSelectedItem();
                        ClusteringAlgorithmsEnum selectedAlgorithm = ClusteringAlgorithmsEnum.COBWEB;
                        for (ClusteringAlgorithmsEnum a : ClusteringAlgorithmsEnum.values()) {
                                if (a.getName().equals(algorithm)) {
                                        selectedAlgorithm = a;
                                }
                        }

                        parameterSet.setParameterValue(
                                ClusteringParameters.clusteringAlgorithm,
                                selectedAlgorithm);

                        parameterSet.setParameterValue(
                                ClusteringParameters.visualization,
                                comboClusteringVisualization.getSelectedItem());
                        try {
                                if (this.comboClusteringAlgorithm.getSelectedItem().toString().equals(ClusteringAlgorithmsEnum.FARTHESTFIRST.getName()) ||
                                        this.comboClusteringAlgorithm.getSelectedItem().toString().equals(ClusteringAlgorithmsEnum.SIMPLEKMEANS.getName())) {
                                        parameterSet.setParameterValue(
                                                ClusteringParameters.numberOfGroups,
                                                Integer.parseInt(this.textFieldClusteringNumberGroups.getText()));
                                }
                        } catch (NumberFormatException e) {
                                desktop.displayErrorMessage("The number of groups must be an integer (Example: 3)");
                                return;
                        }
                        exitCode = ExitCode.OK;
                        dispose();
                        return;
                }

                if (src == comboClusteringAlgorithm) {
                        if (!this.comboClusteringAlgorithm.getSelectedItem().toString().equals(ClusteringAlgorithmsEnum.FARTHESTFIRST.getName()) &&
                                !this.comboClusteringAlgorithm.getSelectedItem().toString().equals(ClusteringAlgorithmsEnum.SIMPLEKMEANS.getName())) {
                                textFieldClusteringNumberGroups.setEnabled(false);
                        } else {
                                textFieldClusteringNumberGroups.setEnabled(true);
                        }
                }

                if (src == btnCancel) {
                        exitCode = ExitCode.CANCEL;
                        dispose();
                        return;
                }

                if (src == btnSelectAllFiles) {
                        for (JCheckBox box : rawDataFileCheckBoxes) {
                                box.setSelected(true);
                        }
                        return;
                }

                if (src == btnDeselectAllFiles) {
                        for (JCheckBox box : rawDataFileCheckBoxes) {
                                box.setSelected(false);
                        }
                        return;
                }

                if (src == btnSelectAllPeaks) {
                        for (JCheckBox box : peakCheckBoxes) {
                                box.setSelected(true);
                        }
                        return;
                }

                if (src == btnDeselectAllPeaks) {
                        for (JCheckBox box : peakCheckBoxes) {
                                box.setSelected(false);
                        }
                        return;
                }

        }

        public ExitCode getExitCode() {
                return exitCode;
        }
}
