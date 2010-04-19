/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massfilters;

import java.awt.Color;
import java.util.Arrays;
import java.util.Vector;

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.modules.peakpicking.chromatogrambuilder.MzPeak;
import net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massdetection.MassDetector;
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
public class MassFilterSetupDialog extends ParameterSetupDialogWithScanPreview {

	public static final Color removedPeaksColor = Color.orange;

	// Mass detector and filter;
	private MassDetector massDetector;
	private MassFilter massFilter;

	

	/**
	 * @param parameters
	 * @param massFilterTypeNumber
	 */
	public MassFilterSetupDialog(MassDetector massDetector,
			MassFilter massFilter) {

		super(massFilter.getName() + "'s parameter setup dialog ", massFilter
				.getParameters(), massFilter.getHelpFileLocation());

		this.massFilter = massFilter;
		this.massDetector = massDetector;

	}

	/**
	 * This function set all the information into the plot chart
	 * 
	 * @param scanNumber
	 */
	protected void loadPreview(SpectraPlot spectrumPlot, Scan previewScan) {
		
		ScanDataSet scanDataSet = new ScanDataSet(previewScan);

		MzPeak mzValues[] = massDetector.getMassValues(previewScan);
		MzPeak remainingMzValues[] = massFilter.filterMassValues(mzValues);

		Vector<MzPeak> removedPeaks = new Vector<MzPeak>();
		removedPeaks.addAll(Arrays.asList(mzValues));
		removedPeaks.removeAll(Arrays.asList(remainingMzValues));
		MzPeak removedMzValues[] = removedPeaks.toArray(new MzPeak[0]);

		MzPeaksDataSet removedPeaksDataSet = new MzPeaksDataSet("Removed peaks",
				removedMzValues);
		MzPeaksDataSet remainingPeaksDataSet = new MzPeaksDataSet("Remaining peaks",
				remainingMzValues);

		spectrumPlot.removeAllDataSets();
		spectrumPlot.addDataSet(scanDataSet, SpectraVisualizerWindow.scanColor,
				false);
		spectrumPlot.addDataSet(removedPeaksDataSet, removedPeaksColor, false);
		spectrumPlot.addDataSet(remainingPeaksDataSet,
				SpectraVisualizerWindow.peaksColor, false);

		// if the scan is centroided, switch to centroid mode
		if (previewScan.isCentroided()) {
			spectrumPlot.setPlotMode(PlotMode.CENTROID);
		} else {
			spectrumPlot.setPlotMode(PlotMode.CONTINUOUS);
		}
		
	}

}