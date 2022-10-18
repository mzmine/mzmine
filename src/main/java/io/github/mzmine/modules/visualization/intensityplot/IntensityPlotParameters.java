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

package io.github.mzmine.modules.visualization.intensityplot;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureSelectionParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import java.util.Arrays;

public class IntensityPlotParameters extends SimpleParameterSet {

  public static final String rawDataFilesOption = "Raw data file";

  public static final FeatureListsParameter featureList = new FeatureListsParameter(1, 1);

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  public static final ComboParameter<Object> xAxisValueSource = new ComboParameter<>("X axis value",
      "X axis value", new Object[]{rawDataFilesOption});

  public static final ComboParameter<YAxisValueSource> yAxisValueSource = new ComboParameter<>(
      "Y axis value", "Y axis value", YAxisValueSource.values());

  public static final FeatureSelectionParameter selectedRows = new FeatureSelectionParameter();

  /**
   * Windows size and position
   */
  public static final WindowSettingsParameter windowSettings = new WindowSettingsParameter();

  public IntensityPlotParameters() {
    super(new Parameter[]{featureList, dataFiles, xAxisValueSource, yAxisValueSource, selectedRows,
            windowSettings},
        "https://mzmine.github.io/mzmine_documentation/visualization_modules/processed_additional/processed_additional.html#feature-intensity-plot");
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {

    FeatureList[] selectedFeatureLists = getParameter(featureList).getValue()
        .getMatchingFeatureLists();
    if (selectedFeatureLists.length > 0) {
      RawDataFile[] plDataFiles = selectedFeatureLists[0].getRawDataFiles()
          .toArray(RawDataFile[]::new);
      FeatureListRow[] plRows = selectedFeatureLists[0].getRows().toArray(FeatureListRow[]::new);
      Arrays.sort(plRows, new FeatureListRowSorter(SortingProperty.MZ, SortingDirection.Ascending));

    }

    return super.showSetupDialog(valueCheckRequired);
  }

}
