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

package io.github.mzmine.modules.visualization.spectra.simplespectra;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.gui.chartbasics.ChartLogicsFX;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

/**
 * Spectrum visualizer
 */
public class SpectraVisualizerModule implements MZmineRunnableModule {

  private static final String MODULE_NAME = "Spectra visualizer";
  private static final String MODULE_DESCRIPTION = "Spectra visualizer."; // TODO

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  @NotNull
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {
    RawDataFile dataFile = parameters.getParameter(SpectraVisualizerParameters.dataFiles).getValue()
        .getMatchingRawDataFiles()[0];

    int scanNumber = parameters.getParameter(SpectraVisualizerParameters.scanNumber).getValue();
    Scan scan = dataFile.getScanAtNumber(scanNumber);
    if (scan == null) {
      assert MZmineCore.getDesktop() != null;
      MZmineCore.getDesktop().displayErrorMessage(
          "Raw data file " + dataFile + " does not contain scan #" + scanNumber + ".");
      return ExitCode.ERROR;
    }

    addNewSpectrumTab(dataFile, scan);
    return ExitCode.OK;
  }

  public static SpectraVisualizerTab addNewSpectrumTab(Scan scan) {
    return addNewSpectrumTab(scan.getDataFile(), scan);
  }

  public static SpectraVisualizerTab addNewSpectrumTab(RawDataFile dataFile, Scan scan) {
    return addNewSpectrumTab(dataFile, scan, null, null, null, null);
  }

  public static SpectraVisualizerTab addNewSpectrumTab(RawDataFile dataFile, Scan scan,
      Feature peak) {
    return addNewSpectrumTab(dataFile, scan, peak, null, null, null);
  }

  public static SpectraVisualizerTab addNewSpectrumTab(RawDataFile dataFile, Scan scan,
      IsotopePattern detectedPattern) {
    return addNewSpectrumTab(dataFile, scan, null, detectedPattern, null, null);
  }

  public static SpectraVisualizerTab addNewSpectrumTab(RawDataFile dataFile, Scan scan,
      Feature peak, IsotopePattern detectedPattern, IsotopePattern predictedPattern) {
    return addNewSpectrumTab(dataFile, scan, peak, detectedPattern, predictedPattern, null);
  }

  public static SpectraVisualizerTab addNewSpectrumTab(RawDataFile dataFile, Scan scan,
      Feature peak, IsotopePattern detectedPattern, IsotopePattern predictedPattern,
      IsotopePattern spectrum) {

    assert Platform.isFxApplicationThread();

    if (scan == null) {
      assert MZmineCore.getDesktop() != null;
      MZmineCore.getDesktop()
          .displayErrorMessage("Raw data file " + dataFile + " does not contain the given scan.");
      return null;
    }

    SpectraVisualizerTab newTab = new SpectraVisualizerTab(dataFile, scan, true);
    newTab.loadRawData(scan);

    if (peak != null) {
      newTab.loadSinglePeak(peak);
    }

    Range<Double> zoomMzRange = null;

    if (detectedPattern != null) {
      newTab.loadIsotopes(detectedPattern);
      zoomMzRange = detectedPattern.getDataPointMZRange();
    }

    if (predictedPattern != null) {
      newTab.loadIsotopes(predictedPattern);
      var otherRange = predictedPattern.getDataPointMZRange();
      zoomMzRange = zoomMzRange == null ? otherRange : zoomMzRange.span(otherRange);
    }

    if (spectrum != null) {
      newTab.loadSpectrum(spectrum);
    }

    if (zoomMzRange != null) {
      // zoom to the isotope pattern
      newTab.getSpectrumPlot().getXYPlot().getDomainAxis()
          .setRange(zoomMzRange.lowerEndpoint() - 3, zoomMzRange.upperEndpoint() + 3);
      ChartLogicsFX.autoRangeAxis(newTab.getSpectrumPlot());
    }
    MZmineCore.getDesktop().addTab(newTab);

    return newTab;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.VISUALIZATIONRAWDATA;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return SpectraVisualizerParameters.class;
  }

}
