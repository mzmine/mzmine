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

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.CsvWriter;
import io.github.mzmine.util.io.WriterOptions;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Text output of the resource estimation, shared by the log and the result dialog.
 */
final class BatchResourceReport {

  private static final Logger logger = Logger.getLogger(BatchResourceReport.class.getName());

  private BatchResourceReport() {
  }

  /**
   * @param maxHeapGB     currently configured maximum heap (-Xmx)
   * @param tempDirFreeGB free space in the temp directory
   * @return resource statistics without the verdict, heap reliability or validation, see
   * {@link EstimationOutcome}
   */
  static @NotNull List<String> summaryLines(
      @NotNull final BatchResourcePrediction prediction, final double maxHeapGB,
      final double tempDirFreeGB) {
    final List<String> lines = new ArrayList<>();
    final StepResourcePrediction reliablePeak = prediction.reliableHeapPeak();
    if (reliablePeak == null) {
      lines.add("Peak RAM: unknown (no reliable step measurements)");
    } else {
      lines.add("%s: %.1f GB (%s)\nHighest use: %s".formatted(
          prediction.heapReliable() ? "Peak RAM" : "Peak RAM from reliable steps",
          reliablePeak.heapGB(), xmxRecommendation(prediction, maxHeapGB), reliablePeak.name()));
    }
    lines.add(
        "RAM kept between steps: %.1f GB\nHighest use: %s".formatted(prediction.retainedHeapGB(),
        prediction.retainedBottleneckStep()));
    lines.add("Temporary storage: %.1f GB (%.1f GB free)".formatted(
        prediction.tempGB(), tempDirFreeGB));
    lines.add("Processing time: %s, aligned feature rows: %d".formatted(
        formatDuration(prediction.seconds()), prediction.rows()));
    return lines;
  }

  /**
   * The latest validation, shown only in the verdict banner.
   */
  static @NotNull List<String> reliabilityLines(@NotNull final BatchResourceModel model) {
    final List<String> lines = new ArrayList<>();
    final FitCheck check = model.latestCheck() != null ? model.latestCheck() : model.fitCheck();
    if (check != null) {
      lines.add(("Validation run on %d files (estimated / measured): RAM %.2f / %.2f GB (%s), "
          + "temporary storage %.2f / %.2f GB (%s). "
          + "This run is included in the estimate.").formatted(check.files(),
          check.predictedHeapGB(), check.measuredHeapGB(),
          measuredDifference(check.predictedHeapGB(), check.measuredHeapGB()),
          check.predictedTempGB(), check.measuredTempGB(),
          measuredDifference(check.predictedTempGB(), check.measuredTempGB())));
    }
    return lines;
  }

  /**
   * Describes measured usage relative to the estimate, so e.g. 0.5 GB measured vs 1 GB estimated
   * means 50% less was needed. This is separate from the model's validation error threshold.
   */
  static @NotNull String measuredDifference(final double estimated, final double measured) {
    if (estimated <= 0) {
      return measured <= 0 ? "as estimated" : "more than estimated";
    }
    final long percent = Math.round(Math.abs(measured - estimated) / estimated * 100);
    if (percent == 0) {
      return "about as estimated";
    }
    return "%d%% %s than estimated".formatted(percent, measured < estimated ? "less" : "more");
  }

  /**
   * decision: only recommend a higher -Xmx than configured, a lower value does not help the batch
   */
  static @NotNull String xmxRecommendation(@NotNull final BatchResourcePrediction prediction,
      final double maxHeapGB) {
    final StepResourcePrediction reliablePeak = prediction.reliableHeapPeak();
    if (reliablePeak == null) {
      return "RAM requirement uncertain, %.1f GB available to MZmine".formatted(maxHeapGB);
    }
    final int recommendedXmxGB = (int) Math.ceil(
        reliablePeak.heapGB() * BatchResourceModel.GC_HEADROOM_FACTOR);
    if (recommendedXmxGB > maxHeapGB) {
      return "increase MZmine's RAM limit to at least %d GB, currently %.1f GB".formatted(
          recommendedXmxGB, maxHeapGB);
    }
    return "%.1f GB available to MZmine".formatted(maxHeapGB);
  }

