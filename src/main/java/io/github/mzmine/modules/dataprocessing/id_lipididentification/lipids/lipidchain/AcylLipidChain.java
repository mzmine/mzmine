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
 * USA.
 *
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
