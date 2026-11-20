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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.RealVariable;

class CanonicalParameterSpaceTest {

  @Test
  void roundTripUsesLinearLogarithmicAndEffectiveOrdinalValues() {
    final CanonicalParameterSpace space = new CanonicalParameterSpace(new MixedVariableProblem());
    final Solution original = new MixedVariableProblem().newSolution();
    RealVariable.setReal(original.getVariable(0), 26d);
    RealVariable.setReal(original.getVariable(1), 3d);
    RealVariable.setReal(original.getVariable(2), 2.6d);

    final double[] canonical = space.encode(original);
    final Solution restored = space.materialize(canonical, SolutionOrigin.PATTERN_SEARCH);

    Assertions.assertArrayEquals(new double[]{0.25d, 0.5d, 2d / 3d}, canonical, 1e-12);
    Assertions.assertEquals(26d, OrdinalIntegerVariable.effectiveValue(restored, 0), 1e-12);
    Assertions.assertEquals(3d, OrdinalIntegerVariable.effectiveValue(restored, 1), 1e-12);
    Assertions.assertEquals(3d, OrdinalIntegerVariable.effectiveValue(restored, 2), 1e-12);
    Assertions.assertInstanceOf(OrdinalIntegerVariable.class, restored.getVariable(2));
    Assertions.assertEquals(1, restored.getNumberOfConstraints());
    Assertions.assertEquals(SolutionOrigin.PATTERN_SEARCH, SolutionOrigin.of(restored));
  }

  @Test
  void canonicalizationClampsAndDeduplicatesOrdinalFractions() {
    final CanonicalParameterSpace space = new CanonicalParameterSpace(new MixedVariableProblem());

    final double[] first = space.canonicalize(new double[]{-0.2d, 1.4d, 0.34d});
    final double[] second = space.canonicalize(new double[]{0d, 1d, 0.49d});

    Assertions.assertArrayEquals(new double[]{0d, 1d, 1d / 3d}, first, 1e-12);
    Assertions.assertEquals(space.key(first), space.key(second));
    Assertions.assertEquals(1d / 3d, space.ordinalStep(2), 1e-12);
  }

  @Test
  void candidateKeysMatchTheValuesWrittenToMaterializedSolutions() {
    final MixedVariableProblem problem = new MixedVariableProblem();
    final CanonicalParameterSpace space = new CanonicalParameterSpace(problem);

    for (int step = 0; step <= 100; step++) {
      final double value = step / 100d;
      final double[] candidate = {value, value, value};
      final Solution materialized = space.materialize(candidate, SolutionOrigin.PATTERN_SEARCH);

      Assertions.assertEquals(space.key(candidate), space.key(materialized));
    }
  }
}
