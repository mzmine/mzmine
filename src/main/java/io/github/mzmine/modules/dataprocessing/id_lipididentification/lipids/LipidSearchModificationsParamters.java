package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids;

import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.lipidmodifications.LipidModification;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.lipidmodifications.LipidModificationChoiceParameter;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;

public class LipidSearchModificationsParamters extends SimpleParameterSet {

  public static final LipidModificationChoiceParameter modification =
      new LipidModificationChoiceParameter("Lipid modifications", "Add lipid modifications",
          new LipidModification[0]);

  public LipidSearchModificationsParamters() {
    super(new Parameter[] {modification});
  }

}
