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

import org.jetbrains.annotations.NotNull;

/**
 * Predicted resources of one batch step for a given number of files. Flat so that it can be
 * written as CSV.
 *
 * @param step            1-based step number
 * @param name            module name of the step
 * @param scaling         how the step grows, see {@link StepScalingMode}
 * @param heapGB          predicted peak live heap during the step, including the application
 *                        baseline
 * @param retainedHeapGB  part of {@link #heapGB} that is retained data, including the baseline
 * @param liveTempGB      predicted temp file data alive at the end of the step
 * @param seconds         predicted wall clock time
 * @param heapReliable    measurement used a full GC sample, or all input measurements of the
 *                        extrapolation did; does not guarantee the live peak was captured
 */
public record StepResourcePrediction(int step, @NotNull String name, @NotNull String scaling,
                                     double heapGB, double retainedHeapGB, double liveTempGB,
                                     double seconds, boolean heapReliable) {

  public @NotNull String heapReliability() {
    return heapReliable ? "Reliable" : "Unreliable";
  }

}
