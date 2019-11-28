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
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.types.DataType;
import io.github.mzmine.datamodel.data.types.fx.DataTypeCellFactory;
import io.github.mzmine.datamodel.data.types.fx.RawsMapCellValueFactory;
import io.github.mzmine.datamodel.data.types.modifiers.SubColumnsFactory;
import javafx.scene.control.TreeTableColumn;

public abstract class NumberRangeType<T extends Comparable<?>> extends DataType<Range<T>>
    implements SubColumnsFactory {

  public NumberRangeType(Range<T> value) {
    super(value);
  }

  public abstract NumberFormat getFormatter();

  @Override
  @Nonnull
  public String getFormattedString() {
    return value == null ? ""
        : getFormatter().format(value.lowerEndpoint()) + "-"
            + getFormatter().format(value.upperEndpoint());
  }

  @Override
  @Nonnull
  public List<TreeTableColumn<ModularFeatureListRow, ?>> createSubColumns() {
    List<TreeTableColumn<ModularFeatureListRow, ?>> cols = new ArrayList<>();

    // create column per name
    TreeTableColumn<ModularFeatureListRow, NumberRangeType<?>> min = new TreeTableColumn<>("min");
    min.setCellValueFactory(new RawsMapCellValueFactory<>(null, this.getClass()));
    min.setCellFactory(new DataTypeCellFactory<>(null, this.getClass(), 0));

    TreeTableColumn<ModularFeatureListRow, NumberRangeType<?>> max = new TreeTableColumn<>("max");
    max.setCellValueFactory(new RawsMapCellValueFactory<>(null, this.getClass()));
    max.setCellFactory(new DataTypeCellFactory<>(null, this.getClass(), 1));

    // add all
    cols.add(min);
    cols.add(max);

    return cols;
  }

  @Override
  @Nullable
  public String getFormattedSubColValue(int subcolumn) {
    if (value == null)
      return "";
    switch (subcolumn) {
      case 0:
        return getFormatter().format(value.lowerEndpoint());
      case 1:
        return getFormatter().format(value.upperEndpoint());
    }
    return "";
  }

}
