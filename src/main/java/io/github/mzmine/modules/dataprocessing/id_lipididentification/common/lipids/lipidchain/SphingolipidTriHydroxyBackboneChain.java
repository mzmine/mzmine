package io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.lipidchain;

import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipidutils.LipidParsingUtils;
import io.github.mzmine.util.FormulaUtils;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class SphingolipidTriHydroxyBackboneChain implements ILipidChain {

  private static final String XML_ELEMENT = "sphingolipiddihydroxybackbonechain";
  private static final String XML_CHAIN_ANNOTATION = "chainannotation";
  private static final String XML_CHAIN_FORMULA = "chainformula";
  private static final String XML_NUMBER_OF_CARBONS = "numberOfCarbons";
  private static final String XML_NUMBER_OF_DBES = "numberofdbes";
  private static final String XML_CHAIN_TYPE = "chaintype";
  private static final String XML_NUMBER_OF_OXYGENS = "numberofoxygens";

  private final String chainAnnotation;
  private final IMolecularFormula molecularFormula;
  private final int numberOfCarbons;
  private final int numberOfDBEs;
  private static final LipidChainType LIPID_CHAIN_TYPE = LipidChainType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN;
  private final int numberOfOxygens;

  public SphingolipidTriHydroxyBackboneChain(String chainAnnotation,
      IMolecularFormula molecularFormula, int numberOfCarbons, int numberOfDBEs) {
    this.chainAnnotation = chainAnnotation;
    this.molecularFormula = molecularFormula;
    this.numberOfCarbons = numberOfCarbons;
    this.numberOfDBEs = numberOfDBEs;
    numberOfOxygens = LIPID_CHAIN_TYPE.getFixNumberOfOxygens();
  }

  public SphingolipidTriHydroxyBackboneChain(String chainAnnotation,
      IMolecularFormula molecularFormula, int numberOfCarbons, int numberOfDBEs,
      int numberOfAdditionalOxygens) {
    this.chainAnnotation = chainAnnotation;
    this.molecularFormula = molecularFormula;
    this.numberOfCarbons = numberOfCarbons;
    this.numberOfDBEs = numberOfDBEs;
    this.numberOfOxygens = LIPID_CHAIN_TYPE.getFixNumberOfOxygens() + numberOfAdditionalOxygens;
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


  public int getNumberOfOxygens() {
    return numberOfOxygens;
  }

  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);
    writer.writeStartElement(XML_CHAIN_ANNOTATION);
    writer.writeCharacters(chainAnnotation);
    writer.writeEndElement();
    writer.writeStartElement(XML_CHAIN_FORMULA);
    writer.writeCharacters(MolecularFormulaManipulator.getString(molecularFormula));
    writer.writeEndElement();
    writer.writeStartElement(XML_NUMBER_OF_CARBONS);
    writer.writeCharacters(String.valueOf(numberOfCarbons));
    writer.writeEndElement();
    writer.writeStartElement(XML_NUMBER_OF_DBES);
    writer.writeCharacters(String.valueOf(numberOfDBEs));
    writer.writeEndElement();
    writer.writeStartElement(XML_CHAIN_TYPE);
    writer.writeCharacters(LIPID_CHAIN_TYPE.name());
    writer.writeEndElement();
    writer.writeStartElement(XML_NUMBER_OF_OXYGENS);
    writer.writeCharacters(String.valueOf(numberOfOxygens));
    writer.writeEndElement();
    writer.writeEndElement();
  }

  public static ILipidChain loadFromXML(XMLStreamReader reader) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      throw new IllegalStateException(
          "Cannot load sphingolipid trihydroxy backbone chain from the current element. Wrong name.");
    }

    String chainAnnotation = null;
    IMolecularFormula molecularFormula = null;
    Integer numberOfCarbons = null;
    Integer numberOfDBEs = null;
    LipidChainType lipidChainType = null;
    Integer numberOfOxygens = null;
    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(XML_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      switch (reader.getLocalName()) {
        case XML_CHAIN_ANNOTATION:
          chainAnnotation = reader.getElementText();
          break;
        case XML_CHAIN_FORMULA:
          molecularFormula = FormulaUtils.createMajorIsotopeMolFormula(reader.getElementText());
          break;
        case XML_NUMBER_OF_CARBONS:
          numberOfCarbons = Integer.parseInt(reader.getElementText());
          break;
        case XML_NUMBER_OF_DBES:
          numberOfDBEs = Integer.parseInt(reader.getElementText());
          break;
        case XML_CHAIN_TYPE:
          lipidChainType = LipidParsingUtils.lipidChainTypeNameToLipidChainType(
              reader.getElementText());
          break;
        case XML_NUMBER_OF_OXYGENS:
          numberOfOxygens = Integer.parseInt(reader.getElementText());
          break;
        default:
          break;
      }
    }
    if (lipidChainType != null && lipidChainType.equals(
        LipidChainType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN)) {
      return new SphingolipidTriHydroxyBackboneChain(chainAnnotation, molecularFormula,
          numberOfCarbons, numberOfDBEs, numberOfOxygens);
    }
    return null;
  }

}
