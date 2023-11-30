package io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.customlipidclass;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.customlipidclass.CustomLipidClassFragmentationRulesChoiceComponent.AddLipidFragmentationRuleParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;

public class AddLipidFragmentationRuleSetupDialog extends ParameterSetupDialog {

  private ParameterSet parameters;
  private PolarityType polarityType;
  private IonizationType ionizationType;
  private LipidFragmentationRuleType lipidFragmentationRuleType;
  private LipidAnnotationLevel lipidAnnotationLevel;
  private String formula;

  public AddLipidFragmentationRuleSetupDialog(boolean valueCheckRequired, ParameterSet parameters) {
    super(valueCheckRequired, parameters);
    this.parameters = parameters;
    this.polarityType = parameters.getParameter(AddLipidFragmentationRuleParameters.polarity)
        .getValue();
    this.ionizationType = parameters.getParameter(
        AddLipidFragmentationRuleParameters.ionizationMethod).getValue();
    this.lipidFragmentationRuleType = parameters.getParameter(
        AddLipidFragmentationRuleParameters.lipidFragmentationRuleType).getValue();
    this.lipidAnnotationLevel = parameters.getParameter(
        AddLipidFragmentationRuleParameters.lipidFragmentationRuleInformationLevel).getValue();
    this.formula = parameters.getParameter(AddLipidFragmentationRuleParameters.formula).getValue();
  }

  @Override
  protected void parametersChanged() {
    super.parametersChanged();
    this.updateParameterSetFromComponents();
    polarityType = parameters.getParameter(AddLipidFragmentationRuleParameters.polarity).getValue();
    ionizationType = parameters.getParameter(AddLipidFragmentationRuleParameters.ionizationMethod)
        .getValue();
    lipidFragmentationRuleType = parameters.getParameter(
        AddLipidFragmentationRuleParameters.lipidFragmentationRuleType).getValue();
    lipidAnnotationLevel = parameters.getParameter(
        AddLipidFragmentationRuleParameters.lipidFragmentationRuleInformationLevel).getValue();
    formula = parameters.getParameter(AddLipidFragmentationRuleParameters.formula).getValue();
  }


}
