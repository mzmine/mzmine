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

package net.sf.mzmine.modules.masslistmethods.massfilters;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;

public class MassFilteringModule implements MZmineProcessingModule {

	private MassFilteringParameters parameters = new MassFilteringParameters();

	/**
	 * @see net.sf.mzmine.modules.MZmineProcessingModule#toString()
	 */
	public String toString() {
		return "Mass list filtering";
	}

	/**
	 * @see net.sf.mzmine.modules.BatchStep#setupParameters(net.sf.mzmine.data.ParameterSet)
	 */

	/**
	 * @see net.sf.mzmine.modules.MZmineModule#getParameterSet()
	 */
	public ParameterSet getParameterSet() {
		return parameters;
	}

	public Task[] runModule(ParameterSet parameters) {

		RawDataFile[] dataFiles = parameters.getParameter(
				MassFilteringParameters.dataFiles).getValue();

		// prepare a new group of tasks
		Task tasks[] = new MassFilteringTask[dataFiles.length];
		for (int i = 0; i < dataFiles.length; i++) {
			tasks[i] = new MassFilteringTask(dataFiles[i], parameters.clone());
		}

		MZmineCore.getTaskController().addTasks(tasks);

		return tasks;
	}

	public MZmineModuleCategory getModuleCategory() {
		return MZmineModuleCategory.PEAKPICKING;
	}

}
