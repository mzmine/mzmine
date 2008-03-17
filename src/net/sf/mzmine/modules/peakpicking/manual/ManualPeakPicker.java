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

package net.sf.mzmine.modules.peakpicking.manual;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.batchmode.BatchStepPeakPicking;
import net.sf.mzmine.modules.peakpicking.accuratemass.AccurateMassPickerParameters;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskGroup;
import net.sf.mzmine.taskcontrol.TaskGroupListener;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

public class ManualPeakPicker implements BatchStepPeakPicking, TaskListener,
        ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private AccurateMassPickerParameters parameters;

    private Desktop desktop;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {

        this.desktop = MZmineCore.getDesktop();

        parameters = new AccurateMassPickerParameters();

        desktop.addMenuItem(MZmineMenu.PEAKPICKING,
                "Accurate mass peak detector", this, null, KeyEvent.VK_A,
                false, true);

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
        ManualPickerTask rtTask = (ManualPickerTask) task;
        /*logger.info("Running accurate mass peak picker on "
                + rtTask.getDataFile());*/

    }

    public void taskFinished(Task task) {

        /*
        ManualPickerTask rtTask = (ManualPickerTask) task;

        if (task.getStatus() == Task.TaskStatus.FINISHED) {
            logger.info("Finished accurate mass peak picker on "
                    + rtTask.getDataFile());
        }

        if (task.getStatus() == Task.TaskStatus.ERROR) {
            String msg = "Error while running accurate mass peak picker on file "
                    + rtTask.getDataFile() + ": " + task.getErrorMessage();
            logger.severe(msg);
            desktop.displayErrorMessage(msg);
        }
        */

    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#toString()
     */
    public String toString() {
        return "Accurate mass peak detector";
    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#setupParameters(net.sf.mzmine.data.ParameterSet)
     */
    public ExitCode setupParameters(ParameterSet currentParameters) {
        ParameterSetupDialog dialog = new ParameterSetupDialog(
                "Please set parameter values for " + toString(),
                (SimpleParameterSet) currentParameters);
        dialog.setVisible(true);
        return dialog.getExitCode();
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
     */
    public ParameterSet getParameterSet() {
        return parameters;
    }

    public void setParameters(ParameterSet parameters) {
        this.parameters = (AccurateMassPickerParameters) parameters;
    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#runModule(net.sf.mzmine.io.RawDataFile[],
     *      net.sf.mzmine.data.AlignmentResult[],
     *      net.sf.mzmine.data.ParameterSet,
     *      net.sf.mzmine.taskcontrol.TaskGroupListener)
     */
    public TaskGroup runModule(RawDataFile[] dataFiles,
            PeakList[] alignmentResults, ParameterSet parameters,
            TaskGroupListener taskGroupListener) {

        // check data files
        if ((dataFiles == null) || (dataFiles.length == 0)) {
            desktop.displayErrorMessage("Please select data files for peak picking");
            return null;
        }

        // prepare a new group of tasks
/*        Task tasks[] = new AccurateMassPickerTask[dataFiles.length];
        for (int i = 0; i < dataFiles.length; i++) {
            tasks[i] = new AccurateMassPickerTask(dataFiles[i],
                    (AccurateMassPickerParameters) parameters);
        }
        TaskGroup newGroup = new TaskGroup(tasks, this, taskGroupListener);

        // start the group
        newGroup.start();
*/
        return null;

    }

    /*
    float minRT, maxRT, minMZ, maxMZ;
    if (clickedPeak != null) {
        minRT = clickedPeak.getRawDataPointMinRT();
        maxRT = clickedPeak.getRawDataPointMaxRT();
        minMZ = clickedPeak.getRawDataPointMinMZ();
        maxMZ = clickedPeak.getRawDataPointMaxMZ();
    } else {
        minRT = clickedPeakListRow.getAverageRT();
        maxRT = clickedPeakListRow.getAverageRT();
        minMZ = clickedPeakListRow.getAverageMZ();
        maxMZ = clickedPeakListRow.getAverageMZ();

        for (Peak peak : clickedPeakListRow.getPeaks()) {
            if (peak == null)
                continue;
            if (peak.getRawDataPointMinRT() < minRT)
                minRT = peak.getRawDataPointMinRT();
            if (peak.getRawDataPointMaxRT() > maxRT)
                maxRT = peak.getRawDataPointMaxRT();
            if (peak.getRawDataPointMinMZ() < minMZ)
                minMZ = peak.getRawDataPointMinMZ();
            if (peak.getRawDataPointMaxMZ() > maxMZ)
                maxMZ = peak.getRawDataPointMaxMZ();
        }
    }

    NumberFormatter mzFormat = MZmineCore.getDesktop().getMZFormat();
    NumberFormatter rtFormat = MZmineCore.getDesktop().getRTFormat();

    Parameter minRTparam = new SimpleParameter(ParameterType.FLOAT,
            "Retention time min", "Retention time min", "s", minRT,
            clickedDataFile.getDataMinRT(1),
            clickedDataFile.getDataMaxRT(1), rtFormat);
    Parameter maxRTparam = new SimpleParameter(ParameterType.FLOAT,
            "Retention time max", "Retention time max", "s", maxRT,
            clickedDataFile.getDataMinRT(1),
            clickedDataFile.getDataMaxRT(1), rtFormat);
    Parameter minMZparam = new SimpleParameter(ParameterType.FLOAT,
            "m/z min", "m/z min", "Da", minMZ,
            clickedDataFile.getDataMinMZ(1),
            clickedDataFile.getDataMaxMZ(1), mzFormat);
    Parameter maxMZparam = new SimpleParameter(ParameterType.FLOAT,
            "m/z max", "m/z max", "Da", maxMZ,
            clickedDataFile.getDataMinMZ(1),
            clickedDataFile.getDataMaxMZ(1), mzFormat);
    Parameter[] params = { minRTparam, maxRTparam, minMZparam,
            maxMZparam };

    SimpleParameterSet parameterSet = new SimpleParameterSet(params);

    ParameterSetupDialog parameterSetupDialog = new ParameterSetupDialog(
            "Please set peak boundaries", parameterSet);

    parameterSetupDialog.setVisible(true);

    if (parameterSetupDialog.getExitCode() != ExitCode.OK)
        return;

    minRT = (Float) parameterSet.getParameterValue(minRTparam);
    maxRT = (Float) parameterSet.getParameterValue(maxRTparam);
    minMZ = (Float) parameterSet.getParameterValue(minMZparam);
    maxMZ = (Float) parameterSet.getParameterValue(maxMZparam);

    ManuallyDefinePeakTask task = new ManuallyDefinePeakTask(
            clickedPeakListRow, clickedDataFile, minRT, maxRT, minMZ,
            maxMZ);

    MZmineCore.getTaskController().addTask(task);
    */
}
