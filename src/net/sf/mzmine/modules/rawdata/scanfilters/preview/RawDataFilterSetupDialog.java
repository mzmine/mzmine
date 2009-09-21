/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.rawdata.scanfilters.preview;

import java.awt.Color;
import java.lang.reflect.Constructor;
import java.util.logging.Logger;

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.rawdata.scanfilters.RawDataFilteringParameters;
import net.sf.mzmine.modules.visualization.spectra.PlotMode;
import net.sf.mzmine.modules.visualization.spectra.SpectraPlot;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizerWindow;
import net.sf.mzmine.modules.visualization.spectra.datasets.ScanDataSet;
import net.sf.mzmine.util.dialogs.ParameterSetupDialogWithScanPreview;

/**
 * This class extends ParameterSetupDialog class, including a spectraPlot. This
 * is used to preview how the selected raw data filter and his parameters works
 * over the raw data file.
 */
public class RawDataFilterSetupDialog extends
		ParameterSetupDialogWithScanPreview {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	// Raw Data Filter;
	private RawDataFilter rawDataFilter;
	private SimpleParameterSet mdParameters;
	private int rawDataFilterTypeNumber;

	/**
	 * @param parameters
	 * @param rawDataFilterTypeNumber
	 */
	public RawDataFilterSetupDialog(RawDataFilteringParameters parameters,
			int rawDataFilterTypeNumber) {

		super(
				RawDataFilteringParameters.rawDataFilterNames[rawDataFilterTypeNumber]
						+ "'s parameter setup dialog ",
				parameters
						.getRawDataFilteringParameters(rawDataFilterTypeNumber),
				RawDataFilteringParameters.rawDataFilterHelpFiles[rawDataFilterTypeNumber]);

		this.rawDataFilterTypeNumber = rawDataFilterTypeNumber;

		// Parameters of local raw data filter to get preview values
		mdParameters = parameters
				.getRawDataFilteringParameters(rawDataFilterTypeNumber);

	}

	/**
	 * This function set all the information into the plot chart
	 * 
	 * @param scanNumber
	 */
	protected void loadPreview(SpectraPlot spectrumPlot, Scan previewScan) {

		String rawDataFilterClassName = RawDataFilteringParameters.rawDataFilterClasses[rawDataFilterTypeNumber];

		try {
			Class rawDataFilterClass = Class.forName(rawDataFilterClassName);
			Constructor rawDataFilterConstruct = rawDataFilterClass
					.getConstructors()[0];
			rawDataFilter = (RawDataFilter) rawDataFilterConstruct
					.newInstance(mdParameters);
		} catch (Exception e) {
			MZmineCore.getDesktop().displayErrorMessage(
					"Error trying to make an instance of raw data filter "
							+ rawDataFilterClassName);
			logger
					.warning("Error trying to make an instance of raw data filter "
							+ rawDataFilterClassName);
			return;
		}

		Scan newScan = rawDataFilter.getNewScan(previewScan);

		ScanDataSet spectraDataSet = new ScanDataSet(newScan);
		ScanDataSet spectraOriginalDataSet = new ScanDataSet(previewScan);

		spectrumPlot.addDataSet(spectraOriginalDataSet,
				SpectraVisualizerWindow.scanColor, true);
		spectrumPlot.addDataSet(spectraDataSet, Color.green, true);

		// if the scan is centroided, switch to centroid mode
		if (previewScan.isCentroided()) {
			spectrumPlot.setPlotMode(PlotMode.CENTROID);
		} else {
			spectrumPlot.setPlotMode(PlotMode.CONTINUOUS);
		}

	}

}