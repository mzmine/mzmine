package io.github.mzmine.modules.dataprocessing.id_localcsvsearch;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.ModuleComboParameter;
import io.github.mzmine.parameters.parametertypes.ParameterSetParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.submodules.SubModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import java.text.NumberFormat;


public class IsotopePatternMatcherParameterSet extends SimpleParameterSet {

  public static final MZToleranceParameter isotopeMzTolerance = new MZToleranceParameter(
      "Isotope m/z tolerance",
      "Maximum allowed difference between two m/z values to be considered same.\\n"
          + "            + \"The value is specified both as absolute tolerance (in m/z) and relative tolerance (in ppm).\\n"
          + "            + \"The tolerance range is calculated using maximum of the absolute and relative tolerances.",
      0.005, 10);
  public static final PercentParameter minIntensity = new PercentParameter(
      "Minimum isotope intensity (%)",
      "Minimum intensity percentage (%) that the isotopes must have in order to apply to the isotope pattern.", 0.05);
  public static final DoubleParameter minIsotopeScore = new DoubleParameter("Minimum isotope score",
      "Minimum isotope pattern score that the detected isotope pattern must have in order to apply to the database hits",
      MZmineCore.getConfiguration().getScoreFormat(), 0.0);

  public IsotopePatternMatcherParameterSet() {
    super(new Parameter[]{isotopeMzTolerance, minIntensity, minIsotopeScore});
  }
}
