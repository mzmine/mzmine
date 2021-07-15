/*
 * Copyright 2006-2021 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.lipidchain;

import org.openscience.cdk.interfaces.IMolecularFormula;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidChainType;

public class AcylLipidChain implements ILipidChain {

  private String chainAnnotation;
  private IMolecularFormula molecularFormula;
  private int numberOfCarbons;
  private int numberOfDBEs;
  private static final LipidChainType LIPID_CHAIN_TYPE = LipidChainType.ACYL_CHAIN;

  public AcylLipidChain(String chainAnnotation, IMolecularFormula molecularFormula,
      int numberOfCarbons, int numberOfDBEs) {
    this.chainAnnotation = chainAnnotation;
    this.molecularFormula = molecularFormula;
    this.numberOfCarbons = numberOfCarbons;
    this.numberOfDBEs = numberOfDBEs;
  }

  public String getChainAnnotation() {
    return chainAnnotation;
  }


  public IMolecularFormula getChainMolecularFormula() {
    return molecularFormula;
  }


  public int getNumberOfCarbons() {
    return numberOfCarbons;
  }


  public int getNumberOfDBEs() {
    return numberOfDBEs;
  }


  public LipidChainType getLipidChainType() {
    return LIPID_CHAIN_TYPE;
  }

}
