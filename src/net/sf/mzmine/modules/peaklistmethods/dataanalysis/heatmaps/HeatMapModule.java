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

package net.sf.mzmine.modules.peaklistmethods.dataanalysis.heatmaps;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;

public class HeatMapModule implements MZmineProcessingModule {

	private HeatMapParameters parameters = new HeatMapParameters();

	public ParameterSet getParameterSet() {
		return parameters;
	}

	@Override
	public String toString() {
		return "Heat map plot";
	}

	public Task[] runModule(ParameterSet parameters) {
		PeakList[] selectedDatasets = MZmineCore.getDesktop()
				.getSelectedPeakLists();
		HeatMapTask heatMapTask = new HeatMapTask(selectedDatasets[0],
				parameters);
		MZmineCore.getTaskController().addTask(heatMapTask);
		return new Task[] { heatMapTask };

	}

	public MZmineModuleCategory getModuleCategory() {
		return MZmineModuleCategory.DATAANALYSIS;
	}

}
