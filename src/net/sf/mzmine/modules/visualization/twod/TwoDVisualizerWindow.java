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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.twod;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JInternalFrame;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.dialogs.AxesSetupDialog;

/**
 * 2D visualizer using JFreeChart library
 */
public class TwoDVisualizerWindow extends JInternalFrame implements
        ActionListener, TaskListener {

    private TwoDToolBar toolBar;
    private TwoDPlot twoDPlot;
    private TwoDBottomPanel bottomPanel;

    private TwoDDataSet dataset;

    private RawDataFile dataFile;
    private int msLevel;

    private Desktop desktop;

    public TwoDVisualizerWindow(RawDataFile dataFile, int msLevel,
            Range rtRange, Range mzRange,
            PeakThresholdParameters peakThresholdParameters) {

        super(dataFile.toString(), true, true, true, true);

        this.desktop = MZmineCore.getDesktop();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        this.dataFile = dataFile;
        this.msLevel = msLevel;

        dataset = new TwoDDataSet(dataFile, msLevel, rtRange, mzRange, this);

        toolBar = new TwoDToolBar(this);
        add(toolBar, BorderLayout.EAST);

        twoDPlot = new TwoDPlot(dataFile, this, dataset, rtRange, mzRange);
        add(twoDPlot, BorderLayout.CENTER);

        bottomPanel = new TwoDBottomPanel(this, dataFile,
                peakThresholdParameters);
        add(bottomPanel, BorderLayout.SOUTH);

        updateTitle();

        pack();

        // After we have constructed everything, load the peak lists into the
        // bottom panel
        bottomPanel.rebuildPeakListSelector();
        bottomPanel.buildPeakThresholdSelector();
    }

    void updateTitle() {

        StringBuffer title = new StringBuffer();
        title.append("[");
        title.append(dataFile.toString());
        title.append("]: 2D view");

        setTitle(title.toString());

        title.append(", MS");
        title.append(msLevel);

        twoDPlot.setTitle(title.toString());

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        if (command.equals("SWITCH_PALETTE")) {
            twoDPlot.getXYPlot().switchPalette();
        }

        if (command.equals("SHOW_DATA_POINTS")) {
            twoDPlot.switchDataPointsVisible();
        }

        if (command.equals("PEAKLIST_CHANGE")
                || command.equals("PEAKS_VIEW_THRESHOLD")) {

            setPeakThreshold();
        }

        if (command.equals("SETUP_AXES")) {
            AxesSetupDialog dialog = new AxesSetupDialog(twoDPlot.getXYPlot());
            dialog.setVisible(true);
        }

        if (command.equals("SWITCH_PLOTMODE")) {

            if (twoDPlot.getPlotMode() == PlotMode.CENTROID) {
                toolBar.setCentroidButton(true);
                twoDPlot.setPlotMode(PlotMode.CONTINUOUS);
            } else {
                toolBar.setCentroidButton(false);
                twoDPlot.setPlotMode(PlotMode.CENTROID);
            }
        }

        if (command.equals("PEAKS_VIEW_TEXTFIELD")) {

            PeakList selectedPeakList = bottomPanel.getPeaksInThreshold();
            if (selectedPeakList == null) {
                return;
            }

            twoDPlot.loadPeakList(selectedPeakList);
        }
    }

    private void setPeakThreshold() {
        toolBar.toggleContinuousModeButtonSetEnable(true);

        String selectedPeakThreshold = bottomPanel.getPeaksSelectedThreshold();
        if (selectedPeakThreshold == null) {
            return;
        }

        if (selectedPeakThreshold.equals(PeakThresholdMode.NO_PEAKS.getName())) {
            bottomPanel.peakTextFieldSetVisible(false);
            toolBar.toggleContinuousModeButtonSetEnable(false);
            twoDPlot.setPeaksNotVisible();
        }
        if (selectedPeakThreshold.equals(PeakThresholdMode.ALL_PEAKS.getName())) {
            bottomPanel.peakTextFieldSetVisible(false);
        }
        if (selectedPeakThreshold.equals(PeakThresholdMode.ABOVE_INTENSITY_PEAKS.getName())
                || selectedPeakThreshold.equals(PeakThresholdMode.TOP_PEAKS.getName())
                || selectedPeakThreshold.equals(PeakThresholdMode.TOP_PEAKS_AREA.getName())) {
            bottomPanel.peakTextFieldSetVisible(true);
        }

        PeakList selectedPeakList = bottomPanel.getPeaksInThreshold();
        if (selectedPeakList == null) {
            return;
        }
        twoDPlot.loadPeakList(selectedPeakList);

        bottomPanel.revalidate();
    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskFinished(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskFinished(Task task) {
        if (task.getStatus() == TaskStatus.ERROR) {
            desktop.displayErrorMessage("Error while updating 2D visualizer: "
                    + task.getErrorMessage());
        }

        if (task.getStatus() == TaskStatus.FINISHED) {
            // Add this window to desktop
            desktop.addInternalFrame(this);
        }
    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskStarted(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskStarted(Task task) {
        // ignore
    }

    TwoDPlot getPlot() {
        return twoDPlot;
    }

}
