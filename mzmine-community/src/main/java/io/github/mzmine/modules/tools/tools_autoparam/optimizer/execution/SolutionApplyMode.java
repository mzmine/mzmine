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

package io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution;

import org.jetbrains.annotations.NotNull;

/**
 * Which values of an optimization solution are applied to the wizard, see
 * {@link WizardOptimizationProblem#applySolutionToWizard}.
 */
public enum SolutionApplyMode {

  /**
   * Only the optimized parameters are applied. All other wizard values stay as they are, including
   * values the user set manually.
   */
  OPTIMIZED_ONLY("Apply only optimized parameters", """
      Applies only the parameters that were optimized. All other wizard parameters keep their \
      current values, including manually set ones. The result may differ from the evaluated \
      solution, because it was evaluated together with the raw data estimates."""),

  /**
   * The raw data estimates and the optimized parameters are applied, reproducing the evaluated
   * solution as closely as possible.
   */
  ESTIMATED_AND_OPTIMIZED("Apply estimated and optimized parameters", """
      Applies the parameters estimated from the raw data and the optimized parameters, exactly as \
      the solution was evaluated. Overwrites manually set values of all estimated parameters.""");

  private final @NotNull String label;
  private final @NotNull String description;

  SolutionApplyMode(@NotNull String label, @NotNull String description) {
    this.label = label;
    this.description = description;
  }

  public @NotNull String getLabel() {
    return label;
  }

  public @NotNull String getDescription() {
    return description;
  }

  @Override
  public String toString() {
    return label;
  }
}
