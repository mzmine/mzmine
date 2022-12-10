/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.featdet_shoulderpeaksfilter;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerTab;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectrumPlotType;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.DataPointsDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.ScanDataSet;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialogWithScanPreview;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

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
    spectrumPlot.addDataSet(scanDataSet, SpectraVisualizerTab.scanColor, false, true);

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

    spectrumPlot.addDataSet(removedPeaksDataSet, negativeColor, false, true);
    spectrumPlot.addDataSet(remainingPeaksDataSet, positiveColor, false, true);

  }

}
