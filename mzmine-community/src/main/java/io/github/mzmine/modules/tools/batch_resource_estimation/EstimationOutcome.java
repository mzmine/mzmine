package io.github.mzmine.modules.tools.batch_resource_estimation;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The verdict of a resource estimation for a number of files with the reasons and the advice.
 *
 * @param verdict      overall result
 * @param details      reasons and advice for the user, one sentence per entry
 * @param nextRunFiles files of the larger test run for {@link EstimationVerdict#LARGER_RUN_NEEDED},
 *                     null otherwise
 */
public record EstimationOutcome(@NotNull EstimationVerdict verdict, @NotNull List<String> details,
                                @Nullable Integer nextRunFiles) {

  // decision: the largest test run may deviate by this fraction from its prediction for a
  // reliable estimate
  static final double MAX_RELIABLE_DEVIATION = 0.25;

  /**
   * @param maxHeapGB     currently configured maximum heap (-Xmx)
   * @param tempDirFreeGB free space in the temp directory
   */
  public static @NotNull EstimationOutcome of(@NotNull final BatchResourceModel model,
      @NotNull final BatchResourcePrediction prediction, final double maxHeapGB,
      final double tempDirFreeGB) {
    // decision: the heap needs the GC headroom of the recommended -Xmx
    final boolean heapEnough = prediction.recommendedXmxGB() <= maxHeapGB;
    final boolean tempEnough = prediction.tempGB() <= tempDirFreeGB;
    final FitCheck check = model.latestCheck();
    final boolean reliable = check == null || check.maxResourceError() <= MAX_RELIABLE_DEVIATION;

    final List<String> details = new ArrayList<>();
    details.add(("Processing %d files needs about %.0f GB heap (-Xmx %d GB with headroom, %.0f GB "
        + "configured) and %.0f GB temp space (%.0f GB free).").formatted(prediction.files(),
        prediction.heapGB(), prediction.recommendedXmxGB(), maxHeapGB, prediction.tempGB(),
        tempDirFreeGB));
    if (check != null) {
      details.add("The largest test run with %d files deviated %.0f %% from its prediction%s."
          .formatted(check.files(), check.maxResourceError() * 100,
              reliable ? ", the estimate is reliable" : ", the estimate is less reliable"));
    }

    if (heapEnough && tempEnough) {
      return new EstimationOutcome(reliable ? EstimationVerdict.ENOUGH_RESOURCES
          : EstimationVerdict.ENOUGH_RESOURCES_UNCERTAIN, details, null);
    }
    final Integer nextRunFiles = model.nextRunFiles();
    if (!reliable && nextRunFiles != null) {
      details.add("A test run with %d files improves the estimate before you change the settings."
          .formatted(nextRunFiles));
      return new EstimationOutcome(EstimationVerdict.LARGER_RUN_NEEDED, details, nextRunFiles);
    }
    details.add(advice(heapEnough, tempEnough));
    return new EstimationOutcome(EstimationVerdict.NOT_ENOUGH_RESOURCES, details, null);
  }

  private static @NotNull String advice(final boolean heapEnough, final boolean tempEnough) {
    final List<String> options = new ArrayList<>();
    if (!heapEnough) {
      options.add("increase -Xmx (needs that much RAM)");
    }
    if (!tempEnough) {
      options.add("free space or choose a larger temp directory in the preferences");
    }
    options.add("use stricter feature detection settings, e.g. a higher noise level or minimum "
        + "feature height, to reduce the features");
    options.add("process fewer files per batch");
    return "Options: " + String.join("; ", options) + ".";
  }
}
