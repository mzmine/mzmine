package io.github.mzmine.modules.tools.batch_resource_estimation;

import io.github.mzmine.modules.batchmode.timing.StepMeasurement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Extrapolates the resources of a batch from runs on subsets of its files. Every step is scaled on
 * its own: retained data and temp files grow with all files, the working memory of thread limited
 * steps only with the files that run in parallel, and the working memory of alignment and gap
 * filling with all files. Each scaling is extrapolated from the last segment of the measured
 * curve, which captures steps that level off.
 *
 * @param totalFiles         number of files in the batch
 * @param numThreads         number of threads that the subset runs used and the prediction
 *                           assumes
 * @param baselineHeapGB     application heap without data, added to all heap predictions
 * @param baselineLiveTempGB temp file data without data of the runs
 * @param steps              per step scaling fits
 * @param rows               scaling of the rows of the largest feature list
 * @param measured           the measurements of the subset runs in the form of a prediction,
 *                           ascending by number of files, without the validation run
 * @param extensions         the measurements of the runs that were added after the validation
 *                           run, ascending by number of files
 * @param fitCheck           validation of the model on the largest subset, null with fewer than 3
 *                           subset runs
 * @param validation         comparison of the validation run with the extrapolation of the other
 *                           runs, null if no validation run was done. The final fit includes the
 *                           validation run and the extensions.
 * @param latestCheck        the largest run predicted from all smaller runs and compared to its
 *                           measurement, shows how reliable the extrapolation is. Null without a
 *                           validation run.
 * @param nextRunFiles       files of a further run with twice the files of the largest run, null
 *                           if the largest run already used all files of the batch
 */
