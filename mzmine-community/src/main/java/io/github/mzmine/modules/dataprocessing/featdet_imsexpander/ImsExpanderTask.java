/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import com.google.common.collect.Lists;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
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
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.threadpools.ThreadPoolTask;
import io.github.mzmine.util.DataTypeUtils;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ImsExpanderTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(ImsExpanderTask.class.getName());
  private final int NUM_THREADS = MZmineCore.getConfiguration().getPreferences()
      .getParameter(MZminePreferences.numOfThreads).getValue();
  private static final String SUFFIX = " expanded";
  protected final ParameterSet parameters;
  protected final ModularFeatureList flist;
  final List<AbstractTask> tasks = new ArrayList<>();
  private final MZmineProject project;
  private final MZTolerance mzTolerance;
  private final boolean useMzToleranceRange;
  private final AtomicInteger processedRows = new AtomicInteger(0);
  private final int binWidth;
  private final int maxNumTraces;
  private final OriginalFeatureListOption handleOriginal;
  private String desc = "Mobility expanding.";
  private long totalRows = 1;
  private long createdRows = 0;

  public ImsExpanderTask(@Nullable final MemoryMapStorage storage,
      @NotNull final ParameterSet parameters, @NotNull final ModularFeatureList flist,
      MZmineProject project, @NotNull Instant moduleCallDate) {
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
    handleOriginal = this.parameters.getParameter(ImsExpanderParameters.handleOriginal).getValue();
  }

  @Override
  public String getTaskDescription() {
    return desc;
  }

  @Override
  public double getFinishedPercentage() {
    // stream / iterator for loop may lead to concurrend mod exception, use classic for loop here
    double sum = 0.0;
    for (int i = 0; i < tasks.size(); i++) {
      AbstractTask task = tasks.get(i);
      double finishedPercentage = task.getFinishedPercentage();
      sum += finishedPercentage;
    }
    return 0.4 * sum / tasks.size() + 0.4 * (processedRows.get() / (double) totalRows)
           + 0.2 * createdRows / (double) totalRows;
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

    desc = "Mobility expanding feature list " + flist.getName();

    final List<? extends FeatureListRow> rows = new ArrayList<>(flist.getRows());
    rows.sort((Comparator.comparingDouble(FeatureListRow::getAverageMZ)));

    // either we use the row m/z + tolerance range, or we use the mz range of the feature.
    final List<ExpandingTrace> expandingTraces = new ArrayList<>(rows.stream().map(
        row -> new ExpandingTrace((ModularFeatureListRow) row,
            useMzToleranceRange ? mzTolerance.getToleranceRange(row.getAverageMZ())
                : row.getFeature(imsFile).getRawDataPointsMZRange())).toList());

    if (expandingTraces.isEmpty()) {
      newFlist.getAppliedMethods().add(
          new SimpleFeatureListAppliedMethod(ImsExpanderModule.class, parameters,
              getModuleCallDate()));
      handleOriginal.reflectNewFeatureListToProject(SUFFIX, project, newFlist, flist);
      setStatus(TaskStatus.FINISHED);
      desc = "No traces in feature list " + flist.getName();
      return;
    }

    final List<Frame> frames = (List<Frame>) flist.getSeletedScans(flist.getRawDataFile(0));
    assert frames != null;

    // we partition the traces (sorted by rt) so we can start and end at specific frames. By splitting
    // the traces and not frames, we can also directly store the raw data on the SSD/HDD as soon as
    // a thread finishes. Thereby we can reduce the memory consumption, especially in images.
    final int tracesPerList = Math.max(1,
        Math.min(expandingTraces.size() / NUM_THREADS, maxNumTraces));
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
              mobilogramDataAccess, imsFile));
    }

    // might need a copy of task list -  we usually clear the tasks list to not hold on to memory
    ThreadPoolTask poolTask = ThreadPoolTask.createDefaultTaskManagerPool(getTaskDescription(),
        new ArrayList<>(tasks));
    var wrappedTask = MZmineCore.getTaskController().runTaskOnThisThreadBlocking(poolTask);

    if (wrappedTask == null || poolTask.isCanceled()) {
      final String errors = tasks.stream().map(AbstractTask::getErrorMessage)
          .filter(Objects::nonNull).distinct().collect(Collectors.joining(", "));
      setErrorMessage(errors);
      setStatus(poolTask.getStatus());
      return;
    }

    desc = "Creating new features for feature list " + flist.getName();
    for (AbstractTask task : tasks) {
      final ImsExpanderSubTask t = (ImsExpanderSubTask) task;
      final List<ExpandedTrace> expandedTraces = t.getExpandedTraces();

      for (ExpandedTrace expandedTrace : expandedTraces) {
        final ModularFeatureListRow row = new ModularFeatureListRow(newFlist,
            expandedTrace.oldRow(), false);
        final ModularFeature f = new ModularFeature(newFlist, expandedTrace.oldFeature());
        f.set(FeatureDataType.class, expandedTrace.series());
        FeatureDataUtils.recalculateIonSeriesDependingTypes(f);
        row.addFeature(imsFile, f);
        newFlist.addRow(row);
        createdRows++;
      }
    }

    // explicitly don't renumber, IDs are kept from the old flist.
    FeatureListUtils.sortByDefaultRT(newFlist, false);

    newFlist.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(ImsExpanderModule.class, parameters,
            getModuleCallDate()));
    handleOriginal.reflectNewFeatureListToProject(SUFFIX, project, newFlist, flist);
    setStatus(TaskStatus.FINISHED);
  }

  @Override
  public TaskPriority getTaskPriority() {
    return TaskPriority.HIGH; // master task needs high priority, so it does not brick the task controller
  }
}
