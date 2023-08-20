package io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.lipidfragmentannotation;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipidutils.LipidChainFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidannotationmodules.fattyacyls.FattyAcylAnnotationChainParameters;
import java.util.ArrayList;
import java.util.List;

public class FattyAcylFragmentFactory extends AbstractLipidFragmentFactory implements
    ILipidFragmentFactory {

  private static final LipidChainFactory LIPID_CHAIN_FACTORY = new LipidChainFactory();
  private final int minChainLength;
  private final int maxChainLength;
  private final int maxDoubleBonds;
  private final int minDoubleBonds;
  private final Boolean onlySearchForEvenChains;

  public FattyAcylFragmentFactory(Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation,
      IonizationType ionizationType, LipidFragmentationRule[] rules, DataPoint dataPoint,
      Scan msMsScan, FattyAcylAnnotationChainParameters chainParameters) {
    super(mzTolRangeMSMS, lipidAnnotation, ionizationType, rules, dataPoint, msMsScan);
    this.minChainLength = chainParameters.getParameter(
        FattyAcylAnnotationChainParameters.minChainLength).getValue();
    this.maxChainLength = chainParameters.getParameter(
        FattyAcylAnnotationChainParameters.maxChainLength).getValue();
    this.minDoubleBonds = chainParameters.getParameter(FattyAcylAnnotationChainParameters.minDBEs)
        .getValue();
    this.maxDoubleBonds = chainParameters.getParameter(FattyAcylAnnotationChainParameters.maxDBEs)
        .getValue();
    this.onlySearchForEvenChains = chainParameters.getParameter(
        FattyAcylAnnotationChainParameters.onlySearchForEvenChainLength).getValue();
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
      LipidFragment detectedFragment = checkForFattyAcylSpecificRuleTypes(rule);
      if (detectedFragment != null) {
        lipidFragments.add(detectedFragment);
      }
    }
    return lipidFragments;
  }

  //Nothing to do here yet
  private LipidFragment checkForFattyAcylSpecificRuleTypes(LipidFragmentationRule rule) {
    return null;
  }

}
