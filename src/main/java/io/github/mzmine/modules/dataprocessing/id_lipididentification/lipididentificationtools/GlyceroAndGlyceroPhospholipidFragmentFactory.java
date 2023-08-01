package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.lipidchain.ILipidChain;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipidutils.LipidChainFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidannotationmodules.glyceroandglycerophospholipids.GlyceroAndGlycerophospholipidAnnotationChainParameters;
import io.github.mzmine.util.FormulaUtils;
import java.util.List;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class GlyceroAndGlyceroPhospholipidFragmentFactory extends
    AbstractLipidFragmentFactory implements ILipidFragmentFactory {

  private static final LipidChainFactory LIPID_CHAIN_FACTORY = new LipidChainFactory();
  private final int minChainLength;
  private final int maxChainLength;
  private final int maxDoubleBonds;
  private final int minDoubleBonds;
  private final Boolean onlySearchForEvenChains;

  public GlyceroAndGlyceroPhospholipidFragmentFactory(Range<Double> mzTolRangeMSMS,
      ILipidAnnotation lipidAnnotation, IonizationType ionizationType,
      LipidFragmentationRule[] rules, DataPoint dataPoint, Scan msMsScan,
      GlyceroAndGlycerophospholipidAnnotationChainParameters chainParameters) {
    super(mzTolRangeMSMS, lipidAnnotation, ionizationType, rules, dataPoint, msMsScan);
    this.minChainLength = chainParameters.getParameter(
        GlyceroAndGlycerophospholipidAnnotationChainParameters.minChainLength).getValue();
    this.maxChainLength = chainParameters.getParameter(
        GlyceroAndGlycerophospholipidAnnotationChainParameters.maxChainLength).getValue();
    this.minDoubleBonds = chainParameters.getParameter(
        GlyceroAndGlycerophospholipidAnnotationChainParameters.minDBEs).getValue();
    this.maxDoubleBonds = chainParameters.getParameter(
        GlyceroAndGlycerophospholipidAnnotationChainParameters.maxDBEs).getValue();
    this.onlySearchForEvenChains = chainParameters.getParameter(
            GlyceroAndGlycerophospholipidAnnotationChainParameters.onlySearchForEvenChainLength)
        .getValue();
  }

  public LipidFragment findLipidFragment() {
    LipidFragment commonLipidFragment = findCommonLipidFragment();
    if (commonLipidFragment != null) {
      return commonLipidFragment;
    }
    LipidFragment lipidFragment = null;
    for (LipidFragmentationRule rule : rules) {
      if (!ionizationType.equals(rule.getIonizationType())
          || rule.getLipidFragmentationRuleType() == null) {
        continue;
      }
      LipidFragment detectedFragment = checkForGlyceroAndGlyceroPhospholipidSpecificRuleTypes(rule);
      if (detectedFragment != null) {
        lipidFragment = detectedFragment;
        break;
      }
    }
    return lipidFragment;
  }

  private LipidFragment checkForGlyceroAndGlyceroPhospholipidSpecificRuleTypes(
      LipidFragmentationRule rule) {
    LipidFragmentationRuleType ruleType = rule.getLipidFragmentationRuleType();
    switch (ruleType) {
      case ACYLCHAIN_FRAGMENT -> {
        return checkForAcylChainFragment(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint,
            msMsScan);
      }
      case ACYLCHAIN_FRAGMENT_NL -> {
        return checkForAcylChainFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint,
            msMsScan);
      }
      case ACYLCHAIN_MINUS_FORMULA_FRAGMENT -> {
        //TODO
        return null;
      }
      case ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL -> {
        return checkForAcylChainMinusFormulaFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation,
            dataPoint, msMsScan);
      }
      case ACYLCHAIN_PLUS_FORMULA_FRAGMENT -> {
        return checkForAcylChainPlusFormulaFragment(rule, mzTolRangeMSMS, lipidAnnotation,
            dataPoint, msMsScan);
      }
      case ACYLCHAIN_PLUS_FORMULA_FRAGMENT_NL -> {
        return checkForAcylChainPlusFormulaFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation,
            dataPoint, msMsScan);
      }
      case TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT -> {
        return checkForTwoAcylChainsPlusFormulaFragment(rule, mzTolRangeMSMS, lipidAnnotation,
            dataPoint, msMsScan);
      }
//      case ALKYLCHAIN_FRAGMENT -> {
//        // TODO
//        return null;
//      }
//      case ALKYLCHAIN_FRAGMENT_NL -> {
//        // TODO
//        return null;
//      }
      case ALKYLCHAIN_PLUS_FORMULA_FRAGMENT -> {
        // TODO
        return checkForAlkylChainPlusFormulaFragment(rule, mzTolRangeMSMS, lipidAnnotation,
            dataPoint, msMsScan);
      }
