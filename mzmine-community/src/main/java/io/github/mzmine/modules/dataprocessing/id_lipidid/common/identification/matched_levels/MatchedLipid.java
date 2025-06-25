/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.IonTypeParser;
import io.github.mzmine.datamodel.structures.MolecularStructure;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.molecular_species.MolecularSpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.species_level.SpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidFragment;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.ParsingUtils;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class MatchedLipid implements FeatureAnnotation {

  public static final String XML_ELEMENT = "matchedlipid";
  private static final Logger logger = Logger.getLogger(MatchedLipid.class.getName());
  private static final String XML_LIPID_ANNOTATION_ELEMENT = "lipidannotation";
  private static final String XML_ACCURATE_MZ = "accuratemz";
  private static final String XML_IONIZATION_TYPE = "ionizationtype";
  private static final String XML_MATCHED_FRAGMENTS = "matchedfragments";
  private static final String XML_MSMS_SCORE = "msmsscore";
  private static final String XML_COMMENT = "comment";
  private static final String XML_STATUS = "status";

  private ILipidAnnotation lipidAnnotation;
  private Double accurateMz;
  private IonizationType ionizationType;
  private Set<LipidFragment> matchedFragments;
  private Double msMsScore;
  private String comment;
  private MatchedLipidStatus status;

  public MatchedLipid(ILipidAnnotation lipidAnnotation, Double accurateMz,
      IonizationType ionizationType, Set<LipidFragment> matchedFragments, Double msMsScore) {
    this(lipidAnnotation, accurateMz, ionizationType, matchedFragments, msMsScore,
        MatchedLipidStatus.MATCHED);
  }

  public MatchedLipid(ILipidAnnotation lipidAnnotation, Double accurateMz,
      IonizationType ionizationType, Set<LipidFragment> matchedFragments, Double msMsScore,
      @NotNull MatchedLipidStatus status) {
    this.lipidAnnotation = lipidAnnotation;
    this.accurateMz = accurateMz;
    this.ionizationType = ionizationType;
    this.matchedFragments = matchedFragments;
    this.msMsScore = msMsScore;
    this.status = status;
    this.comment = status.getComment();
  }

  public static MatchedLipid loadFromXML(XMLStreamReader reader,
      Collection<RawDataFile> possibleFiles) throws XMLStreamException {
    // differentiate between old and new versions
    if ((reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      // is old style start element
      logger.finest("Found old style lipid annotation entry, will convert to new one on save.");
    } else if (reader.isStartElement() && reader.getLocalName()
        .equals(FeatureAnnotation.XML_ELEMENT) && reader.getAttributeValue(null,
        FeatureAnnotation.XML_TYPE_ATTR).equals(XML_ELEMENT)) {
      // is new style start element
      logger.finest("Found new style lipid annotation entry.");
    } else {
      throw new IllegalStateException(
          "Cannot load matched lipid from the current element. Wrong name.");
    }

    ILipidAnnotation lipidAnnotation = null;
    Double accurateMz = null;
    IonizationType ionizationType = null;
    Set<LipidFragment> lipidFragments = null;
    Double msMsScore = null;
    String comment = "";
    MatchedLipidStatus status = null;
    while (reader.hasNext() && !(reader.isEndElement() && (reader.getLocalName().equals(XML_ELEMENT)
        || reader.getLocalName().equals(FeatureAnnotation.XML_ELEMENT)))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      switch (reader.getLocalName()) {
        case XML_LIPID_ANNOTATION_ELEMENT -> {
          if (reader.getAttributeValue(null, XML_LIPID_ANNOTATION_ELEMENT)
              .equals(LipidAnnotationLevel.SPECIES_LEVEL.name())) {
            lipidAnnotation = SpeciesLevelAnnotation.loadFromXML(reader);
          } else if (reader.getAttributeValue(null, XML_LIPID_ANNOTATION_ELEMENT)
              .equals(LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL.name())) {
            lipidAnnotation = MolecularSpeciesLevelAnnotation.loadFromXML(reader);
          }
        }
        case XML_ACCURATE_MZ -> accurateMz = Double.parseDouble(reader.getElementText());
        case XML_IONIZATION_TYPE ->
            ionizationType = ParsingUtils.ionizationNameToIonizationType(reader.getElementText());
        case XML_MATCHED_FRAGMENTS ->
            lipidFragments = loadLipidFragmentsFromXML(reader, possibleFiles);
        case XML_MSMS_SCORE -> msMsScore = Double.parseDouble(reader.getElementText());
        case XML_COMMENT -> {
          if (reader.hasNext() && reader.next() == XMLStreamConstants.CHARACTERS) {
            String text = reader.getText();
            if (!Objects.equals(text, CONST.XML_NULL_VALUE)) {
              comment = text;
            }
          }
        }
        case XML_STATUS -> status = MatchedLipidStatus.parseOrElse(reader.getElementText(),
            MatchedLipidStatus.UNCONFIRMED);
        default -> {
        }
      }
    }
    if (status == null) {
      status = MatchedLipidStatus.UNCONFIRMED; // should always load
    }

    MatchedLipid matchedLipid = new MatchedLipid(lipidAnnotation, accurateMz, ionizationType,
        lipidFragments, msMsScore, status);
    if (comment != null) {
      matchedLipid.setComment(comment);
    }
    return matchedLipid;
  }

  private static Set<LipidFragment> loadLipidFragmentsFromXML(XMLStreamReader reader,
      Collection<RawDataFile> possibleFiles) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_MATCHED_FRAGMENTS))) {
      throw new IllegalStateException(
          "Cannot load matched lipid fragments from the current element. Wrong name.");
    }

    Set<LipidFragment> lipidFragments = new HashSet<>();
    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(XML_MATCHED_FRAGMENTS))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }
      lipidFragments.add(LipidFragment.loadFromXML(reader, possibleFiles));
    }
    return lipidFragments;
  }

  public static double getExactMass(MatchedLipid match) {
    return MolecularFormulaManipulator.getMass(match.getLipidAnnotation().getMolecularFormula(),
        AtomContainerManipulator.MonoIsotopic) + match.getIonizationType().getAddedMass();
  }

  public ILipidAnnotation getLipidAnnotation() {
    return lipidAnnotation;
  }

  public void setLipidAnnotation(ILipidAnnotation lipidAnnotation) {
    this.lipidAnnotation = lipidAnnotation;
  }

  public Double getAccurateMz() {
    return accurateMz;
  }

  public void setAccurateMz(Double accurateMz) {
    this.accurateMz = accurateMz;
  }

  public IonizationType getIonizationType() {
    return ionizationType;
  }

  public void setIonizationType(IonizationType ionizationType) {
    this.ionizationType = ionizationType;
  }

  public Set<LipidFragment> getMatchedFragments() {
    return matchedFragments;
  }

  public void setMatchedFragments(Set<LipidFragment> matchedFragments) {
    this.matchedFragments = matchedFragments;
  }

  public Double getMsMsScore() {
    return msMsScore;
  }

  public void setMsMsScore(Double msMsScore) {
    this.msMsScore = msMsScore;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public MatchedLipidStatus getStatus() {
    return status;
  }

  public void setStatus(MatchedLipidStatus status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return lipidAnnotation.getAnnotation();
  }


  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, ModularFeatureList flist,
      ModularFeatureListRow row) throws XMLStreamException {
    writeOpeningTag(writer);

    lipidAnnotation.saveToXML(writer);
    writer.writeStartElement(XML_ACCURATE_MZ);
    writer.writeCharacters(accurateMz.toString());
    writer.writeEndElement();
    writer.writeStartElement(XML_IONIZATION_TYPE);
    writer.writeCharacters(ionizationType.name());
    writer.writeEndElement();
    writer.writeStartElement(XML_MATCHED_FRAGMENTS);
    if (matchedFragments != null) {
      for (LipidFragment lipidFragment : matchedFragments) {
        lipidFragment.saveToXML(writer);
      }
    } else {
      writer.writeCharacters(CONST.XML_NULL_VALUE);
    }
    writer.writeEndElement();
    writer.writeStartElement(XML_MSMS_SCORE);
    writer.writeCharacters(msMsScore.toString());
    writer.writeEndElement();
    writer.writeStartElement(XML_COMMENT);
    if (comment != null) {
      writer.writeCharacters(comment);
    } else {
      writer.writeCharacters(CONST.XML_NULL_VALUE);
    }
    writer.writeEndElement();
    writer.writeStartElement(XML_STATUS);
    writer.writeCharacters(status.name());
    writer.writeEndElement();

    writeClosingTag(writer);
  }

  @Override
  public @Nullable Double getPrecursorMZ() {
    return getExactMass(this);
  }

  @Override
  public @Nullable MolecularStructure getStructure() {
    return null;
  }

  @Override
  public @Nullable String getSmiles() {
    return null;
  }

  @Override
  public @Nullable String getInChI() {
    return null;
  }

  @Override
  public @Nullable String getInChIKey() {
    return null;
  }

  @Override
  public @Nullable String getCompoundName() {
    return getLipidAnnotation().getAnnotation();
  }

  @Override
  public @Nullable String getFormula() {
    return MolecularFormulaManipulator.getString(getLipidAnnotation().getMolecularFormula());
  }

  @Override
  public @Nullable IonType getAdductType() {
    try {
      return IonTypeParser.parse(getIonizationType().toString());
    } catch (Exception e) {
      logger.fine(() -> "Error parsing ion type " + getIonizationType().toString());
      return null;
    }
  }

  @Override
  public @Nullable Float getMobility() {
    return null;
  }

  @Override
  public @Nullable Float getCCS() {
    return null;
  }

  @Override
  public @Nullable Float getRT() {
    return null;
  }

  @Override
  public @Nullable Float getScore() {
    return getMsMsScore() != null ? getMsMsScore().floatValue() : null;
  }

  @Override
  public @Nullable String getDatabase() {
    return "Rule-based lipid fragmentation";
  }

  @Override
  public @NotNull String getXmlAttributeKey() {
    return XML_ELEMENT;
  }
}
