package io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.lipidfragmentannotation;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipidutils.LipidChainFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidannotationmodules.LipidAnnotationChainParameters;
import java.util.ArrayList;
import java.util.List;

public class FattyAcylFragmentFactory extends AbstractLipidFragmentFactory implements
    ILipidFragmentFactory {

  private static final LipidChainFactory LIPID_CHAIN_FACTORY = new LipidChainFactory();

  public FattyAcylFragmentFactory(Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation,
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
