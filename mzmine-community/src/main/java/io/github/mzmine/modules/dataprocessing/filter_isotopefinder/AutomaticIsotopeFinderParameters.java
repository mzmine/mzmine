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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder;

import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.signal.CarbonEnvelopeParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.ToleranceType;
import org.jetbrains.annotations.NotNull;

/**
 * The simplified isotope finder setup: only the three parameters that usually need tuning per
 * dataset, with the rest defaulted from {@link CarbonModelAlgorithmParameters}.
 * <p>
 * decision: the three are declared again here rather than reusing that class's instances. An
 * instance carries its value and both option sets live in the configuration at once, so sharing
 * would make them share one value; only names, descriptions and defaults are shared, so the two
 * dialogs cannot describe one setting differently.
 */
public class AutomaticIsotopeFinderParameters extends SimpleParameterSet {

  public static final MZToleranceParameter isotopeMzTolerance = new MZToleranceParameter(
      ToleranceType.FEATURE_TO_SCAN,
      CarbonModelAlgorithmParameters.DEFAULT_MZ_TOLERANCE.getMzTolerance(),
      CarbonModelAlgorithmParameters.DEFAULT_MZ_TOLERANCE.getPpmTolerance());

  public static final BooleanParameter requireC13 = new BooleanParameter(
      CarbonModelAlgorithmParameters.REQUIRE_C13_NAME,
      CarbonModelAlgorithmParameters.REQUIRE_C13_DESCRIPTION,
      CarbonModelAlgorithmParameters.DEFAULT_REQUIRE_C13);

  public static final IntegerParameter maxCharge = new IntegerParameter(
      CarbonModelAlgorithmParameters.MAX_CHARGE_NAME,
      CarbonModelAlgorithmParameters.MAX_CHARGE_DESCRIPTION,
      CarbonModelAlgorithmParameters.DEFAULT_MAX_CHARGE, true, 1, 1000);

  public AutomaticIsotopeFinderParameters() {
    super(isotopeMzTolerance, requireC13, maxCharge);
  }

  public static void setDefaults(@NotNull ParameterSet params) {
    setAll(params, CarbonModelAlgorithmParameters.DEFAULT_REQUIRE_C13,
        CarbonModelAlgorithmParameters.DEFAULT_MZ_TOLERANCE,
        CarbonModelAlgorithmParameters.DEFAULT_MAX_CHARGE);
  }
  public static void setAll(@NotNull ParameterSet params, boolean requireC13,
      @NotNull MZTolerance mzTolerance, int maxCharge) {
    params.setParameter(AutomaticIsotopeFinderParameters.requireC13, requireC13);
    params.setParameter(AutomaticIsotopeFinderParameters.maxCharge, maxCharge);
    params.setParameter(AutomaticIsotopeFinderParameters.isotopeMzTolerance, mzTolerance);
  }

  /**
   * Map the few exposed values onto the full carbon-model setup, defaulting everything else.
   */
  public static @NotNull CarbonModelAlgorithmParameters toCarbonModelParameters(
      @NotNull final ParameterSet params) {
    final CarbonModelAlgorithmParameters full = CarbonModelAlgorithmParameters.createDefault();
    full.setAll(CarbonModelAlgorithmParameters.DEFAULT_ELEMENTS,
        CarbonModelAlgorithmParameters.DEFAULT_ELEMENT_DETECTION_MODE,
        params.getValue(isotopeMzTolerance), params.getValue(maxCharge),
        params.getValue(requireC13),
        CarbonModelAlgorithmParameters.DEFAULT_EXPLAINABLE_SIGNALS_ONLY,
        CarbonModelAlgorithmParameters.DEFAULT_FWHM_REFINE,
        CarbonEnvelopeParameters.createDefault());
    return full;
  }
}
