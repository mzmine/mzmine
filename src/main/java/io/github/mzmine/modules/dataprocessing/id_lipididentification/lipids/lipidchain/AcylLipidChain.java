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

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.lipidchain;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidParsingUtils;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidChainType;
import io.github.mzmine.util.FormulaUtils;

public class AcylLipidChain implements ILipidChain {

  private static final String XML_ELEMENT = "acylchain";
  private static final String XML_CHAIN_ANNOTATION = "chainannotation";
  private static final String XML_CHAIN_FORMULA = "chainformula";
  private static final String XML_NUMBER_OF_CARBONS = "numberOfCarbons";
  private static final String XML_NUMBER_OF_DBES = "numberofdbes";
  private static final String XML_CHAIN_TYPE = "chaintype";

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
    writer.writeEndElement();
  }

  public static ILipidChain loadFromXML(XMLStreamReader reader) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      throw new IllegalStateException(
          "Cannot load acyl chain from the current element. Wrong name.");
    }

    String chainAnnotation = null;
    IMolecularFormula molecularFormula = null;
    Integer numberOfCarbons = null;
    Integer numberOfDBEs = null;
    LipidChainType lipidChainType = null;
    while (reader.hasNext()
        && !(reader.isEndElement() && reader.getLocalName().equals(XML_ELEMENT))) {
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
          lipidChainType = LipidParsingUtils.lipidChainTypeNameToLipidChainType(reader.getElementText());
          break;
        default:
          break;
      }
    }
    if (lipidChainType != null && lipidChainType.equals(LipidChainType.ACYL_CHAIN)) {
      return new AcylLipidChain(chainAnnotation, molecularFormula, numberOfCarbons, numberOfDBEs);
    }
    return null;
  }

}
