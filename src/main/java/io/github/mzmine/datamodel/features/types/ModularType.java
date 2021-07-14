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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.features.types;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ModularType offers a main column for multiple sub columns (DataTypes) that are stored in a {@link ModularTypeProperty}
 * An example implementation is given by {@link SpectralLibraryMatchType}, which is extending the scope with one column
 * (type) that defines all the other columns.
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public abstract class ModularType extends DataType<ModularTypeProperty>
        implements SubColumnsFactory<ModularTypeProperty> {

  private ObservableMap<Class<? extends DataType>, DataType> subTypeMap;

  /**
   * The unmodifiable list of sub data types. Order reflects the initial order of columns.
   *
   * @return
   */
  @NotNull
  public abstract List<DataType> getSubDataTypes();

  /**
   * The unmodifiable map as a reference for the ModularTypeProperty
   * @return
   */
  @NotNull
  public ObservableMap<Class<? extends DataType>, DataType> getSubDataTypesMap() {
    if(subTypeMap==null) {
      LinkedHashMap<Class<? extends DataType>, DataType> map = new LinkedHashMap<>();
      getSubDataTypes().forEach(type -> map.put(type.getClass(), type));
      subTypeMap = FXCollections.unmodifiableObservableMap(FXCollections.observableMap(map));
    }
    return subTypeMap;
  }

  @Override
  public ModularTypeProperty createProperty() {
    // create map of datatype -> property for all sub types
    LinkedHashMap<DataType, Property<?>> map = new LinkedHashMap<>();
    getSubDataTypes().stream().forEach(t -> map.put(t, t.createProperty()));
    return new ModularTypeProperty(FXCollections.unmodifiableObservableMap(FXCollections.observableMap(map)), this);
  }

  @Override
  @NotNull
  public String getFormattedString(@NotNull ModularTypeProperty value) {
    ObservableMap<DataType, Property<?>> map = value.getValue();
    return map == null || map.isEmpty() ? "" : map.entrySet().stream()
            .map(e -> e.getKey().getFormattedString(e.getValue())).collect(Collectors.joining(";"));
  }

  @Override
  @NotNull
  public List<TreeTableColumn<ModularFeatureListRow, Object>> createSubColumns(@Nullable RawDataFile raw) {
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
    return index>=0 && index<getSubDataTypes().size()? getSubDataTypes().get(index) : null;
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
    if(subcolumn>=0 && subcolumn<list.size())
    return list.get(subcolumn).getHeaderString();
    else throw new IndexOutOfBoundsException("Sub column index "+subcolumn+" is out of range "+list.size());
  }

  @Override
  @Nullable
  public String getFormattedSubColValue(int subcolumn,
                                        TreeTableCell<ModularFeatureListRow, Object> cell,
                                        TreeTableColumn<ModularFeatureListRow, Object> coll, Object value, RawDataFile raw) {
    if (value == null)
      return "";
    DataType sub = getSubTypeAt(subcolumn);
    if(sub==null)
      return "";

    Map<DataType, Property<?>> map = (Map<DataType, Property<?>>) value;
    return sub.getFormattedString(map.get(sub));
  }

  @Nullable
  @Override
  public Node getSubColNode(int subcolumn, TreeTableCell<ModularFeatureListRow, Object> cell, TreeTableColumn<ModularFeatureListRow, Object> coll, Object cellData, RawDataFile raw) {
    if (cellData == null)
      return null;
    DataType sub = getSubTypeAt(subcolumn);
    if(sub==null || !(sub instanceof GraphicalColumType))
      return null;

    Map<DataType, Property<?>> map = (Map<DataType, Property<?>>) cellData;
    return ((GraphicalColumType)sub).getCellNode(cell, coll, cellData, raw);
  }

}
