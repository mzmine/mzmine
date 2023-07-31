/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipidutils;

import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.lipidchain.AcylLipidChain;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.lipidchain.AlkylLipidChain;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.lipidchain.AmidLipidChain;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.lipidchain.ILipidChain;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.lipidchain.SphingolipidDiHydroxyBackboneChain;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.lipidchain.TwoAcylLipidChains;
import io.github.mzmine.util.FormulaUtils;
import java.util.ArrayList;
import java.util.List;
import org.openscience.cdk.interfaces.IMolecularFormula;

/**
 * This class constructs alkyl and acyl chains for lipids.
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class LipidChainFactory {

  public ILipidChain buildLipidChain(LipidChainType chainType, int chainLength, int numberOfDBE) {
    if (chainLength / 2 < numberOfDBE) {
      return null;
    }
    IMolecularFormula chainFormula = buildLipidChainFormula(chainType, chainLength, numberOfDBE);
    String chainAnnotation = buildLipidChainAnnotation(chainType, chainLength, numberOfDBE);

    return switch (chainType) {
      case ACYL_CHAIN:
        yield new AcylLipidChain(chainAnnotation, chainFormula, chainLength, numberOfDBE);
      case ACYL_MONO_HYDROXY_CHAIN:
        yield null;
      case TWO_ACYL_CHAINS_COMBINED:
        yield new TwoAcylLipidChains(chainAnnotation, chainFormula, chainLength, numberOfDBE);
      case ALKYL_CHAIN:
        yield new AlkylLipidChain(chainAnnotation, chainFormula, chainLength, numberOfDBE);
      case AMID_CHAIN:
        yield null;//TODO
      case AMID_MONO_HYDROXY_CHAIN:
        yield new AmidLipidChain(chainAnnotation, chainFormula, chainLength, numberOfDBE);
      case SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN:
        yield null;//TODO
      case SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN:
        yield new SphingolipidDiHydroxyBackboneChain(chainAnnotation, chainFormula, chainLength,
            numberOfDBE);
      case SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN:
        yield null;//TODO
    };
  }

  public IMolecularFormula buildLipidChainFormula(LipidChainType chainType, int chainLength,
      int numberOfDBE) {
    if (chainLength / 2 < numberOfDBE) {
      return null;
    }
    return switch (chainType) {
      case ACYL_CHAIN -> calculateMolecularFormulaAcylChain(chainLength, numberOfDBE);
      case ACYL_MONO_HYDROXY_CHAIN -> null; //TODO
      case TWO_ACYL_CHAINS_COMBINED -> calculateMolecularFormulaAcylChain(chainLength, numberOfDBE);
      case ALKYL_CHAIN -> calculateMolecularFormulaAlkylChain(chainLength, numberOfDBE);
      case AMID_CHAIN -> calculateMolecularFormulaAmidChain(chainLength, numberOfDBE);//TODO
      case AMID_MONO_HYDROXY_CHAIN -> null;//TODO
      case SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN ->
          calculateMolecularFormulaSphingolipidBackboneChain(chainLength, numberOfDBE, chainType);
      case SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN ->
          calculateMolecularFormulaSphingolipidBackboneChain(chainLength, numberOfDBE, chainType);
      case SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN ->
          calculateMolecularFormulaSphingolipidBackboneChain(chainLength, numberOfDBE, chainType);
    };
  }

  public List<ILipidChain> buildLipidChainsInRange(LipidChainType chainType, int minChainLength,
      int maxChainLength, int minDBEs, int maxDBEs, boolean onlySearchForEvenChainLengths) {
    List<ILipidChain> lipidChains = new ArrayList<>();
    for (int chainLength = minChainLength; chainLength <= maxChainLength; chainLength++) {
      if (onlySearchForEvenChainLengths && chainLength % 2 != 0) {
        continue;
      }
      for (int numberOfDBEs = minDBEs; numberOfDBEs <= maxDBEs; numberOfDBEs++) {

        if (chainLength / 2 < numberOfDBEs) {
          continue;
        }
        ILipidChain chain = buildLipidChain(chainType, chainLength, numberOfDBEs);
        lipidChains.add(chain);
      }
    }
    return lipidChains;
  }


  private IMolecularFormula calculateMolecularFormulaAcylChain(int chainLength,
      int numberOfDoubleBonds) {
    int numberOfHAtoms = chainLength * 2 - numberOfDoubleBonds * 2;
    int numberOfOAtoms = 2;
    return FormulaUtils.createMajorIsotopeMolFormula(
        "C" + chainLength + "H" + numberOfHAtoms + "O" + numberOfOAtoms);
  }

  private IMolecularFormula calculateMolecularFormulaAlkylChain(int chainLength,
      int numberOfDoubleBonds) {
    int numberOfHAtoms = chainLength * 2 - numberOfDoubleBonds * 2 + 2;
    return FormulaUtils.createMajorIsotopeMolFormula("C" + chainLength + "H" + numberOfHAtoms);
  }

  private IMolecularFormula calculateMolecularFormulaAmidChain(int chainLength,
      int numberOfDoubleBonds) {
    int numberOfHAtoms = chainLength * 2 - numberOfDoubleBonds * 2 + 1;
    int numberOfOAtoms = 1;
    int numberOfNAtoms = 1;
    return FormulaUtils.createMajorIsotopeMolFormula(
        "C" + chainLength + "H" + numberOfHAtoms + "O" + numberOfOAtoms + "N" + numberOfNAtoms);
  }

  private IMolecularFormula calculateMolecularFormulaSphingolipidBackboneChain(int chainLength,
      int numberOfDoubleBonds, LipidChainType chainType) {
    int numberOfOAtoms = chainType.getFixNumberOfOxygens();
    int numberOfHAtoms = chainLength * 2 - numberOfDoubleBonds * 2 + 1;
    return FormulaUtils.createMajorIsotopeMolFormula(
        "C" + chainLength + "H" + numberOfHAtoms + "O" + numberOfOAtoms);
  }

  private String buildLipidChainAnnotation(LipidChainType chainType, int chainLength,
      int numberOfDBE) {
    return switch (chainType) {
      case ACYL_CHAIN -> chainLength + ":" + numberOfDBE;
      case ACYL_MONO_HYDROXY_CHAIN -> chainLength + ":" + numberOfDBE + ";O";
      case TWO_ACYL_CHAINS_COMBINED -> chainLength + ":" + numberOfDBE;
      case ALKYL_CHAIN -> "O-" + chainLength + ":" + numberOfDBE;
      case AMID_CHAIN -> chainLength + ":" + numberOfDBE;
      case AMID_MONO_HYDROXY_CHAIN -> chainLength + ":" + numberOfDBE + ";O";
      case SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN -> chainLength + ":" + numberOfDBE + ";O";
      case SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN -> chainLength + ":" + numberOfDBE + ";2O";
      case SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN -> chainLength + ":" + numberOfDBE + ";3O";
    };
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
          sb.append("/").append(chains.get(i).getChainAnnotation());
        } else {
          sb.append("_").append(chains.get(i).getChainAnnotation());
        }
      }
    }
    return sb.toString();
  }

  private boolean allChainsAreSame(List<ILipidChain> chains) {
    String firstChainAnnotation = chains.get(0).getChainAnnotation();
    for (int i = 1; i < chains.size(); i++) {
      if (!chains.get(i).getChainAnnotation().equals(firstChainAnnotation)) {
        return false;
      }
    }
    return true;
  }


}