//      case ALKYLCHAIN_PLUS_FORMULA_FRAGMENT_NL -> {
//        // TODO
//        return null;
//      }
//      case ALKYLCHAIN_MINUS_FORMULA_FRAGMENT -> {
//        // TODO
//        return null;
//      }
//      case ALKYLCHAIN_MINUS_FORMULA_FRAGMENT_NL -> {
//        // TODO
//        return null;
//      }
    }
    return null;
  }


  private LipidFragment checkForAcylChainFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {

    if (rule.getPolarityType().equals(PolarityType.NEGATIVE)) {
      List<ILipidChain> fattyAcylChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
          LipidChainType.ACYL_CHAIN, minChainLength, maxChainLength, minDoubleBonds, maxDoubleBonds,
          onlySearchForEvenChains);
      for (ILipidChain lipidChain : fattyAcylChains) {
        Double mzExact = MolecularFormulaManipulator.getMass(lipidChain.getChainMolecularFormula(),
            MolecularFormulaManipulator.MonoIsotopic)
            + IonizationType.NEGATIVE_HYDROGEN.getAddedMass();
        if (mzTolRangeMSMS.contains(mzExact)) {
          int chainLength = lipidChain.getNumberOfCarbons();
          int numberOfDoubleBonds = lipidChain.getNumberOfDBEs();
          return new LipidFragment(rule.getLipidFragmentationRuleType(),
              rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
              lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
              LipidChainType.ACYL_CHAIN, msMsScan);
        }
      }
    }
    return null;
  }

  private LipidFragment checkForAcylChainFragmentNL(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    List<ILipidChain> fattyAcylChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.ACYL_CHAIN, minChainLength, maxChainLength, minDoubleBonds, maxDoubleBonds,
        onlySearchForEvenChains);
    Double mzPrecursorExact =
        MolecularFormulaManipulator.getMass(lipidAnnotation.getMolecularFormula(),
            AtomContainerManipulator.MonoIsotopic) + rule.getIonizationType().getAddedMass();
    for (ILipidChain lipidChain : fattyAcylChains) {
      Double mzFattyAcid = MolecularFormulaManipulator.getMass(
          lipidChain.getChainMolecularFormula(), MolecularFormulaManipulator.MonoIsotopic);
      Double mzExact = mzPrecursorExact - mzFattyAcid;
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = lipidChain.getNumberOfCarbons();
        int numberOfDoubleBonds = lipidChain.getNumberOfDBEs();
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            LipidChainType.ACYL_CHAIN, msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForAcylChainMinusFormulaFragmentNL(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    String fragmentFormula = rule.getMolecularFormula();
    Double mzPrecursorExact =
        MolecularFormulaManipulator.getMass(lipidAnnotation.getMolecularFormula(),
            AtomContainerManipulator.MonoIsotopic) + rule.getIonizationType().getAddedMass();
    Double mzFragmentExact = FormulaUtils.calculateExactMass(fragmentFormula);
    List<ILipidChain> fattyAcylChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.ACYL_CHAIN, minChainLength, maxChainLength, minDoubleBonds, maxDoubleBonds,
        onlySearchForEvenChains);
    for (ILipidChain lipidChain : fattyAcylChains) {
      Double mzChainMinusmzFragment =
          MolecularFormulaManipulator.getMass(lipidChain.getChainMolecularFormula(),
              MolecularFormulaManipulator.MonoIsotopic) - mzFragmentExact;
      Double mzExact = mzPrecursorExact - mzChainMinusmzFragment;
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = lipidChain.getNumberOfCarbons();
        int numberOfDoubleBonds = lipidChain.getNumberOfDBEs();
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            LipidChainType.ACYL_CHAIN, msMsScan);
      }
    }
    return null;
  }


  private LipidFragment checkForAcylChainPlusFormulaFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {

    String fragmentFormula = rule.getMolecularFormula();
    Double mzFragmentExact = FormulaUtils.calculateExactMass(fragmentFormula);
    List<ILipidChain> fattyAcylChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.ACYL_CHAIN, minChainLength, maxChainLength, minDoubleBonds, maxDoubleBonds,
        onlySearchForEvenChains);
    for (ILipidChain lipidChain : fattyAcylChains) {
      Double mzExact = MolecularFormulaManipulator.getMass(lipidChain.getChainMolecularFormula(),
          MolecularFormulaManipulator.MonoIsotopic) + mzFragmentExact;
      mzExact = ionizeFragmentBasedOnPolarity(mzExact, rule.getPolarityType());
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = lipidChain.getNumberOfCarbons();
        int numberOfDoubleBonds = lipidChain.getNumberOfDBEs();
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            LipidChainType.ACYL_CHAIN, msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForAcylChainPlusFormulaFragmentNL(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    String fragmentFormula = rule.getMolecularFormula();
    Double mzPrecursorExact =
        MolecularFormulaManipulator.getMass(lipidAnnotation.getMolecularFormula(),
            AtomContainerManipulator.MonoIsotopic) + rule.getIonizationType().getAddedMass();
    Double mzFragmentExact = FormulaUtils.calculateExactMass(fragmentFormula);
    List<ILipidChain> fattyAcylChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.ACYL_CHAIN, minChainLength, maxChainLength, minDoubleBonds, maxDoubleBonds,
        onlySearchForEvenChains);
    for (ILipidChain lipidChain : fattyAcylChains) {
      Double mzChainPlusmzFragment =
          MolecularFormulaManipulator.getMass(lipidChain.getChainMolecularFormula(),
              MolecularFormulaManipulator.MonoIsotopic) + mzFragmentExact;
      Double mzExact = mzPrecursorExact - mzChainPlusmzFragment;
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = lipidChain.getNumberOfCarbons();
        int numberOfDoubleBonds = lipidChain.getNumberOfDBEs();
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            LipidChainType.ACYL_CHAIN, msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForTwoAcylChainsPlusFormulaFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    String fragmentFormula = rule.getMolecularFormula();
    Double mzFragmentExact = FormulaUtils.calculateExactMass(fragmentFormula);
    List<ILipidChain> fattyAcylChainsOne = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.ACYL_CHAIN, minChainLength, maxChainLength, minDoubleBonds, maxDoubleBonds,
        onlySearchForEvenChains);
    List<ILipidChain> fattyAcylChainsTwo = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.ACYL_CHAIN, minChainLength, maxChainLength, minDoubleBonds, maxDoubleBonds,
        onlySearchForEvenChains);
    for (int i = 0; i < fattyAcylChainsOne.size(); i++) {
      Double mzFattyAcidOne = MolecularFormulaManipulator.getMass(
          fattyAcylChainsOne.get(i).getChainMolecularFormula(),
          MolecularFormulaManipulator.MonoIsotopic);
      for (int j = 0; j < fattyAcylChainsTwo.size(); j++) {
        Double mzFattyAcidTwo = MolecularFormulaManipulator.getMass(
            fattyAcylChainsTwo.get(j).getChainMolecularFormula(),
            MolecularFormulaManipulator.MonoIsotopic);
        Double mzExact = mzFattyAcidOne + mzFattyAcidTwo + mzFragmentExact;
        mzExact = ionizeFragmentBasedOnPolarity(mzExact, rule.getPolarityType());
        if (mzTolRangeMSMS.contains(mzFattyAcidOne + mzFattyAcidTwo + mzFragmentExact)) {
          return new LipidFragment(rule.getLipidFragmentationRuleType(),
              rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
              lipidAnnotation.getLipidClass(),
              fattyAcylChainsOne.get(i).getNumberOfCarbons() + fattyAcylChainsTwo.get(j)
                  .getNumberOfCarbons(),
              fattyAcylChainsOne.get(i).getNumberOfDBEs() + fattyAcylChainsTwo.get(j)
                  .getNumberOfDBEs(), LipidChainType.TWO_ACYL_CHAINS_COMBINED, msMsScan);
        }
      }
    }
    return null;
  }

  private LipidFragment checkForAlkylChainPlusFormulaFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {

    String fragmentFormula = rule.getMolecularFormula();
    Double mzFragmentExact = FormulaUtils.calculateExactMass(fragmentFormula);
    List<ILipidChain> alkylChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.ALKYL_CHAIN, minChainLength, maxChainLength, minDoubleBonds, maxDoubleBonds,
        onlySearchForEvenChains);
    for (ILipidChain lipidChain : alkylChains) {
      Double mzExact = MolecularFormulaManipulator.getMass(lipidChain.getChainMolecularFormula(),
          MolecularFormulaManipulator.MonoIsotopic) + mzFragmentExact;
      mzExact = ionizeFragmentBasedOnPolarity(mzExact, rule.getPolarityType());

      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = lipidChain.getNumberOfCarbons();
        int numberOfDoubleBonds = lipidChain.getNumberOfDBEs();
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            LipidChainType.ALKYL_CHAIN, msMsScan);
      }
    }
    return null;
  }

}
