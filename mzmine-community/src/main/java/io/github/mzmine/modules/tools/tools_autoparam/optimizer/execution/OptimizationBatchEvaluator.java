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

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.batchmode.BatchModeModule;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.batchmode.BatchTask;
import io.github.mzmine.modules.dataprocessing.filter_isotopegrouper.IsotopeGrouperModule;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterModule;
import io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.multithreaded.MultiThreadPeakFinderModule;
import io.github.mzmine.modules.dataprocessing.group_compoundgrouper.CompoundGrouperModule;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.corrgrouping.CorrelateGroupingModule;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.MainSpectralNetworkingModule;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkingModule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationModule;
import io.github.mzmine.modules.dataprocessing.id_spectral_library_match.SpectralLibrarySearchModule;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.DataImportWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.MetricContext;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.PrecisionDiagnostic;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.ShapeScoreDiagnostic;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.SweepMetric;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.FeatureRecord;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntSupplier;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.moeaframework.core.Solution;

/**
 * Runs one optimized batch queue and translates its feature list into scores and diagnostics.
 */
final class OptimizationBatchEvaluator {

  private static final Logger logger = Logger.getLogger(OptimizationBatchEvaluator.class.getName());

  private final File @NotNull [] files;
  private final @NotNull List<SweepMetric> metrics;
  private final @NotNull MetricContext metricContext;
  private final @NotNull List<FeatureRecord> benchmarkFeatures;
  private final @NotNull AtomicReference<TaskStatus> externalStatus;

  OptimizationBatchEvaluator(File @NotNull [] files, @NotNull List<SweepMetric> metrics,
      @NotNull MetricContext metricContext, @NotNull List<FeatureRecord> benchmarkFeatures,
      @NotNull AtomicReference<TaskStatus> externalStatus) {
    this.files = files.clone();
    this.metrics = List.copyOf(metrics);
    this.metricContext = metricContext;
    this.benchmarkFeatures = List.copyOf(benchmarkFeatures);
    this.externalStatus = externalStatus;
  }

  private @NotNull BatchQueue createEvaluationQueue(@NotNull WizardSequence sequence) {
    final WorkflowWizardParameterFactory workflow = (WorkflowWizardParameterFactory) sequence.get(
        WizardPart.WORKFLOW).orElseThrow().getFactory();

    // file count is needed to derive some settings in the batch builder.
    sequence.get(WizardPart.DATA_IMPORT).get()
        .setParameter(DataImportWizardParameters.fileNames, files);

    final BatchQueue queue = workflow.getBatchBuilder(sequence).createQueue();
    queue.removeIf(step -> isPostProcessingModule(step.getModule()));
    return queue;
  }

  private static boolean isPostProcessingModule(@NotNull Object module) {
    return module instanceof MultiThreadPeakFinderModule || module instanceof RowsFilterModule
        || module instanceof CorrelateGroupingModule || module instanceof IonNetworkingModule
        || module instanceof LipidAnnotationModule || module instanceof SpectralLibrarySearchModule
        || module instanceof MainSpectralNetworkingModule || module instanceof IsotopeGrouperModule
        || module instanceof CompoundGrouperModule;
  }

  private static void waitForCompletion(@NotNull BatchTask batchTask) {
    while (!batchTask.isFinished() && !batchTask.isCanceled()) {
      try {
        TimeUnit.MILLISECONDS.sleep(200);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Interrupted while waiting for optimized batch", e);
      }
    }
  }

  private static void applyDiagnostics(@NotNull FeatureList featureList, @NotNull Solution solution,
      boolean shapeDiagnosticEnabled) {
    if (shapeDiagnosticEnabled) {
      final long shapeStart = System.nanoTime();
      final ShapeScoreDiagnostic.Result shape = ShapeScoreDiagnostic.evaluate(featureList,
          ShapeScoreDiagnostic.STRICT_SHAPE_SCORE);
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
    final BatchQueue queue = createEvaluationQueue(sequence);
    final MZmineProject project = ProjectService.getProject();
    // decision: reserve immediately before launch so a generational algorithm cannot overshoot
    // the full-batch budget between termination checks.
    final int batchExecutionIndex = reserveBatchExecution.getAsInt();
    final BatchTask batchTask = BatchModeModule.runBatchQueue(queue, project, files, null, null,
        null, Instant.now(), null, null);

    waitForCompletion(batchTask);
    if (batchTask.isCanceled() || externalStatus.get() != TaskStatus.PROCESSING) {
      throw new RuntimeException("Batch optimization task was canceled");
    }

    final FeatureList newest = batchTask.getLatestCreatedFeatureLists().getFirst();
    applyScores(newest, solution);
    applyDiagnostics(newest, solution, shapeDiagnosticEnabled);
    solution.setAttribute(WizardOptimizationProblem.ATTR_BATCH_RUNTIME_SECONDS,
        batchTask.getStepTimes().getLast().secondsToFinish());
    project.removeFeatureLists(batchTask.getLatestCreatedFeatureLists());
    return batchExecutionIndex;
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
    solution.setAttribute("Total features", featureList.streamFeatures().count());
    solution.setAttribute("Rows (incl. isotopes)", featureList.getRows().size());
  }
}