  static double maxHeapGB() {
    return Runtime.getRuntime().maxMemory() / (double) (1 << 30);
  }

  /**
   * Reads the disk, call from a task and not on UI events.
   */
  static double tempDirFreeGB() {
    return FileAndPathUtil.getTempDir().getUsableSpace() / 1e9;
  }

  /**
   * Logs the verdict, the summary and the per step prediction for the files of the batch.
   */
  static void logPrediction(@NotNull final BatchResourceModel model) {
    final BatchResourcePrediction prediction = model.predict(model.totalFiles());
    final double maxHeapGB = maxHeapGB();
    final double tempFreeGB = tempDirFreeGB();
    final EstimationOutcome outcome = EstimationOutcome.of(model, prediction, maxHeapGB,
        tempFreeGB);
    logger.info("Batch resource estimation for %d files (%d threads): %s\n%s\n%s\n%s".formatted(
        prediction.files(), model.numThreads(), outcome.verdict().title(),
        String.join("\n", outcome.details()),
        String.join("\n", summaryLines(prediction, maxHeapGB, tempFreeGB)),
        stepsCsv(prediction)));
  }

  static @NotNull String stepsCsv(@NotNull final BatchResourcePrediction prediction) {
    return CsvWriter.writeToString(prediction.steps(), StepResourcePrediction.class, '\t', true)
        .stripTrailing();
  }

  /**
   * Replaces the optional TSV with all measured runs, validation, and the latest prediction for the
   * full batch. Called on the task thread after the initial estimation and each added run.
   */
  static void exportStatistics(@NotNull final ParameterSet parameters,
      @NotNull final BatchResourceModel model) throws IOException {
    final File output = parameters.getEmbeddedParameterValueIfSelectedOrElse(
        BatchResourceEstimationParameters.statisticsFile, null);
    if (output == null) {
      return;
    }
    final List<StatisticsRow> rows = new ArrayList<>();
    for (final BatchResourcePrediction run : model.measured()) {
      addStatistics(rows, "Subset (measured)", run, model.numThreads());
    }
    final ModelValidation validation = model.validation();
    if (validation != null) {
      addStatistics(rows, "Validation (measured)", validation.measured(), model.numThreads());
      addStatistics(rows, "Validation (estimated)", validation.predicted(), model.numThreads());
    }
    for (final BatchResourcePrediction run : model.extensions()) {
      addStatistics(rows, "Additional run (measured)", run, model.numThreads());
    }
    addStatistics(rows, "Full batch (estimated)", model.predict(model.totalFiles()),
        model.numThreads());
    CsvWriter.writeToFile(output.getAbsoluteFile(), rows, StatisticsRow.class,
        WriterOptions.REPLACE, '\t');
  }

  private static void addStatistics(@NotNull final List<StatisticsRow> rows,
      @NotNull final String runType, @NotNull final BatchResourcePrediction prediction,
      final int threads) {
    for (final StepResourcePrediction step : prediction.steps()) {
      rows.add(
          new StatisticsRow(runType, prediction.files(), threads, prediction.rows(), step.step(),
              step.name(), step.scaling(), step.heapGB(), step.heapReliability(),
              step.retainedHeapGB(), step.liveTempGB(), step.seconds()));
    }
  }

  @JsonPropertyOrder({"runType", "files", "threads", "alignedFeatureRows", "step", "module",
      "dependsOn", "peakRamGB", "ramReliability", "retainedRamGB", "temporaryStorageGB",
      "processingSeconds"})
  private record StatisticsRow(String runType, int files, int threads, int alignedFeatureRows,
                               int step, String module, String dependsOn, double peakRamGB,
                               String ramReliability, double retainedRamGB,
                               double temporaryStorageGB, double processingSeconds) {

  }

  static @NotNull String formatDuration(final double seconds) {
    final Duration duration = Duration.ofSeconds(Math.round(seconds));
    return "%d:%02d:%02d".formatted(duration.toHours(), duration.toMinutesPart(),
        duration.toSecondsPart());
  }
}
