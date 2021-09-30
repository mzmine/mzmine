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

package io.github.mzmine.datamodel.features.types;

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImsMsMsInfo;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ListDataType;
import io.github.mzmine.datamodel.impl.ImsMsMsInfoImpl;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import java.util.List;
import javafx.beans.property.ListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ImsMsMsInfoType extends ListDataType<ImsMsMsInfo> {

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "ion_mobility_msms_info";
  }

  @NotNull
  @Override
  public String getHeaderString() {
    return "ImsMsMsInfo";
  }

  @NotNull
  @Override
  public String getFormattedString(@NotNull ListProperty<ImsMsMsInfo> property) {
    return property.get() != null ? String.valueOf(property.get().size()) : "0";
  }

  @NotNull
  @Override
  public String getFormattedString(@Nullable Object value) {
    if (value instanceof ListProperty) {
      return String.valueOf(((ListProperty) value).size());
    }
    if (value instanceof List) {
      return String.valueOf(((List) value).size());
    }
    return "0";
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if (!(value instanceof List list)) {
      return;
    }

    for (Object o : list) {
      if (o instanceof ImsMsMsInfo info) {
        info.writeToXML(writer);
      }
    }

  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull ModularFeatureList flist,
      @NotNull ModularFeatureListRow row, @Nullable ModularFeature feature,
      @Nullable RawDataFile file) throws XMLStreamException {

    ObservableList<ImsMsMsInfo> infos = FXCollections.observableArrayList();

    while (reader.hasNext()) {
      reader.next();
      if (reader.isEndElement() && reader.getLocalName().equals(CONST.XML_DATA_TYPE_ELEMENT)) {
        break;
      }
      if (!reader.isStartElement()) {
        continue;
      }

      if (reader.getLocalName().equals(ImsMsMsInfoImpl.XML_ELEMENT)) {
        infos.add(ImsMsMsInfoImpl.loadFromXML(reader, (IMSRawDataFile) file));
      }
    }
    return infos;
  }
}