public record BatchResourceModel(int totalFiles, int numThreads, double baselineHeapGB,
                                 double baselineLiveTempGB, @NotNull List<StepScaling> steps,
                                 @NotNull ScalingFit rows,
                                 @NotNull List<BatchResourcePrediction> measured,
                                 @NotNull List<BatchResourcePrediction> extensions,
                                 @Nullable FitCheck fitCheck,
                                 @Nullable ModelValidation validation,
                                 @Nullable FitCheck latestCheck, @Nullable Integer nextRunFiles) {

  // decision: G1 needs free heap to work efficiently, keep the live data below 2/3 of -Xmx
  static final double GC_HEADROOM_FACTOR = 1.5;

  /**
   * Fits the model without a validation run.
   */
  public static @NotNull BatchResourceModel fit(@NotNull final List<SubsetRun> runs,
      @NotNull final List<StepScalingMode> modes, final int totalFiles, final int numThreads) {
    return fit(runs, null, List.of(), modes, totalFiles, numThreads);
  }

  /**
   * Fits the model without extension runs.
   */
  public static @NotNull BatchResourceModel fit(@NotNull final List<SubsetRun> runs,
      @Nullable final SubsetRun validationRun, @NotNull final List<StepScalingMode> modes,
      final int totalFiles, final int numThreads) {
    return fit(runs, validationRun, List.of(), modes, totalFiles, numThreads);
  }

  /**
   * @param runs          subset runs of the same batch, ascending by size
   * @param validationRun a larger run that validates the extrapolation of the other runs and is
   *                      added to the final fit, null if none was done
   * @param extensionRuns runs larger than the validation run that were added because a step did
   *                      not level off, ascending by size. Part of the final fit.
   * @param modes         scaling mode of each step, same order as {@link SubsetRun#steps()}
   * @param totalFiles    number of files in the batch
   * @param numThreads    number of threads the runs used
   */
  public static @NotNull BatchResourceModel fit(@NotNull final List<SubsetRun> runs,
      @Nullable final SubsetRun validationRun, @NotNull final List<SubsetRun> extensionRuns,
      @NotNull final List<StepScalingMode> modes, final int totalFiles, final int numThreads) {
    if (runs.isEmpty()) {
      throw new IllegalArgumentException("Needs at least one subset run");
    }
    final List<SubsetRun> allRuns = new ArrayList<>(runs);
    if (validationRun != null) {
      allRuns.add(validationRun);
    }
    allRuns.addAll(extensionRuns);
    // decision: one common baseline for all runs. The baseline before a run may still contain
    // garbage of the previous run that is collected during the run. Subtracting it would hide the
    // growth of the larger subsets, so the lowest baseline is the application without data.
    final double heapBaseline = allRuns.stream().mapToDouble(SubsetRun::baselineHeapGB).min()
        .orElse(0);
    final double tempBaseline = allRuns.stream().mapToDouble(SubsetRun::baselineLiveTempGB).min()
        .orElse(0);
    final List<SubsetRun> normalized = runs.stream()
        .map(run -> run.withBaselines(heapBaseline, tempBaseline)).toList();

    final BatchResourceModel model = fitSteps(normalized, modes, totalFiles, numThreads);
    final List<BatchResourcePrediction> measured = normalized.stream()
        .map(model::measuredAsPrediction).toList();
    FitCheck check = null;
    if (normalized.size() >= 3) {
      final SubsetRun largest = normalized.getLast();
      final BatchResourcePrediction predicted = fitSteps(
          normalized.subList(0, normalized.size() - 1), modes, totalFiles, numThreads).predict(
          largest.files(), largest.fileEquivalents());
      check = compare(predicted, measured.getLast());
    }
    if (validationRun == null) {
      return new BatchResourceModel(totalFiles, numThreads, heapBaseline, tempBaseline,
          model.steps, model.rows, measured, List.of(), check, null, null, null);
    }

    final SubsetRun normalizedValidation = validationRun.withBaselines(heapBaseline,
        tempBaseline);
    final BatchResourcePrediction predicted = model.predict(normalizedValidation.files(),
        normalizedValidation.fileEquivalents());
    final BatchResourcePrediction measuredValidation = model.measuredAsPrediction(
        normalizedValidation);
    final ModelValidation validation = new ModelValidation(predicted, measuredValidation,
        compare(predicted, measuredValidation));
    // the validation run and the extensions extend the measured range, so they are part of the
    // final fit
    final List<SubsetRun> finalRuns = new ArrayList<>(normalized);
    finalRuns.add(normalizedValidation);
    extensionRuns.stream().map(run -> run.withBaselines(heapBaseline, tempBaseline))
        .forEach(finalRuns::add);
    final BatchResourceModel finalModel = fitSteps(finalRuns, modes, totalFiles, numThreads);
    final List<BatchResourcePrediction> extensions = finalRuns.subList(normalized.size() + 1,
        finalRuns.size()).stream().map(finalModel::measuredAsPrediction).toList();

    // decision: the reliability of the extrapolation is checked on the largest run, predicted from
    // all smaller runs. Equals the validation without extensions.
    final SubsetRun largest = finalRuns.getLast();
    final FitCheck latestCheck = compare(
        fitSteps(finalRuns.subList(0, finalRuns.size() - 1), modes, totalFiles,
            numThreads).predict(largest.files(), largest.fileEquivalents()),
        finalModel.measuredAsPrediction(largest));
    final int nextRunFiles = Math.min(largest.files() * 2, totalFiles);
    return new BatchResourceModel(totalFiles, numThreads, heapBaseline, tempBaseline,
        finalModel.steps, finalModel.rows, measured, extensions, check, validation, latestCheck,
        nextRunFiles > largest.files() ? nextRunFiles : null);
  }

  private static @NotNull FitCheck compare(@NotNull final BatchResourcePrediction predicted,
      @NotNull final BatchResourcePrediction measured) {
    return new FitCheck(measured.files(), predicted.heapGB(), measured.heapGB(),
        predicted.tempGB(), measured.tempGB(), predicted.seconds(), measured.seconds());
  }

  /**
   * @param runs runs with the common baselines, ascending by size
   */
  private static @NotNull BatchResourceModel fitSteps(@NotNull final List<SubsetRun> runs,
      @NotNull final List<StepScalingMode> modes, final int totalFiles, final int numThreads) {
    final int numSteps = modes.size();
    if (runs.stream().anyMatch(run -> run.steps().size() != numSteps)) {
      throw new IllegalArgumentException("All subset runs need the same batch steps");
    }
    final SubsetRun first = runs.getFirst();
    final List<StepScaling> steps = IntStream.range(0, numSteps)
        .mapToObj(i -> StepScaling.fit(runs, i, modes.get(i), numThreads)).toList();
    final ScalingFit rows = ScalingFit.fitLastSegment(
        runs.stream().mapToDouble(SubsetRun::fileEquivalents).toArray(),
        runs.stream().mapToDouble(SubsetRun::maxRows).toArray());
    return new BatchResourceModel(totalFiles, numThreads, first.baselineHeapGB(),
        first.baselineLiveTempGB(), steps, rows, List.of(), List.of(), null, null, null, null);
  }

  /**
   * Predicts the resources for a number of files with the mean file size of the batch.
   */
  public @NotNull BatchResourcePrediction predict(final int files) {
    return predict(files, files);
  }

  private @NotNull BatchResourcePrediction predict(final int files, final double fileEquivalents) {
    final List<StepResourcePrediction> predictions = new ArrayList<>(steps.size());
    final double alignedRows = rows.predict(fileEquivalents);
    for (final StepScaling step : steps) {
      final double retained = baselineHeapGB + step.retained().predict(fileEquivalents);
      final double working = step.predictWorking(fileEquivalents, files, numThreads, alignedRows);
      final double seconds = step.seconds()
          .predict(StepScaling.timeX(step.mode(), fileEquivalents, numThreads, alignedRows));
      predictions.add(
          new StepResourcePrediction(step.step(), step.name(), step.mode().toString(),
              round3(retained + working), round3(retained),
              round3(step.liveTemp().predict(fileEquivalents)), round3(seconds)));
    }
    return toBatchPrediction(files, predictions,
        (int) Math.round(rows.predict(fileEquivalents)));
  }

  /**
   * The measured values of a run in the same form as a prediction, to compare both.
   */
  private @NotNull BatchResourcePrediction measuredAsPrediction(@NotNull final SubsetRun run) {
    final List<StepResourcePrediction> measured = new ArrayList<>(steps.size());
    for (int i = 0; i < steps.size(); i++) {
      final StepMeasurement m = run.steps().get(i);
      measured.add(new StepResourcePrediction(m.step(), m.name(), steps.get(i).mode().toString(),
          StepScaling.livePeakGB(m),
          run.baselineHeapGB() + StepScaling.retainedAboveBaseline(run, i),
          Math.max(0, m.liveTempFileUsedGB() - run.baselineLiveTempGB()), m.secondsToFinish()));
    }
    return toBatchPrediction(run.files(), measured, run.maxRows());
  }

  private static @NotNull BatchResourcePrediction toBatchPrediction(final int files,
      @NotNull final List<StepResourcePrediction> steps, final int rows) {
    final StepResourcePrediction heapPeakStep = steps.stream()
        .max(Comparator.comparingDouble(StepResourcePrediction::heapGB)).orElse(null);
    final double heap = heapPeakStep == null ? 0 : heapPeakStep.heapGB();
    final StepResourcePrediction retainedPeakStep = steps.stream()
        .max(Comparator.comparingDouble(StepResourcePrediction::retainedHeapGB)).orElse(null);
    final double retained = retainedPeakStep == null ? 0 : retainedPeakStep.retainedHeapGB();
    final double temp = steps.stream().mapToDouble(StepResourcePrediction::liveTempGB).max()
        .orElse(0);
    final double seconds = steps.stream().mapToDouble(StepResourcePrediction::seconds).sum();
    final int xmx = (int) Math.ceil(heap * GC_HEADROOM_FACTOR);
    return new BatchResourcePrediction(files, steps, round3(heap), round3(retained), round3(temp),
        round3(seconds), rows, xmx, heapPeakStep == null ? "" : heapPeakStep.name(),
        retainedPeakStep == null ? "" : retainedPeakStep.name());
  }

  private static double round3(final double value) {
    return Math.round(value * 1e3) / 1e3;
  }
}
