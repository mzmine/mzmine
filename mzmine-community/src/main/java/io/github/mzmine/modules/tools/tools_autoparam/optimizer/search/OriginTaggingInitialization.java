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

package io.github.mzmine.modules.tools.tools_autoparam.optimizer.search;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.moeaframework.core.Solution;
import org.moeaframework.core.initialization.InjectedInitialization;
import org.moeaframework.problem.Problem;

/**
 * {@link InjectedInitialization} that labels the random solutions it draws to fill the initial
 * population, so the results table can separate them from the injected guesses and from the
 * offspring produced later.
 * <p>
 * decision: tagged here instead of counted by evaluation index in the problem. The random fill and
 * the offspring both arrive at the evaluation untagged, and only the initialization knows which is
 * which.
 */
public class OriginTaggingInitialization extends InjectedInitialization {

  /**
   * @param injectedSolutions warm-start solutions, expected to carry their own
   *                          {@link SolutionOrigin}. May be empty, in which case the whole
   *                          population is random.
   */
  public OriginTaggingInitialization(@NotNull Problem problem,
      @NotNull List<Solution> injectedSolutions) {
    super(problem, injectedSolutions);
  }

  @Override
  public Solution[] initialize(int populationSize) {
    final Solution[] population = super.initialize(populationSize);
    for (final Solution solution : population) {
      SolutionOrigin.RANDOM.applyIfAbsent(solution);
    }
    return population;
  }
}
