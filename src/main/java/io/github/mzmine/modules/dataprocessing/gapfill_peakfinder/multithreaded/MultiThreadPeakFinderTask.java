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
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.Gap;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

class MultiThreadPeakFinderTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(MultiThreadPeakFinderTask.class.getName());

  private final ModularFeatureList peakList;
  private final ModularFeatureList processedPeakList;
  private final double intTolerance;
  private final MZTolerance mzTolerance;
  private final RTTolerance rtTolerance;
  private final AtomicInteger processedScans = new AtomicInteger(0);
  // start and end (exclusive) for raw data file processing
  private final int start;
  private final int endexcl;
  private final int taskIndex;
  private final int minDataPoints;
  private int totalScans;

  MultiThreadPeakFinderTask(ModularFeatureList peakList, ModularFeatureList processedPeakList,
      ParameterSet parameters, int start, int endexcl, int taskIndex,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);

    this.taskIndex = taskIndex;

    this.peakList = peakList;
    this.processedPeakList = processedPeakList;

    intTolerance = parameters.getValue(MultiThreadPeakFinderParameters.intTolerance);
    mzTolerance = parameters.getValue(MultiThreadPeakFinderParameters.MZTolerance);
    rtTolerance = parameters.getValue(MultiThreadPeakFinderParameters.RTTolerance);
    minDataPoints = parameters.getValue(MultiThreadPeakFinderParameters.minDataPoints);

    this.start = start;
    this.endexcl = endexcl;
  }

  public void run() {

    setStatus(TaskStatus.PROCESSING);
    logger.info(
        "Running multithreaded gap filler " + taskIndex + " on raw files " + (start + 1) + "-"
        + endexcl + " of pkl:" + peakList);

    int totalDataFiles = endexcl - start;
    // Calculate total number of scans in all files
    for (int i = start; i < endexcl; i++) {
      RawDataFile dataFile = peakList.getRawDataFile(i);
      totalScans += peakList.getSeletedScans(dataFile).size();
    }

    int filled = 0;

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

      List<Gap> gaps = new ArrayList<>();

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

          if (peakList.hasFeatureType(MobilityType.class) && dataFile instanceof IMSRawDataFile) {
            Range<Float> mobilityRange = sourceRow.getMobilityRange();
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
      // Finalize gaps and add to feature list
      for (Gap gap : gaps) {
        if (gap.noMoreOffers(minDataPoints)) {
          filled++;
        }
      }

      // log progress for long running tasks, different levels
      final int processedDataFiles = i - start + 1;
      if (processedDataFiles % 5 == 0) {
        logger.fine(() -> String.format(
            "Multithreaded gap filler (%d): %d of %d raw files processed (%.1f %%)", taskIndex,
            processedDataFiles, totalDataFiles,
            (processedDataFiles / (float) totalDataFiles) * 100));
      } else {
        logger.finest(() -> String.format(
            "Multithreaded gap filler (%d): %d of %d raw files processed (%.1f %%)", taskIndex,
            processedDataFiles, totalDataFiles,
            (processedDataFiles / (float) totalDataFiles) * 100));
      }
    }

    logger.info(String.format(
        "Finished sub task: Multithreaded gap filler %d on raw files %d-%d in feature list %s. (Gaps filled: %d)",
        taskIndex, (start + 1), endexcl, peakList.toString(), filled));
    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Needed high priority so that sub tasks do not wait for free task slot. Main task is also taking
   * one slot. Main task cannot be high because batch mode wont wait for that.
   */
  @Override
  public TaskPriority getTaskPriority() {
    return TaskPriority.HIGH;
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
