/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.data.types.numbers;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import javax.annotation.Nonnull;
import io.github.mzmine.datamodel.data.types.numbers.abstr.DoubleRangeType;
import io.github.mzmine.main.MZmineCore;

public class MZRangeType extends DoubleRangeType {

  public MZRangeType() {
    super(new DecimalFormat("0.0000"));
  }

  @Override
  public NumberFormat getFormatter() {
    try {
      return MZmineCore.getConfiguration().getMZFormat();
    } catch (NullPointerException e) {
      // only happens if types are used without initializing the MZmineCore
      return DEFAULT_FORMAT;
    }
  }

  @Override
  @Nonnull
  public String getHeaderString() {
    return "m/z Range";
  }

}
