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

package net.sf.mzmine.modules.batchmode;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Logger;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.TaskGroup;
import net.sf.mzmine.taskcontrol.TaskGroupListener;
import net.sf.mzmine.taskcontrol.TaskGroup.TaskGroupStatus;
import net.sf.mzmine.util.dialogs.ExitCode;

/**
 * Batch mode module
 */
public class BatchMode implements MZmineModule, TaskGroupListener,
        ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private Desktop desktop;

    private BatchQueue currentBatchSteps;
    private boolean batchRunning = false;
    private int currentStep;

    private RawDataFile selectedDataFiles[];
    private PeakList selectedPeakLists[];

    private Vector<RawDataFile> previousProjectDataFiles;
    private Vector<PeakList> previousProjectPeakLists;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {

        this.desktop = MZmineCore.getDesktop();

        currentBatchSteps = new BatchQueue();

        desktop.addMenuItem(MZmineMenu.PROJECT, "Run batch...",
                "Configure and run a batch of tasks", KeyEvent.VK_R, this, null);

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        if (batchRunning) {
            desktop.displayErrorMessage("Batch is already running");
            return;
        }

        logger.finest("Showing batch mode setup dialog");

        BatchModeDialog setupDialog = new BatchModeDialog(currentBatchSteps);
        setupDialog.setVisible(true);

        if (setupDialog.getExitCode() == ExitCode.OK) {
            batchRunning = true;
            currentStep = 0;

            selectedDataFiles = desktop.getSelectedDataFiles();
            selectedPeakLists = desktop.getSelectedPeakLists();

            MZmineProject project = MZmineCore.getCurrentProject();
            previousProjectDataFiles = new Vector<RawDataFile>(
                    Arrays.asList(project.getDataFiles()));
            previousProjectPeakLists = new Vector<PeakList>(
                    Arrays.asList(project.getPeakLists()));

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
            if (currentStep < currentBatchSteps.size()) {

                // Get current project state
                MZmineProject project = MZmineCore.getCurrentProject();
                Vector<RawDataFile> currentProjectDataFiles = new Vector<RawDataFile>(
                        Arrays.asList(project.getDataFiles()));
                Vector<PeakList> currentProjectPeakLists = new Vector<PeakList>(
                        Arrays.asList(project.getPeakLists()));

                // Check if we have any newly added files or peaklists
                Vector<RawDataFile> newProjectDataFiles = new Vector<RawDataFile>(
                        currentProjectDataFiles);
                newProjectDataFiles.removeAll(previousProjectDataFiles);
                if (!newProjectDataFiles.isEmpty())
                    selectedDataFiles = newProjectDataFiles.toArray(new RawDataFile[0]);
                Vector<PeakList> newProjectPeakLists = new Vector<PeakList>(
                        currentProjectPeakLists);
                newProjectPeakLists.removeAll(previousProjectPeakLists);
                if (!newProjectPeakLists.isEmpty())
                    selectedPeakLists = newProjectPeakLists.toArray(new PeakList[0]);

                // Memorize current state of the project for batch processing
                previousProjectDataFiles = currentProjectDataFiles;
                previousProjectPeakLists = currentProjectPeakLists;

                runNextStep();

            } else {
                desktop.displayMessage("Batch processing done.");
                batchRunning = false;
            }
        }
    }

    private void runNextStep() {

        logger.finest("Batch mode runNextStep");

        BatchStepWrapper newStep = currentBatchSteps.get(currentStep);
        BatchStep method = newStep.getMethod();

        TaskGroup newSequence = method.runModule(selectedDataFiles,
                selectedPeakLists, newStep.getParameters(), this);

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
        return currentBatchSteps;
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
     */
    public void setParameters(ParameterSet parameters) {
        currentBatchSteps = (BatchQueue) parameters;
    }

}