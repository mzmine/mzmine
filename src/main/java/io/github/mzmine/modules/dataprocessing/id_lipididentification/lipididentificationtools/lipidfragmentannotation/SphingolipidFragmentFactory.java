package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.lipidfragmentannotation;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.LipidFragmentationRuleType;

public class SphingolipidFragmentFactory extends AbstractLipidFragmentFactory implements
    ILipidFragmentFactory {

  private static final ChainTools CHAIN_TOOLS = new ChainTools();

  public SphingolipidFragmentFactory(Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation,
      IonizationType ionizationType, LipidFragmentationRule[] rules, DataPoint dataPoint,
      Scan msMsScan) {
    super(mzTolRangeMSMS, lipidAnnotation, ionizationType, rules, dataPoint, msMsScan);
  }

  @Override
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
      LipidFragment detectedFragment = checkForSphingolipidSpecificRuleTypes(rule);
      if (detectedFragment != null) {
        lipidFragment = detectedFragment;
        break;
      }
    }
    return lipidFragment;
  }

  private LipidFragment checkForSphingolipidSpecificRuleTypes(LipidFragmentationRule rule) {
    LipidFragmentationRuleType ruleType = rule.getLipidFragmentationRuleType();
    switch (ruleType) {
      case SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN_FRAGMENT -> {
      }
      case SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_FRAGMENT -> {
        return checkForSphingolipidChainFragment(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint,
            msMsScan);
      }
      case SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_FRAGMENT -> {
      }
      case SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN_AND_SUBSTRUCTURE_NEUTRAL_LOSS_FRAGMENT -> {
      }
      case SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_AND_SUBSTRUCTURE_NEUTRAL_LOSS_FRAGMENT -> {
      }
      case SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_AND_SUBSTRUCTURE_NEUTRAL_LOSS_FRAGMENT -> {
      }
      case AMID_CHAIN_FRAGMENT -> {
      }
      case AMID_CHAIN_PLUS_SUBSTRUCTURE_FRAGMENT -> {
      }
      case AMID_CHAIN_AND_SUBSTRUCTURE_NEUTRAL_LOSS_FRAGMENT -> {
      }
      case AMID_MONO_HYDROXY_CHAIN_PLUS_SUBSTRUCTURE_FRAGMENT -> {
      }
      case AMID_MONO_HYDROXY_CHAIN_PLUS_SUBSTRUCTURE_MINUS_SUBSTRUCTURE_FRAGMENT -> {
      }
      default -> {
        return null;
      }
    }

    return null;
  }

  private LipidFragment checkForSphingolipidChainFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {

    //  List<String> fattyAcidFormulas = CHAIN_TOOLS.c

//      List<String> fattyAcidFormulas = CHAIN_TOOLS.calculateFattyAcidFormulas();
//      for (String fattyAcidFormula : fattyAcidFormulas) {
//        Double mzExact = FormulaUtils.calculateExactMass(fattyAcidFormula)
//            + IonizationType.NEGATIVE_HYDROGEN.getAddedMass();
//        if (mzTolRangeMSMS.contains(mzExact)) {
//          int chainLength = CHAIN_TOOLS.getChainLengthFromFormula(fattyAcidFormula);
//          int numberOfDoubleBonds = CHAIN_TOOLS.getNumberOfDoubleBondsFromFormula(fattyAcidFormula);
//          return new LipidFragment(rule.getLipidFragmentationRuleType(),
//              rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
//              lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
//              LipidChainType.ACYL_CHAIN, msMsScan);
//        }
    //}

    return null;
  }

}
