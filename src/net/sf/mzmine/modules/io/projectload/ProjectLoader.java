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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.io.projectload;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.batchmode.BatchStep;
import net.sf.mzmine.modules.batchmode.BatchStepCategory;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.dialogs.ExitCode;

/**
 * This class implements BatchStep interface, so project loading can be
 * activated in a batch.
 * 
 */
public class ProjectLoader implements BatchStep, ActionListener {

	private ProjectLoaderParameters parameters;

	private Desktop desktop;

	public void initModule() {

		this.desktop = MZmineCore.getDesktop();

		parameters = new ProjectLoaderParameters();

		desktop.addMenuItem(MZmineMenu.PROJECTIO, "Open project...",
				"Loads a stored MZmine project", KeyEvent.VK_O, true, this,
				null);

	}

	public Task[] runModule(RawDataFile[] dataFiles, PeakList[] peakLists,
			ParameterSet parameterSet) {
		ProjectLoaderParameters parameters = (ProjectLoaderParameters) parameterSet;
		String selectedFileName = (String) parameters
				.getParameterValue(ProjectLoaderParameters.projectFile);
		File selectedFile = new File(selectedFileName);
		ProjectOpeningTask task = new ProjectOpeningTask(selectedFile);
		Task[] tasksArray = new Task[] { task };
		MZmineCore.getTaskController().addTasks(tasksArray);
		return tasksArray;
	}

	public ExitCode setupParameters(ParameterSet parameterSet) {

		ProjectLoaderParameters parameters = (ProjectLoaderParameters) parameterSet;
		String lastDirectory = (String) parameters
				.getParameterValue(ProjectLoaderParameters.lastDirectory);
		JFileChooser chooser = new JFileChooser();
		if (lastDirectory != null)
			chooser.setCurrentDirectory(new File(lastDirectory));

		FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"MZmine 2 projects", "mzmine");
		
		chooser.setFileFilter(filter);
		int returnVal = chooser.showOpenDialog(MZmineCore.getDesktop()
				.getMainFrame());

		if (returnVal != JFileChooser.APPROVE_OPTION)
			return ExitCode.CANCEL;

		File selectedFile = chooser.getSelectedFile();
		lastDirectory = selectedFile.getParent();
		parameters.setParameterValue(ProjectLoaderParameters.projectFile,
				selectedFile.getPath());
		parameters.setParameterValue(ProjectLoaderParameters.lastDirectory,
				lastDirectory);
		return ExitCode.OK;

	}

	public void actionPerformed(ActionEvent event) {

		ExitCode setupExitCode = setupParameters(parameters);

		if (setupExitCode != ExitCode.OK) {
			return;
		}

		runModule(null, null, parameters.clone());

	}

	public ParameterSet getParameterSet() {
		return parameters;
	}

	public void setParameters(ParameterSet parameters) {
		this.parameters = (ProjectLoaderParameters) parameters;
	}

	public BatchStepCategory getBatchStepCategory() {
		return BatchStepCategory.PROJECT;
	}
	
	public String toString() {
		return "Project load"; 
	}

}
