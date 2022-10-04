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

package io.github.mzmine.modules.visualization.histogram;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.MultiChoiceParameter;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.util.ExitCode;

public class HistogramParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureList = new FeatureListsParameter(1, 1);

  public static final MultiChoiceParameter<RawDataFile> dataFiles =
      new MultiChoiceParameter<RawDataFile>("Raw data files", "Column of features to be plotted",
          new RawDataFile[0]);

  public static final HistogramRangeParameter dataRange = new HistogramRangeParameter();

  public static final IntegerParameter numOfBins =
      new IntegerParameter("Number of bins", "The plot is divides into this number of bins", 10);

  /**
   * Windows size and position
   */
  public static final WindowSettingsParameter windowSettings = new WindowSettingsParameter();

  public HistogramParameters() {
    super(new Parameter[] {featureList, dataFiles, dataRange, numOfBins, windowSettings},
        "https://mzmine.github.io/mzmine_documentation/visualization_modules/feature_histograms/feature_hist#feature-list-histogram");
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    FeatureList selectedFeatureLists[] =
        getParameter(HistogramParameters.featureList).getValue().getMatchingFeatureLists();
    RawDataFile dataFiles[];
    if ((selectedFeatureLists == null) || (selectedFeatureLists.length != 1)) {
      dataFiles = MZmineCore.getProjectManager().getCurrentProject().getDataFiles();
    } else {
      dataFiles = selectedFeatureLists[0].getRawDataFiles().toArray(RawDataFile[]::new);
    }
    getParameter(HistogramParameters.dataFiles).setChoices(dataFiles);
    return super.showSetupDialog(valueCheckRequired);
  }

}
