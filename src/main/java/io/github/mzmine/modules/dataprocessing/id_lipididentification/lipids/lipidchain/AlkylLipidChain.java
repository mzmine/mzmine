package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.lipidchain;

import org.openscience.cdk.interfaces.IMolecularFormula;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidChainType;

public class AlkylLipidChain implements ILipidChain {

  private String chainAnnotation;
  private IMolecularFormula molecularFormula;
  private int numberOfCarbons;
  private int numberOfDBEs;
  private static final LipidChainType lipidChainType = LipidChainType.ALKYL_CHAIN;

  public AlkylLipidChain(String chainAnnotation, IMolecularFormula molecularFormula,
      int numberOfCarbons, int numberOfDBEs) {
    this.chainAnnotation = chainAnnotation;
    this.molecularFormula = molecularFormula;
    this.numberOfCarbons = numberOfCarbons;
    this.numberOfDBEs = numberOfDBEs;
  }

  public int getNumberOfCarbons() {
    return numberOfCarbons;
  }

  public int getNumberOfDBEs() {
    return numberOfDBEs;
  }

  public String getChainAnnotation() {
    return chainAnnotation;
  }

  public IMolecularFormula getChainMolecularFormula() {
    return molecularFormula;
  }


  public LipidChainType getLipidChainType() {
    return lipidChainType;
  }

}
