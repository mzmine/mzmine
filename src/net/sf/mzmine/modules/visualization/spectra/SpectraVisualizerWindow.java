/*
 * Copyright 2006-2008 The MZmine Development Team
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

package net.sf.mzmine.modules.visualization.spectra;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.userinterface.dialogs.AxesSetupDialog;

/**
 * Spectrum visualizer using JFreeChart library
 */
class SpectraVisualizerWindow extends JInternalFrame implements ActionListener {

    private static final float zoomCoefficient = 1.2f;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private SpectraToolBar toolBar;
    private SpectraPlot spectrumPlot;
    private SpectraBottomPanel bottomPanel;

    private RawDataFile dataFile;

    // Currently loaded scan
    private Scan currentScan;

    // Current scan data set
    private ScanDataSet scanDataSet;

    private NumberFormat rtFormat = MZmineCore.getDesktop().getRTFormat();
    private NumberFormat mzFormat = MZmineCore.getDesktop().getMZFormat();
    private NumberFormat intensityFormat = MZmineCore.getDesktop().getIntensityFormat();

    SpectraVisualizerWindow(RawDataFile dataFile, int scanNumber) {

        super(dataFile.toString(), true, true, true, true);

        this.dataFile = dataFile;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        spectrumPlot = new SpectraPlot(this);
        add(spectrumPlot, BorderLayout.CENTER);

        toolBar = new SpectraToolBar(this);
        add(toolBar, BorderLayout.EAST);

        bottomPanel = new SpectraBottomPanel(this);
        add(bottomPanel, BorderLayout.SOUTH);

        loadScan(scanNumber);

        pack();

    }

    private void loadScan(int scanNumber) {

        logger.finest("Loading scan #" + scanNumber + " from " + dataFile
                + " for spectra visualizer");

        currentScan = dataFile.getScan(scanNumber);
        scanDataSet = new ScanDataSet(currentScan);

        JComboBox peakListSelector = bottomPanel.getPeakListSelector();

        PeakList selectedPeakList = (PeakList) peakListSelector.getSelectedItem();
        PeakListDataSet peaksDataSet = null;

        if (selectedPeakList != null) {
            toolBar.setPeaksButtonEnabled(true);
            peaksDataSet = new PeakListDataSet(dataFile, scanNumber,
                    selectedPeakList);
        } else {
            toolBar.setPeaksButtonEnabled(false);
        }

        // Set plot data sets
        spectrumPlot.setDataSets(scanDataSet, peaksDataSet);

        // Update the peak list combo contents
        peakListSelector.removeAllItems();
        MZmineProject project = MZmineCore.getCurrentProject();
        PeakList availablePeakLists[] = project.getPeakLists(dataFile);
        // Add peak lists in reverse order        
        for (int i = availablePeakLists.length - 1; i >= 0; i--)
            peakListSelector.addItem(availablePeakLists[i]);
        if (selectedPeakList != null)
            peakListSelector.setSelectedItem(selectedPeakList);
        peakListSelector.setEnabled((currentScan.getMSLevel() == 1)
                && (availablePeakLists.length > 0));

        // if the scan is centroided, switch to centroid mode
        if (currentScan.isCentroided()) {
            spectrumPlot.setPlotMode(PlotMode.CENTROID);
            toolBar.setCentroidButton(false);
        } else {
            spectrumPlot.setPlotMode(PlotMode.CONTINUOUS);
            toolBar.setCentroidButton(true);
        }

        // Clean up the MS/MS selector combo

        JComboBox msmsSelector = bottomPanel.getMSMSSelector();
        msmsSelector.removeAllItems();
        msmsSelector.setEnabled(false);

        // Add parent scan to MS/MS selector combo

        int parentNumber = currentScan.getParentScanNumber();
        if ((currentScan.getMSLevel() > 1) && (parentNumber > 0)) {

            String itemText = "Parent scan #"
                    + parentNumber
                    + ", RT: "
                    + rtFormat.format(dataFile.getScan(parentNumber).getRetentionTime())
                    + ", precursor m/z: "
                    + mzFormat.format(currentScan.getPrecursorMZ());

            if (currentScan.getPrecursorCharge() > 0)
                itemText += " (chrg " + currentScan.getPrecursorCharge() + ")";

            msmsSelector.addItem(itemText);
            msmsSelector.setEnabled(true);

        }

        // Add all fragment scans to MS/MS selector combo
        int fragmentScans[] = currentScan.getFragmentScanNumbers();
        if (fragmentScans != null) {

            for (int fragment : fragmentScans) {
                Scan fragmentScan = dataFile.getScan(fragment);
                String itemText = "Fragment scan #" + fragment + ", RT: "
                        + rtFormat.format(fragmentScan.getRetentionTime())
                        + ", precursor m/z: "
                        + mzFormat.format(fragmentScan.getPrecursorMZ());
                msmsSelector.addItem(itemText);
                msmsSelector.setEnabled(true);
            }

        }

        // Set window and plot titles

        String title = "[" + dataFile.toString() + "] scan #"
                + currentScan.getScanNumber();

        String subTitle = "MS" + currentScan.getMSLevel() + ", RT "
                + rtFormat.format(currentScan.getRetentionTime())
                + ", base peak: "
                + mzFormat.format(currentScan.getBasePeakMZ()) + " m/z ("
                + intensityFormat.format(currentScan.getBasePeakIntensity())
                + ")";

        setTitle(title);
        spectrumPlot.setTitle(title, subTitle);

    }

    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        if (command.equals("SHOW_DATA_POINTS")) {
            spectrumPlot.switchDataPointsVisible();
        }

