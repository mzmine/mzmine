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

package io.github.mzmine.modules.dataprocessing.featdet_shoulderpeaksfilter;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerTab;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectrumPlotType;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.DataPointsDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.ScanDataSet;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialogWithScanPreview;

/**
 * This class extends ParameterSetupDialog class, including a spectraPlot. This is used to preview
 * how the selected mass detector and his parameters works over the raw data file.
 */
public class ShoulderPeaksFilterSetupDialog extends ParameterSetupDialogWithScanPreview {

  private static final Color removedPeaksColor = Color.orange;

  private ParameterSet parameters;

  /**
   * @param parameters
   */
  public ShoulderPeaksFilterSetupDialog(boolean valueCheckRequired, ParameterSet parameters) {
    super(valueCheckRequired, parameters);
    this.parameters = parameters;
  }

  /**
   * This function set all the information into the plot chart
   */
  @Override
  protected void loadPreview(SpectraPlot spectrumPlot, Scan previewScan) {

    // Remove previous data sets
    spectrumPlot.removeAllDataSets();

    // Add scan data set
    ScanDataSet scanDataSet = new ScanDataSet(previewScan);
    spectrumPlot.addDataSet(scanDataSet, SpectraVisualizerTab.scanColor, false);

    // If the scan is centroided, switch to centroid mode
    spectrumPlot.setPlotMode(SpectrumPlotType.fromScan(previewScan));

    // If the parameters are not complete, exit
    ArrayList<String> errors = new ArrayList<String>();
    boolean paramsOK = parameters.checkParameterValues(errors);
    if (!paramsOK) {
      return;
    }

    // Get mass list
    MassList massList = previewScan.getMassList();
    if (massList == null) {
      return;
    }

    // Perform filtering
    DataPoint mzValues[] = massList.getDataPoints();
    DataPoint remainingMzValues[] = ShoulderPeaksFilter.filterMassValues(mzValues, parameters);

    Vector<DataPoint> removedPeaks = new Vector<DataPoint>();
    removedPeaks.addAll(Arrays.asList(mzValues));
    removedPeaks.removeAll(Arrays.asList(remainingMzValues));
    DataPoint removedMzValues[] = removedPeaks.toArray(new DataPoint[0]);

    // Add mass list data sets
    DataPointsDataSet removedPeaksDataSet = new DataPointsDataSet("Removed peaks", removedMzValues);
    DataPointsDataSet remainingPeaksDataSet = new DataPointsDataSet("Remaining peaks",
        remainingMzValues);

    final Color positiveColor = MZmineCore.getConfiguration().getDefaultColorPalette()
        .getPositiveColorAWT();
    final Color negativeColor = MZmineCore.getConfiguration().getDefaultColorPalette()
        .getNegativeColorAWT();

    spectrumPlot.addDataSet(removedPeaksDataSet, negativeColor, false);
    spectrumPlot.addDataSet(remainingPeaksDataSet, positiveColor, false);

  }

}
