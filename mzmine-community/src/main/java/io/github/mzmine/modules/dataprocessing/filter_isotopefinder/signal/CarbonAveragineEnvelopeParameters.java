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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder.signal;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import java.text.DecimalFormat;

/**
 * Parameters for the signal-based carbon-averagine envelope model. The number of carbons is
 * estimated from the searched neutral mass (typical and maximum carbon-per-Dalton), which drives a
 * 13C isotope envelope. Heavy-isotope (S/Cl/Br ...) contributions widen only the upper bound and
 * are derived from the shared element list of the isotope finder.
 */
public class CarbonAveragineEnvelopeParameters extends SimpleParameterSet {

  public static final DoubleParameter carbonPerDaltonMin = new DoubleParameter(
      "Minimum C per Dalton",
      "Minimum number of carbons per Dalton used as the lower 13C bound for the optional \"require 13C\" gate (≈ 1/20 for heteroatom-rich molecules).",
      new DecimalFormat("0.####"), 1d / 20d, 0.001, 1d / 12d);

  public static final DoubleParameter carbonPerDaltonTypical = new DoubleParameter(
      "Typical C per Dalton",
      "Typical number of carbons per Dalton used to model the expected 13C envelope (≈ 1/14 for organic molecules).",
      new DecimalFormat("0.####"), 1d / 14d, 0.001, 1d / 12d);

  public static final DoubleParameter carbonPerDaltonMax = new DoubleParameter(
      "Maximum C per Dalton",
      "Maximum number of carbons per Dalton used to widen the upper intensity bound (≈ 1/12 = pure carbon).",
      new DecimalFormat("0.####"), 1d / 12d, 0.001, 1d / 12d);

  public static final DoubleParameter minRelIntensity = new DoubleParameter(
      "Minimum relative intensity",
      "Envelope cutoff: offsets predicted below this relative intensity (base peak = 1) are not expected.",
      new DecimalFormat("0.####"), 0.01, 0d, 1d);

  public static final BooleanParameter usePoissonNotBinomial = new BooleanParameter(
      "Use Poisson model",
      "Model the 13C envelope with a Poisson distribution (recommended). If disabled, a binomial model is used.",
      true);

  public CarbonAveragineEnvelopeParameters() {
    super(new Parameter[]{carbonPerDaltonMin, carbonPerDaltonTypical, carbonPerDaltonMax,
        minRelIntensity, usePoissonNotBinomial});
  }
}
