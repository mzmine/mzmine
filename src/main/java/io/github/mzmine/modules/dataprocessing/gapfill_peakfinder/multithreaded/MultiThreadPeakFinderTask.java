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

package io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.multithreaded;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.MobilityScanDataType;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.data_access.MobilityScanDataAccess;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.Gap;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.IonMobilityUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

class MultiThreadPeakFinderTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private ModularFeatureList peakList, processedPeakList;
  private double intTolerance;
  private MZTolerance mzTolerance;
  private RTTolerance rtTolerance;
  private int totalScans;
  private AtomicInteger processedScans = new AtomicInteger(0);

  // start and end (exclusive) for raw data file processing
  private int start;
  private int endexcl;

  private int taskIndex;

  MultiThreadPeakFinderTask(ModularFeatureList peakList, ModularFeatureList processedPeakList,
      ParameterSet parameters, int start, int endexcl, int taskIndex, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);

    this.taskIndex = taskIndex;

    this.peakList = peakList;
    this.processedPeakList = processedPeakList;

    intTolerance = parameters.getParameter(MultiThreadPeakFinderParameters.intTolerance).getValue();
    mzTolerance = parameters.getParameter(MultiThreadPeakFinderParameters.MZTolerance).getValue();
    rtTolerance = parameters.getParameter(MultiThreadPeakFinderParameters.RTTolerance).getValue();

    this.start = start;
    this.endexcl = endexcl;
  }

  public void run() {

    setStatus(TaskStatus.PROCESSING);
    logger.info(
        "Running multithreaded gap filler " + taskIndex + " on raw files " + (start + 1) + "-"
        + endexcl + " of pkl:" + peakList);

    // Calculate total number of scans in all files
    for (int i = start; i < endexcl; i++) {
      RawDataFile dataFile = peakList.getRawDataFile(i);
      totalScans += peakList.getSeletedScans(dataFile).size();
    }

    // Process all raw data files
    for (int i = start; i < endexcl; i++) {
      RawDataFile dataFile = peakList.getRawDataFile(i);
      final BinningMobilogramDataAccess mobilogramAccess = // todo how to determine previous bin width for an aligned list?
          dataFile instanceof IMSRawDataFile ? EfficientDataAccess.of((IMSRawDataFile) dataFile,
              BinningMobilogramDataAccess.getRecommendedBinWidth((IMSRawDataFile) dataFile)) : null;

      // Canceled?
      if (isCanceled()) {
        return;
      }

      List<Gap> gaps = new ArrayList<Gap>();

      // Fill each row of this raw data file column, create new empty
      // gaps
      // if necessary
      for (int row = 0; row < peakList.getNumberOfRows(); row++) {
        FeatureListRow sourceRow = peakList.getRow(row);
        FeatureListRow newRow = processedPeakList.getRow(row);

        Feature sourcePeak = sourceRow.getFeature(dataFile);

        if (sourcePeak == null || sourcePeak.getFeatureStatus().equals(FeatureStatus.UNKNOWN)) {
          // Create a new gap
          Range<Double> mzRange = mzTolerance.getToleranceRange(sourceRow.getAverageMZ());
          Range<Float> rtRange = rtTolerance.getToleranceRange(sourceRow.getAverageRT());
          Range<Float> mobilityRange = IonMobilityUtils
              .getRowMobilityrange((ModularFeatureListRow) sourceRow);

          if (peakList.hasFeatureType(MobilityType.class) && dataFile instanceof IMSRawDataFile) {
            Gap newGap = new ImsGap(newRow, dataFile, mzRange, rtRange, mobilityRange, intTolerance,
                mobilogramAccess);
            gaps.add(newGap);
          } else {
            Gap newGap = new Gap(newRow, dataFile, mzRange, rtRange, intTolerance);
            gaps.add(newGap);
          }
        }
      }

      // Stop processing this file if there are no gaps
      if (gaps.isEmpty()) {
        processedScans.addAndGet(dataFile.getNumOfScans());
        continue;
      }

      // Get all scans of this data file
      processFile(dataFile, gaps);

      if (isCanceled()) {
        return;
      }

      // Finalize gaps
      for (Gap gap : gaps) {
        gap.noMoreOffers();
      }
    }

    logger.info(
        "Finished sub task: Multithreaded gap filler " + taskIndex + " on raw files " + (start + 1)
        + "-" + endexcl + " of pkl:" + peakList);
    setStatus(TaskStatus.FINISHED);
  }

  public double getFinishedPercentage() {
    if (totalScans == 0) {
      return 0;
    }
    return (double) processedScans.get() / (double) totalScans;
  }

  public String getTaskDescription() {
    return "Sub task " + taskIndex + ": Gap filling on raw files " + (start + 1) + "-" + endexcl
           + " of pkl:" + peakList;
  }

  FeatureList getPeakList() {
    return peakList;
  }

  private void processFile(RawDataFile file, List<Gap> gaps) {
    if (file instanceof IMSRawDataFile imsFile && peakList.hasFeatureType(MobilityType.class)) {
      final MobilityScanDataAccess access = new MobilityScanDataAccess(imsFile,
          MobilityScanDataType.CENTROID, (List<Frame>) peakList.getSeletedScans(file));
      List<ImsGap> imsGaps = (List<ImsGap>) (List<? extends Gap>) gaps;

      while (access.hasNextFrame()) {
        if (isCanceled()) {
          return;
        }

        final Frame frame = access.nextFrame();
        for (ImsGap gap : imsGaps) {
          access.resetMobilityScan();
          gap.offerNextScan(access);
        }
        processedScans.incrementAndGet();
      }

    } else {
      // no IMS dimension

      final ScanDataAccess scanAccess = EfficientDataAccess.of(file, ScanDataType.CENTROID,
          peakList.getSeletedScans(file));
      while (scanAccess.hasNextScan()) {
        if (isCanceled()) {
          return;
        }
        scanAccess.nextScan();
        // Feed this scan to all gaps
        for (Gap gap : gaps) {
          gap.offerNextScan(scanAccess);
        }

        processedScans.incrementAndGet();
      }
    }
  }
}
