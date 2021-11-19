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
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils;

import java.util.List;
import org.openscience.cdk.interfaces.IMolecularFormula;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.lipidchain.AcylLipidChain;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.lipidchain.AlkylLipidChain;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.lipidchain.ILipidChain;
import io.github.mzmine.util.FormulaUtils;

/**
 * This class constructs alkyl and acyl chains for lipids.
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class LipidChainFactory {

  public ILipidChain buildLipidChain(LipidChainType chainType, int chainLength, int numberOfDBE) {
    IMolecularFormula chainFormula = buildLipidChainFormula(chainType, chainLength, numberOfDBE);
    String chainAnnotation = builLipidChainAnnotation(chainType, chainLength, numberOfDBE);

    if (chainType.equals(LipidChainType.ACYL_CHAIN)) {
      return new AcylLipidChain(chainAnnotation, chainFormula, chainLength, numberOfDBE);
    } else if (chainType.equals(LipidChainType.ALKYL_CHAIN)) {
      return new AlkylLipidChain(chainAnnotation, chainFormula, chainLength, numberOfDBE);
    } else {
      return null;
    }
  }

  public IMolecularFormula buildLipidChainFormula(LipidChainType chainType, int chainLength,
      int numberOfDBE) {
    switch (chainType) {
      case ACYL_CHAIN:
        return calculateMolecularFormulaAcylChain(chainLength, numberOfDBE);
      case ALKYL_CHAIN:
        return calculateMolecularFormulaAlkylChain(chainLength, numberOfDBE);
      default:
        return calculateMolecularFormulaAcylChain(chainLength, numberOfDBE);
    }
  }

  private IMolecularFormula calculateMolecularFormulaAcylChain(int chainLength,
      int numberOfDoubleBonds) {
    int numberOfCAtoms = chainLength;
    int numberOfHAtoms = numberOfCAtoms * 2 - numberOfDoubleBonds * 2;
    int numberOfOAtoms = 2;
    return FormulaUtils.createMajorIsotopeMolFormula(
        "C" + numberOfCAtoms + "H" + numberOfHAtoms + "O" + numberOfOAtoms);
  }

  private IMolecularFormula calculateMolecularFormulaAlkylChain(int chainLength,
      int numberOfDoubleBonds) {
    int numberOfCAtoms = chainLength;
    int numberOfHAtoms = numberOfCAtoms * 2 - numberOfDoubleBonds * 2 + 2;
    return FormulaUtils.createMajorIsotopeMolFormula("C" + numberOfCAtoms + "H" + numberOfHAtoms);
  }

  private String builLipidChainAnnotation(LipidChainType chainType, int chainLength,
      int numberOfDBE) {
    switch (chainType) {
      case ACYL_CHAIN:
        return chainLength + ":" + numberOfDBE;
      case ALKYL_CHAIN:
        return "O-" + chainLength + ":" + numberOfDBE;
      default:
        return chainLength + ":" + numberOfDBE;
    }
  }

  public String connectLipidChainAnnotations(List<ILipidChain> chains) {
    StringBuilder sb = new StringBuilder();
    chains.sort((o1, o2) -> {

      // alkyl before acyl
      if (o1.getLipidChainType().equals(LipidChainType.ALKYL_CHAIN)
          && !(o2.getLipidChainType().equals(LipidChainType.ALKYL_CHAIN))) {
        return -1;
      }

      // chain length
      if (o1.getNumberOfCarbons() < o2.getNumberOfCarbons()) {
        return -1;
      }

      // DBE number
      if (o1.getNumberOfDBEs() < o2.getNumberOfDBEs()) {
        return -1;
      }
      return 0;
    });
    boolean allChainsAreSame = allChainsAreSame(chains);
    for (int i = 0; i < chains.size(); i++) {
      if (i == 0) {
        sb.append(chains.get(i).getChainAnnotation());
      } else {
        if (allChainsAreSame) {
          sb.append("/" + chains.get(i).getChainAnnotation());
        } else {
          sb.append("_" + chains.get(i).getChainAnnotation());
        }
      }
    }
    return sb.toString();
  }

  private boolean allChainsAreSame(List<ILipidChain> chains) {
    String firstChainAnnotation = chains.get(0).getChainAnnotation();
    for (int i = 1; i < chains.size(); i++)
      if (!chains.get(i).getChainAnnotation().equals(firstChainAnnotation)) {
        return false;
      }
    return true;
  }


}
