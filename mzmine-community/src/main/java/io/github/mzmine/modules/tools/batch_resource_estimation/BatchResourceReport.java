package io.github.mzmine.modules.tools.batch_resource_estimation;

import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.CsvWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Text output of the resource estimation, shared by the log and the result dialog.
 */
public final class BatchResourceReport {

  private static final Logger logger = Logger.getLogger(BatchResourceReport.class.getName());

  private BatchResourceReport() {
  }

  /**
   * @param maxHeapGB     currently configured maximum heap (-Xmx)
   * @param tempDirFreeGB free space in the temp directory
   * @return human readable summary lines without the verdict, see {@link EstimationOutcome}
   */
  public static @NotNull List<String> summaryLines(@NotNull final BatchResourceModel model,
      @NotNull final BatchResourcePrediction prediction, final double maxHeapGB,
      final double tempDirFreeGB) {
    final List<String> lines = new ArrayList<>();
    lines.add("Estimate for %d files (%d threads)".formatted(prediction.files(),
        model.numThreads()));

    lines.add("Peak heap: %.1f GB in step \"%s\", %s".formatted(prediction.heapGB(),
        prediction.heapBottleneckStep(), xmxRecommendation(prediction, maxHeapGB)));
    lines.add("Retained heap: %.1f GB in step \"%s\"".formatted(prediction.retainedHeapGB(),
        prediction.retainedBottleneckStep()));
    lines.add("Temp files: %.1f GB (free in temp directory: %.1f GB)".formatted(
        prediction.tempGB(), tempDirFreeGB));
    lines.add("Processing time: %s, aligned rows: %d".formatted(
        formatDuration(prediction.seconds()), prediction.rows()));

    final FitCheck check = model.fitCheck();
    if (check != null) {
      lines.add(("Validation on %d files predicted from the smaller subsets: heap %.2f / %.2f GB, "
          + "temp %.2f / %.2f GB, time %s / %s (predicted / measured)").formatted(check.files(),
          check.predictedHeapGB(), check.measuredHeapGB(), check.predictedTempGB(),
          check.measuredTempGB(), formatDuration(check.predictedSeconds()),
          formatDuration(check.measuredSeconds())));
    }
    final ModelValidation validation = model.validation();
    if (validation != null) {
      final FitCheck totals = validation.totals();
      lines.add(("Validation run on %d files predicted from the subsets: heap %.2f / %.2f GB, "
          + "temp %.2f / %.2f GB, time %s / %s (predicted / measured), max deviation %.0f %%. "
          + "The extrapolation includes this run.").formatted(totals.files(),
          totals.predictedHeapGB(), totals.measuredHeapGB(), totals.predictedTempGB(),
          totals.measuredTempGB(), formatDuration(totals.predictedSeconds()),
          formatDuration(totals.measuredSeconds()), totals.maxRelativeError() * 100));
    }
    lines.addAll(latestCheckLines(model));
    return lines;
  }

  /**
   * decision: only recommend a higher -Xmx than configured, a lower value does not help the batch
   */
  static @NotNull String xmxRecommendation(@NotNull final BatchResourcePrediction prediction,
      final double maxHeapGB) {
    if (prediction.recommendedXmxGB() > maxHeapGB) {
      return "recommended -Xmx %d GB (configured: %.1f GB)".formatted(
          prediction.recommendedXmxGB(), maxHeapGB);
    }
    return "configured -Xmx %.1f GB is enough".formatted(maxHeapGB);
  }


  /**
   * The check of the largest run if runs were added after the validation run.
   */
  static @NotNull List<String> latestCheckLines(@NotNull final BatchResourceModel model) {
    final FitCheck check = model.latestCheck();
    if (check == null || model.extensions().isEmpty()) {
      return List.of();
    }
    return List.of(("Largest run on %d files predicted from the smaller runs: heap %.2f / %.2f GB, "
        + "temp %.2f / %.2f GB, time %s / %s (predicted / measured)").formatted(check.files(),
        check.predictedHeapGB(), check.measuredHeapGB(), check.predictedTempGB(),
        check.measuredTempGB(), formatDuration(check.predictedSeconds()),
        formatDuration(check.measuredSeconds())));
  }

  public static double maxHeapGB() {
    return Runtime.getRuntime().maxMemory() / (double) (1 << 30);
  }

  /**
   * Reads the disk, call from a task and not on UI events.
   */
  public static double tempDirFreeGB() {
    return FileAndPathUtil.getTempDir().getUsableSpace() / 1e9;
  }

  /**
   * Logs the verdict, the summary and the per step prediction for the files of the batch.
   */
  public static void logPrediction(@NotNull final BatchResourceModel model) {
    final BatchResourcePrediction prediction = model.predict(model.totalFiles());
    final double maxHeapGB = maxHeapGB();
    final double tempFreeGB = tempDirFreeGB();
    final EstimationOutcome outcome = EstimationOutcome.of(model, prediction, maxHeapGB,
        tempFreeGB);
    logger.info("Batch resource estimation: %s\n%s\n%s\n%s".formatted(outcome.verdict().title(),
        String.join("\n", outcome.details()),
        String.join("\n", summaryLines(model, prediction, maxHeapGB, tempFreeGB)),
        stepsCsv(prediction)));
  }

  public static @NotNull String stepsCsv(@NotNull final BatchResourcePrediction prediction) {
    return CsvWriter.writeToString(prediction.steps(), StepResourcePrediction.class, '\t', true)
        .stripTrailing();
  }

  public static @NotNull String formatDuration(final double seconds) {
    final Duration duration = Duration.ofSeconds(Math.round(seconds));
    return "%d:%02d:%02d".formatted(duration.toHours(), duration.toMinutesPart(),
        duration.toSecondsPart());
  }
}
