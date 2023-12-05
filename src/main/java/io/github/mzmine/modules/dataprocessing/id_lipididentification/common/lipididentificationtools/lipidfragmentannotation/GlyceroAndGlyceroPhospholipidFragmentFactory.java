package io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.lipidfragmentannotation;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidannotationmodules.LipidAnnotationChainParameters;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.ArrayList;
import java.util.List;

public class GlyceroAndGlyceroPhospholipidFragmentFactory extends
    AbstractLipidFragmentFactory implements ILipidFragmentFactory {

  public GlyceroAndGlyceroPhospholipidFragmentFactory(MZTolerance mzToleranceMS2,
      ILipidAnnotation lipidAnnotation, IonizationType ionizationType,
      LipidFragmentationRule[] rules, Scan msMsScan,
      LipidAnnotationChainParameters chainParameters) {
    super(mzToleranceMS2, lipidAnnotation, ionizationType, rules, msMsScan, chainParameters);
  }

  public List<LipidFragment> findLipidFragments() {
    List<LipidFragment> commonLipidFragments = findCommonLipidFragments();
    List<LipidFragment> lipidFragments = new ArrayList<>(commonLipidFragments);
    for (LipidFragmentationRule rule : rules) {
      if (!ionizationType.equals(rule.getIonizationType())
          || rule.getLipidFragmentationRuleType() == null) {
        continue;
      }
      List<LipidFragment> detectedFragments = checkForGlyceroAndGlyceroPhospholipidSpecificRuleTypes(
          rule);
      if (detectedFragments != null) {
        lipidFragments.addAll(detectedFragments);
      }
    }
    return lipidFragments;
  }

  private List<LipidFragment> checkForGlyceroAndGlyceroPhospholipidSpecificRuleTypes(
      LipidFragmentationRule rule) {
    LipidFragmentationRuleType ruleType = rule.getLipidFragmentationRuleType();
    switch (ruleType) {

//      case ALKYLCHAIN_FRAGMENT -> {
//        // TODO
//        return null;
//      }
//      case ALKYLCHAIN_FRAGMENT_NL -> {
//        // TODO
//        return null;
//      }
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
    return List.of();
  }

}
