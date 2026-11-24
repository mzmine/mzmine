package io.github.mzmine.modules.tools.batchwizard.subparameters;

import io.github.mzmine.modules.tools.batchwizard.LipidDoubleBondWizardTechnique;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;

/**
 * Options of the wizard lipid annotation, shown below "Annotate lipids".
 */
public class LipidAnnotationWizardSubParameters extends SimpleParameterSet {

  public static final OptionalParameter<ComboParameter<LipidDoubleBondWizardTechnique>> lipidDoubleBonds = new OptionalParameter<>(
      new ComboParameter<>("Localize lipid double bonds", """
          Adds double bond positions to the lipid annotations, e.g., PC 16:0_18:1(9).
          Requires MS2 spectra that were acquired with the selected fragmentation technique,
          e.g., OAD (oxygen attachment dissociation).
          Only use this option for data acquired with this technique.""",
          LipidDoubleBondWizardTechnique.values(), LipidDoubleBondWizardTechnique.OAD), false);

  public LipidAnnotationWizardSubParameters() {
    super(lipidDoubleBonds);
  }
}
