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

/**
 * All runs of a resource estimation and what is needed to add further runs to it.
 *
 * @param runner        runs further subsets of the same batch and files
 * @param modes         scaling mode of each batch step
 * @param numThreads    number of threads of the runs
 * @param fitRuns       the subsets of the extrapolation, ascending by size
 * @param validationRun the run that validates the extrapolation of the subsets
 * @param extensionRuns larger runs that were added after the validation run, ascending by size
 */
record EstimationSession(@NotNull SubsetRunner runner,
                                @NotNull List<StepScalingMode> modes, int numThreads,
                                @NotNull List<SubsetRun> fitRuns, @NotNull SubsetRun validationRun,
                                @NotNull List<SubsetRun> extensionRuns) {

  @NotNull BatchResourceModel fitModel() {
    return BatchResourceModel.fit(fitRuns, validationRun, extensionRuns, modes,
        runner.totalFiles(), numThreads);
  }

  /**
   * @return this session with a further larger run
   */
  @NotNull EstimationSession withExtension(@NotNull final SubsetRun run) {
    final List<SubsetRun> extensions = new ArrayList<>(extensionRuns);
    extensions.add(run);
    return new EstimationSession(runner, modes, numThreads, fitRuns, validationRun,
        List.copyOf(extensions));
  }
}
