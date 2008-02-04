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

package net.sf.mzmine.modules.visualization.twod;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JInternalFrame;

import org.jfree.chart.plot.XYPlot;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;
import net.sf.mzmine.userinterface.Desktop;

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

    public TwoDVisualizerWindow(RawDataFile dataFile, int msLevel, float rtMin,
            float rtMax, float mzMin, float mzMax) {

        super(dataFile.toString(), true, true, true, true);

        this.desktop = MZmineCore.getDesktop();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        this.dataFile = dataFile;
        this.msLevel = msLevel;

        dataset = new TwoDDataSet(dataFile, msLevel, rtMin, rtMax, mzMin,
                mzMax, this);

        toolBar = new TwoDToolBar(this);
        add(toolBar, BorderLayout.EAST);

        twoDPlot = new TwoDPlot(this, dataset);
        add(twoDPlot, BorderLayout.CENTER);

        bottomPanel = new TwoDBottomPanel(this, dataFile);
        add(bottomPanel, BorderLayout.SOUTH);

        updateTitle();

        pack();

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
            twoDPlot.getXYPlot().switchPalette();
            repaint();
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
