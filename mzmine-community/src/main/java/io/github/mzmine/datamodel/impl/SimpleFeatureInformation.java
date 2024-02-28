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

import io.github.mzmine.datamodel.FeatureInformation;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jetbrains.annotations.NotNull;

/**
 * @author aleksandrsmirnov
 */
public class SimpleFeatureInformation implements FeatureInformation {

  private final Map<String, String> properties;

  // ------------------------------------------------------------------------
  // ----- Constructors -----------------------------------------------------
  // ------------------------------------------------------------------------

  public SimpleFeatureInformation() {
    properties = new HashMap<>();
  }

  public SimpleFeatureInformation(String propertyName, String propertyValue) {
    this();
    properties.put(propertyName, propertyValue);
  }

  public SimpleFeatureInformation(Map<String, String> properties) {
    this.properties = properties;
  }

  // ------------------------------------------------------------------------
  // ----- Methods ----------------------------------------------------------
  // ------------------------------------------------------------------------

  public void addProperty(String name, String value) {
    properties.put(name, value);
  }

  public void addProperty(Map<String, String> properties) {
    this.properties.putAll(properties);
  }

  // ------------------------------------------------------------------------
  // ----- Properties -------------------------------------------------------
  // ------------------------------------------------------------------------

  @Override
  @NotNull
  public String getPropertyValue(String propertyName) {
    return properties.get(propertyName);
  }

  @Override
  @NotNull
  public String getPropertyValue(String propertyName, String defaultValue) {
    String value = properties.get(propertyName);
    if (value == null) {
      value = defaultValue;
    }
    return value;
  }

  @Override
  @NotNull
  public Map<String, String> getAllProperties() {
    return properties;
  }

  @Override
  @NotNull
  public synchronized SimpleFeatureInformation clone() {
    return new SimpleFeatureInformation(new HashMap<>(properties));
  }

  public static FeatureInformation loadFromXML(XMLStreamReader reader) throws XMLStreamException {
    while (
        !(reader.isStartElement() && reader.getLocalName().equals(FeatureInformation.XML_ELEMENT))
            && reader.hasNext()) {
      if (reader.isEndElement() && reader.getLocalName().equals(CONST.XML_DATA_TYPE_ELEMENT)) {
        return null;
      }
      reader.next();
    }

    SimpleFeatureInformation fi = new SimpleFeatureInformation();

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(FeatureInformation.XML_ELEMENT)) && !(reader.isEndElement() && reader.getLocalName()
        .equals(CONST.XML_DATA_TYPE_ELEMENT))) {
      if (reader.isStartElement() && reader.getLocalName()
          .equals(FeatureInformation.XML_PROPERTY_ELEMENT)) {
        String att = reader.getAttributeValue(null, FeatureInformation.XML_NAME_ATTR);
        String text = reader.getElementText();
        fi.addProperty(att, text);
      }
      reader.next();
    }

    return fi;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SimpleFeatureInformation that = (SimpleFeatureInformation) o;
    return Objects.equals(properties, that.properties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(properties);
  }
}
