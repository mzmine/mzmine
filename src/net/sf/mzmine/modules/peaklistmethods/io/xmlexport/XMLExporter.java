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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.io.xmlexport;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.modules.batchmode.BatchStep;
import net.sf.mzmine.modules.batchmode.BatchStepCategory;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.dialogs.ExitCode;

public class XMLExporter implements MZmineModule, ActionListener, BatchStep {

	final String helpID = GUIUtils.generateHelpID(this);
	
	public static final String MODULE_NAME = "Export to XML file";

    private XMLExporterParameters parameters;
    private Desktop desktop;


	public ParameterSet getParameterSet() {
        return parameters;
	}

	public XMLExporter() {
		this.desktop = MZmineCore.getDesktop();

        parameters = new XMLExporterParameters();

        desktop.addMenuItem(MZmineMenu.PEAKLISTEXPORT, MODULE_NAME,
                "Save a peak list to XML file", KeyEvent.VK_X, true,
                this, null);
	}

	public void setParameters(ParameterSet parameterValues) {
        this.parameters = (XMLExporterParameters) parameterValues;
	}

	public void actionPerformed(ActionEvent e) {
        PeakList[] selectedPeakLists = desktop.getSelectedPeakLists();

        if (selectedPeakLists.length != 1) {
            desktop.displayErrorMessage("Please select a single peak list to save");
            return;
        }

        ExitCode setupExitCode = parameters.showSetupDialog();

        if (setupExitCode != ExitCode.OK) {
            return;
        }

        runModule(null, selectedPeakLists, parameters);
	}

	public Task[] runModule(RawDataFile[] dataFiles, PeakList[] peakLists,
			ParameterSet parameters) {

		XMLExportTask task = new XMLExportTask(peakLists[0],
                (XMLExporterParameters) parameters);

		MZmineCore.getTaskController().addTask(task);

        return new Task[] { task };

	}

	

	public BatchStepCategory getBatchStepCategory() {
		return BatchStepCategory.PROJECT;
	}

    public String toString() {
        return MODULE_NAME;
    }
    
}
