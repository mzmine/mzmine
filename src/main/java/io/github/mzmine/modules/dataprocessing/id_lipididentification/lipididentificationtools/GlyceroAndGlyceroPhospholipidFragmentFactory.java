package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.util.FormulaUtils;
import java.util.List;
import java.util.Objects;

public class GlyceroAndGlyceroPhospholipidFragmentFactory extends AbstractLipidFragmentFactory {

  private static final ChainTools CHAIN_TOOLS = new ChainTools();

  public GlyceroAndGlyceroPhospholipidFragmentFactory(Range<Double> mzTolRangeMSMS,
      ILipidAnnotation lipidAnnotation, IonizationType ionizationType,
      LipidFragmentationRule[] rules, DataPoint dataPoint, Scan msMsScan) {
    super(mzTolRangeMSMS, lipidAnnotation, ionizationType, rules, dataPoint, msMsScan);
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
    if (Objects.requireNonNull(ruleType) == LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT) {
      return checkForAcylChainFragment(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint, msMsScan);
//      case ACYLCHAIN_FRAGMENT_NL:
//        return checkForAcylChainFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint,
//            msMsScan);
//      case ACYLCHAIN_MINUS_FORMULA_FRAGMENT:
//        return checkForAcylChainMinusFormulaFragment(rule, mzTolRangeMSMS, lipidAnnotation,
//            dataPoint, msMsScan);
//      case ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL:
//        return checkForAcylChainMinusFormulaFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation,
//            dataPoint, msMsScan);
//      case ACYLCHAIN_PLUS_FORMULA_FRAGMENT:
//        return checkForAcylChainPlusFormulaFragment(rule, mzTolRangeMSMS, lipidAnnotation,
//            dataPoint, msMsScan);
//      case ACYLCHAIN_PLUS_FORMULA_FRAGMENT_NL:
//        return checkForAcylChainPlusFormulaFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation,
//            dataPoint, msMsScan);
//      case TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT:
//        return checkForTwoAcylChainsPlusFormulaFragment(rule, mzTolRangeMSMS, lipidAnnotation,
//            dataPoint, msMsScan);
//      case ALKYLCHAIN_FRAGMENT:
//        return checkForAlkylChainFragment(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint,
//            msMsScan);
//      case ALKYLCHAIN_FRAGMENT_NL:
//        return checkForAlkylChainFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint,
//            msMsScan);
//      case ALKYLCHAIN_MINUS_FORMULA_FRAGMENT:
//        return checkForAlkylChainMinusFormulaFragment(rule, mzTolRangeMSMS, lipidAnnotation,
//            dataPoint, msMsScan);
//      case ALKYLCHAIN_MINUS_FORMULA_FRAGMENT_NL:
//        return checkForAlkylChainMinusFormulaFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation,
//            dataPoint, msMsScan);
//      case ALKYLCHAIN_PLUS_FORMULA_FRAGMENT:
//        return checkForAlkylChainPlusFormulaFragment(rule, mzTolRangeMSMS, lipidAnnotation,
//            dataPoint, msMsScan);
//      case ALKYLCHAIN_PLUS_FORMULA_FRAGMENT_NL:
//        return checkForAlkylChainPlusFormulaFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation,
//            dataPoint, msMsScan);
    }
    return null;
  }

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
}
