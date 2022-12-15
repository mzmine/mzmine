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

package io.github.mzmine.datamodel.features.compoundannotations;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Describes a common feature annotation. Future implementations should also extend the
 * {@link io.github.mzmine.util.FeatureUtils#getBestFeatureAnnotation(ModularDataModel)}
 * method.
 */
public interface FeatureAnnotation {

  public static final String XML_ELEMENT = "feature_annotation";
  public static final String XML_TYPE_ATTR = "annotation_type";

  public static FeatureAnnotation loadFromXML(XMLStreamReader reader, MZmineProject project,
      ModularFeatureList flist, ModularFeatureListRow row) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      throw new IllegalStateException("Current element is not a feature annotation element");
    }

    return switch (reader.getAttributeValue(null, XML_TYPE_ATTR)) {
      case SpectralDBAnnotation.XML_ATTR ->
          SpectralDBAnnotation.loadFromXML(reader, project.getCurrentRawDataFiles());
      case SimpleCompoundDBAnnotation.XML_ATTR ->
          SimpleCompoundDBAnnotation.loadFromXML(reader, project, flist, row);
      default -> null;
    };
  }

  public void saveToXML(@NotNull XMLStreamWriter writer, ModularFeatureList flist,
      ModularFeatureListRow row) throws XMLStreamException;

  @Nullable Double getPrecursorMZ();

  @Nullable String getSmiles();

  @Nullable String getCompoundName();

  @Nullable String getFormula();

  @Nullable IonType getAdductType();

  @Nullable Float getMobility();

  @Nullable Float getCCS();

  @Nullable Float getRT();

  @Nullable Float getScore();

  @Nullable String getDatabase();

  /**
   * @return A unique identifier for saving any sub-class of this interface to XML.
   */
  @NotNull String getXmlAttributeKey();

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
  public default void writeOpeningTag(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(FeatureAnnotation.XML_ELEMENT);
    writer.writeAttribute(FeatureAnnotation.XML_TYPE_ATTR, getXmlAttributeKey());
  }

  /**
   * Convenience method to supply developers with a closing method for
   * {@link FeatureAnnotation#writeOpeningTag(XMLStreamWriter)}.
   */
  public default void writeClosingTag(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeEndElement();
  }
}
