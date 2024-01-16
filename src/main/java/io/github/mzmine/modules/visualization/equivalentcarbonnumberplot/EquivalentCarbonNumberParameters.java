package io.github.mzmine.modules.visualization.equivalentcarbonnumberplot;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;

public class EquivalentCarbonNumberParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public EquivalentCarbonNumberParameters() {
    super(new Parameter[]{featureLists});
  }

}
