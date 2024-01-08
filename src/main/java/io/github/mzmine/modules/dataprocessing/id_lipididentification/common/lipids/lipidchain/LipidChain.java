package io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.lipidchain;

import io.github.mzmine.util.FormulaUtils;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class LipidChain implements ILipidChain {

  private static final String XML_ELEMENT = "lipidchain";
  private static final String XML_CHAIN_ANNOTATION = "chainAnnotation";
  private static final String XML_CHAIN_FORMULA = "chainFormula";
  private static final String XML_NUMBER_OF_CARBONS = "numberOfCarbons";
  private static final String XML_NUMBER_OF_DBES = "numberOfDBEs";
  private static final String XML_CHAIN_TYPE = "chainType";
  private static final String XML_NUMBER_OF_OXYGENS = "numberOfOxygens";

  private final String chainAnnotation;
  private final IMolecularFormula molecularFormula;
  private final int numberOfCarbons;
  private final int numberOfDBEs;
  private final LipidChainType lipidChainType;
  private final int numberOfOxygens;

  public LipidChain(String chainAnnotation, IMolecularFormula molecularFormula, int numberOfCarbons,
      int numberOfDBEs, LipidChainType lipidChainType) {
    this.chainAnnotation = chainAnnotation;
    this.molecularFormula = molecularFormula;
    this.numberOfCarbons = numberOfCarbons;
    this.numberOfDBEs = numberOfDBEs;
    this.lipidChainType = lipidChainType;
    this.numberOfOxygens = lipidChainType.getFixNumberOfOxygens();
  }

  public LipidChain(String chainAnnotation, IMolecularFormula molecularFormula, int numberOfCarbons,
      int numberOfDBEs, LipidChainType lipidChainType, int numberOfAdditionalOxygens) {
    this.chainAnnotation = chainAnnotation;
    this.molecularFormula = molecularFormula;
    this.numberOfCarbons = numberOfCarbons;
    this.numberOfDBEs = numberOfDBEs;
    this.lipidChainType = lipidChainType;
    this.numberOfOxygens = lipidChainType.getFixNumberOfOxygens() + numberOfAdditionalOxygens;
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
    return lipidChainType;
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
    writer.writeCharacters(lipidChainType.name());
    writer.writeEndElement();
    writer.writeStartElement(XML_NUMBER_OF_OXYGENS);
    writer.writeCharacters(String.valueOf(numberOfOxygens));
    writer.writeEndElement();
    writer.writeEndElement();
  }

  public static ILipidChain loadFromXML(XMLStreamReader reader) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      throw new IllegalStateException("Expected start of lipid chain element.");
    }

    String chainAnnotation = null;
    IMolecularFormula molecularFormula = null;
    Integer numberOfCarbons = null;
    Integer numberOfDBEs = null;
    LipidChainType lipidChainType = null;

    while (reader.hasNext()) {
      reader.next();
      if (reader.isStartElement()) {
        String elementName = reader.getLocalName();
        switch (elementName) {
          case "chainAnnotation" -> chainAnnotation = reader.getElementText();
          case "chainFormula" ->
              molecularFormula = FormulaUtils.createMajorIsotopeMolFormula(reader.getElementText());
          case "numberOfCarbons" -> numberOfCarbons = Integer.parseInt(reader.getElementText());
          case "numberOfDBEs" -> numberOfDBEs = Integer.parseInt(reader.getElementText());
          case "chainType" -> lipidChainType = LipidChainType.valueOf(reader.getElementText());
        }
      } else if (reader.isEndElement() && reader.getLocalName().equals(XML_ELEMENT)) {
        break;
      }
    }

    if (chainAnnotation == null || molecularFormula == null || numberOfCarbons == null
        || numberOfDBEs == null || lipidChainType == null) {
      throw new XMLStreamException("Missing data in lipid chain XML.");
    }

    return new LipidChain(chainAnnotation, molecularFormula, numberOfCarbons, numberOfDBEs,
        lipidChainType);
  }


}
