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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moeaframework.core.Solution;

/**
 * Where a candidate's parameter vector came from. Every uncached proposal costs a full batch run,
 * so the results table has to show which phase of the run spent it: the raw data estimate, the
 * perturbed variants around it, the random samples that fill the rest of the initial population, or
 * the offspring the variation operators produced during evolution.
 * <p>
 * Without this the phases can only be told apart by evaluation index, which silently breaks as soon
 * as the population size, the number of guesses or the warm-start setting changes.
 */
public enum SolutionOrigin {

  /**
   * The unmodified estimate derived from the raw data statistics.
   */
  ESTIMATE("Estimate"),

  /**
   * A Gaussian perturbation around the estimate, injected into the initial population.
   */
  PERTURBED("Perturbed"),

  /**
   * A uniform random sample drawn to fill the initial population slots the guesses left open.
   */
  RANDOM("Random"),

  /**
   * Offspring of the algorithm's variation operators, i.e. everything the search itself produced.
   */
  EVOLUTION("Evolution"),

  /**
   * A coordinate poll or deterministic restart proposed by adaptive pattern search.
   */
  PATTERN_SEARCH("Pattern search");

  /**
   * Attribute key the origin is stored under. Also the results table column header and the csv
   * column name, both of which derive their columns from the attribute map.
   */
  public static final String ATTRIBUTE = "Origin";

  private final @NotNull String label;

  SolutionOrigin(@NotNull String label) {
    this.label = label;
  }

  /**
   * @return the origin of a solution, or null when it was never tagged.
   */
  public static @Nullable SolutionOrigin of(@NotNull Solution solution) {
    return solution.getAttribute(ATTRIBUTE) instanceof SolutionOrigin origin ? origin : null;
  }

  /**
   * Tags the solution, replacing an existing origin.
   */
  public void applyTo(@NotNull Solution solution) {
    solution.setAttribute(ATTRIBUTE, this);
  }

  /**
   * Tags the solution only when it does not carry an origin yet.
   * <p>
   * assumption: an untagged solution reaching the evaluation is offspring. Variation operators
   * build their children with {@link Solution#copy()}, which copies the variables but deliberately
   * not the attributes, so a child never inherits its parent's origin. Both supported optimizers
   * tag their initial candidates explicitly.
   */
  public void applyIfAbsent(@NotNull Solution solution) {
    if (!solution.hasAttribute(ATTRIBUTE)) {
      applyTo(solution);
    }
  }

  /**
   * decision: the label, not the enum name, because the results table and the csv export both
   * render attribute values through their string representation.
   */
  @Override
  public String toString() {
    return label;
  }
}
