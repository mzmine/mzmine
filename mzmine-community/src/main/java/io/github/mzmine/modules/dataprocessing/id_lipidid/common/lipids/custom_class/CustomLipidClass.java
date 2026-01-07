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

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class;

import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidCategories;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidMainClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.utils.LipidParsingUtils;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

public class CustomLipidClass implements ILipidClass {

  private static final String XML_ELEMENT = "lipidclass";
  private static final String XML_LIPID_CLASS_NAME = "lipidclassname";
  private static final String XML_LIPID_CLASS_ABBR = "lipidclassabbr";
  private static final String XML_LIPID_CATEGORY = "lipidcategory";
  private static final String XML_LIPID_MAIN_CLASS = "lipidmainclass";
  private static final String XML_LIPID_CLASS_BACKBONE_FORMULA = "lipidclassbackboneformula";
  private static final String XML_LIPID_CLASS_CHAIN_TYPE = "lipidclasschaintypes";
  private static final String XML_LIPID_CLASS_FRAGMENTATION_RULES = "lipidclassfragmentationrules";


  private final String name;
  private final String abbr;
  private final LipidCategories coreClass;
  private final LipidMainClasses mainClass;
  private final String backBoneFormula;
  private final LipidChainType[] chainTypes;
  private final LipidFragmentationRule[] fragmentationRules;

  public CustomLipidClass(String name, String abbr, LipidCategories coreClass,
      LipidMainClasses mainClass, String backBoneFormula, LipidChainType[] chainTypes,
      LipidFragmentationRule[] fragmentationRules) {
    this.name = name;
    this.abbr = abbr;
    this.coreClass = coreClass;
    this.mainClass = mainClass;
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

  @Override
  public LipidCategories getCoreClass() {
    return coreClass;
  }

  @Override
  public LipidMainClasses getMainClass() {
    return mainClass;
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
    writer.writeStartElement(XML_LIPID_CATEGORY);
    writer.writeCharacters(coreClass.getName());
    writer.writeEndElement();
    writer.writeStartElement(XML_LIPID_CATEGORY);
    writer.writeCharacters(mainClass.getName());
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
    LipidCategories lipidCategory = null;
    LipidMainClasses lipidMainClass = null;
    LipidChainType[] chainTypes = null;
    LipidFragmentationRule[] fragmentationRules = null;
    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(XML_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      if (reader.getLocalName().equals(XML_LIPID_CLASS_NAME)) {
        return LipidParsingUtils.lipidClassNameToLipidClass(reader.getElementText());
      }

      switch (reader.getLocalName()) {
        case XML_LIPID_CLASS_NAME -> name = reader.getElementText();
        case XML_LIPID_CLASS_ABBR -> abbr = reader.getElementText();
        case XML_LIPID_CATEGORY ->
            lipidCategory = LipidParsingUtils.lipidCategoryNameToLipidLipidCategory(
                reader.getElementText());
        case XML_LIPID_MAIN_CLASS ->
            lipidMainClass = LipidParsingUtils.lipidMainClassNameToLipidLipidMainClass(
                reader.getElementText());
        case XML_LIPID_CLASS_BACKBONE_FORMULA -> backBoneFormula = reader.getElementText();
        case XML_LIPID_CLASS_CHAIN_TYPE -> chainTypes = loadLipidChainTypesFromXML(reader);
        case XML_LIPID_CLASS_FRAGMENTATION_RULES ->
            fragmentationRules = loadLipidFragmentationRulesFromXML(reader);
        default -> {
        }
      }
    }
    return new CustomLipidClass(name, abbr, lipidCategory, lipidMainClass, backBoneFormula,
        chainTypes, fragmentationRules);
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
