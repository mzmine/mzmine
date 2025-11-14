/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain;

import io.github.mzmine.util.FormulaUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class LipidChainFactory {

  public ILipidChain buildLipidChain(LipidChainType chainType, int chainLength, int numberOfDBE) {
    if (chainLength / 2 < numberOfDBE) {
      return null;
    }
    IMolecularFormula chainFormula = buildLipidChainFormula(chainType, chainLength, numberOfDBE);
    String chainAnnotation = buildLipidChainAnnotation(chainType, chainLength, numberOfDBE);

    return createLipidChain(chainAnnotation, chainFormula, chainLength, numberOfDBE, chainType);

  }

  public IMolecularFormula buildLipidChainFormula(LipidChainType chainType, int chainLength,
      int numberOfDBE) {
    if (chainLength / 2 < numberOfDBE) {
      return null;
    }
    return switch (chainType) {
      case ACYL_CHAIN, TWO_ACYL_CHAINS_COMBINED ->
          calculateMolecularFormulaAcylChain(chainLength, numberOfDBE);
      case ACYL_MONO_HYDROXY_CHAIN ->
          calculateMolecularFormulaAcylMonoHydroxyChain(chainLength, numberOfDBE,
              chainType.getFixNumberOfOxygens());
      case ALKYL_CHAIN -> calculateMolecularFormulaAlkylChain(chainLength, numberOfDBE);
      case AMID_CHAIN -> calculateMolecularFormulaAmidChain(chainLength, numberOfDBE);
      case AMID_MONO_HYDROXY_CHAIN ->
          calculateMolecularFormulaAmidMonoHydroxyChain(chainLength, numberOfDBE,
              chainType.getFixNumberOfOxygens());
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
    return FormulaUtils.createMajorIsotopeMolFormulaWithCharge(
        "C" + chainLength + "H" + numberOfHAtoms + "O" + numberOfOAtoms);
  }

  private IMolecularFormula calculateMolecularFormulaAcylMonoHydroxyChain(int chainLength,
      int numberOfDoubleBonds, int fixNumberOfOxygens) {
    int numberOfHAtoms = chainLength * 2 - numberOfDoubleBonds * 2;
    int numberOfOAtoms = 2 + fixNumberOfOxygens;
    return FormulaUtils.createMajorIsotopeMolFormulaWithCharge(
        "C" + chainLength + "H" + numberOfHAtoms + "O" + numberOfOAtoms);
  }

  private IMolecularFormula calculateMolecularFormulaAlkylChain(int chainLength,
      int numberOfDoubleBonds) {
    int numberOfHAtoms = chainLength * 2 - numberOfDoubleBonds * 2 + 2;
    return FormulaUtils.createMajorIsotopeMolFormulaWithCharge(
        "C" + chainLength + "H" + numberOfHAtoms);
  }

  private IMolecularFormula calculateMolecularFormulaAmidChain(int chainLength,
      int numberOfDoubleBonds) {
    int numberOfHAtoms = chainLength * 2 - numberOfDoubleBonds * 2 + 1;
    int numberOfOAtoms = 1;
    int numberOfNAtoms = 1;
    return FormulaUtils.createMajorIsotopeMolFormulaWithCharge(
        "C" + chainLength + "H" + numberOfHAtoms + "O" + numberOfOAtoms + "N" + numberOfNAtoms);
  }

  private IMolecularFormula calculateMolecularFormulaAmidMonoHydroxyChain(int chainLength,
      int numberOfDoubleBonds, int fixNumberOfOxygens) {
    int numberOfHAtoms = chainLength * 2 - numberOfDoubleBonds * 2 + 1;
    int numberOfOAtoms = 1 + fixNumberOfOxygens;
    int numberOfNAtoms = 1;
    return FormulaUtils.createMajorIsotopeMolFormulaWithCharge(
        "C" + chainLength + "H" + numberOfHAtoms + "O" + numberOfOAtoms + "N" + numberOfNAtoms);
  }

  private IMolecularFormula calculateMolecularFormulaSphingolipidBackboneChain(int chainLength,
      int numberOfDoubleBonds, LipidChainType chainType) {
    int numberOfOAtoms = chainType.getFixNumberOfOxygens();
    int numberOfHAtoms = chainLength * 2 - numberOfDoubleBonds * 2 + 3;
    int numberOfNAtoms = 1;
    return FormulaUtils.createMajorIsotopeMolFormulaWithCharge(
        "C" + chainLength + "H" + numberOfHAtoms + "N" + numberOfNAtoms + "O" + numberOfOAtoms);
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
    Comparator<ILipidChain> chainComparator = Comparator.comparing(
            (ILipidChain chain) -> chain.getLipidChainType().getPriorityForSorting())
        .thenComparing(ILipidChain::getNumberOfCarbons).thenComparing(ILipidChain::getNumberOfDBEs)
        .thenComparing(ILipidChain::getNumberOfOxygens);
    chains.sort(chainComparator);
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
    boolean chainsAreEqual = false;
    boolean sameNumberOfCarbons = false;
    boolean sameNumberOfDoubleBondEquivalents = false;
    boolean sameNumberOfOxygens = false;
    boolean sameChainTypes = false;

    if (chains.size() > 1) {

      // check carbons
      sameNumberOfCarbons = allElementsAreSame(
          chains.stream().map(ILipidChain::getNumberOfCarbons).toArray(Integer[]::new));

      // check dbes
      if (sameNumberOfCarbons) {
        sameNumberOfDoubleBondEquivalents = allElementsAreSame(
            chains.stream().map(ILipidChain::getNumberOfDBEs).toArray(Integer[]::new));
      }

      // check number of oxygens
      if (sameNumberOfCarbons && sameNumberOfDoubleBondEquivalents) {
        sameNumberOfOxygens = allElementsAreSame(
            chains.stream().map(ILipidChain::getNumberOfOxygens).toArray(Integer[]::new));
      }
      if (sameNumberOfCarbons && sameNumberOfDoubleBondEquivalents && sameNumberOfOxygens) {
        sameChainTypes = allChainTypesAreSame(
            chains.stream().map(ILipidChain::getLipidChainType).toArray(LipidChainType[]::new));
      }
      if (sameNumberOfCarbons && sameNumberOfDoubleBondEquivalents && sameNumberOfOxygens
          && sameChainTypes) {
        chainsAreEqual = true;
      }
      // in case of Sphingolipids evaluate always true
      if (!chainsAreEqual) {
        boolean isSphingolipid = chains.stream().anyMatch(chain ->
            chain.getLipidChainType() == LipidChainType.SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN
            || chain.getLipidChainType() == LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN
            || chain.getLipidChainType() == LipidChainType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN);
        if (isSphingolipid) {
          chainsAreEqual = true;
        }
      }
    }
    return chainsAreEqual;
  }

  private boolean allChainTypesAreSame(LipidChainType[] types) {
    LipidChainType firstChain = types[0];
    for (int i = 1; i < types.length; i++) {
      if (types[i] != firstChain) {
        return false;
      }
    }
    return true;
  }

  private boolean allElementsAreSame(Integer[] arr) {
    Integer firstElement = arr[0];
    for (int i = 1; i < arr.length; i++) {
      if (arr[i] != firstElement) {
        return false;
      }
    }
    return true;
  }

  public static ILipidChain createLipidChain(String chainAnnotation,
      IMolecularFormula molecularFormula, int numberOfCarbons, int numberOfDBEs,
      LipidChainType type) {
    return new LipidChain(chainAnnotation, molecularFormula, numberOfCarbons, numberOfDBEs, type);
  }

  public static ILipidChain createLipidChain(String chainAnnotation,
      IMolecularFormula molecularFormula, int numberOfCarbons, int numberOfDBEs,
      LipidChainType type, int additionalNumberOfOxygens) {
    return new LipidChain(chainAnnotation, molecularFormula, numberOfCarbons, numberOfDBEs, type,
        additionalNumberOfOxygens);
  }

  public static ILipidChain loadLipidChainFromXML(XMLStreamReader reader)
      throws XMLStreamException {
    return LipidChain.loadFromXML(reader);
  }
}
