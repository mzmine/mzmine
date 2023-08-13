package io.github.mzmine.modules.visualization.lipidannotationoverview;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import org.jetbrains.annotations.NotNull;

public class LipidAnnotationOverviewParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureList = new FeatureListsParameter(1, 1);

  public LipidAnnotationOverviewParameters() {
    super(new Parameter[]{featureList}, "");
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }


}