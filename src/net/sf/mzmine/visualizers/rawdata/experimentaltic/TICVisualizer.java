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

package net.sf.mzmine.visualizers.rawdata.experimentaltic;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.visualizers.RawDataVisualizer;

/**
 * This class defines the total ion chromatogram visualizer for raw data
 */
public class TICVisualizer extends JInternalFrame implements RawDataVisualizer,
        TaskListener, ActionListener {

    private TICToolBar toolBar;
    private TICPlot ticPlot;
    private JLabel titleLabel;

    private Hashtable<RawDataFile, RawDataFileDataSet> rawDataFiles;
    private int msLevel;

    private boolean xicMode = false;

    /**
     * Constructor for total ion chromatogram visualizer
     * 
     */
    public TICVisualizer(RawDataFile rawDataFile, int msLevel) {

        super(rawDataFile.toString() + " TIC", true, true, true, true);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        titleLabel = new JLabel(rawDataFile.toString() + " TIC", JLabel.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        titleLabel.setFont(titleLabel.getFont().deriveFont(11.0f));
        add(titleLabel, BorderLayout.NORTH);

        toolBar = new TICToolBar(this);
        add(toolBar, BorderLayout.EAST);

        ticPlot = new TICPlot(this);
        add(ticPlot, BorderLayout.CENTER);
        
        this.msLevel = msLevel;
        this.rawDataFiles = new Hashtable<RawDataFile,RawDataFileDataSet>();
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
        ticPlot.getPlot().getDomainAxis().setRange(rtMin, rtMax);
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setIntensityRange(double,
     *      double)
     */
    public void setIntensityRange(double intensityMin, double intensityMax) {
        ticPlot.getPlot().getRangeAxis().setRange(intensityMin, intensityMax);
    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskFinished(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskFinished(Task task) {
        if (task.getStatus() == TaskStatus.ERROR) {
            MainWindow.getInstance().displayErrorMessage(
                    "Error while updating TIC visualizer: "
                            + task.getErrorMessage());
        }

    }

    public void taskStarted(Task task) {
        // if we have not added this frame before, do it now
        if (getParent() == null)
            MainWindow.getInstance().addInternalFrame(this);
    }
    
    void addRawDataFile(RawDataFile newFile) {
        RawDataFileDataSet dataset = new RawDataFileDataSet(newFile, msLevel, this);
        rawDataFiles.put(newFile, dataset);
        ticPlot.addDataset(dataset);
        if (rawDataFiles.size() == 1) {
            setRTRange(newFile.getDataMinRT() * 1000, newFile.getDataMaxRT() * 1000);
            setIntensityRange(0, newFile.getDataMaxTotalIonCurrent(msLevel) * 1.05);
        }
    }
    
    void removeRawDataFile(RawDataFile file) {
        RawDataFileDataSet dataset = rawDataFiles.get(file);
        ticPlot.getPlot().setDataset(ticPlot.getPlot().indexOf(dataset), null);
        rawDataFiles.remove(file);
    }
    

    void updateTitle() {
        DateFormat rtFormat = new SimpleDateFormat("m:ss"); // TODO
        NumberFormat intensityFormat = new DecimalFormat("0.00E0");

        String TICXIC = xicMode ? "XIC" : "TIC";
        String scan = "";
        setTitle(rawDataFiles.keySet().toString() + " " + TICXIC + " MS " + msLevel);

        /*for (int i = 0; i < ticPlot.getDataset().getItemCount(); i++) {
            double val = ticPlot.getDataset().getXValue(0, i);
            if (val == ticPlot.getPlot().getDomainCrosshairValue()) {
                scan = ", scan #"
                        + scanNumbers[i]
                        + ", RT: "
                        + rtFormat.format(ticPlot.getPlot()
                                .getDomainCrosshairValue())
                        + ", IC: "
                        + intensityFormat.format(ticPlot.getDataset()
                                .getYValue(0, i));
                break;
            }
        }
        */
        titleLabel.setText(rawDataFiles.keySet().toString() + TICXIC + " MS " + msLevel + scan);

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        if (command.equals("SHOW_DATA_POINTS")) {
//            ticPlot.getRenderer().setBaseShapesVisible(
                    //!ticPlot.getRenderer().getBaseShapesVisible());
        }

        if (command.equals("SHOW_ANNOTATIONS")) {
//            ticPlot.getRenderer().setItemLabelsVisible(
                    ///!ticPlot.getRenderer().isSeriesItemLabelsVisible(0));
        }

        if (command.equals("SHOW_SPECTRUM")) {

/*            for (int i = 0; i < ticPlot.getSeries().getItemCount(); i++) {
                double val = ticPlot.getDataset().getXValue(0, i);
                if (val == ticPlot.getPlot().getDomainCrosshairValue()) {
                    SpectrumVisualizer specVis = new SpectrumVisualizer(
                            rawDataFile, scanNumbers[i]);
                    MainWindow.getInstance().addInternalFrame(specVis);
                    return;
                }
            }
            */

        }

        if (command.equals("CHANGE_XIC_TIC")) {

            if (xicMode) {
                xicMode = false;
                toolBar.setXicButton(true);

                updateTitle();
/*                Task updateTask = new TICDataRetrievalTask(rawDataFile,
                        scanNumbers, ticPlot.getSeries());
                /*
                 * if the file data is preloaded in memory, we can update the
                 * visualizer in this thread, otherwise start a task
                 
                if (rawDataFile.getPreloadLevel() == PreloadLevel.PRELOAD_ALL_SCANS)
                    updateTask.run();
                else
                    TaskController.getInstance().addTask(updateTask, this);
*/
            } else {

                // Default range is cursor location +- 0.25
                double ricMZ = 0;
                double ricMZDelta = (double) 0.25;

                // Show dialog
                XICSetupDialog psd = new XICSetupDialog(
                        "Please give centroid and delta MZ values for XIC",
                        ricMZ, ricMZDelta);
                psd.setVisible(true);
                // if cancel was clicked
                if (psd.getExitCode() == -1) {
                    MainWindow.getInstance().getStatusBar().setStatusText(
                            "Switch to XIC cancelled.");
                    return;
                }

                // Validate given parameter values
                ricMZ = psd.getXicMZ();
                if (ricMZ < 0) {
                    MainWindow.getInstance().getStatusBar().setStatusText(
                            "Error: incorrect parameter values.");
                    return;
                }

                ricMZDelta = psd.getXicMZDelta();
                if (ricMZDelta < 0) {
                    MainWindow.getInstance().getStatusBar().setStatusText(
                            "Error: incorrect parameter values.");
                    return;
                }

                xicMode = true;
                toolBar.setXicButton(false);

                updateTitle();
/*                Task updateTask = new TICDataRetrievalTask(rawDataFile,
                        scanNumbers, ticPlot.getSeries(), ricMZ - ricMZDelta,
                        ricMZ + ricMZDelta);
                /*
                 * if the file data is preloaded in memory, we can update the
                 * visualizer in this thread, otherwise start a task
                 */
                /*if (rawDataFile.getPreloadLevel() == PreloadLevel.PRELOAD_ALL_SCANS) {
                    taskStarted(updateTask);
                    updateTask.run();
                    taskFinished(updateTask);
                } else
                    TaskController.getInstance().addTask(updateTask, this);
*/
            }
        }

    }



    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#getRawDataFiles()
     */
    public RawDataFile[] getRawDataFiles() {
        return rawDataFiles.keySet().toArray(new RawDataFile[0]);
    }

}
