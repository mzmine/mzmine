/*
 * Copyright 2006-2022 The MZmine Development Team
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
 */

package io.github.mzmine.datamodel.features.types.numbers;

import io.github.mzmine.datamodel.features.types.numbers.abstr.FloatType;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import org.jetbrains.annotations.NotNull;

public class PotentialType extends FloatType {

  private static final DecimalFormat format = new DecimalFormat("0.00");

  public PotentialType() {
    super(format);
  }

  @Override
  public @NotNull String getUniqueID() {
    return "ec_ms_potential";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Potential / V";
  }

  @Override
  public NumberFormat getFormatter() {
    return format;
  }
}
