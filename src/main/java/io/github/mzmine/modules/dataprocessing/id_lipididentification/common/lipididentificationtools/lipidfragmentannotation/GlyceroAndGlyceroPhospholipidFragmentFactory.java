package io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.lipidfragmentannotation;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.lipidchain.ILipidChain;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipidutils.LipidChainFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidannotationmodules.glyceroandglycerophospholipids.GlyceroAndGlycerophospholipidAnnotationChainParameters;
import io.github.mzmine.util.FormulaUtils;
import java.util.ArrayList;
import java.util.List;
import org.openscience.cdk.interfaces.IMolecularFormula;
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

  public List<LipidFragment> findLipidFragments() {
    List<LipidFragment> commonLipidFragments = findCommonLipidFragment();
    if (commonLipidFragments != null && !commonLipidFragments.isEmpty()) {
      return commonLipidFragments;
    }
    List<LipidFragment> lipidFragments = new ArrayList<>();
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
        IMolecularFormula lipidChainFormula = lipidChain.getChainMolecularFormula();
        IonizationType.NEGATIVE_HYDROGEN.ionizeFormula(lipidChainFormula);
        Double mzExact = FormulaUtils.calculateMzRatio(lipidChainFormula);
        if (mzTolRangeMSMS.contains(mzExact)) {
          int chainLength = lipidChain.getNumberOfCarbons();
          int numberOfDoubleBonds = lipidChain.getNumberOfDBEs();
          return new LipidFragment(rule.getLipidFragmentationRuleType(),
              rule.getLipidFragmentInformationLevelType(), mzExact,
              MolecularFormulaManipulator.getString(lipidChainFormula), dataPoint,
              lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
              lipidChain.getNumberOfOxygens(), LipidChainType.ACYL_CHAIN, msMsScan);
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
    for (ILipidChain lipidChain : fattyAcylChains) {
      IMolecularFormula lipidFormula = null;
      try {
        lipidFormula = (IMolecularFormula) lipidAnnotation.getMolecularFormula().clone();
      } catch (CloneNotSupportedException e) {
        throw new RuntimeException(e);
      }
      rule.getIonizationType().ionizeFormula(lipidFormula);
      IMolecularFormula lipidChainFormula = lipidChain.getChainMolecularFormula();
      IMolecularFormula fragmentFormula = FormulaUtils.subtractFormula(lipidFormula,
          lipidChainFormula);
      Double mzExact = FormulaUtils.calculateMzRatio(fragmentFormula);
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = lipidChain.getNumberOfCarbons();
        int numberOfDoubleBonds = lipidChain.getNumberOfDBEs();
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), mzExact,
            MolecularFormulaManipulator.getString(fragmentFormula), dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            lipidChain.getNumberOfOxygens(), LipidChainType.ACYL_CHAIN, msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForAcylChainMinusFormulaFragmentNL(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    IMolecularFormula modificationFormula = FormulaUtils.createMajorIsotopeMolFormula(
        rule.getMolecularFormula());
    List<ILipidChain> fattyAcylChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.ACYL_CHAIN, minChainLength, maxChainLength, minDoubleBonds, maxDoubleBonds,
        onlySearchForEvenChains);
    for (ILipidChain lipidChain : fattyAcylChains) {
      IMolecularFormula lipidFormula = null;
      try {
        lipidFormula = (IMolecularFormula) lipidAnnotation.getMolecularFormula().clone();
      } catch (CloneNotSupportedException e) {
        throw new RuntimeException(e);
      }
      rule.getIonizationType().ionizeFormula(lipidFormula);
      IMolecularFormula lipidChainFormula = lipidChain.getChainMolecularFormula();
      IMolecularFormula fragmentFormula = FormulaUtils.subtractFormula(lipidChainFormula,
          modificationFormula);
      IMolecularFormula lipidMinusFragmentFormula = FormulaUtils.subtractFormula(lipidFormula,
          fragmentFormula);
      Double mzExact = FormulaUtils.calculateMzRatio(lipidMinusFragmentFormula);
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = lipidChain.getNumberOfCarbons();
        int numberOfDoubleBonds = lipidChain.getNumberOfDBEs();
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), mzExact,
            MolecularFormulaManipulator.getString(lipidMinusFragmentFormula), dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            lipidChain.getNumberOfOxygens(), LipidChainType.ACYL_CHAIN, msMsScan);
      }
    }
    return null;
  }


  private LipidFragment checkForAcylChainPlusFormulaFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    IMolecularFormula modificationFormula = FormulaUtils.createMajorIsotopeMolFormula(
        rule.getMolecularFormula());
    List<ILipidChain> fattyAcylChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.ACYL_CHAIN, minChainLength, maxChainLength, minDoubleBonds, maxDoubleBonds,
        onlySearchForEvenChains);
    for (ILipidChain lipidChain : fattyAcylChains) {
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
            rule.getLipidFragmentInformationLevelType(), mzExact,
            MolecularFormulaManipulator.getString(ionizedFragmentFormula), dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            lipidChain.getNumberOfOxygens(), LipidChainType.ACYL_CHAIN, msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForAcylChainPlusFormulaFragmentNL(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    IMolecularFormula modificationFormula = FormulaUtils.createMajorIsotopeMolFormula(
        rule.getMolecularFormula());
    List<ILipidChain> fattyAcylChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.ACYL_CHAIN, minChainLength, maxChainLength, minDoubleBonds, maxDoubleBonds,
        onlySearchForEvenChains);
    for (ILipidChain lipidChain : fattyAcylChains) {
      IMolecularFormula lipidFormula = null;
      try {
        lipidFormula = (IMolecularFormula) lipidAnnotation.getMolecularFormula().clone();
      } catch (CloneNotSupportedException e) {
        throw new RuntimeException(e);
      }
      rule.getIonizationType().ionizeFormula(lipidFormula);
      IMolecularFormula lipidChainFormula = lipidChain.getChainMolecularFormula();
      IMolecularFormula fragmentFormula = FormulaUtils.addFormula(lipidChainFormula,
          modificationFormula);
      IMolecularFormula lipidMinusFragmentFormula = FormulaUtils.subtractFormula(lipidFormula,
          fragmentFormula);
      Double mzExact = FormulaUtils.calculateMzRatio(lipidMinusFragmentFormula);
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = lipidChain.getNumberOfCarbons();
        int numberOfDoubleBonds = lipidChain.getNumberOfDBEs();
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), mzExact,
            MolecularFormulaManipulator.getString(lipidMinusFragmentFormula), dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            lipidChain.getNumberOfOxygens(), LipidChainType.ACYL_CHAIN, msMsScan);
      }
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
              rule.getLipidFragmentInformationLevelType(), mzExact,
              MolecularFormulaManipulator.getString(ionizedFragmentFormula), dataPoint,
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
            rule.getLipidFragmentInformationLevelType(), mzExact,
            MolecularFormulaManipulator.getString(ionizedFragmentFormula), dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            lipidChain.getNumberOfOxygens(), LipidChainType.ALKYL_CHAIN, msMsScan);
      }
    }
    return null;
  }

}
