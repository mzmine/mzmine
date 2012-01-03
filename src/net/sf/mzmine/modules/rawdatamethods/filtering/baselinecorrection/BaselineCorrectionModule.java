/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */

package net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;

/**
 * Baseline correction module.
 * 
 * @author $Author$
 * @version $Revision$
 */
public class BaselineCorrectionModule implements MZmineProcessingModule {

	// Module name.
	private static final String MODULE_NAME = "Baseline correction";

	// Parameters.
	private final BaselineCorrectionParameters parameterSet = new BaselineCorrectionParameters();

	@Override
	public ParameterSet getParameterSet() {
		return parameterSet;
	}

	@Override
	public String toString() {
		return MODULE_NAME;
	}

	@Override
	public Task[] runModule(final ParameterSet parameters) {

		RawDataFile dataFiles[] = parameters.getParameter(
				BaselineCorrectionParameters.dataFiles).getValue();

		// Create a task for each data file.
		Task[] tasks = new BaselineCorrectionTask[dataFiles.length];
		int i = 0;
		for (final RawDataFile dataFile : dataFiles) {

			tasks[i++] = new BaselineCorrectionTask(dataFile, parameters);
		}

		// Queue and return tasks.
		MZmineCore.getTaskController().addTasks(tasks);

		return tasks;
	}

	@Override
	public MZmineModuleCategory getModuleCategory() {
		return MZmineModuleCategory.RAWDATAFILTERING;
	}

}
