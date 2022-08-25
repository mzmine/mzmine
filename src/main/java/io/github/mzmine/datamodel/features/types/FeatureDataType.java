/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.datamodel.features.types;

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.IonMobilogramTimeSeriesFactory;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.RowBinding;
import io.github.mzmine.datamodel.features.types.modifiers.NoTextColumn;
import io.github.mzmine.datamodel.features.types.modifiers.NullColumnType;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FeatureDataType extends
    DataType<IonTimeSeries<? extends Scan>> implements NoTextColumn,
    NullColumnType {

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "feature_data";
  }

  @NotNull
  @Override
  public String getHeaderString() {
    return "Feature data";
  }

  @Override
  public ObjectProperty<IonTimeSeries<? extends Scan>> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public Class<IonTimeSeries<? extends Scan>> getValueClass() {
    return (Class) IonTimeSeries.class;
  }

  @NotNull
  @Override
  public List<RowBinding> createDefaultRowBindings() {
    // listen to changes in DataPointsType for all ModularFeatures
    return List.of();
  }

  @Override
  public void saveToXML(@NotNull final XMLStreamWriter writer, @Nullable final Object value,
      @NotNull final ModularFeatureList flist, @NotNull final ModularFeatureListRow row,
      @Nullable final ModularFeature feature, @Nullable final RawDataFile file)
      throws XMLStreamException {
    if(value == null) {
      return;
    }
    if (!(value instanceof IonTimeSeries series)) {
      throw new IllegalArgumentException(
          "Wrong value type for data type: " + this.getClass().getName() + " value class: " + value.getClass());
    }
    if (file == null) {
      throw new IllegalArgumentException("Cannot save feature data for file = null");
    }
    final List<? extends Scan> selectedScans = flist.getSeletedScans(file);
    if (selectedScans == null) {
      // sanity check during saving.
      throw new IllegalArgumentException("Cannot find selected scans.");
    }

    writer.writeStartElement(getUniqueID());
    series.saveValueToXML(writer, file.getScans()); // use ALL scans of the given raw data file.
    writer.writeEndElement();
  }

  @Override
  public Object loadFromXML(@NotNull final XMLStreamReader reader, @NotNull MZmineProject project,
      @NotNull final ModularFeatureList flist, @NotNull final ModularFeatureListRow row,
      @Nullable final ModularFeature feature, @Nullable final RawDataFile file)
      throws XMLStreamException {

    assert file != null;

    while (reader.hasNext()) {
      if (reader.isEndElement() && reader.getLocalName().equals(CONST.XML_DATA_TYPE_ELEMENT)) {
        // nothing saved
        return null;
      }
      if (reader.isStartElement() && (reader.getLocalName().equals(SimpleIonTimeSeries.XML_ELEMENT)
          || reader.getLocalName().equals(SimpleIonMobilogramTimeSeries.XML_ELEMENT))) {
        // found start element
        break;
      }
      reader.next();
    }

    switch (reader.getLocalName()) {
      case SimpleIonTimeSeries.XML_ELEMENT -> {
        return SimpleIonTimeSeries.loadFromXML(reader, flist.getMemoryMapStorage(), file);
      }
      case SimpleIonMobilogramTimeSeries.XML_ELEMENT -> {
        return IonMobilogramTimeSeriesFactory
            .loadFromXML(reader, flist.getMemoryMapStorage(), (IMSRawDataFile) file);
      }
    }
    return null;
  }
}
