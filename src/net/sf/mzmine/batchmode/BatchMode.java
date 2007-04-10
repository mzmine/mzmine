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

package net.sf.mzmine.batchmode;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.batchmode.BatchModeDialog.ExitCode;
import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.TaskSequence;
import net.sf.mzmine.taskcontrol.TaskSequenceListener;
import net.sf.mzmine.taskcontrol.TaskSequence.TaskSequenceStatus;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;

/**
 * Batch mode module
 */
public class BatchMode implements MZmineModule, ListSelectionListener,
        TaskSequenceListener, ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private MZmineCore core;
    private Desktop desktop;

    private JMenuItem batchMenuItem;

    private Vector<BatchStep> currentBatchSteps;
    private boolean batchRunning = false;
    private int currentStep;
    private OpenedRawDataFile[] selectedDataFiles;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule(MZmineCore core) {

        this.core = core;
        this.desktop = core.getDesktop();

        currentBatchSteps = new Vector<BatchStep>();

        batchMenuItem = desktop.addMenuItem(MZmineMenu.BATCH,
                "Define batch operations", this, null, KeyEvent.VK_D, false,
                false);
        desktop.addSelectionListener(this);

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

        logger.finest("Showing parameter setup dialog");

        BatchModeDialog setupDialog = new BatchModeDialog(desktop.getMainFrame(), core,
                currentBatchSteps);
        setupDialog.setVisible(true);

        if (setupDialog.getExitCode() == ExitCode.OK) {
            batchRunning = true;
            currentStep = 0;
            runNextStep();
        }

    }

    /**
     * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
     */
    public void valueChanged(ListSelectionEvent e) {
        batchMenuItem.setEnabled(desktop.isDataFileSelected());
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#toString()
     */
    public String toString() {
        return "Batch mode";
    }

    public void taskSequenceStarted(TaskSequence sequence) {
        logger.finest("Batch mode received task sequence started call");
    }
    
    public void taskSequenceFinished(TaskSequence sequence) {

        logger.finest("Batch mode received task sequence finished call");

        if ((sequence.getStatus() == TaskSequenceStatus.ERROR)
                || (sequence.getStatus() == TaskSequenceStatus.CANCELED)) {
            desktop.displayErrorMessage("Batch processing canceled.");
            batchRunning = false;
            return;
        }

        if (sequence.getStatus() == TaskSequenceStatus.FINISHED) {
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

        BatchStep newStep = currentBatchSteps.get(currentStep);
        Method method = newStep.getMethod();

        AlignmentResult[] lastResultOnly = null;
        AlignmentResult[] allResults = MZmineProject.getCurrentProject().getAlignmentResults();
        if (allResults.length > 0)
            lastResultOnly = new AlignmentResult[] { allResults[allResults.length - 1] };

        TaskSequence newSequence = method.runMethod(selectedDataFiles, lastResultOnly,
                newStep.getParameters(), this);
        
        if (newSequence == null)  {
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