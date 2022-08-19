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

package io.github.mzmine.modules.visualization.intensityplot;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureSelectionParameter;
import io.github.mzmine.util.FeatureListRowSorter;
import java.util.Arrays;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.MultiChoiceParameter;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;

public class IntensityPlotParameters extends SimpleParameterSet {

  public static final String rawDataFilesOption = "Raw data file";

  public static final FeatureListsParameter featureList = new FeatureListsParameter(1, 1);

  public static final MultiChoiceParameter<RawDataFile> dataFiles =
      new MultiChoiceParameter<RawDataFile>("Raw data files", "Raw data files to display",
          new RawDataFile[0]);

  public static final ComboParameter<Object> xAxisValueSource =
      new ComboParameter<Object>("X axis value", "X axis value", new Object[] {rawDataFilesOption});

  public static final ComboParameter<YAxisValueSource> yAxisValueSource =
      new ComboParameter<YAxisValueSource>("Y axis value", "Y axis value",
          YAxisValueSource.values());

  public static final FeatureSelectionParameter selectedRows = new FeatureSelectionParameter();

  /**
   * Windows size and position
   */
  public static final WindowSettingsParameter windowSettings = new WindowSettingsParameter();

  public IntensityPlotParameters() {
    super(new Parameter[] {featureList, dataFiles, xAxisValueSource, yAxisValueSource, selectedRows,
        windowSettings},
        "https://mzmine.github.io/mzmine_documentation/visualization_modules/processed_additional/processed_additional.md#feature-intensity-plot");
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {

    FeatureList selectedFeatureLists[] = getParameter(featureList).getValue().getMatchingFeatureLists();
    if (selectedFeatureLists.length > 0) {
      RawDataFile plDataFiles[] =
          selectedFeatureLists[0].getRawDataFiles().toArray(RawDataFile[]::new);
      FeatureListRow plRows[] = selectedFeatureLists[0].getRows().toArray(FeatureListRow[]::new);
      Arrays.sort(plRows, new FeatureListRowSorter(SortingProperty.MZ, SortingDirection.Ascending));
      getParameter(dataFiles).setChoices(plDataFiles);
      getParameter(dataFiles).setValue(plDataFiles);
    }

    return super.showSetupDialog(valueCheckRequired);
  }

}
