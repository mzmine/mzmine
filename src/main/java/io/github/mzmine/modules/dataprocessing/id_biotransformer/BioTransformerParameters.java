package io.github.mzmine.modules.dataprocessing.id_biotransformer;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import javafx.collections.FXCollections;

public class BioTransformerParameters extends SimpleParameterSet {

  public static final FileNameParameter bioPath = new FileNameParameter("Bio Transformer Path",
      "The path to bio transformer.", FileSelectionType.OPEN);

  public static final StringParameter cmdOptions = new StringParameter("Command line options",
      "Additional options to pass to the BioTransformer command line.", "");

  public static final ComboParameter<String> transformationType = new ComboParameter<String>(
      "Transformation type", "The Biotransformer transformation type to use.",
      FXCollections.observableArrayList("ecbased", "cyp450", "phaseii", "hgut", "allHuman",
          "superbio", "env"), "env");

  public static final IntegerParameter steps = new IntegerParameter("Steps",
      "The number of steps to use for bio transformer.", 1, 1, 10);

  public BioTransformerParameters() {
    super(new Parameter[]{bioPath, transformationType, steps, cmdOptions});
  }
}
