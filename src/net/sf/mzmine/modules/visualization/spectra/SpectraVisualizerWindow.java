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
import javax.swing.JInternalFrame;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.dialogs.AxesSetupDialog;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;

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

    private NumberFormat rtFormat = MZmineCore.getRTFormat();
    private NumberFormat mzFormat = MZmineCore.getMZFormat();
    private NumberFormat intensityFormat = MZmineCore.getIntensityFormat();

    private Desktop desktop;

    SpectraVisualizerWindow(RawDataFile dataFile, int scanNumber) {

        super(dataFile.toString(), true, true, true, true);

        this.dataFile = dataFile;
        desktop = MZmineCore.getDesktop();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        spectrumPlot = new SpectraPlot(this);
        add(spectrumPlot, BorderLayout.CENTER);

        toolBar = new SpectraToolBar(this);
        add(toolBar, BorderLayout.EAST);

        bottomPanel = new SpectraBottomPanel(this, dataFile);
        add(bottomPanel, BorderLayout.SOUTH);

        loadScan(scanNumber);

        pack();

        // After we have constructed everything, load the peak lists into the
        // bottom panel
        bottomPanel.rebuildPeakListSelector(MZmineCore.getCurrentProject());

    }

    private void loadScan(final int scanNumber) {

        logger.finest("Loading scan #" + scanNumber + " from " + dataFile
                + " for spectra visualizer");

        // Perform the actual loading of the data in a new thread, so we don't
        // block the GUI
        Runnable newThreadRunnable = new Runnable() {

            public void run() {

                // Synchronize over data file so the threads don't fight each
                // other while updating the GUI components
                synchronized (dataFile) {

                    currentScan = dataFile.getScan(scanNumber);
                    scanDataSet = new ScanDataSet(currentScan);

                    PeakList selectedPeakList = bottomPanel.getSelectedPeakList();
                    PeakListDataSet peaksDataSet = null;

                    if (selectedPeakList != null) {
                        toolBar.setPeaksButtonEnabled(true);
                        peaksDataSet = new PeakListDataSet(dataFile,
                                scanNumber, selectedPeakList);
                    } else {
                        toolBar.setPeaksButtonEnabled(false);
                    }

                    // Set plot data sets
                    spectrumPlot.setDataSets(scanDataSet, peaksDataSet);

                    // Set plot mode only if it hasn't been set before
                    if (spectrumPlot.getPlotMode() == PlotMode.UNDEFINED)
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
                    boolean msmsVisible = false;

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
                            itemText += " (chrg "
                                    + currentScan.getPrecursorCharge() + ")";

                        msmsSelector.addItem(itemText);
                        msmsVisible = true;

                    }

                    // Add all fragment scans to MS/MS selector combo
                    int fragmentScans[] = currentScan.getFragmentScanNumbers();
                    if (fragmentScans != null) {

                        for (int fragment : fragmentScans) {
                            Scan fragmentScan = dataFile.getScan(fragment);
                            String itemText = "Fragment scan #"
                                    + fragment
                                    + ", RT: "
                                    + rtFormat.format(fragmentScan.getRetentionTime())
                                    + ", precursor m/z: "
                                    + mzFormat.format(fragmentScan.getPrecursorMZ());
                            msmsSelector.addItem(itemText);
                            msmsVisible = true;
                        }

                    }

                    // Update the visibility of MS/MS selection combo
                    bottomPanel.setMSMSSelectorVisible(msmsVisible);

                    // Set window and plot titles

                    String title = "[" + dataFile.toString() + "] scan #"
                            + currentScan.getScanNumber();

                    String subTitle = "MS" + currentScan.getMSLevel() + ", RT "
                            + rtFormat.format(currentScan.getRetentionTime());

                    DataPoint basePeak = currentScan.getBasePeak();
                    if (basePeak != null) {
                        subTitle += ", base peak: "
                                + mzFormat.format(basePeak.getMZ())
                                + " m/z ("
                                + intensityFormat.format(basePeak.getIntensity())
                                + ")";
                    }

                    setTitle(title);
                    spectrumPlot.setTitle(title, subTitle);
                }
            }

        };

        Thread newThread = new Thread(newThreadRunnable);
        newThread.start();

    }

    public void setAxesRange(float xMin, float xMax, float xTickSize,
            float yMin, float yMax, float yTickSize) {
        NumberAxis xAxis = (NumberAxis) spectrumPlot.getXYPlot().getDomainAxis();
        NumberAxis yAxis = (NumberAxis) spectrumPlot.getXYPlot().getRangeAxis();
        xAxis.setRange(xMin, xMax);
        xAxis.setTickUnit(new NumberTickUnit(xTickSize));
        yAxis.setRange(yMin, yMax);
        yAxis.setTickUnit(new NumberTickUnit(yTickSize));
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
            AxesSetupDialog dialog = new AxesSetupDialog(
                    spectrumPlot.getXYPlot());
            dialog.setVisible(true);
        }

        if (command.equals("SET_SAME_RANGE")) {

            // Get current axes range
            NumberAxis xAxis = (NumberAxis) spectrumPlot.getXYPlot().getDomainAxis();
            NumberAxis yAxis = (NumberAxis) spectrumPlot.getXYPlot().getRangeAxis();
            float xMin = (float) xAxis.getRange().getLowerBound();
            float xMax = (float) xAxis.getRange().getUpperBound();
            float xTick = (float) xAxis.getTickUnit().getSize();
            float yMin = (float) yAxis.getRange().getLowerBound();
            float yMax = (float) yAxis.getRange().getUpperBound();
            float yTick = (float) yAxis.getTickUnit().getSize();

            // Get all frames of my class
            JInternalFrame spectraFrames[] = desktop.getInternalFrames();

            // Set the range of these frames
            for (JInternalFrame frame : spectraFrames) {
                if ((!(frame instanceof SpectraVisualizerWindow))
                        || (frame == this))
                    continue;
                SpectraVisualizerWindow spectraFrame = (SpectraVisualizerWindow) frame;
                spectraFrame.setAxesRange(xMin, xMax, xTick, yMin, yMax, yTick);
            }

        }

        if (command.equals("PEAKLIST_CHANGE")) {

            // If no scan is loaded yet, ignore
            if (currentScan == null)
                return;

            PeakList selectedPeakList = bottomPanel.getSelectedPeakList();
            PeakListDataSet peaksDataSet = null;

            if (selectedPeakList != null) {
                logger.finest("Loading a peak list " + selectedPeakList
                        + " to a spectrum window " + getTitle());
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
            specVis.showNewSpectrumWindow(dataFile, selectedScan);
        }

    }

}