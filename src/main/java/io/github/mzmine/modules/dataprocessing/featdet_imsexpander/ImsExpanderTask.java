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

package io.github.mzmine.modules.dataprocessing.featdet_imsexpander;

import com.google.common.collect.Lists;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.AllTasksFinishedListener;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataTypeUtils;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ImsExpanderTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(ImsExpanderTask.class.getName());
  private static final int NUM_THREADS = MZmineCore.getConfiguration().getPreferences()
      .getParameter(MZminePreferences.numOfThreads).getValue();
  private static final String SUFFIX = " expanded ";
  protected final ParameterSet parameters;
  protected final ModularFeatureList flist;
  final List<AbstractTask> tasks = new ArrayList<>();
  private final MZmineProject project;
  private final MZTolerance mzTolerance;
  private final boolean useMzToleranceRange;
  private final AtomicInteger processedFrames = new AtomicInteger(0);
  private final AtomicInteger processedRows = new AtomicInteger(0);
  private final int binWidth;
  private String desc = "Mobility expanding.";
  private long totalFrames = 1;
  private long totalRows = 1;

  private final int maxNumTraces;

  public ImsExpanderTask(@Nullable final MemoryMapStorage storage,
      @NotNull final ParameterSet parameters, @NotNull final ModularFeatureList flist,
      MZmineProject project, final int allowedThreads, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);
    this.parameters = parameters;
    this.project = project;
    this.flist = flist;
    useMzToleranceRange = parameters.getParameter(ImsExpanderParameters.mzTolerance).getValue();
    mzTolerance = parameters.getParameter(ImsExpanderParameters.mzTolerance).getEmbeddedParameter()
        .getValue();
    maxNumTraces =
        parameters.getValue(ImsExpanderParameters.maxNumTraces) ? parameters.getParameter(
            ImsExpanderParameters.maxNumTraces).getEmbeddedParameter().getValue()
            : Integer.MAX_VALUE;
    binWidth = parameters.getParameter(ImsExpanderParameters.mobilogramBinWidth).getValue()
        ? parameters.getParameter(ImsExpanderParameters.mobilogramBinWidth).getEmbeddedParameter()
        .getValue() : BinningMobilogramDataAccess.getRecommendedBinWidth(
        (IMSRawDataFile) flist.getRawDataFile(0));
  }

  @Override
  public String getTaskDescription() {
    return desc;
  }

  @Override
  public double getFinishedPercentage() {
    return
        0.5 * tasks.stream().mapToDouble(AbstractTask::getFinishedPercentage).sum() / tasks.size()
            + 0.5 * (processedRows.get() / (double) totalRows);
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    if (flist.getNumberOfRawDataFiles() != 1 || !(flist.getRawDataFile(
        0) instanceof IMSRawDataFile imsFile)) {
      setErrorMessage("More than one raw data file in feature list " + flist.getName()
          + " or no mobility dimension in raw data file.");
      setStatus(TaskStatus.ERROR);
      return;
    }

    totalRows = flist.getNumberOfRows();

    final ModularFeatureList newFlist = new ModularFeatureList(flist.getName() + SUFFIX,
        getMemoryMapStorage(), imsFile);
    newFlist.setSelectedScans(imsFile, flist.getSeletedScans(imsFile));
    newFlist.getAppliedMethods().addAll(flist.getAppliedMethods());
    DataTypeUtils.addDefaultIonMobilityTypeColumns(newFlist);

    final List<? extends FeatureListRow> rows = new ArrayList<>(flist.getRows());
    rows.sort((Comparator.comparingDouble(FeatureListRow::getAverageMZ)));

    // either we use the row m/z + tolerance range, or we use the mz range of the feature.
    final List<ExpandingTrace> expandingTraces = new ArrayList<>(rows.stream().map(
        row -> new ExpandingTrace((ModularFeatureListRow) row,
            useMzToleranceRange ? mzTolerance.getToleranceRange(row.getAverageMZ())
                : row.getFeature(imsFile).getRawDataPointsMZRange())).toList());

    final List<Frame> frames = (List<Frame>) flist.getSeletedScans(flist.getRawDataFile(0));
    assert frames != null;

    // we partition the traces (sorted by rt) so we can start and end at specific frames. By splitting
    // the traces and not frames, we can also directly store the raw data on the SSD/HDD as soon as
    // a thread finishes. Thereby we can reduce the memory consumption, especially in images.
    final int tracesPerList = Math.min(expandingTraces.size() / NUM_THREADS, maxNumTraces);
    expandingTraces.sort(
        (a, b) -> Float.compare(a.getRtRange().lowerEndpoint(), b.getRtRange().lowerEndpoint()));
    final List<List<ExpandingTrace>> subLists = Lists.partition(expandingTraces, tracesPerList);

    for (final List<ExpandingTrace> subList : subLists) {
      final Frame firstFrame = (Frame) subList.get(0).getRow().getBestFeature().getFeatureData()
          .getSpectrum(0);
      final ExpandingTrace lastFrameTrace = subList.stream().max(
              (a, b) -> Float.compare(a.getRtRange().upperEndpoint(), b.getRtRange().upperEndpoint()))
          .orElseThrow(() -> new IllegalStateException("Cannot determine last frame."));
      final IonTimeSeries<? extends Scan> lastTraceData = lastFrameTrace.getRow().getBestFeature()
          .getFeatureData();
      final Frame lastFrame = (Frame) lastTraceData.getSpectrum(
          lastTraceData.getNumberOfValues() - 1);
      final List<Frame> framesSubList = frames.subList(frames.indexOf(firstFrame),
          frames.indexOf(lastFrame) + 1);

      final ArrayList<ExpandingTrace> traces = new ArrayList<>(subList);
      traces.sort(Comparator.comparingDouble(a -> a.getRow().getAverageMZ()));

      final BinningMobilogramDataAccess mobilogramDataAccess = EfficientDataAccess.of(imsFile,
          binWidth);
      tasks.add(
          new ImsExpanderSubTask(getMemoryMapStorage(), parameters, framesSubList, flist, traces,
              mobilogramDataAccess, newFlist));
    }

    final AtomicBoolean allThreadsFinished = new AtomicBoolean(false);
    final AtomicBoolean mayContinue = new AtomicBoolean(true);

    final AllTasksFinishedListener listener = new AllTasksFinishedListener(tasks, true,
        c -> allThreadsFinished.set(true), c -> {
      mayContinue.set(false);
      allThreadsFinished.set(true);
    }, c -> {
      mayContinue.set(false);
      allThreadsFinished.set(true);
    });

    MZmineCore.getTaskController().addTasks(tasks.toArray(AbstractTask[]::new));

    while (!allThreadsFinished.get()) {
      try {
        Thread.sleep(100L);
      } catch (InterruptedException e) {
        e.printStackTrace();
        logger.log(Level.WARNING, e.getMessage(), e);
        setErrorMessage(e.getMessage());
        setStatus(TaskStatus.ERROR);
        return;
      }
    }

    if (!mayContinue.get() || getStatus() == TaskStatus.CANCELED) {
      setStatus(TaskStatus.CANCELED);
      return;
    }

    // explicitly don't renumber, IDs are kept from the old flist.
    FeatureListUtils.sortByDefaultRT(newFlist, false);

    newFlist.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(ImsExpanderModule.class, parameters,
            getModuleCallDate()));
    final OriginalFeatureListOption handleOriginal = parameters.getParameter(
        ImsExpanderParameters.handleOriginal).getValue();
    // add new list / remove old if requested
    handleOriginal.reflectNewFeatureListToProject(SUFFIX, project, newFlist, flist);
    setStatus(TaskStatus.FINISHED);
  }

  @Override
  public TaskPriority getTaskPriority() {
    return TaskPriority.HIGH; // master task needs high priority, so it does not brick the task controller
  }
}
