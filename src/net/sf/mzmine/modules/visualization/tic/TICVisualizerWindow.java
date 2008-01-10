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

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizer;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.dialogs.AxesSetupDialog;
import net.sf.mzmine.util.CursorPosition;

/**
 * Total ion chromatogram visualizer using JFreeChart library
 */
public class TICVisualizerWindow extends JInternalFrame implements
        TaskListener, ActionListener {

    private TICToolBar toolBar;
    private TICPlot plot;

    // Data sets
    private Hashtable<RawDataFile, TICDataSet> ticDataSets;

    private Object plotType;
    private int msLevel;
    private float rtMin, rtMax, mzMin, mzMax;

    private static final double zoomCoefficient = 1.2;

    private Desktop desktop;

    /**
     * Constructor for total ion chromatogram visualizer
     * 
     */
    public TICVisualizerWindow(RawDataFile dataFiles[], Object plotType,
            int msLevel, float rtMin, float rtMax, float mzMin, float mzMax,
            Peak[] peaks) {

        super(null, true, true, true, true);

        this.desktop = MZmineCore.getDesktop();
        this.plotType = plotType;
        this.msLevel = msLevel;
        this.ticDataSets = new Hashtable<RawDataFile, TICDataSet>();
        this.rtMin = rtMin;
        this.rtMax = rtMax;
        this.mzMin = mzMin;
        this.mzMax = mzMax;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        toolBar = new TICToolBar(this);
        add(toolBar, BorderLayout.EAST);

        plot = new TICPlot(this);
        add(plot, BorderLayout.CENTER);

        // add all peaks
        if (peaks != null) {
            for (Peak peak : peaks) {
                PeakDataSet peakDataSet = new PeakDataSet(peak);
                plot.addPeakDataset(peakDataSet);
            }
        }

        // add all data files
        for (RawDataFile dataFile : dataFiles)
            addRawDataFile(dataFile);

        pack();

    }

    void updateTitle() {

        NumberFormat rtFormat = desktop.getRTFormat();
        NumberFormat mzFormat = desktop.getMZFormat();
        NumberFormat intensityFormat = desktop.getIntensityFormat();

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
                if ((df.getDataMinMZ(msLevel) < mzMin)
                        || (df.getDataMinMZ(msLevel) > mzMax)) {
                    ticOrXIC = "XIC";
                    break;
                }
            }
            mainTitle.append(ticOrXIC);
        }

        mainTitle.append(", MS" + msLevel);
        mainTitle.append(", m/z: " + mzFormat.format(mzMin) + " - "
                + mzFormat.format(mzMax));

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
        plot.setTitle(mainTitle.toString(), subTitle.toString());

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
     * @see net.sf.mzmine.modules.RawDataVisualizer#setRTRange(double, double)
     */
    public void setRTRange(double rtMin, double rtMax) {
        plot.getXYPlot().getDomainAxis().setRange(rtMin, rtMax);
    }

    /**
     * @see net.sf.mzmine.modules.RawDataVisualizer#setIntensityRange(double,
     *      double)
     */
    public void setIntensityRange(double intensityMin, double intensityMax) {
        plot.getXYPlot().getRangeAxis().setRange(intensityMin, intensityMax);
    }

    /**
     * @see net.sf.mzmine.modules.RawDataVisualizer#getRawDataFiles()
     */
    public RawDataFile[] getRawDataFiles() {
        return ticDataSets.keySet().toArray(new RawDataFile[0]);
    }

    public void addRawDataFile(RawDataFile newFile) {

        int scanNumbers[] = newFile.getScanNumbers(msLevel, rtMin, rtMax);
        if (scanNumbers.length == 0) {
            desktop.displayErrorMessage("No scans found at MS level " + msLevel
                    + " within given retention time range.");
            return;
        }

        TICDataSet ticDataset = new TICDataSet(newFile, scanNumbers, mzMin,
                mzMax, this);
        ticDataSets.put(newFile, ticDataset);
        plot.addTICDataset(ticDataset);

        if (ticDataSets.size() == 1) {
            // when adding first file, set the retention time range
            setRTRange(rtMin, rtMax);
        }

    }

    public void removeRawDataFile(RawDataFile file) {
        TICDataSet dataset = ticDataSets.get(file);
        plot.getXYPlot().setDataset(plot.getXYPlot().indexOf(dataset), null);
        ticDataSets.remove(file);
    }

    /**
     * @return current cursor position
     */
    public CursorPosition getCursorPosition() {
        float selectedRT = (float) plot.getXYPlot().getDomainCrosshairValue();
        float selectedIT = (float) plot.getXYPlot().getRangeCrosshairValue();
        Enumeration<TICDataSet> e = ticDataSets.elements();
        while (e.hasMoreElements()) {
            TICDataSet dataSet = e.nextElement();
            int index = dataSet.getIndex(selectedRT, selectedIT);
            if (index >= 0) {
                float mz = 0;
                if (plotType == TICVisualizerParameters.plotTypeBP)
                    mz = (float) dataSet.getZValue(0, index);
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
        plot.getXYPlot().setDomainCrosshairValue(
                newPosition.getRetentionTime(), false);
        plot.getXYPlot().setRangeCrosshairValue(newPosition.getIntensityValue());
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

        if (command.equals("SHOW_DATA_POINTS")) {
            plot.switchDataPointsVisible();
        }

        if (command.equals("SHOW_ANNOTATIONS")) {
            plot.switchItemLabelsVisible();
        }

        if (command.equals("SHOW_SPECTRUM")) {
            CursorPosition pos = getCursorPosition();
            if (pos != null) {
                SpectraVisualizer specVis = SpectraVisualizer.getInstance();
                specVis.showNewSpectraWindow(pos.getDataFile(),
                        pos.getScanNumber());
            }
        }

        if (command.equals("SETUP_AXES")) {
            AxesSetupDialog dialog = new AxesSetupDialog(
                    desktop.getMainFrame(), plot.getChart().getXYPlot());
            dialog.setVisible(true);
        }

        if (command.equals("MOVE_CURSOR_LEFT")) {
            CursorPosition pos = getCursorPosition();
            if (pos != null) {
                TICDataSet dataSet = ticDataSets.get(pos.getDataFile());
                int index = dataSet.getIndex(pos.getRetentionTime(),
                        pos.getIntensityValue());
                if (index > 0) {
                    index--;
                    pos.setRetentionTime((float) dataSet.getXValue(0, index));
                    pos.setIntensityValue((float) dataSet.getYValue(0, index));
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
                        pos.setRetentionTime((float) dataSet.getXValue(0, index));
                        pos.setIntensityValue((float) dataSet.getYValue(0,
                                index));
                        setCursorPosition(pos);
                    }
                }
            }
        }

        if (command.equals("ZOOM_IN")) {
            plot.getXYPlot().getDomainAxis().resizeRange(1 / zoomCoefficient);
        }

        if (command.equals("ZOOM_OUT")) {
            plot.getXYPlot().getDomainAxis().resizeRange(zoomCoefficient);
        }

    }

}
