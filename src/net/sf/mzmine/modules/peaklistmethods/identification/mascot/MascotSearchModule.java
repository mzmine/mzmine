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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.mascot;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;

/**
 *
 */
public class MascotSearchModule implements MZmineProcessingModule {

	public static final String MODULE_NAME = "Mascot MS/MS Ion Search (experimental)";

    // private Logger logger = Logger.getLogger(this.getClass().getName());
    
    
    private MascotSearchParameters parameters= new MascotSearchParameters();
	
    // private ParameterSet mascotParameters;

	/**
	 * @see net.sf.mzmine.modules.MZmineModule#getParameterSet()
	 */
	public ParameterSet getParameterSet() {
		return parameters;
	}


	/**
	 * @see net.sf.mzmine.modules.MZmineProcessingModule#getModuleCategory()
	 */
	public MZmineModuleCategory getModuleCategory() {
		return MZmineModuleCategory.IDENTIFICATION;
	}

	/**
	 * @see net.sf.mzmine.modules.batchmode.BatchStep#runModule(net.sf.mzmine.data.RawDataFile[],
	 *      net.sf.mzmine.data.PeakList[], net.sf.mzmine.data.ParameterSet,
	 *      net.sf.mzmine.taskcontrol.Task[]Listener)
	 */
	public Task[] runModule(
			ParameterSet parameters) {
		
		PeakList peakLists[] = parameters.getParameter(MascotParameters.peakLists).getValue();
		
		if (peakLists == null) {
			throw new IllegalArgumentException(
					"Cannot run identification without a peak list");
		}

		// prepare a new sequence of tasks
		Task tasks[] = new MascotSearchTask[peakLists.length];
		for (int i = 0; i < peakLists.length; i++) {
			tasks[i] = new MascotSearchTask(
					(MascotParameters) parameters, peakLists[i]);
		}

		// execute the sequence
		MZmineCore.getTaskController().addTasks(tasks);

		return tasks;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return MODULE_NAME;
	}

}
