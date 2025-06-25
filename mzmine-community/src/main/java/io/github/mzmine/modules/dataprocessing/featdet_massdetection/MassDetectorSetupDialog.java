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

  /**
   * @param mainParams
   */
  public MassDetectorSetupDialog(boolean valueCheckRequired, ParameterSet mainParams) {
    super(valueCheckRequired, mainParams);
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

    var detector = parameterSet.getParameter(MassDetectionParameters.massDetector)
        .getValueWithParameters();
    MassDetector massDetector = detector.value().createMassDetector(detector.parameters());

    double[][] mzValues = massDetector.getMassValues(previewScan);

    MassListDataSet peaksDataSet = new MassListDataSet(mzValues[0], mzValues[1]);

    spectrumPlot.addDataSet(peaksDataSet, SpectraVisualizerTab.peaksColor, false, true);
  }

}
