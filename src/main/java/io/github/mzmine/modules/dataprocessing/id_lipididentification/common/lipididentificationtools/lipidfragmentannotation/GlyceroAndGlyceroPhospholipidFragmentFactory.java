package io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.lipidfragmentannotation;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.lipidchain.ILipidChain;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidannotationmodules.LipidAnnotationChainParameters;
import io.github.mzmine.util.FormulaUtils;
import java.util.ArrayList;
import java.util.List;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class GlyceroAndGlyceroPhospholipidFragmentFactory extends
    AbstractLipidFragmentFactory implements ILipidFragmentFactory {

  public GlyceroAndGlyceroPhospholipidFragmentFactory(Range<Double> mzTolRangeMSMS,
      ILipidAnnotation lipidAnnotation, IonizationType ionizationType,
      LipidFragmentationRule[] rules, DataPoint dataPoint, Scan msMsScan,
      LipidAnnotationChainParameters chainParameters) {
    super(mzTolRangeMSMS, lipidAnnotation, ionizationType, rules, dataPoint, msMsScan,
        chainParameters);
  }

  public List<LipidFragment> findLipidFragments() {
    List<LipidFragment> commonLipidFragments = findCommonLipidFragment();
    List<LipidFragment> lipidFragments = new ArrayList<>(commonLipidFragments);
    for (LipidFragmentationRule rule : rules) {
      if (!ionizationType.equals(rule.getIonizationType())
          || rule.getLipidFragmentationRuleType() == null) {
        continue;
      }
      LipidFragment detectedFragment = checkForGlyceroAndGlyceroPhospholipidSpecificRuleTypes(rule);
      if (detectedFragment != null) {
        lipidFragments.add(detectedFragment);
      }
    }
    return lipidFragments;
  }

  protected LipidFragment checkForGlyceroAndGlyceroPhospholipidSpecificRuleTypes(
      LipidFragmentationRule rule) {
    LipidFragmentationRuleType ruleType = rule.getLipidFragmentationRuleType();
    switch (ruleType) {
      case ACYLCHAIN_MINUS_FORMULA_FRAGMENT -> {
        //TODO
        return null;
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

  private LipidFragment checkForTwoAcylChainsPlusFormulaFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    IMolecularFormula modificationFormula = FormulaUtils.createMajorIsotopeMolFormula(
        rule.getMolecularFormula());
    List<ILipidChain> fattyAcylChainsOne = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.ACYL_CHAIN, minChainLength, maxChainLength, minDoubleBonds, maxDoubleBonds,
        onlySearchForEvenChains);
    List<ILipidChain> fattyAcylChainsTwo = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.ACYL_CHAIN, minChainLength, maxChainLength, minDoubleBonds, maxDoubleBonds,
        onlySearchForEvenChains);
    for (ILipidChain lipidChain1 : fattyAcylChainsOne) {
      IMolecularFormula lipidChainFormulaOne = lipidChain1.getChainMolecularFormula();
      for (ILipidChain lipidChain2 : fattyAcylChainsTwo) {
        IMolecularFormula lipidChainFormulaTwo = lipidChain2.getChainMolecularFormula();
        IMolecularFormula combinedChainsFormula = FormulaUtils.addFormula(lipidChainFormulaOne,
            lipidChainFormulaTwo);
        IMolecularFormula fragmentFormula = FormulaUtils.addFormula(combinedChainsFormula,
            modificationFormula);
        IMolecularFormula ionizedFragmentFormula = ionizeFragmentBasedOnPolarity(fragmentFormula,
            rule.getPolarityType());
        Double mzExact = FormulaUtils.calculateMzRatio(ionizedFragmentFormula);
        if (mzTolRangeMSMS.contains(mzExact)) {
          return new LipidFragment(rule.getLipidFragmentationRuleType(),
              rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
              mzExact, MolecularFormulaManipulator.getString(ionizedFragmentFormula), dataPoint,
              lipidAnnotation.getLipidClass(),
              lipidChain1.getNumberOfCarbons() + lipidChain2.getNumberOfCarbons(),
              lipidChain1.getNumberOfDBEs() + lipidChain2.getNumberOfDBEs(),
              lipidChain2.getNumberOfOxygens(), LipidChainType.TWO_ACYL_CHAINS_COMBINED, msMsScan);
        }
      }
    }
    return null;
  }

  private LipidFragment checkForAlkylChainPlusFormulaFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    IMolecularFormula modificationFormula = FormulaUtils.createMajorIsotopeMolFormula(
        rule.getMolecularFormula());
    List<ILipidChain> alkylChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.ALKYL_CHAIN, minChainLength, maxChainLength, minDoubleBonds, maxDoubleBonds,
        onlySearchForEvenChains);
    for (ILipidChain lipidChain : alkylChains) {
      IMolecularFormula lipidChainFormula = lipidChain.getChainMolecularFormula();
      IMolecularFormula fragmentFormula = FormulaUtils.addFormula(lipidChainFormula,
          modificationFormula);
      IMolecularFormula ionizedFragmentFormula = ionizeFragmentBasedOnPolarity(fragmentFormula,
          rule.getPolarityType());
      Double mzExact = FormulaUtils.calculateMzRatio(ionizedFragmentFormula);
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = lipidChain.getNumberOfCarbons();
        int numberOfDoubleBonds = lipidChain.getNumberOfDBEs();
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
            mzExact, MolecularFormulaManipulator.getString(ionizedFragmentFormula), dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            lipidChain.getNumberOfOxygens(), LipidChainType.ALKYL_CHAIN, msMsScan);
      }
    }
    return null;
  }

}
