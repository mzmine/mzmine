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

package io.github.mzmine.util;

import com.google.common.collect.Range;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * XML processing utilities
 */
public class XMLUtils {

  public static Range<Integer> parseIntegerRange(Element xmlElement, String tagName) {
    NodeList items = xmlElement.getElementsByTagName(tagName);
    if (items.getLength() == 0)
      return null;
    Element tag = (Element) items.item(0);
    items = tag.getElementsByTagName("min");
    if (items.getLength() == 0)
      return null;
    Element min = (Element) items.item(0);
    items = tag.getElementsByTagName("max");
    if (items.getLength() == 0)
      return null;
    Element max = (Element) items.item(0);

    String minText = min.getTextContent();
    String maxText = max.getTextContent();
    Range<Integer> r = Range.closed(Integer.valueOf(minText), Integer.valueOf(maxText));
    return r;
  }

  public static Range<Double> parseDoubleRange(Element xmlElement, String tagName) {
    NodeList items = xmlElement.getElementsByTagName(tagName);
    if (items.getLength() == 0)
      return null;
    Element tag = (Element) items.item(0);
    items = tag.getElementsByTagName("min");
    if (items.getLength() == 0)
      return null;
    Element min = (Element) items.item(0);
    items = tag.getElementsByTagName("max");
    if (items.getLength() == 0) {
      return null;
    }
    Element max = (Element) items.item(0);

    String minText = min.getTextContent();
    String maxText = max.getTextContent();
    Range<Double> r = Range.closed(Double.valueOf(minText), Double.valueOf(maxText));
    return r;
  }

  public static Range<Float> parseFloatRange(Element xmlElement, String tagName) {
    Range<Double> doubleRange = parseDoubleRange(xmlElement, tagName);
    return doubleRange != null ? Range.closed(doubleRange.lowerEndpoint().floatValue(),
        doubleRange.upperEndpoint().floatValue()) : null;
  }

  public static void appendRange(Element xmlElement, String tagName, Range<?> range) {
    if (range == null) {
      return;
    }
    Document parentDocument = xmlElement.getOwnerDocument();
    Element newElement = parentDocument.createElement(tagName);
    Element minElement = parentDocument.createElement("min");
    minElement.setTextContent(String.valueOf(range.lowerEndpoint()));
    newElement.appendChild(minElement);
    Element maxElement = parentDocument.createElement("max");
    maxElement.setTextContent(String.valueOf(range.upperEndpoint()));
    newElement.appendChild(maxElement);
    xmlElement.appendChild(newElement);

  }

  public static void appendString(Element xmlElement, String tagName, String value) {
    if (value == null)
      return;
    Document parentDocument = xmlElement.getOwnerDocument();
    Element newElement = parentDocument.createElement(tagName);
    newElement.setTextContent(value);
  }

  public static String parseString(Element xmlElement, String tagName) {
    NodeList items = xmlElement.getElementsByTagName(tagName);
    if (items.getLength() == 0)
      return null;
    else
      return items.item(0).getTextContent();
  }
}
