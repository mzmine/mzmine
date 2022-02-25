package io.github.mzmine.modules.dataprocessing.id_ecmscalcpotential;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import java.text.DecimalFormat;

public class CalcEcmsPotentialParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final DoubleParameter tubingLengthMM = new DoubleParameter("Tubing length / mm",
      "Tubing length between EC-Cell and ESI-Needle.", new DecimalFormat("0.0"), 750d);

  public static final DoubleParameter tubingIdMM = new DoubleParameter("Tubing inner diameter / mm",
      "Inner diameter of the tubing.", new DecimalFormat("0.000"), 0.127d);

  public static final DoubleParameter flowRateMicroLiterPerMin = new DoubleParameter(
      "Flow rate / μL/min", "Tubing length between EC-Cell and ESI-Needle.",
      new DecimalFormat("0.0"));

  public static final DoubleParameter potentialRampSpeed = new DoubleParameter(
      "Potential ramp / mV/s", "Potential ramp speed in mV/s.");

  public static final PercentParameter potentialAssignmentIntensityPercentage = new PercentParameter(
      "Potential assingment intensity",
      "Percentage of the maximum metabolite intensity that will be used to assign the formation potential to a metabolite.");

  public CalcEcmsPotentialParameters() {
    super(new Parameter[]{flists, tubingLengthMM, tubingIdMM, flowRateMicroLiterPerMin, potentialRampSpeed,
        potentialAssignmentIntensityPercentage});
  }
}
