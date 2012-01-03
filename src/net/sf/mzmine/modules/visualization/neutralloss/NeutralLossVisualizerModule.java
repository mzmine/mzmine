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

package net.sf.mzmine.modules.visualization.neutralloss;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;

/**
 * Neutral loss (MS/MS) visualizer using JFreeChart library
 */
public class NeutralLossVisualizerModule implements MZmineProcessingModule {

	private NeutralLossParameters parameters = new NeutralLossParameters();

	/**
	 * @see net.sf.mzmine.modules.MZmineModule#toString()
	 */
	public String toString() {
		return "Neutral loss visualizer";
	}

	/**
	 * @see net.sf.mzmine.modules.MZmineModule#getParameterSet()
	 */
	public ParameterSet getParameterSet() {
		return parameters;
	}

	@Override
	public Task[] runModule(ParameterSet parameters) {
		
		RawDataFile dataFiles[] = parameters.getParameter(
				NeutralLossParameters.dataFiles).getValue();

		if ((dataFiles == null) || (dataFiles.length == 0)) {
			MZmineCore.getDesktop().displayErrorMessage(
					"Please select raw data file");
			return null;
		}

		NeutralLossVisualizerWindow newWindow = new NeutralLossVisualizerWindow(
				dataFiles[0], parameters);
		MZmineCore.getDesktop().addInternalFrame(newWindow);

		return null;
	}

	@Override
	public MZmineModuleCategory getModuleCategory() {
		return MZmineModuleCategory.VISUALIZATIONRAWDATA;
	}

}