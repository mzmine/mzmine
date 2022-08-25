/*
 *  Copyright 2006-2022 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.datamodel.features.compoundannotations;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
