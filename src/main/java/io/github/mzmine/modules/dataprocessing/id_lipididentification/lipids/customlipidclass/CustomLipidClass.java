package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.customlipidclass;

import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidChainType;

public class CustomLipidClass implements ILipidClass {

  private String name;
  private String abbr;
  private String backBoneFormula;
  private LipidChainType[] chainTypes;
  private LipidFragmentationRule[] fragmentationRules;

  public CustomLipidClass(String name, String abbr, String backBoneFormula,
      LipidChainType[] chainTypes, LipidFragmentationRule[] fragmentationRules) {
    this.name = name;
    this.abbr = abbr;
    this.backBoneFormula = backBoneFormula;
    this.chainTypes = chainTypes;
    this.fragmentationRules = fragmentationRules;
  }

  public String getName() {
    return name;
  }

  public String getAbbr() {
    return abbr;
  }

  public String getBackBoneFormula() {
    return backBoneFormula;
  }

  public LipidChainType[] getChainTypes() {
    return chainTypes;
  }

  public LipidFragmentationRule[] getFragmentationRules() {
    return fragmentationRules;
  }

  @Override
  public String toString() {
    return this.abbr + " " + this.name;
  }

}
