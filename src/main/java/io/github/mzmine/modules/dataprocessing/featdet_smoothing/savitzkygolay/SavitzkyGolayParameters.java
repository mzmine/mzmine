package io.github.mzmine.modules.dataprocessing.featdet_smoothing.savitzkygolay;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;

public class SavitzkyGolayParameters extends SimpleParameterSet {

  public static final OptionalParameter<ComboParameter<Integer>> rtSmoothing = new OptionalParameter<>(
      new ComboParameter<Integer>("Retention time smoothing",
          "Enables intensity smoothing along the rt axis.",
          new Integer[]{0, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25}, 5), false);

  public static final OptionalParameter<ComboParameter<Integer>> mobilitySmoothing = new OptionalParameter<>(
      new ComboParameter<Integer>("Mobility smoothing",
          "Enables intensity smoothing of the summed mobilogram.",
          new Integer[]{0, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25}, 5), false);

  public SavitzkyGolayParameters() {
    super(new Parameter[]{rtSmoothing, mobilitySmoothing});
  }
}
