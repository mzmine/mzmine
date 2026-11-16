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

package io.github.mzmine.modules.tools.batch_resource_estimation;

import io.github.mzmine.modules.batchmode.timing.StepMeasurement;
import java.util.List;
import java.util.function.ToDoubleFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Scaling fits of one batch step, fitted over all subset runs. The heap of a step is split into
 * the retained data that stays in memory and grows with all files (raw data, mass lists, feature
 * lists) and the heap on top of it while the step runs.
 *
 * @param step     1-based step number
 * @param name     module name of the step
 * @param mode     how the heap on top of the retained data and the time grow
 * @param retained heap above the baseline that is alive before or after the step, over file
 *                 equivalents (GB)
 * @param working  peak live heap during the step above the retained heap, over
 *                 {@link #workingX(StepScalingMode, double, int, int, double)} (GB). Used for
 *                 thread limited steps only if {@link #inStep} is null.
 * @param inStep   for thread limited steps: the heap above the retained heap split into the
 *                 working memory of the threads and the data that grows with the files during the
 *                 step. Null if fewer than two runs kept all threads busy.
 * @param liveTemp live temp file data at the end of the step above the baseline, over file
 *                 equivalents (GB)
 * @param seconds  wall clock time of the step, over
 *                 {@link #timeX(StepScalingMode, double, int, double)}
 * @param heapReliable all input runs observed a full collection during this step
 */
record StepScaling(int step, @NotNull String name, @NotNull StepScalingMode mode,
                          @NotNull ScalingFit retained, @NotNull ScalingFit working,
                          @Nullable InStepMemory inStep, @NotNull ScalingFit liveTemp,
                          @NotNull ScalingFit seconds, boolean heapReliable) {

  /**
   * @param runs       subset runs, ascending by size
   * @param index      index of the step in {@link SubsetRun#steps()}
   * @param numThreads number of threads that run tasks in parallel
   */
  static @NotNull StepScaling fit(@NotNull final List<SubsetRun> runs, final int index,
      @NotNull final StepScalingMode mode, final int numThreads) {
    final StepMeasurement first = runs.getFirst().steps().get(index);
    final double[] fileEquivalents = runs.stream().mapToDouble(SubsetRun::fileEquivalents)
        .toArray();
    final double[] retained = runs.stream()
        .mapToDouble(run -> retainedAboveBaseline(run, index)).toArray();
    final double[] working = runs.stream().mapToDouble(run -> workingGB(run, index)).toArray();
    final double[] workingX = runs.stream().mapToDouble(
        run -> workingX(mode, run.fileEquivalents(), run.files(), numThreads, run.maxRows()))
        .toArray();
    final double[] timeX = runs.stream()
        .mapToDouble(run -> timeX(mode, run.fileEquivalents(), numThreads, run.maxRows()))
        .toArray();
    final double[] liveTemp = values(runs, index, StepMeasurement::liveTempFileUsedGB,
        SubsetRun::baselineLiveTempGB);
    final double[] seconds = values(runs, index, StepMeasurement::secondsToFinish, _ -> 0);

    // only runs that keep all threads busy separate the working memory of the threads from the
    // input and results of the step that are alive at the same time and grow with the files
    final List<SubsetRun> saturated = runs.stream().filter(run -> run.files() >= numThreads)
        .toList();
    final InStepMemory inStep = switch (mode) {
      case SCALES_WITH_THREADS -> InStepMemory.fit(
          saturated.stream().mapToDouble(SubsetRun::fileEquivalents).toArray(),
          saturated.stream().mapToDouble(run -> workingGB(run, index)).toArray());
      case SCALES_WITH_FILES -> null;
    };

    // decision: extrapolate from the last segment, which describes the growth beyond the largest
    // run better than the steep growth of the small subsets
    return new StepScaling(first.step(), first.name(), mode,
        ScalingFit.fitLastSegment(fileEquivalents, retained),
        ScalingFit.fitLastSegment(workingX, working), inStep,
        ScalingFit.fitLastSegment(fileEquivalents, liveTemp),
        ScalingFit.fitLastSegment(timeX, seconds), runs.stream()
        .allMatch(run -> run.steps().get(index).heapMeasurementQuality().isReliable()));
  }

  /**
   * @param fileEquivalents dataset size in mean files
   * @param files           number of files, limits the parallel tasks
   * @param rows            rows of the aligned feature list
   * @return the heap on top of the retained data while the step runs
   */
  double predictWorking(final double fileEquivalents, final int files, final int numThreads,
      final double rows) {
    return switch (mode) {
      case SCALES_WITH_THREADS -> inStep != null
          ? inStep.predict(fileEquivalents, files, numThreads)
          : working.predict(workingX(mode, fileEquivalents, files, numThreads, rows));
      case SCALES_WITH_FILES -> working.predict(workingX(mode, fileEquivalents, files,
          numThreads, rows));
    };
  }

  /**
   * @param fileEquivalents dataset size in mean files
   * @param files           number of files, limits the parallel tasks
   * @param rows            rows of the aligned feature list
   * @return x of the working memory fit
   */
  static double workingX(@NotNull final StepScalingMode mode, final double fileEquivalents,
      final int files, final int numThreads, final double rows) {
    return switch (mode) {
      // the files that run in parallel, weighted by their mean size relative to the dataset
      case SCALES_WITH_THREADS -> Math.min(files, numThreads) * fileEquivalents / files;
      case SCALES_WITH_FILES -> featureMatrixSize(fileEquivalents, rows);
    };
  }

  /**
   * @param rows rows of the aligned feature list
   * @return x of the time fit
   */
  static double timeX(@NotNull final StepScalingMode mode, final double fileEquivalents,
      final int numThreads, final double rows) {
    return switch (mode) {
      // assumption: below the thread count all tasks run at once and take about one task's time,
      // above it the time grows with the number of thread waves
      case SCALES_WITH_THREADS -> Math.max(1, fileEquivalents / numThreads);
      case SCALES_WITH_FILES -> featureMatrixSize(fileEquivalents, rows);
    };
  }

  /**
   * decision: alignment, gap filling and correlation grouping work on the aligned feature list
   * with one feature per row and file, e.g. correlation grouping caches the intensities of all
   * features. Their memory and time grow with this matrix and not with the files alone, as the
   * rows grow slower than the files.
   *
   * @return rows times file equivalents in millions
   */
  static double featureMatrixSize(final double fileEquivalents, final double rows) {
    return Math.max(1, rows) * fileEquivalents / 1e6;
  }

  /**
   * @return peak live heap of the step above the retained heap and the baseline
   */
  static double workingGB(@NotNull final SubsetRun run, final int index) {
    return Math.max(0, livePeakGB(run.steps().get(index)) - run.baselineHeapGB()
        - retainedAboveBaseline(run, index));
  }

  /**
   * The highest live heap during the step. Falls back to the peak heap, an upper bound, if no GC
   * happened during the step.
   */
  static double livePeakGB(@NotNull final StepMeasurement measurement) {
    final Double afterGc = measurement.peakHeapAfterGcGB();
    return afterGc != null ? afterGc : measurement.peakHeapGB();
  }

  /**
   * The input of a step is alive until the step finished and its output after, so the larger of
   * the retained heap before and after the step is part of the step peak.
   */
  static double retainedAboveBaseline(@NotNull final SubsetRun run, final int index) {
    final double after = retainedGB(run.steps().get(index), run.baselineHeapGB());
    final double before =
        index == 0 ? run.baselineHeapGB() : retainedGB(run.steps().get(index - 1),
            run.baselineHeapGB());
    return Math.max(0, Math.max(before, after) - run.baselineHeapGB());
  }

  /**
   * @return used heap after the GC at the end of the step, the baseline if not tracked
   */
  private static double retainedGB(@NotNull final StepMeasurement measurement,
      final double baselineGB) {
    final Double used = parseGB(measurement.usedHeapGB());
    return used != null ? used : baselineGB;
  }

  /**
   * The used heap is formatted with the default locale, which may use a decimal comma.
   */
  static @Nullable Double parseGB(@Nullable final String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Double.parseDouble(value.strip().replace(',', '.'));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static double @NotNull [] values(@NotNull final List<SubsetRun> runs, final int index,
      @NotNull final ToDoubleFunction<StepMeasurement> value,
      @NotNull final ToDoubleFunction<SubsetRun> baseline) {
    return runs.stream().mapToDouble(
        run -> Math.max(0,
            value.applyAsDouble(run.steps().get(index)) - baseline.applyAsDouble(run))).toArray();
  }
}
