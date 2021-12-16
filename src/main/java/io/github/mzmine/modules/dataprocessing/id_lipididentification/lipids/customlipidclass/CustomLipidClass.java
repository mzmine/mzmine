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

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.customlipidclass;

import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidParsingUtils;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidChainType;

public class CustomLipidClass implements ILipidClass {

  private static final String XML_ELEMENT = "lipidclass";
  private static final String XML_LIPID_CLASS_NAME = "lipidclassname";
  private static final String XML_LIPID_CLASS_ABBR = "lipidclassabbr";
  private static final String XML_LIPID_CLASS_BACKBONE_FORMULA = "lipidclassbackboneformula";
  private static final String XML_LIPID_CLASS_CHAIN_TYPE = "lipidclasschaintypes";
  private static final String XML_LIPID_CLASS_FRAGMENTATION_RULES = "lipidclassfragmentationrules";


  private String name;
  private String abbr;
  private String backBoneFormula;
  private LipidChainType[] chainTypes;
  private LipidFragmentationRule[] fragmentationRules;

  public CustomLipidClass(String name, String abbr, String backBoneFormula,
      LipidChainType[] chainTypes, LipidFragmentationRule[] fragmentationRules) {
    this.name = name;
    this.abbr = abbr;
    this.backBoneFormula = backBoneFormula;
    this.chainTypes = chainTypes;
    this.fragmentationRules = fragmentationRules;
  }

  public String getName() {
    return name;
  }

  public String getAbbr() {
    return abbr;
  }

  public String getBackBoneFormula() {
    return backBoneFormula;
  }

  public LipidChainType[] getChainTypes() {
    return chainTypes;
  }

  public LipidFragmentationRule[] getFragmentationRules() {
    return fragmentationRules;
  }

  @Override
  public String toString() {
    return this.abbr + " " + this.name;
  }

  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);
    writer.writeAttribute(XML_ELEMENT, CustomLipidClass.class.getSimpleName());
    writer.writeStartElement(XML_LIPID_CLASS_NAME);
    writer.writeCharacters(name);
    writer.writeEndElement();
    writer.writeStartElement(XML_LIPID_CLASS_ABBR);
    writer.writeCharacters(abbr);
    writer.writeEndElement();
    writer.writeStartElement(XML_LIPID_CLASS_BACKBONE_FORMULA);
    writer.writeCharacters(backBoneFormula);
    writer.writeEndElement();
    writer.writeStartElement(XML_LIPID_CLASS_CHAIN_TYPE);
    if (chainTypes != null) {
      for (LipidChainType lipidChainType : chainTypes) {
        writer.writeCharacters(lipidChainType.getName());
      }
    } else {
      writer.writeCharacters(CONST.XML_NULL_VALUE);
    }
    writer.writeEndElement();
    writer.writeStartElement(XML_LIPID_CLASS_FRAGMENTATION_RULES);
    if (fragmentationRules != null) {
      for (LipidFragmentationRule lipidFragmentationRule : fragmentationRules) {
        lipidFragmentationRule.saveToXML(writer);
      }
    } else {
      writer.writeCharacters(CONST.XML_NULL_VALUE);
    }
    writer.writeEndElement();

    writer.writeEndElement();

  }

  public static ILipidClass loadFromXML(XMLStreamReader reader) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      throw new IllegalStateException(
          "Cannot load lipid class from the current element. Wrong name.");
    }

    String name = null;
    String abbr = null;
    String backBoneFormula = null;
    LipidChainType[] chainTypes = null;
    LipidFragmentationRule[] fragmentationRules = null;
    while (reader.hasNext()
        && !(reader.isEndElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      if (reader.getLocalName().equals(XML_LIPID_CLASS_NAME)) {
        return LipidParsingUtils.lipidClassNameToLipidClass(reader.getElementText());
      }

      switch (reader.getLocalName()) {
        case XML_LIPID_CLASS_NAME:
          break;
        case XML_LIPID_CLASS_ABBR:
          abbr = reader.getElementText();
          break;
        case XML_LIPID_CLASS_BACKBONE_FORMULA:
          backBoneFormula = reader.getElementText();
          break;
        case XML_LIPID_CLASS_CHAIN_TYPE:
          chainTypes = loadLipidChainTypesFromXML(reader);
          break;
        case XML_LIPID_CLASS_FRAGMENTATION_RULES:
          fragmentationRules = loadLipidFragmentationRulesFromXML(reader);
          break;
        default:
          break;
      }
    }
    return new CustomLipidClass(name, abbr, backBoneFormula, chainTypes, fragmentationRules);
  }

  private static LipidChainType[] loadLipidChainTypesFromXML(XMLStreamReader reader)
      throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_LIPID_CLASS_CHAIN_TYPE))) {
      throw new IllegalStateException(
          "Cannot load matched lipid fragments from the current element. Wrong name.");
    }

    List<LipidChainType> lipidChainTypes = new ArrayList<>();
    while (reader.hasNext()
        && !(reader.isEndElement() && reader.getLocalName().equals(XML_LIPID_CLASS_CHAIN_TYPE))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }
      lipidChainTypes.add(LipidParsingUtils.lipidChainTypeNameToLipidChainType(reader.getElementText()));
    }

    return lipidChainTypes.toArray(new LipidChainType[0]);
  }

  private static LipidFragmentationRule[] loadLipidFragmentationRulesFromXML(XMLStreamReader reader)
      throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_LIPID_CLASS_CHAIN_TYPE))) {
      throw new IllegalStateException(
          "Cannot load matched lipid fragments from the current element. Wrong name.");
    }

    List<LipidFragmentationRule> lipidFragmentationRules = new ArrayList<>();
    while (reader.hasNext()
        && !(reader.isEndElement() && reader.getLocalName().equals(XML_LIPID_CLASS_CHAIN_TYPE))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }
      lipidFragmentationRules.add(LipidFragmentationRule.loadFromXML(reader));
    }

    return lipidFragmentationRules.toArray(new LipidFragmentationRule[0]);
  }

}
