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

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.species_level;

import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.utils.LipidParsingUtils;
import io.github.mzmine.util.FormulaUtils;
import java.util.Objects;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class SpeciesLevelAnnotation implements ILipidAnnotation {

  private static final String XML_ELEMENT = "lipidannotation";
  private static final String XML_LIPID_CLASS = "lipidclass";
  private static final String XML_NAME = "name";
  private static final String XML_LIPID_ANNOTAION_LEVEL = "lipidannotationlevel";
  private static final String XML_LIPID_FORMULA = "molecularformula";
  private static final String XML_NUMBER_OF_CARBONS = "numberOfCarbons";
  private static final String XML_NUMBER_OF_DBES = "numberofdbes";
  private static final String XML_NUMBER_OF_OXYGENS = "numberofoxygens";

  private ILipidClass lipidClass;
  private String annotation;
  private static final LipidAnnotationLevel LIPID_ANNOTATION_LEVEL = LipidAnnotationLevel.SPECIES_LEVEL;
  private final IMolecularFormula molecularFormula;
  private final int numberOfCarbons;
  private final int numberOfDBEs;
  private final int numberOfOxygens;

  public SpeciesLevelAnnotation(ILipidClass lipidClass, String annotation,
      IMolecularFormula molecularFormula, int numberOfCarbons, int numberOfDBEs,
      int numberOfOxygens) {
    this.lipidClass = lipidClass;
    this.annotation = annotation;
    this.molecularFormula = molecularFormula;
    this.numberOfCarbons = numberOfCarbons;
    this.numberOfDBEs = numberOfDBEs;
    this.numberOfOxygens = numberOfOxygens;
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

  public int getNumberOfCarbons() {
    return numberOfCarbons;
  }

  public int getNumberOfDBEs() {
    return numberOfDBEs;
  }

  public int getNumberOfOxygens() {
    return numberOfOxygens;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SpeciesLevelAnnotation that = (SpeciesLevelAnnotation) o;
    return numberOfCarbons == that.numberOfCarbons && numberOfDBEs == that.numberOfDBEs
        && numberOfOxygens == that.numberOfOxygens && Objects.equals(lipidClass, that.lipidClass)
        && Objects.equals(annotation, that.annotation) && Objects.equals(molecularFormula,
        that.molecularFormula);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lipidClass, annotation, molecularFormula, numberOfCarbons, numberOfDBEs,
        numberOfOxygens);
  }

  @Override

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
    writer.writeStartElement(XML_NUMBER_OF_CARBONS);
    writer.writeCharacters(String.valueOf(numberOfCarbons));
    writer.writeEndElement();
    writer.writeStartElement(XML_NUMBER_OF_DBES);
    writer.writeCharacters(String.valueOf(numberOfDBEs));
    writer.writeEndElement();
    writer.writeStartElement(XML_NUMBER_OF_OXYGENS);
    writer.writeCharacters(String.valueOf(numberOfOxygens));
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
    Integer numberOfCarbons = null;
    Integer numberOfDBEs = null;
    Integer numberOfOxygens = null;
    while (reader.hasNext()
        && !(reader.isEndElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      switch (reader.getLocalName()) {
        case XML_LIPID_CLASS:
          if (reader.getAttributeValue(null, XML_LIPID_CLASS)
              .equals(LipidClasses.class.getSimpleName())) {
            lipidClass = LipidClasses.loadFromXML(reader);
          } else if (reader.getAttributeValue(null, XML_LIPID_CLASS)
              .equals(CustomLipidClass.class.getSimpleName())) {
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
        case XML_NUMBER_OF_CARBONS:
          numberOfCarbons = Integer.parseInt(reader.getElementText());
          break;
        case XML_NUMBER_OF_DBES:
          numberOfDBEs = Integer.parseInt(reader.getElementText());
          break;
        case XML_NUMBER_OF_OXYGENS:
          numberOfOxygens = Integer.parseInt(reader.getElementText());
          break;
        default:
          break;
      }
    }

    if (lipidAnnotationLevel != null
        && lipidAnnotationLevel.equals(LipidAnnotationLevel.SPECIES_LEVEL)) {
      return new SpeciesLevelAnnotation(lipidClass, annotation, molecularFormula, numberOfCarbons,
          numberOfDBEs, numberOfOxygens);
    }
    return null;
  }

}
