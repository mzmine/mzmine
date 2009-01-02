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

package net.sf.mzmine.modules.normalization.standardcompound;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.logging.Logger;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskGroup;
import net.sf.mzmine.taskcontrol.TaskGroupListener;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.util.PeakListRowSorterByMZ;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

/**
 * Normalization module using selected internal standards
 */
public class StandardCompoundNormalizer implements MZmineModule, TaskListener,
        ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private StandardCompoundNormalizerParameters parameters;

    private Desktop desktop;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {

        this.desktop = MZmineCore.getDesktop();

        parameters = new StandardCompoundNormalizerParameters();

        desktop.addMenuItem(MZmineMenu.NORMALIZATION,
                "Standard compound normalizer",
                "Peak list normalization using selected internal standards",
                KeyEvent.VK_S, false, this, null);

    }

    public String toString() {
        return "Standard compound normalizer";
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
     */
    public ParameterSet getParameterSet() {
        return parameters;
    }

    public void setParameters(ParameterSet parameters) {
        this.parameters = (StandardCompoundNormalizerParameters) parameters;
    }

    /**
     * @see net.sf.mzmine.modules.batchmode.BatchStep#setupParameters(net.sf.mzmine.data.ParameterSet)
     */
    public ExitCode setupParameters(ParameterSet parameters) {
        ParameterSetupDialog dialog = new ParameterSetupDialog(
                "Please set parameter values for " + toString(),
                (SimpleParameterSet) parameters);
        dialog.setVisible(true);
        return dialog.getExitCode();
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        PeakList[] selectedPeakLists = desktop.getSelectedPeakLists();
        if (selectedPeakLists.length != 1) {
            desktop.displayErrorMessage("Please select a single peak list for normalization");
            return;
        }

        for (PeakList pl : selectedPeakLists) {
        	
        	SimpleParameter p = (SimpleParameter) parameters.getParameter("Standard compounds");
        	p.setPossibleValues(getPeakListRows(pl));
        	
            ExitCode exitCode = setupParameters(parameters);
            if (exitCode != ExitCode.OK) {
                return;
            }

            runModule(null, new PeakList[] { pl },
                    (StandardCompoundNormalizerParameters) parameters.clone(),
                    null);
        }

    }

    public void taskStarted(Task task) {
        logger.info("Running standard compound normalizer");
    }

    public void taskFinished(Task task) {

        if (task.getStatus() == Task.TaskStatus.FINISHED) {
            logger.info("Finished standard compound normalizer");
        }

        if (task.getStatus() == Task.TaskStatus.ERROR) {
            String msg = "Error while normalizing peak list: "
                    + task.getErrorMessage();
            logger.severe(msg);
            desktop.displayErrorMessage(msg);

        }

    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#runModule(net.sf.mzmine.data.RawDataFile[],
     *      net.sf.mzmine.data.PeakList[], net.sf.mzmine.data.ParameterSet,
     *      net.sf.mzmine.taskcontrol.TaskGroupListener)
     */
    public TaskGroup runModule(RawDataFile[] dataFiles,
            PeakList[] alignmentResults,
            StandardCompoundNormalizerParameters parameters,
            TaskGroupListener taskGroupListener) {

        // prepare a new group of tasks

        Task tasks[] = new StandardCompoundNormalizerTask[alignmentResults.length];
        for (int i = 0; i < alignmentResults.length; i++) {
            tasks[i] = new StandardCompoundNormalizerTask(alignmentResults[i],
                    parameters);
        }
        TaskGroup newGroup = new TaskGroup(tasks, this, taskGroupListener);

        // start the group
        newGroup.start();

        return newGroup;

    }
    
    /**
     * Return an array of peak list rows
     * 
     * @param peakList
     * @return PeakListRow[]
     */
    private static PeakListRow[] getPeakListRows(PeakList peakList){
        // Get all rows and sort them
        PeakListRow rows[] = peakList.getRows();
        Arrays.sort(rows, new PeakListRowSorterByMZ());
        return rows;
    }

}
