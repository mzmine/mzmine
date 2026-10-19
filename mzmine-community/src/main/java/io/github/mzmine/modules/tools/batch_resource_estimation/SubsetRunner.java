package io.github.mzmine.modules.tools.batch_resource_estimation;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.batchmode.BatchModeParameters;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.batchmode.BatchTask;
import io.github.mzmine.modules.batchmode.timing.StepMeasurement;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorageStats;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Runs the batch of a resource estimation on a subset of its files and measures it. Keeps the batch
 * and the file order so that later runs, e.g. a larger run that is added to a result, use the same
 * files.
 */
public final class SubsetRunner {

  private static final Logger logger = Logger.getLogger(SubsetRunner.class.getName());

  private final @NotNull BatchQueue queue;
  private final @NotNull List<EstimationInputFile> order;
  private final @Nullable File metadataFile;
  private final double meanBytesPerFile;
  private final @NotNull EstimationProjectProvider projectProvider;
  private volatile @Nullable BatchTask currentBatch;
  private volatile boolean canceled;

  /**
   * @param queue            the batch without export steps
   * @param order            all files in the order in which they are added to subsets, see
   *                         {@link ResourceSubsetSelector#orderForSubsets(List)}
   * @param metadataFile     metadata file of the data import, null if none
   * @param meanBytesPerFile mean file size of all files of the batch
   */
  public SubsetRunner(@NotNull final BatchQueue queue,
      @NotNull final List<EstimationInputFile> order, @Nullable final File metadataFile,
      final double meanBytesPerFile, @NotNull final EstimationProjectProvider projectProvider) {
    this.queue = queue;
    this.order = order;
    this.metadataFile = metadataFile;
    this.meanBytesPerFile = meanBytesPerFile;
    this.projectProvider = projectProvider;
  }

  /**
   * @return the number of files in the batch
   */
  public int totalFiles() {
    return order.size();
  }

  /**
   * Runs the batch on the first files of the order on an empty project.
   *
   * @return the measurements or null if the run was canceled
   * @throws IllegalStateException if the batch failed
   */
  public @Nullable SubsetRun run(final int files) {
    canceled = false;
    final List<EstimationInputFile> subset = order.subList(0, files);
    final MZmineProject project = projectProvider.projectForRun();
    try {
      // the baseline of the application without any data of the previous runs
      collectReleasedData();
      final double baselineHeapGB = ConfigService.getConfiguration().getUsedMemoryGB();
      final double baselineLiveTempGB = MemoryMapStorageStats.snapshot().liveUsedBytes() / 1e9;

      final BatchQueue subsetQueue = queue.clone();
      final File[] subsetFiles = subset.stream().map(EstimationInputFile::file)
          .toArray(File[]::new);
      if (!subsetQueue.setImportFiles(subsetFiles, metadataFile, null)) {
        throw new IllegalStateException(
            "Cannot set the subset files in the data import step of the batch");
      }
      final BatchModeParameters batchParameters = new BatchModeParameters();
      batchParameters.setParameter(BatchModeParameters.batchQueue, subsetQueue);
      final BatchTask batch = new BatchTask(project, batchParameters, Instant.now(), null);
      // the GC after each step measures the heap that each step retains
      batch.setRunGCafterBatchStep(true);
      currentBatch = batch;
      // decision: forced full collections measure the live heap independent of -Xmx
      final double gcSeconds;
      try (final PeriodicGcSampler sampler = PeriodicGcSampler.start()) {
        batch.run();
        gcSeconds = sampler.gcSeconds();
      }
      currentBatch = null;

      if (batch.getStatus() != TaskStatus.FINISHED) {
        if (canceled) {
          return null;
        }
        throw new IllegalStateException(
            "Batch failed on a subset of %d files: %s".formatted(files, batch.getErrorMessage()));
      }

      final List<StepMeasurement> steps = withoutGcTime(
          batch.getStepMeasurements().stream().filter(m -> m.step() > 0).toList(), gcSeconds);
      final int maxRows = project.getCurrentFeatureLists().stream()
          .mapToInt(FeatureList::getNumberOfRows).max().orElse(0);
      final long subsetBytes = subset.stream().mapToLong(EstimationInputFile::sizeBytes).sum();
      final SubsetRun run = new SubsetRun(files, subsetBytes / meanBytesPerFile, baselineHeapGB,
          baselineLiveTempGB, maxRows, steps);
      logger.info(("Resource estimation subset run: %d files (%.2f file equivalents), baseline "
          + "heap %.2f GB, baseline temp %.3f GB, %d rows").formatted(run.files(),
          run.fileEquivalents(), baselineHeapGB, baselineLiveTempGB, maxRows));
      return run;
    } finally {
      currentBatch = null;
      projectProvider.release(project);
      collectReleasedData();
    }
  }

  /**
   * The forced collections run in regular intervals, so they are removed from the step times in
   * proportion to the duration of each step.
   *
   * @param gcSeconds seconds of the forced collections during all steps
   */
  static @NotNull List<StepMeasurement> withoutGcTime(@NotNull final List<StepMeasurement> steps,
      final double gcSeconds) {
    final double totalSeconds = steps.stream().mapToDouble(StepMeasurement::secondsToFinish).sum();
    if (totalSeconds <= 0 || gcSeconds <= 0) {
      return steps;
    }
    // assumption: the collections never take most of the time, keep at least 10 %
    final double keep = Math.max(0.1, 1 - gcSeconds / totalSeconds);
    return steps.stream().map(m -> new StepMeasurement(m.step(), m.name(),
        m.secondsToFinish() * keep, m.usedHeapGB(), m.peakHeapGB(), m.peakHeapAfterGcGB(),
        m.tempFilesCreated(), m.reservedTempFileGB(), m.usedTempFileGB(), m.liveTempFiles(),
        m.liveTempFileUsedGB())).toList();
  }

  /**
   * Stops the current run after its current batch step.
   */
  public void cancel() {
    canceled = true;
    final BatchTask batch = currentBatch;
    if (batch != null) {
      batch.cancel();
    }
  }

  /**
   * Frees the data of a released project before the next measurement. The GUI keeps the previous
   * project until the FX thread switched to the new one. Memory mapped temp files are released by
   * a cleaner after their owners were collected, which the second GC picks up.
   */
  private static void collectReleasedData() {
    if (DesktopService.isGUI()) {
      FxThread.runOnFxThreadAndWait(() -> {
      });
    }
    System.gc();
    try {
      // give the reference handler and cleaner threads time to process the collected objects
      TimeUnit.MILLISECONDS.sleep(200);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    System.gc();
  }
}
