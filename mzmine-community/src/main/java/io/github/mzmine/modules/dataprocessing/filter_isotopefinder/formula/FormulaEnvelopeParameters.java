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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder.formula;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.elements.ElementsCompositionRangeParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import java.text.DecimalFormat;

/**
 * Parameters for the formula-prediction envelope model. Candidate molecular formulas are enumerated
 * for the searched neutral mass within the element ranges, and the admissible envelope is the union
 * of their CDK-predicted isotope patterns. Guardrails (mass cutoff + candidate cap) keep the cost
 * bounded; above the cutoff the model falls back to the carbon-averagine envelope.
 */
public class FormulaEnvelopeParameters extends SimpleParameterSet {

  public static final ElementsCompositionRangeParameter elements = new ElementsCompositionRangeParameter(
      "Elements", "Allowed elements and their min/max counts used for formula enumeration.");

  public static final MZToleranceParameter formulaMzTolerance = new MZToleranceParameter(
      "Formula m/z tolerance",
      "m/z tolerance used to bound the neutral mass during formula search.", 0.002, 5);

  public static final DoubleParameter maxNeutralMassCutoff = new DoubleParameter(
      "Max neutral mass for prediction",
      "Above this neutral mass the formula enumeration is skipped and the carbon-averagine envelope is used instead.",
      new DecimalFormat("0.#"), 1000d, 0d, null);

  public static final IntegerParameter maxCandidateFormulas = new IntegerParameter(
      "Max candidate formulas",
      "Maximum number of candidate formulas to evaluate per charge before stopping (guardrail).",
      500, 1, 1_000_000);

  public static final DoubleParameter minRelIntensity = new DoubleParameter(
      "Minimum relative intensity",
      "Envelope cutoff: predicted offsets below this relative intensity (base peak = 1) are ignored.",
      new DecimalFormat("0.####"), 0.01, 0d, 1d);

  public FormulaEnvelopeParameters() {
    super(new Parameter[]{elements, formulaMzTolerance, maxNeutralMassCutoff, maxCandidateFormulas,
        minRelIntensity});
  }
}
