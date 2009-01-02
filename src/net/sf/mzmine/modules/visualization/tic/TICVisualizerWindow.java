/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.tic;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;

import javax.swing.JInternalFrame;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizer;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;
import net.sf.mzmine.util.Range;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;

/**
 * Total ion chromatogram visualizer using JFreeChart library
 */
public class TICVisualizerWindow extends JInternalFrame implements
        TaskListener, ActionListener {

    private TICToolBar toolBar;
    private TICPlot ticPlot;

    // Data sets
    private Hashtable<RawDataFile, TICDataSet> ticDataSets;

    private Object plotType;
    private int msLevel;
    private Range rtRange, mzRange;

    private Desktop desktop;

    /**
     * Constructor for total ion chromatogram visualizer
     * 
     */
    public TICVisualizerWindow(RawDataFile dataFiles[], Object plotType,
            int msLevel, Range rtRange, Range mzRange,
            ChromatographicPeak[] peaks) {

        super(null, true, true, true, true);

        this.desktop = MZmineCore.getDesktop();
        this.plotType = plotType;
        this.msLevel = msLevel;
        this.ticDataSets = new Hashtable<RawDataFile, TICDataSet>();
        this.rtRange = rtRange;
        this.mzRange = mzRange;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        ticPlot = new TICPlot(this);
        add(ticPlot, BorderLayout.CENTER);

        // toolBar = new TICToolBar(this);
        toolBar = new TICToolBar(ticPlot);
        add(toolBar, BorderLayout.EAST);

        // add all peaks
        if (peaks != null) {
            for (ChromatographicPeak peak : peaks) {
                PeakDataSet peakDataSet = new PeakDataSet(peak);
                ticPlot.addPeakDataset(peakDataSet);
            }
        }

        // add all data files
        for (RawDataFile dataFile : dataFiles)
            addRawDataFile(dataFile);

        pack();

    }

    void updateTitle() {

        NumberFormat rtFormat = MZmineCore.getRTFormat();
        NumberFormat mzFormat = MZmineCore.getMZFormat();
        NumberFormat intensityFormat = MZmineCore.getIntensityFormat();

        StringBuffer mainTitle = new StringBuffer();
        StringBuffer subTitle = new StringBuffer();

        if (plotType == TICVisualizerParameters.plotTypeBP)
            mainTitle.append("Base peak plot");
        else {
            // If all datafiles have m/z range less than or equal to range of
            // the plot (mzMin, mzMax), then call this TIC, otherwise XIC
            Set<RawDataFile> fileSet = ticDataSets.keySet();
            String ticOrXIC = "TIC";
            for (RawDataFile df : fileSet) {
                if (!mzRange.containsRange(df.getDataMZRange(msLevel))) {
                    ticOrXIC = "XIC";
                    break;
                }
            }
            mainTitle.append(ticOrXIC);
        }

        mainTitle.append(", MS" + msLevel);
        mainTitle.append(", m/z: " + mzFormat.format(mzRange.getMin()) + " - "
                + mzFormat.format(mzRange.getMax()));

        CursorPosition pos = getCursorPosition();

        if (pos != null) {
            subTitle.append("Selected scan #");
            subTitle.append(pos.getScanNumber());
            if (ticDataSets.size() > 1)
                subTitle.append(" (" + pos.getDataFile() + ")");
            subTitle.append(", RT: " + rtFormat.format(pos.getRetentionTime()));
            if (plotType == TICVisualizerParameters.plotTypeBP)
                subTitle.append(", base peak: "
                        + mzFormat.format(pos.getMzValue()) + " m/z");
            subTitle.append(", IC: "
                    + intensityFormat.format(pos.getIntensityValue()));
        }

        // update window title
        setTitle(ticDataSets.keySet().toString() + " chromatogram");

        // update plot title
        ticPlot.setTitle(mainTitle.toString(), subTitle.toString());

    }

    /**
     * @return Returns the plotType.
     */
    Object getPlotType() {
        return plotType;
    }

    TICDataSet[] getAllDataSets() {
        return ticDataSets.values().toArray(new TICDataSet[0]);
    }

    /**
     */
    public void setRTRange(Range rtRange) {
        ticPlot.getXYPlot().getDomainAxis().setRange(rtRange.getMin(),
                rtRange.getMax());
    }

    public void setAxesRange(double xMin, double xMax, double xTickSize,
            double yMin, double yMax, double yTickSize) {
        NumberAxis xAxis = (NumberAxis) ticPlot.getXYPlot().getDomainAxis();
        NumberAxis yAxis = (NumberAxis) ticPlot.getXYPlot().getRangeAxis();
        xAxis.setRange(xMin, xMax);
        xAxis.setTickUnit(new NumberTickUnit(xTickSize));
        yAxis.setRange(yMin, yMax);
        yAxis.setTickUnit(new NumberTickUnit(yTickSize));
    }

    /**
     * @see net.sf.mzmine.modules.RawDataVisualizer#setIntensityRange(double,
     *      double)
     */
    public void setIntensityRange(double intensityMin, double intensityMax) {
        ticPlot.getXYPlot().getRangeAxis().setRange(intensityMin, intensityMax);
    }

    /**
     * @see net.sf.mzmine.modules.RawDataVisualizer#getRawDataFiles()
     */
    public RawDataFile[] getRawDataFiles() {
        return ticDataSets.keySet().toArray(new RawDataFile[0]);
    }

    public void addRawDataFile(RawDataFile newFile) {

        int scanNumbers[] = newFile.getScanNumbers(msLevel, rtRange);
        if (scanNumbers.length == 0) {
            desktop.displayErrorMessage("No scans found at MS level " + msLevel
                    + " within given retention time range.");
            return;
        }

        TICDataSet ticDataset = new TICDataSet(newFile, scanNumbers, mzRange,
                this);
        ticDataSets.put(newFile, ticDataset);
        ticPlot.addTICDataset(ticDataset);

        if (ticDataSets.size() == 1) {
            // when adding first file, set the retention time range
            setRTRange(rtRange);
        }

    }

    public void removeRawDataFile(RawDataFile file) {
        TICDataSet dataset = ticDataSets.get(file);
        ticPlot.getXYPlot().setDataset(ticPlot.getXYPlot().indexOf(dataset),
                null);
        ticDataSets.remove(file);
    }

    /**
     * @return current cursor position
     */
    public CursorPosition getCursorPosition() {
        double selectedRT = (double) ticPlot.getXYPlot().getDomainCrosshairValue();
        double selectedIT = (double) ticPlot.getXYPlot().getRangeCrosshairValue();
        Enumeration<TICDataSet> e = ticDataSets.elements();
        while (e.hasMoreElements()) {
            TICDataSet dataSet = e.nextElement();
            int index = dataSet.getIndex(selectedRT, selectedIT);
            if (index >= 0) {
                double mz = 0;
                if (plotType == TICVisualizerParameters.plotTypeBP)
                    mz = (double) dataSet.getZValue(0, index);
                CursorPosition pos = new CursorPosition(selectedRT, mz,
                        selectedIT, dataSet.getDataFile(),
                        dataSet.getScanNumber(0, index));
                return pos;
            }
        }
        return null;
    }

    /**
     * @return current cursor position
     */
    public void setCursorPosition(CursorPosition newPosition) {
        ticPlot.getXYPlot().setDomainCrosshairValue(
                newPosition.getRetentionTime(), false);
        ticPlot.getXYPlot().setRangeCrosshairValue(
                newPosition.getIntensityValue());
    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskFinished(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskFinished(Task task) {
        if (task.getStatus() == TaskStatus.ERROR) {
            desktop.displayErrorMessage("Error while updating TIC visualizer: "
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
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        if (command.equals("SHOW_SPECTRUM")) {
            CursorPosition pos = getCursorPosition();
            if (pos != null) {
                SpectraVisualizer.showNewSpectrumWindow(pos.getDataFile(),
                        pos.getScanNumber());
            }
        }

        if (command.equals("MOVE_CURSOR_LEFT")) {
            CursorPosition pos = getCursorPosition();
            if (pos != null) {
                TICDataSet dataSet = ticDataSets.get(pos.getDataFile());
                int index = dataSet.getIndex(pos.getRetentionTime(),
                        pos.getIntensityValue());
                if (index > 0) {
                    index--;
                    pos.setRetentionTime((double) dataSet.getXValue(0, index));
                    pos.setIntensityValue((double) dataSet.getYValue(0, index));
                    setCursorPosition(pos);

                }
            }
        }

        if (command.equals("MOVE_CURSOR_RIGHT")) {
            CursorPosition pos = getCursorPosition();
            if (pos != null) {
                TICDataSet dataSet = ticDataSets.get(pos.getDataFile());
                int index = dataSet.getIndex(pos.getRetentionTime(),
                        pos.getIntensityValue());
                if (index >= 0) {
                    index++;
                    if (index < dataSet.getItemCount(0)) {
                        pos.setRetentionTime((double) dataSet.getXValue(0,
                                index));
                        pos.setIntensityValue((double) dataSet.getYValue(0,
                                index));
                        setCursorPosition(pos);
                    }
                }
            }
        }

    }

}
