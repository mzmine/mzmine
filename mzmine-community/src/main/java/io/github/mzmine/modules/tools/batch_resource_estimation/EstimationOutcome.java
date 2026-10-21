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

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The verdict of a resource estimation for a number of files with heap reliability and validation.
 *
 * @param verdict      overall result
 * @param details      heap reliability and the latest validation for the banner
 * @param nextRunFiles files of an optional larger test run for an uncertain result, null otherwise
 */
record EstimationOutcome(@NotNull EstimationVerdict verdict, @NotNull List<String> details,
                                @Nullable Integer nextRunFiles) {

  // decision: only underestimates reduce confidence in resource sufficiency; overestimates are
  // conservative. The tolerance is a fraction of the measured usage.
  static final double MAX_RELIABLE_DEVIATION = 0.25;

  /**
   * @param maxHeapGB     currently configured maximum heap (-Xmx)
   * @param tempDirFreeGB free space in the temp directory
   */
  static @NotNull EstimationOutcome of(@NotNull final BatchResourceModel model,
      @NotNull final BatchResourcePrediction prediction, final double maxHeapGB,
      final double tempDirFreeGB) {
    // decision: the heap needs the GC headroom of the recommended -Xmx
    final boolean heapEnough = prediction.recommendedXmxGB() <= maxHeapGB;
    final boolean tempEnough = prediction.tempGB() <= tempDirFreeGB;
    final FitCheck check = model.latestCheck();
    final boolean validationReliable =
        check == null || check.maxResourceUnderestimation() <= MAX_RELIABLE_DEVIATION;
    final Integer nextRunFiles = model.nextRunFiles();
    final StepResourcePrediction reliablePeak = prediction.reliableHeapPeak();
    final boolean reliableStepsExceedRam = reliablePeak != null
        && Math.ceil(reliablePeak.heapGB() * BatchResourceModel.GC_HEADROOM_FACTOR) > maxHeapGB;
    final boolean peakMeasurementReliable =
        reliablePeak != null && reliablePeak.heapGB() >= prediction.heapGB();
    final boolean reliable = peakMeasurementReliable && validationReliable;

    final List<String> details = new ArrayList<>();
    if (!peakMeasurementReliable) {
      if (!heapEnough && !reliableStepsExceedRam) {
        details.add(("Peak RAM predicts %.1f GB usage, but the estimate is based on unreliable "
            + "measurements: no full memory cleanup was confirmed during some steps. "
            + "These steps may need less RAM than estimated.%s").formatted(
            prediction.heapGB(),
            nextRunFiles == null ? "" : " A larger run may lead to a better estimate."));
      } else {
        details.add("Some peak RAM estimates are not reliable: measurements may include memory "
            + "that was no longer needed. These steps may need less RAM than estimated.");
      }
      if (reliablePeak == null) {
        details.add("No steps have reliable peak RAM measurements. The batch's RAM requirement "
            + "cannot be determined from these runs.");
      } else {
        details.add("The RAM requirement uses only reliable steps. The RAM needed by the other "
            + "steps is unknown.");
      }
    }
    details.addAll(BatchResourceReport.reliabilityLines(model));

    if (heapEnough && tempEnough && reliablePeak != null) {
      return new EstimationOutcome(reliable ? EstimationVerdict.ENOUGH_RESOURCES
          : EstimationVerdict.ENOUGH_RESOURCES_UNCERTAIN, details, null);
    }
    if (tempEnough && !reliableStepsExceedRam && !prediction.heapReliable()) {
      return new EstimationOutcome(EstimationVerdict.RAM_REQUIREMENT_UNCERTAIN, details,
          nextRunFiles);
    }
    if (validationReliable && (reliableStepsExceedRam || !tempEnough)) {
      return new EstimationOutcome(EstimationVerdict.NOT_ENOUGH_RESOURCES, details, null);
    }
    if (!reliable && nextRunFiles != null) {
      return new EstimationOutcome(EstimationVerdict.LARGER_RUN_NEEDED, details, nextRunFiles);
    }
    return new EstimationOutcome(EstimationVerdict.NOT_ENOUGH_RESOURCES, details, null);
  }
}
