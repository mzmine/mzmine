/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
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
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.main.mzmineclient.MZmineModule;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.util.dialogs.ExitCode;

/**
 * Batch mode module
 */
public class BatchMode implements MZmineModule, 
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
     * @see net.sf.mzmine.main.mzmineclient.MZmineModule#initModule(net.sf.mzmine.main.mzmineclient.MZmineCore)
     */
    public void initModule() {

        this.desktop = MZmineCore.getDesktop();

        currentBatchSteps = new BatchQueue();

        desktop.addMenuItem(MZmineMenu.PROJECT, "Run batch...",
                "Configure and run a batch of tasks", KeyEvent.VK_B, true,
                this, null);

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
     * @see net.sf.mzmine.main.mzmineclient.MZmineModule#toString()
     */
    public String toString() {
        return "Batch mode";
    }


    private void runNextStep() {

        logger.finest("Batch mode runNextStep");

        BatchStepWrapper newStep = currentBatchSteps.get(currentStep);
        BatchStep method = newStep.getMethod();

        /*
		 * Task[] newSequence = method.runModule(selectedDataFiles,
		 * selectedPeakLists, newStep.getParameters(), this);
		 * 
		 * if (newSequence == null) {
		 * desktop.displayErrorMessage("Batch processing cannot continue.");
		 * batchRunning = false; }
		 */

        currentStep++;

    }

    /**
     * @see net.sf.mzmine.main.mzmineclient.MZmineModule#getParameterSet()
     */
    public ParameterSet getParameterSet() {
        return currentBatchSteps;
    }

    /**
     * @see net.sf.mzmine.main.mzmineclient.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
     */
    public void setParameters(ParameterSet parameters) {
        currentBatchSteps = (BatchQueue) parameters;
    }

}