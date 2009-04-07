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

package net.sf.mzmine.modules.io.xmlexport;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.main.mzmineclient.MZmineModule;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

public class XMLExporter implements MZmineModule, ActionListener {
	
    private XMLExporterParameters parameters;
    private Desktop desktop;


	public ParameterSet getParameterSet() {
        return parameters;
	}

	public void initModule() {
		this.desktop = MZmineCore.getDesktop();

        parameters = new XMLExporterParameters();

        desktop.addMenuItem(MZmineMenu.PEAKLISTEXPORT, "Export to XML file",
                "Save a peak list to XML file", KeyEvent.VK_X, true,
                this, null);		
	}

	public void setParameters(ParameterSet parameterValues) {
        this.parameters = (XMLExporterParameters) parameters;
	}

	public void actionPerformed(ActionEvent e) {
        PeakList[] selectedPeakLists = desktop.getSelectedPeakLists();
        
        if (selectedPeakLists.length != 1) {
            desktop.displayErrorMessage("Please select a single peak list to save");
            return;
        }

        ExitCode setupExitCode = setupParameters(parameters);

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

	public ExitCode setupParameters(ParameterSet parameters) {
        ParameterSetupDialog dialog = new ParameterSetupDialog(
                "Please set parameter values for " + toString(),
                (XMLExporterParameters) parameters);

        dialog.setVisible(true);

        return dialog.getExitCode();
	}
	

}
