/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 */

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;

/**
 * A double-bond position using delta numbering from the chain's C1 carbon.
 *
 * @param position one-based carbon number of the first carbon in the double bond; for example 9 in
 *                 {@code 18:1(9)}
 * @param geometry cis/trans geometry when known, otherwise {@link DoubleBondGeometry#UNKNOWN}
 */
public record DoubleBondPosition(int position, @NotNull DoubleBondGeometry geometry)
    implements Comparable<DoubleBondPosition> {

  static final String XML_ELEMENT = "doublebondposition";
  static final String XML_POSITION = "position";
  static final String XML_GEOMETRY = "geometry";

  public DoubleBondPosition {
    if (position < 1) {
      throw new IllegalArgumentException("Double-bond position must be at least 1.");
    }
    if (geometry == null) {
      throw new NullPointerException("Double-bond geometry must not be null.");
    }
  }

  /** Creates a position when the analytical evidence does not resolve E/Z geometry. */
  public static @NotNull DoubleBondPosition unknownGeometry(final int position) {
    return new DoubleBondPosition(position, DoubleBondGeometry.UNKNOWN);
  }

  /** Returns the shorthand component without parentheses, for example {@code 9} or {@code 9Z}. */
  public @NotNull String toShorthand() {
    return position + geometry.getSymbol();
  }

  @Override
  public int compareTo(final @NotNull DoubleBondPosition other) {
    final int positionComparison = Integer.compare(position, other.position);
    return positionComparison != 0 ? positionComparison : geometry.compareTo(other.geometry);
  }

  /** Lexicographically compares two already chain-associated position lists. */
  public static int compareLists(final @NotNull List<DoubleBondPosition> first,
      final @NotNull List<DoubleBondPosition> second) {
    final int commonSize = Math.min(first.size(), second.size());
    for (int i = 0; i < commonSize; i++) {
      final int comparison = first.get(i).compareTo(second.get(i));
      if (comparison != 0) {
        return comparison;
      }
    }
    return Integer.compare(first.size(), second.size());
  }

  public void saveToXML(final @NotNull XMLStreamWriter writer) throws XMLStreamException {
    writer.writeEmptyElement(XML_ELEMENT);
    writer.writeAttribute(XML_POSITION, Integer.toString(position));
    writer.writeAttribute(XML_GEOMETRY, geometry.getUniqueID());
  }

  public static @NotNull DoubleBondPosition loadFromXML(final @NotNull XMLStreamReader reader)
      throws XMLStreamException {
    if (!(reader.isStartElement() && XML_ELEMENT.equals(reader.getLocalName()))) {
      throw new XMLStreamException("Expected a double-bond position element.");
    }
    final String positionText = reader.getAttributeValue(null, XML_POSITION);
    if (positionText == null) {
      throw new XMLStreamException("Missing double-bond position attribute.");
    }
    final DoubleBondGeometry geometry = UniqueIdSupplier.parseOrElse(
        reader.getAttributeValue(null, XML_GEOMETRY), DoubleBondGeometry.values(),
        DoubleBondGeometry.UNKNOWN);
    return new DoubleBondPosition(Integer.parseInt(positionText), geometry);
  }
}
