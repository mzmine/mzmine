/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.collect.Range;

/**
 * XML processing utilities
 */
public class XMLUtils {

    public static Range<Integer> parseIntegerRange(Element xmlElement,
            String tagName) {
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
        Range<Integer> r = Range.closed(Integer.valueOf(minText),
                Integer.valueOf(maxText));
        return r;
    }

    public static Range<Double> parseDoubleRange(Element xmlElement,
            String tagName) {
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
        Range<Double> r = Range.closed(Double.valueOf(minText),
                Double.valueOf(maxText));
        return r;
    }

    public static void appendRange(Element xmlElement, String tagName,
            Range<?> range) {
        if (range == null)
            return;
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

    public static void appendString(Element xmlElement, String tagName,
            String value) {
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
