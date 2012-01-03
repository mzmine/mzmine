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

package net.sf.mzmine.modules.peaklistmethods.alignment.path;

import java.util.logging.Logger;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;

/**
 * 
 */
public class PathAlignerModule implements MZmineProcessingModule {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private PathAlignerParameters parameters = new PathAlignerParameters();
	private Desktop desktop;

	@Override
	public String toString() {
		return "Path alignment";
	}

	public ParameterSet getParameterSet() {
		return parameters;
	}

	public void taskStarted(Task task) {
		logger.info("Running Path alignment");
	}

	public Task[] runModule(ParameterSet parameters) {

		PeakList peakLists[] = parameters.getParameter(
				PathAlignerParameters.peakLists).getValue();

		// check peak lists
		if ((peakLists == null) || (peakLists.length == 0)) {
			desktop.displayErrorMessage("Please select peak lists for alignment");
			return null;
		}

		// prepare a new group with just one task
		Task task = new PathAlignerTask(peakLists, parameters);

		MZmineCore.getTaskController().addTask(task);

		return new Task[] { task };
	}

	@Override
	public MZmineModuleCategory getModuleCategory() {
		return MZmineModuleCategory.ALIGNMENT;
	}

}
