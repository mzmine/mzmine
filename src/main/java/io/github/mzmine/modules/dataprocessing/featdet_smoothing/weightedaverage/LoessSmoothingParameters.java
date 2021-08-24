package io.github.mzmine.modules.dataprocessing.featdet_smoothing.weightedaverage;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;

public class LoessSmoothingParameters extends SimpleParameterSet {

  public static final OptionalParameter<IntegerParameter> rtSmoothing = new OptionalParameter<>(
      new IntegerParameter("Retention time width (scans)",
          "Enables intensity smoothing along the rt axis.", 5, 0, Integer.MAX_VALUE));

  public static final OptionalParameter<IntegerParameter> mobilitySmoothing = new OptionalParameter<IntegerParameter>(
      new IntegerParameter("Mobility width (scans)",
          "Enables intensity smoothing along the mobility axis.", 5, 0, Integer.MAX_VALUE));

  public LoessSmoothingParameters() {
    super(new Parameter[]{rtSmoothing, mobilitySmoothing});
  }
}
