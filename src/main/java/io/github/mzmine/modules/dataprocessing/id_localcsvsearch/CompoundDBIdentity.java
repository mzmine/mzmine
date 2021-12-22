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

package io.github.mzmine.modules.dataprocessing.id_localcsvsearch;

import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.impl.SimpleFeatureIdentity;
import java.util.Map;
import java.util.Objects;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

public class CompoundDBIdentity extends SimpleFeatureIdentity {

  public static final String XML_IDENTITY_TYPE = "compounddbfeatureidentity";

  public CompoundDBIdentity() {
    super();
  }

  public CompoundDBIdentity(String name, String formula, String method, String url) {
    super(name, formula, method, null, url);
  }

  @Override
  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_GENERAL_IDENTITY_ELEMENT);
    writer.writeAttribute(FeatureIdentity.XML_IDENTITY_TYPE_ATTR, XML_IDENTITY_TYPE);
    savePropertyMap(writer);
    writer.writeEndElement();
  }

  public static FeatureIdentity loadFromXML(XMLStreamReader reader) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName()
        .equals(FeatureIdentity.XML_GENERAL_IDENTITY_ELEMENT) && reader
        .getAttributeValue(null, FeatureIdentity.XML_IDENTITY_TYPE_ATTR)
        .equals(CompoundDBIdentity.XML_IDENTITY_TYPE))) {
      throw new IllegalArgumentException("Cannot load compound feature identity. Wrong xml element.");
    }

    Map<String, String> properties = null;

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(XML_GENERAL_IDENTITY_ELEMENT))) {
      reader.next();
      if (reader.isStartElement() && reader.getLocalName().equals(XML_PROPERTIES_ELEMENT)) {
        properties = readPropertyValues(reader);
      }
    }

    if(properties == null) {
      return null;
    }

    CompoundDBIdentity fi = new CompoundDBIdentity();
    properties.forEach(fi::setPropertyValue);
    return fi;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getAllProperties());
  }

  @Override
  public boolean equals(Object obj) {
    if(!(obj instanceof CompoundDBIdentity that)) {
      return false;
    }
    return Objects.equals(getAllProperties(), that.getAllProperties());
  }
}
