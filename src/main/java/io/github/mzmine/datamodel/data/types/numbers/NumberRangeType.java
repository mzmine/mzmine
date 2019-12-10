/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.data.types.numbers;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.types.fx.DataTypeCellFactory;
import io.github.mzmine.datamodel.data.types.fx.DataTypeCellValueFactory;
import io.github.mzmine.datamodel.data.types.modifiers.SubColumnsFactory;
import io.github.mzmine.datamodel.data.types.numbers.abstr.NumberType;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;

public abstract class NumberRangeType<T extends Comparable<?>> extends
    NumberType<ObjectProperty<Range<T>>> implements SubColumnsFactory<ObjectProperty<Range<T>>> {

  protected NumberRangeType(NumberFormat defaultFormat) {
    super(defaultFormat);
  }

  @Override
  public abstract NumberFormat getFormatter();

  @Override
  @Nonnull
  public String getFormattedString(@Nonnull ObjectProperty<Range<T>> value) {
    return value.getValue() == null ? ""
        : getFormatter().format(value.getValue().lowerEndpoint()) + "-"
            + getFormatter().format(value.getValue().upperEndpoint());
  }

  @Override
  public ObjectProperty<Range<T>> createProperty() {
    return new SimpleObjectProperty<Range<T>>();
  }


  @Override
  @Nonnull
  public List<TreeTableColumn<ModularFeatureListRow, ObjectProperty<Range<T>>>> createSubColumns(
      @Nullable RawDataFile raw) {
    List<TreeTableColumn<ModularFeatureListRow, ObjectProperty<Range<T>>>> cols = new ArrayList<>();

    // create column per name
    TreeTableColumn<ModularFeatureListRow, ObjectProperty<Range<T>>> min =
        new TreeTableColumn<>("min");
    DataTypeCellValueFactory cvFactoryMin = new DataTypeCellValueFactory<>(raw, this);
    min.setCellValueFactory(cvFactoryMin);
    min.setCellFactory(new DataTypeCellFactory<>(raw, this, 0));

    TreeTableColumn<ModularFeatureListRow, ObjectProperty<Range<T>>> max =
        new TreeTableColumn<>("max");
    DataTypeCellValueFactory cvFactoryMax = new DataTypeCellValueFactory<>(raw, this);
    max.setCellValueFactory(cvFactoryMax);
    max.setCellFactory(new DataTypeCellFactory<>(raw, this, 1));

    // add all
    cols.add(min);
    cols.add(max);

    return cols;
  }

  @Override
  @Nullable
  public String getFormattedSubColValue(int subcolumn,
      TreeTableCell<ModularFeatureListRow, ObjectProperty<Range<T>>> cell,
      TreeTableColumn<ModularFeatureListRow, ObjectProperty<Range<T>>> coll,
      ObjectProperty<Range<T>> value, RawDataFile raw) {
    if (value.getValue() == null)
      return "";
    switch (subcolumn) {
      case 0:
        return getFormatter().format(value.getValue().lowerEndpoint());
      case 1:
        return getFormatter().format(value.getValue().upperEndpoint());
    }
    return "";
  }

}
