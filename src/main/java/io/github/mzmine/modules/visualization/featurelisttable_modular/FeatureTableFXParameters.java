package io.github.mzmine.modules.visualization.featurelisttable_modular;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;

public class FeatureTableFXParameters extends SimpleParameterSet {

  public static final BooleanParameter showXIC = new BooleanParameter("Show XIC",
      "Shows the XIC of the selected feature.", false);
  public static final BooleanParameter showSpectrum = new BooleanParameter("Show spectrum",
      "Shows the spectrum of the selected feature.", false);

  public FeatureTableFXParameters() {
    super(new Parameter[]{showXIC, showSpectrum});
  }

}
