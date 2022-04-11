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

import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidParsingUtils;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.customlipidclass.CustomLipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidChainType;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;

public class LipidFragment {

  private static final String XML_ELEMENT = "lipidfragment";
  private static final String XML_LIPID_FRAGMENTATION_RULE_TYPE = "ruletype";
  private static final String XML_LIPID_ANNOTAION_LEVEL = "lipidannotationlevel";
  private static final String XML_MZ_EXACT = "mzexact";
  private static final String XML_DATA_POINT_INTENSITY = "datapointintensity";
  private static final String XML_DATA_POINT_MZ = "datapointmz";
  private static final String XML_LIPID_CLASS = "lipidclass";
  private static final String XML_CHAIN_LENGTH = "chainlength";
  private static final String XML_NUMBER_OF_DBES = "numberofdbes";
  private static final String XML_LIPID_CHAIN_TYPE = "lipidchaintype";

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
    writer.writeAttribute(XML_ELEMENT, lipidFragmentInformationLevelType.name());
    writer.writeStartElement(XML_LIPID_FRAGMENTATION_RULE_TYPE);
    writer.writeCharacters(ruleType.name());
    writer.writeEndElement();
    writer.writeStartElement(XML_LIPID_ANNOTAION_LEVEL);
    writer.writeCharacters(lipidFragmentInformationLevelType.name());
    writer.writeEndElement();
    writer.writeStartElement(XML_MZ_EXACT);
    writer.writeCharacters(mzExact.toString());
    writer.writeEndElement();
    writer.writeStartElement(XML_DATA_POINT_INTENSITY);
    writer.writeCharacters(String.valueOf(dataPoint.getIntensity()));
    writer.writeEndElement();
    writer.writeStartElement(XML_DATA_POINT_MZ);
    writer.writeCharacters(String.valueOf(dataPoint.getMZ()));
    writer.writeEndElement();

    lipidClass.saveToXML(writer);
    writer.writeStartElement(XML_CHAIN_LENGTH);
    if (chainLength != null) {
      writer.writeCharacters(chainLength.toString());
    } else {
      writer.writeCharacters(CONST.XML_NULL_VALUE);
    }
    writer.writeEndElement();
    writer.writeStartElement(XML_NUMBER_OF_DBES);
    if (numberOfDBEs != null) {
      writer.writeCharacters(numberOfDBEs.toString());
    } else {
      writer.writeCharacters(CONST.XML_NULL_VALUE);
    }
    writer.writeEndElement();
    writer.writeStartElement(XML_LIPID_CHAIN_TYPE);
    if (lipidChainType != null) {
      writer.writeCharacters(lipidChainType.name());
    } else {
      writer.writeCharacters(CONST.XML_NULL_VALUE);
    }
    writer.writeEndElement();
    if (msMsScan != null) {
      Scan.saveScanToXML(writer, msMsScan);
    }

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
    Double intensity = null;
    Double mz = null;
    ILipidClass lipidClass = null;
    Integer chainLength = null;
    Integer numberOfDBEs = null;
    LipidChainType lipidChainType = null;
    Scan msMsScan = null;

    if (reader.getAttributeValue(null, XML_ELEMENT)
        .equals(LipidAnnotationLevel.SPECIES_LEVEL.name())) {
      lipidFragmentInformationLevelType = LipidAnnotationLevel.SPECIES_LEVEL;
    } else if (reader.getAttributeValue(null, XML_ELEMENT)
        .equals(LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL.name())) {
      lipidFragmentInformationLevelType = LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL;
    }
    while (reader.hasNext()
        && !(reader.isEndElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      switch (reader.getLocalName()) {
        case XML_LIPID_FRAGMENTATION_RULE_TYPE:
          ruleType = LipidParsingUtils
              .lipidFragmentationRuleNameToLipidFragmentationRuleType(reader.getElementText());
          break;
        case XML_LIPID_ANNOTAION_LEVEL:
          lipidFragmentInformationLevelType =
                  LipidParsingUtils.lipidAnnotationLevelNameToLipidAnnotationLevel(reader.getElementText());
          break;
        case XML_MZ_EXACT:
          mzExact = Double.parseDouble(reader.getElementText());
          break;
        case XML_DATA_POINT_INTENSITY:
          intensity = Double.parseDouble(reader.getElementText());
          break;
        case XML_DATA_POINT_MZ:
          mz = Double.parseDouble(reader.getElementText());
          break;
        case XML_LIPID_CLASS:
          if (reader.getAttributeValue(null, XML_LIPID_CLASS)
              .equals(LipidClasses.class.getSimpleName())) {
            lipidClass = LipidClasses.loadFromXML(reader);
          } else if (reader.getAttributeValue(null, XML_LIPID_CLASS)
              .equals(CustomLipidClass.class.getSimpleName())) {
            lipidClass = CustomLipidClass.loadFromXML(reader);
          }
          break;
        case XML_CHAIN_LENGTH:
          if (lipidFragmentInformationLevelType != null && lipidFragmentInformationLevelType
              .equals(LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL)) {
            chainLength = Integer.parseInt(reader.getElementText());
          }
          break;
        case XML_NUMBER_OF_DBES:
          if (lipidFragmentInformationLevelType != null&&lipidFragmentInformationLevelType
              .equals(LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL)) {
            numberOfDBEs = Integer.parseInt(reader.getElementText());
          }
          break;
        case XML_LIPID_CHAIN_TYPE:
          if (lipidFragmentInformationLevelType != null && lipidFragmentInformationLevelType
              .equals(LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL)) {
            lipidChainType =
                    LipidParsingUtils.lipidChainTypeNameToLipidChainType(reader.getElementText());
          }
          break;
        case CONST.XML_RAW_FILE_SCAN_ELEMENT:
          msMsScan = Scan.loadScanFromXML(reader, possibleFiles);
          break;
        default:
          break;
      }
    }

    return new LipidFragment(ruleType, lipidFragmentInformationLevelType, mzExact,
        new SimpleDataPoint(mz, intensity), lipidClass, chainLength, numberOfDBEs, lipidChainType,
        msMsScan);
  }

}
