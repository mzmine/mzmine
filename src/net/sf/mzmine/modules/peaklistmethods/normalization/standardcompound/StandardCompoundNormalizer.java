/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.normalization.standardcompound;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Arrays;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

/**
 * Normalization module using selected internal standards
 */
public class StandardCompoundNormalizer implements MZmineModule, 
        ActionListener {

	final String helpID = GUIUtils.generateHelpID(this);
	
	public static final String MODULE_NAME = "Standard compound normalizer";
	
    private StandardCompoundNormalizerParameters parameters;

    private Desktop desktop;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {

        this.desktop = MZmineCore.getDesktop();

        parameters = new StandardCompoundNormalizerParameters();

        desktop.addMenuItem(MZmineMenu.NORMALIZATION,
        		MODULE_NAME,
                "Peak list normalization using selected internal standards",
                KeyEvent.VK_S, false, this, null);

    }

    public String toString() {
        return MODULE_NAME;
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
                (SimpleParameterSet) parameters, helpID);
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

            PeakListRow rows[] = pl.getRows();
            Arrays.sort(rows, new PeakListRowSorter(SortingProperty.MZ,
					SortingDirection.Ascending));
            parameters.setMultipleSelection(
                    StandardCompoundNormalizerParameters.standardCompounds,
                    rows);

            ExitCode exitCode = setupParameters(parameters);
            if (exitCode != ExitCode.OK) {
                return;
            }

            runModule(null, new PeakList[] { pl },
                    (StandardCompoundNormalizerParameters) parameters.clone());
        }

    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#runModule(net.sf.mzmine.data.RawDataFile[],
     *      net.sf.mzmine.data.PeakList[], net.sf.mzmine.data.ParameterSet,
     *      net.sf.mzmine.taskcontrol.Task[]Listener)
     */
    public Task[] runModule(RawDataFile[] dataFiles,
            PeakList[] alignmentResults,
            StandardCompoundNormalizerParameters parameters) {

        // prepare a new group of tasks

        Task tasks[] = new StandardCompoundNormalizerTask[alignmentResults.length];
        for (int i = 0; i < alignmentResults.length; i++) {
            tasks[i] = new StandardCompoundNormalizerTask(alignmentResults[i],
                    parameters);
        }

        MZmineCore.getTaskController().addTasks(tasks);

        return tasks;

    }

}
