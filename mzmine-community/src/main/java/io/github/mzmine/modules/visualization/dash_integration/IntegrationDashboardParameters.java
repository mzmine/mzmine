package io.github.mzmine.modules.visualization.dash_integration;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;

public class IntegrationDashboardParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter(1, 1);

  public IntegrationDashboardParameters() {
    super(flists);
  }

}
