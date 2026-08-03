package io.github.mzmine.modules.dataprocessing.filter_isotopefinder;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.ToleranceType;

/**
 * The simplified isotope finder setup: only the three parameters that usually need tuning per
 * dataset. Everything else is filled with the defaults of the
 * {@link CarbonAveragineAlgorithmParameters}, which this option is mapped onto (see
 * {@link AutomaticIsotopeFinderModule}).
 */
public class AutomaticIsotopeFinderParameters extends SimpleParameterSet {

  public static final MZToleranceParameter isotopeMzTolerance = new MZToleranceParameter(
      ToleranceType.FEATURE_TO_SCAN, 0.0005, 10);

  public static final BooleanParameter requireC13 = new BooleanParameter("Require 13C isotope peak",
      """
          If enabled, a charge is only accepted when the signals form a gap-free ladder on the \
          charge-adjusted 13C grid through the detected pattern. Features without such a ladder are \
          skipped (useful to suppress noise / heavy-isotope-only artifacts). Note that this also \
          truncates the reported pattern at the first missing 13C position.""",
      CarbonAveragineAlgorithmParameters.DEFAULT_REQUIRE_C13);

  public static final IntegerParameter maxCharge = new IntegerParameter(
      "Maximum charge of isotope m/z",
      "Maximum possible charge of the isotope distribution. Charges 1..maxCharge are evaluated and "
          + "the most probable charge is selected; other highly probable charges are flagged.",
      CarbonAveragineAlgorithmParameters.DEFAULT_MAX_CHARGE, true, 1, 1000);

  public AutomaticIsotopeFinderParameters() {
    super(new Parameter[]{isotopeMzTolerance, requireC13, maxCharge});
  }
}
