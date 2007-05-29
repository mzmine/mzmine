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

package net.sf.mzmine.modules.normalization.linear;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.Parameter.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.DataProcessingMethod;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskGroup;
import net.sf.mzmine.taskcontrol.TaskGroupListener;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.ExitCode;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;

/**
 * 
 */
public class LinearNormalizer implements DataProcessingMethod, TaskListener,
        ActionListener {

    protected static final String NormalizationTypeAverageIntensity = "Average intensity";
    protected static final String NormalizationTypeAverageSquaredIntensity = "Average squared intensity";
    protected static final String NormalizationTypeMaximumPeakHeight = "Maximum peak intensity";
    protected static final String NormalizationTypeTotalRawSignal = "Total raw signal";

    protected static final Object[] normalizationTypePossibleValues = {
            NormalizationTypeAverageIntensity,
            NormalizationTypeAverageSquaredIntensity,
            NormalizationTypeMaximumPeakHeight, NormalizationTypeTotalRawSignal };

    protected static final Parameter normalizationType = new SimpleParameter(
            ParameterType.STRING, "Normalization type",
            "Normalize intensities by...", NormalizationTypeAverageIntensity,
            normalizationTypePossibleValues);

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private ParameterSet parameters;

    private TaskController taskController;
    private Desktop desktop;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule(MZmineCore core) {

        this.taskController = core.getTaskController();
        this.desktop = core.getDesktop();

        parameters = new SimpleParameterSet(
                new Parameter[] { normalizationType });

        desktop.addMenuItem(MZmineMenu.NORMALIZATION, "Linear normalizer",
                this, null, KeyEvent.VK_A, false, true);

    }

    public String toString() {
        return "Linear normalizer";
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
     */
    public ParameterSet getParameterSet() {
        return parameters;
    }

    public void setParameters(ParameterSet parameters) {
        this.parameters = parameters;
    }

    /**
     * @see net.sf.mzmine.modules.DataProcessingMethod#setupParameters(net.sf.mzmine.data.ParameterSet)
     */
    public ParameterSet setupParameters(ParameterSet currentParameters) {
        ParameterSetupDialog dialog = new ParameterSetupDialog(
                desktop.getMainFrame(), "Please check parameter values for "
                        + toString(), (SimpleParameterSet) currentParameters);
        dialog.setVisible(true);
        if (dialog.getExitCode() == ExitCode.CANCEL)
            return null;
        return currentParameters.clone();
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        PeakList[] selectedPeakLists = desktop.getSelectedAlignmentResults();
        if (selectedPeakLists.length < 1) {
            desktop.displayErrorMessage("Please select alignment result");
            return;
        }

        ParameterSet param = setupParameters(parameters);
        if (param == null)
            return;
        runMethod(null, selectedPeakLists, param, null);

    }

    public void taskStarted(Task task) {
        logger.info("Running linear normalizer");
    }

    public void taskFinished(Task task) {

        if (task.getStatus() == Task.TaskStatus.FINISHED) {

            logger.info("Finished linear normalizer");

            PeakList normalizedPeakList = (PeakList) task.getResult();

            MZmineProject.getCurrentProject().addAlignmentResult(
                    normalizedPeakList);

        } else if (task.getStatus() == Task.TaskStatus.ERROR) {
            /* Task encountered an error */
            String msg = "Error while normalizing alignment result(s): "
                    + task.getErrorMessage();
            logger.severe(msg);
            desktop.displayErrorMessage(msg);

        }

    }

    /**
     * @see net.sf.mzmine.modules.DataProcessingMethod#runMethod(net.sf.mzmine.io.OpenedRawDataFile[],
     *      net.sf.mzmine.data.PeakList[],
     *      net.sf.mzmine.data.ParameterSet,
     *      net.sf.mzmine.taskcontrol.TaskGroupListener)
     */
    public TaskGroup runMethod(OpenedRawDataFile[] dataFiles,
            PeakList[] alignmentResults, ParameterSet parameters,
            TaskGroupListener methodListener) {

        // prepare a new sequence of tasks
        Task tasks[] = new LinearNormalizerTask[alignmentResults.length];
        for (int i = 0; i < alignmentResults.length; i++) {
            tasks[i] = new LinearNormalizerTask(alignmentResults[i], parameters);
        }
        TaskGroup newSequence = new TaskGroup(tasks, this, methodListener,
                taskController);

        // execute the sequence
        newSequence.run();

        return newSequence;

    }

}
