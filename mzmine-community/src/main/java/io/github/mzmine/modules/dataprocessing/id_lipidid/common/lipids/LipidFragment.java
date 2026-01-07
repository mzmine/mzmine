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

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRuleRating;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.utils.LipidParsingUtils;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import java.util.Collection;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

public class LipidFragment {

  private static final String XML_ELEMENT = "lipidfragment";
  private static final String XML_LIPID_FRAGMENTATION_RULE_TYPE = "ruletype";
  private static final String XML_LIPID_FRAGMENTATION_RULE_RATING = "lipidFragmentationRuleRating";
  private static final String XML_LIPID_ANNOTAION_LEVEL = "lipidannotationlevel";
  private static final String XML_MZ_EXACT = "mzexact";
  private static final String XML_ION_FORMULA = "ionFormula";
  private static final String XML_DATA_POINT_INTENSITY = "datapointintensity";
  private static final String XML_DATA_POINT_MZ = "datapointmz";
  private static final String XML_LIPID_CLASS = "lipidclass";
  private static final String XML_CHAIN_LENGTH = "chainlength";
  private static final String XML_NUMBER_OF_DBES = "numberofdbes";
  private static final String XML_NUMBER_OF_OXYGENS = "numberofoxygens";
  private static final String XML_LIPID_CHAIN_TYPE = "lipidchaintype";

  private final LipidFragmentationRuleType ruleType;
  private final LipidAnnotationLevel lipidFragmentInformationLevelType;
  private final LipidFragmentationRuleRating lipidFragmentationRuleRating;
  private final Double mzExact;
  private final String ionFormula;
  private final DataPoint dataPoint;
  private final ILipidClass lipidClass;
  private final Integer chainLength;
  private final Integer numberOfDBEs;
  private final Integer numberOfOxygens;
  private final LipidChainType lipidChainType;
  private final Scan msMsScan;

  public LipidFragment(LipidFragmentationRuleType ruleType,
      LipidAnnotationLevel lipidFragmentInformationLevelType,
      LipidFragmentationRuleRating lipidFragmentationRuleRating, Double mzExact, String ionFormula,
      DataPoint dataPoint, ILipidClass lipidClass, Integer chainLength, Integer numberOfDBEs,
      Integer numberOfOxygens, LipidChainType lipidChainType, Scan msMsScan) {
    this.ruleType = ruleType;
    this.lipidFragmentInformationLevelType = lipidFragmentInformationLevelType;
    this.lipidFragmentationRuleRating = lipidFragmentationRuleRating;
    this.mzExact = mzExact;
    this.ionFormula = ionFormula;
    this.dataPoint = dataPoint;
    this.lipidClass = lipidClass;
    this.chainLength = chainLength;
    this.numberOfDBEs = numberOfDBEs;
    this.numberOfOxygens = numberOfOxygens;
    this.lipidChainType = lipidChainType;
    this.msMsScan = msMsScan;
  }

  public LipidFragmentationRuleType getRuleType() {
    return ruleType;
  }

  public LipidAnnotationLevel getLipidFragmentInformationLevelType() {
    return lipidFragmentInformationLevelType;
  }

  public LipidFragmentationRuleRating getLipidFragmentationRuleRating() {
    return lipidFragmentationRuleRating;
  }

  public Double getMzExact() {
    return mzExact;
  }

  public String getIonFormula() {
    return ionFormula;
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

  public Integer getNumberOfOxygens() {
    return numberOfOxygens;
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
    writer.writeStartElement(XML_LIPID_FRAGMENTATION_RULE_RATING);
    writer.writeCharacters(lipidFragmentationRuleRating.name());
    writer.writeEndElement();
    writer.writeStartElement(XML_LIPID_ANNOTAION_LEVEL);
    writer.writeCharacters(lipidFragmentInformationLevelType.name());
    writer.writeEndElement();
    writer.writeStartElement(XML_MZ_EXACT);
    writer.writeCharacters(mzExact.toString());
    writer.writeEndElement();
    writer.writeStartElement(XML_ION_FORMULA);
    writer.writeCharacters(ionFormula);
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
    writer.writeStartElement(XML_NUMBER_OF_OXYGENS);
    if (numberOfOxygens != null) {
      writer.writeCharacters(numberOfOxygens.toString());
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
    LipidFragmentationRuleRating rating = null;
    LipidAnnotationLevel lipidFragmentInformationLevelType = null;
    Double mzExact = null;
    String ionFormula = null;
    Double intensity = null;
    Double mz = null;
    ILipidClass lipidClass = null;
    Integer chainLength = null;
    Integer numberOfDBEs = null;
    Integer numberOfOxygens = null;
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
          ruleType = LipidParsingUtils.lipidFragmentationRuleNameToLipidFragmentationRuleType(
              reader.getElementText());
          break;
        case XML_LIPID_FRAGMENTATION_RULE_RATING:
          rating = LipidParsingUtils.lipidFragmentationRuleNameToLipidFragmentationRuleRaiting(
              reader.getElementText());
          break;
        case XML_LIPID_ANNOTAION_LEVEL:
          lipidFragmentInformationLevelType = LipidParsingUtils.lipidAnnotationLevelNameToLipidAnnotationLevel(
              reader.getElementText());
          break;
        case XML_MZ_EXACT:
          mzExact = Double.parseDouble(reader.getElementText());
          break;
        case XML_ION_FORMULA:
          ionFormula = reader.getElementText();
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
          if (lipidFragmentInformationLevelType != null && lipidFragmentInformationLevelType.equals(
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL)) {
            chainLength = Integer.parseInt(reader.getElementText());
          }
          break;
        case XML_NUMBER_OF_DBES:
          if (lipidFragmentInformationLevelType != null && lipidFragmentInformationLevelType.equals(
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL)) {
            numberOfDBEs = Integer.parseInt(reader.getElementText());
          }
          break;
        case XML_NUMBER_OF_OXYGENS:
          if (lipidFragmentInformationLevelType != null && lipidFragmentInformationLevelType.equals(
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL)) {
            numberOfOxygens = Integer.parseInt(reader.getElementText());
          }
          break;
        case XML_LIPID_CHAIN_TYPE:
          if (lipidFragmentInformationLevelType != null && lipidFragmentInformationLevelType.equals(
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL)) {
            lipidChainType = LipidParsingUtils.lipidChainTypeNameToLipidChainType(
                reader.getElementText());
          }
          break;
        case CONST.XML_RAW_FILE_SCAN_ELEMENT:
          msMsScan = Scan.loadScanFromXML(reader, possibleFiles);
          break;
        default:
          break;
      }
    }

    return new LipidFragment(ruleType, lipidFragmentInformationLevelType, rating, mzExact,
        ionFormula, new SimpleDataPoint(mz, intensity), lipidClass, chainLength, numberOfDBEs,
        numberOfOxygens, lipidChainType, msMsScan);
  }

}
