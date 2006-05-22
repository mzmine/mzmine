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

import javax.swing.BorderFactory;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.visualizers.RawDataVisualizer;
import net.sf.mzmine.visualizers.rawdata.spectra.SpectrumVisualizer;

/**
 * This class defines a base peak intensity visualizer for raw data
 */
public class BasePeakVisualizer extends JInternalFrame implements RawDataVisualizer,
        TaskListener, ActionListener {

    private BasePeakToolBar toolBar;
    private BasePeakPlot basePeakPlot;
    private JLabel titleLabel;

    private Hashtable<RawDataFile, BasePeakDataSet> rawDataFiles;
    private int msLevel;

    // TODO: get these from parameter storage
    private static DateFormat rtFormat = new SimpleDateFormat("m:ss");
    private static NumberFormat intensityFormat = new DecimalFormat("0.00E0");

    /**
     * Constructor for total ion chromatogram visualizer
     * 
     */
    public BasePeakVisualizer(RawDataFile rawDataFile, int msLevel) {

        super(rawDataFile.toString() + " base peak intensity", true, true, true, true);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        titleLabel = new JLabel(rawDataFile.toString() + " base peak intensity", JLabel.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        titleLabel.setFont(titleLabel.getFont().deriveFont(11.0f));
        add(titleLabel, BorderLayout.NORTH);

        toolBar = new BasePeakToolBar(this);
        add(toolBar, BorderLayout.EAST);

        basePeakPlot = new BasePeakPlot(this);
        add(basePeakPlot, BorderLayout.CENTER);

        this.msLevel = msLevel;
        this.rawDataFiles = new Hashtable<RawDataFile, BasePeakDataSet>();

        addRawDataFile(rawDataFile);

        pack();

    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setMZRange(double,
     *      double)
     */
    public void setMZRange(double mzMin, double mzMax) {
        // do nothing
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setRTRange(double,
     *      double)
     */
    public void setRTRange(double rtMin, double rtMax) {
        basePeakPlot.getPlot().getDomainAxis().setRange(rtMin, rtMax);
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setIntensityRange(double,
     *      double)
     */
    public void setIntensityRange(double intensityMin, double intensityMax) {
        basePeakPlot.getPlot().getRangeAxis().setRange(intensityMin, intensityMax);
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#getRawDataFiles()
     */
    public RawDataFile[] getRawDataFiles() {
        return rawDataFiles.keySet().toArray(new RawDataFile[0]);
    }
 

    void addRawDataFile(RawDataFile newFile) {
        BasePeakDataSet dataset = new BasePeakDataSet(newFile, msLevel,
                this);
        rawDataFiles.put(newFile, dataset);
        basePeakPlot.addDataset(dataset);
        if (rawDataFiles.size() == 1) {
            setRTRange(newFile.getDataMinRT() * 1000,
                    newFile.getDataMaxRT() * 1000);
            setIntensityRange(0,
                    newFile.getDataMaxBasePeakIntensity(msLevel) * 1.05);
        }

        // when displaying more than one file, show a legend
        if (rawDataFiles.size() > 1) {
            basePeakPlot.showLegend(true);
        }

    }

    void removeRawDataFile(RawDataFile file) {
        BasePeakDataSet dataset = rawDataFiles.get(file);
        basePeakPlot.getPlot().setDataset(basePeakPlot.getPlot().indexOf(dataset), null);
        rawDataFiles.remove(file);

        // when displaying less than two files, hide a legend
        if (rawDataFiles.size() < 2) {
            basePeakPlot.showLegend(false);
        }

    }

    void updateTitle() {

        String scan = "", selectedValue = "";
        setTitle("Base peak intensity " + rawDataFiles.keySet().toString() + " MS"
                + msLevel);

        double selectedRT = basePeakPlot.getPlot().getDomainCrosshairValue();
        double selectedIT = basePeakPlot.getPlot().getRangeCrosshairValue();

        if (selectedIT > 0) {
            selectedValue = ", RT: " + rtFormat.format(selectedRT) + ", IC: "
                    + intensityFormat.format(selectedIT);
        }
        Enumeration<BasePeakDataSet> e = rawDataFiles.elements();
        while (e.hasMoreElements()) {
            BasePeakDataSet dataSet = e.nextElement();
            int index = dataSet.getSeriesIndex(selectedRT, selectedIT);
            if (index >= 0) {
                int scanNumber = dataSet.getScanNumber(index);
                scan = ", scan #" + scanNumber;
                if (rawDataFiles.size() > 1)
                    scan += " (" + dataSet.getRawDataFile() + ")";
                break;
            }
        }

        String newLabel = "Base peak intensity " + rawDataFiles.keySet().toString()
                + " MS" + msLevel + scan + selectedValue;
        titleLabel.setText(newLabel);

    }
    
    void showSpectrum() {
        double selectedRT = basePeakPlot.getPlot().getDomainCrosshairValue();
        double selectedIT = basePeakPlot.getPlot().getRangeCrosshairValue();
        Enumeration<BasePeakDataSet> e = rawDataFiles.elements();
        while (e.hasMoreElements()) {
            BasePeakDataSet dataSet = e.nextElement();
            int index = dataSet.getSeriesIndex(selectedRT, selectedIT);
            if (index >= 0) {
                int scanNumber = dataSet.getScanNumber(index);
                SpectrumVisualizer specVis = new SpectrumVisualizer(dataSet
                        .getRawDataFile(), scanNumber);
                MainWindow.getInstance().addInternalFrame(specVis);
                return;
            }
        }
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
            basePeakPlot.switchDataPointsVisible();
        }

        if (command.equals("SHOW_ANNOTATIONS")) {
            basePeakPlot.switchItemLabelsVisible();
        }

        if (command.equals("SHOW_SPECTRUM")) {
            showSpectrum();
        }

    }


}
