package io.github.mzmine.modules.dataprocessing.filter_interestingfeaturefinder;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;

public class IsomerQualityParameters extends SimpleParameterSet {

  public static final DoubleParameter minIntensity = new DoubleParameter("Minimum intensity",
      "Minimum intensity of a possible isomer to be annotated.",
      MZmineCore.getConfiguration().getIntensityFormat(), 1E3, 0d, 1E10);

  public static final IntegerParameter minDataPointsInTrace = new IntegerParameter(
      "Minimum number of datapoints in trace",
      "Minimum number of data points in ion mobility trace to be recognised as a"
          + " isomeric compound.\nUsed to filter out noise after resolving.",
      30, 1, 500);

  public IsomerQualityParameters() {
    super(new Parameter[]{minIntensity, minDataPointsInTrace});
  }
}
