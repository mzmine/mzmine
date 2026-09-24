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
 * Parameters of the carbon envelope model: the carbon count is estimated from the searched neutral
 * mass via a carbon-per-Dalton ratio, which drives the 13C envelope. Heavy-isotope contributions
 * widen only the upper bound and come from the isotope finder's shared element list.
 */
public class CarbonEnvelopeParameters extends SimpleParameterSet {

  // single source of truth: used to build the parameters below AND by createDefault(), so callers
  // never depend on the possibly-overwritten value of a shared static template
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

  public CarbonEnvelopeParameters() {
    super(new Parameter[]{carbonPerDaltonMin, carbonPerDaltonTypical, carbonPerDaltonMax,
        minRelIntensity, usePoissonNotBinomial});
  }

  /**
   * An independent parameter set with every value actively set to its default. Prefer it over the
   * plain constructor, which stores the shared static templates ({@link SimpleParameterSet} does not
   * clone) whose values may have been overwritten by a config load or the GUI.
   */
  public static @NotNull CarbonEnvelopeParameters createDefault() {
    return create(DEFAULT_CARBON_PER_DALTON_MIN, DEFAULT_CARBON_PER_DALTON_TYPICAL,
        DEFAULT_CARBON_PER_DALTON_MAX, DEFAULT_MIN_REL_INTENSITY, DEFAULT_USE_POISSON);
  }

  /**
   * As {@link #createDefault()} but with the given values set. Never mutate a plain
   * {@code new CarbonEnvelopeParameters()} instead: it shares the static templates, so setting
   * values on it corrupts the global defaults.
   */
  public static @NotNull CarbonEnvelopeParameters create(
      final double carbonPerDaltonMinValue, final double carbonPerDaltonTypicalValue,
      final double carbonPerDaltonMaxValue, final double minRelIntensityValue,
      final boolean usePoisson) {
    final CarbonEnvelopeParameters params = (CarbonEnvelopeParameters) new CarbonEnvelopeParameters().cloneParameterSet();
    params.getParameter(carbonPerDaltonMin).setValue(carbonPerDaltonMinValue);
    params.getParameter(carbonPerDaltonTypical).setValue(carbonPerDaltonTypicalValue);
    params.getParameter(carbonPerDaltonMax).setValue(carbonPerDaltonMaxValue);
    params.getParameter(minRelIntensity).setValue(minRelIntensityValue);
    params.getParameter(usePoissonNotBinomial).setValue(usePoisson);
    return params;
  }
}
