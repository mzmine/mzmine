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

package io.github.mzmine.datamodel.features;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.BindingsType;
import org.jetbrains.annotations.NotNull;

/**
 * Create standard RowBindings for AVERAGE, SUM, MAX, MIN, RANGES, etc Specific {@link DataType}s
 * define the binding
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class SimpleRowBinding implements RowBinding {

  private final @NotNull DataType rowType;
  private final @NotNull DataType featureType;
  private final @NotNull BindingsType bindingType;

  public SimpleRowBinding(@NotNull DataType<?> featureAndRowType,
      @NotNull BindingsType bindingType) {
    this(featureAndRowType, featureAndRowType, bindingType);
  }

  public SimpleRowBinding(@NotNull DataType<?> rowType, @NotNull DataType<?> featureType,
      @NotNull BindingsType bindingType) {
    super();
    this.rowType = rowType;
    this.featureType = featureType;
    this.bindingType = bindingType;
  }

  @Override
  public void apply(FeatureListRow row) {
    // row might be null if the feature was not yet added
    if (row != null) {
      Object value = featureType.evaluateBindings(bindingType, row.getFeatures());
      row.set(rowType, value);
    }
  }

  @Override
  public DataType getRowType() {
    return rowType;
  }

  @Override
  public DataType getFeatureType() {
    return featureType;
  }

  @Override
  public void valueChanged(ModularDataModel dataModel, DataType type, Object oldValue,
      Object newValue) {
    if (dataModel instanceof Feature feature) {
      // change in feature applied to its row
      apply(feature.getRow());
    } else {
      throw new UnsupportedOperationException(
          "Cannot apply a SimpleRowBinding if the changed data model is not a Feature");
    }
  }
}
