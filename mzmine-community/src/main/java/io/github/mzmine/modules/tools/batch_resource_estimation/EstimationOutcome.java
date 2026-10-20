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
    final boolean validationReliable =
        check == null || check.maxResourceError() <= MAX_RELIABLE_DEVIATION;
    final boolean reliable = prediction.heapReliable() && validationReliable;

    final List<String> details = new ArrayList<>();
    if (!prediction.heapReliable()) {
      details.add("The peak RAM estimate is not reliable: some measurements may include memory "
          + "that was no longer needed.");
    }
    details.addAll(BatchResourceReport.reliabilityLines(model));

    if (heapEnough && tempEnough) {
      return new EstimationOutcome(reliable ? EstimationVerdict.ENOUGH_RESOURCES
          : EstimationVerdict.ENOUGH_RESOURCES_UNCERTAIN, details, null);
    }
    final Integer nextRunFiles = model.nextRunFiles();
    if (!reliable && nextRunFiles != null) {
      return new EstimationOutcome(EstimationVerdict.LARGER_RUN_NEEDED, details, nextRunFiles);
    }
    return new EstimationOutcome(EstimationVerdict.NOT_ENOUGH_RESOURCES, details, null);
  }
}
