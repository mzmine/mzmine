package io.github.mzmine.modules.dataprocessing.featdet_smoothing2;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.util.ExitCode;
import java.util.Collection;

public class SmoothingParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final OptionalParameter<ComboParameter<Integer>> rtSmoothing = new OptionalParameter<>(
      new ComboParameter<Integer>("Retention time smoothing",
          "Enables intensity smoothing along the rt axis.",
          new Integer[]{0, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25}, 5));

  public static final OptionalParameter<ComboParameter<Integer>> mobilitySmoothing = new OptionalParameter<>(
      new ComboParameter<Integer>("Mobility smoothing",
          "Enables intensity smoothing of the summed mobilogram.",
          new Integer[]{0, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25}, 5));

  public static final BooleanParameter removeOriginal = new BooleanParameter(
      "Remove original feature list",
      "The originial feature list is removed after the processing has finished");

  public static final StringParameter suffix = new StringParameter("Suffix",
      "The suffix to be added to processed feature lists.", " sm");

  public SmoothingParameters() {
    super(new Parameter[]{featureLists, rtSmoothing, mobilitySmoothing, removeOriginal, suffix});
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages) {
    boolean superCheck = super.checkParameterValues(errorMessages);
    if (!superCheck) {
      return false;
    }

    if (!this.getParameter(mobilitySmoothing).getValue()
        && !this.getParameter(rtSmoothing).getValue()) {
      errorMessages.add("At least one smoothing type must be selected");
      return false;
    }
    return true;
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    ParameterSetupDialog dialog = new SmoothingSetupDialog(valueCheckRequired, this);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
