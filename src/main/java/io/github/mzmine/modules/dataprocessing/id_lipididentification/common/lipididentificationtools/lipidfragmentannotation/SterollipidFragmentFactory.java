package io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.lipidfragmentannotation;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidannotationmodules.LipidAnnotationChainParameters;
import java.util.List;

public class SterollipidFragmentFactory extends AbstractLipidFragmentFactory implements
    ILipidFragmentFactory {

  public SterollipidFragmentFactory(Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation,
      IonizationType ionizationType, LipidFragmentationRule[] rules, DataPoint dataPoint,
      Scan msMsScan, LipidAnnotationChainParameters chainParameters) {
    super(mzTolRangeMSMS, lipidAnnotation, ionizationType, rules, dataPoint, msMsScan,
        chainParameters);
  }

  @Override
  public List<LipidFragment> findLipidFragments() {
    return findCommonLipidFragment();
  }
}
