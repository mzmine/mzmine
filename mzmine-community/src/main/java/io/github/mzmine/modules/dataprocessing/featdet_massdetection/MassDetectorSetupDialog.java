/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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
import io.github.mzmine.javafx.mvci.LatestTaskScheduler;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectrumPlotType;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.ScanDataSet;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialogWithScanPreview;
import java.util.ArrayList;
import javafx.util.Duration;
import org.jetbrains.annotations.Nullable;

/**
 * This class extends ParameterSetupDialog class, including a spectraPlot. This is used to preview
 * how the selected mass detector and his parameters works over the raw data file.
 */
public class MassDetectorSetupDialog extends ParameterSetupDialogWithScanPreview {

  /**
   * Runs the mass detection off the GUI thread, only the latest preview updates the plot
   */
  private final LatestTaskScheduler previewScheduler = new LatestTaskScheduler();
  /**
   * Detector of the last preview, hands its per file cache to the next detector
   */
  private @Nullable MassDetector lastDetector;

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

    // If there is some illegal value, show no centroids. The empty task still replaces older
    // previews, so they do not add their centroids.
    ArrayList<String> errorMessages = new ArrayList<String>();
    boolean paramsOK = parameterSet.checkParameterValues(errorMessages, true);
    MassDetector massDetector = null;
    if (paramsOK) {
      var detector = parameterSet.getParameter(MassDetectionParameters.massDetector)
          .getValueWithParameters();
      massDetector = detector.value().createMassDetector(detector.parameters());
    }
    final MassDetector previousDetector = lastDetector;
    if (massDetector != null) {
      lastDetector = massDetector;
    }

    // decision: detection off the GUI thread, some detectors scan the whole raw file first (e.g.,
    // auto saturation threshold). The delay accumulates fast parameter changes.
    previewScheduler.onTaskThreadDelayed(
        new MassDetectorPreviewUpdateTask(spectrumPlot, massDetector, previousDetector,
            previewScan), Duration.millis(100));
  }
}
