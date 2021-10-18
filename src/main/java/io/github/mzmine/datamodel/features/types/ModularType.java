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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.annotations.SpectralLibraryMatchType;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.modules.io.projectload.version_3_0.DataTypes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * ModularType offers a main column for multiple sub columns (DataTypes) that are stored in a {@link
 * ModularTypeMap} An example implementation is given by {@link SpectralLibraryMatchType}, which is
 * extending the scope with one column (type) that defines all the other columns.
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public abstract class ModularType extends DataType<ModularTypeMap> implements
    SubColumnsFactory<ModularTypeMap> {

  private static final Logger logger = Logger.getLogger(ModularType.class.getName());

  private ObservableMap<Class<? extends DataType>, DataType> subTypeMap;


  @Override
  public Class<ModularTypeMap> getValueClass() {
    return ModularTypeMap.class;
  }

  /**
   * Create the empty value map
   *
   * @return
   */
  public ModularTypeMap createMap() {
    return new ModularTypeMap(this);
  }

  /**
   * The unmodifiable list of sub data types. Order reflects the initial order of columns.
   *
   * @return
   */
  @NotNull
  public abstract List<DataType> getSubDataTypes();

  /**
   * The unmodifiable map as a reference for the ModularTypeProperty
   *
   * @return
   */
  @NotNull
  public ObservableMap<Class<? extends DataType>, DataType> getSubDataTypesMap() {
    if (subTypeMap == null) {
      LinkedHashMap<Class<? extends DataType>, DataType> map = new LinkedHashMap<>();
      getSubDataTypes().forEach(type -> map.put(type.getClass(), type));
      subTypeMap = FXCollections.unmodifiableObservableMap(FXCollections.observableMap(map));
    }
    return subTypeMap;
  }

  @Override
  public Property<ModularTypeMap> createProperty() {
    // create map of datatype -> property for all sub types
    return new SimpleObjectProperty<>();
  }

  @Override
  @NotNull
  public String getFormattedString(@NotNull ModularTypeMap value) {
    return value.stream().map(e -> e.getKey().getFormattedString(e.getValue()))
        .collect(Collectors.joining(";"));
  }

  @Override
  @NotNull
  public List<TreeTableColumn<ModularFeatureListRow, Object>> createSubColumns(
      @Nullable RawDataFile raw) {
    final ModularType thisType = this;
    // add column for each sub data type
    List<TreeTableColumn<ModularFeatureListRow, Object>> cols = new ArrayList<>();

    for (DataType subType : getSubDataTypes()) {
      // create column and replace value factory to access sub type
      TreeTableColumn<ModularFeatureListRow, Object> col = subType.createColumn(raw, thisType);
      cols.add(col);
    }
    return cols;
  }

  /**
   * Sub DataType is the sub column at index (see {@link #getSubDataTypes()#})
   *
   * @param index
   * @return
   */
  public DataType getSubTypeAt(int index) {
    return index >= 0 && index < getSubDataTypes().size() ? getSubDataTypes().get(index) : null;
  }


  @NotNull
  @Override
  public int getNumberOfSubColumns() {
    return getSubDataTypes().size();
  }

  @Nullable
  @Override
  public String getHeader(int subcolumn) {
    List<DataType> list = getSubDataTypes();
    if (subcolumn >= 0 && subcolumn < list.size()) {
      return list.get(subcolumn).getHeaderString();
    } else {
      throw new IndexOutOfBoundsException(
          "Sub column index " + subcolumn + " is out of range " + list.size());
    }
  }

  @Override
  @Nullable
  public String getFormattedSubColValue(int subcolumn,
      TreeTableCell<ModularFeatureListRow, Object> cell,
      TreeTableColumn<ModularFeatureListRow, Object> coll, Object value, RawDataFile raw) {

    if (value == null) {
      return "";
    }

    DataType sub = getSubTypeAt(subcolumn);
    if (sub == null) {
      return "";
    }

    Map<DataType, Property<?>> map = (Map<DataType, Property<?>>) value;
    return sub.getFormattedString(map.get(sub));
  }

  @Nullable
  @Override
  public Node getSubColNode(int subcolumn, TreeTableCell<ModularFeatureListRow, Object> cell,
      TreeTableColumn<ModularFeatureListRow, Object> coll, Object cellData, RawDataFile raw) {
    if (cellData == null) {
      return null;
    }
    DataType sub = getSubTypeAt(subcolumn);
    if (sub == null || !(sub instanceof GraphicalColumType)) {
      return null;
    }

    Map<DataType, Property<?>> map = (Map<DataType, Property<?>>) cellData;
    return ((GraphicalColumType) sub).getCellNode(cell, coll, cellData, raw);
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if (value == null) {
      return;
    }
    if (!(value instanceof Map map)) {
      throw new IllegalArgumentException(
          "Wrong value type for data type: " + this.getClass().getName() + " value class: "
          + value.getClass());
    }

    for (Object obj : map.entrySet()) {
      if (!(obj instanceof Entry entry)) {
        continue;
      }
      Object key = entry.getKey();
      Object val = entry.getValue();
      if (val instanceof Property) {
        val = ((Property<?>) val).getValue();
      }

      if (!(key instanceof DataType dt)) {
        return;
      }

      writer.writeStartElement(CONST.XML_DATA_TYPE_ELEMENT);
      writer.writeAttribute(CONST.XML_DATA_TYPE_ID_ATTR, dt.getUniqueID());

      try { // catch here, so we can easily debug and don't destroy the flist while saving in case an unexpected exception happens
        dt.saveToXML(writer, val, flist, row, feature, file);
      } catch (XMLStreamException e) {
        final Object finalVal = val;
        logger.warning(
            () -> "Error while writing data type " + dt.getClass().getSimpleName() + " with value "
                  + finalVal + " to xml.");
        e.printStackTrace();
      }

      writer.writeEndElement();
    }
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull ModularFeatureList flist,
      @NotNull ModularFeatureListRow row, @Nullable ModularFeature feature,
      @Nullable RawDataFile file) throws XMLStreamException {

    int parsed = 0;

    ModularTypeMap dataMap = createMap();
    while (reader.hasNext()) {
      int next = reader.next();

      if (next == XMLEvent.END_ELEMENT && reader.getLocalName()
          .equals(CONST.XML_DATA_TYPE_ELEMENT)) {
        // TODO check if end element is from parent type
        break;
      }
      if (reader.isStartElement() && reader.getLocalName().equals(CONST.XML_DATA_TYPE_ELEMENT)) {
        DataType type = DataTypes.getTypeForId(
            reader.getAttributeValue(null, CONST.XML_DATA_TYPE_ID_ATTR));
        Object o = type.loadFromXML(reader, flist, row, feature, file);
        dataMap.set(type, o);
        parsed++;
      }
    }

    return dataMap;
  }
}
