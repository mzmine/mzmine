package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidChainType;

public class LipidFragment {

  private LipidFragmentationRuleType ruleType;
  private LipidAnnotationLevel lipidFragmentInformationLevelType;
  private Double mzExact;
  private DataPoint dataPoint;
  private ILipidClass lipidClass;
  private Integer chainLength;
  private Integer numberOfDBEs;
  private LipidChainType lipidChainType;
  private Scan msMsScan;

  public LipidFragment(LipidFragmentationRuleType ruleType,
      LipidAnnotationLevel lipidFragmentInformationLevelType, Double mzExact, DataPoint dataPoint,
      ILipidClass lipidClass, Integer chainLength, Integer numberOfDBEs,
      LipidChainType lipidChainType, Scan msMsScan) {
    this.ruleType = ruleType;
    this.lipidFragmentInformationLevelType = lipidFragmentInformationLevelType;
    this.mzExact = mzExact;
    this.dataPoint = dataPoint;
    this.lipidClass = lipidClass;
    this.chainLength = chainLength;
    this.numberOfDBEs = numberOfDBEs;
    this.lipidChainType = lipidChainType;
    this.msMsScan = msMsScan;
  }

  public LipidFragmentationRuleType getRuleType() {
    return ruleType;
  }

  public LipidAnnotationLevel getLipidFragmentInformationLevelType() {
    return lipidFragmentInformationLevelType;
  }

  public Double getMzExact() {
    return mzExact;
  }

  public DataPoint getDataPoint() {
    return dataPoint;
  }

  public ILipidClass getLipidClass() {
    return lipidClass;
  }

  public Integer getChainLength() {
    return chainLength;
  }

  public Integer getNumberOfDBEs() {
    return numberOfDBEs;
  }

  public LipidChainType getLipidChainType() {
    return lipidChainType;
  }

  public Scan getMsMsScan() {
    return msMsScan;
  }

}
