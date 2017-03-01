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

package net.sf.mzmine.modules.visualization.peaklisttable.export;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.ExitCode;

public class MSMSExportModule implements MZmineModule {

    private static final String MODULE_NAME = "MS/MS pattern export";

    @Override
    public @Nonnull String getName() {
	return MODULE_NAME;
    }

    public static void exportMSMS(PeakListRow row) {

	ParameterSet parameters = MZmineCore.getConfiguration()
		.getModuleParameters(MSMSExportModule.class);

	ExitCode exitCode = parameters.showSetupDialog(MZmineCore.getDesktop()
		.getMainWindow(), true);
	if (exitCode != ExitCode.OK)
	    return;

	File outputFile = parameters.getParameter(
		MSMSExportParameters.outputFile).getValue();
	String massListName = parameters.getParameter(
		MSMSExportParameters.massList).getValue();

	if ((outputFile == null) || (massListName == null))
	    return;

	// Best peak always exists, because peak list row has at least one peak
	Feature bestPeak = row.getBestPeak();

	// Get the MS/MS scan number
	int msmsScanNumber = bestPeak.getMostIntenseFragmentScanNumber();
	if (msmsScanNumber < 1) {
	    MZmineCore.getDesktop().displayErrorMessage(
		    MZmineCore.getDesktop().getMainWindow(),
		    "There is no MS/MS scan for peak " + bestPeak);
	    return;
	}

	// MS/MS scan must exist, because msmsScanNumber was > 0
	Scan msmsScan = bestPeak.getDataFile().getScan(msmsScanNumber);

	MassList massList = msmsScan.getMassList(massListName);
	if (massList == null) {
	    MZmineCore.getDesktop().displayErrorMessage(
		    MZmineCore.getDesktop().getMainWindow(),
		    "There is no mass list called " + massListName
			    + " for MS/MS scan #" + msmsScanNumber + " ("
			    + bestPeak.getDataFile() + ")");
	    return;
	}

	DataPoint peaks[] = massList.getDataPoints();

	try {
	    FileWriter fileWriter = new FileWriter(outputFile);
	    BufferedWriter writer = new BufferedWriter(fileWriter);

	    for (DataPoint peak : peaks) {
		writer.write(peak.getMZ() + " " + peak.getIntensity());
		writer.newLine();
	    }

	    writer.close();

	} catch (Exception e) {
	    e.printStackTrace();
	    MZmineCore.getDesktop().displayErrorMessage(
		    MZmineCore.getDesktop().getMainWindow(),
		    "Error writing to file " + outputFile + ": "
			    + ExceptionUtils.exceptionToString(e));
	}

    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
	return MSMSExportParameters.class;
    }

}