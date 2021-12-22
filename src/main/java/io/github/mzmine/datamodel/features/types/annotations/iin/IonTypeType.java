/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.datamodel.features.types.annotations.iin;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Specifies an ion type.
 * Todo: remove {@link io.github.mzmine.datamodel.features.types.annotations.iin.IonAdductType}, rename this?
 */
public class IonTypeType extends DataType<IonType> {

  @Override
  public @NotNull String getUniqueID() {
    return "ion_type_type";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Adduct";
  }

  @Override
  public Property<IonType> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public Class<IonType> getValueClass() {
    return IonType.class;
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if (!(value instanceof IonType ionType)) {
      return;
    }
    ionType.saveToXML(writer);
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull ModularFeatureList flist,
      @NotNull ModularFeatureListRow row, @Nullable ModularFeature feature,
      @Nullable RawDataFile file) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(CONST.XML_DATA_TYPE_ELEMENT)
        && reader.getAttributeValue(null, CONST.XML_DATA_TYPE_ID_ATTR).equals(getUniqueID()))) {
      throw new IllegalStateException("Current type is not a ion type.");
    }

    while (reader.hasNext() && !(reader.isStartElement() && reader.getLocalName()
        .equals(IonType.XML_ELEMENT))) {
      reader.next();
      if (reader.isEndElement() && reader.getLocalName().equals(CONST.XML_DATA_TYPE_ELEMENT)) {
        return null;
      }
    }

    return IonType.loadFromXML(reader);
  }

  @Override
  public @NotNull String getFormattedString(IonType value) {
    if (value == null) {
      return "";
    }
    return value.toString(false);
  }
}
