package io.github.mzmine.modules.dataprocessing.featdet_massdetection.auto;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;

public class AutoMassDetectorParameters extends SimpleParameterSet {

  public static final DoubleParameter noiseLevel = new DoubleParameter("Noise level",
      "The minimum signal intensity to be considered a peak.",
      MZmineCore.getConfiguration().getIntensityFormat(), 1E3);

  public AutoMassDetectorParameters() {
    super(new Parameter[] {noiseLevel});
  }
}
