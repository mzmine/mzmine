/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.featdet_massdetection;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerTab;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectrumPlotType;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.MassListDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.ScanDataSet;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialogWithScanPreview;
import java.util.ArrayList;

/**
 * This class extends ParameterSetupDialog class, including a spectraPlot. This is used to preview
 * how the selected mass detector and his parameters works over the raw data file.
 */
public class MassDetectorSetupDialog extends ParameterSetupDialogWithScanPreview {

  protected MassDetector massDetector;
  protected ParameterSet parameters;

  /**
   * @param parameters
   */
  public MassDetectorSetupDialog(boolean valueCheckRequired, Class<?> massDetectorClass,
      ParameterSet parameters) {

    super(valueCheckRequired, parameters);

    this.parameters = parameters;

    for (MassDetector detector : MassDetectionParameters.massDetectors) {
      if (detector.getClass().equals(massDetectorClass)) {
        this.massDetector = detector;
      }
    }
  }

  @Override
  protected void loadPreview(SpectraPlot spectrumPlot, Scan previewScan) {

    ScanDataSet spectraDataSet = new ScanDataSet(previewScan);

    // Set plot mode only if it hasn't been set before
    // if the scan is centroided, switch to centroid mode
    spectrumPlot.setPlotMode(SpectrumPlotType.fromScan(previewScan));

    spectrumPlot.removeAllDataSets();
    spectrumPlot.addDataSet(spectraDataSet, previewScan.getDataFile().getColorAWT(), false, true);

    // If there is some illegal value, do not load the preview but just exit
    ArrayList<String> errorMessages = new ArrayList<String>();
    boolean paramsOK = parameterSet.checkParameterValues(errorMessages);
    if (!paramsOK) {
      return;
    }

    double[][] mzValues = massDetector.getMassValues(previewScan, parameters);

    MassListDataSet peaksDataSet = new MassListDataSet(mzValues[0], mzValues[1]);

    spectrumPlot.addDataSet(peaksDataSet, SpectraVisualizerTab.peaksColor, false, true);
  }

}
