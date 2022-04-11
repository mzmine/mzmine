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

package io.github.mzmine.datamodel.features.types.modifiers;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import java.util.List;
import javafx.scene.control.TreeTableColumn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This data type contains sub columns. Master column is not visualized. Only sub columns
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public interface SubColumnsFactory {

  /**
   * Creates sub columns which are then added to the parent column by the parent datatype
   *
   * @return list of sub columns
   */
  @NotNull List<TreeTableColumn<ModularFeatureListRow, Object>> createSubColumns(
      final @Nullable RawDataFile raw, final @Nullable SubColumnsFactory parentType);

  int getNumberOfSubColumns();

  @Nullable String getHeader(int subcolumn);

  /**
   * The unique ID in a machine readable format
   *
   * @param subcolumn
   * @return parsable format of ID
   */
  @Nullable String getUniqueID(int subcolumn);

  /**
   * The data type of the subcolumn
   *
   * @param subcolumn index of subcolumn
   * @return datatype of subcolumn
   */
  @NotNull DataType<?> getType(int subcolumn);

  @Nullable
  String getFormattedSubColValue(int subcolumn, Object cellData);

  @Nullable
  Object getSubColValue(int subcolumn, Object cellData);

  /**
   * Handle value change in this parent type
   *
   * @param model          original data model that holds the parent Type (this)
   * @param subType        the sub type that was changed
   * @param subColumnIndex the index of the sub column that was changed in this parent type
   * @param newValue       the new value for the subType in this parent type
   * @param <T>            value type of DataType
   */
  default <T> void valueChanged(ModularDataModel model, DataType<T> subType, int subColumnIndex,
      T newValue) {
  }

}
