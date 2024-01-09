package io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.lipidfragmentannotation;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidannotationmodules.LipidAnnotationChainParameters;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
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
    return findCommonLipidFragments();
  }

}
