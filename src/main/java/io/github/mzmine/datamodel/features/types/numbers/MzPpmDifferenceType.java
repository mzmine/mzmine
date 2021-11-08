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

import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaListType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.FloatType;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import org.jetbrains.annotations.NotNull;

/**
 * Relative m/z difference in ppm (parts-per-million: 10^-6), e.g., used in {@link FormulaListType}
 * to describe the difference between the measured (accurate) and the calculated (exact) m/z
 */
public class MzPpmDifferenceType extends FloatType {

  public MzPpmDifferenceType() {
    super(new DecimalFormat("0.000"));
  }

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "mz_diff_ppm";
  }

  @Override
  public @NotNull String getHeaderString() {
    // Delta
    return "\u0394 m/z ppm";
  }

  @Override
  public NumberFormat getFormatter() {
    return DEFAULT_FORMAT;
  }
}
