/*
 * Copyright (c) 2004-2024 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.fragmentation;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationChainParameters;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.ILipidChain;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainFactory;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class LipidFragmentFactory implements ILipidFragmentFactory {

  protected static final LipidChainFactory LIPID_CHAIN_FACTORY = new LipidChainFactory();
  protected final int minChainLength;
  protected final int maxChainLength;
  protected final int maxDoubleBonds;
  protected final int minDoubleBonds;
  protected final Boolean onlySearchForEvenChains;
  protected MZTolerance mzToleranceMS2;
  protected ILipidAnnotation lipidAnnotation;
  protected IonizationType ionizationType;
  protected LipidFragmentationRule[] rules;
  protected Scan msMsScan;

  public LipidFragmentFactory(MZTolerance mzToleranceMS2, ILipidAnnotation lipidAnnotation,
      IonizationType ionizationType, LipidFragmentationRule[] rules, Scan msMsScan,
      LipidAnnotationChainParameters chainParameters) {
    this.mzToleranceMS2 = mzToleranceMS2;
    this.lipidAnnotation = lipidAnnotation;
    this.ionizationType = ionizationType;
    this.rules = rules;
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


  @Override
  public List<LipidFragment> findLipidFragments() {
    List<LipidFragment> lipidFragments = new ArrayList<>();
    for (LipidFragmentationRule rule : rules) {
      if (!ionizationType.equals(rule.getIonizationType())
          || rule.getLipidFragmentationRuleType() == null) {
        continue;
      }
      List<LipidFragment> detectedFragments = checkForRuleTypes(rule);
      if (detectedFragments != null) {
        lipidFragments.addAll(detectedFragments);
      }
    }
    return lipidFragments;
  }

  private List<LipidFragment> checkForRuleTypes(LipidFragmentationRule rule) {
    LipidFragmentationRuleType ruleType = rule.getLipidFragmentationRuleType();
    return switch (ruleType) {
      case HEADGROUP_FRAGMENT -> checkForHeadgroupFragment(rule, lipidAnnotation, msMsScan);
      case HEADGROUP_FRAGMENT_NL -> checkForHeadgroupFragmentNL(rule, lipidAnnotation, msMsScan);
      case PRECURSOR -> checkForOnlyPrecursor(rule, lipidAnnotation, msMsScan);
      case ACYLCHAIN_FRAGMENT -> checkForAcylChainFragment(rule, lipidAnnotation, msMsScan);
      case ACYLCHAIN_FRAGMENT_NL ->
          findChainFragmentNL(rule, lipidAnnotation, msMsScan, LipidChainType.ACYL_CHAIN);
      case ACYLCHAIN_MINUS_FORMULA_FRAGMENT ->
          findChainMinusFormulaFragment(rule, lipidAnnotation, msMsScan, LipidChainType.ACYL_CHAIN);
      case ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL ->
          findChainMinusFormulaFragmentNL(rule, lipidAnnotation, msMsScan,
              LipidChainType.ACYL_CHAIN);
      case ACYLCHAIN_PLUS_FORMULA_FRAGMENT ->
          findChainPlusFormulaFragment(rule, lipidAnnotation, msMsScan, LipidChainType.ACYL_CHAIN);
      case ACYLCHAIN_PLUS_FORMULA_FRAGMENT_NL ->
          findChainPlusFormulaFragmentNL(rule, lipidAnnotation, msMsScan,
              LipidChainType.ACYL_CHAIN);
      case TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT ->
          checkForTwoAcylChainsPlusFormulaFragment(rule, lipidAnnotation, msMsScan);
      case ALKYLCHAIN_PLUS_FORMULA_FRAGMENT ->
          findChainPlusFormulaFragment(rule, lipidAnnotation, msMsScan, LipidChainType.ALKYL_CHAIN);
      case AMID_CHAIN_FRAGMENT ->
          findChainFragment(rule, lipidAnnotation, msMsScan, LipidChainType.AMID_CHAIN);
      case AMID_CHAIN_PLUS_FORMULA_FRAGMENT ->
          findChainPlusFormulaFragment(rule, lipidAnnotation, msMsScan, LipidChainType.AMID_CHAIN);
      case AMID_CHAIN_MINUS_FORMULA_FRAGMENT ->
          findChainMinusFormulaFragment(rule, lipidAnnotation, msMsScan, LipidChainType.AMID_CHAIN);
      case AMID_MONO_HYDROXY_CHAIN_FRAGMENT -> findChainFragment(rule, lipidAnnotation, msMsScan,
          LipidChainType.AMID_MONO_HYDROXY_CHAIN);
      case AMID_MONO_HYDROXY_CHAIN_PLUS_FORMULA_FRAGMENT ->
          findChainPlusFormulaFragment(rule, lipidAnnotation, msMsScan,
              LipidChainType.AMID_MONO_HYDROXY_CHAIN);
      case AMID_MONO_HYDROXY_CHAIN_MINUS_FORMULA_FRAGMENT ->
          findChainMinusFormulaFragment(rule, lipidAnnotation, msMsScan,
              LipidChainType.AMID_MONO_HYDROXY_CHAIN);
      case AMID_CHAIN_FRAGMENT_NL ->
          findChainFragmentNL(rule, lipidAnnotation, msMsScan, LipidChainType.AMID_CHAIN);
      case AMID_CHAIN_PLUS_FORMULA_FRAGMENT_NL ->
          findChainPlusFormulaFragmentNL(rule, lipidAnnotation, msMsScan,
              LipidChainType.AMID_CHAIN);
      case AMID_CHAIN_MINUS_FORMULA_FRAGMENT_NL ->
          findChainMinusFormulaFragmentNL(rule, lipidAnnotation, msMsScan,
              LipidChainType.AMID_CHAIN);
      case SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN_FRAGMENT ->
          findChainFragment(rule, lipidAnnotation, msMsScan,
              LipidChainType.SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN);
      case SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_FRAGMENT ->
          findChainFragment(rule, lipidAnnotation, msMsScan,
              LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN);
      case SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_FRAGMENT ->
          findChainFragment(rule, lipidAnnotation, msMsScan,
              LipidChainType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN);
      case SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT ->
          findChainMinusFormulaFragment(rule, lipidAnnotation, msMsScan,
              LipidChainType.SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN);
      case SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT ->
          findChainMinusFormulaFragment(rule, lipidAnnotation, msMsScan,
              LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN);
      case SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT ->
          findChainMinusFormulaFragment(rule, lipidAnnotation, msMsScan,
              LipidChainType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN);
    };
  }

  private List<LipidFragment> checkForOnlyPrecursor(LipidFragmentationRule rule,
      ILipidAnnotation lipidAnnotation, Scan msMsScan) {
    IMolecularFormula lipidFormula;
    try {
      lipidFormula = (IMolecularFormula) lipidAnnotation.getMolecularFormula().clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
    rule.getIonizationType().ionizeFormula(lipidFormula);
    return findLipidFragmentFromIonFormula(rule, lipidAnnotation, msMsScan, lipidFormula);
  }


  private List<LipidFragment> checkForHeadgroupFragment(LipidFragmentationRule rule,
      ILipidAnnotation lipidAnnotation, Scan msMsScan) {
    String fragmentFormula = rule.getMolecularFormula();
    return findLipidFragmentFromIonFormula(rule, lipidAnnotation, msMsScan, fragmentFormula);
  }

  private List<LipidFragment> checkForHeadgroupFragmentNL(LipidFragmentationRule rule,
      ILipidAnnotation lipidAnnotation, Scan msMsScan) {
    IMolecularFormula formulaNL = FormulaUtils.createMajorIsotopeMolFormulaWithCharge(
        rule.getMolecularFormula());
    IMolecularFormula lipidFormula;
    try {
      lipidFormula = (IMolecularFormula) lipidAnnotation.getMolecularFormula().clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
    rule.getIonizationType().ionizeFormula(lipidFormula);
    IMolecularFormula fragmentFormula = FormulaUtils.subtractFormula(lipidFormula, formulaNL);
    return findLipidFragmentFromIonFormula(rule, lipidAnnotation, msMsScan, fragmentFormula);
  }

  private List<LipidFragment> checkForAcylChainFragment(LipidFragmentationRule rule,
      ILipidAnnotation lipidAnnotation, Scan msMsScan) {
    if (rule.getPolarityType().equals(PolarityType.NEGATIVE)) {
      List<ILipidChain> fattyAcylChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
          LipidChainType.ACYL_CHAIN, minChainLength, maxChainLength, minDoubleBonds, maxDoubleBonds,
          onlySearchForEvenChains);
      List<LipidFragment> matchedFragments = new ArrayList<>();
      for (ILipidChain lipidChain : fattyAcylChains) {
        IMolecularFormula lipidChainFormula = lipidChain.getChainMolecularFormula();
        IonizationType.NEGATIVE_HYDROGEN.ionizeFormula(lipidChainFormula);
        addMatchedChainFragment(rule, lipidAnnotation, msMsScan, matchedFragments, lipidChain,
            lipidChainFormula);
      }
      return matchedFragments;
    }
    return null;
  }

  @NotNull
  private List<LipidFragment> findLipidFragmentFromIonFormula(LipidFragmentationRule rule,
      ILipidAnnotation lipidAnnotation, Scan msMsScan, IMolecularFormula ionFormula) {
    String ionFormulaString = MolecularFormulaManipulator.getString(ionFormula);
    Double mzExact = FormulaUtils.calculateMzRatio(ionFormulaString);
    BestDataPoint bestDataPoint = getBestDataPoint(mzExact);
    if (bestDataPoint.fragmentMatched()) {
      return List.of(new LipidFragment(rule.getLipidFragmentationRuleType(),
          rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
          mzExact, ionFormulaString,
          new SimpleDataPoint(bestDataPoint.mzValue(), bestDataPoint.intensity()),
          lipidAnnotation.getLipidClass(), null, null, null, null, msMsScan));
    } else {
      return List.of();
    }
  }

  @NotNull
  private List<LipidFragment> findLipidFragmentFromIonFormula(LipidFragmentationRule rule,
      ILipidAnnotation lipidAnnotation, Scan msMsScan, String ionFormula) {
    Double mzExact = FormulaUtils.calculateMzRatio(ionFormula);
    BestDataPoint bestDataPoint = getBestDataPoint(mzExact);
    if (bestDataPoint.fragmentMatched()) {
      return List.of(new LipidFragment(rule.getLipidFragmentationRuleType(),
          rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
          mzExact, ionFormula,
          new SimpleDataPoint(bestDataPoint.mzValue(), bestDataPoint.intensity()),
          lipidAnnotation.getLipidClass(), null, null, null, null, msMsScan));
    } else {
      return List.of();
    }
  }

  @NotNull
  protected List<LipidFragment> findChainMinusFormulaFragment(LipidFragmentationRule rule,
      ILipidAnnotation lipidAnnotation, Scan msMsScan, LipidChainType chainType) {
    IMolecularFormula modificationFormula = FormulaUtils.createMajorIsotopeMolFormulaWithCharge(
        rule.getMolecularFormula());
    List<ILipidChain> chains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(chainType,
        minChainLength, maxChainLength, minDoubleBonds, maxDoubleBonds, onlySearchForEvenChains);
    List<LipidFragment> matchedFragments = new ArrayList<>();
    for (ILipidChain lipidChain : chains) {
      IMolecularFormula lipidChainFormula = lipidChain.getChainMolecularFormula();
      IMolecularFormula fragmentFormula = FormulaUtils.subtractFormula(lipidChainFormula,
          modificationFormula);
      IMolecularFormula ionizedFragmentFormula = ionizeFragmentBasedOnPolarity(fragmentFormula,
          rule.getPolarityType());
      addMatchedChainFragment(rule, lipidAnnotation, msMsScan, matchedFragments, lipidChain,
          ionizedFragmentFormula);
    }
    return matchedFragments;
  }

  private void addMatchedChainFragment(LipidFragmentationRule rule,
      ILipidAnnotation lipidAnnotation, Scan msMsScan, List<LipidFragment> matchedFragments,
      ILipidChain lipidChain, IMolecularFormula ionizedFragmentFormula) {
    Double mzExact = FormulaUtils.calculateMzRatio(ionizedFragmentFormula);
    BestDataPoint bestDataPoint = getBestDataPoint(mzExact);
    if (bestDataPoint.fragmentMatched()) {
      int chainLength = lipidChain.getNumberOfCarbons();
      int numberOfDoubleBonds = lipidChain.getNumberOfDBEs();
      matchedFragments.add(new LipidFragment(rule.getLipidFragmentationRuleType(),
          rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
          mzExact, MolecularFormulaManipulator.getString(ionizedFragmentFormula),
          new SimpleDataPoint(bestDataPoint.mzValue(), bestDataPoint.intensity()),
          lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
          lipidChain.getNumberOfOxygens(), lipidChain.getLipidChainType(), msMsScan));
    }
  }

  @NotNull
  protected List<LipidFragment> findChainMinusFormulaFragmentNL(LipidFragmentationRule rule,
      ILipidAnnotation lipidAnnotation, Scan msMsScan, LipidChainType chainType) {
    IMolecularFormula modificationFormula = FormulaUtils.createMajorIsotopeMolFormulaWithCharge(
        rule.getMolecularFormula());
    List<ILipidChain> chains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(chainType,
        minChainLength, maxChainLength, minDoubleBonds, maxDoubleBonds, onlySearchForEvenChains);
    List<LipidFragment> matchedFragments = new ArrayList<>();
    for (ILipidChain lipidChain : chains) {
      IMolecularFormula lipidFormula;
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
      addMatchedChainFragment(rule, lipidAnnotation, msMsScan, matchedFragments, lipidChain,
          lipidMinusFragmentFormula);
    }
    return matchedFragments;
  }

  @NotNull
  protected List<LipidFragment> findChainPlusFormulaFragment(LipidFragmentationRule rule,
      ILipidAnnotation lipidAnnotation, Scan msMsScan, LipidChainType lipidChainType) {
    IMolecularFormula modificationFormula = FormulaUtils.createMajorIsotopeMolFormulaWithCharge(
        rule.getMolecularFormula());
    List<ILipidChain> chains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(lipidChainType,
        minChainLength, maxChainLength, minDoubleBonds, maxDoubleBonds, onlySearchForEvenChains);
    List<LipidFragment> matchedFragments = new ArrayList<>();
    for (ILipidChain lipidChain : chains) {
      IMolecularFormula lipidChainFormula = lipidChain.getChainMolecularFormula();
      IMolecularFormula fragmentFormula = FormulaUtils.addFormula(lipidChainFormula,
          modificationFormula);
      IMolecularFormula ionizedFragmentFormula = ionizeFragmentBasedOnPolarity(fragmentFormula,
          rule.getPolarityType());
      addMatchedChainFragment(rule, lipidAnnotation, msMsScan, matchedFragments, lipidChain,
          ionizedFragmentFormula);
    }
    return matchedFragments;
  }

  private List<LipidFragment> checkForTwoAcylChainsPlusFormulaFragment(LipidFragmentationRule rule,
      ILipidAnnotation lipidAnnotation, Scan msMsScan) {
    IMolecularFormula modificationFormula = FormulaUtils.createMajorIsotopeMolFormulaWithCharge(
        rule.getMolecularFormula());
    List<ILipidChain> combinedFattyAcylChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.ACYL_CHAIN, minChainLength * 2, maxChainLength * 2, minDoubleBonds * 2,
        maxDoubleBonds * 2, onlySearchForEvenChains);
    List<LipidFragment> matchedFragments = new ArrayList<>();
    for (ILipidChain combinedFattyAcylChain : combinedFattyAcylChains) {
      IMolecularFormula combinedChainsFormula = combinedFattyAcylChain.getChainMolecularFormula();
      IMolecularFormula fragmentFormula = FormulaUtils.addFormula(combinedChainsFormula,
          modificationFormula);
      IMolecularFormula ionizedFragmentFormula = ionizeFragmentBasedOnPolarity(fragmentFormula,
          rule.getPolarityType());
      Double mzExact = FormulaUtils.calculateMzRatio(ionizedFragmentFormula);
      BestDataPoint bestDataPoint = getBestDataPoint(mzExact);
      if (bestDataPoint.fragmentMatched()) {
        matchedFragments.add(new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
            mzExact, MolecularFormulaManipulator.getString(ionizedFragmentFormula),
            new SimpleDataPoint(bestDataPoint.mzValue(), bestDataPoint.intensity()),
            lipidAnnotation.getLipidClass(), combinedFattyAcylChain.getNumberOfCarbons(),
            combinedFattyAcylChain.getNumberOfDBEs(), combinedFattyAcylChain.getNumberOfOxygens(),
            LipidChainType.TWO_ACYL_CHAINS_COMBINED, msMsScan));
      }
    }
    return matchedFragments;
  }

  @NotNull
  protected List<LipidFragment> findChainFragment(LipidFragmentationRule rule,
      ILipidAnnotation lipidAnnotation, Scan msMsScan, LipidChainType lipidChainType) {
    List<ILipidChain> chains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(lipidChainType,
        minChainLength, maxChainLength, minDoubleBonds, maxDoubleBonds, onlySearchForEvenChains);
    List<LipidFragment> matchedFragments = new ArrayList<>();
    for (ILipidChain lipidChain : chains) {
      IMolecularFormula lipidChainFormula = lipidChain.getChainMolecularFormula();
      IMolecularFormula ionizedFragmentFormula = ionizeFragmentBasedOnPolarity(lipidChainFormula,
          rule.getPolarityType());
      addMatchedChainFragment(rule, lipidAnnotation, msMsScan, matchedFragments, lipidChain,
          ionizedFragmentFormula);
    }
    return matchedFragments;
  }

  @NotNull
  protected List<LipidFragment> findChainFragmentNL(LipidFragmentationRule rule,
      ILipidAnnotation lipidAnnotation, Scan msMsScan, LipidChainType lipidChainType) {
    List<ILipidChain> chains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(lipidChainType,
        minChainLength, maxChainLength, minDoubleBonds, maxDoubleBonds, onlySearchForEvenChains);
    List<LipidFragment> matchedFragments = new ArrayList<>();
    for (ILipidChain lipidChain : chains) {
      IMolecularFormula lipidFormula;
      try {
        lipidFormula = (IMolecularFormula) lipidAnnotation.getMolecularFormula().clone();
      } catch (CloneNotSupportedException e) {
        throw new RuntimeException(e);
      }
      rule.getIonizationType().ionizeFormula(lipidFormula);
      IMolecularFormula lipidChainFormula = lipidChain.getChainMolecularFormula();
      IMolecularFormula fragmentFormula = FormulaUtils.subtractFormula(lipidFormula,
          lipidChainFormula);
      addMatchedChainFragment(rule, lipidAnnotation, msMsScan, matchedFragments, lipidChain,
          fragmentFormula);
    }
    return matchedFragments;
  }

  @NotNull
  protected List<LipidFragment> findChainPlusFormulaFragmentNL(LipidFragmentationRule rule,
      ILipidAnnotation lipidAnnotation, Scan msMsScan, LipidChainType lipidChainType) {
    IMolecularFormula modificationFormula = FormulaUtils.createMajorIsotopeMolFormulaWithCharge(
        rule.getMolecularFormula());
    List<ILipidChain> chains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(lipidChainType,
        minChainLength, maxChainLength, minDoubleBonds, maxDoubleBonds, onlySearchForEvenChains);
    List<LipidFragment> matchedFragments = new ArrayList<>();
    for (ILipidChain lipidChain : chains) {
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
      addMatchedChainFragment(rule, lipidAnnotation, msMsScan, matchedFragments, lipidChain,
          lipidMinusFragmentFormula);
    }
    return matchedFragments;
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

  @NotNull
  protected BestDataPoint getBestDataPoint(Double mzExact) {
    boolean fragmentMatched = false;
    MassList massList = msMsScan.getMassList();
    if (massList != null) {
      Range<Double> toleranceRange = mzToleranceMS2.getToleranceRange(mzExact);
      int index = massList.binarySearch(toleranceRange.lowerEndpoint(), DefaultTo.GREATER_EQUALS);
      if (index < 0) {
        return new BestDataPoint(fragmentMatched, 0.0, 0.0);
      }
      int numberOfDataPoints = massList.getNumberOfDataPoints();
      double maxIntensity = 0.0;
      double bestMzValue = 0.0;
      for (int i = index; i < numberOfDataPoints; i++) {
        double intensity = massList.getIntensityValue(i);
        double mzValue = massList.getMzValue(i);
        Range<Double> mzTolRangeMSMS = mzToleranceMS2.getToleranceRange(mzValue);
        if (mzTolRangeMSMS.contains(mzExact) && intensity > maxIntensity) {
          maxIntensity = intensity;
          bestMzValue = mzValue;
          fragmentMatched = true;
        }
        if (mzTolRangeMSMS.upperEndpoint() < mzValue) {
          break;
        }
      }
      return new BestDataPoint(fragmentMatched, bestMzValue, maxIntensity);
    }
    return new BestDataPoint(fragmentMatched, 0.0, 0.0);
  }

  protected record BestDataPoint(boolean fragmentMatched, double mzValue, double intensity) {

  }

}
