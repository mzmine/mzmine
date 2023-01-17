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
