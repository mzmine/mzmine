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

package net.sf.mzmine.visualizers.rawdata.basepeak;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.JInternalFrame;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;
import net.sf.mzmine.userinterface.dialogs.OpenScansDialog;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.util.CursorPosition;
import net.sf.mzmine.visualizers.rawdata.MultipleRawDataVisualizer;
import net.sf.mzmine.visualizers.rawdata.spectra.SpectrumVisualizer;

/**
 * This class defines a base peak intensity visualizer for raw data
 */
public class BasePeakVisualizer extends JInternalFrame implements
        MultipleRawDataVisualizer, TaskListener, ActionListener {

    private BasePeakToolBar toolBar;
    private BasePeakPlot plot;

    private Hashtable<RawDataFile, BasePeakDataSet> rawDataFiles;
    private int msLevel;

    // TODO: get these from parameter storage
    private static DateFormat rtFormat = new SimpleDateFormat("m:ss");
    private static NumberFormat intensityFormat = new DecimalFormat("0.00E0");
    private static NumberFormat mzFormat = new DecimalFormat("0.00");

    private static final double zoomCoefficient = 1.2;

    /**
     * Constructor for total ion chromatogram visualizer
     * 
     */
    public BasePeakVisualizer(RawDataFile rawDataFile, int msLevel,
            double rtMin, double rtMax) {

        super(rawDataFile.toString() + " base peak intensity", true, true,
                true, true);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        toolBar = new BasePeakToolBar(this);
        add(toolBar, BorderLayout.EAST);

        plot = new BasePeakPlot(this);
        add(plot, BorderLayout.CENTER);

        this.msLevel = msLevel;
        this.rawDataFiles = new Hashtable<RawDataFile, BasePeakDataSet>();

        addRawDataFile(rawDataFile);

        pack();

    }

    void updateTitle() {

        StringBuffer title = new StringBuffer();
        title.append(rawDataFiles.keySet().toString() + ": Base peak intensity");
        title.append(" MS" + msLevel);

        setTitle(title.toString());

        CursorPosition pos = getCursorPosition();

        if (pos != null) {
            title.append(", scan #");
            title.append(pos.getScanNumber());
            if (rawDataFiles.size() > 1)
                title.append(" (" + pos.getRawDataFile() + ")");
            title.append(", RT: " + rtFormat.format(pos.getRetentionTime()));
            title.append(", base peak: " + mzFormat.format(pos.getMzValue()));
            title.append(" m/z, IC: "
                    + intensityFormat.format(pos.getIntensityValue()));
        }

        plot.setTitle(title.toString());

    }

    /**
     * @see net.sf.mzmine.visualizers.rawdata.RawDataVisualizer#setMZRange(double,
     *      double)
     */
    public void setMZRange(double mzMin, double mzMax) {
        // do nothing
    }

    /**
     * @see net.sf.mzmine.visualizers.rawdata.RawDataVisualizer#setRTRange(double,
     *      double)
     */
    public void setRTRange(double rtMin, double rtMax) {
        plot.getXYPlot().getDomainAxis().setRange(rtMin, rtMax);
    }

    /**
     * @see net.sf.mzmine.visualizers.rawdata.RawDataVisualizer#setIntensityRange(double,
     *      double)
     */
    public void setIntensityRange(double intensityMin, double intensityMax) {
        plot.getXYPlot().getRangeAxis().setRange(intensityMin, intensityMax);
    }

    /**
     * @see net.sf.mzmine.visualizers.rawdata.RawDataVisualizer#getRawDataFiles()
     */
    public RawDataFile[] getRawDataFiles() {
        return rawDataFiles.keySet().toArray(new RawDataFile[0]);
    }

    public void addRawDataFile(RawDataFile newFile) {
        BasePeakDataSet dataset = new BasePeakDataSet(newFile, msLevel, this);
        rawDataFiles.put(newFile, dataset);
        plot.addDataset(dataset);
        if (rawDataFiles.size() == 1) {
            setRTRange(newFile.getDataMinRT(msLevel) * 1000,
                    newFile.getDataMaxRT(msLevel) * 1000);
            setIntensityRange(0,
                    newFile.getDataMaxBasePeakIntensity(msLevel) * 1.05);
        }

        // when displaying more than one file, show a legend
        if (rawDataFiles.size() > 1) {
            plot.showLegend(true);
        }

    }

    public void removeRawDataFile(RawDataFile file) {
        BasePeakDataSet dataset = rawDataFiles.get(file);
        plot.getXYPlot().setDataset(plot.getXYPlot().indexOf(dataset), null);
        rawDataFiles.remove(file);

        // when displaying less than two files, hide a legend
        if (rawDataFiles.size() < 2) {
            plot.showLegend(false);
        }

    }

    /**
     * @return current cursor position
     */
    public CursorPosition getCursorPosition() {
        double selectedRT = plot.getXYPlot().getDomainCrosshairValue();
        double selectedIT = plot.getXYPlot().getRangeCrosshairValue();
        Enumeration<BasePeakDataSet> e = rawDataFiles.elements();
        while (e.hasMoreElements()) {
            BasePeakDataSet dataSet = e.nextElement();
            int index = dataSet.getSeriesIndex(selectedRT, selectedIT);
            if (index >= 0) {
                double basePeakMz = dataSet.getMZValue(index);
                CursorPosition pos = new CursorPosition(selectedRT, basePeakMz,
                        selectedIT, dataSet.getRawDataFile(),
                        dataSet.getScanNumber(index));
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
            MainWindow.getInstance().displayErrorMessage(
                    "Error while updating base peak visualizer: "
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
                new SpectrumVisualizer(pos.getRawDataFile(),
                        pos.getScanNumber());
            }
        }

        if (command.equals("MOVE_CURSOR_LEFT")) {
            CursorPosition pos = getCursorPosition();
            if (pos != null) {
                BasePeakDataSet dataSet = rawDataFiles.get(pos.getRawDataFile());
                int index = dataSet.getSeriesIndex(pos.getRetentionTime(),
                        pos.getIntensityValue());
                if (index > 0) {
                    index--;
                    pos.setRetentionTime(dataSet.getXValue(0, index));
                    pos.setIntensityValue(dataSet.getYValue(0, index));
                    setCursorPosition(pos);

                }
            }
        }

        if (command.equals("MOVE_CURSOR_RIGHT")) {
            CursorPosition pos = getCursorPosition();
            if (pos != null) {
                BasePeakDataSet dataSet = rawDataFiles.get(pos.getRawDataFile());
                int index = dataSet.getSeriesIndex(pos.getRetentionTime(),
                        pos.getIntensityValue());
                if (index >= 0) {
                    index++;
                    if (index < dataSet.getItemCount(0)) {
                        pos.setRetentionTime(dataSet.getXValue(0, index));
                        pos.setIntensityValue(dataSet.getYValue(0, index));
                        setCursorPosition(pos);
                    }
                }
            }
        }

        if (command.equals("ZOOM_IN")) {
            plot.getXYPlot().getDomainAxis().resizeRange(1 / zoomCoefficient);
            plot.getXYPlot().getRangeAxis().resizeRange(1 / zoomCoefficient);
        }

        if (command.equals("ZOOM_OUT")) {
            plot.getXYPlot().getDomainAxis().resizeRange(zoomCoefficient);
            plot.getXYPlot().getRangeAxis().resizeRange(zoomCoefficient);
        }

        if (command.equals("SHOW_MULTIPLE_SPECTRA")) {
            CursorPosition pos = getCursorPosition();
            if (pos != null) {
                if (pos != null) {
                    int scanNumbers[] = pos.getRawDataFile().getScanNumbers(
                            msLevel);
                    OpenScansDialog dialog = new OpenScansDialog(
                            pos.getRawDataFile(), scanNumbers,
                            pos.getScanNumber(), pos.getScanNumber());
                    dialog.setVisible(true);
                }
            }
        }

    }

}
