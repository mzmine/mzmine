/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipidid.utils.LipidParsingUtils;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.ParsingUtils;
import static io.github.mzmine.util.StringUtils.inQuotes;
import io.mzio.general.Result;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class LipidFragmentationRule {

  private static final String XML_ELEMENT = "lipidfragmentationrule";
  private static final String XML_POLARITY_TYPE = "polaritytype";
  private static final String XML_IONIZATION_TYPE = "ionizationtype";
  private static final String XML_LIPID_FRAGMENTATION_RULE_TYPE = "lipidFragmentationRuleType";
  private static final String XML_LIPID_ANNOTAION_LEVEL = "lipidannotationlevel";
  private static final String XML_LIPID_FORMULA = "molecularformula";
  private static final String XML_LIPID_FRAGMENTATION_RULE_RATING = "lipidFragmentationRuleRating";

  private final PolarityType polarityType;
  private final IonizationType ionizationType;
  private LipidFragmentationRuleType lipidFragmentationRuleType;
  private LipidAnnotationLevel lipidFragmentInformationLevelType;
  private String molecularFormula;
  private LipidFragmentationRuleRating lipidFragmentationRuleRating;


  public LipidFragmentationRule(PolarityType polarityType, IonizationType ionizationType) {
    this.polarityType = polarityType;
    this.ionizationType = ionizationType;
    this.molecularFormula = "";
  }

  public LipidFragmentationRule(PolarityType polarityType, IonizationType ionizationType,
      LipidFragmentationRuleType lipidFragmentationRuleType,
      LipidAnnotationLevel lipidFragmentInformationLevelType) {
    this(polarityType, ionizationType);
    this.lipidFragmentationRuleType = lipidFragmentationRuleType;
    this.lipidFragmentInformationLevelType = lipidFragmentInformationLevelType;
    this.lipidFragmentationRuleRating = LipidFragmentationRuleRating.MAJOR;
  }

  public LipidFragmentationRule(PolarityType polarityType, IonizationType ionizationType,
      LipidFragmentationRuleType lipidFragmentationRuleType,
      LipidAnnotationLevel lipidFragmentInformationLevelType, String molecularFormula) {
    this(polarityType, ionizationType, lipidFragmentationRuleType,
        lipidFragmentInformationLevelType);
    this.molecularFormula = molecularFormula;
    this.lipidFragmentationRuleRating = LipidFragmentationRuleRating.MAJOR;
  }

  public LipidFragmentationRule(PolarityType polarityType, IonizationType ionizationType,
      LipidFragmentationRuleType lipidFragmentationRuleType,
      LipidAnnotationLevel lipidFragmentInformationLevelType,
      LipidFragmentationRuleRating lipidFragmentationRuleRating) {
    this(polarityType, ionizationType, lipidFragmentationRuleType,
        lipidFragmentInformationLevelType);
    this.lipidFragmentationRuleRating = lipidFragmentationRuleRating;
  }

  public LipidFragmentationRule(PolarityType polarityType, IonizationType ionizationType,
      LipidFragmentationRuleType lipidFragmentationRuleType,
      LipidAnnotationLevel lipidFragmentInformationLevelType, String molecularFormula,
      LipidFragmentationRuleRating lipidFragmentationRuleRating) {
    this(polarityType, ionizationType, lipidFragmentationRuleType,
        lipidFragmentInformationLevelType);
    this.molecularFormula = molecularFormula;
    this.lipidFragmentationRuleRating = lipidFragmentationRuleRating;
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

  public LipidFragmentationRuleRating getLipidFragmentationRuleRating() {
    return lipidFragmentationRuleRating;
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
    writer.writeStartElement(XML_LIPID_FRAGMENTATION_RULE_RATING);
    writer.writeCharacters(lipidFragmentationRuleRating.getName());
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
    LipidFragmentationRuleRating lipidFragmentationRuleRating = null;
    LipidAnnotationLevel lipidFragmentInformationLevelType = null;
    String molecularFormula = null;
    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(XML_ELEMENT))) {
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
          lipidFragmentationRuleType = LipidParsingUtils.lipidFragmentationRuleNameToLipidFragmentationRuleType(
              reader.getElementText());
          break;
        case XML_LIPID_FRAGMENTATION_RULE_RATING:
          lipidFragmentationRuleRating = LipidParsingUtils.lipidFragmentationRuleNameToLipidFragmentationRuleRaiting(
              reader.getElementText());
          break;
        case XML_LIPID_ANNOTAION_LEVEL:
          lipidFragmentInformationLevelType = LipidParsingUtils.lipidAnnotationLevelNameToLipidAnnotationLevel(
              reader.getElementText());
          break;
        case XML_LIPID_FORMULA:
          molecularFormula = reader.getElementText();
          break;
        default:
          break;
      }
    }

    if (polarityType != null && ionizationType != null && lipidFragmentationRuleType != null
        && lipidFragmentInformationLevelType != null && molecularFormula != null
        && lipidFragmentationRuleRating != null) {
      return new LipidFragmentationRule(polarityType, ionizationType, lipidFragmentationRuleType,
          lipidFragmentInformationLevelType, molecularFormula, lipidFragmentationRuleRating);
    } else if (polarityType != null && ionizationType != null && lipidFragmentationRuleType != null
        && lipidFragmentInformationLevelType != null && molecularFormula != null) {
      return new LipidFragmentationRule(polarityType, ionizationType, lipidFragmentationRuleType,
          lipidFragmentInformationLevelType, molecularFormula);
    } else if (polarityType != null && ionizationType != null && lipidFragmentationRuleType != null
        && lipidFragmentInformationLevelType != null && lipidFragmentationRuleRating != null) {
      return new LipidFragmentationRule(polarityType, ionizationType, lipidFragmentationRuleType,
          lipidFragmentInformationLevelType, lipidFragmentationRuleRating);
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

  public static Result validate(@Nullable LipidFragmentationRule rule) {
    StringBuilder errors = new StringBuilder();
    if (rule == null) {
      errors.append("Rule is null\n");
      return Result.error(errors.toString());
    }

    return validate(rule.getLipidFragmentationRuleType(), rule.getMolecularFormula());
  }

  public static Result validate(@Nullable LipidFragmentationRuleType ruleType,
      @Nullable String formula) {
    StringBuilder errors = new StringBuilder();
    if (ruleType == null) {
      errors.append("Fragmentation rule type is null, but may not be null.\n");
      return Result.error(errors.toString());
    }

    if (ruleType.requiresFromulaFragment()) {
      if ((formula == null || formula.isBlank())) {
        errors.append("Formula is empty but requires a value for rule type ")
            .append(inQuotes(ruleType.toString()));
      } else {
        try {
          final IMolecularFormula molFormula = FormulaUtils.createMajorIsotopeMolFormula(formula);
          if (molFormula == null) {
            errors.append("Invalid custom lipid fragment rule formula: ").append(formula)
                .append(" -> ").append(" could not parse.\n");
          }
        } catch (final RuntimeException e) {
          errors.append("Invalid custom lipid fragment rule formula: ").append(formula)
              .append(" -> ").append(" could not parse.\n");
        }
      }
    }
    return !errors.isEmpty() ? Result.error(errors.toString()) : Result.ok();
  }
}
