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

package io.github.mzmine.datamodel.features.types.numbers;

import io.github.mzmine.datamodel.features.RowBinding;
import io.github.mzmine.datamodel.features.SimpleRowBinding;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.BindingsType;
import io.github.mzmine.datamodel.features.types.modifiers.ExpandableType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.FloatType;
import io.github.mzmine.main.MZmineCore;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Retention time type
 */
public class RTType extends FloatType implements ExpandableType {

  public RTType() {
    super(new DecimalFormat("0.00"));
  }

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "rt";
  }

  @Override
  public NumberFormat getFormatter() {
    try {
      return MZmineCore.getConfiguration().getRTFormat();
    } catch (NullPointerException e) {
      // only happens if types are used without initializing the MZmineCore
      return DEFAULT_FORMAT;
    }
  }

  @Override
  public @NotNull String getHeaderString() {
    return "RT";
  }

  @NotNull
  @Override
  public List<RowBinding> createDefaultRowBindings() {
    return List.of(new SimpleRowBinding(this, BindingsType.AVERAGE));
  }

  @NotNull
  @Override
  public Class<? extends DataType<?>> getExpandedTypeClass() {
    return RTRangeType.class;
  }

  @NotNull
  @Override
  public Class<? extends DataType<?>> getHiddenTypeClass() {
    return getClass();
  }
}
