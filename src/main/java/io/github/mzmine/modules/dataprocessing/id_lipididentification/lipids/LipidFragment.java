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

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids;

import java.util.Collection;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.apache.commons.lang.StringUtils;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.customlipidclass.CustomLipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidChainType;
import io.github.mzmine.util.ParsingUtils;

public class LipidFragment {

  private static final String XML_ELEMENT = "lipidfragment";
  private static final String XML_LIPID_FRAGMENTATION_RULE_TYPE = "ruletype";
  private static final String XML_LIPID_ANNOTAION_LEVEL = "lipidannotationlevel";
  private static final String XML_MZ_EXACT = "mzexact";
  private static final String XML_LIPID_CLASS = "lipidclass";
  private static final String XML_CHAIN_LENGTH = "chainlength";
  private static final String XML_NUMBER_OF_DBES = "numberofdbes";
  private static final String XML_LIPID_CHAIN_TYPE = "lipidchaintype";
  private static final String XML_MSMS_SCAN_NUMBER = "msmsscannumber";

  private LipidFragmentationRuleType ruleType;
  private LipidAnnotationLevel lipidFragmentInformationLevelType;
  private Double mzExact;
  private DataPoint dataPoint;
  private ILipidClass lipidClass;
  private Integer chainLength;
  private Integer numberOfDBEs;
  private LipidChainType lipidChainType;
  private Scan msMsScan;

  public LipidFragment(LipidFragmentationRuleType ruleType,
      LipidAnnotationLevel lipidFragmentInformationLevelType, Double mzExact, DataPoint dataPoint,
      ILipidClass lipidClass, Integer chainLength, Integer numberOfDBEs,
      LipidChainType lipidChainType, Scan msMsScan) {
    this.ruleType = ruleType;
    this.lipidFragmentInformationLevelType = lipidFragmentInformationLevelType;
    this.mzExact = mzExact;
    this.dataPoint = dataPoint;
    this.lipidClass = lipidClass;
    this.chainLength = chainLength;
    this.numberOfDBEs = numberOfDBEs;
    this.lipidChainType = lipidChainType;
    this.msMsScan = msMsScan;
  }

  public LipidFragmentationRuleType getRuleType() {
    return ruleType;
  }

  public LipidAnnotationLevel getLipidFragmentInformationLevelType() {
    return lipidFragmentInformationLevelType;
  }

  public Double getMzExact() {
    return mzExact;
  }

  public DataPoint getDataPoint() {
    return dataPoint;
  }

  public ILipidClass getLipidClass() {
    return lipidClass;
  }

  public Integer getChainLength() {
    return chainLength;
  }

  public Integer getNumberOfDBEs() {
    return numberOfDBEs;
  }

  public LipidChainType getLipidChainType() {
    return lipidChainType;
  }

  public Scan getMsMsScan() {
    return msMsScan;
  }

  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);
    writer.writeStartElement(XML_LIPID_FRAGMENTATION_RULE_TYPE);
    writer.writeCharacters(ruleType.name());
    writer.writeEndElement();
    writer.writeStartElement(XML_LIPID_ANNOTAION_LEVEL);
    writer.writeCharacters(lipidFragmentInformationLevelType.name());
    writer.writeEndElement();
    writer.writeStartElement(XML_MZ_EXACT);
    writer.writeCharacters(mzExact.toString());
    writer.writeEndElement();
    writer.writeStartElement(XML_LIPID_CLASS);
    lipidClass.saveToXML(writer);
    writer.writeEndElement();
    writer.writeStartElement(XML_CHAIN_LENGTH);
    if (chainLength != null) {
      writer.writeCharacters(chainLength.toString());
    } else {
      writer.writeCharacters(StringUtils.EMPTY);
    }
    writer.writeEndElement();
    writer.writeStartElement(XML_NUMBER_OF_DBES);
    if (numberOfDBEs != null) {
      writer.writeCharacters(numberOfDBEs.toString());
    } else {
      writer.writeCharacters(StringUtils.EMPTY);
    }
    writer.writeEndElement();
    writer.writeStartElement(XML_LIPID_CHAIN_TYPE);
    if (lipidChainType != null) {
      writer.writeCharacters(lipidChainType.getName());
    } else {
      writer.writeCharacters(StringUtils.EMPTY);
    }
    writer.writeEndElement();
    writer.writeStartElement(XML_MSMS_SCAN_NUMBER);
    if (msMsScan != null) {
      Scan.saveScanToXML(writer, msMsScan);
    } else {
      writer.writeCharacters(StringUtils.EMPTY);
    }
    writer.writeEndElement();

    writer.writeEndElement();

  }

  public static LipidFragment loadFromXML(XMLStreamReader reader,
      Collection<RawDataFile> possibleFiles) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      throw new IllegalStateException(
          "Cannot load lipid fragmentation rule from the current element. Wrong name.");
    }

    LipidFragmentationRuleType ruleType = null;
    LipidAnnotationLevel lipidFragmentInformationLevelType = null;
    Double mzExact = null;
    DataPoint dataPoint = null;
    ILipidClass lipidClass = null;
    Integer chainLength = null;
    Integer numberOfDBEs = null;
    LipidChainType lipidChainType = null;
    Scan msMsScan = null;

    while (reader.hasNext()
        && !(reader.isEndElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      switch (reader.getLocalName()) {
        case XML_LIPID_FRAGMENTATION_RULE_TYPE:
          ruleType = ParsingUtils
              .lipidFragmentationRuleNameToLipidFragmentationRuleType(reader.getElementText());
          break;
        case XML_LIPID_ANNOTAION_LEVEL:
          lipidFragmentInformationLevelType =
              ParsingUtils.lipidAnnotationLevelNameToLipidAnnotationLevel(reader.getElementText());
          break;
        case XML_MZ_EXACT:
          mzExact = Double.parseDouble(reader.getElementText());
          break;
        case XML_LIPID_CLASS:
          lipidClass = LipidClasses.loadFromXML(reader);
          if (lipidClass == null) {
            lipidClass = CustomLipidClass.loadFromXML(reader);
          }
          break;
        case XML_CHAIN_LENGTH:
          chainLength = Integer.parseInt(reader.getElementText());
          break;
        case XML_NUMBER_OF_DBES:
          numberOfDBEs = Integer.parseInt(reader.getElementText());
          break;
        case XML_LIPID_CHAIN_TYPE:
          lipidChainType = ParsingUtils.lipidChainTypeNameToLipidChainType(reader.getElementText());
          break;
        case XML_MSMS_SCAN_NUMBER:
          msMsScan = Scan.loadScanFromXML(reader, possibleFiles);
          break;
        default:
          break;
      }
    }

    return new LipidFragment(ruleType, lipidFragmentInformationLevelType, mzExact, dataPoint,
        lipidClass, chainLength, numberOfDBEs, lipidChainType, msMsScan);
  }

}
