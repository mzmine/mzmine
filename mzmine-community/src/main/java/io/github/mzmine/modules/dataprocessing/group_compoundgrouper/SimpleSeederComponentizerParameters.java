package io.github.mzmine.modules.dataprocessing.group_compoundgrouper;

import io.github.mzmine.main.ConfigService;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.ToleranceType;
import java.text.DecimalFormat;
import org.jetbrains.annotations.NotNull;

public class SimpleSeederComponentizerParameters extends SimpleParameterSet {

  public static final MZTolerance DEFAULT_MZ_TOLERANCE = MZTolerance.NARROW_5_PPM_OR_1_MDA;
  public static final RTTolerance DEFAULT_RT_TOLERANCE = new RTTolerance(0.03f, Unit.MINUTES);
  public static final double DEFAULT_MIN_DENSITY = 0.4d;

  public static final MZToleranceParameter MZ_TOLERANCE = new MZToleranceParameter(
      ToleranceType.INTRA_SAMPLE,
      "Tolerance for matching isotopologue m/z spacings during role assignment.",
      DEFAULT_MZ_TOLERANCE);

  public static final RTToleranceParameter RT_TOLERANCE = new RTToleranceParameter("RT tolerance",
      "Maximum allowed retention time difference between a member and the representative when "
          + "checking isotopologue / coherence relations.", DEFAULT_RT_TOLERANCE);

  public static final DoubleParameter MIN_DENSITY = new DoubleParameter("Min network density",
      "Minimum edge density [0..1] required to accept a residual (no-IIN) correlation cluster as "
          + "a compound. 1.0 = clique (fully connected sub graph). Lower values accept looser communities; nodes that fail "
          + "the threshold are peeled off and emitted as singleton compounds.",
      new DecimalFormat("0.00"), DEFAULT_MIN_DENSITY, 0d, 1d);

  public SimpleSeederComponentizerParameters() {
    super(MZ_TOLERANCE, RT_TOLERANCE, MIN_DENSITY);
  }

  public static SimpleSeederComponentizerParameters create(final @NotNull MZTolerance mzTolerance,
      final @NotNull RTTolerance rtTolerance, final double minDensity) {
    final SimpleSeederComponentizerParameters params = (SimpleSeederComponentizerParameters) ConfigService.getConfiguration()
        .getModuleParameters(SimpleSeederComponentizerModule.class).cloneParameterSet();
    setAll(params, mzTolerance, rtTolerance, minDensity);
    return params;
  }

  public static void setAll(final @NotNull ParameterSet param,
      final @NotNull MZTolerance mzTolerance, final @NotNull RTTolerance rtTolerance,
      final double minDensity) {
    param.setParameter(MZ_TOLERANCE, mzTolerance);
    param.setParameter(RT_TOLERANCE, rtTolerance);
    param.setParameter(MIN_DENSITY, minDensity);
  }

}
