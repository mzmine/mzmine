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

package net.sf.mzmine.modules.peakpicking.recursivethreshold;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.util.logging.Logger;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.Parameter.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.BatchStep;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskGroup;
import net.sf.mzmine.taskcontrol.TaskGroupListener;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.ExitCode;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;

public class RecursivePicker implements BatchStep, TaskListener, ActionListener {

    public static final NumberFormat percentFormat = NumberFormat.getPercentInstance();

    public static final Parameter binSize = new SimpleParameter(
            ParameterType.FLOAT, "M/Z bin width",
            "Width of M/Z range for each precalculated XIC", "Da", new Float(
                    0.25), new Float(0.05), null);

    public static final Parameter chromatographicThresholdLevel = new SimpleParameter(
            ParameterType.FLOAT, "Chromatographic threshold level",
            "Used in defining threshold level value from an XIC", "%",
            new Float(0.0), new Float(0.0), new Float(1.0), percentFormat);

    public static final Parameter noiseLevel = new SimpleParameter(
            ParameterType.FLOAT, "Noise level",
            "Intensities less than this value are interpreted as noise",
            "absolute", new Float(10.0), new Float(0.0), null);

    public static final Parameter minimumPeakHeight = new SimpleParameter(
            ParameterType.FLOAT, "Min peak height",
            "Minimum acceptable peak height", "absolute", new Float(100.0),
            new Float(0.0), null);

    public static final Parameter minimumPeakDuration = new SimpleParameter(
            ParameterType.FLOAT, "Min peak duration",
            "Minimum acceptable peak duration", "seconds", new Float(4.0),
            new Float(0.0), null);

    public static final Parameter minimumMZPeakWidth = new SimpleParameter(
            ParameterType.FLOAT, "Min M/Z peak width",
            "Minimum acceptable peak width in M/Z", "Da", new Float(0.2),
            new Float(0.0), null);

    public static final Parameter maximumMZPeakWidth = new SimpleParameter(
            ParameterType.FLOAT, "Max M/Z peak width",
            "Maximum acceptable peak width in M/Z", "Da", new Float(1.00),
            new Float(0.0), null);

    public static final Parameter mzTolerance = new SimpleParameter(
            ParameterType.FLOAT,
            "M/Z tolerance",
            "Maximum allowed distance in M/Z between centroid peaks in successive scans",
            "Da", new Float(0.1), new Float(0.0), null);

    public static final Parameter intTolerance = new SimpleParameter(
            ParameterType.FLOAT,
            "Intensity tolerance",
            "Maximum allowed deviation from expected /\\ shape of a peak in chromatographic direction",
            "%", new Float(0.15), new Float(0.0), null, percentFormat);

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private ParameterSet parameters;

    private Desktop desktop;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {

        this.desktop = MZmineCore.getDesktop();

        parameters = new SimpleParameterSet(new Parameter[] { binSize,
                chromatographicThresholdLevel, noiseLevel, minimumPeakHeight,
                minimumPeakDuration, minimumMZPeakWidth, maximumMZPeakWidth,
                mzTolerance, intTolerance });

        desktop.addMenuItem(MZmineMenu.PEAKPICKING,
                "Recursive threshold peak detector", this, null, KeyEvent.VK_R,
                false, true);

    }

    public void setParameters(ParameterSet parameters) {
        this.parameters = parameters;
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        RawDataFile[] dataFiles = desktop.getSelectedDataFiles();
        if (dataFiles.length == 0) {
            desktop.displayErrorMessage("Please select at least one data file");
            return;
        }

        ExitCode exitCode = setupParameters(parameters);
        if (exitCode != ExitCode.OK)
            return;

        runModule(dataFiles, null, parameters.clone(), null);

    }

    public void taskStarted(Task task) {
        logger.info("Running recursive threshold peak picker on "
                + ((RecursivePickerTask) task).getDataFile());

    }

    public void taskFinished(Task task) {

        if (task.getStatus() == Task.TaskStatus.FINISHED) {

            logger.info("Finished recursive threshold peak picker on "
                    + ((RecursivePickerTask) task).getDataFile());

            Object[] result = (Object[]) task.getResult();
            RawDataFile dataFile = (RawDataFile) result[0];
            PeakList peakList = (PeakList) result[1];

            MZmineProject currentProject = MZmineCore.getCurrentProject();

            // Add peak list as data unit to current file
            currentProject.setFilePeakList(dataFile, peakList);

            // Notify listeners
            desktop.notifySelectionListeners();

        } else if (task.getStatus() == Task.TaskStatus.ERROR) {
            /* Task encountered an error */
            String msg = "Error while peak picking a file: "
                    + task.getErrorMessage();
            logger.severe(msg);
            desktop.displayErrorMessage(msg);

        }

    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#toString()
     */
    public String toString() {
        return "Recursive threshold peak detector";
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#setCurrentParameters(net.sf.mzmine.data.ParameterSet)
     */
    public void setCurrentParameters(ParameterSet parameters) {
        this.parameters = parameters;
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
     *      net.sf.mzmine.data.AlignmentResult[],
     *      net.sf.mzmine.data.ParameterSet,
     *      net.sf.mzmine.taskcontrol.TaskGroupListener)
     */
    public TaskGroup runModule(RawDataFile[] dataFiles,
            PeakList[] alignmentResults, ParameterSet parameters,
            TaskGroupListener methodListener) {

        // prepare a new sequence of tasks
        Task tasks[] = new RecursivePickerTask[dataFiles.length];
        for (int i = 0; i < dataFiles.length; i++) {
            tasks[i] = new RecursivePickerTask(dataFiles[i],
                    (SimpleParameterSet) parameters);
        }
        TaskGroup newSequence = new TaskGroup(tasks, this, methodListener);

        // execute the sequence
        newSequence.run();

        return newSequence;

    }

}
