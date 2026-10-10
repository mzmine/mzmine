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

import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.OptimizationMetrics;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.SweepMetric;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.CheckListParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import java.util.ArrayList;

public class MoeadOptimizerParameters extends SimpleParameterSet {

  public static final CheckListParameter<SweepMetric> optimizationTargets = new CheckListParameter<>(
      "Optimization targets", "Quality metrics that MOEA/D optimizes as separate objectives.",
      OptimizationMetrics.ALL, new ArrayList<>(OptimizationMetrics.DEFAULT));

  public static final OptionalParameter<ComboParameter<WarmStartSampling>> rawDataInitialization = new OptionalParameter<>(
      new ComboParameter<>("Raw data-based initialization", """
          Initialize the MOEA/D population around parameters estimated from the raw data. The
          sampling method controls how the population covers the neighbourhood of that estimate.
          """, WarmStartSampling.values(), WarmStartSampling.GAUSSIAN), true);

  public MoeadOptimizerParameters() {
    super(optimizationTargets, rawDataInitialization);
  }
}
