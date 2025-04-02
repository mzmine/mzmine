package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.internal;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidCategories;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidMainClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.FormulaUtils;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import org.w3c.dom.Element;

/**
 * Represents a custom lipid class. Not intended to be used as part of a module, does not support
 * saving.
 */
public class AddCustomLipidClassParameters extends SimpleParameterSet {

  public static final StringParameter name = new StringParameter("Custom lipid class name",
      "Enter the name of the custom lipid class", "My lipid class", true);

  public static final StringParameter abbr = new StringParameter("Custom lipid class abbreviation",
      "Enter a abbreviation for the custom lipid class", "MyClass", true);

  public static final StringParameter backBoneFormula = new StringParameter(
      "Lipid backbone molecular formula",
      "Enter the backbone molecular formula of the custom lipid class. Include all elements of the original molecular, e.g. in case of glycerol based lipid classes add C3H8O3. "
          + "For fatty acids start with H2O, for ceramides start with C3H8", "C3H8O3", true);

  public static final ComboParameter<LipidMainClasses> lipidMainClass = new ComboParameter<>(
      "Lipid main class", "Enter the name of the custom lipid class", LipidMainClasses.values(),
      LipidMainClasses.PHOSPHATIDYLCHOLINE);

  public static final ComboParameter<LipidCategories> lipidCategory = new ComboParameter<>(
      "Lipid category",
      "The selected lipid category influences the calculation of the lipid class and the available fragmentation rules",
      new LipidCategories[]{LipidCategories.FATTYACYLS, LipidCategories.GLYCEROLIPIDS,
          LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidCategories.SPHINGOLIPIDS,
          LipidCategories.STEROLLIPIDS}, LipidCategories.GLYCEROPHOSPHOLIPIDS);

  public static final CustomLipidChainChoiceParameter lipidChainTypes = new CustomLipidChainChoiceParameter(
      "Add lipid chains", "Add Lipid Chains",
      new LipidChainType[]{LipidChainType.ACYL_CHAIN, LipidChainType.ACYL_CHAIN});

  public static final CustomLipidClassFragmentationRulesChoiceParameter customLipidClassFragmentationRules = new CustomLipidClassFragmentationRulesChoiceParameter(
      "Add fragmentation rules", "Add custom lipid class fragmentation rules",
      new LipidFragmentationRule[]{
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN)});

  public AddCustomLipidClassParameters() {
    super(lipidCategory, lipidMainClass, name, abbr, backBoneFormula, lipidChainTypes,
        customLipidClassFragmentationRules);
    setModuleNameAttribute("Define a custom lipid");
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages,
      boolean skipRawDataAndFeatureListParameters) {
    final boolean superCheck = super.checkParameterValues(errorMessages,
        skipRawDataAndFeatureListParameters);

    boolean thisCheck = true;
    final String formula = getValue(backBoneFormula);

    if (!FormulaUtils.checkMolecularFormula(formula)) {
      errorMessages.add("Invalid backbone formula: " + formula);
      thisCheck = false;
    }

    if (!getValue(lipidCategory).equals(LipidCategories.SPHINGOLIPIDS) && Arrays.stream(
        getValue(lipidChainTypes)).anyMatch(lipidChainType ->
        lipidChainType.equals(LipidChainType.SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN)
            || lipidChainType.equals(LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN)
            || lipidChainType.equals(LipidChainType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN))) {
      final String message =
          "You are using a sphingolipid specific chain for a lipid of the category "
              + lipidCategory.getValue()
              + ". This may result in unexpected behaviour and is not recommended. Please select Sphingolipids as lipid category.";
      errorMessages.add(message);
      thisCheck = false;
    }

    if (!getValue(lipidCategory).equals(LipidCategories.SPHINGOLIPIDS) && Arrays.stream(
        getValue(customLipidClassFragmentationRules)).anyMatch(
        rule -> (rule != null && rule.getLipidFragmentationRuleType() != null && (
            rule.getLipidFragmentationRuleType().equals(
                LipidFragmentationRuleType.SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN_FRAGMENT)
                || rule.getLipidFragmentationRuleType()
                .equals(LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_FRAGMENT)
                || rule.getLipidFragmentationRuleType()
                .equals(LipidFragmentationRuleType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_FRAGMENT)
                || rule.getLipidFragmentationRuleType().equals(
                LipidFragmentationRuleType.SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT)
                || rule.getLipidFragmentationRuleType().equals(
                LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT)
                || rule.getLipidFragmentationRuleType().equals(
                LipidFragmentationRuleType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT))))) {
      String message = "Lipid fragmentation rule warning. "
          + "You are using a sphingolipid specific fragmentation rule for a lipid of the category "
          + lipidCategory.getValue()
          + ". This may result in unexpected behaviour and is not recommended. Please select Sphingolipids as lipid category.";
      errorMessages.add(message);
      thisCheck = false;
    }

    return superCheck && thisCheck;
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    AddCustomLipidClassSetupDialog dialog = new AddCustomLipidClassSetupDialog(valueCheckRequired,
        this);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public Map<String, Parameter<?>> loadValuesFromXML(Element xmlElement) {
    throw new UnsupportedOperationException(
        "This parameter set is only intended for GUI usage and for loading and saving to an xml file.");
  }

  @Override
  public void saveValuesToXML(Element xmlElement) {
    throw new UnsupportedOperationException(
        "This parameter set is only intended for GUI usage and for loading and saving to an xml file.");
  }
}
