package io.github.mzmine.modules.dataprocessing.group_compoundgrouper;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.ToleranceType;
import java.text.DecimalFormat;

public class SimpleSeederComponentizerParameters extends SimpleParameterSet {

  public static final MZToleranceParameter MZ_TOLERANCE = new MZToleranceParameter(
      ToleranceType.INTRA_SAMPLE,
      "Tolerance for matching isotopologue m/z spacings during role assignment.");

  public static final RTToleranceParameter RT_TOLERANCE = new RTToleranceParameter("RT tolerance",
      "Maximum allowed retention time difference between a member and the representative when "
          + "checking isotopologue / coherence relations.");

  public static final DoubleParameter MIN_DENSITY = new DoubleParameter("Min residual density",
      "Minimum edge density [0..1] required to accept a residual (no-IIN) correlation cluster as "
          + "a compound. 1.0 = clique. Lower values accept looser communities; nodes that fail "
          + "the threshold are peeled off and emitted as singleton compounds.",
      new DecimalFormat("0.00"), 0.3, 0d, 1d);

  public SimpleSeederComponentizerParameters() {
    super(MZ_TOLERANCE, RT_TOLERANCE, MIN_DENSITY);
  }
}
