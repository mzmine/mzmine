/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.tools.msmsscore;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class MSMSScoreParameters extends SimpleParameterSet {

  public static final MZToleranceParameter msmsTolerance = new MZToleranceParameter(
      "MS/MS m/z tolerance", "Tolerance of the mass value to search (+/- range)", 0.002, 8d);

  public static final PercentParameter msmsMinScore = new PercentParameter("MS/MS score threshold",
      "If the score for MS/MS is lower, discard this match", 0d);

  public static final OptionalParameter<IntegerParameter> useTopNSignals = new OptionalParameter<>(
      new IntegerParameter("Use only top N signals",
          "Use only the most abundant N signals for scoring (speeds up the process)", 20), true);

  public MSMSScoreParameters() {
    super(new Parameter[]{msmsTolerance, msmsMinScore, useTopNSignals});
  }

}
