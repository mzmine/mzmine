package io.github.mzmine.modules.dataprocessing.featdet_imsmsi;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;

public class IonMobilityImageExpanderParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public IonMobilityImageExpanderParameters() {
    super(new Parameter[]{featureLists});
  }
}
