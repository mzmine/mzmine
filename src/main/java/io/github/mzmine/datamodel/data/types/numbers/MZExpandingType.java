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

package io.github.mzmine.datamodel.data.types.numbers;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.types.DataType;
import io.github.mzmine.datamodel.data.types.fx.DataTypeCellFactory;
import io.github.mzmine.datamodel.data.types.fx.DataTypeCellValueFactory;
import io.github.mzmine.datamodel.data.types.modifiers.ExpandingState;
import io.github.mzmine.datamodel.data.types.modifiers.ExpandingType;
import io.github.mzmine.main.MZmineCore;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.util.Pair;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

// TODO: rethink declaration of classes implementing ExpandingType to use abstract DataTypes
public class MZExpandingType extends DataType<ObjectProperty<Pair<Double, Range<Double>>>>
    implements ExpandingType<Double, Range<Double>> {

  private ExpandingState state = ExpandingState.HIDDEN;

  public MZExpandingType() {
  }

  public NumberFormat getFormatter() {
    try {
      return MZmineCore.getConfiguration().getMZFormat();
    } catch (NullPointerException e) {
      // only happens if types are used without initializing the MZmineCore
      return new DecimalFormat("0.0000");
    }
  }

  @Override
  @Nonnull
  public String getHeaderString() {
    // create static method in ExpandingType for ▼ and ▶
    return state == ExpandingState.EXPANDED ?
        ExpandingType.getExpandedSymbol() + " m/z Range" :
        ExpandingType.getHiddenSymbol() + " m/z";
  }

  @Override
  public ObjectProperty<Pair<Double, Range<Double>>> createProperty() {
    return new SimpleObjectProperty<Pair<Double, Range<Double>>>();
  }

  @Override
  public Boolean isExpanded() {
    return state == ExpandingState.EXPANDED;
  }

  @Override
  public Boolean isHidden() {
    return state == ExpandingState.HIDDEN;
  }

  @Override
  public void invertState() {
    state = state == ExpandingState.EXPANDED ? ExpandingState.HIDDEN : ExpandingState.EXPANDED;
  }

  @Override
  @Nonnull
  public List<TreeTableColumn<ModularFeatureListRow, Object>> createSubColumns(
      @Nullable RawDataFile raw) {
    if(state == ExpandingState.HIDDEN) {
      return Collections.emptyList();
    }

    List<TreeTableColumn<ModularFeatureListRow, Object>> cols = new ArrayList<>();

    // create column per name
    TreeTableColumn<ModularFeatureListRow, Object> min = new TreeTableColumn<>("min");
    DataTypeCellValueFactory cvFactoryMin = new DataTypeCellValueFactory(raw, this);
    min.setCellValueFactory(cvFactoryMin);
    min.setCellFactory(new DataTypeCellFactory(raw, this, 0));

    TreeTableColumn<ModularFeatureListRow, Object> max = new TreeTableColumn<>("max");
    DataTypeCellValueFactory cvFactoryMax = new DataTypeCellValueFactory(raw, this);
    max.setCellValueFactory(cvFactoryMax);
    max.setCellFactory(new DataTypeCellFactory(raw, this, 1));

    // add all
    cols.add(min);
    cols.add(max);

    return cols;
  }

  @Override
  @Nullable
  public String getFormattedSubColValue(int subcolumn,
      TreeTableCell<ModularFeatureListRow, Object> cell,
      TreeTableColumn<ModularFeatureListRow, Object> coll, Object value, RawDataFile raw) {
    if (value == null)
      return "";
    switch (subcolumn) {
      case 0:
        return getFormatter().format(((Range)((Pair) value).getValue()).lowerEndpoint());
      case 1:
        return getFormatter().format(((Range)((Pair) value).getValue()).upperEndpoint());
    }
    return "";
  }

  @Override
  @Nonnull
  public String getFormattedString(@Nullable Object value) {
    return getFormatter().format(((Pair)value).getKey());
  }
}
