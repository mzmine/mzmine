/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_imsexpander;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.MobilityScanDataType;
import io.github.mzmine.datamodel.data_access.MobilityScanDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import io.github.mzmine.util.exceptions.MissingMassListException;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ImsExpanderSubTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(ImsExpanderSubTask.class.getName());

  protected final ParameterSet parameters;
  private final List<Frame> frames;
  private final ModularFeatureList flist;
  private List<ExpandingTrace> expandingTraces;

  private final AtomicInteger processedFrames = new AtomicInteger(0);
  private final Boolean useRawData;
  private final Double customNoiseLevel;
  private final Range<Double> traceMzRange;

  private long totalFrames = 1;
  private String desc;
  private final BinningMobilogramDataAccess mobilogramDataAccess;
  private final List<ExpandedTrace> expandedTraces;
  private final IMSRawDataFile imsFile;
  private double createdRows = 0;

  public ImsExpanderSubTask(@Nullable final MemoryMapStorage storage,
      @NotNull final ParameterSet parameters, @NotNull final List<Frame> frames,
      @NotNull final ModularFeatureList flist, @NotNull final List<ExpandingTrace> expandingTraces,
      BinningMobilogramDataAccess mobilogramDataAccess, IMSRawDataFile imsFile) {
    super(storage, Instant.now()); // just a subtask, date irrelevant
    this.parameters = parameters;
    this.frames = frames;
    this.flist = flist;
    this.expandingTraces = expandingTraces;
    this.useRawData = parameters.getParameter(ImsExpanderParameters.useRawData).getValue();
    this.customNoiseLevel = parameters.getParameter(ImsExpanderParameters.useRawData)
        .getEmbeddedParameter().getValue();
    traceMzRange = expandingTraces.size() > 0 ? Range.closed(
        expandingTraces.get(0).getMzRange().lowerEndpoint(),
        expandingTraces.get(expandingTraces.size() - 1).getMzRange().upperEndpoint())
        : Range.singleton(0d);

    totalFrames = frames.size();
    desc = flist.getName() + ": expanding traces for frame " + processedFrames.get() + "/"
        + totalFrames + " m/z range: " + RangeUtils.formatRange(traceMzRange,
        MZmineCore.getConfiguration().getMZFormat());
    this.mobilogramDataAccess = mobilogramDataAccess;
    expandedTraces = new ArrayList<>(expandingTraces.size());
    this.imsFile = imsFile;
  }

  @Override
  public String getTaskDescription() {
    return desc;
  }

  @Override
  public double getFinishedPercentage() {
    if (expandingTraces == null) {
      return 1.0d;
    }
    return (processedFrames.get() / (double) totalFrames) * 0.5
        + createdRows / (double) expandingTraces.size() * 0.5;
  }

  // does not work, but why?
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.finest("Initialising data access for file " + imsFile.getName());
    final MobilityScanDataAccess access = new MobilityScanDataAccess(imsFile,
        useRawData ? MobilityScanDataType.RAW : MobilityScanDataType.MASS_LIST, frames);

    final NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();

    final int numTraces = expandingTraces.size();
    try {

      for (int i = 0; i < access.getNumberOfScans(); i++) {
        if (isCanceled()) {
          // allow traces to be released
          expandingTraces = null;
          return;
        }

        final Frame frame = access.nextFrame();

        while (access.hasNextMobilityScan()) {
          final MobilityScan mobilityScan = access.nextMobilityScan();
          if (access.getNumberOfDataPoints() == 0) {
            continue;
          }

          // memorise the first trace that had an mz range that fit the last mz
          int firstTraceIndex = 0;
          for (int dpIndex = 0;
              dpIndex < access.getNumberOfDataPoints() && firstTraceIndex < numTraces; dpIndex++) {

            final double mz = access.getMzValue(dpIndex);
            final double intensity = access.getIntensityValue(dpIndex);

            if (useRawData && intensity < customNoiseLevel) {
              continue;
            }

            // offer the data point to all traces, but only those that have the fitting mz ranges
            for (int traceIndex = firstTraceIndex; traceIndex < expandingTraces.size();
                traceIndex++) {
              final ExpandingTrace trace = expandingTraces.get(traceIndex);
              final Range<Double> traceMzRange = trace.getMzRange();

              if (traceMzRange.lowerEndpoint() > mz) {
                // all further traces have an even higher start-mz
                break;
              }

              // this assumes that the range width at a similar m/z is also of almost equal width.
              // e.g. a feature with m/z 600 and a feature with m/z 601:
              // the range of the feature at m/z 600 may not be wider than the range of the feature at m/z 601
              if (traceMzRange.upperEndpoint() < mz) {
                firstTraceIndex++;
                continue;
              }

              // offer the data point to all traces
              trace.offerDataPoint(access, dpIndex);
            }
          }
        }
        processedFrames.getAndIncrement();

        desc = flist.getName() + ": expanding traces for frame " + processedFrames.get() + "/"
            + totalFrames + " m/z range: " + RangeUtils.formatRange(traceMzRange, mzFormat);
      }
    } catch (MissingMassListException e) {
      // allow traces to be released
      expandingTraces = null;
      e.printStackTrace();
      logger.log(Level.WARNING, e.getMessage(), e);
      setErrorMessage(e.getMessage());
      setStatus(TaskStatus.ERROR);
      return;
    }

    for (var expandingTrace : expandingTraces) {
      desc = "Creating new features " + createdRows + "/" + expandingTraces.size();

      if (expandingTrace.getNumberOfMobilityScans() > 1) {
        final IonMobilogramTimeSeries series = expandingTrace.toIonMobilogramTimeSeries(
            getMemoryMapStorage(), mobilogramDataAccess);
        expandedTraces.add(new ExpandedTrace(series, expandingTrace.getRow(),
            expandingTrace.getRow().getFeature(imsFile)));
      }

      createdRows++;

      if (isCanceled()) {
        // allow traces to be released
        expandingTraces = null;
        return;
      }
    }

    // allow traces to be released
    expandingTraces = null;

    setStatus(TaskStatus.FINISHED);
  }

  // works but 5 times slower
  //  @Override
  public void run2() {
    setStatus(TaskStatus.PROCESSING);
    logger.finest("Initialising data access for file " + imsFile.getName());
    final MobilityScanDataAccess access = new MobilityScanDataAccess(imsFile,
        useRawData ? MobilityScanDataType.RAW : MobilityScanDataType.MASS_LIST, frames);

    final NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();

    final int numTraces = expandingTraces.size();
    try {

      for (int i = 0; i < access.getNumberOfScans(); i++) {
        if (isCanceled()) {
          // allow traces to be released
          expandingTraces = null;
          return;
        }

        final Frame frame = access.nextFrame();

        while (access.hasNextMobilityScan()) {
          final MobilityScan mobilityScan = access.nextMobilityScan();

          if (access.getNumberOfDataPoints() == 0) {
            continue;
          }

          // since traces are sorted by lower mz range, we only need to search for a data point for
          // the next trace between the start index taht was found for the last trace and the max
          // number of data points
          int lastStartIndex = 0;
          for (int traceIndex = 0; traceIndex < numTraces; traceIndex++) {
            final Range<Double> traceMzRange = expandingTraces.get(traceIndex).getMzRange();
            // jump to the closest data point of the trace for each mobility scan
            final double lowerMz = traceMzRange.lowerEndpoint();
            final int start = access.binarySearch(lowerMz, DefaultTo.CLOSEST_VALUE, lastStartIndex,
                access.getNumberOfDataPoints());
            if (start == -1) {
//               traces are sorted by lower mz, if no data point was found that is greater than the
//               current lower mz bound, we can break and go to the next mobility scan
              break;
            }
            lastStartIndex = start;

            for (int dpIndex = start; dpIndex < access.getNumberOfDataPoints(); dpIndex++) {
              final double mz = access.getMzValue(dpIndex);
              final double intensity = access.getIntensityValue(dpIndex);

              // if the trace upper mz is smaller than the current mz, we break and go to the next trace
              if (traceMzRange.upperEndpoint() < mz) {
                break;
              }

              if (useRawData && intensity < customNoiseLevel) {
                continue;
              }

              if (traceMzRange.contains(mz)) {
                expandingTraces.get(traceIndex).offerDataPoint(access, dpIndex);
              }
            }
          }
        }
        processedFrames.getAndIncrement();

        desc = flist.getName() + ": expanding traces for frame " + processedFrames.get() + "/"
            + totalFrames + " m/z range: " + RangeUtils.formatRange(traceMzRange, mzFormat);
      }
    } catch (MissingMassListException e) {
      // allow traces to be released
      expandingTraces = null;
      e.printStackTrace();
      logger.log(Level.WARNING, e.getMessage(), e);
      setErrorMessage(e.getMessage());
      setStatus(TaskStatus.ERROR);
    }

    for (var expandingTrace : expandingTraces) {
      desc = "Creating new features " + createdRows + "/" + expandingTraces.size();

      if (expandingTrace.getNumberOfMobilityScans() > 1) {
        final IonMobilogramTimeSeries series = expandingTrace.toIonMobilogramTimeSeries(
            getMemoryMapStorage(), mobilogramDataAccess);
        expandedTraces.add(new ExpandedTrace(series, expandingTrace.getRow(),
            expandingTrace.getRow().getFeature(imsFile)));
      }

      createdRows++;

      if (isCanceled()) {
        // allow traces to be released
        expandingTraces = null;
        return;
      }
    }

    // allow traces to be released
    expandingTraces = null;

//    synchronized (newFlist) {
//      newRows.forEach(newFlist::addRow);
//    }

    setStatus(TaskStatus.FINISHED);
  }

  @NotNull
  public List<ExpandedTrace> getExpandedTraces() {
    return expandedTraces;
  }
}
