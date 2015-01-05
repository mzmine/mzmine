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

package net.sf.mzmine.modules.masslistmethods.shoulderpeaksfilter;

import java.awt.Color;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MassList;
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
public class ShoulderPeaksFilterSetupDialog extends
	ParameterSetupDialogWithScanPreview {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private static final Color removedPeaksColor = Color.orange;

    private ParameterSet parameters;

    /**
     * @param parameters
     * @param massFilterTypeNumber
     */
    public ShoulderPeaksFilterSetupDialog(Window parent,
	    boolean valueCheckRequired, ParameterSet parameters) {
	super(parent, valueCheckRequired, parameters);
	this.parameters = parameters;
    }

    /**
     * This function set all the information into the plot chart
     * 
     * @param scanNumber
     */
    protected void loadPreview(SpectraPlot spectrumPlot, Scan previewScan) {

	// Remove previous data sets
	spectrumPlot.removeAllDataSets();

	// Add scan data set
	ScanDataSet scanDataSet = new ScanDataSet(previewScan);
	spectrumPlot.addDataSet(scanDataSet, SpectraVisualizerWindow.scanColor,
		false);

	// If the scan is centroided, switch to centroid mode
	spectrumPlot.setPlotMode(previewScan.getSpectrumType());

	// If the parameters are not complete, exit
	ArrayList<String> errors = new ArrayList<String>();
	boolean paramsOK = parameters.checkParameterValues(errors);
	if (!paramsOK)
	    return;

	// Get mass list
	String massListName = parameters.getParameter(
		ShoulderPeaksFilterParameters.massList).getValue();
	MassList massList = previewScan.getMassList(massListName);
	if (massList == null)
	    return;

	// Perform filtering
	DataPoint mzValues[] = massList.getDataPoints();
	DataPoint remainingMzValues[] = ShoulderPeaksFilter.filterMassValues(
		mzValues, parameters);

	Vector<DataPoint> removedPeaks = new Vector<DataPoint>();
	removedPeaks.addAll(Arrays.asList(mzValues));
	removedPeaks.removeAll(Arrays.asList(remainingMzValues));
	DataPoint removedMzValues[] = removedPeaks.toArray(new DataPoint[0]);

	// Add mass list data sets
	DataPointsDataSet removedPeaksDataSet = new DataPointsDataSet(
		"Removed peaks", removedMzValues);
	DataPointsDataSet remainingPeaksDataSet = new DataPointsDataSet(
		"Remaining peaks", remainingMzValues);

	spectrumPlot.addDataSet(removedPeaksDataSet, removedPeaksColor, false);
	spectrumPlot.addDataSet(remainingPeaksDataSet,
		SpectraVisualizerWindow.peaksColor, false);

    }

}