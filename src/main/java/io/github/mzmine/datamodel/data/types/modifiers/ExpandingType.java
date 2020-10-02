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

package io.github.mzmine.datamodel.data.types.modifiers;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Data type having two states: expanded and hidden. Expanded state type must provide subcolumns.
 *
 * @param <T> type corresponding to values of hidden state
 * @param <S> type corresponding to values of expanded state
 */
public interface ExpandingType<T, S> extends SubColumnsFactory<ObjectProperty<S>> {

  static String getExpandedSymbol() {
    return "▼";
  }

  static String getHiddenSymbol() {
    return "▶";
  }

  Boolean isExpanded();

  Boolean isHidden();

  void invertState();

  @Override
  @Nonnull
  List<TreeTableColumn<ModularFeatureListRow, Object>> createSubColumns(
      final @Nullable RawDataFile raw);

  @Override
  @Nullable
  String getFormattedSubColValue(int subcolumn,
      TreeTableCell<ModularFeatureListRow, Object> cell,
      TreeTableColumn<ModularFeatureListRow, Object> coll, Object cellData, RawDataFile raw);


  @Override
  @Nullable
  default Node getSubColNode(int subcolumn,
      TreeTableCell<ModularFeatureListRow, Object> cell,
      TreeTableColumn<ModularFeatureListRow, Object> coll, Object cellData, RawDataFile raw) {
    return null;
  }
}
