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

package io.github.mzmine.modules.visualization.chromatogram;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeaturesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.util.ExitCode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TICVisualizerParameters extends SimpleParameterSet {

  /**
   * The data file.
   */
  public static final RawDataFilesParameter DATA_FILES = new RawDataFilesParameter();

  /**
   * Scans (used to be RT range).
   */
  public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter(
      new ScanSelection(1));

  /**
   * Type of plot.
   */
  public static final ComboParameter<TICPlotType> PLOT_TYPE = new ComboParameter<TICPlotType>(
      "Plot type", "Type of Y value calculation (TIC = sum, base peak = max)",
      TICPlotType.values());

  /**
   * m/z range.
   */
  public static final MZRangeParameter MZ_RANGE = new MZRangeParameter();

  public static final OptionalParameter<IntegerParameter> ticMaxSamples = new OptionalParameter<>(
      new IntegerParameter("Omit EIC lines >n samples",
          "Removes the sample traces (lines) above n samples. Then only feature shapes are drawn. Put to 0 to exclude sample lines in all cases.",
          20, true), false);

  /**
   * Peaks to display.
   */
  public static final FeaturesParameter PEAKS = new FeaturesParameter();

  // Maps peaks to their labels - not a user configurable parameter.
  private Map<Feature, String> peakLabelMap;

  /**
   * Windows size and position
   */
  public static final WindowSettingsParameter WINDOWSETTINGSPARAMETER = new WindowSettingsParameter();

  /**
   * Create the parameter set.
   */
  public TICVisualizerParameters() {
    super(new Parameter[]{DATA_FILES, scanSelection, PLOT_TYPE, ticMaxSamples, MZ_RANGE, PEAKS,
            WINDOWSETTINGSPARAMETER},
        "https://mzmine.github.io/mzmine_documentation/visualization_modules/raw_data_overview/raw_data_additional.html#chromatogram-plot");
    peakLabelMap = null;
  }

  /**
   * Gets the peak labels map.
   *
   * @return the map.
   */
  public Map<Feature, String> getPeakLabelMap() {

    return peakLabelMap == null ? null : new HashMap<Feature, String>(peakLabelMap);
  }

  /**
   * Sets the peak labels map.
   *
   * @param map the new map.
   */
  public void setPeakLabelMap(final Map<Feature, String> map) {

    peakLabelMap = map == null ? null : new HashMap<Feature, String>(map);
  }

  /**
   * Show the setup dialog.
   *
   * @param allFiles      files to choose from.
   * @param selectedFiles default file selections.
   * @param allPeaks      peaks to choose from.
   * @param selectedPeaks default peak selections.
   * @return an ExitCode indicating the user's action.
   */
  public ExitCode showSetupDialog(boolean valueCheckRequired, final RawDataFile[] allFiles,
      final RawDataFile[] selectedFiles, final Feature[] allPeaks, final Feature[] selectedPeaks) {

    getParameter(DATA_FILES).setValue(RawDataFilesSelectionType.SPECIFIC_FILES, selectedFiles);
    // getParameter(PEAKS).setChoices(allPeaks);
    List<Feature> selectedFeatures = Arrays.asList(allPeaks);
    getParameter(PEAKS).setValue(selectedFeatures);
    return super.showSetupDialog(valueCheckRequired);
  }
}
