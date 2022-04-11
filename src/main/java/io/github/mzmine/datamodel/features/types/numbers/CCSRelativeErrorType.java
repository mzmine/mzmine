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

package io.github.mzmine.datamodel.features.types.numbers;

import io.github.mzmine.datamodel.features.types.numbers.abstr.FloatType;
import io.github.mzmine.gui.preferences.UnitFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import org.jetbrains.annotations.NotNull;

public class CCSRelativeErrorType extends FloatType {

  private static final NumberFormat defaultFormat = new DecimalFormat("0.00 %");
  private static final String headerString = UnitFormat.DIVIDE.format("\u0394 CCS", "%");

  public CCSRelativeErrorType() {
    super(defaultFormat);
  }

  @Override
  public @NotNull String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "ccs_percent_error";
  }

  @Override
  public @NotNull String getHeaderString() {
    return headerString;
  }

  @Override
  public NumberFormat getFormatter() {
    return defaultFormat;
  }
}
