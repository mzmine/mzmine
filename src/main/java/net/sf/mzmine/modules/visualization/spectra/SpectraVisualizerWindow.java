/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.spectra;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.data.xy.XYDataset;

import com.google.common.base.Strings;
import com.google.common.collect.Range;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import net.sf.mzmine.datamodel.MassSpectrumType;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.desktop.impl.WindowsMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.modules.visualization.spectra.datasets.IsotopesDataSet;
import net.sf.mzmine.modules.visualization.spectra.datasets.PeakListDataSet;
import net.sf.mzmine.modules.visualization.spectra.datasets.ScanDataSet;
import net.sf.mzmine.modules.visualization.spectra.datasets.SinglePeakDataSet;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.WindowSettingsParameter;
import net.sf.mzmine.util.dialogs.AxesSetupDialog;

/**
 * Spectrum visualizer using JFreeChart library
 */
public class SpectraVisualizerWindow extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    public static final Color scanColor = new Color(0, 0, 192);
    public static final Color peaksColor = Color.red;
    public static final Color singlePeakColor = Color.magenta;
    public static final Color detectedIsotopesColor = Color.magenta;
    public static final Color predictedIsotopesColor = Color.green;

    private SpectraToolBar toolBar;
    private SpectraPlot spectrumPlot;
    private SpectraBottomPanel bottomPanel;

    private RawDataFile dataFile;

    // Currently loaded scan
    private Scan currentScan;

    private File lastExportDirectory;

    // Current scan data set
    private ScanDataSet spectrumDataSet;

    private static final double zoomCoefficient = 1.2f;

    public SpectraVisualizerWindow(RawDataFile dataFile) {

        super("Spectrum loading...");
        this.dataFile = dataFile;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        spectrumPlot = new SpectraPlot(this);
        add(spectrumPlot, BorderLayout.CENTER);

        toolBar = new SpectraToolBar(this);
        add(toolBar, BorderLayout.EAST);

        // Create relationship between the current Plot and Tool bar
        // spectrumPlot.setRelatedToolBar(toolBar);

        bottomPanel = new SpectraBottomPanel(this, dataFile);
        add(bottomPanel, BorderLayout.SOUTH);

        MZmineCore.getDesktop().addPeakListTreeListener(bottomPanel);

        // Add the Windows menu
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(new WindowsMenu());
        setJMenuBar(menuBar);

        pack();

        // get the window settings parameter
        ParameterSet paramSet = MZmineCore.getConfiguration()
                .getModuleParameters(SpectraVisualizerModule.class);
        WindowSettingsParameter settings = paramSet
                .getParameter(SpectraVisualizerParameters.windowSettings);

        // update the window and listen for changes
        settings.applySettingsToWindow(this);
        this.addComponentListener(settings);

    }

    public void dispose() {
        super.dispose();
        MZmineCore.getDesktop().removePeakListTreeListener(bottomPanel);
    }

    public void loadRawData(Scan scan) {

        logger.finest("Loading scan #" + scan.getScanNumber() + " from "
                + dataFile + " for spectra visualizer");

        spectrumDataSet = new ScanDataSet(scan);

        this.currentScan = scan;

        // If the plot mode has not been set yet, set it accordingly
        if (spectrumPlot.getPlotMode() == null) {
            spectrumPlot.setPlotMode(currentScan.getSpectrumType());
            toolBar.setCentroidButton(currentScan.getSpectrumType());
        }

        // Clean up the MS/MS selector combo

        final JComboBox<String> msmsSelector = bottomPanel.getMSMSSelector();

        // We disable the MSMS selector first and then enable it again later
        // after updating the items. If we skip this, the size of the
        // selector may not be adjusted properly (timing issues?)
        msmsSelector.setEnabled(false);

        msmsSelector.removeAllItems();
        boolean msmsVisible = false;

        // Add parent scan to MS/MS selector combo

        NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
        NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
        NumberFormat intensityFormat = MZmineCore.getConfiguration()
                .getIntensityFormat();

        // Add all fragment scans to MS/MS selector combo
        int fragmentScans[] = currentScan.getFragmentScanNumbers();
        if (fragmentScans != null) {
            for (int fragment : fragmentScans) {
                Scan fragmentScan = dataFile.getScan(fragment);
                if (fragmentScan == null)
                    continue;
                final String itemText = "Fragment scan #" + fragment + ", RT: "
                        + rtFormat.format(fragmentScan.getRetentionTime())
                        + ", precursor m/z: "
                        + mzFormat.format(fragmentScan.getPrecursorMZ());
                // Updating the combo in other than Swing thread may cause
                // exception
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        msmsSelector.addItem(itemText);
                    }
                });
                msmsVisible = true;
            }
        }

        msmsSelector.setEnabled(true);

        // Update the visibility of MS/MS selection combo
        bottomPanel.setMSMSSelectorVisible(msmsVisible);

        // Set window and plot titles
        String windowTitle = "Spectrum: [" + dataFile.getName() + "; scan #"
                + currentScan.getScanNumber() + "]";

        String spectrumTitle = dataFile.getName() + " scan"
                + currentScan.toString();

        DataPoint basePeak = currentScan.getHighestDataPoint();
        if (basePeak != null) {
            spectrumTitle += ", base peak: " + mzFormat.format(basePeak.getMZ())
                    + " m/z (" + intensityFormat.format(basePeak.getIntensity())
                    + ")";
        }
        String spectrumSubtitle = null;
        if (!Strings.isNullOrEmpty(currentScan.getScanDefinition())) {
            spectrumSubtitle = "Scan definition: "
                    + currentScan.getScanDefinition();
        }

        setTitle(windowTitle);
        spectrumPlot.setTitle(spectrumTitle, spectrumSubtitle);

        // Set plot data set
        spectrumPlot.removeAllDataSets();
        spectrumPlot.addDataSet(spectrumDataSet, scanColor, false);

        // Reload peak list
        bottomPanel.rebuildPeakListSelector();

    }

    public void loadPeaks(PeakList selectedPeakList) {

        spectrumPlot.removePeakListDataSets();

        if (selectedPeakList == null) {
            return;
        }

        logger.finest("Loading a peak list " + selectedPeakList
                + " to a spectrum window " + getTitle());

        PeakListDataSet peaksDataSet = new PeakListDataSet(dataFile,
                currentScan.getScanNumber(), selectedPeakList);

        // Set plot data sets
        spectrumPlot.addDataSet(peaksDataSet, peaksColor, true);

    }

    public void loadSinglePeak(Feature peak) {

        SinglePeakDataSet peakDataSet = new SinglePeakDataSet(
                currentScan.getScanNumber(), peak);

        // Set plot data sets
        spectrumPlot.addDataSet(peakDataSet, singlePeakColor, true);

    }

    public void loadIsotopes(IsotopePattern newPattern) 
    {
        // We need to find a normalization factor for the new isotope
        // pattern, to show meaningful intensity range
        double mz = newPattern.getHighestDataPoint().getMZ();
        Range<Double> searchMZRange = Range.closed(mz - 0.5, mz + 0.5);
        ScanDataSet scanDataSet = spectrumPlot.getMainScanDataSet();
        double normalizationFactor = scanDataSet
                .getHighestIntensity(searchMZRange);

        // If normalization factor is 0, it means there were no data points
        // in given m/z range. In such case we use the max intensity of
        // whole scan as normalization factor.
        if (normalizationFactor == 0) {
            searchMZRange = Range.atLeast(0.0);
            normalizationFactor = scanDataSet
                    .getHighestIntensity(searchMZRange);
        }

        IsotopePattern normalizedPattern = IsotopePatternCalculator
                .normalizeIsotopePattern(newPattern, normalizationFactor);
        Color newColor;
        if (newPattern.getStatus() == IsotopePatternStatus.DETECTED)
            newColor = detectedIsotopesColor;
        else
            newColor = predictedIsotopesColor;
        IsotopesDataSet newDataSet = new IsotopesDataSet(normalizedPattern);
        spectrumPlot.addDataSet(newDataSet, newColor, true);

    }

    public void loadSpectrum(IsotopePattern newPattern) {
        Color newColor = newPattern.getStatus() == IsotopePatternStatus.DETECTED ?
                detectedIsotopesColor : predictedIsotopesColor;
        IsotopesDataSet newDataSet = new IsotopesDataSet(newPattern);
        spectrumPlot.addDataSet(newDataSet, newColor, true);
    }
    
    public void setAxesRange(double xMin, double xMax, double xTickSize,
            double yMin, double yMax, double yTickSize) {
        NumberAxis xAxis = (NumberAxis) spectrumPlot.getXYPlot()
                .getDomainAxis();
        NumberAxis yAxis = (NumberAxis) spectrumPlot.getXYPlot().getRangeAxis();
        xAxis.setRange(xMin, xMax);
        xAxis.setTickUnit(new NumberTickUnit(xTickSize));
        yAxis.setRange(yMin, yMax);
        yAxis.setTickUnit(new NumberTickUnit(yTickSize));
    }

    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        if (command.equals("PEAKLIST_CHANGE")) {

            // If no scan is loaded yet, ignore
            if (currentScan == null)
                return;

            PeakList selectedPeakList = bottomPanel.getSelectedPeakList();
            loadPeaks(selectedPeakList);

        }

        if (command.equals("PREVIOUS_SCAN")) {

            if (dataFile == null)
                return;

            int msLevel = currentScan.getMSLevel();
            int scanNumbers[] = dataFile.getScanNumbers(msLevel);
            int scanIndex = Arrays.binarySearch(scanNumbers,
                    currentScan.getScanNumber());
            if (scanIndex > 0) {
                final int prevScanIndex = scanNumbers[scanIndex - 1];

                Runnable newThreadRunnable = new Runnable() {

                    public void run() {
                        loadRawData(dataFile.getScan(prevScanIndex));
                    }

                };

                Thread newThread = new Thread(newThreadRunnable);
                newThread.start();

            }
        }

        if (command.equals("NEXT_SCAN")) {

            if (dataFile == null)
                return;

            int msLevel = currentScan.getMSLevel();
            int scanNumbers[] = dataFile.getScanNumbers(msLevel);
            int scanIndex = Arrays.binarySearch(scanNumbers,
                    currentScan.getScanNumber());

            if (scanIndex < (scanNumbers.length - 1)) {
                final int nextScanIndex = scanNumbers[scanIndex + 1];

                Runnable newThreadRunnable = new Runnable() {

                    public void run() {
                        loadRawData(dataFile.getScan(nextScanIndex));
                    }

                };

                Thread newThread = new Thread(newThreadRunnable);
                newThread.start();

            }
        }

        if (command.equals("SHOW_MSMS")) {

            String selectedScanString = (String) bottomPanel.getMSMSSelector()
                    .getSelectedItem();
            if (selectedScanString == null)
                return;

            int sharpIndex = selectedScanString.indexOf('#');
            int commaIndex = selectedScanString.indexOf(',');
            selectedScanString = selectedScanString.substring(sharpIndex + 1,
                    commaIndex);
            int selectedScan = Integer.valueOf(selectedScanString);

            SpectraVisualizerModule.showNewSpectrumWindow(dataFile,
                    selectedScan);
        }

        if (command.equals("TOGGLE_PLOT_MODE")) {
            if (spectrumPlot.getPlotMode() == MassSpectrumType.CENTROIDED) {
                spectrumPlot.setPlotMode(MassSpectrumType.PROFILE);
                toolBar.setCentroidButton(MassSpectrumType.PROFILE);
            } else {
                spectrumPlot.setPlotMode(MassSpectrumType.CENTROIDED);
                toolBar.setCentroidButton(MassSpectrumType.CENTROIDED);
            }
        }

        if (command.equals("SHOW_DATA_POINTS")) {
            spectrumPlot.switchDataPointsVisible();
        }

        if (command.equals("SHOW_ANNOTATIONS")) {
            spectrumPlot.switchItemLabelsVisible();
        }

        if (command.equals("SHOW_PICKED_PEAKS")) {
            spectrumPlot.switchPickedPeaksVisible();
        }

        if (command.equals("SHOW_ISOTOPE_PEAKS")) {
            spectrumPlot.switchIsotopePeaksVisible();
        }

        if (command.equals("SETUP_AXES")) {
            AxesSetupDialog dialog = new AxesSetupDialog(this,
                    spectrumPlot.getXYPlot());
            dialog.setVisible(true);
        }

        if (command.equals("EXPORT_SPECTRA")) {

            File selectedFile = null;
            String extension = "";

            JFileChooser fileChooser = new JFileChooser();

            // Remove the accept-all (.*) file filter
            fileChooser.setAcceptAllFileFilterUsed(false);

            // Add file filters
            fileChooser.addChoosableFileFilter(new FileNameExtensionFilter(
                    "MGF - Mascot Generic Format", "mgf"));
            fileChooser.addChoosableFileFilter(new FileNameExtensionFilter(
                    "MSP - NIST file Format", "msp"));
            fileChooser.addChoosableFileFilter(new FileNameExtensionFilter(
                    "TXT - Plain text Format", "txt"));
            fileChooser.addChoosableFileFilter(
                    new FileNameExtensionFilter("mzML - mzML Format", "mzML"));

            // Export file chooser
            do {

                if ((lastExportDirectory != null)
                        && (lastExportDirectory.isDirectory())) {
                    fileChooser.setCurrentDirectory(lastExportDirectory);
                }

                int result = fileChooser.showSaveDialog(null);
                if (result != JFileChooser.APPROVE_OPTION)
                    return;

                selectedFile = fileChooser.getSelectedFile();
                lastExportDirectory = selectedFile.getParentFile();

                String path = selectedFile.getAbsolutePath();

                FileFilter selectedFilter = fileChooser.getFileFilter();

                if (selectedFilter == null) {
                    extension = "mgf";
                } else {
                    if (selectedFilter.getDescription().contains("MGF")) {
                        extension = "mgf";
                    } else if (selectedFilter.getDescription()
                            .contains("TXT")) {
                        extension = "txt";
                    } else if (selectedFilter.getDescription()
                            .contains("MSP")) {
                        extension = "msp";
                    } else if (selectedFilter.getDescription()
                            .contains("mzML")) {
                        extension = "mzML";
                    }
                }

                if (!path.toLowerCase().endsWith(extension.toLowerCase())) {
                    path += "." + extension;
                    selectedFile = new File(path);
                }

                if (selectedFile.exists()) {

                    String msg;
                    if (extension.equals("mzML"))
                        msg = "The file already exists. Do you want to overwrite the file?";
                    else
                        msg = "The file already exists. Do you want to append the spectra to the file?";

                    int confirmResult = JOptionPane.showConfirmDialog(this, msg,
                            "Existing file", JOptionPane.YES_NO_CANCEL_OPTION);

                    if (confirmResult == JOptionPane.NO_OPTION)
                        selectedFile = null;

                    if (confirmResult == JOptionPane.CANCEL_OPTION)
                        return;

                }
            } while (selectedFile == null);

            MZmineCore.getTaskController().addTask(new ExportSpectraTask(
                    currentScan, selectedFile, extension));
        }

        if (command.equals("ADD_ISOTOPE_PATTERN")) {

            IsotopePattern newPattern = IsotopePatternCalculator
                    .showIsotopePredictionDialog(this, true);

            if (newPattern == null)
                return;

            loadIsotopes(newPattern);

        }

        if ((command.equals("ZOOM_IN"))
                || (command.equals("ZOOM_IN_BOTH_COMMAND"))) {
            spectrumPlot.getXYPlot().getDomainAxis()
                    .resizeRange(1 / zoomCoefficient);
        }

        if ((command.equals("ZOOM_OUT"))
                || (command.equals("ZOOM_OUT_BOTH_COMMAND"))) {
            spectrumPlot.getXYPlot().getDomainAxis()
                    .resizeRange(zoomCoefficient);
        }

        if (command.equals("SET_SAME_RANGE")) {

            // Get current axes range
            NumberAxis xAxis = (NumberAxis) spectrumPlot.getXYPlot()
                    .getDomainAxis();
            NumberAxis yAxis = (NumberAxis) spectrumPlot.getXYPlot()
                    .getRangeAxis();
            double xMin = (double) xAxis.getRange().getLowerBound();
            double xMax = (double) xAxis.getRange().getUpperBound();
            double xTick = (double) xAxis.getTickUnit().getSize();
            double yMin = (double) yAxis.getRange().getLowerBound();
            double yMax = (double) yAxis.getRange().getUpperBound();
            double yTick = (double) yAxis.getTickUnit().getSize();

            // Get all frames of my class
            Window spectraFrames[] = JFrame.getWindows();

            // Set the range of these frames
            for (Window frame : spectraFrames) {
                if (!(frame instanceof SpectraVisualizerWindow))
                    continue;
                SpectraVisualizerWindow spectraFrame = (SpectraVisualizerWindow) frame;
                spectraFrame.setAxesRange(xMin, xMax, xTick, yMin, yMax, yTick);
            }

        }

    }

    public void addAnnotation(Map<DataPoint, String> annotation) {
        spectrumDataSet.addAnnotation(annotation);
    }

    public SpectraPlot getSpectrumPlot() {
        return spectrumPlot;
    }

    public void addDataSet(XYDataset dataset, Color color) {
        spectrumPlot.addDataSet(dataset, color, true);
    }

}