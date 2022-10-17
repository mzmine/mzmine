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

package io.github.mzmine.modules.dataprocessing.featdet_manual;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import org.jetbrains.annotations.NotNull;

import com.google.common.collect.Range;

import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.ExitCode;

public class ManualFeaturePickerModule implements MZmineModule {

  /**
   * @see io.github.mzmine.modules.MZmineProcessingModule#getName()
   */
  public @NotNull String getName() {
    return "Manual feature detector";
  }

  public static ExitCode runManualDetection(RawDataFile dataFile, FeatureListRow featureListRow,
      FeatureList featureList, FeatureTableFX table) {
    return runManualDetection(new RawDataFile[] {dataFile}, featureListRow, featureList, table);
  }

  public static ExitCode runManualDetection(RawDataFile dataFiles[], FeatureListRow featureListRow,
      FeatureList featureList, FeatureTableFX table) {

    Range<Double> mzRange = null;
    Range<Float> rtRange = null;

    // Check the features for selected data files
    for (RawDataFile dataFile : dataFiles) {
      Feature feature = featureListRow.getFeature(dataFile);
      if (feature == null)
        continue;
      if ((mzRange == null) || (rtRange == null)) {
        mzRange = feature.getRawDataPointsMZRange();
        rtRange = feature.getRawDataPointsRTRange();
      } else {
        mzRange = mzRange.span(feature.getRawDataPointsMZRange());
        rtRange = rtRange.span(feature.getRawDataPointsRTRange());
      }

    }

    // If none of the data files had a feature, check the whole row
    if (mzRange == null) {
      for (Feature feature : featureListRow.getFeatures()) {
        if (feature == null)
          continue;
        if ((mzRange == null) || (rtRange == null)) {
          mzRange = feature.getRawDataPointsMZRange();
          rtRange = feature.getRawDataPointsRTRange();
        } else {
          mzRange = mzRange.span(feature.getRawDataPointsMZRange());
          rtRange = rtRange.span(feature.getRawDataPointsRTRange());
        }

      }
    }

    ManualPickerParameters parameters = new ManualPickerParameters();

    if (mzRange != null) {
      // TODO: retentionTimeRange parameter to float range
      parameters.getParameter(ManualPickerParameters.retentionTimeRange).setValue(Range.closed(rtRange.lowerEndpoint().doubleValue(), rtRange.upperEndpoint().doubleValue()));
      parameters.getParameter(ManualPickerParameters.mzRange).setValue(mzRange);
    }

    ExitCode exitCode = parameters.showSetupDialog(true);

    if (exitCode != ExitCode.OK)
      return exitCode;

    ManualPickerTask task = new ManualPickerTask(MZmineCore.getProjectManager().getCurrentProject(),
        featureListRow, dataFiles, parameters, featureList);

    MZmineCore.getTaskController().addTask(task);
    return exitCode;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return ManualPickerParameters.class;
  }

}
