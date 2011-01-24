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

package net.sf.mzmine.modules.rawdatamethods.peakpicking.chromatogrambuilder.massdetection;

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.chromatogrambuilder.MzPeak;
import net.sf.mzmine.modules.visualization.spectra.PlotMode;
import net.sf.mzmine.modules.visualization.spectra.SpectraPlot;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizerWindow;
import net.sf.mzmine.modules.visualization.spectra.datasets.MzPeaksDataSet;
import net.sf.mzmine.modules.visualization.spectra.datasets.ScanDataSet;
import net.sf.mzmine.util.dialogs.ParameterSetupDialogWithScanPreview;

/**
 * This class extends ParameterSetupDialog class, including a spectraPlot. This
 * is used to preview how the selected mass detector and his parameters works
 * over the raw data file.
 */
public class MassDetectorSetupDialog extends
		ParameterSetupDialogWithScanPreview {

	// Mass Detector;
	private MassDetector massDetector;

	/**
	 * @param parameters
	 * @param massDetectorTypeNumber
	 */
	public MassDetectorSetupDialog(MassDetector massDetector) {

		super(massDetector.getName() + "'s parameter setup dialog ",
				massDetector.getParameters(), null);

		this.massDetector = massDetector;

	}

	protected void loadPreview(SpectraPlot spectrumPlot, Scan previewScan) {

		ScanDataSet spectraDataSet = new ScanDataSet(previewScan);

		// Set plot mode only if it hasn't been set before
		// if the scan is centroided, switch to centroid mode
		if (previewScan.isCentroided()) {
			spectrumPlot.setPlotMode(PlotMode.CENTROID);
		} else {
			spectrumPlot.setPlotMode(PlotMode.CONTINUOUS);
		}

		MzPeak[] mzValues = massDetector.getMassValues(previewScan);

		MzPeaksDataSet peaksDataSet = new MzPeaksDataSet("Detected peaks",
				mzValues);

		spectrumPlot.removeAllDataSets();
		spectrumPlot.addDataSet(spectraDataSet,
				SpectraVisualizerWindow.scanColor, false);
		spectrumPlot.addDataSet(peaksDataSet,
				SpectraVisualizerWindow.peaksColor, false);

	}

}