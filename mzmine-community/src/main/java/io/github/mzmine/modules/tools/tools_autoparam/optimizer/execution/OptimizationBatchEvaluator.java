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

package io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.batchmode.BatchModeModule;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.batchmode.BatchTask;
import io.github.mzmine.modules.batchmode.BatchUtils;
import io.github.mzmine.modules.batchmode.autosave.AutoSaveBatchModule;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.FeatureFilterParameters;
import io.github.mzmine.modules.dataprocessing.filter_isotopegrouper.IsotopeGrouperModule;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterModule;
import io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.multithreaded.MultiThreadPeakFinderModule;
import io.github.mzmine.modules.dataprocessing.group_compoundgrouper.CompoundGrouperModule;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.corrgrouping.CorrelateGroupingModule;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.MainSpectralNetworkingModule;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkingModule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationModule;
import io.github.mzmine.modules.dataprocessing.id_spectral_library_match.SpectralLibrarySearchModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.DataImportWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.MetricContext;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.PrecisionDiagnostic;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.ShapeScoreDiagnostic;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.SweepMetric;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.FeatureRecord;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntSupplier;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moeaframework.core.Solution;

/**
 * Runs one optimized batch queue and translates its feature list into scores and diagnostics.
 */
final class OptimizationBatchEvaluator implements AutoCloseable {

  private static final Logger logger = Logger.getLogger(OptimizationBatchEvaluator.class.getName());

  enum ProjectMode {
    ACTIVE_PROJECT, ISOLATED_PROJECT
  }

  private final File @NotNull [] files;
  private final @NotNull List<SweepMetric> metrics;
  private final @NotNull MetricContext metricContext;
  private final @NotNull List<FeatureRecord> benchmarkFeatures;
  private final @NotNull AtomicReference<TaskStatus> externalStatus;
  private final @NotNull ProjectMode projectMode;
  private final @NotNull MZmineProject evaluationProject;
  private final @NotNull Object lifecycleLock = new Object();
  /** Guarded by {@link #lifecycleLock}. */
  private @Nullable BatchTask activeBatchTask;
  /** Guarded by {@link #lifecycleLock}. */
  private boolean batchRunning;
  /** Guarded by {@link #lifecycleLock}. */
  private boolean cancelRequested;
  /** Guarded by {@link #lifecycleLock}. */
  private boolean closed;
  private final @NotNull Map<File, Map<MetadataColumn<?>, Object>> sourceMetadata;
  private final @NotNull AtomicBoolean imported = new AtomicBoolean();

  OptimizationBatchEvaluator(@NotNull MZmineProject sourceProject, File @NotNull [] files,
      @NotNull List<SweepMetric> metrics, @NotNull MetricContext metricContext,
      @NotNull List<FeatureRecord> benchmarkFeatures,
      @NotNull AtomicReference<TaskStatus> externalStatus) {
    // decision: retain isolation for later review; all normal optimizer runs use the active project.
    this(sourceProject, files, metrics, metricContext, benchmarkFeatures, externalStatus,
        ProjectMode.ACTIVE_PROJECT);
  }

  OptimizationBatchEvaluator(@NotNull MZmineProject sourceProject, File @NotNull [] files,
      @NotNull List<SweepMetric> metrics, @NotNull MetricContext metricContext,
      @NotNull List<FeatureRecord> benchmarkFeatures,
      @NotNull AtomicReference<TaskStatus> externalStatus, @NotNull ProjectMode projectMode) {
    this.files = files.clone();
    this.metrics = List.copyOf(metrics);
    this.metricContext = metricContext;
    this.benchmarkFeatures = List.copyOf(benchmarkFeatures);
    this.externalStatus = externalStatus;
    this.projectMode = projectMode;
    evaluationProject = switch (projectMode) {
      case ACTIVE_PROJECT -> sourceProject;
      case ISOLATED_PROJECT -> new MZmineProjectImpl();
    };
    sourceMetadata = projectMode == ProjectMode.ISOLATED_PROJECT
        ? snapshotMetadata(sourceProject.getProjectMetadata()) : Map.of();
  }

