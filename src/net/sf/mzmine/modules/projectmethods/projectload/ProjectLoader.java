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

package net.sf.mzmine.modules.projectmethods.projectload;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.batchmode.BatchStep;
import net.sf.mzmine.modules.batchmode.BatchStepCategory;
import net.sf.mzmine.modules.projectmethods.projectsave.ProjectSaver;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.dialogs.ExitCode;

/**
 * This class implements BatchStep interface, so project loading can be
 * activated in a batch.
 * 
 */
public class ProjectLoader implements BatchStep, ActionListener {

	final String helpID = GUIUtils.generateHelpID(ProjectSaver.class);

	public static final String MODULE_NAME = "Open project";

	private static ProjectLoader myInstance;

	private ProjectLoaderParameters parameters;

	public ProjectLoader() {

		myInstance = this;

		parameters = new ProjectLoaderParameters();

		MZmineCore.getDesktop().addMenuItem(MZmineMenu.PROJECTIO, MODULE_NAME,
				"Loads a stored MZmine project", KeyEvent.VK_O, true, this,
				null);

	}

	public Task[] runModule(RawDataFile[] dataFiles, PeakList[] peakLists,
			ParameterSet parameters) {
		File selectedFile = parameters.getParameter(
				ProjectLoaderParameters.projectFile).getValue();
		if (selectedFile == null) {
			return null;
		}
		ProjectOpeningTask task = new ProjectOpeningTask(selectedFile);
		Task[] tasksArray = new Task[] { task };
		MZmineCore.getTaskController().addTasks(tasksArray);
		return tasksArray;
	}

	public void actionPerformed(ActionEvent event) {

		ExitCode setupExitCode = parameters.showSetupDialog();

		if (setupExitCode != ExitCode.OK) {
			return;
		}

		runModule(null, null, parameters.clone());

	}

	public ParameterSet getParameterSet() {
		return parameters;
	}

	public BatchStepCategory getBatchStepCategory() {
		return BatchStepCategory.PROJECT;
	}

	public String toString() {
		return MODULE_NAME;
	}

	public static ProjectLoader getInstance() {
		return myInstance;
	}

}
