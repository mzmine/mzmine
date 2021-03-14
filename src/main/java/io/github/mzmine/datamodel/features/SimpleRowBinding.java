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

package io.github.mzmine.datamodel.features;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.BindingsFactoryType;
import io.github.mzmine.datamodel.features.types.modifiers.BindingsType;
import javafx.beans.binding.ObjectBinding;

/**
 * Create standard RowBindings for AVERAGE, SUM, MAX, MIN, RANGES, etc Specific {@link DataType}s
 * define the binding
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class SimpleRowBinding implements RowBinding {

  private final DataType rowType;
  private final BindingsFactoryType featureType;
  private final BindingsType bindingType;

  public SimpleRowBinding(BindingsFactoryType featureAndRowType, BindingsType bindingType) {
    this((DataType) featureAndRowType, featureAndRowType, bindingType);
  }

  public SimpleRowBinding(DataType rowType, BindingsFactoryType featureType,
      BindingsType bindingType) {
    super();
    assert featureType instanceof DataType : "feature type needs to be a DataType";
    this.rowType = rowType;
    this.featureType = featureType;
    this.bindingType = bindingType;
  }

  @Override
  public void apply(ModularFeatureListRow row) {
    if (row.get(rowType) != null) {
      ObjectBinding<?> binding = featureType.createBinding(bindingType, row);
      row.get(rowType).bind(binding);
    }
  }

  @Override
  public DataType getRowType() {
    return rowType;
  }

  @Override
  public DataType getFeatureType() {
    return (DataType) featureType;
  }
}
