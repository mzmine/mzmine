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

package io.github.mzmine.datamodel.features.compoundannotations;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.DatabaseNameType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonAdductType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;

public interface CompoundDBAnnotation {

  String XML_ELEMENT = "compound_db_annotation";
  String XML_TYPE_ATTRIBUTE = "annotationtype";
  String XML_NUM_ENTRIES_ATTR = "entries";

  <T> T get(@NotNull DataType<T> key);

  <T> T put(@NotNull DataType<T> key, T value);

  void saveToXML(@NotNull XMLStreamWriter writer, ModularFeatureList flist,
      ModularFeatureListRow row) throws XMLStreamException;

  static CompoundDBAnnotation loadFromXML(@NotNull final XMLStreamReader reader,
      @NotNull final ModularFeatureList flist, @NotNull final ModularFeatureListRow row)
      throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      throw new IllegalStateException("Invalid xml element to load CompoundDBAnnotation from.");
    }

    return switch (reader.getAttributeValue(null, XML_TYPE_ATTRIBUTE)) {
      case SimpleCompoundDBAnnotation.XML_TYPE_NAME -> SimpleCompoundDBAnnotation.loadFromXML(
          reader, flist, row);
//      case BioTransformerAnnotationImpl.XML_TYPE_NAME -> BioTransformerAnnotationImpl.loadFromXML(
//          reader, flist, row);
      default -> null;
    };
  }

  public default Double getExactMass() {
    return get(new PrecursorMZType());
  }

  public default String getSmiles() {
    return get(new SmilesStructureType());
  }

  public default String getCompundName() {
    return get(new CompoundNameType());
  }

  public default String getFormula() {
    return get(new FormulaType());
  }

  public default String getAdductType() {
    return get(new IonAdductType());
  }

  public default Float getMobility() {
    return get(new MobilityType());
  }

  public default Float getCCS() {
    return get(new CCSType());
  }

  public default String getDatabase() {
    return get(new DatabaseNameType());
  }
}
