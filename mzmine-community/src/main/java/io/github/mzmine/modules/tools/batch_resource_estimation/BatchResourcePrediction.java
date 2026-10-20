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

import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Predicted resources of the whole batch for a given number of files.
 *
 * @param files                  number of files the prediction is for
 * @param steps                  per step predictions
 * @param heapGB                 highest predicted live heap of all steps
 * @param retainedHeapGB         highest predicted retained heap of all steps, the data that stays
 *                               in memory independent of the threads
 * @param tempGB                 highest predicted live temp file data of all steps
 * @param seconds                predicted total time of all steps
 * @param rows                   predicted rows of the largest (aligned) feature list
 * @param recommendedXmxGB       heap to configure (-Xmx) including GC headroom, whole GB
 * @param heapBottleneckStep     name of the step with the highest heap
 * @param retainedBottleneckStep name of the step with the highest retained heap
 */
public record BatchResourcePrediction(int files, @NotNull List<StepResourcePrediction> steps,
                                      double heapGB, double retainedHeapGB, double tempGB,
                                      double seconds, int rows, int recommendedXmxGB,
                                      @NotNull String heapBottleneckStep,
                                      @NotNull String retainedBottleneckStep) {

  public boolean heapReliable() {
    return steps.stream().allMatch(StepResourcePrediction::heapReliable);
  }

  /**
   * The peak among steps with confirmed full collections in all input runs. Missing steps still
   * have an unknown live RAM requirement; this is not a complete batch estimate.
   */
  public @Nullable StepResourcePrediction reliableHeapPeak() {
    return steps.stream().filter(StepResourcePrediction::heapReliable)
        .max(Comparator.comparingDouble(StepResourcePrediction::heapGB)).orElse(null);
  }

}
