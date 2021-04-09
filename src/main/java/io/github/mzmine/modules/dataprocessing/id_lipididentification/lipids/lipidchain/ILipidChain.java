package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.lipidchain;

import org.openscience.cdk.interfaces.IMolecularFormula;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidChainType;

public interface ILipidChain {

  String getChainAnnotation();

  int getNumberOfCarbons();

  int getNumberOfDBEs();

  IMolecularFormula getChainMolecularFormula();

  LipidChainType getLipidChainType();

}
