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

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidParsingUtils;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidAnnotationLevel;
import io.github.mzmine.util.ParsingUtils;

public class LipidFragmentationRule {

  private static final String XML_ELEMENT = "lipidfragmentationrule";
  private static final String XML_POLARITY_TYPE = "polaritytype";
  private static final String XML_IONIZATION_TYPE = "ionizationtype";
  private static final String XML_LIPID_FRAGMENTATION_RULE_TYPE = "lipidFragmentationRuleType";
  private static final String XML_LIPID_ANNOTAION_LEVEL = "lipidannotationlevel";
  private static final String XML_LIPID_FORMULA = "molecularformula";

  private PolarityType polarityType;
  private IonizationType ionizationType;
  private LipidFragmentationRuleType lipidFragmentationRuleType;
  private LipidAnnotationLevel lipidFragmentInformationLevelType;
  private String molecularFormula;

  public LipidFragmentationRule(PolarityType polarityType, IonizationType ionizationType) {
    this.polarityType = polarityType;
    this.ionizationType = ionizationType;
    this.molecularFormula = "";
  }

  public LipidFragmentationRule(PolarityType polarityType, IonizationType ionizationType,
      LipidFragmentationRuleType lipidFragmentationRuleType,
      LipidAnnotationLevel lipidFragmentInformationLevelType) {
    this.polarityType = polarityType;
    this.ionizationType = ionizationType;
    this.lipidFragmentationRuleType = lipidFragmentationRuleType;
    this.lipidFragmentInformationLevelType = lipidFragmentInformationLevelType;
    this.molecularFormula = "";
  }

  public LipidFragmentationRule(PolarityType polarityType, IonizationType ionizationType,
      LipidFragmentationRuleType lipidFragmentationRuleType,
      LipidAnnotationLevel lipidFragmentInformationLevelType, String molecularFormula) {
    this.polarityType = polarityType;
    this.ionizationType = ionizationType;
    this.lipidFragmentationRuleType = lipidFragmentationRuleType;
    this.lipidFragmentInformationLevelType = lipidFragmentInformationLevelType;
    this.molecularFormula = molecularFormula;
  }

  public PolarityType getPolarityType() {
    return polarityType;
  }

  public IonizationType getIonizationType() {
    return ionizationType;
  }

  public LipidFragmentationRuleType getLipidFragmentationRuleType() {
    return lipidFragmentationRuleType;
  }

  public LipidAnnotationLevel getLipidFragmentInformationLevelType() {
    return lipidFragmentInformationLevelType;
  }

  public String getMolecularFormula() {
    return molecularFormula;
  }

  @Override
  public String toString() {
    if (lipidFragmentationRuleType != null) {
      return ionizationType + ", " + lipidFragmentationRuleType + " " + molecularFormula;
    } else {
      return ionizationType.getAdductName();
    }
  }

  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);

    writer.writeStartElement(XML_POLARITY_TYPE);
    writer.writeCharacters(polarityType.name());
    writer.writeEndElement();
    writer.writeStartElement(XML_IONIZATION_TYPE);
    writer.writeCharacters(ionizationType.name());
    writer.writeEndElement();
    writer.writeStartElement(XML_LIPID_FRAGMENTATION_RULE_TYPE);
    writer.writeCharacters(lipidFragmentationRuleType.name());
    writer.writeEndElement();
    writer.writeStartElement(XML_LIPID_ANNOTAION_LEVEL);
    writer.writeCharacters(lipidFragmentInformationLevelType.name());
    writer.writeEndElement();
    writer.writeStartElement(XML_LIPID_FORMULA);
    if (molecularFormula != null) {
      writer.writeCharacters(molecularFormula);
    } else {
      writer.writeCharacters(CONST.XML_NULL_VALUE);
    }
    writer.writeEndElement();
    writer.writeEndElement();

  }

  public static LipidFragmentationRule loadFromXML(XMLStreamReader reader)
      throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      throw new IllegalStateException(
          "Cannot load lipid fragmentation rule from the current element. Wrong name.");
    }

    PolarityType polarityType = null;
    IonizationType ionizationType = null;
    LipidFragmentationRuleType lipidFragmentationRuleType = null;
    LipidAnnotationLevel lipidFragmentInformationLevelType = null;
    String molecularFormula = null;
    while (reader.hasNext()
        && !(reader.isEndElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      switch (reader.getLocalName()) {
        case XML_POLARITY_TYPE:
          polarityType = ParsingUtils.polarityNameToPolarityType(reader.getElementText());
          break;
        case XML_IONIZATION_TYPE:
          ionizationType = ParsingUtils.ionizationNameToIonizationType(reader.getElementText());
          break;
        case XML_LIPID_FRAGMENTATION_RULE_TYPE:
          lipidFragmentationRuleType = LipidParsingUtils
              .lipidFragmentationRuleNameToLipidFragmentationRuleType(reader.getElementText());
          break;
        case XML_LIPID_ANNOTAION_LEVEL:
          lipidFragmentInformationLevelType =
                  LipidParsingUtils.lipidAnnotationLevelNameToLipidAnnotationLevel(reader.getElementText());
          break;
        case XML_LIPID_FORMULA:
          molecularFormula = reader.getElementText();
          break;
        default:
          break;
      }
    }

    if (polarityType != null && ionizationType != null && lipidFragmentationRuleType != null
        && lipidFragmentInformationLevelType != null && molecularFormula != null) {
      return new LipidFragmentationRule(polarityType, ionizationType);
    } else if (polarityType != null && ionizationType != null && lipidFragmentationRuleType != null
        && lipidFragmentInformationLevelType != null) {
      return new LipidFragmentationRule(polarityType, ionizationType, lipidFragmentationRuleType,
          lipidFragmentInformationLevelType);
    } else if (polarityType != null && ionizationType != null) {
      return new LipidFragmentationRule(polarityType, ionizationType);
    } else {
      return null;
    }

  }
}
