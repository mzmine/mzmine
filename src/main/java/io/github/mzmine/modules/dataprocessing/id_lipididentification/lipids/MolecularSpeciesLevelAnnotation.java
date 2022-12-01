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

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids;

import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidParsingUtils;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.customlipidclass.CustomLipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.lipidchain.AcylLipidChain;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.lipidchain.AlkylLipidChain;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.lipidchain.ILipidChain;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidChainType;
import io.github.mzmine.util.FormulaUtils;

public class MolecularSpeciesLevelAnnotation implements ILipidAnnotation {

  private static final String XML_ELEMENT = "lipidannotation";
  private static final String XML_LIPID_CLASS = "lipidclass";
  private static final String XML_NAME = "name";
  private static final String XML_LIPID_ANNOTAION_LEVEL = "lipidannotationlevel";
  private static final String XML_LIPID_FORMULA = "molecularformula";
  private static final String XML_LIPID_CHAINS = "lipidchains";

  private ILipidClass lipidClass;
  private String annotation;
  private static final LipidAnnotationLevel LIPID_ANNOTATION_LEVEL =
      LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL;
  private IMolecularFormula molecularFormula;
  private List<ILipidChain> lipidChains;

  public MolecularSpeciesLevelAnnotation(ILipidClass lipidClass, String annotation,
      IMolecularFormula molecularFormula, List<ILipidChain> lipidChains) {
    this.lipidClass = lipidClass;
    this.annotation = annotation;
    this.molecularFormula = molecularFormula;
    this.lipidChains = lipidChains;
  }

  @Override
  public ILipidClass getLipidClass() {
    return lipidClass;
  }

  @Override
  public void setLipidClass(ILipidClass lipidClass) {
    this.lipidClass = lipidClass;
  }

  @Override
  public String getAnnotation() {
    return annotation;
  }

  @Override
  public void setAnnotation(String annotation) {
    this.annotation = annotation;
  }

  @Override
  public LipidAnnotationLevel getLipidAnnotationLevel() {
    return LIPID_ANNOTATION_LEVEL;
  }

  @Override
  public IMolecularFormula getMolecularFormula() {
    return molecularFormula;
  }

  public List<ILipidChain> getLipidChains() {
    return lipidChains;
  }

  public void setLipidChains(List<ILipidChain> lipidChains) {
    this.lipidChains = lipidChains;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((annotation == null) ? 0 : annotation.hashCode());
    result = prime * result + ((lipidChains == null) ? 0 : lipidChains.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MolecularSpeciesLevelAnnotation other = (MolecularSpeciesLevelAnnotation) obj;
    if (annotation == null) {
      if (other.annotation != null)
        return false;
    } else if (!annotation.equals(other.annotation))
      return false;
    if (lipidChains == null) {
      if (other.lipidChains != null)
        return false;
    } else if (!lipidChains.equals(other.lipidChains))
      return false;
    return true;
  }

  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);
    writer.writeAttribute(XML_ELEMENT, LIPID_ANNOTATION_LEVEL.name());
    lipidClass.saveToXML(writer);
    writer.writeStartElement(XML_NAME);
    writer.writeCharacters(annotation);
    writer.writeEndElement();
    writer.writeStartElement(XML_LIPID_ANNOTAION_LEVEL);
    writer.writeCharacters(LIPID_ANNOTATION_LEVEL.name());
    writer.writeEndElement();
    writer.writeStartElement(XML_LIPID_FORMULA);
    writer.writeCharacters(MolecularFormulaManipulator.getString(molecularFormula));
    writer.writeEndElement();
    writer.writeStartElement(XML_LIPID_CHAINS);
    if (lipidChains != null) {
      for (ILipidChain lipidChain : lipidChains) {
        lipidChain.saveToXML(writer);
      }
    } else {
      writer.writeCharacters(CONST.XML_NULL_VALUE);
    }
    writer.writeEndElement();
    writer.writeEndElement();

  }

  public static ILipidAnnotation loadFromXML(XMLStreamReader reader) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      throw new IllegalStateException(
          "Cannot load lipid class from the current element. Wrong name.");
    }

    ILipidClass lipidClass = null;
    String annotation = null;
    LipidAnnotationLevel lipidAnnotationLevel = null;
    IMolecularFormula molecularFormula = null;
    List<ILipidChain> lipidChains = null;
    while (reader.hasNext()
        && !(reader.isEndElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      switch (reader.getLocalName()) {
        case XML_LIPID_CLASS:
          lipidClass = LipidClasses.loadFromXML(reader);
          if (lipidClass == null) {
            lipidClass = CustomLipidClass.loadFromXML(reader);
          }
          break;
        case XML_NAME:
          annotation = reader.getElementText();
          break;
        case XML_LIPID_ANNOTAION_LEVEL:
          lipidAnnotationLevel =
                  LipidParsingUtils.lipidAnnotationLevelNameToLipidAnnotationLevel(reader.getElementText());
          break;
        case XML_LIPID_FORMULA:
          molecularFormula = FormulaUtils.createMajorIsotopeMolFormula(reader.getElementText());
          break;
        case XML_LIPID_CHAINS:
          lipidChains = loadLipidChainsFromXML(reader);
          break;
        default:
          break;
      }
    }
    if (lipidAnnotationLevel != null
        && lipidAnnotationLevel.equals(LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL)) {
      return new MolecularSpeciesLevelAnnotation(lipidClass, annotation, molecularFormula,
          lipidChains);
    }
    return null;
  }

  private static List<ILipidChain> loadLipidChainsFromXML(XMLStreamReader reader)
      throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_LIPID_CHAINS))) {
      throw new IllegalStateException(
          "Cannot load lipid chains from the current element. Wrong name.");
    }

    List<ILipidChain> lipidChains = new ArrayList<>();
    while (reader.hasNext()
        && !(reader.isEndElement() && reader.getLocalName().equals(XML_LIPID_CHAINS))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }
      if (reader.getLocalName().equals(LipidChainType.ACYL_CHAIN.name())) {
        lipidChains.add(AcylLipidChain.loadFromXML(reader));
      } else if (reader.getLocalName().equals(LipidChainType.ALKYL_CHAIN.name())) {
        lipidChains.add(AlkylLipidChain.loadFromXML(reader));
      }

    }
    return lipidChains;
  }

}
