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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;

import javax.swing.JInternalFrame;

import net.sf.mzmine.interfaces.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.RawDataFile.PreloadLevel;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.Task.TaskPriority;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.util.CursorPosition;
import net.sf.mzmine.util.RawDataAcceptor;
import net.sf.mzmine.util.RawDataRetrievalTask;
import net.sf.mzmine.visualizers.rawdata.RawDataVisualizer;
import net.sf.mzmine.visualizers.rawdata.spectra.SpectrumPlot.PlotMode;

import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;

/**
 * 
 */
public class SpectrumVisualizer extends JInternalFrame implements
        RawDataVisualizer, ActionListener, RawDataAcceptor, TaskListener {

    // TODO: open a precursor scan/ open dependant MS/MS, zooming by + - keys
    
    private SpectrumToolBar toolBar;
    private SpectrumPlot spectrumPlot;

    private RawDataFile rawDataFile;

    private DefaultTableXYDataset dataset;
    private XYSeries series;

    private Scan[] scans;
    private int loadedScans = 0;

    // TODO: get these from parameter storage
    private static DateFormat rtFormat = new SimpleDateFormat("m:ss");
    private static NumberFormat mzFormat = new DecimalFormat("0.00");
    private static NumberFormat intensityFormat = new DecimalFormat("0.00E0");

    public SpectrumVisualizer(RawDataFile rawDataFile, int scanNumber) {
        this(rawDataFile, new int[] { scanNumber });
    }

    public SpectrumVisualizer(RawDataFile rawDataFile, int[] scanNumbers) {

        super(rawDataFile.toString(), true, true, true, true);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        toolBar = new SpectrumToolBar(this);
        add(toolBar, BorderLayout.EAST);

        this.rawDataFile = rawDataFile;

        dataset = new DefaultTableXYDataset();
        // set minimum width for peak line (in fact, a bar)
        dataset.setIntervalWidth(Double.MIN_VALUE);
        
        // keep the series sorted, for easy searching for local maxima
        series = new XYSeries(rawDataFile.toString(), true, false);
        dataset.addSeries(series);

        spectrumPlot = new SpectrumPlot(this, dataset);
        add(spectrumPlot, BorderLayout.CENTER);

        scans = new Scan[scanNumbers.length];

        Task updateTask = new RawDataRetrievalTask(rawDataFile, scanNumbers,
                this);

        /*
         * if the file data is preloaded in memory, we can update the visualizer
         * in this thread, otherwise start a task
         */
        if (rawDataFile.getPreloadLevel() == PreloadLevel.PRELOAD_ALL_SCANS) {
            taskStarted(updateTask);
            updateTask.run();
            taskFinished(updateTask);
        } else
            TaskController.getInstance().addTask(updateTask, TaskPriority.HIGH,
                    this);

        pack();

    }

    /**
     * @see net.sf.mzmine.visualizers.rawdata.RawDataVisualizer#setMZRange(double,
     *      double)
     */
    public void setMZRange(double mzMin, double mzMax) {
        spectrumPlot.getXYPlot().getDomainAxis().setRange(mzMin, mzMax);
    }

    /**
     * @see net.sf.mzmine.visualizers.rawdata.RawDataVisualizer#setRTRange(double,
     *      double)
     */
    public void setRTRange(double rtMin, double rtMax) {
        // do nothing

    }

    /**
     * @see net.sf.mzmine.visualizers.rawdata.RawDataVisualizer#setIntensityRange(double,
     *      double)
     */
    public void setIntensityRange(double intensityMin, double intensityMax) {
        spectrumPlot.getXYPlot().getRangeAxis().setRange(intensityMin,
                intensityMax);
    }

    void updateTitle() {

        StringBuffer title = new StringBuffer();
        title.append("[");
        title.append(rawDataFile.toString());
        title.append("]: ");

        if (loadedScans == 1) {
            title.append("scan #");
            title.append(scans[0].getScanNumber());
            setTitle(title.toString());

            title.append(", MS");
            title.append(scans[0].getMSLevel());
            title.append(", RT ");
            title.append(rtFormat.format(scans[0].getRetentionTime() * 1000));
            
            title.append(", base peak: ");
            title.append(mzFormat.format(scans[0].getBasePeakMZ()));
            title.append(" m/z (");
            title.append(intensityFormat.format(scans[0].getBasePeakIntensity()));
            title.append(")");
            
        } else {
            title.append("combination of spectra, RT ");
            title.append(rtFormat.format(scans[0].getRetentionTime() * 1000));
            title.append(" - ");
            title.append(rtFormat.format(scans[loadedScans - 1]
                    .getRetentionTime() * 1000));
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

        if (command.equals("TOGGLE_PLOT_MODE")) {
            if (spectrumPlot.getPlotMode() == PlotMode.CONTINUOUS) {
                spectrumPlot.setPlotMode(PlotMode.CENTROID);
                toolBar.setCentroidButton(false);
            } else {
                spectrumPlot.setPlotMode(PlotMode.CONTINUOUS);
                toolBar.setCentroidButton(true);
            }
        }

    }

    /**
     * @see net.sf.mzmine.visualizers.rawdata.RawDataVisualizer#getRawDataFiles()
     */
    public RawDataFile[] getRawDataFiles() {
        return new RawDataFile[] { rawDataFile };
    }

    /**
     * @see net.sf.mzmine.util.RawDataAcceptor#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Updating spectrum visualizer of " + rawDataFile;
    }

    /**
     * @see net.sf.mzmine.util.RawDataAcceptor#addScan(net.sf.mzmine.interfaces.Scan)
     */
    public void addScan(Scan scan) {

        scans[loadedScans++] = scan;

        if (loadedScans == 1) {
            // if the scans are centroided, switch to centroid mode
            if (scan.isCentroided()) {
                spectrumPlot.setPlotMode(PlotMode.CENTROID);
                toolBar.setCentroidButton(false);
            } else {
                spectrumPlot.setPlotMode(PlotMode.CONTINUOUS);
                toolBar.setCentroidButton(true);
            }

        }

        double mzValues[] = scan.getMZValues();
        double intValues[] = scan.getIntensityValues();
        for (int j = 0; j < mzValues.length; j++) {

            int index = series.indexOf(mzValues[j]);
            
            // if we don't have this m/z value yet, add it. 
            // otherwise make a sum of intensities
            if (index < 0) {
                series.add(mzValues[j], intValues[j], false);
            } else {
                double newVal = dataset.getYValue(0, index) + intValues[j];
                series.updateByIndex(index, newVal);
            }

        }

        series.fireSeriesChanged();

        updateTitle();

    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskFinished(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskFinished(Task task) {
        if (task.getStatus() == TaskStatus.ERROR) {
            MainWindow.getInstance().displayErrorMessage(
                    "Error while updating spectrum visualizer: "
                            + task.getErrorMessage());
        }

    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskStarted(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskStarted(Task task) {
        // if we have not added this frame before, do it now
        if (getParent() == null)
            MainWindow.getInstance().addInternalFrame(this);
    }

    /**
     * @see net.sf.mzmine.visualizers.rawdata.RawDataVisualizer#getCursorPosition()
     */
    public CursorPosition getCursorPosition() {
        return null;
    }

    /**
     * @see net.sf.mzmine.visualizers.rawdata.RawDataVisualizer#setCursorPosition(net.sf.mzmine.util.CursorPosition)
     */
    public void setCursorPosition(CursorPosition newPosition) {
        // do nothing
        
    }

}