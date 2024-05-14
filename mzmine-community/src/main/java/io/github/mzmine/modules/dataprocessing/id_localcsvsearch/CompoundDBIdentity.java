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
