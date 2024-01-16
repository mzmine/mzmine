package io.github.mzmine.modules.visualization.lipidannotationsummary;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;

public class LipidAnnotationSummaryParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public LipidAnnotationSummaryParameters() {
    super(new Parameter[]{featureLists});
  }

}
