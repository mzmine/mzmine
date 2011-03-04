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

package net.sf.mzmine.modules.peaklistmethods.io.xmlimport;

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
import net.sf.mzmine.modules.peaklistmethods.io.xmlexport.XMLExporter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.dialogs.ExitCode;

public class XMLImporter implements MZmineModule, ActionListener, BatchStep {

	final String helpID = GUIUtils.generateHelpID(XMLExporter.class);

	public static final String MODULE_NAME = "Import from XML file";

	private XMLImporterParameters parameters;
	private Desktop desktop;
	public static XMLImporter myInstance;

	public ParameterSet getParameterSet() {
		return parameters;
	}

	public XMLImporter() {

		this.desktop = MZmineCore.getDesktop();

		parameters = new XMLImporterParameters();

		desktop.addMenuItem(MZmineMenu.PEAKLISTEXPORT, MODULE_NAME,
				"Load a peak list from a XML file", KeyEvent.VK_I, true, this,
				null);

		myInstance = this;

	}

	public void initLightModule() {

		this.desktop = MZmineCore.getDesktop();

		parameters = new XMLImporterParameters();

	}

	public void actionPerformed(ActionEvent e) {

		ExitCode setupExitCode = parameters.showSetupDialog();

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

/*	public void loadPeakLists(String[] peakListNames) {

		Parameter filename;
		ParameterSet parameterSet;
		for (String name : peakListNames) {
			parameterSet = new XMLImporterParameters();
			filename = parameterSet.getParameter("Filename");
			filename.setValue(name);
			runModule(null, null, parameterSet);
		}

	}*/

	public BatchStepCategory getBatchStepCategory() {
		return BatchStepCategory.PROJECT;
	}

	public String toString() {
		return MODULE_NAME;
	}

}
