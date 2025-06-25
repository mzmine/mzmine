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

package io.github.mzmine.datamodel.features.compoundannotations;

import com.sun.xml.txw2.output.IndentingXMLStreamWriter;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.structures.MolecularStructure;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Describes a common feature annotation. Future implementations should also extend the
 * {@link io.github.mzmine.util.FeatureUtils#getBestFeatureAnnotation(ModularDataModel)} method.
 */
public interface FeatureAnnotation {

  Logger logger = Logger.getLogger(FeatureAnnotation.class.getName());

  String XML_ELEMENT = "feature_annotation";
  String XML_TYPE_ATTR = "annotation_type";

  static FeatureAnnotation loadFromXML(XMLStreamReader reader, MZmineProject project,
      ModularFeatureList flist, ModularFeatureListRow row) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      throw new IllegalStateException("Current element is not a feature annotation element");
    }

    return switch (reader.getAttributeValue(null, XML_TYPE_ATTR)) {
      case SpectralDBAnnotation.XML_ATTR ->
          SpectralDBAnnotation.loadFromXML(reader, project, project.getCurrentRawDataFiles());
      case SimpleCompoundDBAnnotation.XML_ATTR ->
          SimpleCompoundDBAnnotation.loadFromXML(reader, project, flist, row);
      case MatchedLipid.XML_ELEMENT ->
          MatchedLipid.loadFromXML(reader, project.getCurrentRawDataFiles());
      default -> null;
    };
  }

  /**
   * Translates a list of annotations to an XML so it can be saved outside of the project load/save
   * operations. Useful if a annotation was done/edited manually and it has to be saved in a
   * parameter set.
   * <p></p>
   * Can be used in conjuction with the
   * {@link io.github.mzmine.parameters.parametertypes.EmbeddedXMLParameter} to store annotations in
   * a parameter set.
   */
  static String toXMLString(List<FeatureAnnotation> annotations, ModularFeatureList flist,
      ModularFeatureListRow row) {

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      final XMLOutputFactory xof = XMLOutputFactory.newInstance();
      final XMLStreamWriter writer = new IndentingXMLStreamWriter(
          xof.createXMLStreamWriter(baos, "UTF-8"));
      writer.writeStartDocument();

      for (FeatureAnnotation annotation : annotations) {
        annotation.saveToXML(writer, flist, row);
      }

      writer.writeEndDocument();
      writer.flush();
      writer.close();

      return baos.toString();
    } catch (IOException | XMLStreamException e) {
      logger.log(Level.WARNING, "Error when parsing annotation to xml string.", e);
      return null;
    }
  }

  /**
   * Translates an XML string to a list of feature annotations. Can be used to load an annotation
   * from an applied method, e.g., after storing it in a string parameter. Requires the same
   * parameters as
   * {@link #loadFromXML(XMLStreamReader, MZmineProject, ModularFeatureList,
   * ModularFeatureListRow)}
   */
  static List<FeatureAnnotation> parseFromXMLString(@NotNull String xml, MZmineProject project,
      ModularFeatureList flist, ModularFeatureListRow row) {
    try (ByteArrayInputStream inputStream = new ByteArrayInputStream(
        xml.getBytes(StandardCharsets.UTF_8))) {
      XMLInputFactory xif = XMLInputFactory.newInstance();
      XMLStreamReader reader = xif.createXMLStreamReader(inputStream);
      List<FeatureAnnotation> annotations = new ArrayList<>();
      while (reader.hasNext()) {
        while (reader.hasNext() && !(reader.isStartElement() && reader.getLocalName()
            .equals(FeatureAnnotation.XML_ELEMENT))) {
          reader.next();
        }

        if (reader.isStartElement() && reader.getLocalName()
            .equals(FeatureAnnotation.XML_ELEMENT)) {
          final FeatureAnnotation annotation = loadFromXML(reader, project, flist, row);
          annotations.add(annotation);
        }
      }
      return annotations;
    } catch (IOException | XMLStreamException e) {
      logger.log(Level.WARNING, "Error when parsing annotation from xml string.", e);
      return List.of();
    }
  }

  void saveToXML(@NotNull XMLStreamWriter writer, ModularFeatureList flist,
      ModularFeatureListRow row) throws XMLStreamException;

  @Nullable
  Double getPrecursorMZ();

  /**
   * This should be usually recalculated on demand from smiles or inchi. Also if smiles or inchi
   * changes the getStructure structure should be recalculated or just set to null
   *
   * @return the structure parsed from smiles or inchi
   */
  @Nullable
  MolecularStructure getStructure();

  @Nullable
  String getSmiles();

  @Nullable
  String getInChI();

  @Nullable
  String getInChIKey();

  @Nullable
  String getCompoundName();

  @Nullable
  String getFormula();

  @Nullable
  IonType getAdductType();

  @Nullable
  Float getMobility();

  @Nullable
  Float getCCS();

  @Nullable
  Float getRT();

  @Nullable
  Float getScore();

  @Nullable
  default String getScoreString() {
    var score = getScore();
    if (score == null) {
      return null;
    }
    return MZmineCore.getConfiguration().getScoreFormat().format(score);
  }

  @Nullable
  String getDatabase();

  /**
   * Keep stable as its exported to tools. often the xml key but not always
   *
   * @return defining the annotation method
   */
  default @NotNull String getAnnotationMethodUniqueId() {
    return getXmlAttributeKey();
  }

  /**
   * @return A unique identifier for saving any sub-class of this interface to XML.
   */
  @NotNull
  String getXmlAttributeKey();

  /**
   * Writes an opening tag that conforms with the
   * {@link FeatureAnnotation#loadFromXML(XMLStreamReader, MZmineProject, ModularFeatureList,
   * ModularFeatureListRow)}  method, so the type can be recognised and delegated to the correct
   * subclass. This allows combination of multiple different {@link FeatureAnnotation}
   * implementations in a single list.
   * <p></p>
   * Uses {@link FeatureAnnotation#getXmlAttributeKey()} to write the correct tags.
   *
   * @param writer The writer
   */
  default void writeOpeningTag(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(FeatureAnnotation.XML_ELEMENT);
    writer.writeAttribute(FeatureAnnotation.XML_TYPE_ATTR, getXmlAttributeKey());
  }

  /**
   * Convenience method to supply developers with a closing method for
   * {@link FeatureAnnotation#writeOpeningTag(XMLStreamWriter)}.
   */
  default void writeClosingTag(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeEndElement();
  }
}
