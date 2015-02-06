/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.ExitCode;

public class FormulaPredictionModule implements MZmineModule {

    private static final String MODULE_NAME = "Formula prediction";

    public static void showSingleRowIdentificationDialog(PeakListRow row) {

	ParameterSet parameters = MZmineCore.getConfiguration()
		.getModuleParameters(FormulaPredictionModule.class);

	double mzValue = row.getAverageMZ();
	parameters.getParameter(FormulaPredictionParameters.neutralMass)
		.setIonMass(mzValue);

	int bestScanNum = row.getBestPeak().getRepresentativeScanNumber();
	if (bestScanNum > 0) {
	    RawDataFile dataFile = row.getBestPeak().getDataFile();
	    Scan bestScan = dataFile.getScan(bestScanNum);
	    PolarityType scanPolarity = bestScan.getPolarity();
	    switch (scanPolarity) {
	    case POSITIVE:
		parameters
			.getParameter(FormulaPredictionParameters.neutralMass)
			.setIonType(IonizationType.POSITIVE_HYDROGEN);
		break;
	    case NEGATIVE:
		parameters
			.getParameter(FormulaPredictionParameters.neutralMass)
			.setIonType(IonizationType.NEGATIVE_HYDROGEN);
		break;
	    default:
		break;
	    }
	}

	int charge = row.getBestPeak().getCharge();
	if (charge > 0) {
	    parameters.getParameter(FormulaPredictionParameters.neutralMass)
		    .setCharge(charge);
	}

	ExitCode exitCode = parameters.showSetupDialog(MZmineCore.getDesktop()
		.getMainWindow(), true);
	if (exitCode != ExitCode.OK)
	    return;

	SingleRowPredictionTask newTask = new SingleRowPredictionTask(
		parameters.cloneParameterSet(), row);

	// execute the sequence
	MZmineCore.getTaskController().addTask(newTask);

    }

    @Override
    public @Nonnull String getName() {
	return MODULE_NAME;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
	return FormulaPredictionParameters.class;
    }

}
