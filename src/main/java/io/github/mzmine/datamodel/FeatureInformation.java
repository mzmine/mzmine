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
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.datamodel;

import java.util.Map;
import java.util.Map.Entry;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;

/**
 * This interface is used to keep extra information about peaks
 *
 * @author aleksandrsmirnov
 */
public interface FeatureInformation {

  String XML_PROPERTY_ELEMENT = "property";
  String XML_ELEMENT = "featureinformation";
  String XML_NAME_ATTR = "name";

  /**
   * Returns the value of a property
   *
   * @param property name
   * @return
   */

  @NotNull
  String getPropertyValue(String property);

  @NotNull
  String getPropertyValue(String property, String defaultValue);

  /**
   * Returns all the properties in the form of a map <key, value>
   *
   * @return
   */

  @NotNull
  Map<String, String> getAllProperties();

  default void saveToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);

    for (Entry<String, String> entry : getAllProperties().entrySet()) {
      writer.writeStartElement(XML_PROPERTY_ELEMENT);
      writer.writeAttribute(XML_NAME_ATTR, entry.getKey());
      writer.writeCharacters(entry.getValue());
      writer.writeEndElement();
    }

    writer.writeEndElement();
  }
}
