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

package net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.dialogs.ExitCode;

/**
 * 
 */
public class FormulaPrediction implements MZmineModule {

	final static String helpID = GUIUtils
			.generateHelpID(FormulaPrediction.class);

	public static final String MODULE_NAME = "Formula prediction";

	private FormulaPredictionParameters parameters;

	private static FormulaPrediction myInstance;

	/**
	 * @see net.sf.mzmine.modules.MZmineModule#getParameterSet()
	 */
	public ParameterSet getParameterSet() {
		return parameters;
	}

	public FormulaPrediction() {

		parameters = new FormulaPredictionParameters();

		myInstance = this;

	}

	public static FormulaPrediction getInstance() {
		return myInstance;
	}

	public static void showSingleRowIdentificationDialog(PeakList peakList,
			PeakListRow row) {

		assert myInstance != null;

		ParameterSet parameters = myInstance.getParameterSet();

		double mzValue = row.getAverageMZ();
		parameters.getParameter(FormulaPredictionParameters.neutralMass)
				.setIonMass(mzValue);

		int charge = row.getBestPeak().getCharge();
		if (charge > 0) {
			parameters.getParameter(FormulaPredictionParameters.neutralMass)
					.setCharge(charge);
		}

		ExitCode exitCode = parameters.showSetupDialog();
		if (exitCode != ExitCode.OK)
			return;

		SingleRowPredictionTask newTask = new SingleRowPredictionTask(
				parameters.clone(), peakList, row);

		// execute the sequence
		MZmineCore.getTaskController().addTask(newTask);

	}

	public String toString() {
		return MODULE_NAME;
	}

}