  private static @NotNull Map<File, Map<MetadataColumn<?>, Object>> snapshotMetadata(
      @NotNull MetadataTable metadata) {
    final Map<File, Map<MetadataColumn<?>, Object>> snapshot = new HashMap<>();
    for (final MetadataColumn<?> column : metadata.getColumns()) {
      final Map<RawDataFile, Object> values = metadata.getColumnData(column);
      if (values == null) {
        continue;
      }
      values.forEach((file, value) -> snapshot.computeIfAbsent(file.getAbsoluteFilePath(),
          _ -> new HashMap<>()).put(column, value));
    }
    return Map.copyOf(snapshot);
  }

  private @NotNull BatchQueue createEvaluationQueue(final @NotNull WizardSequence sequence) {
    final WorkflowWizardParameterFactory workflow = (WorkflowWizardParameterFactory) sequence.get(
        WizardPart.WORKFLOW).orElseThrow().getFactory();

    // file count is needed to derive some settings in the batch builder.
    sequence.get(WizardPart.DATA_IMPORT).get()
        .setParameter(DataImportWizardParameters.fileNames, files);

    final BatchQueue queue = workflow.getBatchBuilder(sequence).createQueue();
    queue.removeIf(step -> isPostProcessingModule(step.getModule())
        || step.getModule() instanceof AutoSaveBatchModule);
    // decision: match runBatchQueue after wizard overrides; metadata was imported during preparation.
    if (!queue.setImportFiles(files, null, null)) {
      throw new IllegalStateException("Could not set the optimization batch input files");
    }
    if (!BatchUtils.confirmModuleOrderWarnings(queue)) {
      throw new IllegalStateException(
          "Optimization batch was not started because processing-order warnings were declined");
    }
    return queue;
  }

  private static boolean isPostProcessingModule(@NotNull Object module) {
    return module instanceof MultiThreadPeakFinderModule || module instanceof RowsFilterModule
        || module instanceof CorrelateGroupingModule || module instanceof IonNetworkingModule
        || module instanceof LipidAnnotationModule || module instanceof SpectralLibrarySearchModule
        || module instanceof MainSpectralNetworkingModule || module instanceof IsotopeGrouperModule
        || module instanceof CompoundGrouperModule;
  }

  private void waitForBatchToExitLocked() {
    boolean interrupted = false;
    while (batchRunning) {
      try {
        lifecycleLock.wait();
      } catch (InterruptedException e) {
        interrupted = true;
      }
    }
    if (interrupted) {
      Thread.currentThread().interrupt();
    }
  }

  private static void applyDiagnostics(@NotNull FeatureList featureList, @NotNull Solution solution,
      boolean shapeDiagnosticEnabled) {
    if (shapeDiagnosticEnabled) {
      final long shapeStart = System.nanoTime();
      final ShapeScoreDiagnostic.Result shape = ShapeScoreDiagnostic.evaluate(featureList,
          FeatureFilterParameters.DEFAULT_SHAPE_SCORE);
      solution.setAttribute(ShapeScoreDiagnostic.ATTR_REMOVE_PERCENT, shape.wouldRemovePercent());
      solution.setConstraintValue(0, shape.wouldRemovePercent());
      solution.setAttribute(ShapeScoreDiagnostic.ATTR_DOUBLE_PEAK_PERCENT,
          shape.doublePeakPercent());
      solution.setAttribute("Shape score sample", shape.inspected());
      logger.finest("Shape diagnostic: %s (took %.1f s)".formatted(shape,
          (System.nanoTime() - shapeStart) / 1e9));
    }

    final long precisionStart = System.nanoTime();
    final PrecisionDiagnostic.Result precision = PrecisionDiagnostic.evaluate(featureList);
    solution.setAttribute(PrecisionDiagnostic.ATTR_SINGLE_FILE_PERCENT,
        precision.singleFilePercent());
    solution.setAttribute(PrecisionDiagnostic.ATTR_NO_ISOTOPE_PERCENT,
        precision.withoutIsotopesPercent());
    solution.setAttribute(PrecisionDiagnostic.ATTR_MEDIAN_HEIGHT, precision.medianHeight());
    solution.setAttribute(PrecisionDiagnostic.ATTR_LOW_HEIGHT, precision.lowHeight());
    logger.finest("Precision diagnostic: %s (took %.2f s)".formatted(precision,
        (System.nanoTime() - precisionStart) / 1e9));
  }

