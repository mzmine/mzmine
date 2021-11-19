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

import io.github.mzmine.datamodel.features.types.DataType;
import org.jetbrains.annotations.NotNull;

/**
 * DataType, which is either hidden or expanded, must have a buddy type, which is either expanded or
 * hidden respectively. This interface enables to create expandable columns in a feature list table.
 */
public interface ExpandableType {

  @NotNull
  Class<? extends DataType<?>> getExpandedTypeClass();

  @NotNull
  Class<? extends DataType<?>> getHiddenTypeClass();

  default boolean isHiddenType() {
    return getClass().equals(getHiddenTypeClass());
  }

  default boolean isExpandedType() {
    return getClass().equals(getExpandedTypeClass());
  }

  @NotNull
  default Class<? extends DataType<?>> getBuddyTypeClass() {
    return isExpandedType() ? getHiddenTypeClass() : getExpandedTypeClass();
  }

  default Character getSymbol() {
    return isExpandedType() ? '▾' : '▸';
  }

}
