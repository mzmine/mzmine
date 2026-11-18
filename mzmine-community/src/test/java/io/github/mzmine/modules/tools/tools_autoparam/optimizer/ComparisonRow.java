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

package io.github.mzmine.modules.tools.tools_autoparam.optimizer;

import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.SolutionOrigin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One line of {@link EstimateVsOptimumTest}'s output: what the estimator guessed for a quantity and
 * what the optimization made of it.
 *
 * @param seed      random seed the run used, so results from several draws can be pooled
 * @param kind      "parameter", "metric" or "diagnostic", so the csv can be pivoted per group
 * @param perturbed value of the best feasible {@link SolutionOrigin#PERTURBED} solution, null when
 *                  no perturbation was feasible
 * @param front     value of the best solution on the non-dominated front, null when it was empty
 */
public record ComparisonRow(@NotNull String dataset, long seed, @NotNull String kind,
                            @NotNull String name, @Nullable Double estimate,
                            @Nullable Double perturbed, @Nullable Double front) {

}
