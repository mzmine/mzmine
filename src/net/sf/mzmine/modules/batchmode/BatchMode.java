/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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
import java.util.logging.Logger;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.taskcontrol.TaskPriority;
import net.sf.mzmine.util.dialogs.ExitCode;

/**
 * Batch mode module
 */
public class BatchMode implements MZmineModule, 
        ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private Desktop desktop;

    private BatchQueue currentBatchConfiguration;

    private RawDataFile selectedDataFiles[];
    private PeakList selectedPeakLists[];

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {

        this.desktop = MZmineCore.getDesktop();

        currentBatchConfiguration = new BatchQueue();

        desktop.addMenuItem(MZmineMenu.PROJECT, "Run batch...",
                "Configure and run a batch of tasks", KeyEvent.VK_B, true,
                this, null);

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        logger.finest("Showing batch mode setup dialog");

        BatchModeDialog setupDialog = new BatchModeDialog(
				currentBatchConfiguration);
        setupDialog.setVisible(true);

        if (setupDialog.getExitCode() == ExitCode.OK) {
            
        	BatchQueue newBatchRun = currentBatchConfiguration.clone();

            selectedDataFiles = desktop.getSelectedDataFiles();
            selectedPeakLists = desktop.getSelectedPeakLists();

            BatchTask newTask = new BatchTask(newBatchRun, selectedDataFiles,
					selectedPeakLists);

			MZmineCore.getTaskController().addTask(newTask, TaskPriority.HIGH);

        }

    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#toString()
     */
    public String toString() {
        return "Batch mode";
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
     */
    public ParameterSet getParameterSet() {
        return currentBatchConfiguration;
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
     */
    public void setParameters(ParameterSet parameters) {
    	currentBatchConfiguration = (BatchQueue) parameters;
    }

}