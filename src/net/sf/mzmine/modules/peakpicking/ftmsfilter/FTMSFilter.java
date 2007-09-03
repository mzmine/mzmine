/*
 * Copyright 2006-2007 The MZmine Development Team
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

package net.sf.mzmine.modules.peakpicking.ftmsfilter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.util.logging.Logger;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.batchmode.BatchStepPeakPicking;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskGroup;
import net.sf.mzmine.taskcontrol.TaskGroupListener;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.ExitCode;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;

/**
 * FTMS shoulder peaks filter
 * 
 * Fourier-transform mass spectrometers produce continuous spectra with side
 * peaks (called shoulder peaks) on both sides of each m/z peak. Intensity of
 * these peaks is usually less than 5% of the main (real) peak. This filter can
 * remove such interference peaks from the peak list.
 * 
 */

public class FTMSFilter implements BatchStepPeakPicking, TaskListener,
        ActionListener {

    public static final Parameter mzDifferenceMin = new SimpleParameter(
            ParameterType.FLOAT, "M/Z difference minimum",
            "Minimum m/z difference between real peak and shoulder peak", "Da",
            new Float(0.001), new Float(0.0), null);

    public static final Parameter mzDifferenceMax = new SimpleParameter(
            ParameterType.FLOAT, "M/Z difference maximum",
            "Maximum m/z difference between real peak and shoulder peak", "Da",
            new Float(0.005), new Float(0.0), null);

    public static final Parameter rtDifferenceMax = new SimpleParameter(
            ParameterType.FLOAT,
            "RT difference maximum",
            "Maximum retention time difference between real peak and shoulder peak",
            "seconds", new Float(5.0), new Float(0.0), null);

    public static final Parameter heightMax = new SimpleParameter(
            ParameterType.FLOAT, "Maximum height",
            "Maximum height of shoulder peak", "%", new Float(0.05), new Float(
                    0.0), null, NumberFormat.getPercentInstance());

    private ParameterSet parameters;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private Desktop desktop;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {

        this.desktop = MZmineCore.getDesktop();

        parameters = new SimpleParameterSet(new Parameter[] { mzDifferenceMin,
                mzDifferenceMax, rtDifferenceMax, heightMax });

        desktop.addMenuItem(MZmineMenu.PEAKPICKING,
                "FTMS shoulder peak filter", this, null, KeyEvent.VK_F, false,
                true);

    }

    public void setParameters(ParameterSet parameters) {
        this.parameters = parameters;
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        RawDataFile[] dataFiles = desktop.getSelectedDataFiles();
        MZmineProject currentProject = MZmineCore.getCurrentProject();

        if (dataFiles.length == 0) {
            desktop.displayErrorMessage("Please select data file");
            return;
        }

        for (RawDataFile dataFile : dataFiles) {
            if (currentProject.getFilePeakList(dataFile) == null) {
                desktop.displayErrorMessage(dataFile
                        + " has no peak list. Please run peak picking first.");
                return;
            }
        }

        ExitCode exitCode = setupParameters(parameters);
        if (exitCode != ExitCode.OK)
            return;

        runModule(dataFiles, null, parameters.clone(), null);
    }

    public void taskStarted(Task task) {
        logger.info("Running FTMS shoulder peak filter on "
                + ((FTMSFilterTask) task).getDataFile());
    }

    public void taskFinished(Task task) {

        if (task.getStatus() == Task.TaskStatus.FINISHED) {

            logger.info("Finished FTMS shoulder peak filter on "
                    + ((FTMSFilterTask) task).getDataFile());

        } else if (task.getStatus() == Task.TaskStatus.ERROR) {
            /* Task encountered an error */
            String msg = "Error while filtering file: "
                    + task.getErrorMessage();
            logger.severe(msg);
            desktop.displayErrorMessage(msg);

        }

    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#toString()
     */
    public String toString() {
        return "FTMS shoulder peak filter";
    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#setupParameters(net.sf.mzmine.data.ParameterSet)
     */
    public ExitCode setupParameters(ParameterSet currentParameters) {
        ParameterSetupDialog dialog = new ParameterSetupDialog(
                desktop.getMainFrame(), "Please check parameter values for "
                        + toString(), (SimpleParameterSet) currentParameters);
        dialog.setVisible(true);
        return dialog.getExitCode();
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
     */
    public ParameterSet getParameterSet() {
        return parameters;
    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#runModule(net.sf.mzmine.io.RawDataFile[],
     *      net.sf.mzmine.data.PeakList[], net.sf.mzmine.data.ParameterSet,
     *      net.sf.mzmine.taskcontrol.TaskGroupListener)
     */
    public TaskGroup runModule(RawDataFile[] dataFiles,
            PeakList[] alignmentResults, ParameterSet parameters,
            TaskGroupListener methodListener) {

        MZmineProject currentProject = MZmineCore.getCurrentProject();

        // prepare a new sequence of tasks
        Task tasks[] = new FTMSFilterTask[dataFiles.length];
        for (int i = 0; i < dataFiles.length; i++) {

            if (currentProject.getFilePeakList(dataFiles[i]) == null) {
                String msg = "Cannot start filtering of " + dataFiles[i]
                        + ", please run peak picking first.";
                logger.severe(msg);
                desktop.displayErrorMessage(msg);
                return null;
            }
            tasks[i] = new FTMSFilterTask(dataFiles[i],
                    (SimpleParameterSet) parameters);
        }

        TaskGroup newSequence = new TaskGroup(tasks, this, methodListener);

        // execute the sequence
        newSequence.run();

        return newSequence;

    }

}
