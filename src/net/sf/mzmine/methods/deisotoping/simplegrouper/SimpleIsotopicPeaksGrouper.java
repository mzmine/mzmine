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

package net.sf.mzmine.methods.deisotoping.simplegrouper;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;

/**
 * This class implements a simple isotopic peaks grouper method based on
 * searhing for neighbouring peaks from expected locations.
 *
 * @version 31 March 2006
 */

public class SimpleIsotopicPeaksGrouper implements Method,
        TaskListener, ListSelectionListener, ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private TaskController taskController;
    private Desktop desktop;
    private JMenuItem myMenuItem;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule(MZmineCore core) {

        this.taskController = core.getTaskController();
        this.desktop = core.getDesktop();

        desktop.addMenuSeparator(MZmineMenu.PEAKPICKING);

        myMenuItem = desktop.addMenuItem(MZmineMenu.PEAKPICKING,
                "Simple isotopic peaks grouper", this, null, KeyEvent.VK_S,
                false, false);

        desktop.addSelectionListener(this);

    }

    /**
     * This function displays a modal dialog to define method parameters
     *
     * @see net.sf.mzmine.methods.Method#askParameters()
     */
    public MethodParameters askParameters() {

		logger.finest("Showing simple isotopic peaks grouper parameter setup dialog");

        MZmineProject currentProject = MZmineProject.getCurrentProject();
        SimpleIsotopicPeaksGrouperParameters currentParameters = (SimpleIsotopicPeaksGrouperParameters) currentProject.getParameters(this);
        if (currentParameters == null)
            currentParameters = new SimpleIsotopicPeaksGrouperParameters();

        SimpleIsotopicPeaksGrouperParameterSetupDialog sdpsd = new SimpleIsotopicPeaksGrouperParameterSetupDialog(
                desktop.getMainFrame(), currentParameters);
        sdpsd.setVisible(true);

        if (sdpsd.getExitCode() == -1) {
            return null;
        }

        return sdpsd.getParameters();

    }

    /**
     * @see net.sf.mzmine.methods.Method#runMethod(net.sf.mzmine.methods.MethodParameters,
     *      net.sf.mzmine.io.RawDataFile[],
     *      net.sf.mzmine.methods.alignment.AlignmentResult[])
     */
    public void runMethod(MethodParameters parameters,
            OpenedRawDataFile[] dataFiles, AlignmentResult[] alignmentResults) {

        logger.finest("Running simple isotopic peaks grouper");

        SimpleIsotopicPeaksGrouperParameters param = (SimpleIsotopicPeaksGrouperParameters) parameters;

        for (OpenedRawDataFile dataFile : dataFiles) {
            PeakList currentPeakList = (PeakList)dataFile.getCurrentFile().getData(PeakList.class)[0];
            if (currentPeakList == null)
                continue;
            Task peaklistProcessorTask = new SimpleIsotopicPeaksGrouperTask(
                    dataFile, currentPeakList, param);
            taskController.addTask(peaklistProcessorTask, this);
        }

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        MethodParameters parameters = askParameters();
        if (parameters == null)
            return;

        OpenedRawDataFile[] dataFiles = desktop.getSelectedDataFiles();

        runMethod(parameters, dataFiles, null);

    }

    /**
     * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
     */
    public void valueChanged(ListSelectionEvent e) {

        OpenedRawDataFile[] dataFiles = desktop.getSelectedDataFiles();

        for (OpenedRawDataFile file : dataFiles) {
			if (file.getCurrentFile().hasData(PeakList.class)) {
                myMenuItem.setEnabled(true);
                return;
            }
        }
        myMenuItem.setEnabled(false);
    }

    public void taskStarted(Task task) {
        // do nothing
    }

    public void taskFinished(Task task) {

        if (task.getStatus() == Task.TaskStatus.FINISHED) {

            Object[] result = (Object[]) task.getResult();
            OpenedRawDataFile dataFile = (OpenedRawDataFile) result[0];
            PeakList peakList = (PeakList) result[1];
            MethodParameters params = (MethodParameters) result[2];

            dataFile.addHistoryEntry(dataFile.getCurrentFile().getFile(), this,
                    params);

            // Add peak list to MZmineProject
            dataFile.getCurrentFile().addData(PeakList.class, peakList);


        } else if (task.getStatus() == Task.TaskStatus.ERROR) {
            /* Task encountered an error */
            String msg = "Error while deisotoping a file: "
                    + task.getErrorMessage();
            logger.severe(msg);
            desktop.displayErrorMessage(msg);

        }

    }

    /**
     * @see net.sf.mzmine.methods.Method#toString()
     */
    public String toString() {
        return "Simple isotopic peaks grouper";
    }

}
