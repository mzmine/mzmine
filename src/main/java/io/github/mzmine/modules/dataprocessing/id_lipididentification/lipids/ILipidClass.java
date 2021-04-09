package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids;

import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidChainType;

public interface ILipidClass {

  String getName();

  String getAbbr();

  String getBackBoneFormula();

  LipidChainType[] getChainTypes();

  LipidFragmentationRule[] getFragmentationRules();

}
