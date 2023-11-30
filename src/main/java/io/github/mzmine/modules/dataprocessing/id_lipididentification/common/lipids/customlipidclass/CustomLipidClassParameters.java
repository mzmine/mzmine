package io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.customlipidclass;

import io.github.mzmine.parameters.impl.SimpleParameterSet;

public class CustomLipidClassParameters extends SimpleParameterSet {

  public static final CustomLipidClassChoiceParameter customLipidClassChoices = new CustomLipidClassChoiceParameter(
      "Custom lipid classes", "Click add to define custom lipid classes", new CustomLipidClass[0]);

  public CustomLipidClassParameters() {
    super(customLipidClassChoices);
  }

}
