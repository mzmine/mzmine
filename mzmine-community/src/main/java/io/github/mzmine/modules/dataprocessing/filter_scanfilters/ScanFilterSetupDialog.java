/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_scanfilters;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerTab;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectrumPlotType;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.ScanDataSet;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialogWithScanPreview;
import java.awt.Color;

/**
 * This class extends ParameterSetupDialog class, including a spectraPlot. This is used to preview
 * how the selected raw data filter and his parameters works over the raw data file.
 */
public class ScanFilterSetupDialog extends ParameterSetupDialogWithScanPreview {

  private ParameterSet filterParameters;
  private ScanFilter rawDataFilter;
  private RawDataFile tmpFile;

  /**
   *
   */
  public ScanFilterSetupDialog(boolean valueCheckRequired, ParameterSet filterParameters,
      Class<? extends ScanFilter> filterClass) {

    super(valueCheckRequired, filterParameters);
    this.filterParameters = filterParameters;

    try {
      this.rawDataFilter = filterClass.getDeclaredConstructor().newInstance();
      this.tmpFile = MZmineCore.createNewFile("tmp", null, null);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * This function set all the information into the plot chart
   *
   * @param
   */
  @Override
  protected void loadPreview(SpectraPlot spectrumPlot, Scan previewScan) {

    Scan newScan = rawDataFilter.filterScan(tmpFile, previewScan);

    ScanDataSet spectraDataSet = new ScanDataSet("Filtered scan", newScan);
    ScanDataSet spectraOriginalDataSet = new ScanDataSet("Original scan", previewScan);

    spectrumPlot.removeAllDataSets();

    spectrumPlot.addDataSet(spectraOriginalDataSet, SpectraVisualizerTab.scanColor, true, true);
    spectrumPlot.addDataSet(spectraDataSet, Color.green, true, true);

    // if the scan is centroided, switch to centroid mode
    spectrumPlot.setPlotMode(SpectrumPlotType.fromScan(previewScan));

  }
}
