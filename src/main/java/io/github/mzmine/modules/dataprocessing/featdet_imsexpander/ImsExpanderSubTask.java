/*
 *  Copyright 2006-2020 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_imsexpander;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.MobilityScanDataType;
import io.github.mzmine.datamodel.data_access.MobilityScanDataAccess;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.exceptions.MissingMassListException;
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
  private final List<ExpandingTrace> expandingTraces;

  private String desc = "Mobility expanding.";
  private final AtomicInteger processedFrames = new AtomicInteger(0);
  private final AtomicInteger processedRows = new AtomicInteger(0);

  private long totalFrames = 1;

  public ImsExpanderSubTask(@Nullable final MemoryMapStorage storage,
      @NotNull final ParameterSet parameters, @NotNull final List<Frame> frames,
      @NotNull final ModularFeatureList flist,
      @NotNull final List<ExpandingTrace> expandingTraces) {
    super(storage);
    this.parameters = parameters;
    this.frames = frames;
    this.flist = flist;
    this.expandingTraces = expandingTraces;
  }

  @Override
  public String getTaskDescription() {
    return desc;
  }

  @Override
  public double getFinishedPercentage() {
    return (processedFrames.get() / (double) totalFrames);
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    final IMSRawDataFile imsFile = (IMSRawDataFile) frames.get(0).getDataFile();
    logger.finest("Initialising data access for file " + imsFile.getName());
    final MobilityScanDataAccess access = new MobilityScanDataAccess(imsFile,
        MobilityScanDataType.CENTROID, frames);

    totalFrames = access.getNumberOfScans();

    final int numTraces = expandingTraces.size();
    try {

      for (int i = 0; i < access.getNumberOfScans(); i++) {
        if (isCanceled()) {
          return;
        }

        final Frame frame = access.nextFrame();

        desc = flist.getName() + ": expanding traces for frame " + processedFrames.get() + "/"
            + totalFrames + ".";

        while (access.hasNextMobilityScan()) {
          final MobilityScan mobilityScan = access.nextMobilityScan();

          int traceIndex = 0;
          for (int dpIndex = 0; dpIndex < access.getNumberOfDataPoints() && traceIndex < numTraces;
              dpIndex++) {
            double mz = access.getMzValue(dpIndex);
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
            while (expandingTraces.get(traceIndex).getMzRange().contains(mz) && !expandingTraces
                .get(traceIndex).offerDataPoint(access, dpIndex) && traceIndex < numTraces - 1) {
              traceIndex++;
            }
          }

          /*int dpIndex = 0;
          for(int traceIndex = 0; i < expandingTraces.size(); traceIndex++) {
            final double mz = access.getMzValue(dpIndex);
            final ExpandingTrace trace = expandingTraces.get(traceIndex);

            if(trace.getMzRange().lowerEndpoint() > mz) {
              continue;
            } else if(trace.getMzRange().upperEndpoint() < mz) {
              continue;
            }
          }*/
        }
        processedFrames.getAndIncrement();
      }
    } catch (MissingMassListException e) {
      e.printStackTrace();
      logger.log(Level.WARNING, e.getMessage(), e);
      setErrorMessage(e.getMessage());
      setStatus(TaskStatus.ERROR);
    }

    setStatus(TaskStatus.FINISHED);
  }
}
