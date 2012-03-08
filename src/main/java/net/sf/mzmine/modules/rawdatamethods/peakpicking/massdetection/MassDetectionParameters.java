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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.centroid.CentroidMassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.exactmass.ExactMassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.localmaxima.LocalMaxMassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.recursive.RecursiveMassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.wavelet.WaveletMassDetector;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.MSLevelParameter;
import net.sf.mzmine.parameters.parametertypes.ModuleComboParameter;
import net.sf.mzmine.parameters.parametertypes.RawDataFilesParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.util.ExitCode;

public class MassDetectionParameters extends SimpleParameterSet {

    public static final MassDetector massDetectors[] = {
	    new CentroidMassDetector(), new ExactMassDetector(),
	    new LocalMaxMassDetector(), new RecursiveMassDetector(),
	    new WaveletMassDetector() };

    public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

    public static final ModuleComboParameter<MassDetector> massDetector = new ModuleComboParameter<MassDetector>(
	    "Mass detector",
	    "Algorithm to use for mass detection and its parameters",
	    massDetectors);

    public static final MSLevelParameter msLevel = new MSLevelParameter();

    public static final StringParameter name = new StringParameter(
	    "Mass list name",
	    "Name of the new mass list. If the processed scans already have a mass list of that name, it will be replaced.",
	    "masses");

    public MassDetectionParameters() {
	super(new Parameter[] { dataFiles, massDetector, msLevel, name });
    }

    @Override
    public ExitCode showSetupDialog() {

	ExitCode exitCode = super.showSetupDialog();

	// If the parameters are not complete, let's just stop here
	if (exitCode != ExitCode.OK)
	    return exitCode;

	// Do an additional check for centroid/continuous data and show a
	// warning if there is a potential problem
	boolean centroidData = false;
	int selectedMSLevel = getParameter(msLevel).getValue();
	RawDataFile selectedFiles[] = getParameter(dataFiles).getValue();

	// If no file selected (e.g. in batch mode setup), just return
	if (selectedFiles == null)
	    return exitCode;

	for (RawDataFile file : selectedFiles) {
	    int scanNums[] = file.getScanNumbers(selectedMSLevel);
	    for (int scanNum : scanNums) {
		Scan s = file.getScan(scanNum);
		if (s.isCentroided())
		    centroidData = true;
	    }
	}

	// Check the selected mass detector
	String massDetectorName = getParameter(massDetector).getValue()
		.toString();

	if ((centroidData) && (!massDetectorName.startsWith("Centroid"))) {
	    String msg = "One or more selected files contains centroided data points at MS level "
		    + selectedMSLevel
		    + ". The selected mass detector could give unexpected results.";
	    MZmineCore.getDesktop().displayMessage(msg);
	}

	if ((!centroidData) && (massDetectorName.startsWith("Centroid"))) {
	    String msg = "None one of the selected files contain centroided data points at MS level "
		    + selectedMSLevel
		    + ". The selected mass detector could give unexpected results.";
	    MZmineCore.getDesktop().displayMessage(msg);
	}

	return exitCode;

    }

}
