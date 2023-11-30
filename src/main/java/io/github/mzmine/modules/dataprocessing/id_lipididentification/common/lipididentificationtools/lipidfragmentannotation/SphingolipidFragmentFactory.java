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

public class SphingolipidFragmentFactory extends AbstractLipidFragmentFactory implements
    ILipidFragmentFactory {

  public SphingolipidFragmentFactory(Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation,
      IonizationType ionizationType, LipidFragmentationRule[] rules, DataPoint dataPoint,
      Scan msMsScan, LipidAnnotationChainParameters chainParameters) {
    super(mzTolRangeMSMS, lipidAnnotation, ionizationType, rules, dataPoint, msMsScan,
        chainParameters);
  }

  @Override
  public List<LipidFragment> findLipidFragments() {
    List<LipidFragment> commonLipidFragments = findCommonLipidFragment();
    List<LipidFragment> lipidFragments = new ArrayList<>(commonLipidFragments);
    for (LipidFragmentationRule rule : rules) {
      if (!ionizationType.equals(rule.getIonizationType())
          || rule.getLipidFragmentationRuleType() == null) {
        continue;
      }
      LipidFragment detectedFragment = checkForSphingolipidSpecificRuleTypes(rule);
      if (detectedFragment != null) {
        lipidFragments.add(detectedFragment);
      }
    }
    return lipidFragments;
  }

  private LipidFragment checkForSphingolipidSpecificRuleTypes(LipidFragmentationRule rule) {
    LipidFragmentationRuleType ruleType = rule.getLipidFragmentationRuleType();
    switch (ruleType) {
      case SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN_FRAGMENT -> {
        checkForSphingolipidMonoHydroxyChainFragment(rule, mzTolRangeMSMS, lipidAnnotation,
            dataPoint, msMsScan);
      }
      case SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_FRAGMENT -> {
        return checkForSphingolipidDiHydroxyChainFragment(rule, mzTolRangeMSMS, lipidAnnotation,
            dataPoint, msMsScan);
      }
      case SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_FRAGMENT -> {
        return checkForSphingolipidTriHydroxyChainFragment(rule, mzTolRangeMSMS, lipidAnnotation,
            dataPoint, msMsScan);
      }
      case SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT -> {
        return checkForSphingolipidMonoHydroxyChainAndSubstructureNLFragment(rule, mzTolRangeMSMS,
            lipidAnnotation, dataPoint, msMsScan);
      }
      case SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT -> {
        return checkForSphingolipidDiHydroxyChainAndSubstructureNLFragment(rule, mzTolRangeMSMS,
            lipidAnnotation, dataPoint, msMsScan);
      }
      case SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT -> {
        return checkForSphingolipidTriHydroxyChainAndSubstructureNLFragment(rule, mzTolRangeMSMS,
            lipidAnnotation, dataPoint, msMsScan);
      }
      default -> {
        return null;
      }
    }

    return null;
  }

  private LipidFragment checkForSphingolipidMonoHydroxyChainFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    List<ILipidChain> sphingolipidBackboneChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN, minChainLength, maxChainLength,
        minDoubleBonds, maxDoubleBonds, onlySearchForEvenChains);
    for (ILipidChain lipidChain : sphingolipidBackboneChains) {
      IMolecularFormula sphingosineFormula = lipidChain.getChainMolecularFormula();
      rule.getIonizationType().ionizeFormula(sphingosineFormula);
      Double mzExact = FormulaUtils.calculateMzRatio(sphingosineFormula);
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = lipidChain.getNumberOfCarbons();
        int numberOfDoubleBonds = lipidChain.getNumberOfDBEs();
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
            mzExact, MolecularFormulaManipulator.getString(sphingosineFormula), dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            lipidChain.getNumberOfOxygens(),
            LipidChainType.SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN, msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForSphingolipidDiHydroxyChainFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    List<ILipidChain> sphingolipidBackboneChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN, minChainLength, maxChainLength,
        minDoubleBonds, maxDoubleBonds, onlySearchForEvenChains);
    for (ILipidChain lipidChain : sphingolipidBackboneChains) {
      IMolecularFormula sphingosineFormula = lipidChain.getChainMolecularFormula();
      rule.getIonizationType().ionizeFormula(sphingosineFormula);
      Double mzExact = FormulaUtils.calculateMzRatio(sphingosineFormula);
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = lipidChain.getNumberOfCarbons();
        int numberOfDoubleBonds = lipidChain.getNumberOfDBEs();
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
            mzExact, MolecularFormulaManipulator.getString(sphingosineFormula), dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            lipidChain.getNumberOfOxygens(), LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN,
            msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForSphingolipidTriHydroxyChainFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    List<ILipidChain> sphingolipidBackboneChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN, minChainLength, maxChainLength,
        minDoubleBonds, maxDoubleBonds, onlySearchForEvenChains);
    for (ILipidChain lipidChain : sphingolipidBackboneChains) {
      IMolecularFormula sphingosineFormula = lipidChain.getChainMolecularFormula();
      rule.getIonizationType().ionizeFormula(sphingosineFormula);
      Double mzExact = FormulaUtils.calculateMzRatio(sphingosineFormula);
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = lipidChain.getNumberOfCarbons();
        int numberOfDoubleBonds = lipidChain.getNumberOfDBEs();
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
            mzExact, MolecularFormulaManipulator.getString(sphingosineFormula), dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            lipidChain.getNumberOfOxygens(), LipidChainType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN,
            msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForSphingolipidMonoHydroxyChainAndSubstructureNLFragment(
      LipidFragmentationRule rule, Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation,
      DataPoint dataPoint, Scan msMsScan) {
    IMolecularFormula modificationFormula = FormulaUtils.createMajorIsotopeMolFormula(
        rule.getMolecularFormula());
    List<ILipidChain> sphingolipidBackboneChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN, minChainLength, maxChainLength,
        minDoubleBonds, maxDoubleBonds, onlySearchForEvenChains);
    for (ILipidChain lipidChain : sphingolipidBackboneChains) {
      IMolecularFormula sphingosineFormula = lipidChain.getChainMolecularFormula();
      IMolecularFormula fragmentFormula = FormulaUtils.subtractFormula(sphingosineFormula,
          modificationFormula);
      rule.getIonizationType().ionizeFormula(fragmentFormula);
      Double mzExact = FormulaUtils.calculateMzRatio(fragmentFormula);
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = lipidChain.getNumberOfCarbons();
        int numberOfDoubleBonds = lipidChain.getNumberOfDBEs();
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
            mzExact, MolecularFormulaManipulator.getString(fragmentFormula), dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            lipidChain.getNumberOfOxygens(),
            LipidChainType.SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN, msMsScan);
      }
    }
    return null;
  }


  private LipidFragment checkForSphingolipidDiHydroxyChainAndSubstructureNLFragment(
      LipidFragmentationRule rule, Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation,
      DataPoint dataPoint, Scan msMsScan) {
    IMolecularFormula modificationFormula = FormulaUtils.createMajorIsotopeMolFormula(
        rule.getMolecularFormula());
    List<ILipidChain> sphingolipidBackboneChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN, minChainLength, maxChainLength,
        minDoubleBonds, maxDoubleBonds, onlySearchForEvenChains);
    for (ILipidChain lipidChain : sphingolipidBackboneChains) {
      IMolecularFormula sphingosineFormula = lipidChain.getChainMolecularFormula();
      IMolecularFormula fragmentFormula = FormulaUtils.subtractFormula(sphingosineFormula,
          modificationFormula);
      ionizeFragmentBasedOnPolarity(fragmentFormula, rule.getPolarityType());
      Double mzExact = FormulaUtils.calculateMzRatio(fragmentFormula);
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = lipidChain.getNumberOfCarbons();
        int numberOfDoubleBonds = lipidChain.getNumberOfDBEs();
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
            mzExact, MolecularFormulaManipulator.getString(fragmentFormula), dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            lipidChain.getNumberOfOxygens(), LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN,
            msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForSphingolipidTriHydroxyChainAndSubstructureNLFragment(
      LipidFragmentationRule rule, Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation,
      DataPoint dataPoint, Scan msMsScan) {
    IMolecularFormula modificationFormula = FormulaUtils.createMajorIsotopeMolFormula(
        rule.getMolecularFormula());
    List<ILipidChain> sphingolipidBackboneChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN, minChainLength, maxChainLength,
        minDoubleBonds, maxDoubleBonds, onlySearchForEvenChains);
    for (ILipidChain lipidChain : sphingolipidBackboneChains) {
      IMolecularFormula sphingosineFormula = lipidChain.getChainMolecularFormula();
      IMolecularFormula fragmentFormula = FormulaUtils.subtractFormula(sphingosineFormula,
          modificationFormula);
      ionizeFragmentBasedOnPolarity(fragmentFormula, rule.getPolarityType());
      Double mzExact = FormulaUtils.calculateMzRatio(fragmentFormula);

      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = lipidChain.getNumberOfCarbons();
        int numberOfDoubleBonds = lipidChain.getNumberOfDBEs();
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
            mzExact, MolecularFormulaManipulator.getString(fragmentFormula), dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            lipidChain.getNumberOfOxygens(), LipidChainType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN,
            msMsScan);
      }
    }
    return null;
  }

}
