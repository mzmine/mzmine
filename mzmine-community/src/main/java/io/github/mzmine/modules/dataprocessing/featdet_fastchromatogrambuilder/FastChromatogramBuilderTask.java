/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder;

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.norm_remove_scanrtcal.RemoveScanRtCorrectionModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.combowithinput.MZToleranceOrAuto;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskService;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.impl.WrappedTask;
import io.github.mzmine.taskcontrol.utils.TaskUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.exceptions.MissingMassListException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds the chromatograms of all selected raw data files with the {@link FastChromatogramBuilder}.
 * A single main task checks all files first, estimates the m/z tolerance if it is set to auto and
 * then processes the files in parallel, one {@link FastChromatogramFileTask} per file run by the
 * task controller. The feature lists are added in the order of the files.
 */
public class FastChromatogramBuilderTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(
      FastChromatogramBuilderTask.class.getName());

  // decision: the files with the most data points give the most pairs of consecutive signals.
  // All files are assumed to have the same instrument properties.
  static final int MAX_SAMPLE_FILES = 3;
  private static final MZTolerance FALLBACK_TOLERANCE = new MZTolerance(0.002, 10);

  private final @NotNull MZmineProject project;
  private final @NotNull List<RawDataFile> dataFiles;
  private final @NotNull ParameterSet parameters;
  private final @NotNull Class<? extends MZmineModule> callingModule;
  private final @NotNull ScanSelection scanSelection;
  private final @NotNull MZToleranceOrAuto toleranceSetting;
  private final int minimumConsecutiveScans;
  private final double minGroupIntensity;
  private final double minHighestPoint;
  private final @NotNull String suffix;
  private final boolean clearRtCorrection;

  // set once before the file tasks run, read by the progress and cancel of other threads
  private volatile @NotNull List<FastChromatogramFileTask> fileTasks = List.of();
  private volatile double progress = 0d;
  private volatile @NotNull String description;
  private @Nullable MzToleranceEstimate toleranceEstimate;
  private @Nullable MZTolerance usedTolerance;

  /**
   * @param parameters a clone of the module parameters, the used m/z tolerance is written into it
   */
  public FastChromatogramBuilderTask(@NotNull MZmineProject project,
      @NotNull RawDataFile[] dataFiles, @NotNull ParameterSet parameters,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      @NotNull Class<? extends MZmineModule> callingModule) {
    super(storage, moduleCallDate);
    this.project = project;
    this.dataFiles = List.of(dataFiles);
    this.parameters = parameters;
    this.callingModule = callingModule;
    scanSelection = parameters.getValue(FastChromatogramBuilderParameters.scanSelection);
    toleranceSetting = parameters.getValue(FastChromatogramBuilderParameters.mzTolerance);
    minimumConsecutiveScans = parameters.getValue(
        FastChromatogramBuilderParameters.minimumConsecutiveScans);
    minGroupIntensity = parameters.getValue(FastChromatogramBuilderParameters.minGroupIntensity);
    minHighestPoint = parameters.getValue(FastChromatogramBuilderParameters.minHighestPoint);
    suffix = parameters.getValue(FastChromatogramBuilderParameters.suffix);
    clearRtCorrection = parameters.getValue(FastChromatogramBuilderParameters.clearRtCorrection);
    description = "Detecting chromatograms in %d files".formatted(dataFiles.length);
    // the sub tasks follow the status of the main task
    addTaskStatusListener((_, newStatus, _) -> {
      if (newStatus == TaskStatus.CANCELED) {
        cancelSubTasks();
      }
    });
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    final List<FastChromatogramFileTask> tasks = fileTasks;
    if (tasks.isEmpty()) {
      return progress;
    }
    double sum = 0d;
    for (final FastChromatogramFileTask task : tasks) {
      sum += task.getFinishedPercentage();
    }
    return progress + (1d - progress) * sum / tasks.size();
  }

  /**
   * The main task only waits for the file tasks, a high priority keeps it from blocking a thread of
   * the task controller.
   */
  @Override
  public TaskPriority getTaskPriority() {
    return TaskPriority.HIGH;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    if (dataFiles.isEmpty()) {
      setStatus(TaskStatus.FINISHED);
      return;
    }
    logger.info(() -> "Started fast chromatogram builder on %d files".formatted(dataFiles.size()));

    if (clearRtCorrection) {
      RemoveScanRtCorrectionModule.clearRtCorrection(dataFiles.toArray(RawDataFile[]::new),
          getModuleCallDate(), "Resetting RT correction during chromatogram builder.");
    }

    // check all files before processing any
    final List<Scan[]> selectedScans = new ArrayList<>(dataFiles.size());
    final Set<String> warnings = new LinkedHashSet<>();
    for (final RawDataFile file : dataFiles) {
      final Scan[] scans = scanSelection.getMatchingScans(file);
      if (!checkScans(file, scans, warnings)) {
        return;
      }
      selectedScans.add(scans);
    }
    for (final String warning : warnings) {
      DesktopService.getDesktop().displayMessage(warning);
    }

    final MZTolerance tolerance = resolveTolerance(selectedScans);
    if (tolerance == null || isCanceled()) {
      return;
    }
    usedTolerance = tolerance;
    // decision: the applied method stores the used tolerance, also for auto, so that processed data
    // and later modules see the actual value
    parameters.setParameter(FastChromatogramBuilderParameters.mzTolerance,
        new MZToleranceOrAuto(toleranceSetting.option(), tolerance));

    final List<FastChromatogramFileTask> tasks = new ArrayList<>(dataFiles.size());
    for (int i = 0; i < dataFiles.size(); i++) {
      tasks.add(new FastChromatogramFileTask(dataFiles.get(i), selectedScans.get(i), tolerance,
          minimumConsecutiveScans, minGroupIntensity, minHighestPoint, suffix,
          parameters.cloneParameterSet(), getMemoryMapStorage(), getModuleCallDate(),
          callingModule));
    }
    fileTasks = List.copyOf(tasks);
    if (!runFileTasks()) {
      return;
    }

    for (final FastChromatogramFileTask task : fileTasks) {
      project.addFeatureList(Objects.requireNonNull(task.getFeatureList()));
    }
    progress = 1d;
    setStatus(TaskStatus.FINISHED);
    logger.info(
        () -> "Finished fast chromatogram builder on %d files with m/z tolerance %s".formatted(
            dataFiles.size(), tolerance));
  }

  /**
   * @return false if a file task failed or the main task was canceled
   */
  private boolean runFileTasks() {
    description = "Detecting chromatograms in %d files with m/z tolerance %s".formatted(
        dataFiles.size(), usedTolerance);
    if (fileTasks.size() == 1) {
      // decision: a single file runs directly on this thread
      fileTasks.getFirst().run();
    } else {
      // decision: the task controller runs the file tasks with the number of threads of the
      // preferences, like the multithreaded gap filling. The wait cancels them with this task.
      final WrappedTask[] wrapped = TaskService.getController()
          .addTasks(fileTasks.toArray(Task[]::new));
      TaskUtils.waitForTasksToFinish(this, wrapped);
    }
    if (isCanceled()) {
      return false;
    }
    for (final FastChromatogramFileTask task : fileTasks) {
      switch (task.getStatus()) {
        case FINISHED -> {
        }
        case ERROR -> {
          error("Chromatogram building failed for %s: %s".formatted(task.getDataFile(),
              task.getErrorMessage()));
          return false;
        }
        case CANCELED, WAITING, PROCESSING -> {
          // canceled from the task view or never ran
          cancel();
          return false;
        }
      }
    }
    return true;
  }

  private void cancelSubTasks() {
    for (final FastChromatogramFileTask task : fileTasks) {
      task.cancel();
    }
  }

  /**
   * @return the custom tolerance or the estimate, null if canceled
   */
  @Nullable
  private MZTolerance resolveTolerance(@NotNull List<Scan[]> selectedScans) {
    return switch (toleranceSetting.option()) {
      case CUSTOM -> Objects.requireNonNull(toleranceSetting.tolerance(),
          "The custom m/z tolerance is not set");
      case AUTO -> estimateTolerance(selectedScans);
    };
  }

  @Nullable
  private MZTolerance estimateTolerance(@NotNull List<Scan[]> selectedScans) {
    final List<Integer> samples = selectSampleFiles(dataFiles, selectedScans, MAX_SAMPLE_FILES);
    final List<String> sampleNames = samples.stream().map(i -> dataFiles.get(i).getName()).toList();
    description = "Estimating the m/z tolerance from " + String.join(", ", sampleNames);
    final List<MzIntensityScans> sampleScans = new ArrayList<>(samples.size());
    for (final int i : samples) {
      sampleScans.add(new ScanDataAccessScans(
          EfficientDataAccess.of(dataFiles.get(i), ScanDataType.MASS_LIST,
              Arrays.asList(selectedScans.get(i)))));
    }
    final MzToleranceEstimate estimate;
    try {
      estimate = MzToleranceEstimation.estimate(sampleScans, minimumConsecutiveScans,
          minGroupIntensity, minHighestPoint, this::isCanceled);
    } catch (MissingMassListException e) {
      error(e.getMessage(), e);
      return null;
    }
    if (isCanceled()) {
      return null;
    }
    progress = 0.1d;
    if (estimate == null) {
      final MZTolerance fallback = Objects.requireNonNullElse(toleranceSetting.tolerance(),
          FALLBACK_TOLERANCE);
      logger.warning(() -> """
          Could not estimate the m/z tolerance from %s, too few signals in consecutive scans. \
          Using %s""".formatted(sampleNames, fallback));
      return fallback;
    }
    toleranceEstimate = estimate;
    logger.info(() -> "Estimated the scan to scan m/z tolerance from %s: %s".formatted(sampleNames,
        estimate));
    return estimate.tolerance();
  }

  /**
   * @return indices of the files with the most data points in the selected scans, ties by name, in
   * the order of the files
   */
  @NotNull
  static List<Integer> selectSampleFiles(@NotNull List<RawDataFile> files,
      @NotNull List<Scan[]> selectedScans, int maxSamples) {
    final long[] numDataPoints = new long[files.size()];
    final List<Integer> order = new ArrayList<>(files.size());
    for (int i = 0; i < files.size(); i++) {
      for (final Scan scan : selectedScans.get(i)) {
        final MassList masses = scan.getMassList();
        numDataPoints[i] += masses == null ? 0 : masses.getNumberOfDataPoints();
      }
      order.add(i);
    }
    order.sort((a, b) -> {
      int result = Long.compare(numDataPoints[b], numDataPoints[a]);
      if (result == 0) {
        result = files.get(a).getName().compareTo(files.get(b).getName());
      }
      return result != 0 ? result : Integer.compare(a, b);
    });
    final List<Integer> samples = new ArrayList<>(
        order.subList(0, Math.min(maxSamples, order.size())));
    samples.sort(Integer::compare);
    return samples;
  }

  /**
   * Same checks and messages as the ADAP chromatogram builder.
   *
   * @param warnings collects messages for the user, shown once for all files
   * @return false if the scans cannot be processed, sets the error
   */
  private boolean checkScans(@NotNull RawDataFile dataFile, @NotNull Scan[] scans,
      @NotNull Set<String> warnings) {
    if (scans.length == 0) {
      error("""
          There are no scans in file "%s" satisfying scan filters. Consider updating filters
          with "Show" on the "Scan filters" parameter. Filter was: %s""".formatted(
          dataFile.getName(), scanSelection.toShortDescription()));
      return false;
    }

    int emptyScans = 0;
    double previousRt = Double.NEGATIVE_INFINITY;
    for (final Scan scan : scans) {
      if (scan.getMassList() == null) {
        error(missingMassListMessage(dataFile, scan));
        return false;
      }
      if (scan.isEmptyScan()) {
        emptyScans++;
        continue;
      }
      if (scan.getRetentionTime() < previousRt) {
        error("Retention time of scan #" + scan.getScanNumber() + " in file " + dataFile.getName()
            + " is smaller then the retention time of the previous scan."
            + " Please make sure you only use scans with increasing retention times."
            + " You can restrict the scan numbers in the parameters, or you can use the Crop"
            + " filter module");
        return false;
      }
      previousRt = scan.getRetentionTime();
    }
    if (emptyScans > 0) {
      final int numEmpty = emptyScans;
      logger.info(() -> "%d scans in %s were found to be empty.".formatted(numEmpty, dataFile));
    }

    final int level = scans[0].getMSLevel();
    final PolarityType polarity = scans[0].getPolarity();
    for (int i = 1; i < scans.length; i++) {
      if (level != scans[i].getMSLevel()) {
        warnings.add("mzmine thinks that you are running the chromatogram builder on both MS1- and "
            + "MS2-scans. " + "This will likely produce wrong results. "
            + "Please, set the scan filter parameter to a specific MS level");
        break;
      }
      if (polarity != scans[i].getPolarity()) {
        warnings.add("""
            mzmine thinks you are processing data of multiple polarities (%s and %s)
            at the same time. This will likely lead to wrong results.
            Set the polarity filter in the wizard or the chromatogram builder step to process
            each polarity individually.""".formatted(polarity, scans[i].getPolarity()));
        break;
      }
    }
    return true;
  }

  @NotNull
  private static String missingMassListMessage(@NotNull RawDataFile dataFile, @NotNull Scan scan) {
    final StringBuilder b = new StringBuilder("Scan #");
    b.append(scan.getScanNumber()).append(" from ");
    b.append(dataFile.getName());
    b.append(" does not have a mass list. Please run \"Raw data methods\" -> \"Mass detection\"");
    if (dataFile instanceof IMSRawDataFile) {
      b.append("\nIMS files require mass detection on the frame level (Scan type = \"Frames ");
      b.append("only\" or \"All scan types\"");
    }
    return b.toString();
  }

  /**
   * @return the created feature lists in the order of the data files after the task finished,
   * otherwise an empty list
   */
  @NotNull
  public List<ModularFeatureList> getFeatureLists() {
    if (getStatus() != TaskStatus.FINISHED) {
      return List.of();
    }
    return fileTasks.stream().map(FastChromatogramFileTask::getFeatureList).filter(Objects::nonNull)
        .toList();
  }

  /**
   * @return the statistics of the chromatogram detection per data file after the task finished,
   * otherwise an empty list
   */
  @NotNull
  public List<FastChromatogramBuilderStatistics> getStatistics() {
    if (getStatus() != TaskStatus.FINISHED) {
      return List.of();
    }
    return fileTasks.stream().map(FastChromatogramFileTask::getStatistics).filter(Objects::nonNull)
        .toList();
  }

  /**
   * @return the estimate if the tolerance was estimated, otherwise null
   */
  @Nullable
  public MzToleranceEstimate getToleranceEstimate() {
    return toleranceEstimate;
  }

  /**
   * @return the used m/z tolerance after it was resolved, otherwise null
   */
  @Nullable
  public MZTolerance getUsedTolerance() {
    return usedTolerance;
  }
}
