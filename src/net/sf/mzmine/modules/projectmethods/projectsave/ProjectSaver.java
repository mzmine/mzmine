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

package net.sf.mzmine.modules.projectmethods.projectsave;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JMenuItem;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.batchmode.BatchStep;
import net.sf.mzmine.modules.batchmode.BatchStepCategory;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.dialogs.ExitCode;

/**
 * This class implements BatchStep interface, so that project saving can be
 * activated in a batch
 * 
 */
public class ProjectSaver implements BatchStep, ActionListener {

	final String helpID = GUIUtils.generateHelpID(this);

	public static final String MODULE_NAME = "Save project";

	private ProjectSaverParameters parameters;
	private JMenuItem projectSave, projectSaveAs;

	private Desktop desktop;

	public ProjectSaver() {

		this.desktop = MZmineCore.getDesktop();

		parameters = new ProjectSaverParameters();

		projectSave = desktop.addMenuItem(MZmineMenu.PROJECTIO, MODULE_NAME,
				"Saves the MZmine project", KeyEvent.VK_S, true, this, null);

		projectSaveAs = desktop.addMenuItem(MZmineMenu.PROJECTIO,
				"Save project as...",
				"Saves the MZmine project under a different name",
				KeyEvent.VK_A, true, this, null);

	}

	public Task[] runModule(RawDataFile[] dataFiles, PeakList[] peakLists,
			ParameterSet parameters) {
		File selectedFile = parameters.getParameter(
				ProjectSaverParameters.projectFile).getValue();
		if (selectedFile == null)
			return null;

		ProjectSavingTask task = new ProjectSavingTask(selectedFile);
		Task[] tasksArray = new Task[] { task };
		MZmineCore.getTaskController().addTasks(tasksArray);
		return tasksArray;
	}

	public void actionPerformed(ActionEvent event) {

		Object src = event.getSource();

		if (src == projectSave) {
			File currentFile = MZmineCore.getCurrentProject().getProjectFile();

			// If the project has not been saved yet, do Save as..
			if (currentFile == null) {
				ExitCode setupExitCode = parameters.showSetupDialog();
				if (setupExitCode != ExitCode.OK)
					return;
				runModule(null, null, parameters.clone());
				return;
			}

			ParameterSet parametersCopy = parameters.clone();
			parametersCopy.getParameter(ProjectSaverParameters.projectFile)
					.setValue(currentFile);
			runModule(null, null, parametersCopy);
		}

		if (src == projectSaveAs) {
			ExitCode setupExitCode = parameters.showSetupDialog();
			if (setupExitCode != ExitCode.OK)
				return;
			ParameterSet parametersCopy = parameters.clone();
			runModule(null, null, parametersCopy);

		}

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

}
