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

package io.github.mzmine.modules.dataprocessing.gapfill_samerange;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.scans.ScanUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class SameRangeTask extends AbstractTask {

  private final static Logger logger = Logger.getLogger(SameRangeTask.class.getName());

  private final MZmineProject project;
  private final OriginalFeatureListOption handleOriginal;
  private final ModularFeatureList peakList;
  private ModularFeatureList processedPeakList;

  private final String suffix;
  private final MZTolerance mzTolerance;

  private int totalRows;
  private final AtomicInteger processedRowsAtomic = new AtomicInteger(0);

  private final ParameterSet parameters;

  SameRangeTask(MZmineProject project, FeatureList peakList, ParameterSet parameters,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.peakList = (ModularFeatureList) peakList;
    this.parameters = parameters;

    suffix = parameters.getParameter(SameRangeGapFillerParameters.suffix).getValue();
    mzTolerance = parameters.getParameter(SameRangeGapFillerParameters.mzTolerance).getValue();
    handleOriginal = parameters.getParameter(SameRangeGapFillerParameters.handleOriginal)
        .getValue();

  }

  @Override
  public void run() {

    logger.info("Started gap-filling " + peakList);

    setStatus(TaskStatus.PROCESSING);

    // Get total number of rows
    totalRows = peakList.getNumberOfRows();

    // Get feature list columns
    RawDataFile[] columns = peakList.getRawDataFiles().toArray(RawDataFile[]::new);

    // Create new feature list
    processedPeakList = new ModularFeatureList(peakList + " " + suffix, getMemoryMapStorage(),
        columns);

    List<FeatureListRow> outputList = Collections.synchronizedList(new ArrayList<>());

    peakList.stream().map(r -> (ModularFeatureListRow) r).forEach(sourceRow -> {
      // Canceled?
      if (isCanceled()) {
        return;
      }

      FeatureListRow newRow = new ModularFeatureListRow(processedPeakList, sourceRow.getID(),
          sourceRow, true);

      // Copy each peaks and fill gaps
      for (RawDataFile column : columns) {
        // Canceled?
        if (isCanceled()) {
          return;
        }

        // Get current peak
        Feature currentPeak = sourceRow.getFeature(column);

        // If there is a gap, try to fill it
        if (currentPeak == null || currentPeak.getFeatureStatus().equals(FeatureStatus.UNKNOWN)) {
          currentPeak = fillGap(sourceRow, column);
        }
      }

      outputList.add(newRow);

      processedRowsAtomic.getAndAdd(1);
    });

    outputList.forEach(newRow -> {
      processedPeakList.addRow(newRow);
    });

    /* End Parallel Implementation */

    // Canceled?
    if (isCanceled()) {
      return;
    }
    // Append processed feature list to the project
    handleOriginal.reflectNewFeatureListToProject(suffix, project, processedPeakList, peakList);

    // Add quality parameters to peaks
    //QualityParameters.calculateQualityParameters(processedPeakList);

    // Add task description to peakList
    processedPeakList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("Gap filling using RT and m/z range",
            SameRangeGapFillerModule.class, parameters, getModuleCallDate()));

    setStatus(TaskStatus.FINISHED);

    logger.info("Finished gap-filling " + peakList);

  }

  private Feature fillGap(FeatureListRow row, RawDataFile column) {

    Range<Double> mzRange = null;
    Range<Float> rtRange = null;

    // Check the peaks for selected data files
    for (RawDataFile dataFile : row.getRawDataFiles()) {
      Feature peak = row.getFeature(dataFile);
      if (peak == null) {
        continue;
      }
      if ((mzRange == null) || (rtRange == null)) {
        mzRange = peak.getRawDataPointsMZRange();
        rtRange = peak.getRawDataPointsRTRange();
      } else {
        mzRange = mzRange.span(peak.getRawDataPointsMZRange());
        rtRange = rtRange.span(peak.getRawDataPointsRTRange());
      }
    }

    assert mzRange != null;
    assert rtRange != null;

    Range<Double> mzRangeWithTol = mzTolerance.getToleranceRange(mzRange);
    final double centerMZ = RangeUtils.rangeCenter(mzRangeWithTol);

    // Get scan numbers
    Scan[] scanNumbers = column.getScanNumbers(1, rtRange);

    boolean dataPointFound = false;

    // results
    SimpleGapFeature gapFeature = new SimpleGapFeature();

    for (Scan scan : scanNumbers) {

      if (isCanceled()) {
        return null;
      }

      // Find most intense m/z peak
      DataPoint basePeak = ScanUtils.findBasePeak(scan, mzRangeWithTol);

      if (basePeak != null) {
        if (basePeak.getIntensity() > 0) {
          dataPointFound = true;
        }
        gapFeature.addDataPoint(scan, basePeak);
      } else {
        DataPoint fakeDataPoint = new SimpleDataPoint(centerMZ, 0);
        gapFeature.addDataPoint(scan, fakeDataPoint);
      }

    }

    if (dataPointFound) {
      gapFeature.removeEdgeZeroIntensities();
      if (gapFeature.isEmpty()) {
        return null;
      }
      final ModularFeature feature = new ModularFeature(processedPeakList, column,
          gapFeature.toIonTimeSeries(processedPeakList.getMemoryMapStorage()),
          FeatureStatus.ESTIMATED);

      var allMS2 = ScanUtils.streamAllMS2FragmentScans(column, rtRange, mzRange).toList();
      feature.setAllMS2FragmentScans(allMS2);
      return feature;
    }

    return null;
  }

  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0;
    }
    return (double) processedRowsAtomic.get() / (double) totalRows;

  }

  @Override
  public String getTaskDescription() {
    return "Gap filling " + peakList + " using RT and m/z range";
  }

}