  int evaluate(@NotNull WizardSequence sequence, @NotNull Solution solution,
      boolean shapeDiagnosticEnabled, @NotNull IntSupplier reserveBatchExecution) {
    ensureNotCanceledOrClosed();
    final BatchQueue queue = createEvaluationQueue(sequence);
    if (projectMode == ProjectMode.ISOLATED_PROJECT) {
      // Private imports inherit the source metadata below; UI-bound followups must never run here.
      disableTrialImportFollowups(queue);
      initializeImports(queue);
    }
    // decision: reserve immediately before launch so a generational algorithm cannot overshoot
    // the full-batch budget between termination checks.
    final int batchExecutionIndex = reserveBatchExecution.getAsInt();
    final BatchTask batchTask = launch(queue, "optimization batch");
    try {
      // BatchTask drains submitted children before returning, including after cancellation.
      runBatch(batchTask);
      if (batchTask.getStatus() == TaskStatus.ERROR) {
        throw new RuntimeException("Batch optimization task failed: " + batchTask.getErrorMessage());
      }
      if (batchTask.isCanceled() || externalStatus.get() != TaskStatus.PROCESSING) {
        throw new RuntimeException("Batch optimization task was canceled");
      }

      final List<FeatureList> createdLists = batchTask.getLatestCreatedFeatureLists();
      if (createdLists == null || createdLists.isEmpty()) {
        throw new IllegalStateException("Optimization batch produced no feature list");
      }
      final FeatureList newest = createdLists.getFirst();
      applyScores(newest, solution);
      applyDiagnostics(newest, solution, shapeDiagnosticEnabled);
      solution.setAttribute(WizardOptimizationProblem.ATTR_BATCH_RUNTIME_SECONDS,
          batchTask.getStepTimes().getLast().secondsToFinish());
      return batchExecutionIndex;
    } finally {
      try {
        switch (projectMode) {
          case ACTIVE_PROJECT -> evaluationProject.removeFeatureLists(batchTask.getResultFeatureLists());
          case ISOLATED_PROJECT -> cleanupFeatureLists();
        }
      } finally {
        finishBatch(batchTask);
      }
    }
  }

  private void initializeImports(@NotNull BatchQueue queue) {
    if (!imported.compareAndSet(false, true)) {
      return;
    }
    final BatchQueue importQueue = new BatchQueue();
    queue.stream().filter(step -> step.getModule() instanceof AllSpectralDataImportModule)
        .findFirst().ifPresent(importQueue::add);
    if (importQueue.isEmpty()) {
      throw new IllegalStateException("Optimization batch has no raw-data import step");
    }
    final BatchTask importTask = launch(importQueue, "raw-data import");
    try {
      runBatch(importTask);
      if (importTask.isCanceled() || importTask.getStatus() == TaskStatus.ERROR) {
        throw new RuntimeException("Isolated raw-data import failed: " + importTask.getErrorMessage());
      }
      copySourceMetadata();
    } finally {
      finishBatch(importTask);
    }
  }

