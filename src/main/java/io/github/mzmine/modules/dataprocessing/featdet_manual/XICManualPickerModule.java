/*
 *  Copyright 2006-2022 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.dataprocessing.featdet_manual;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.RangeUtils;
import org.jetbrains.annotations.NotNull;

public class XICManualPickerModule implements MZmineModule {

  /**
   * @see io.github.mzmine.modules.MZmineProcessingModule#getName()
   */
  public @NotNull String getName() {
    return "XIC Manual feature detector";
  }

  // public static ExitCode runManualDetection(RawDataFile dataFile,
  // PeakListRow peakListRow,
  // PeakList peakList, PeakListTable table) {
  // return runManualDetection(new RawDataFile[] {dataFile}, peakListRow,
  // peakList, table);
  // }

  public static ExitCode runManualDetection(RawDataFile dataFile, FeatureListRow featureListRow,
      FeatureList featureList) {

    Range<Double> mzRange = null;
    Range<Float> rtRange = null;
    mzRange = Range.closed(featureListRow.getAverageMZ(), featureListRow.getAverageMZ());
    rtRange = Range.closed(featureListRow.getAverageRT(), featureListRow.getAverageRT());

    for (Feature feature : featureListRow.getFeatures()) {
      if (feature == null) {
        continue;
      }
      // if the feature exists in the file, then we just use that one as a
      // base
      if (feature.getRawDataFile() == dataFile) {
        mzRange = feature.getRawDataPointsMZRange();
        rtRange = feature.getRawDataPointsRTRange();
        break;
      }
      // if it does not exist, we set up on basis of the other features
      if (feature != null) {
        mzRange = mzRange.span(feature.getRawDataPointsMZRange());
        rtRange = rtRange.span(feature.getRawDataPointsRTRange());
      }
    }

    XICManualPickerParameters parameters = new XICManualPickerParameters();

    if (mzRange != null) {
      // TODO: FloatRangeParameter
      parameters.getParameter(XICManualPickerParameters.rtRange)
          .setValue(RangeUtils.toDoubleRange(rtRange));
      parameters.getParameter(XICManualPickerParameters.mzRange).setValue(mzRange);
    }
    if (dataFile != null) {
      RawDataFilesSelection selection = new RawDataFilesSelection();
      selection.setSelectionType(RawDataFilesSelectionType.SPECIFIC_FILES);
      selection.setSpecificFiles(new RawDataFile[]{dataFile});
      parameters.getParameter(XICManualPickerParameters.rawDataFiles).setValue(selection);
    }
    parameters.getParameter(XICManualPickerParameters.flists)
        .setValue(new FeatureListsSelection((ModularFeatureList) featureList));

    ExitCode exitCode = parameters.showSetupDialog(true);

    if (exitCode != ExitCode.OK) {
      return exitCode;
    }

    ParameterSet param = new ManualPickerParameters().cloneParameterSet();
    param.getParameter(ManualPickerParameters.mzRange)
        .setValue(parameters.getParameter(XICManualPickerParameters.mzRange).getValue());
    param.getParameter(ManualPickerParameters.retentionTimeRange)
        .setValue(parameters.getParameter(XICManualPickerParameters.rtRange).getValue());

    ManualPickerTask task = new ManualPickerTask(MZmineCore.getProjectManager().getCurrentProject(),
        featureListRow, new RawDataFile[]{dataFile}, param, featureList);

    MZmineCore.getTaskController().addTask(task);
    return exitCode;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return ManualPickerParameters.class;
  }

}
