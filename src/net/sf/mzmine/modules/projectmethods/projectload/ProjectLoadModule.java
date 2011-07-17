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

import java.io.File;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.modules.projectmethods.projectsave.ProjectSaveModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.GUIUtils;

/**
 * This class implements BatchStep interface, so project loading can be
 * activated in a batch.
 * 
 */
public class ProjectLoadModule implements MZmineProcessingModule {

	final String helpID = GUIUtils.generateHelpID(ProjectSaveModule.class);

	public static final String MODULE_NAME = "Open project";

	private static ProjectLoadModule myInstance;

	private ProjectLoaderParameters parameters = new ProjectLoaderParameters();


	public ProjectLoadModule() {
		myInstance = this;
	}

	public Task[] runModule(ParameterSet parameters) {
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



	public ParameterSet getParameterSet() {
		return parameters;
	}

	public MZmineModuleCategory getModuleCategory() {
		return MZmineModuleCategory.PROJECTIO;
	}

	public String toString() {
		return MODULE_NAME;
	}

	public static ProjectLoadModule getInstance() {
		return myInstance;
	}

}