  /** Keep BATCH_LAST selection on repeated imports but do not overwrite copied source metadata. */
  static void disableTrialImportFollowups(@NotNull BatchQueue queue) {
    queue.stream().filter(step -> step.getModule() instanceof AllSpectralDataImportModule)
        .forEach(step -> {
          final var parameters = step.getParameterSet();
          parameters.setParameter(AllSpectralDataImportParameters.metadataFile, false);
          parameters.setParameter(AllSpectralDataImportParameters.extractMetadata, false);
          parameters.setParameter(AllSpectralDataImportParameters.sortAndRecolor, false);
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void copySourceMetadata() {
    copyMetadataByPath(sourceMetadata, evaluationProject);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  static void copyMetadataByPath(@NotNull Map<File, Map<MetadataColumn<?>, Object>> source,
      @NotNull MZmineProject targetProject) {
    final MetadataTable metadata = targetProject.getProjectMetadata();
    metadata.batchUpdate(() -> targetProject.getCurrentRawDataFiles().forEach(file -> {
      final Map<MetadataColumn<?>, Object> values = source.get(file.getAbsoluteFilePath());
      if (values != null) {
        values.forEach((column, value) -> metadata.setValue((MetadataColumn) column, file, value));
      }
    }));
  }

  private void cleanupFeatureLists() {
    final List<FeatureList> featureLists = evaluationProject.getCurrentFeatureLists();
    if (!featureLists.isEmpty()) {
      evaluationProject.removeFeatureLists(featureLists);
    }
  }

  private void ensureNotCanceledOrClosed() {
    synchronized (lifecycleLock) {
      if (closed) {
        throw new IllegalStateException("The optimization evaluator is already closed");
      }
      if (cancelRequested) {
        throw new RuntimeException("Batch optimization task was canceled");
      }
    }
  }

  private @NotNull BatchTask launch(@NotNull BatchQueue queue, @NotNull String description) {
    synchronized (lifecycleLock) {
      ensureNotCanceledOrClosed();
      final BatchTask batchTask = switch (projectMode) {
        case ACTIVE_PROJECT -> BatchModeModule.prepareBatchTask(evaluationProject, queue, Instant.now());
        case ISOLATED_PROJECT -> BatchModeModule.prepareIsolatedBatchTask(evaluationProject, queue,
            Instant.now());
      };
      if (batchTask == null) {
        throw new IllegalStateException("Could not prepare " + description);
      }
      activeBatchTask = batchTask;
      batchRunning = true;
      return batchTask;
    }
  }

  private static void runBatch(final @NotNull BatchTask batchTask) {
    // Keep native authorization, task visibility and concurrent-batch guards while draining here.
    if (MZmineCore.getTaskController().runTaskOnThisThreadBlocking(batchTask) == null) {
      throw new IllegalStateException("The optimization batch could not be submitted");
    }
  }

  private void finishBatch(@NotNull BatchTask batchTask) {
    synchronized (lifecycleLock) {
      if (activeBatchTask == batchTask) {
        activeBatchTask = null;
        batchRunning = false;
        lifecycleLock.notifyAll();
      }
    }
  }

  /** Nonblocking: safe for task-status and UI listeners. */
  void cancel() {
    final @Nullable BatchTask batchTask;
    synchronized (lifecycleLock) {
      cancelRequested = true;
      batchTask = activeBatchTask;
    }
    if (batchTask != null) {
      batchTask.cancel();
    }
  }

  @Override
  public void close() {
    cancel();
    synchronized (lifecycleLock) {
      if (closed) {
        return;
      }
      waitForBatchToExitLocked();
      closed = true;
    }
    if (projectMode == ProjectMode.ISOLATED_PROJECT) {
      cleanupFeatureLists();
      final RawDataFile[] rawDataFiles = evaluationProject.getDataFiles();
      if (rawDataFiles.length > 0) {
        evaluationProject.removeFile(rawDataFiles);
      }
      evaluationProject.clearSpectralLibrary();
    }
  }

  private void applyScores(@NotNull FeatureList featureList, @NotNull Solution solution) {
    int objectiveIndex = 0;
    for (final SweepMetric metric : metrics) {
      solution.setObjectiveValue(objectiveIndex++, metric.evaluate(featureList, metricContext));
      metric.applyAttributes(featureList, solution);
    }

    if (!benchmarkFeatures.isEmpty()) {
      final List<FeatureListRow> rows = featureList.getRowsCopy();
      rows.sort(Comparator.comparing(FeatureListRow::getAverageMZ));
      solution.setAttribute(WizardOptimizationProblem.ATTR_BENCHMARK_FEATURES,
          benchmarkFeatures.stream().parallel().mapToLong(record -> record.getNumMatches(rows))
              .sum());
    }
    solution.setAttribute(WizardOptimizationProblem.ATTR_TOTAL_FEATURES,
        featureList.streamFeatures().count());
    solution.setAttribute("Rows (incl. isotopes)", featureList.getRows().size());
  }
}
