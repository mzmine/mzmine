/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class LipidChainFactory {

  static final String XML_CHAIN_ELEMENT = "lipidchain";
  private static final String XML_CHAIN_ANNOTATION = "chainAnnotation";
  private static final String XML_CHAIN_FORMULA = "chainFormula";
  private static final String XML_NUMBER_OF_CARBONS = "numberOfCarbons";
  private static final String XML_NUMBER_OF_DBES = "numberOfDBEs";
  private static final String XML_CHAIN_TYPE = "chainType";
  private static final String XML_NUMBER_OF_OXYGENS = "numberOfOxygens";

  public ILipidChain buildLipidChain(LipidChainType chainType, int chainLength, int numberOfDBE) {
    if (chainLength / 2 < numberOfDBE) {
      return null;
    }
    IMolecularFormula chainFormula = buildLipidChainFormula(chainType, chainLength, numberOfDBE);
    String chainAnnotation = buildLipidChainAnnotation(chainType, chainLength, numberOfDBE);

    return createLipidChain(chainAnnotation, chainFormula, chainLength, numberOfDBE, chainType);

  }

  /**
   * Builds a localized chain only when every double bond has a distinct position. Partial position
   * lists deliberately fall back to the established molecular-species chain notation.
   */
  public @Nullable ILipidChain buildLipidChain(final @NotNull LipidChainType chainType,
      final int chainLength, final int numberOfDBE,
      final @NotNull List<DoubleBondPosition> positions) {
    final IMolecularFormula chainFormula = buildLipidChainFormula(chainType, chainLength,
        numberOfDBE);
    if (chainFormula == null) {
      return null;
    }
    final String plainAnnotation = buildLipidChainAnnotation(chainType, chainLength, numberOfDBE);
    final LipidChain plainChain = new LipidChain(plainAnnotation, chainFormula, chainLength,
        numberOfDBE, chainType);
    final boolean fullyLocalized = numberOfDBE > 0 && positions.size() == numberOfDBE
        && positions.stream().map(DoubleBondPosition::position).distinct().count() == numberOfDBE;
    if (!fullyLocalized) {
      return plainChain;
    }
    final String positionAnnotation = buildLipidChainAnnotation(chainType, chainLength, numberOfDBE,
        positions);
    return new DoubleBondPositionLipidChain(plainChain, positions, positionAnnotation);
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
    int increment = onlySearchForEvenChainLengths ? 2 : 1;
    for (int chainLength = minChainLength; chainLength <= maxChainLength;
        chainLength += increment) {
      for (int numberOfDBEs = minDBEs; numberOfDBEs <= maxDBEs; numberOfDBEs++) {

        if (chainLength / 2 < numberOfDBEs) {
          break; // early break DBEs loop
        }
        ILipidChain chain = buildLipidChain(chainType, chainLength, numberOfDBEs);
        lipidChains.add(chain);
      }
    }
    return lipidChains;
  }


  /**
   * @return full neutral formula of FA chain with RCOOH
   */
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

  private @NotNull String buildLipidChainAnnotation(final @NotNull LipidChainType chainType,
      final int chainLength, final int numberOfDBE) {
    return buildLipidChainAnnotation(chainType, chainLength, numberOfDBE, List.of());
  }

  private @NotNull String buildLipidChainAnnotation(final @NotNull LipidChainType chainType,
      final int chainLength, final int numberOfDBE,
      final @NotNull List<DoubleBondPosition> positions) {
    final String prefix = switch (chainType) {
      case ALKYL_CHAIN -> "O-";
      case ACYL_CHAIN, ACYL_MONO_HYDROXY_CHAIN, TWO_ACYL_CHAINS_COMBINED, AMID_CHAIN,
           AMID_MONO_HYDROXY_CHAIN, SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN,
           SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN,
           SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN -> "";
    };
    final String oxygenSuffix = switch (chainType) {
      case ACYL_CHAIN, TWO_ACYL_CHAINS_COMBINED, ALKYL_CHAIN, AMID_CHAIN -> "";
      case ACYL_MONO_HYDROXY_CHAIN, AMID_MONO_HYDROXY_CHAIN,
           SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN -> ";O";
      case SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN -> ";O2";
      case SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN -> ";O3";
    };
    return prefix + chainLength + ":" + numberOfDBE
        + formatDoubleBondPositions(numberOfDBE, positions) + oxygenSuffix;
  }

  private @NotNull String formatDoubleBondPositions(final int numberOfDBE,
      final @NotNull List<DoubleBondPosition> positions) {
    if (numberOfDBE == 0 || positions.size() != numberOfDBE
        || positions.stream().map(DoubleBondPosition::position).distinct().count() != numberOfDBE) {
      return "";
    }
    return positions.stream().sorted().map(DoubleBondPosition::toShorthand)
        .collect(java.util.stream.Collectors.joining(",", "(", ")"));
  }

  public @NotNull String connectLipidChainAnnotations(final @NotNull List<ILipidChain> chains) {
    return connectLipidChainAnnotations(chains, false);
  }

  public @NotNull String connectDoubleBondPositionLevelChainAnnotations(
      final @NotNull List<ILipidChain> chains) {
    return connectLipidChainAnnotations(chains, true);
  }

  private @NotNull String connectLipidChainAnnotations(final @NotNull List<ILipidChain> chains,
      final boolean alwaysUseUnderscore) {
    final StringBuilder sb = new StringBuilder();
    final Comparator<ILipidChain> chainComparator = Comparator.comparing(
            (ILipidChain chain) -> chain.getLipidChainType().getPriorityForSorting())
        .thenComparing(ILipidChain::getNumberOfCarbons).thenComparing(ILipidChain::getNumberOfDBEs)
        .thenComparing(ILipidChain::getNumberOfOxygens)
        .thenComparing(ILipidChain::getDoubleBondPositions, DoubleBondPosition::compareLists);
    chains.sort(chainComparator);
    final boolean allChainsAreSame = !alwaysUseUnderscore && allChainsAreSame(chains);
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
                || chain.getLipidChainType()
                == LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN
                || chain.getLipidChainType()
                == LipidChainType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN);
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

  public static @NotNull ILipidChain loadLipidChainFromXML(final @NotNull XMLStreamReader reader)
      throws XMLStreamException {
    if (!(reader.isStartElement() && XML_CHAIN_ELEMENT.equals(reader.getLocalName()))) {
      throw new XMLStreamException("Expected start of lipid chain element.");
    }

    String chainAnnotation = null;
    IMolecularFormula molecularFormula = null;
    Integer numberOfCarbons = null;
    Integer numberOfDBEs = null;
    Integer numberOfOxygens = null;
    LipidChainType lipidChainType = null;
    final List<DoubleBondPosition> positions = new ArrayList<>();

    while (reader.hasNext()) {
      reader.next();
      if (reader.isStartElement()) {
        switch (reader.getLocalName()) {
          case XML_CHAIN_ANNOTATION -> chainAnnotation = reader.getElementText();
          case XML_CHAIN_FORMULA -> molecularFormula =
              FormulaUtils.createMajorIsotopeMolFormulaWithCharge(reader.getElementText());
          case XML_NUMBER_OF_CARBONS -> numberOfCarbons = Integer.parseInt(reader.getElementText());
          case XML_NUMBER_OF_DBES -> numberOfDBEs = Integer.parseInt(reader.getElementText());
          case XML_CHAIN_TYPE -> lipidChainType = LipidChainType.valueOf(reader.getElementText());
          case XML_NUMBER_OF_OXYGENS -> numberOfOxygens = Integer.parseInt(reader.getElementText());
          case DoubleBondPosition.XML_ELEMENT -> positions.add(DoubleBondPosition.loadFromXML(reader));
          default -> {
          }
        }
      } else if (reader.isEndElement() && XML_CHAIN_ELEMENT.equals(reader.getLocalName())) {
        break;
      }
    }

    if (chainAnnotation == null || molecularFormula == null || numberOfCarbons == null
        || numberOfDBEs == null || lipidChainType == null) {
      throw new XMLStreamException("Missing data in lipid chain XML.");
    }
    final int totalOxygens = numberOfOxygens == null ? lipidChainType.getFixNumberOfOxygens()
        : numberOfOxygens;
    final int additionalOxygens = totalOxygens - lipidChainType.getFixNumberOfOxygens();
    final LipidChain plainChain = new LipidChain(
        positions.isEmpty() ? chainAnnotation
            : new LipidChainFactory().buildLipidChainAnnotation(lipidChainType, numberOfCarbons,
                numberOfDBEs), molecularFormula, numberOfCarbons, numberOfDBEs, lipidChainType,
        additionalOxygens);
    return positions.isEmpty() ? plainChain
        : new DoubleBondPositionLipidChain(plainChain, positions, chainAnnotation);
  }

  static void writeChainBody(final @NotNull XMLStreamWriter writer,
      final @NotNull ILipidChain chain) throws XMLStreamException {
    writer.writeStartElement(XML_CHAIN_ANNOTATION);
    writer.writeCharacters(chain.getChainAnnotation());
    writer.writeEndElement();
    writer.writeStartElement(XML_CHAIN_FORMULA);
    writer.writeCharacters(FormulaUtils.getFormulaString(chain.getChainMolecularFormula()));
    writer.writeEndElement();
    writer.writeStartElement(XML_NUMBER_OF_CARBONS);
    writer.writeCharacters(Integer.toString(chain.getNumberOfCarbons()));
    writer.writeEndElement();
    writer.writeStartElement(XML_NUMBER_OF_DBES);
    writer.writeCharacters(Integer.toString(chain.getNumberOfDBEs()));
    writer.writeEndElement();
    writer.writeStartElement(XML_CHAIN_TYPE);
    writer.writeCharacters(chain.getLipidChainType().name());
    writer.writeEndElement();
    writer.writeStartElement(XML_NUMBER_OF_OXYGENS);
    writer.writeCharacters(Integer.toString(chain.getNumberOfOxygens()));
    writer.writeEndElement();
  }
}
