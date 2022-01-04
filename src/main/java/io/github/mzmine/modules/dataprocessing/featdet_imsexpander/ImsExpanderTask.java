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
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.FeatureDataType;
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

    final List<? extends FeatureListRow> rows = new ArrayList<>(flist.getRows());
    rows.sort((Comparator.comparingDouble(FeatureListRow::getAverageMZ)));

    // either we use the row m/z + tolerance range, or we use the mz range of the feature.
    final List<ExpandingTrace> expandingTraces = rows.stream().map(
        row -> new ExpandingTrace((ModularFeatureListRow) row,
            useMzToleranceRange ? mzTolerance.getToleranceRange(row.getAverageMZ())
                : row.getFeature(imsFile).getRawDataPointsMZRange())).toList();

    final List<Frame> frames = (List<Frame>) flist.getSeletedScans(flist.getRawDataFile(0));
    assert frames != null;

    // we partition the frames so we can use multiple MobilityScanDataAccesses on the same raw data
    // file and don't have to extract the data points multiple times for each frame, if we use
    // a TdfRawDataFileImpl (no memory mapped data points)
    final List<List<Frame>> subLists = Lists.partition(frames, frames.size() / NUM_THREADS);

    for (final List<Frame> subList : subLists) {
      final Frame first = subList.get(0);
      final Frame last = subList.get(subList.size() - 1);
      Range<Float> rtRange = Range.closed(first.getRetentionTime(), last.getRetentionTime());
      final List<ExpandingTrace> eligibleTraces = expandingTraces.stream()
          .filter(trace -> trace.getRtRange().isConnected(rtRange)).toList();
      tasks.add(new ImsExpanderSubTask(getMemoryMapStorage(), parameters, subList, flist,
          eligibleTraces));
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

    final BinningMobilogramDataAccess mobilogramDataAccess = EfficientDataAccess.of(imsFile,
        binWidth);

    final ModularFeatureList newFlist = new ModularFeatureList(flist.getName() + SUFFIX,
        getMemoryMapStorage(), imsFile);
    newFlist.setSelectedScans(imsFile, flist.getSeletedScans(imsFile));
    newFlist.getAppliedMethods().addAll(flist.getAppliedMethods());
    DataTypeUtils.addDefaultIonMobilityTypeColumns(newFlist);

    for (ExpandingTrace expandingTrace : expandingTraces) {
      desc = "Creating new features " + processedRows.getAndIncrement() + "/" + totalRows;

      if (expandingTrace.getNumberOfMobilityScans() > 1) {
        final IonMobilogramTimeSeries series = expandingTrace.toIonMobilogramTimeSeries(
            getMemoryMapStorage(), mobilogramDataAccess);
        final ModularFeatureListRow row = new ModularFeatureListRow(newFlist,
            expandingTrace.getRow(), false);
        final ModularFeature f = new ModularFeature(newFlist,
            expandingTrace.getRow().getFeature(imsFile));
        f.set(FeatureDataType.class, series);
        row.addFeature(imsFile, f);
        FeatureDataUtils.recalculateIonSeriesDependingTypes(f);
        newFlist.addRow(row);
      }

      if (isCanceled()) {
        return;
      }
    }

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
