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

import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.signal.CarbonAveragineEnvelopeParameters;
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
 * dataset. Everything else is filled with the defaults of the
 * {@link CarbonAveragineAlgorithmParameters}, which this option is mapped onto (see
 * {@link AutomaticIsotopeFinderModule}).
 * <p>
 * The three parameters are defined again here rather than reusing the instances of
 * {@link CarbonAveragineAlgorithmParameters}: a parameter instance carries its value, and both
 * option sets live in the configuration at the same time, so a shared instance would make the two
 * options share one value. Only the names, descriptions and defaults are shared, so the two dialogs
 * cannot describe the same setting differently.
 */
public class AutomaticIsotopeFinderParameters extends SimpleParameterSet {

  public static final MZToleranceParameter isotopeMzTolerance = new MZToleranceParameter(
      ToleranceType.FEATURE_TO_SCAN,
      CarbonAveragineAlgorithmParameters.DEFAULT_MZ_TOLERANCE.getMzTolerance(),
      CarbonAveragineAlgorithmParameters.DEFAULT_MZ_TOLERANCE.getPpmTolerance());

  public static final BooleanParameter requireC13 = new BooleanParameter(
      CarbonAveragineAlgorithmParameters.REQUIRE_C13_NAME,
      CarbonAveragineAlgorithmParameters.REQUIRE_C13_DESCRIPTION,
      CarbonAveragineAlgorithmParameters.DEFAULT_REQUIRE_C13);

  public static final IntegerParameter maxCharge = new IntegerParameter(
      CarbonAveragineAlgorithmParameters.MAX_CHARGE_NAME,
      CarbonAveragineAlgorithmParameters.MAX_CHARGE_DESCRIPTION,
      CarbonAveragineAlgorithmParameters.DEFAULT_MAX_CHARGE, true, 1, 1000);

  public AutomaticIsotopeFinderParameters() {
    super(isotopeMzTolerance, requireC13, maxCharge);
  }

  public static void setDefaults(@NotNull ParameterSet params) {
    setAll(params, CarbonAveragineAlgorithmParameters.DEFAULT_REQUIRE_C13,
        CarbonAveragineAlgorithmParameters.DEFAULT_MZ_TOLERANCE,
        CarbonAveragineAlgorithmParameters.DEFAULT_MAX_CHARGE);
  }
  public static void setAll(@NotNull ParameterSet params, boolean requireC13,
      @NotNull MZTolerance mzTolerance, int maxCharge) {
    params.setParameter(AutomaticIsotopeFinderParameters.requireC13, requireC13);
    params.setParameter(AutomaticIsotopeFinderParameters.maxCharge, maxCharge);
    params.setParameter(AutomaticIsotopeFinderParameters.isotopeMzTolerance, mzTolerance);
  }

  /**
   * Map the few exposed values onto the full carbon-averagine setup, defaulting everything else.
   *
   * @param params an {@link AutomaticIsotopeFinderParameters} value set.
   * @return a new, independent full parameter set.
   */
  public static @NotNull CarbonAveragineAlgorithmParameters toCarbonAveragineParameters(
      @NotNull final ParameterSet params) {
    final CarbonAveragineAlgorithmParameters full = CarbonAveragineAlgorithmParameters.createDefault();
    full.setAll(CarbonAveragineAlgorithmParameters.DEFAULT_ELEMENTS,
        CarbonAveragineAlgorithmParameters.DEFAULT_ELEMENT_DETECTION_MODE,
        params.getValue(isotopeMzTolerance), params.getValue(maxCharge),
        params.getValue(requireC13),
        CarbonAveragineAlgorithmParameters.DEFAULT_EXPLAINABLE_SIGNALS_ONLY,
        CarbonAveragineAlgorithmParameters.DEFAULT_FWHM_REFINE,
        CarbonAveragineEnvelopeParameters.createDefault());
    return full;
  }
}
