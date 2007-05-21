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

package net.sf.mzmine.modules.visualization.rawdata.twod;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JInternalFrame;

import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.modules.RawDataVisualizer;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.util.CursorPosition;

/**
 * 2D visualizer using JFreeChart library
 */
public class TwoDVisualizerWindow extends JInternalFrame implements
        RawDataVisualizer, ActionListener, TaskListener {

    private TwoDToolBar toolBar;
    private TwoDPlot twoDPlot;
    private JCheckBox resampleCheckBox;

    private TwoDDataSet dataset;

    private OpenedRawDataFile dataFile;
    private int msLevel;

    private Desktop desktop;

    public TwoDVisualizerWindow(TaskController taskController, Desktop desktop,
            OpenedRawDataFile dataFile, int msLevel, double rtMin,
            double rtMax, double mzMin, double mzMax, int rtResolution,
            int mzResolution) {

        super(dataFile.toString(), true, true, true, true);

        this.desktop = desktop;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        this.dataFile = dataFile;
        this.msLevel = msLevel;

        dataset = new TwoDDataSet(taskController, dataFile.getCurrentFile(),
                msLevel, rtMin, rtMax, mzMin, mzMax, rtResolution,
                mzResolution, this);

        toolBar = new TwoDToolBar(this);
        add(toolBar, BorderLayout.EAST);

        twoDPlot = new TwoDPlot(this, dataset);
        add(twoDPlot, BorderLayout.CENTER);
        // make sure the dataset knows about zooming events
        twoDPlot.getXYPlot().addChangeListener(dataset);

        resampleCheckBox = new JCheckBox("Resample when zooming", true);
        resampleCheckBox.setBackground(Color.white);
        resampleCheckBox.setFont(resampleCheckBox.getFont().deriveFont(10f));
        resampleCheckBox.setHorizontalAlignment(JCheckBox.CENTER);
        resampleCheckBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(resampleCheckBox, BorderLayout.SOUTH);

        updateTitle();

        pack();

    }

    /**
     * @see net.sf.mzmine.modules.RawDataVisualizer#setMZRange(double,
     *      double)
     */
    public void setMZRange(double mzMin, double mzMax) {
        twoDPlot.getXYPlot().getRangeAxis().setRange(mzMin, mzMax);
    }

    /**
     * @see net.sf.mzmine.modules.RawDataVisualizer#setRTRange(double,
     *      double)
     */
    public void setRTRange(double rtMin, double rtMax) {
        twoDPlot.getXYPlot().getDomainAxis().setRange(rtMin, rtMax);

    }

    /**
     * @see net.sf.mzmine.modules.RawDataVisualizer#setIntensityRange(double,
     *      double)
     */
    public void setIntensityRange(double intensityMin, double intensityMax) {
        // do nothing
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

        if (command.equals("SHOW_DATA_POINTS")) {
            // TODO
        }

    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskFinished(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskFinished(Task task) {
        if (task.getStatus() == TaskStatus.ERROR) {
            desktop.displayErrorMessage("Error while updating 2D visualizer: "
                    + task.getErrorMessage());
        }
        
        dataset.setDataLoaded();

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
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see net.sf.mzmine.modules.RawDataVisualizer#setCursorPosition(net.sf.mzmine.util.CursorPosition)
     */
    public void setCursorPosition(CursorPosition newPosition) {
        // TODO Auto-generated method stub

    }
    
    TwoDPlot getPlot() {
        return twoDPlot;
    }

}
