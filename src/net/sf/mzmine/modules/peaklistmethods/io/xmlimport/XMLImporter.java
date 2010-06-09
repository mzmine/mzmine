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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.io.xmlimport;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

public class XMLImporter implements MZmineModule, ActionListener {

	private XMLImporterParameters parameters;
	private Desktop desktop;
	public static XMLImporter myInstance;

	public ParameterSet getParameterSet() {
		return parameters;
	}

	public void initModule() {

		this.desktop = MZmineCore.getDesktop();

		parameters = new XMLImporterParameters();

		desktop.addMenuItem(MZmineMenu.PEAKLISTEXPORT, "Import from XML file",
				"Load a peak list from a XML file", KeyEvent.VK_I, true, this,
				null);

		myInstance = this;

	}

	public void initLightModule() {

		this.desktop = MZmineCore.getDesktop();

		parameters = new XMLImporterParameters();

	}

	public void setParameters(ParameterSet parameterValues) {
		this.parameters = (XMLImporterParameters) parameterValues;
	}

	public void actionPerformed(ActionEvent e) {

		ExitCode setupExitCode = setupParameters(parameters);

		if (setupExitCode != ExitCode.OK) {
			return;
		}

		runModule(null, null, parameters);

	}

	public Task[] runModule(RawDataFile[] dataFiles, PeakList[] peakLists,
			ParameterSet parameters) {

		XMLImportTask task = new XMLImportTask(
				(XMLImporterParameters) parameters);

		MZmineCore.getTaskController().addTask(task);

		return new Task[] { task };

	}

	public ExitCode setupParameters(ParameterSet parameters) {
		ParameterSetupDialog dialog = new ParameterSetupDialog(
				"Please set parameter values for " + toString(),
				(XMLImporterParameters) parameters);

		dialog.setVisible(true);

		return dialog.getExitCode();
	}

	public void loadPeakLists(String[] peakListNames) {

		Parameter filename;
		SimpleParameterSet parameterSet;
		for (String name : peakListNames) {
			parameterSet = new XMLImporterParameters();
			filename = parameterSet.getParameter("Filename");
			parameterSet.setParameterValue(filename, name);
			runModule(null, null, parameterSet);
		}

	}

}
