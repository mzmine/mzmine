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

package net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection;

import java.awt.Window;
import java.util.ArrayList;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.modules.visualization.spectra.SpectraPlot;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizerWindow;
import net.sf.mzmine.modules.visualization.spectra.datasets.DataPointsDataSet;
import net.sf.mzmine.modules.visualization.spectra.datasets.ScanDataSet;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialogWithScanPreview;

/**
 * This class extends ParameterSetupDialog class, including a spectraPlot. This
 * is used to preview how the selected mass detector and his parameters works
 * over the raw data file.
 */
public class MassDetectorSetupDialog extends
	ParameterSetupDialogWithScanPreview {

    private static final long serialVersionUID = 1L;
    private MassDetector massDetector;
    private ParameterSet parameters;

    /**
     * @param parameters
     * @param massDetectorTypeNumber
     */
    public MassDetectorSetupDialog(Window parent, boolean valueCheckRequired,
	    Class<?> massDetectorClass, ParameterSet parameters) {

	super(parent, valueCheckRequired, parameters);

	this.parameters = parameters;

	for (MassDetector detector : MassDetectionParameters.massDetectors) {
	    if (detector.getClass().equals(massDetectorClass)) {
		this.massDetector = detector;
	    }
	}
    }

    protected void loadPreview(SpectraPlot spectrumPlot, Scan previewScan) {

	ScanDataSet spectraDataSet = new ScanDataSet(previewScan);

	// Set plot mode only if it hasn't been set before
	// if the scan is centroided, switch to centroid mode
	spectrumPlot.setPlotMode(previewScan.getSpectrumType());

	spectrumPlot.removeAllDataSets();
	spectrumPlot.addDataSet(spectraDataSet,
		SpectraVisualizerWindow.scanColor, false);

	// If there is some illegal value, do not load the preview but just exit
	ArrayList<String> errorMessages = new ArrayList<String>();
	boolean paramsOK = parameterSet.checkParameterValues(errorMessages);
	if (!paramsOK)
	    return;

	DataPoint[] mzValues = massDetector.getMassValues(previewScan,
		parameters);

	DataPointsDataSet peaksDataSet = new DataPointsDataSet(
		"Detected peaks", mzValues);

	spectrumPlot.addDataSet(peaksDataSet,
		SpectraVisualizerWindow.peaksColor, false);

    }

}