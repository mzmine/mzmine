package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.internal;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.mzio.general.Result;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * Represents a fragmentation rule of a custom lipid class. Not intended to be used as part of a
 * module, does not support saving.
 */
public class AddLipidFragmentationRuleParameters extends SimpleParameterSet {

  private static final Logger logger = Logger.getLogger(
      AddLipidFragmentationRuleParameters.class.getName());

  public static final ComboParameter<IonizationType> ionizationMethod = new ComboParameter<>(
      "Ionization method", "Type of ion used to calculate the ionized mass",
      IonizationType.values());
  public static final ComboParameter<LipidFragmentationRuleType> lipidFragmentationRuleType = new ComboParameter<>(
      "Lipid fragmentation rule type", "Choose the type of the lipid fragmentation rule",
      LipidFragmentationRuleType.values());
  public static final ComboParameter<LipidAnnotationLevel> lipidFragmentationRuleInformationLevel = new ComboParameter<>(
      "Lipid fragment information level",
      "Choose the information value of the lipid fragment, molecular formula level, or chain composition level",
      LipidAnnotationLevel.values());
  public static final ComboParameter<PolarityType> polarity = new ComboParameter<>("Polarity",
      "Select polarity type", new PolarityType[]{PolarityType.POSITIVE, PolarityType.NEGATIVE});

  public static final StringParameter formula = new StringParameter("Molecular formula",
      "Enter a molecular formula, if it is involved in the fragmentation rule. E.g. a head group fragment needs to be specified by its molecular formula.",
      "", false, false);

  public AddLipidFragmentationRuleParameters() {
    super(polarity, ionizationMethod, lipidFragmentationRuleType,
        lipidFragmentationRuleInformationLevel, formula);
    setModuleNameAttribute("Define a lipid fragmentation rule");
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages,
      boolean skipRawDataAndFeatureListParameters) {
    final boolean superCheck = super.checkParameterValues(errorMessages,
        skipRawDataAndFeatureListParameters);

    boolean thisCheck = true;
    final LipidFragmentationRuleType rule = getValue(
        AddLipidFragmentationRuleParameters.lipidFragmentationRuleType);
    final String formula = getValue(AddLipidFragmentationRuleParameters.formula);
    final Result check = LipidFragmentationRule.validate(rule, formula);
    if (!check.isOk()) {
      thisCheck = false;
      errorMessages.add(check.message());
    }

    return thisCheck && superCheck;
  }
}
