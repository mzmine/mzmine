/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.SpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.lipidchain.ILipidChain;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidChainFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidChainType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.MatchedLipid;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.FormulaUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * This class contains methods for MS/MS lipid identifications
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class MSMSLipidTools {

  private static final ChainTools CHAIN_TOOLS = new ChainTools();
  private static final LipidFactory LIPID_FACTORY = new LipidFactory();


  public LipidFragment checkForClassSpecificFragment(Range<Double> mzTolRangeMSMS,
      ILipidAnnotation lipidAnnotation, IonizationType ionizationType,
      LipidFragmentationRule[] rules, DataPoint dataPoint, Scan msMsScan) {
    for (int i = 0; i < rules.length; i++) {
      if (!ionizationType.equals(rules[i].getIonizationType())
          || rules[i].getLipidFragmentationRuleType() == null) {
        continue;
      }
      LipidFragment detectedFragment =
          checkForSpecificRuleTpye(rules[i], mzTolRangeMSMS, lipidAnnotation, dataPoint, msMsScan);
      if (detectedFragment != null) {
        return detectedFragment;
      }
    }
    return null;
  }

  private LipidFragment checkForSpecificRuleTpye(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    LipidFragmentationRuleType ruleType = rule.getLipidFragmentationRuleType();
    switch (ruleType) {
      case HEADGROUP_FRAGMENT:
        return checkForHeadgroupFragment(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint,
            msMsScan);
      case HEADGROUP_FRAGMENT_NL:
        return checkForHeadgroupFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint,
            msMsScan);
      case ACYLCHAIN_FRAGMENT:
        return checkForAcylChainFragment(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint,
            msMsScan);
      case ACYLCHAIN_FRAGMENT_NL:
        return checkForAcylChainFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint,
            msMsScan);
      case ACYLCHAIN_MINUS_FORMULA_FRAGMENT:
        return checkForAcylChainMinusFormulaFragment(rule, mzTolRangeMSMS, lipidAnnotation,
            dataPoint, msMsScan);
      case ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL:
        return checkForAcylChainMinusFormulaFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation,
            dataPoint, msMsScan);
      case ACYLCHAIN_PLUS_FORMULA_FRAGMENT:
        return checkForAcylChainPlusFormulaFragment(rule, mzTolRangeMSMS, lipidAnnotation,
            dataPoint, msMsScan);
      case ACYLCHAIN_PLUS_FORMULA_FRAGMENT_NL:
        return checkForAcylChainPlusFormulaFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation,
            dataPoint, msMsScan);
      case TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT:
        return checkForTwoAcylChainsPlusFormulaFragment(rule, mzTolRangeMSMS, lipidAnnotation,
            dataPoint, msMsScan);
      case ALKYLCHAIN_FRAGMENT:
        return checkForAlkylChainFragment(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint,
            msMsScan);
      case ALKYLCHAIN_FRAGMENT_NL:
        return checkForAlkylChainFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint,
            msMsScan);
      case ALKYLCHAIN_MINUS_FORMULA_FRAGMENT:
        return checkForAlkylChainMinusFormulaFragment(rule, mzTolRangeMSMS, lipidAnnotation,
            dataPoint, msMsScan);
      case ALKYLCHAIN_MINUS_FORMULA_FRAGMENT_NL:
        return checkForAlkylChainMinusFormulaFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation,
            dataPoint, msMsScan);
      case ALKYLCHAIN_PLUS_FORMULA_FRAGMENT:
        return checkForAlkylChainPlusFormulaFragment(rule, mzTolRangeMSMS, lipidAnnotation,
            dataPoint, msMsScan);
      case ALKYLCHAIN_PLUS_FORMULA_FRAGMENT_NL:
        return checkForAlkylChainPlusFormulaFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation,
            dataPoint, msMsScan);
      default:
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
          rule.getLipidFragmentInformationLevelType(), mzFragmentExact, dataPoint,
          lipidAnnotation.getLipidClass(), null, null, null, msMsScan);
    } else {
      return null;
    }
  }

  private LipidFragment checkForHeadgroupFragmentNL(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    String fragmentFormula = rule.getMolecularFormula();
    Double mzFragmentExact = FormulaUtils.calculateExactMass(fragmentFormula);
    Double mzPrecursorExact =
        MolecularFormulaManipulator.getMass(lipidAnnotation.getMolecularFormula(),
            AtomContainerManipulator.MonoIsotopic) + rule.getIonizationType().getAddedMass();
    Double mzExact = mzPrecursorExact - mzFragmentExact;
    if (mzTolRangeMSMS.contains(mzExact)) {
      return new LipidFragment(rule.getLipidFragmentationRuleType(),
          rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
          lipidAnnotation.getLipidClass(), null, null, LipidChainType.ACYL_CHAIN, msMsScan);
    } else {
      return null;
    }
  }

  // Acly Chains
  private LipidFragment checkForAcylChainFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {

    if (rule.getPolarityType().equals(PolarityType.NEGATIVE)) {
      List<String> fattyAcidFormulas = CHAIN_TOOLS.calculateFattyAcidFormulas();
      for (String fattyAcidFormula : fattyAcidFormulas) {
        Double mzExact = FormulaUtils.calculateExactMass(fattyAcidFormula)
            + IonizationType.NEGATIVE_HYDROGEN.getAddedMass();
        if (mzTolRangeMSMS.contains(mzExact)) {
          int chainLength = CHAIN_TOOLS.getChainLengthFromFormula(fattyAcidFormula);
          int numberOfDoubleBonds = CHAIN_TOOLS.getNumberOfDoubleBondsFromFormula(fattyAcidFormula);
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
    List<String> fattyAcidFormulas = CHAIN_TOOLS.calculateFattyAcidFormulas();
    Double mzPrecursorExact =
        MolecularFormulaManipulator.getMass(lipidAnnotation.getMolecularFormula(),
            AtomContainerManipulator.MonoIsotopic) + rule.getIonizationType().getAddedMass();
    for (String fattyAcidFormula : fattyAcidFormulas) {
      Double mzFattyAcid = FormulaUtils.calculateExactMass(fattyAcidFormula);
      Double mzExact = mzPrecursorExact - mzFattyAcid;
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = CHAIN_TOOLS.getChainLengthFromFormula(fattyAcidFormula);
        int numberOfDoubleBonds = CHAIN_TOOLS.getNumberOfDoubleBondsFromFormula(fattyAcidFormula);
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            LipidChainType.ACYL_CHAIN, msMsScan);
      }
    }
    return null;
  }


  private LipidFragment checkForAcylChainMinusFormulaFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {

    if (rule.getPolarityType().equals(PolarityType.NEGATIVE)) {
      String fragmentFormula = rule.getMolecularFormula();
      Double mzFragmentExact = FormulaUtils.calculateExactMass(fragmentFormula);
      List<String> fattyAcidFormulas = CHAIN_TOOLS.calculateFattyAcidFormulas();
      for (String fattyAcidFormula : fattyAcidFormulas) {
//        @Ansgar the result is not used and the method itself is buggy as hell - please write a test
//        FormulaUtils.ionizeFormula(fattyAcidFormula, IonizationType.NEGATIVE_HYDROGEN, 1);
        Double mzExact = FormulaUtils.calculateExactMass(fattyAcidFormula) - mzFragmentExact;
        if (mzTolRangeMSMS.contains(mzExact)) {
          int chainLength = CHAIN_TOOLS.getChainLengthFromFormula(fattyAcidFormula);
          int numberOfDoubleBonds = CHAIN_TOOLS.getNumberOfDoubleBondsFromFormula(fattyAcidFormula);
          return new LipidFragment(rule.getLipidFragmentationRuleType(),
              rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
              lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
              LipidChainType.ACYL_CHAIN, msMsScan);
        }
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
    List<String> fattyAcidFormulas = CHAIN_TOOLS.calculateFattyAcidFormulas();
    for (String fattyAcidFormula : fattyAcidFormulas) {
      Double mzExact =
          mzPrecursorExact - FormulaUtils.calculateExactMass(fattyAcidFormula) - mzFragmentExact;
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = CHAIN_TOOLS.getChainLengthFromFormula(fattyAcidFormula);
        int numberOfDoubleBonds = CHAIN_TOOLS.getNumberOfDoubleBondsFromFormula(fattyAcidFormula);
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
    List<String> fattyAcidFormulas = CHAIN_TOOLS.calculateFattyAcidFormulas();
    for (String fattyAcidFormula : fattyAcidFormulas) {
      Double mzExact = FormulaUtils.calculateExactMass(fattyAcidFormula) + mzFragmentExact;
      mzExact = ionizeFragmentBasedOnPolarity(mzExact, rule.getPolarityType());
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = CHAIN_TOOLS.getChainLengthFromFormula(fattyAcidFormula);
        int numberOfDoubleBonds = CHAIN_TOOLS.getNumberOfDoubleBondsFromFormula(fattyAcidFormula);
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
    List<String> fattyAcidFormulas = CHAIN_TOOLS.calculateFattyAcidFormulas();
    for (String fattyAcidFormula : fattyAcidFormulas) {
      Double mzExact =
          mzPrecursorExact - FormulaUtils.calculateExactMass(fattyAcidFormula) + mzFragmentExact;
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = CHAIN_TOOLS.getChainLengthFromFormula(fattyAcidFormula);
        int numberOfDoubleBonds = CHAIN_TOOLS.getNumberOfDoubleBondsFromFormula(fattyAcidFormula);
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
    List<String> fattyAcidFormulasOne = CHAIN_TOOLS.calculateFattyAcidFormulas();
    List<String> fattyAcidFormulasTwo = CHAIN_TOOLS.calculateFattyAcidFormulas();
    for (int i = 0; i < fattyAcidFormulasOne.size(); i++) {
      Double mzFattyAcidOne = FormulaUtils.calculateExactMass(fattyAcidFormulasOne.get(i));
      for (int j = 0; j < fattyAcidFormulasTwo.size(); j++) {
        Double mzFattyAcidTwo = FormulaUtils.calculateExactMass(fattyAcidFormulasTwo.get(j));
        Double mzExact = mzFattyAcidOne + mzFattyAcidTwo + mzFragmentExact;
        mzExact = ionizeFragmentBasedOnPolarity(mzExact, rule.getPolarityType());
        if (mzTolRangeMSMS.contains(mzFattyAcidOne + mzFattyAcidTwo + mzFragmentExact)) {
          return new LipidFragment(rule.getLipidFragmentationRuleType(),
              rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
              lipidAnnotation.getLipidClass(), null, null, null, msMsScan);
        }
      }
    }
    return null;
  }

  // Alkyl Chains
  private LipidFragment checkForAlkylChainFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {

    List<String> chainFormulas = CHAIN_TOOLS.calculateHydroCarbonFormulas();
    for (String chainFormula : chainFormulas) {
      Double mzExact = FormulaUtils.calculateExactMass(chainFormula);
      mzExact = ionizeFragmentBasedOnPolarity(mzExact, rule.getPolarityType());
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = CHAIN_TOOLS.getChainLengthFromFormula(chainFormula);
        int numberOfDoubleBonds = CHAIN_TOOLS.getNumberOfDoubleBondsFromFormula(chainFormula);
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            LipidChainType.ALKYL_CHAIN, msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForAlkylChainFragmentNL(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {

    List<String> chainFormulas = CHAIN_TOOLS.calculateHydroCarbonFormulas();
    Double mzPrecursorExact =
        MolecularFormulaManipulator.getMass(lipidAnnotation.getMolecularFormula(),
            AtomContainerManipulator.MonoIsotopic) + rule.getIonizationType().getAddedMass();
    for (String chainFormula : chainFormulas) {
      Double mzFattyAcid = FormulaUtils.calculateExactMass(chainFormula);
      Double mzExact = mzPrecursorExact - mzFattyAcid;
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = CHAIN_TOOLS.getChainLengthFromFormula(chainFormula);
        int numberOfDoubleBonds = CHAIN_TOOLS.getNumberOfDoubleBondsFromFormula(chainFormula);
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            LipidChainType.ALKYL_CHAIN, msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForAlkylChainMinusFormulaFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {

    if (rule.getPolarityType().equals(PolarityType.NEGATIVE)) {
      String fragmentFormula = rule.getMolecularFormula();
      Double mzFragmentExact = FormulaUtils.calculateExactMass(fragmentFormula);
      List<String> chainFormulas = CHAIN_TOOLS.calculateHydroCarbonFormulas();
      for (String chainFormula : chainFormulas) {
        Double mzExact = FormulaUtils.calculateExactMass(
            chainFormula + IonizationType.NEGATIVE_HYDROGEN.getAddedMass()) - mzFragmentExact;
        if (mzTolRangeMSMS.contains(mzExact)) {
          int chainLength = CHAIN_TOOLS.getChainLengthFromFormula(chainFormula);
          int numberOfDoubleBonds = CHAIN_TOOLS.getNumberOfDoubleBondsFromFormula(chainFormula);
          return new LipidFragment(rule.getLipidFragmentationRuleType(),
              rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
              lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
              LipidChainType.ALKYL_CHAIN, msMsScan);
        }
      }
    }
    return null;
  }

  private LipidFragment checkForAlkylChainMinusFormulaFragmentNL(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {

    String fragmentFormula = rule.getMolecularFormula();
    Double mzPrecursorExact =
        MolecularFormulaManipulator.getMass(lipidAnnotation.getMolecularFormula(),
            AtomContainerManipulator.MonoIsotopic) + rule.getIonizationType().getAddedMass();
    Double mzFragmentExact = FormulaUtils.calculateExactMass(fragmentFormula);
    List<String> chainFormulas = CHAIN_TOOLS.calculateHydroCarbonFormulas();
    for (String chainFormula : chainFormulas) {
      Double mzExact =
          mzPrecursorExact - FormulaUtils.calculateExactMass(chainFormula) - mzFragmentExact;
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = CHAIN_TOOLS.getChainLengthFromFormula(chainFormula);
        int numberOfDoubleBonds = CHAIN_TOOLS.getNumberOfDoubleBondsFromFormula(chainFormula);
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            LipidChainType.ACYL_CHAIN, msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForAlkylChainPlusFormulaFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {

    String fragmentFormula = rule.getMolecularFormula();
    Double mzFragmentExact = FormulaUtils.calculateExactMass(fragmentFormula);
    List<String> chainFormulas = CHAIN_TOOLS.calculateHydroCarbonFormulas();
    for (String chainFormula : chainFormulas) {
      Double mzExact = FormulaUtils.calculateExactMass(chainFormula) + mzFragmentExact;
      mzExact = ionizeFragmentBasedOnPolarity(mzExact, rule.getPolarityType());
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = CHAIN_TOOLS.getChainLengthFromFormula(chainFormula);
        int numberOfDoubleBonds = CHAIN_TOOLS.getNumberOfDoubleBondsFromFormula(chainFormula);
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            LipidChainType.ACYL_CHAIN, msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForAlkylChainPlusFormulaFragmentNL(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {

    String fragmentFormula = rule.getMolecularFormula();
    Double mzPrecursorExact =
        MolecularFormulaManipulator.getMass(lipidAnnotation.getMolecularFormula(),
            AtomContainerManipulator.MonoIsotopic) + rule.getIonizationType().getAddedMass();
    Double mzFragmentExact = FormulaUtils.calculateExactMass(fragmentFormula);
    List<String> chainFormulas = CHAIN_TOOLS.calculateHydroCarbonFormulas();
    for (String chainFormula : chainFormulas) {
      Double mzExact =
          mzPrecursorExact - FormulaUtils.calculateExactMass(chainFormula) + mzFragmentExact;
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = CHAIN_TOOLS.getChainLengthFromFormula(chainFormula);
        int numberOfDoubleBonds = CHAIN_TOOLS.getNumberOfDoubleBondsFromFormula(chainFormula);
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            LipidChainType.ACYL_CHAIN, msMsScan);
      }
    }
    return null;
  }

  private double ionizeFragmentBasedOnPolarity(Double mzExact, PolarityType polarityType) {
    if (polarityType.equals(PolarityType.NEGATIVE)) {
      return mzExact + IonizationType.NEGATIVE.getAddedMass();
    } else if (polarityType.equals(PolarityType.POSITIVE)) {
      return mzExact + IonizationType.POSITIVE.getAddedMass();
    }
    return mzExact;
  }

  public MatchedLipid confirmSpeciesLevelAnnotation(Double accurateMz,
      ILipidAnnotation lipidAnnotation, Set<LipidFragment> listOfAnnotatedFragments,
      DataPoint[] massList, double minMsMsScore, MZTolerance mzTolRangeMSMS,
      IonizationType ionizationType) {
    Set<LipidFragment> speciesLevelFragments = new HashSet<>();
    for (LipidFragment lipidFragment : listOfAnnotatedFragments) {
      if (lipidFragment.getLipidFragmentInformationLevelType()
          .equals(LipidAnnotationLevel.SPECIES_LEVEL)) {
        speciesLevelFragments.add(lipidFragment);
      }
    }
    if (!speciesLevelFragments.isEmpty()) {
      Double msMsScore =
          calculateMsMsScore(massList, speciesLevelFragments, accurateMz, mzTolRangeMSMS);
      if (msMsScore >= minMsMsScore) {
        return new MatchedLipid(lipidAnnotation, accurateMz, ionizationType,
            listOfAnnotatedFragments, msMsScore);
      }
    }
    return null;
  }

  /**
   * This methods tries to reconstruct a possible chain composition of the annotated lipid using the
   * annotated MS/MS fragments
   */
  public Set<MatchedLipid> predictMolecularSpeciesLevelAnnotation(
      Set<LipidFragment> detectedFragments, ILipidAnnotation lipidAnnotation, Double accurateMz,
      DataPoint[] massList, double minMsMsScore, MZTolerance mzTolRangeMSMS,
      IonizationType ionizationType) {

    Set<LipidFragment> detectedFragmentsWithChainInformation = detectedFragments.stream()
        .filter(fragment -> fragment.getLipidFragmentInformationLevelType()
            .equals(LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL))
        .collect(Collectors.toSet());
    List<ILipidChain> chains = getChainsFromFragments(detectedFragmentsWithChainInformation);
    Set<MatchedLipid> matchedMolecularSpeciesLevelAnnotations = new HashSet<>();

    // get number of total C atoms, double bonds and number of chains
    int totalNumberOfCAtoms = 0;
    int totalNumberOfDBEs = 0;
    if (lipidAnnotation instanceof SpeciesLevelAnnotation) {
      totalNumberOfCAtoms = ((SpeciesLevelAnnotation) lipidAnnotation).getNumberOfCarbons();
      totalNumberOfDBEs = ((SpeciesLevelAnnotation) lipidAnnotation).getNumberOfDBEs();
    }
    int chainsInLipid = lipidAnnotation.getLipidClass().getChainTypes().length;

    for (int i = 0; i < chains.size(); i++) {
      int carbonOne = chains.get(i).getNumberOfCarbons();
      int dbeOne = chains.get(i).getNumberOfDBEs();
      if (chainsInLipid == 1 && carbonOne == totalNumberOfCAtoms && dbeOne == totalNumberOfDBEs) {
        List<ILipidChain> predictedChains = new ArrayList<>();
        predictedChains.add(chains.get(i));
        if (checkChainTypesFitLipidClass(predictedChains, lipidAnnotation.getLipidClass())) {
          Set<LipidFragment> fittingFragments = extractFragmentsForFittingChains(predictedChains,
              detectedFragmentsWithChainInformation);
          matchedMolecularSpeciesLevelAnnotations
              .add(buildNewMolecularSpeciesLevelMatch(fittingFragments, lipidAnnotation, accurateMz,
                  massList, predictedChains, minMsMsScore, mzTolRangeMSMS, ionizationType));
        }
      }
      if (chainsInLipid >= 2) {
        for (int j = 0; j < chains.size(); j++) {
          int carbonTwo = chains.get(j).getNumberOfCarbons();
          int dbeTwo = chains.get(j).getNumberOfDBEs();
          if (chainsInLipid == 2 && carbonOne + carbonTwo == totalNumberOfCAtoms
              && dbeOne + dbeTwo == totalNumberOfDBEs) {
            List<ILipidChain> predictedChains = new ArrayList<>();
            predictedChains.add(chains.get(i));
            predictedChains.add(chains.get(j));
            if (checkChainTypesFitLipidClass(predictedChains, lipidAnnotation.getLipidClass())) {
              Set<LipidFragment> fittingFragments = extractFragmentsForFittingChains(
                  predictedChains, detectedFragmentsWithChainInformation);
              matchedMolecularSpeciesLevelAnnotations.add(
                  buildNewMolecularSpeciesLevelMatch(fittingFragments, lipidAnnotation, accurateMz,
                      massList, predictedChains, minMsMsScore, mzTolRangeMSMS, ionizationType));
            }
          }
          if (chainsInLipid >= 3) {
            for (int k = 0; k < chains.size(); k++) {
              int carbonThree = chains.get(k).getNumberOfCarbons();
              int dbeThree = chains.get(k).getNumberOfDBEs();
              if (chainsInLipid == 3 && carbonOne + carbonTwo + carbonThree == totalNumberOfCAtoms
                  && dbeOne + dbeTwo + dbeThree == totalNumberOfDBEs) {
                List<ILipidChain> predictedChains = new ArrayList<>();
                predictedChains.add(chains.get(i));
                predictedChains.add(chains.get(j));
                predictedChains.add(chains.get(k));
                if (checkChainTypesFitLipidClass(predictedChains,
                    lipidAnnotation.getLipidClass())) {
                  Set<LipidFragment> fittingFragments = extractFragmentsForFittingChains(
                      predictedChains, detectedFragmentsWithChainInformation);
                  matchedMolecularSpeciesLevelAnnotations.add(buildNewMolecularSpeciesLevelMatch(
                      fittingFragments, lipidAnnotation, accurateMz, massList, predictedChains,
                      minMsMsScore, mzTolRangeMSMS, ionizationType));
                }
              }
              if (chainsInLipid >= 4) {
                for (int l = 0; l < chains.size(); l++) {
                  int carbonFour = chains.get(l).getNumberOfCarbons();
                  int dbeFour = chains.get(l).getNumberOfDBEs();
                  if (chainsInLipid == 4
                      && carbonOne + carbonTwo + carbonThree + carbonFour == totalNumberOfCAtoms
                      && dbeOne + dbeTwo + dbeThree + dbeFour == totalNumberOfDBEs) {
                    List<ILipidChain> predictedChains = new ArrayList<>();
                    predictedChains.add(chains.get(i));
                    predictedChains.add(chains.get(j));
                    predictedChains.add(chains.get(k));
                    predictedChains.add(chains.get(l));
                    if (checkChainTypesFitLipidClass(predictedChains,
                        lipidAnnotation.getLipidClass())) {
                      Set<LipidFragment> fittingFragments = extractFragmentsForFittingChains(
                          predictedChains, detectedFragmentsWithChainInformation);
                      matchedMolecularSpeciesLevelAnnotations
                          .add(buildNewMolecularSpeciesLevelMatch(fittingFragments, lipidAnnotation,
                              accurateMz, massList, predictedChains, minMsMsScore, mzTolRangeMSMS,
                              ionizationType));
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    return matchedMolecularSpeciesLevelAnnotations;
  }


  private Set<LipidFragment> extractFragmentsForFittingChains(List<ILipidChain> predictedChains,
      Set<LipidFragment> detectedFragmentsWithChainInformation) {
    Set<LipidFragment> fittingFragments = new HashSet<>();
    for (LipidFragment lipidFragment : detectedFragmentsWithChainInformation) {
      for (ILipidChain chain : predictedChains) {
        if (lipidFragment != null && chain != null
            && lipidFragment.getChainLength() == chain.getNumberOfCarbons()
            && lipidFragment.getNumberOfDBEs() == chain.getNumberOfDBEs()) {
          fittingFragments.add(lipidFragment);
        }
      }
    }
    return fittingFragments;
  }

  private MatchedLipid buildNewMolecularSpeciesLevelMatch(Set<LipidFragment> detectedFragments,
      ILipidAnnotation lipidAnnotation, Double accurateMz, DataPoint[] massList,
      List<ILipidChain> predictedChains, double minMsMsScore, MZTolerance mzTolRangeMSMS,
      IonizationType ionizationType) {
    ILipidAnnotation molecularSpeciesLevelAnnotation =
        LIPID_FACTORY.buildMolecularSpeciesLevelLipidFromChains(lipidAnnotation.getLipidClass(),
            predictedChains);
    Double msMsScore =
        calculateMsMsScore(massList, detectedFragments, minMsMsScore, mzTolRangeMSMS);
    if (msMsScore >= minMsMsScore) {
      return new MatchedLipid(molecularSpeciesLevelAnnotation, accurateMz, ionizationType,
          detectedFragments, msMsScore);
    }
    return null;
  }


  private boolean checkChainTypesFitLipidClass(List<ILipidChain> chains, ILipidClass lipidClass) {
    List<LipidChainType> lipidClassChainTypes = Arrays.asList(lipidClass.getChainTypes());
    List<LipidChainType> chainTypes =
        chains.stream().map(ILipidChain::getLipidChainType).collect(Collectors.toList());
    Collections.sort(lipidClassChainTypes);
    Collections.sort(chainTypes);
    return lipidClassChainTypes.equals(chainTypes);
  }

  private List<ILipidChain> getChainsFromFragments(Set<LipidFragment> detectedFragments) {
    LipidChainFactory chainFactory = new LipidChainFactory();
    List<ILipidChain> chains = new ArrayList<>();
    for (LipidFragment lipidFragment : detectedFragments) {
      if (lipidFragment.getLipidChainType() != null && lipidFragment.getChainLength() != null
          && lipidFragment.getNumberOfDBEs() != null) {
        ILipidChain lipidChain = chainFactory.buildLipidChain(lipidFragment.getLipidChainType(),
            lipidFragment.getChainLength(), lipidFragment.getNumberOfDBEs());
        if(lipidChain != null) {
          chains.add(lipidChain);
        }
      }
    }
    return chains.stream().distinct().collect(Collectors.toList());
  }

  /**
   * Calculate the explained intensity of MS/MS signals by lipid fragmentation rules in %
   */
  private Double calculateMsMsScore(DataPoint[] massList, Set<LipidFragment> annotatedFragments,
      Double precursor, MZTolerance mzTolRangeMSMS) {
    Double intensityAllSignals = Arrays.stream(massList)
        .filter(dp -> !mzTolRangeMSMS.checkWithinTolerance(dp.getMZ(), precursor))
        .mapToDouble(DataPoint::getIntensity).sum();
    Double intensityMatchedSignals = annotatedFragments.stream().map(LipidFragment::getDataPoint)
        .mapToDouble(DataPoint::getIntensity).sum();
    return (intensityMatchedSignals / intensityAllSignals) * 100;
  }
}
