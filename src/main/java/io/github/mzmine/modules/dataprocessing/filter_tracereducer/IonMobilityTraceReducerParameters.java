package io.github.mzmine.modules.dataprocessing.filter_tracereducer;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import org.jetbrains.annotations.NotNull;

public class IonMobilityTraceReducerParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final BooleanParameter removeOriginal = new BooleanParameter("Remove original",
      "If checked, the original feature list is removed after this module finishes.", false);

  public static final StringParameter suffix = new StringParameter("Suffix",
      "The suffix to give to the new feature list.", "reduced", true);

  public IonMobilityTraceReducerParameters() {
    super(new Parameter[] {flists, removeOriginal, suffix});
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.ONLY;
  }
}
