package io.github.mzmine.modules.dataprocessing.id_biotransformer;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.ExitCode;
import javafx.application.Platform;
import javafx.collections.FXCollections;

public class BioTransformerParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final FileNameParameter bioPath = new FileNameParameter("Bio Transformer Path",
      "The path to bio transformer.", FileSelectionType.OPEN);

//  public static final StringParameter cmdOptions = new StringParameter("Command line options",
//      "Additional options to pass to the BioTransformer command line.", "");

  public static final ComboParameter<String> transformationType = new ComboParameter<String>(
      "Transformation type", "The Biotransformer transformation type to use.",
      FXCollections.observableArrayList("ecbased", "cyp450", "phaseii", "hgut", "allHuman",
          "superbio", "env"), "env");

  public static final IntegerParameter steps = new IntegerParameter("Steps",
      "The number of steps to use for bio transformer.", 1, 1, 10);

  public static final MZToleranceParameter mzTol = new MZToleranceParameter(0.003, 5);

  public BioTransformerParameters() {
    super(new Parameter[]{flists, bioPath, transformationType, steps, mzTol/*, cmdOptions*/});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    assert Platform.isFxApplicationThread();

    if ((parameters == null) || (parameters.length == 0)) {
      return ExitCode.OK;
    }
    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this, """
        Please cite:
        Djoumbou Feunang Y, Fiamoncini J, de la Fuente AG, Manach C, Greiner R, and Wishart DS; BioTransformer: A Comprehensive Computational Tool for Small Molecule Metabolism Prediction and Metabolite Identification; Journal of Cheminformatics; 2019; Journal of Cheminformatics 11:2; 
        DOI: 10.1186/s13321-018-0324-5
        """);
    dialog.showAndWait();
    return dialog.getExitCode();
  }
}
