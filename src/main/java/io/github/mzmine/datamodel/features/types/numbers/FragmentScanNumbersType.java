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

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.RowBinding;
import io.github.mzmine.datamodel.features.SimpleRowBinding;
import io.github.mzmine.datamodel.features.types.modifiers.BindingsType;
import io.github.mzmine.datamodel.impl.SimpleMergedMsMsSpectrum;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FragmentScanNumbersType extends ScanNumbersType {

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "fragment_scan_number_list";
  }

  @Override
  public String getHeaderString() {
    return "Fragment scans";
  }

  @NotNull
  @Override
  public List<RowBinding> createDefaultRowBindings() {
    return List.of(new SimpleRowBinding(this, BindingsType.CONSENSUS));
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {

    if (!(value instanceof List<?> list)) {
      return;
    }

    for (Object o : list) {
      if (!(o instanceof Scan scan)) {
        throw new IllegalArgumentException("Not a fragment scan");
      }
      if (scan instanceof MergedMsMsSpectrum merged) {
        merged.saveToXML(writer);
      } else {
        writer.writeStartElement(CONST.XML_RAW_FILE_SCAN_ELEMENT);
        String name = scan.getDataFile().getName();
        writer.writeAttribute(CONST.XML_RAW_FILE_ELEMENT, name);
        writer.writeAttribute(CONST.XML_RAW_FILE_SCAN_INDEX_ATTR,
            String.valueOf(scan.getDataFile().getScans().indexOf(scan)));

        writer.writeEndElement();
      }
    }

  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull ModularFeatureList flist,
      @NotNull ModularFeatureListRow row, @Nullable ModularFeature feature,
      @Nullable RawDataFile file) throws XMLStreamException {

    ObservableList<Scan> msmsSpectra = FXCollections.observableArrayList();
    final RawDataFile beginFile = file;

    while (reader.hasNext()) {
      reader.next();
      if (reader.isEndElement() && reader.getLocalName().equals(CONST.XML_DATA_TYPE_ELEMENT)) {
        return null;
      }
      if (!reader.isStartElement()) {
        continue;
      }

      file = beginFile; // reset the file. In case of loading the row type, this might have been set to a different file below

      switch (reader.getLocalName()) {
        case CONST.XML_RAW_FILE_SCAN_ELEMENT -> {
          final String name = reader.getAttributeValue(null, CONST.XML_RAW_FILE_ELEMENT);
          final int index = Integer
              .parseInt(reader.getAttributeValue(null, CONST.XML_RAW_FILE_SCAN_INDEX_ATTR));

          if (file != null && !file.getName().equals(name)) {
            throw new IllegalArgumentException("File names don't match");
          }
          if (file == null) {
            file = flist.getRawDataFiles().stream().filter(f -> f.getName().equals(name))
                .findFirst().orElse(null);
          }
          if (file == null) {
            throw new IllegalArgumentException("Raw data file not found");
          }

          msmsSpectra.add(file.getScan(index));
        }
        case SimpleMergedMsMsSpectrum.XML_ELEMENT -> {
          String name = reader.getAttributeValue(null, CONST.XML_RAW_FILE_ELEMENT);
          if (file != null && !file.getName().equals(name)) {
            throw new IllegalArgumentException("File names don't match");
          }
          if (file == null) {
            file = flist.getRawDataFiles().stream().filter(f -> f.getName().equals(name))
                .findFirst().orElse(null);
          }
          if (file == null) {
            throw new IllegalArgumentException("Raw data file not found");
          }
          msmsSpectra.add(SimpleMergedMsMsSpectrum.loadFromXML(reader, (IMSRawDataFile) file));
        }
      }
    }

    return msmsSpectra;
  }
}
