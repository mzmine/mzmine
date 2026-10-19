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

import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.OptimizationMetrics;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.SweepMetric;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.MoeadOptimizerParameters;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.OptimizerOptions;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.PatternSearchOptimizerParameters;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.WarmStartSampling;
import io.github.mzmine.parameters.ParameterSet;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testutils.MZmineTestUtil;

class OptimizerParametersTest {

  @BeforeAll
  static void initialize() {
    MZmineTestUtil.startMzmineCore();
  }

  @Test
  void patternSearchOwnsExactlyOneTarget() {
    final OptimizerParameters parameters = new OptimizerParameters();
    OptimizerParameters.setOptimizerAndTargets(parameters, OptimizerOptions.PATTERN_SEARCH,
        List.of(OptimizationMetrics.IPO_ISOTOPE_SCORE));

    final ParameterSet selected = OptimizerParameters.getSelectedOptimizerParameters(parameters);
    Assertions.assertInstanceOf(PatternSearchOptimizerParameters.class, selected);
    Assertions.assertEquals(List.of(OptimizationMetrics.IPO_ISOTOPE_SCORE),
        OptimizerParameters.getOptimizationTargets(parameters));
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> OptimizerParameters.setOptimizerAndTargets(parameters,
            OptimizerOptions.PATTERN_SEARCH, List.of(OptimizationMetrics.IPO_ISOTOPE_SCORE,
                OptimizationMetrics.SLAW_INTEGRATION_SCORE)));
  }

  @Test
  void moeadOwnsMultipleTargetsAndInitialization() {
    final OptimizerParameters parameters = new OptimizerParameters();
    final List<SweepMetric> targets = List.of(OptimizationMetrics.IPO_ISOTOPE_SCORE,
        OptimizationMetrics.SLAW_INTEGRATION_SCORE);
    OptimizerParameters.setOptimizerAndTargets(parameters, OptimizerOptions.MOEAD, targets);

    final ParameterSet selected = OptimizerParameters.getSelectedOptimizerParameters(parameters);
    Assertions.assertInstanceOf(MoeadOptimizerParameters.class, selected);
    Assertions.assertEquals(targets, OptimizerParameters.getOptimizationTargets(parameters));
    Assertions.assertTrue(selected.getValue(MoeadOptimizerParameters.rawDataInitialization));
    Assertions.assertEquals(WarmStartSampling.GAUSSIAN,
        selected.getEmbeddedParameterValue(MoeadOptimizerParameters.rawDataInitialization));
  }
}
