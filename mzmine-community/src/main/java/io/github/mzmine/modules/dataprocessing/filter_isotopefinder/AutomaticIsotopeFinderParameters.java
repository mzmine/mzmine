package io.github.mzmine.modules.dataprocessing.filter_isotopefinder;

import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.signal.CarbonAveragineEnvelopeParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
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
