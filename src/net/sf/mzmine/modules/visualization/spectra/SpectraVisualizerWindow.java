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

package net.sf.mzmine.modules.visualization.spectra;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.util.RawDataAcceptor;
import net.sf.mzmine.io.util.RawDataRetrievalTask;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.RawDataVisualizer;
import net.sf.mzmine.modules.visualization.spectra.SpectraPlot.PlotMode;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.Task.TaskPriority;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.dialogs.AxesSetupDialog;
import net.sf.mzmine.util.CursorPosition;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.ScanUtils;
import net.sf.mzmine.util.ScanUtils.BinningType;

import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;

/**
 * Spectrum visualizer using JFreeChart library
 */
public class SpectraVisualizerWindow extends JInternalFrame implements
        RawDataVisualizer, ActionListener, RawDataAcceptor, TaskListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private SpectraToolBar toolBar;
    private SpectraPlot spectrumPlot;

    private RawDataFile dataFile;
    private RawDataFile rawDataFile;

    private DefaultTableXYDataset rawDataSet, peaksDataSet;
    private XYSeries rawDataSeries, peaksSeries;

    private int scanNumbers[];
    private Scan[] scans;
    private int loadedScans = 0;

    // mzMin, mzMax and mzBinSize/numOfBins and binnedTemplate are only used when we plot multiple
    // scans
    private float mzMin, mzMax, mzBinSize;
    private int numOfBins;
    private float rtMin, rtMax;
    private float[] binnedTemplate;

    private static final float zoomCoefficient = 1.2f;

    private boolean initialPropertiesSet = false;

    private Desktop desktop;

    private JPanel msmsPanel;
    private JComboBox fragmentScansSelector;

    public SpectraVisualizerWindow(RawDataFile dataFile, int scanNumber) {
        this(dataFile, new int[] { scanNumber }, -1);
    }

    public SpectraVisualizerWindow(RawDataFile dataFile, int[] scanNumbers,
            float mzBinSize) {

        super(dataFile.toString(), true, true, true, true);

        this.desktop = MZmineCore.getDesktop();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        toolBar = new SpectraToolBar(this);
        add(toolBar, BorderLayout.EAST);

        msmsPanel = new JPanel();
        msmsPanel.setBackground(Color.white);

        this.dataFile = dataFile;
        this.rawDataFile = dataFile;
        this.mzBinSize = mzBinSize;

        rawDataSet = new DefaultTableXYDataset();
        peaksDataSet = new DefaultTableXYDataset();

        // set minimum width for peak line (in fact, a bar)
        rawDataSet.setIntervalWidth(Double.MIN_VALUE);
        peaksDataSet.setIntervalWidth(Double.MIN_VALUE);

        spectrumPlot = new SpectraPlot(this, rawDataSet, peaksDataSet);
        add(spectrumPlot, BorderLayout.CENTER);

        setScans(scanNumbers);

        pack();

    }

    private void setScans(int scanNumbers[]) {

        this.scanNumbers = scanNumbers;

        // wipe previous data, if any
        rawDataSet.removeAllSeries();
        peaksDataSet.removeAllSeries();
        loadedScans = 0;
        scans = new Scan[scanNumbers.length];

        // create sorted series, for easy searching for local maxima in the
        // label generator
        rawDataSeries = new XYSeries("data", true, false);
        rawDataSet.addSeries(rawDataSeries);

        peaksSeries = new XYSeries("peaks", true, false);
        peaksDataSet.addSeries(peaksSeries);

        Task updateTask = new RawDataRetrievalTask(rawDataFile, scanNumbers,
                "Updating spectrum visualizer of " + rawDataFile, this);

        MZmineCore.getTaskController().addTask(updateTask, TaskPriority.HIGH, this);

    }

    /**
     * @see net.sf.mzmine.modules.RawDataVisualizer#setMZRange(float,
     *      float)
     */
    public void setMZRange(float mzMin, float mzMax) {
        spectrumPlot.getXYPlot().getDomainAxis().setRange(mzMin, mzMax);
    }

    /**
     * @see net.sf.mzmine.modules.RawDataVisualizer#setRTRange(float,
     *      float)
     */
    public void setRTRange(float rtMin, float rtMax) {
        // do nothing

    }

    /**
     * @see net.sf.mzmine.modules.RawDataVisualizer#setIntensityRange(float,
     *      float)
     */
    public void setIntensityRange(float intensityMin, float intensityMax) {
        spectrumPlot.getXYPlot().getRangeAxis().setRange(intensityMin,
                intensityMax);
    }

    synchronized void updateTitle() {

        if (loadedScans == 0)
            return;
        
        NumberFormat rtFormat = desktop.getRTFormat();
        NumberFormat mzFormat = desktop.getMZFormat();
        NumberFormat intensityFormat = desktop.getIntensityFormat();

        StringBuffer title = new StringBuffer();
        title.append("[");
        title.append(dataFile.toString());
        title.append("]: ");

        if (loadedScans == 1) {
            title.append("scan #");
            title.append(scans[0].getScanNumber());
            setTitle(title.toString());

            title.append(", MS");
            title.append(scans[0].getMSLevel());
            title.append(", RT ");
            title.append(rtFormat.format(scans[0].getRetentionTime()));

            title.append(", base peak: ");
            title.append(mzFormat.format(scans[0].getBasePeakMZ()));
            title.append(" m/z (");
            title.append(intensityFormat.format(scans[0].getBasePeakIntensity()));
            title.append(")");

        } else {
            title.append("combination of spectra, RT ");
            title.append(rtFormat.format(scans[0].getRetentionTime()));
            title.append(" - ");
            title.append(rtFormat.format(scans[loadedScans - 1].getRetentionTime()));
            setTitle(title.toString());
            title.append(", MS");
            title.append(scans[0].getMSLevel());
        }

        spectrumPlot.setTitle(title.toString());

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
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
        	AxesSetupDialog dialog = new AxesSetupDialog(desktop.getMainFrame(), spectrumPlot.getChart().getXYPlot());
        	dialog.setVisible(true);
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
            if ((scans.length == 1) && (scans[0] != null)) {
                int msLevel = scans[0].getMSLevel();
                int scanNumbers[] = rawDataFile.getScanNumbers(msLevel);
                int scanIndex = Arrays.binarySearch(scanNumbers,
                        scans[0].getScanNumber());
                if (scanIndex > 0) {
                    int newScans[] = { scanNumbers[scanIndex - 1] };
                    setScans(newScans);
                }
            }
        }

        if (command.equals("NEXT_SCAN")) {
            if ((scans.length == 1) && (scans[0] != null)) {
                int msLevel = scans[0].getMSLevel();
                int scanNumbers[] = rawDataFile.getScanNumbers(msLevel);
                int scanIndex = Arrays.binarySearch(scanNumbers,
                        scans[0].getScanNumber());
                if (scanIndex < (scanNumbers.length - 1)) {
                    int newScans[] = { scanNumbers[scanIndex + 1] };
                    setScans(newScans);
                }
            }
        }

        if (command.equals("ZOOM_IN")) {
            spectrumPlot.getXYPlot().getDomainAxis().resizeRange(
                    1 / zoomCoefficient);
        }

        if (command.equals("ZOOM_OUT")) {
            spectrumPlot.getXYPlot().getDomainAxis().resizeRange(
                    zoomCoefficient);
        }

        if (command.equals("SHOW_PARENT")) {
            new SpectraVisualizerWindow(dataFile,
                    scans[0].getParentScanNumber());
        }

        if (command.equals("SHOW_FRAGMENT")) {
            int fragments[] = scans[0].getFragmentScanNumbers();
            int selectedFragment = fragments[fragmentScansSelector.getSelectedIndex()];
            new SpectraVisualizerWindow(dataFile,
                    selectedFragment);
        }

    }

    /**
     * @see net.sf.mzmine.modules.RawDataVisualizer#getRawDataFiles()
     */
    public RawDataFile[] getRawDataFiles() {
        return new RawDataFile[] { rawDataFile };
    }

    
    /**
     * @see net.sf.mzmine.io.RawDataAcceptor#addScan(net.sf.mzmine.data.Scan)
     */
    public synchronized void addScan(Scan scan, int scanIndex, int total) {

        scans[scanIndex] = scan;

        if (!initialPropertiesSet) {

            // if the scans are centroided, switch to centroid mode
            if (scan.isCentroided() || (scans.length > 1)) {
                spectrumPlot.setPlotMode(PlotMode.CENTROID);
                toolBar.setCentroidButton(false);
            } else {
                spectrumPlot.setPlotMode(PlotMode.CONTINUOUS);
                toolBar.setCentroidButton(true);
            }

            // set the X axis range
            setMZRange(scan.getMZRangeMin(), scan.getMZRangeMax());

            initialPropertiesSet = true;
        }

        if (scanIndex == 0) {

            // set the m/z axis range
            mzMin = scan.getMZRangeMin();
            mzMax = scan.getMZRangeMax();

            // set the initial retention time range
            rtMin = scan.getRetentionTime();
            rtMax = scan.getRetentionTime();

            if (scans.length > 1) {
                // count the number of bins and update the bin size accordingly
                numOfBins = (int) Math.round((mzMax - mzMin) / mzBinSize);
                mzBinSize = (mzMax - mzMin) / numOfBins;
                binnedTemplate = new float[numOfBins];
            }

        } else {
            // update the retention time range
            if (scan.getRetentionTime() < rtMin)
                rtMin = scan.getRetentionTime();
            if (scan.getRetentionTime() > rtMax)
                rtMax = scan.getRetentionTime();
        }

        float mzValues[] = scan.getMZValues();
        float intValues[] = scan.getIntensityValues();

        if (scans.length == 1) {
            // plotting single scan - just add values
            for (int j = 0; j < mzValues.length; j++) {
                rawDataSeries.add(mzValues[j], intValues[j], false);
            }

            remove(msmsPanel);
            
            NumberFormat rtFormat = desktop.getRTFormat();
            NumberFormat mzFormat = desktop.getMZFormat();

            int parentNumber = scan.getParentScanNumber();
            if ((scan.getMSLevel() > 1) && (parentNumber > 0)) {
                msmsPanel.removeAll();
                String labelText = "Parent scan #"
                        + parentNumber
                        + ", RT: "
                        + rtFormat.format(rawDataFile.getScan(parentNumber).getRetentionTime())
                        + ", precursor m/z: " 
                        + mzFormat.format(scan.getPrecursorMZ());
                if (scan.getPrecursorCharge() > 0) labelText += " (chrg " + scan.getPrecursorCharge() + ")"; 
                JLabel label = new JLabel(labelText);
                label.setFont(label.getFont().deriveFont(10f));
                msmsPanel.add(label, BorderLayout.CENTER);
                JButton showButton = GUIUtils.addButton(msmsPanel, "Show",
                        null, this, "SHOW_PARENT");
                showButton.setFont(showButton.getFont().deriveFont(10f));
                showButton.setBackground(Color.white);
                msmsPanel.getLayout().addLayoutComponent(BorderLayout.EAST,
                        showButton);
                add(msmsPanel, BorderLayout.SOUTH);

            } else {
                int fragmentScans[] = scan.getFragmentScanNumbers();
                if (fragmentScans != null) {
                    msmsPanel.removeAll();
                    String labelText = "Fragment scans:";
                    JLabel label = new JLabel(labelText);
                    label.setFont(label.getFont().deriveFont(10f));
                    msmsPanel.add(label, BorderLayout.WEST);
                    fragmentScansSelector = new JComboBox();
                    for (int fragment : fragmentScans) {
                        Scan fragmentScan = rawDataFile.getScan(fragment);
                        String item = "#"
                                + fragment
                                + ", RT: "
                                + rtFormat.format(fragmentScan.getRetentionTime())
                                + ", precursor m/z: "
                                + mzFormat.format(fragmentScan.getPrecursorMZ());
                        fragmentScansSelector.addItem(item);
                    }
                    fragmentScansSelector.setBackground(Color.white);
                    fragmentScansSelector.setFont(fragmentScansSelector.getFont().deriveFont(
                            10f));
                    msmsPanel.add(fragmentScansSelector, BorderLayout.CENTER);

                    JButton showButton = GUIUtils.addButton(msmsPanel, "Show",
                            null, this, "SHOW_FRAGMENT");
                    showButton.setBackground(Color.white);
                    showButton.setFont(showButton.getFont().deriveFont(10f));
                    msmsPanel.getLayout().addLayoutComponent(BorderLayout.EAST,
                            showButton);
                    add(msmsPanel, BorderLayout.SOUTH);

                }
            }

        } else {
            // plotting multiple scans - we have to bin the peaks
            float binnedIntValues[] = ScanUtils.binValues(mzValues, intValues,
                    mzMin, mzMax, numOfBins, false, BinningType.SUM);
            
            for (int j = 0; j < binnedIntValues.length; j++) {
            	
            	if (binnedTemplate[j]<binnedIntValues[j])
            		binnedTemplate[j] = binnedIntValues[j];
            }

        }

        // check if we added last scan
        if (scanIndex == total - 1) {
        	
        	// Make a data series of binned template
        	if (scans.length>1) {
                for (int j = 0; j < binnedTemplate.length; j++) {

                    if (binnedTemplate[j] == 0)
                        continue;

                    float mz = mzMin + (j * mzBinSize);
                    rawDataSeries.add(mz, binnedTemplate[j], false);

                }
        		
        	}

            MZmineProject currentProject = MZmineCore.getCurrentProject();
            
            // if we have a peak list, add the eligible peaks
            PeakList peakList = currentProject.getFilePeakList(dataFile);
            if (peakList != null) {

                toolBar.setPeaksButtonEnabled(true);

                Peak[] peaks = peakList.getPeaksInsideScanAndMZRange(dataFile, rtMin,
                        rtMax, mzMin, mzMax);

                for (int i = 0; i < peaks.length; i++) {

                    if (scans.length == 1) {
						float[][] dataPoints = peaks[i].getRawDatapoints(scanNumbers[0]);
                        for (float[] dataPoint : dataPoints) {
                            peaksSeries.add(dataPoint[0], dataPoint[1], false);
                        }
                    } else {
                        float mz = peaks[i].getMZ();
                        float intensity = peaks[i].getHeight();

                        int index = peaksSeries.indexOf(mz);

                        // if we don't have this m/z value yet, add it.
                        // otherwise make a max of intensities
                        if (index < 0) {
                            peaksSeries.add(mz, intensity, false);
                        } else {
                            float newVal = Math.max(
                                    peaksSeries.getY(index).floatValue(),
                                    intensity);
                            peaksSeries.updateByIndex(index, newVal);
                        }

                    }

                }

            } else {
                // no peaklist
                toolBar.setPeaksButtonEnabled(false);
            }

            rawDataSeries.fireSeriesChanged();
            
        }

        // update the counter of loaded scans
        loadedScans++;

        updateTitle();

    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskFinished(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskFinished(Task task) {
        if (task.getStatus() == TaskStatus.ERROR) {
            logger.severe("Error while updating spectrum visualizer: "
                    + task.getErrorMessage());
            desktop.displayErrorMessage("Error while updating spectrum visualizer: "
                    + task.getErrorMessage());
        }

    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskStarted(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskStarted(Task task) {
        // if we have not added this frame before, do it now
        if (getParent() == null)
            desktop.addInternalFrame(this);
    }

    /**
     * @see net.sf.mzmine.modules.RawDataVisualizer#getCursorPosition()
     */
    public CursorPosition getCursorPosition() {
        return null;
    }

    /**
     * @see net.sf.mzmine.modules.RawDataVisualizer#setCursorPosition(net.sf.mzmine.util.CursorPosition)
     */
    public void setCursorPosition(CursorPosition newPosition) {
        // do nothing

    }

}