package io.github.mzmine.modules.dataprocessing.id_localcsvsearch;

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

  public static final BooleanParameter scanBased = new BooleanParameter(
      "Scan-based isotope pattern matcher",
      "Check, if the isotope signals are to be searched for scan-based. \\n"
           +"            + \" Otherwise, isotope signals are searched for in the feature list.", false);
  //public static final OptionalModuleParameter<IsotopePatternMatcherParameterSet> isotopePatternMatcher = new OptionalModuleParameter<>(
  //    "Use isotope matcher", "",
  //    (IsotopePatternMatcherParameterSet) new IsotopePatternMatcherParameterSet().cloneParameterSet());

  public static final MZToleranceParameter isotopeMzTolerance = new MZToleranceParameter(
      "Isotope m/z tolerance",
      "Maximum allowed difference between two m/z values to be considered same.\\n"
          + "            + \"The value is specified both as absolute tolerance (in m/z) and relative tolerance (in ppm).\\n"
          + "            + \"The tolerance range is calculated using maximum of the absolute and relative tolerances.",
      0.005, 10);
  public static final RTToleranceParameter isotopeRtTolerance = new RTToleranceParameter(
      "Isotope retention time tolerance",
      "Maximum allowed difference between two retention time values",
      new RTTolerance(0.1f, Unit.MINUTES));
  public static final PercentParameter minIntensity = new PercentParameter(
      "Minimum isotope intensity (%)",
      "Minimum intensity percentage (%) that the isotopes must have in order to apply to the isotope pattern.");
  public static final DoubleParameter minIsotopeScore = new DoubleParameter("Minimum isotope score",
      "Minimum isotope pattern score that the detected isotope pattern must have in order to apply to the database hits",
      NumberFormat.getNumberInstance(), 0.0);

  public IsotopePatternMatcherParameterSet() {
    super(new Parameter[]{scanBased, isotopeRtTolerance, isotopeMzTolerance, minIntensity, minIsotopeScore});
  }
}
