/*
 * Copyright 2006-2020 The MZmine Development Team
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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.dataprocessing.id_formulaprediction;

import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.identities.MolecularFormulaIdentity;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.util.ParsingUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class ResultFormula extends MolecularFormulaIdentity {

  public static final String XML_ELEMENT = "result_formula";
  public static final String ISOTOPE_SCORE_ELEMENT = "isotope_score";
  public static final String MSMS_SCORE_ELEMENT = "msms_score";
  public static final String MSMS_ANNOTATIONS_ELEMENT = "msms_annotations";
  public static final String ANNOTATION_ELEMENT = "msms_annotation";
  public static final String ANNOTATION_MZ_ATTR = "mz";

  private final Float isotopeScore;
  private final Float msmsScore;
  private final IsotopePattern predictedIsotopePattern;
  private Map<Double, String> msmsAnnotation;

  protected ResultFormula(ResultFormula f) {
    this(f.cdkFormula, f.predictedIsotopePattern, f.getIsotopeScore(), f.getMSMSScore(),
        f.getMSMSannotation(), f.getSearchedNeutralMass());
  }

  public ResultFormula(IMolecularFormula cdkFormula, IsotopePattern predictedIsotopePattern,
      Float isotopeScore, Float msmsScore, Map<Double, String> msmsAnnotation,
      double searchedNeutralMass) {
    super(cdkFormula, searchedNeutralMass);
    this.predictedIsotopePattern = predictedIsotopePattern;
    this.isotopeScore = isotopeScore;
    this.msmsScore = msmsScore;
    this.msmsAnnotation = msmsAnnotation;
  }

  public Map<Double, String> getMSMSannotation() {
    return msmsAnnotation;
  }

  public IsotopePattern getPredictedIsotopes() {
    return predictedIsotopePattern;
  }

  public Float getIsotopeScore() {
    return isotopeScore;
  }

  public Float getMSMSScore() {
    return msmsScore;
  }

  @Override
  public float getScore(double neutralMass, float ppmMax, float fIsotopeScore, float fMSMSscore) {
    float ppmScore = super.getPPMScore(neutralMass, ppmMax);
    float totalScore = ppmScore;
    float div = 1f;
    Float isoScore = getIsotopeScore();
    if (isoScore != null) {
      totalScore += isoScore * fIsotopeScore;
      div += fIsotopeScore;
    }
    Float msmsScore = getMSMSScore();
    if (msmsScore != null) {
      totalScore += msmsScore * fMSMSscore;
      div += fMSMSscore;
    }

    return totalScore / div;
  }

  public float getPpmDiff() {
    return getPpmDiff(searchedNeutralMass);
  }

  public double getAbsoluteMzDiff() {
    return searchedNeutralMass - getExactMass();
  }

  public void saveToXML(@NotNull final XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);

    super.saveToXML(writer);

    writer.writeStartElement(ISOTOPE_SCORE_ELEMENT);
    writer.writeCharacters(ParsingUtils.numberToString(isotopeScore));
    writer.writeEndElement();

    if (predictedIsotopePattern != null) {
      predictedIsotopePattern.saveToXML(writer);
    }

    writer.writeStartElement(MSMS_SCORE_ELEMENT);
    writer.writeCharacters(ParsingUtils.numberToString(msmsScore));
    writer.writeEndElement();

    if (msmsAnnotation != null) {
      writer.writeStartElement(MSMS_ANNOTATIONS_ELEMENT);
      for (Entry<Double, String> entry : msmsAnnotation.entrySet()) {
        writer.writeStartElement(ANNOTATION_ELEMENT);
        writer.writeAttribute(ANNOTATION_MZ_ATTR, ParsingUtils.numberToString(entry.getKey()));
        writer.writeCharacters(ParsingUtils.parseNullableString(entry.getValue()));
        writer.writeEndElement();
      }
      writer.writeEndElement();
    }
    writer.writeEndElement();
  }

  public static ResultFormula loadFromXML(@NotNull final XMLStreamReader reader)
      throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      throw new IllegalStateException(
          "Unexpected xml element for ResultFormula: " + reader.getLocalName());
    }

    Float isotopeScore = null;
    Float msmsScore = null;
    IsotopePattern pattern = null;
    Map<Double, String> annotations = null;
    MolecularFormulaIdentity id = null;

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(XML_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }
      switch (reader.getLocalName()) {
        case MolecularFormulaIdentity.XML_ELEMENT -> id = MolecularFormulaIdentity.loadFromXML(
            reader);
        case ISOTOPE_SCORE_ELEMENT -> isotopeScore = ParsingUtils.stringToFloat(
            reader.getElementText());
        case MSMS_SCORE_ELEMENT -> msmsScore = ParsingUtils.stringToFloat(reader.getElementText());
        case SimpleIsotopePattern.XML_ELEMENT -> pattern = SimpleIsotopePattern.loadFromXML(reader);
        case MSMS_ANNOTATIONS_ELEMENT -> annotations = loadAnnotations(reader);
      }
    }

    return new ResultFormula(id.getFormulaAsObject(), pattern, isotopeScore, msmsScore, annotations,
        id.getSearchedNeutralMass());
  }

  private static Map<Double, String> loadAnnotations(XMLStreamReader reader)
      throws XMLStreamException {
    var map = new HashMap<Double, String>();
    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(MSMS_ANNOTATIONS_ELEMENT))) {
      reader.next();
      if (reader.isStartElement() && reader.getLocalName().equals(ANNOTATION_ELEMENT)) {
        Double key = ParsingUtils.stringToDouble(
            reader.getAttributeValue(null, ANNOTATION_MZ_ATTR));
        String value = ParsingUtils.readNullableString(reader.getElementText());
        map.put(key, value);
      }
    }
    return map;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ResultFormula)) {
      return false;
    }
    ResultFormula that = (ResultFormula) o;
    final boolean patternEquals = Objects.equals(predictedIsotopePattern, that.predictedIsotopePattern);
    final boolean annotationEquals = Objects.equals(msmsAnnotation, that.msmsAnnotation);
    final boolean formulaEquals = Objects.equals(getFormulaAsString(), that.getFormulaAsString());
    return Objects.equals(getIsotopeScore(), that.getIsotopeScore()) && Objects.equals(msmsScore,
        that.msmsScore) && patternEquals && annotationEquals && formulaEquals;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getIsotopeScore(), msmsScore, predictedIsotopePattern, msmsAnnotation);
  }
}
