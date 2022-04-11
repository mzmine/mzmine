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
        featureListRow, dataFiles, parameters, featureList, table);

    MZmineCore.getTaskController().addTask(task);
    return exitCode;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return ManualPickerParameters.class;
  }

}
