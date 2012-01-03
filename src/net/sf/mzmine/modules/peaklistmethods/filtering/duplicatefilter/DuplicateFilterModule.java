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

package net.sf.mzmine.modules.peaklistmethods.filtering.duplicatefilter;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;

/**
 * Duplicate peak filter
 * 
 * This filter cleans up a peak list by keeping only the row with the strongest
 * median peak area of all rows having same (optionally) identification and
 * similar m/z and rt values (within tolerances)
 * 
 * Idea is to run this filter before alignment on peak lists with peaks from a
 * single raw data file in each list, but it will work on aligned peak lists
 * too.
 * 
 */
public class DuplicateFilterModule implements MZmineProcessingModule {

	private DuplicateFilterParameters parameters = new DuplicateFilterParameters();

	/**
	 * @see net.sf.mzmine.modules.MZmineProcessingModule#toString()
	 */
	public String toString() {
		return "Duplicate peak filter";
	}

	/**
	 * @see net.sf.mzmine.modules.MZmineModule#getParameterSet()
	 */
	public ParameterSet getParameterSet() {
		return parameters;
	}

	public Task[] runModule(ParameterSet parameters) {

		PeakList[] peakLists = parameters.getParameter(
				DuplicateFilterParameters.peakLists).getValue();

		// prepare a new group of tasks
		Task tasks[] = new DuplicateFilterTask[peakLists.length];
		for (int i = 0; i < peakLists.length; i++) {
			tasks[i] = new DuplicateFilterTask(peakLists[i], parameters);
		}

		MZmineCore.getTaskController().addTasks(tasks);

		return tasks;

	}

	public MZmineModuleCategory getModuleCategory() {
		return MZmineModuleCategory.PEAKLISTFILTERING;
	}
}
