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
import org.jetbrains.annotations.NotNull;

/**
 * Measurements of one batch run on a subset of the files.
 *
 * @param files              number of raw data files in the subset
 * @param fileEquivalents    raw data bytes of the subset divided by the mean file size of the whole
 *                           dataset, the x of the scaling fits
 * @param baselineHeapGB     used heap after GC right before the run, the application baseline
 * @param baselineLiveTempGB live temp file data right before the run
 * @param maxRows            rows of the largest feature list after the run, usually the aligned
 *                           list
 * @param steps              per step measurements without the whole batch row
 */
record SubsetRun(int files, double fileEquivalents, double baselineHeapGB,
                        double baselineLiveTempGB, int maxRows,
                        @NotNull List<StepMeasurement> steps) {

  /**
   * @return this run with other baselines, e.g. the common baselines of all runs
   */
  @NotNull SubsetRun withBaselines(final double heapGB, final double liveTempGB) {
    return new SubsetRun(files, fileEquivalents, heapGB, liveTempGB, maxRows, steps);
  }
}