        if (command.equals("SHOW_ANNOTATIONS")) {
            spectrumPlot.switchItemLabelsVisible();
        }

        if (command.equals("SHOW_PICKED_PEAKS")) {
            spectrumPlot.switchPickedPeaksVisible();
        }

        if (command.equals("SETUP_AXES")) {
            JFrame mainFrame = MZmineCore.getDesktop().getMainFrame();
            AxesSetupDialog dialog = new AxesSetupDialog(mainFrame,
                    spectrumPlot.getChart().getXYPlot());
            dialog.setVisible(true);
        }

        if (command.equals("PEAKLIST_CHANGE")) {
            JComboBox peakListSelector = bottomPanel.getPeakListSelector();
            PeakList selectedPeakList = (PeakList) peakListSelector.getSelectedItem();
            PeakListDataSet peaksDataSet = null;

            if (selectedPeakList != null) {
                toolBar.setPeaksButtonEnabled(true);
                peaksDataSet = new PeakListDataSet(dataFile,
                        currentScan.getScanNumber(), selectedPeakList);
            } else {
                toolBar.setPeaksButtonEnabled(false);
            }

            spectrumPlot.setDataSets(scanDataSet, peaksDataSet);

        }

        if (command.equals("TOGGLE_PLOT_MODE")) {
            if (spectrumPlot.getPlotMode() == PlotMode.CONTINUOUS) {
                spectrumPlot.setPlotMode(PlotMode.CENTROID);
                toolBar.setCentroidButton(false);
            } else {
                spectrumPlot.setPlotMode(PlotMode.CONTINUOUS);
                toolBar.setCentroidButton(true);
            }
        }

        if (command.equals("PREVIOUS_SCAN")) {

            int msLevel = currentScan.getMSLevel();
            int scanNumbers[] = dataFile.getScanNumbers(msLevel);
            int scanIndex = Arrays.binarySearch(scanNumbers,
                    currentScan.getScanNumber());
            if (scanIndex > 0)
                loadScan(scanNumbers[scanIndex - 1]);
        }

        if (command.equals("NEXT_SCAN")) {
            int msLevel = currentScan.getMSLevel();
            int scanNumbers[] = dataFile.getScanNumbers(msLevel);
            int scanIndex = Arrays.binarySearch(scanNumbers,
                    currentScan.getScanNumber());
            if (scanIndex < (scanNumbers.length - 1))
                loadScan(scanNumbers[scanIndex + 1]);
        }

        if (command.equals("ZOOM_IN")) {
            spectrumPlot.getXYPlot().getDomainAxis().resizeRange(
                    1 / zoomCoefficient);
        }

        if (command.equals("ZOOM_OUT")) {
            spectrumPlot.getXYPlot().getDomainAxis().resizeRange(
                    zoomCoefficient);
        }

        if (command.equals("SHOW_MSMS")) {

            String selectedScanString = (String) bottomPanel.getMSMSSelector().getSelectedItem();
            if (selectedScanString == null)
                return;

            int sharpIndex = selectedScanString.indexOf('#');
            int commaIndex = selectedScanString.indexOf(',');
            selectedScanString = selectedScanString.substring(sharpIndex + 1,
                    commaIndex);
            int selectedScan = Integer.valueOf(selectedScanString);

            SpectraVisualizer specVis = SpectraVisualizer.getInstance();
            specVis.showNewSpectraWindow(dataFile, selectedScan);
        }

    }

}