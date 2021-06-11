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

import io.github.mzmine.datamodel.features.RowGroup;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Level of feature grouping
 */
public class FeatureGroupType extends DataType<ObjectProperty<RowGroup>> {

  @Override
  @Nonnull
  public String getHeaderString() {
    return "Group";
  }

  @Override
  public ObjectProperty<RowGroup> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Nonnull
  @Override
  public String getFormattedString(@Nullable Object value) {
    if (value instanceof RowGroup group) {
      return String.valueOf(group.getGroupID());
    }
    return super.getFormattedString(value);
  }

  public enum GroupType {
    CORRELATED, ISOTOPES, ION_ADDUCTS
  }
}