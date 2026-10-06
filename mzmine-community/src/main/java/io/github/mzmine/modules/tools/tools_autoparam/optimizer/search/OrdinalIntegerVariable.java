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
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.RealVariable;
import org.moeaframework.core.variable.Variable;

/**
 * An integer-valued optimization variable backed by a {@link RealVariable} and rounded on read.
 * <p>
 * MOEA Framework's {@code BinaryIntegerVariable} encodes the value as a gray-coded bit string and
 * mutates it with bit flips and half-uniform crossover. That treats an ordered index as a nominal
 * category: a single flip can move the value across a large part of its range, so the operators
 * cannot make a small step. Mixing it with {@link RealVariable} also makes
 * {@code Problem.isType(RealVariable.class)} false for the whole solution vector, which silently
 * costs MOEA/D its differential evolution operator and falls back to SBX with bit flips.
 * <p>
 * Keeping the value real and rounding on read makes every variation operator treat the index
 * ordinally, and keeps the solution vector homogeneous so MOEA/D runs as MOEA/D-DE.
 */
public class OrdinalIntegerVariable extends RealVariable {

  /**
   * Half a step of padding is added at both ends of the real interval so that every integer in
   * {@code [lowerBound, upperBound]} covers the same width and is therefore sampled with equal
   * probability. Without the padding the two end values would only be half as likely as the
   * interior ones.
   */
  private static final double HALF_STEP = 0.5;

  private final int lowerIntBound;
  private final int upperIntBound;

  public OrdinalIntegerVariable(@NotNull String name, int lowerBound, int upperBound) {
    super(name, lowerBound - HALF_STEP, upperBound + HALF_STEP);
    this.lowerIntBound = lowerBound;
    this.upperIntBound = upperBound;
  }

  /**
   * Reads the variable at {@code index} as an integer.
   *
   * @throws IllegalArgumentException if the variable is not a {@link RealVariable}
   */
  public static int getInt(@NotNull Solution solution, int index) {
    final Variable variable = solution.getVariable(index);
    if (variable instanceof OrdinalIntegerVariable ordinal) {
      return ordinal.getIntValue();
    }
    // decision: tolerate a plain RealVariable instead of throwing, so that a solution which lost
    // the subclass still yields a usable index
    return (int) Math.round(RealVariable.getReal(variable));
  }

  /**
   * The value a solution's variable is actually applied with: rounded for an
   * {@link OrdinalIntegerVariable}, unchanged for a plain {@link RealVariable}.
   * <p>
   * decision: kept here rather than at the call sites, because every caller that reads a solution -
   * the batch queue, the evaluation cache key, the results table, the csv export - has to agree on
   * this or they describe different runs.
   */
  public static double effectiveValue(@NotNull Solution solution, int index) {
    final Variable variable = solution.getVariable(index);
    return variable instanceof OrdinalIntegerVariable ? getInt(solution, index)
        : RealVariable.getReal(variable);
  }

  /**
   * The rounded value. Clamped because both the padded bounds and the variation operators can leave
   * the value just outside the integer range.
   */
  public int getIntValue() {
    return Math.clamp(Math.round(getValue()), lowerIntBound, upperIntBound);
  }

  public int getLowerIntBound() {
    return lowerIntBound;
  }

  public int getUpperIntBound() {
    return upperIntBound;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Overridden so the subclass survives {@link Solution#copy()}, which copies each variable
   * individually. Without this every offspring would degrade to a plain {@link RealVariable}.
   */
  @Override
  public OrdinalIntegerVariable copy() {
    final OrdinalIntegerVariable copy = new OrdinalIntegerVariable(getName(), lowerIntBound,
        upperIntBound);
    copy.setValue(getValue());
    return copy;
  }
}
