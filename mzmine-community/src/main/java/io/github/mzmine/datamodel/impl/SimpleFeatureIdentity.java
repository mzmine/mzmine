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

package io.github.mzmine.datamodel.impl;

import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.util.ParsingUtils;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;
import org.jetbrains.annotations.NotNull;

/**
 * Simple FeatureIdentity implementation;
 */
@Deprecated
@ScheduledForRemoval
public class SimpleFeatureIdentity implements FeatureIdentity {

  public static final String XML_IDENTITY_TYPE = "simplefeatureidentity";
  public static final String XML_PROPERTIES_ELEMENT = "properties";

  private Hashtable<String, String> properties;

  /**
   * This constructor is protected so only derived classes can use it. Other modules using this
   * class should always set the name by default.
   */
  protected SimpleFeatureIdentity() {

    this("Unknown name");
  }

  public SimpleFeatureIdentity(final String name) {

    // Check name.
    if (name == null) {
      throw new IllegalArgumentException("Identity properties must contain name");
    }

    properties = new Hashtable<String, String>();

    properties.put(PROPERTY_NAME, name);

  }

  public SimpleFeatureIdentity(final String name, final String formula, final String method,
      final String id, final String url) {

    // Check name
    if (name == null) {
      throw new IllegalArgumentException("Identity properties must contain name");
    }

    properties = new Hashtable<String, String>();

    properties.put(PROPERTY_NAME, name);

    if (formula != null) {
      properties.put(PROPERTY_FORMULA, formula);
    }
    if (method != null) {
      properties.put(PROPERTY_METHOD, method);
    }
    if (id != null) {
      properties.put(PROPERTY_ID, id);
    }
    if (url != null) {
      properties.put(PROPERTY_URL, url);
    }
  }

  public SimpleFeatureIdentity(final Hashtable<String, String> prop) {

    // Check for name .
    if (prop.get(PROPERTY_NAME) == null) {

      throw new IllegalArgumentException("Identity properties must contain name");
    }

    properties = prop;
  }

  @Override
  public @NotNull String getName() {

    return properties.get(PROPERTY_NAME);
  }

  public String toString() {

    return getName();
  }

  @Override
  public @NotNull String getDescription() {

    final StringBuilder description = new StringBuilder();
    for (final Entry<String, String> entry : properties.entrySet()) {

      if (description.length() > 0) {
        description.append('\n');
      }
      description.append(entry.getKey());
      description.append(": ");
      description.append(entry.getValue());
    }

    return description.toString();
  }

  @Override
  public @NotNull Map<String, String> getAllProperties() {

    return new Hashtable<String, String>(properties);
  }

  @Override
  // Removed @NotNull in front because the function may return null
  public String getPropertyValue(final String property) {
    return properties.get(property);
  }

  public void setPropertyValue(final String property, final String value) {

    // Check name.
    if (property.equals(PROPERTY_NAME) && value == null) {

      throw new IllegalArgumentException("Identity properties must contain name");
    }

    if(value == null) {
      return;
    }

    properties.put(property, value);
  }

  /**
   * Copy the identity.
   *
   * @return the new copy.
   */
  @SuppressWarnings("unchecked")
  @Override
  public synchronized @NotNull Object clone() {
    return new SimpleFeatureIdentity((Hashtable<String, String>) properties.clone());
  }

  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_GENERAL_IDENTITY_ELEMENT);
    writer.writeAttribute(FeatureIdentity.XML_IDENTITY_TYPE_ATTR, XML_IDENTITY_TYPE);
    savePropertyMap(writer);
    writer.writeEndElement();
  }

  protected void savePropertyMap(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_PROPERTIES_ELEMENT);
    for (Entry<String, String> entry : getAllProperties().entrySet()) {
      writer.writeStartElement(XML_PROPERTY_ELEMENT);
      writer.writeAttribute(XML_NAME_ATTR, entry.getKey());
      writer.writeCharacters(ParsingUtils.parseNullableString(entry.getValue()));
      writer.writeEndElement();
    }
    writer.writeEndElement(); // properties
  }

  public static FeatureIdentity loadFromXML(XMLStreamReader reader) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName()
        .equals(FeatureIdentity.XML_GENERAL_IDENTITY_ELEMENT) && reader
        .getAttributeValue(null, FeatureIdentity.XML_IDENTITY_TYPE_ATTR)
        .equals(SimpleFeatureIdentity.XML_IDENTITY_TYPE))) {
      throw new IllegalArgumentException("Cannot load simple feature identity. Wrong xml element.");
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

    SimpleFeatureIdentity fi = new SimpleFeatureIdentity();
    properties.forEach(fi::setPropertyValue);
    return fi;
  }

  @NotNull
  protected static Map<String, String> readPropertyValues(XMLStreamReader reader)
      throws XMLStreamException {
    Map<String, String> properties = new HashMap<>();

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(XML_PROPERTIES_ELEMENT))) {
      if (reader.isStartElement() && reader.getLocalName()
          .equals(FeatureIdentity.XML_PROPERTY_ELEMENT)) {
        String att = reader.getAttributeValue(null, FeatureIdentity.XML_NAME_ATTR);
        String text = ParsingUtils.readNullableString(reader.getElementText());
        properties.put(att, text);
      }
      reader.next();
    }
    return properties;
  }
}
