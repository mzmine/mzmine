/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_mzml;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jetbrains.annotations.NotNull;

/**
 * Cursor operations shared by the document, header and spectrum readers.
 */
public final class MzMLXml {

  /**
   * @param xml  cursor on the opening tag
   * @param name required unqualified attribute
   * @return attribute value
   * @throws XMLStreamException if the attribute is absent
   */
  public static @NotNull String required(@NotNull final XMLStreamReader xml,
      @NotNull final String name) throws XMLStreamException {
    final String value = xml.getAttributeValue(null, name);
    if (value == null) {
      throw new XMLStreamException("Missing " + name + " on " + xml.getLocalName(),
          xml.getLocation());
    }
    return value;
  }

  private MzMLXml() {
  }

  /**
   * @param xml reader on the start of the element to consume, left on its matching end tag
   * @throws XMLStreamException if the document ends inside the element
   */
  public static void skipElement(@NotNull final XMLStreamReader xml) throws XMLStreamException {
    int depth = 1;
    while (depth > 0) {
      switch (xml.next()) {
        case XMLStreamConstants.START_ELEMENT -> depth++;
        case XMLStreamConstants.END_ELEMENT -> depth--;
        case XMLStreamConstants.END_DOCUMENT ->
            throw new XMLStreamException("Unexpected end of document inside an element",
                xml.getLocation());
      }
    }
  }
}
