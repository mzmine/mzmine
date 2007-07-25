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

package net.sf.mzmine.modules.batchmode;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Vector;
import java.util.logging.Logger;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.modules.BatchStep;
import net.sf.mzmine.taskcontrol.TaskGroup;
import net.sf.mzmine.taskcontrol.TaskGroupListener;
import net.sf.mzmine.taskcontrol.TaskGroup.TaskGroupStatus;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.ExitCode;

/**
 * Batch mode module
 */
public class BatchMode implements MZmineModule, TaskGroupListener,
        ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private Desktop desktop;

    private Vector<BatchStepWrapper> currentBatchSteps;
    private boolean batchRunning = false;
    private int currentStep;
    private RawDataFile[] selectedDataFiles;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {

        this.desktop = MZmineCore.getDesktop();

        currentBatchSteps = new Vector<BatchStepWrapper>();

        desktop.addMenuItem(MZmineMenu.PROJECT, "Run batch...", this, null,
                KeyEvent.VK_R, false, true);

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        if (batchRunning) {
            desktop.displayErrorMessage("Batch is already running");
            return;
        }

        selectedDataFiles = desktop.getSelectedDataFiles();
        if (selectedDataFiles.length == 0) {
            desktop.displayErrorMessage("Please select at least one data file");
            return;
        }

        logger.finest("Showing batch mode setup dialog");

        BatchModeDialog setupDialog = new BatchModeDialog(currentBatchSteps);
        setupDialog.setVisible(true);

        if (setupDialog.getExitCode() == ExitCode.OK) {
            batchRunning = true;
            currentStep = 0;
            runNextStep();
        }

    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#toString()
     */
    public String toString() {
        return "Batch mode";
    }

    public void taskGroupStarted(TaskGroup sequence) {
        logger.finest("Batch mode received task sequence started call");
    }

    public void taskGroupFinished(TaskGroup sequence) {

        logger.finest("Batch mode received task sequence finished call");

        if ((sequence.getStatus() == TaskGroupStatus.ERROR)
                || (sequence.getStatus() == TaskGroupStatus.CANCELED)) {
            desktop.displayErrorMessage("Batch processing canceled.");
            batchRunning = false;
            return;
        }

        if (sequence.getStatus() == TaskGroupStatus.FINISHED) {
            if (currentStep < currentBatchSteps.size())
                runNextStep();
            else {
                desktop.displayMessage("Batch processing done.");
                batchRunning = false;
            }
        }
    }

    private void runNextStep() {

        logger.finest("Batch mode runNextStep");

        BatchStepWrapper newStep = currentBatchSteps.get(currentStep);
        BatchStep method = newStep.getMethod();

        PeakList[] lastResultOnly = null;
        PeakList[] allResults = MZmineCore.getCurrentProject().getAlignedPeakLists();
        if (allResults.length > 0)
            lastResultOnly = new PeakList[] { allResults[allResults.length - 1] };

        TaskGroup newSequence = method.runModule(selectedDataFiles,
                lastResultOnly, newStep.getParameters(), this);

        if (newSequence == null) {
            desktop.displayErrorMessage("Batch processing cannot continue.");
            batchRunning = false;
        }

        currentStep++;

    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
     */
    public ParameterSet getParameterSet() {
        return null;
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
     */
    public void setParameters(ParameterSet parameterValues) {
    }

}