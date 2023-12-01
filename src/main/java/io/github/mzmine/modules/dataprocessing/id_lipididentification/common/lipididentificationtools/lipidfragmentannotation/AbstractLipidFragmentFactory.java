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
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidannotationmodules.LipidAnnotationChainParameters;
import io.github.mzmine.util.FormulaUtils;
import java.util.ArrayList;
import java.util.List;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public abstract class AbstractLipidFragmentFactory {

  protected static final LipidChainFactory LIPID_CHAIN_FACTORY = new LipidChainFactory();

  protected Range<Double> mzTolRangeMSMS;
  protected ILipidAnnotation lipidAnnotation;
  protected IonizationType ionizationType;
  protected LipidFragmentationRule[] rules;
  protected DataPoint dataPoint;
  protected Scan msMsScan;

  protected final int minChainLength;
  protected final int maxChainLength;
  protected final int maxDoubleBonds;
  protected final int minDoubleBonds;
  protected final Boolean onlySearchForEvenChains;

  public AbstractLipidFragmentFactory(Range<Double> mzTolRangeMSMS,
      ILipidAnnotation lipidAnnotation, IonizationType ionizationType,
      LipidFragmentationRule[] rules, DataPoint dataPoint, Scan msMsScan,
      LipidAnnotationChainParameters chainParameters) {
    this.mzTolRangeMSMS = mzTolRangeMSMS;
    this.lipidAnnotation = lipidAnnotation;
    this.ionizationType = ionizationType;
    this.rules = rules;
    this.dataPoint = dataPoint;
    this.msMsScan = msMsScan;
    this.minChainLength = chainParameters.getParameter(
        LipidAnnotationChainParameters.minChainLength).getValue();
    this.maxChainLength = chainParameters.getParameter(
        LipidAnnotationChainParameters.maxChainLength).getValue();
    this.minDoubleBonds = chainParameters.getParameter(LipidAnnotationChainParameters.minDBEs)
        .getValue();
    this.maxDoubleBonds = chainParameters.getParameter(LipidAnnotationChainParameters.maxDBEs)
        .getValue();
    this.onlySearchForEvenChains = chainParameters.getParameter(
        LipidAnnotationChainParameters.onlySearchForEvenChainLength).getValue();
  }

  public List<LipidFragment> findCommonLipidFragment() {
    List<LipidFragment> lipidFragment = new ArrayList<>();
    for (LipidFragmentationRule rule : rules) {
      if (!ionizationType.equals(rule.getIonizationType())
          || rule.getLipidFragmentationRuleType() == null) {
        continue;
      }
      LipidFragment detectedFragment = checkForCommonRuleTypes(rule);
      if (detectedFragment != null) {
        lipidFragment.add(detectedFragment);
        break;
      }
    }
    return lipidFragment;
  }

  private LipidFragment checkForCommonRuleTypes(LipidFragmentationRule rule) {
    LipidFragmentationRuleType ruleType = rule.getLipidFragmentationRuleType();
    return switch (ruleType) {
      case HEADGROUP_FRAGMENT ->
          checkForHeadgroupFragment(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint, msMsScan);
      case HEADGROUP_FRAGMENT_NL ->
          checkForHeadgroupFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint, msMsScan);
      case PRECURSOR ->
          checkForOnlyPrecursor(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint, msMsScan);
      case ACYLCHAIN_FRAGMENT ->
          checkForAcylChainFragment(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint, msMsScan);
      case ACYLCHAIN_FRAGMENT_NL ->
          checkForAcylChainFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint, msMsScan);
      case ACYLCHAIN_MINUS_FORMULA_FRAGMENT ->
          checkForAcylChainMinusFormulaFragment(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint,
              msMsScan);
      case ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL ->
          checkForAcylChainMinusFormulaFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint,
              msMsScan);
      case ACYLCHAIN_PLUS_FORMULA_FRAGMENT ->
          checkForAcylChainPlusFormulaFragment(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint,
              msMsScan);
      case ACYLCHAIN_PLUS_FORMULA_FRAGMENT_NL ->
          checkForAcylChainPlusFormulaFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint,
              msMsScan);
      case AMID_CHAIN_FRAGMENT ->
          checkForAmidChainFragment(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint, msMsScan);
      case AMID_CHAIN_PLUS_FORMULA_FRAGMENT ->
          checkForAmidChainPlusFormulaFragment(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint,
              msMsScan);
      case AMID_CHAIN_MINUS_FORMULA_FRAGMENT ->
          checkForAmidChainMinusFormulaFragment(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint,
              msMsScan);
      case AMID_MONO_HYDROXY_CHAIN_FRAGMENT ->
          checkForAmidMonoHydroxyChainFragment(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint,
              msMsScan);
      case AMID_MONO_HYDROXY_CHAIN_PLUS_FORMULA_FRAGMENT ->
          checkForAmidMonoHydroxyChainPlusFormulaFragment(rule, mzTolRangeMSMS, lipidAnnotation,
              dataPoint, msMsScan);
      case AMID_MONO_HYDROXY_CHAIN_MINUS_FORMULA_FRAGMENT ->
          checkForAmidMonoHydroxyChainMinusFormulaFragment(rule, mzTolRangeMSMS, lipidAnnotation,
              dataPoint, msMsScan);
      case AMID_CHAIN_FRAGMENT_NL ->
          checkForAmidChainFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint, msMsScan);
      case AMID_CHAIN_PLUS_FORMULA_FRAGMENT_NL ->
          checkForAmidChainPlusFormulaFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint,
              msMsScan);
      case AMID_CHAIN_MINUS_FORMULA_FRAGMENT_NL ->
          checkForAmidChainMinusFormulaFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint,
              msMsScan);
      default -> null;
    };
  }

  private LipidFragment checkForOnlyPrecursor(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    IMolecularFormula lipidFormula = null;
    try {
      lipidFormula = (IMolecularFormula) lipidAnnotation.getMolecularFormula().clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
    rule.getIonizationType().ionizeFormula(lipidFormula);
    Double mzFragmentExact = FormulaUtils.calculateMzRatio(lipidFormula);

    if (mzTolRangeMSMS.contains(mzFragmentExact)) {
      return new LipidFragment(rule.getLipidFragmentationRuleType(),
          rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
          mzFragmentExact, MolecularFormulaManipulator.getString(lipidFormula), dataPoint,
          lipidAnnotation.getLipidClass(), null, null, null, null, msMsScan);
    } else {
      return null;
    }
  }

  private LipidFragment checkForHeadgroupFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    String fragmentFormula = rule.getMolecularFormula();
    Double mzFragmentExact = FormulaUtils.calculateMzRatio(fragmentFormula);
    if (mzTolRangeMSMS.contains(mzFragmentExact)) {
      return new LipidFragment(rule.getLipidFragmentationRuleType(),
          rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
          mzFragmentExact, fragmentFormula, dataPoint, lipidAnnotation.getLipidClass(), null, null,
          null, null, msMsScan);
    } else {
      return null;
    }
  }

  private LipidFragment checkForHeadgroupFragmentNL(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    IMolecularFormula formulaNL = FormulaUtils.createMajorIsotopeMolFormula(
        rule.getMolecularFormula());
    IMolecularFormula lipidFormula = null;
    try {
      lipidFormula = (IMolecularFormula) lipidAnnotation.getMolecularFormula().clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
    rule.getIonizationType().ionizeFormula(lipidFormula);
    IMolecularFormula fragmentFormula = FormulaUtils.subtractFormula(lipidFormula, formulaNL);
    Double mzFragmentExact = FormulaUtils.calculateMzRatio(fragmentFormula);
    if (mzTolRangeMSMS.contains(mzFragmentExact)) {
      return new LipidFragment(rule.getLipidFragmentationRuleType(),
          rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
          mzFragmentExact, MolecularFormulaManipulator.getString(fragmentFormula), dataPoint,
          lipidAnnotation.getLipidClass(), null, null, null, null, msMsScan);
    } else {
      return null;
    }
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
              rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
              mzExact, MolecularFormulaManipulator.getString(lipidChainFormula), dataPoint,
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
            rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
            mzExact, MolecularFormulaManipulator.getString(fragmentFormula), dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            lipidChain.getNumberOfOxygens(), LipidChainType.ACYL_CHAIN, msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForAcylChainMinusFormulaFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    IMolecularFormula modificationFormula = FormulaUtils.createMajorIsotopeMolFormula(
        rule.getMolecularFormula());
    List<ILipidChain> fattyAcylChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.ACYL_CHAIN, minChainLength, maxChainLength, minDoubleBonds, maxDoubleBonds,
        onlySearchForEvenChains);
    for (ILipidChain lipidChain : fattyAcylChains) {
      IMolecularFormula lipidChainFormula = lipidChain.getChainMolecularFormula();
      IMolecularFormula fragmentFormula = FormulaUtils.subtractFormula(lipidChainFormula,
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
            rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
            mzExact, MolecularFormulaManipulator.getString(lipidMinusFragmentFormula), dataPoint,
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
            rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
            mzExact, MolecularFormulaManipulator.getString(ionizedFragmentFormula), dataPoint,
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
            rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
            mzExact, MolecularFormulaManipulator.getString(lipidMinusFragmentFormula), dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            lipidChain.getNumberOfOxygens(), LipidChainType.ACYL_CHAIN, msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForAmidChainFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {

    if (rule.getPolarityType().equals(PolarityType.NEGATIVE)) {
      List<ILipidChain> fattyAcylChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
          LipidChainType.AMID_CHAIN, minChainLength, maxChainLength, minDoubleBonds, maxDoubleBonds,
          onlySearchForEvenChains);
      for (ILipidChain lipidChain : fattyAcylChains) {
        IMolecularFormula lipidChainFormula = lipidChain.getChainMolecularFormula();
        IMolecularFormula ionizedFragmentFormula = ionizeFragmentBasedOnPolarity(lipidChainFormula,
            rule.getPolarityType());
        Double mzExact = FormulaUtils.calculateMzRatio(ionizedFragmentFormula);
        if (mzTolRangeMSMS.contains(mzExact)) {
          int chainLength = lipidChain.getNumberOfCarbons();
          int numberOfDoubleBonds = lipidChain.getNumberOfDBEs();
          return new LipidFragment(rule.getLipidFragmentationRuleType(),
              rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
              mzExact, MolecularFormulaManipulator.getString(ionizedFragmentFormula), dataPoint,
              lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
              lipidChain.getNumberOfOxygens(), LipidChainType.AMID_CHAIN, msMsScan);
        }
      }
    }
    return null;
  }

  private LipidFragment checkForAmidChainPlusFormulaFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    IMolecularFormula modificationFormula = FormulaUtils.createMajorIsotopeMolFormula(
        rule.getMolecularFormula());
    List<ILipidChain> amidChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.AMID_CHAIN, minChainLength, maxChainLength, minDoubleBonds, maxDoubleBonds,
        onlySearchForEvenChains);
    for (ILipidChain lipidChain : amidChains) {
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
            lipidChain.getNumberOfOxygens(), LipidChainType.AMID_CHAIN, msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForAmidChainMinusFormulaFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    IMolecularFormula modificationFormula = FormulaUtils.createMajorIsotopeMolFormula(
        rule.getMolecularFormula());
    List<ILipidChain> amidChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.AMID_CHAIN, minChainLength, maxChainLength, minDoubleBonds, maxDoubleBonds,
        onlySearchForEvenChains);
    for (ILipidChain lipidChain : amidChains) {
      IMolecularFormula lipidChainFormula = lipidChain.getChainMolecularFormula();
      IMolecularFormula fragmentFormula = FormulaUtils.subtractFormula(lipidChainFormula,
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
            lipidChain.getNumberOfOxygens(), LipidChainType.AMID_CHAIN, msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForAmidMonoHydroxyChainFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {

    if (rule.getPolarityType().equals(PolarityType.NEGATIVE)) {
      List<ILipidChain> fattyAcylChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
          LipidChainType.AMID_MONO_HYDROXY_CHAIN, minChainLength, maxChainLength, minDoubleBonds,
          maxDoubleBonds, onlySearchForEvenChains);
      for (ILipidChain lipidChain : fattyAcylChains) {
        IMolecularFormula lipidChainFormula = lipidChain.getChainMolecularFormula();
        ionizeFragmentBasedOnPolarity(lipidChainFormula, rule.getPolarityType());
        Double mzExact = FormulaUtils.calculateMzRatio(lipidChainFormula);

        if (mzTolRangeMSMS.contains(mzExact)) {
          int chainLength = lipidChain.getNumberOfCarbons();
          int numberOfDoubleBonds = lipidChain.getNumberOfDBEs();
          return new LipidFragment(rule.getLipidFragmentationRuleType(),
              rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
              mzExact, MolecularFormulaManipulator.getString(lipidChainFormula), dataPoint,
              lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
              lipidChain.getNumberOfOxygens(), LipidChainType.AMID_MONO_HYDROXY_CHAIN, msMsScan);
        }
      }
    }
    return null;
  }

  private LipidFragment checkForAmidMonoHydroxyChainPlusFormulaFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    IMolecularFormula modificationFormula = FormulaUtils.createMajorIsotopeMolFormula(
        rule.getMolecularFormula());
    List<ILipidChain> amidChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.AMID_MONO_HYDROXY_CHAIN, minChainLength, maxChainLength, minDoubleBonds,
        maxDoubleBonds, onlySearchForEvenChains);
    for (ILipidChain lipidChain : amidChains) {
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
            lipidChain.getNumberOfOxygens(), LipidChainType.AMID_MONO_HYDROXY_CHAIN, msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForAmidMonoHydroxyChainMinusFormulaFragment(
      LipidFragmentationRule rule, Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation,
      DataPoint dataPoint, Scan msMsScan) {
    IMolecularFormula modificationFormula = FormulaUtils.createMajorIsotopeMolFormula(
        rule.getMolecularFormula());
    List<ILipidChain> amidChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.AMID_MONO_HYDROXY_CHAIN, minChainLength, maxChainLength, minDoubleBonds,
        maxDoubleBonds, onlySearchForEvenChains);
    for (ILipidChain lipidChain : amidChains) {
      IMolecularFormula lipidChainFormula = lipidChain.getChainMolecularFormula();
      IMolecularFormula fragmentFormula = FormulaUtils.subtractFormula(lipidChainFormula,
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
            lipidChain.getNumberOfOxygens(), LipidChainType.AMID_MONO_HYDROXY_CHAIN, msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForAmidChainFragmentNL(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    List<ILipidChain> amidChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.AMID_CHAIN, minChainLength, maxChainLength, minDoubleBonds, maxDoubleBonds,
        onlySearchForEvenChains);
    for (ILipidChain lipidChain : amidChains) {
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
            rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
            mzExact, MolecularFormulaManipulator.getString(fragmentFormula), dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            lipidChain.getNumberOfOxygens(), LipidChainType.AMID_CHAIN, msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForAmidChainPlusFormulaFragmentNL(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    IMolecularFormula modificationFormula = FormulaUtils.createMajorIsotopeMolFormula(
        rule.getMolecularFormula());
    List<ILipidChain> amidChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.AMID_CHAIN, minChainLength, maxChainLength, minDoubleBonds, maxDoubleBonds,
        onlySearchForEvenChains);
    for (ILipidChain lipidChain : amidChains) {
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
            rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
            mzExact, MolecularFormulaManipulator.getString(lipidMinusFragmentFormula), dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            lipidChain.getNumberOfOxygens(), LipidChainType.AMID_CHAIN, msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForAmidChainMinusFormulaFragmentNL(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    IMolecularFormula modificationFormula = FormulaUtils.createMajorIsotopeMolFormula(
        rule.getMolecularFormula());
    List<ILipidChain> amidChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.AMID_CHAIN, minChainLength, maxChainLength, minDoubleBonds, maxDoubleBonds,
        onlySearchForEvenChains);
    for (ILipidChain lipidChain : amidChains) {
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
            rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
            mzExact, MolecularFormulaManipulator.getString(lipidMinusFragmentFormula), dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            lipidChain.getNumberOfOxygens(), LipidChainType.AMID_CHAIN, msMsScan);
      }
    }
    return null;
  }

  protected IMolecularFormula ionizeFragmentBasedOnPolarity(IMolecularFormula formula,
      PolarityType polarityType) {
    if (polarityType.equals(PolarityType.NEGATIVE)) {
      IonizationType.NEGATIVE.ionizeFormula(formula);
      return formula;
    } else if (polarityType.equals(PolarityType.POSITIVE)) {
      IonizationType.POSITIVE.ionizeFormula(formula);
      return formula;
    }
    return formula;
  }

}
