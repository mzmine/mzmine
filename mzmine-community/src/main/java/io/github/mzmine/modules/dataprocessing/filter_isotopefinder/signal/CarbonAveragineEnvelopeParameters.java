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
import org.jetbrains.annotations.NotNull;

/**
 * Parameters for the signal-based carbon-averagine envelope model. The number of carbons is
 * estimated from the searched neutral mass (typical and maximum carbon-per-Dalton), which drives a
 * 13C isotope envelope. Heavy-isotope (S/Cl/Br ...) contributions widen only the upper bound and
 * are derived from the shared element list of the isotope finder.
 */
public class CarbonAveragineEnvelopeParameters extends SimpleParameterSet {

  // default values - single source of truth, used both to build the parameters below and by
  // createDefault() to actively set them on a fresh cloned instance (so callers never depend on the
  // possibly-overwritten value carried by the shared static parameter templates).
  public static final double DEFAULT_CARBON_PER_DALTON_MIN = 1d / 20d;
  public static final double DEFAULT_CARBON_PER_DALTON_TYPICAL = 1d / 14d;
  public static final double DEFAULT_CARBON_PER_DALTON_MAX = 1d / 12d;
  public static final double DEFAULT_MIN_REL_INTENSITY = 0.01d;
  public static final boolean DEFAULT_USE_POISSON = true;

  public static final DoubleParameter carbonPerDaltonMin = new DoubleParameter(
      "Minimum C per Dalton",
      "Minimum number of carbons per Dalton used as the lower 13C bound for the optional \"require 13C\" gate (≈ 1/20 for heteroatom-rich molecules).",
      new DecimalFormat("0.####"), DEFAULT_CARBON_PER_DALTON_MIN, 0.001, 1d / 12d);

  public static final DoubleParameter carbonPerDaltonTypical = new DoubleParameter(
      "Typical C per Dalton",
      "Typical number of carbons per Dalton used to model the expected 13C envelope (≈ 1/14 for organic molecules).",
      new DecimalFormat("0.####"), DEFAULT_CARBON_PER_DALTON_TYPICAL, 0.001, 1d / 12d);

  public static final DoubleParameter carbonPerDaltonMax = new DoubleParameter(
      "Maximum C per Dalton",
      "Maximum number of carbons per Dalton used to widen the upper intensity bound (≈ 1/12 = pure carbon).",
      new DecimalFormat("0.####"), DEFAULT_CARBON_PER_DALTON_MAX, 0.001, 1d / 12d);

  public static final DoubleParameter minRelIntensity = new DoubleParameter(
      "Minimum relative intensity",
      "Envelope cutoff: offsets predicted below this relative intensity (base peak = 1) are not expected.",
      new DecimalFormat("0.####"), DEFAULT_MIN_REL_INTENSITY, 0d, 1d);

  public static final BooleanParameter usePoissonNotBinomial = new BooleanParameter(
      "Use Poisson model",
      "Model the 13C envelope with a Poisson distribution (recommended). If disabled, a binomial model is used.",
      DEFAULT_USE_POISSON);

  public CarbonAveragineEnvelopeParameters() {
    super(new Parameter[]{carbonPerDaltonMin, carbonPerDaltonTypical, carbonPerDaltonMax,
        minRelIntensity, usePoissonNotBinomial});
  }

  /**
   * Create an independent parameter set with every value actively set to its default. Prefer this
   * over {@code new CarbonAveragineEnvelopeParameters()} wherever a defaulted set is needed: the
   * plain constructor stores the shared static parameter templates (a {@link SimpleParameterSet}
   * does not clone), whose values may have been overwritten elsewhere (config load / GUI); this
   * clones and re-sets the documented defaults so the result is self-contained and correct.
   *
   * @return a new, independent parameter set with default values.
   */
  public static @NotNull CarbonAveragineEnvelopeParameters createDefault() {
    return create(DEFAULT_CARBON_PER_DALTON_MIN, DEFAULT_CARBON_PER_DALTON_TYPICAL,
        DEFAULT_CARBON_PER_DALTON_MAX, DEFAULT_MIN_REL_INTENSITY, DEFAULT_USE_POISSON);
  }

  /**
   * Create an independent (cloned) parameter set with the given values actively set. Use this
   * instead of mutating a {@code new CarbonAveragineEnvelopeParameters()}: the plain constructor
   * shares the static parameter templates, so setting values on it would corrupt the global
   * defaults. Cloning first yields a self-contained set.
   *
   * @param carbonPerDaltonMinValue     minimum carbons per Dalton (lower 13C bound).
   * @param carbonPerDaltonTypicalValue typical carbons per Dalton (expected envelope).
   * @param carbonPerDaltonMaxValue     maximum carbons per Dalton (upper intensity bound).
   * @param minRelIntensityValue        envelope relative-intensity cutoff.
   * @param usePoisson                  {@code true} for a Poisson model, {@code false} for
   *                                    binomial.
   * @return a new, independent parameter set with the given values.
   */
  public static @NotNull CarbonAveragineEnvelopeParameters create(
      final double carbonPerDaltonMinValue, final double carbonPerDaltonTypicalValue,
      final double carbonPerDaltonMaxValue, final double minRelIntensityValue,
      final boolean usePoisson) {
    final CarbonAveragineEnvelopeParameters params = (CarbonAveragineEnvelopeParameters) new CarbonAveragineEnvelopeParameters().cloneParameterSet();
    params.getParameter(carbonPerDaltonMin).setValue(carbonPerDaltonMinValue);
    params.getParameter(carbonPerDaltonTypical).setValue(carbonPerDaltonTypicalValue);
    params.getParameter(carbonPerDaltonMax).setValue(carbonPerDaltonMaxValue);
    params.getParameter(minRelIntensity).setValue(minRelIntensityValue);
    params.getParameter(usePoissonNotBinomial).setValue(usePoisson);
    return params;
  }
}
