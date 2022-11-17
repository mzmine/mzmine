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
import io.github.mzmine.util.exceptions.MissingMassListException;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
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
        + createdRows / (double) expandingTraces.size();
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.finest("Initialising data access for file " + imsFile.getName());
    final MobilityScanDataAccess access = new MobilityScanDataAccess(imsFile,
        useRawData ? MobilityScanDataType.RAW : MobilityScanDataType.CENTROID, frames);

    totalFrames = access.getNumberOfScans();
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

          int traceIndex = 0;
          for (int dpIndex = 0; dpIndex < access.getNumberOfDataPoints() && traceIndex < numTraces;
              dpIndex++) {
            final double mz = access.getMzValue(dpIndex);
            final double intensity = access.getIntensityValue(dpIndex);

            if (useRawData && intensity < customNoiseLevel) {
              continue;
            }

            // while the trace upper mz smaller than the current mz, we increment the trace index
            while (expandingTraces.get(traceIndex).getMzRange().upperEndpoint() < mz
                && traceIndex < numTraces - 1) {
              traceIndex++;
            }
            // if the current lower mz passed the current data point, we go to the next data point
            if (expandingTraces.get(traceIndex).getMzRange().lowerEndpoint() > mz) {
              continue;
            }

            // try to offer the current data point to the trace
            while (expandingTraces.get(traceIndex).getMzRange().contains(mz)
                && !expandingTraces.get(traceIndex).offerDataPoint(access, dpIndex)
                && traceIndex < numTraces - 1) {
              traceIndex++;
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
