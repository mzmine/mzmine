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

package io.github.mzmine.datamodel.features.types.numbers;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.NullColumnType;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BestScanNumberType extends DataType<ObjectProperty<Scan>> implements NullColumnType {

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "best_ms1_scan_number";
  }

  @Override
  public String getHeaderString() {
    return "Best scan";
  }

  @Override
  public ObjectProperty<Scan> createProperty() {
    return new SimpleObjectProperty<Scan>();
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if (!(value instanceof Scan scan)) {
      return;
    }

    String name = scan.getDataFile().getName();
    writer.writeAttribute(CONST.XML_RAW_FILE_ELEMENT, name);
    writer.writeAttribute(CONST.XML_RAW_FILE_SCAN_INDEX_ATTR,
        String.valueOf(scan.getDataFile().getScans().indexOf(scan)));
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull ModularFeatureList flist,
      @NotNull ModularFeatureListRow row, @Nullable ModularFeature feature,
      @Nullable RawDataFile file) throws XMLStreamException {

    final String name = reader.getAttributeValue(null, CONST.XML_RAW_FILE_ELEMENT);
    final String strIndex = reader.getAttributeValue(null, CONST.XML_RAW_FILE_SCAN_INDEX_ATTR);
    if(strIndex == null) {
      return null;
    }

    final int index = Integer.parseInt(strIndex);

    if (file != null && !file.getName().equals(name)) {
      throw new IllegalArgumentException("File names don't match");
    }
    if (file == null) {
      file = flist.getRawDataFiles().stream().filter(f -> f.getName().equals(name)).findFirst()
          .orElse(null);
    }
    if (file == null) {
      throw new IllegalArgumentException("Raw data file not found");
    }

    return file.getScan(index);
  }
}
