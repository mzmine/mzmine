/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.parameters.parametertypes.massdefect;

public class MassDefectFilter {

  public static final MassDefectFilter ALL = new MassDefectFilter(0, 1) {
    @Override
    public boolean contains(double mz) {
      return true;
    }
  };

  private final double lower;
  private final double upper;

  public MassDefectFilter(double lower, double upper) {

    if (lower < 0 || lower > 1) {
      throw new IllegalArgumentException("Lower mass defect range must be between 0 and 1.");
    }
    if (upper < 0 || upper > 1) {
      throw new IllegalArgumentException("Upper mass defect range must be between 0 and 1.");
    }

    this.lower = lower;
    this.upper = upper;
  }

  public boolean contains(final double mz) {
    final double massDefect = mz - Math.floor(mz);
    if (lower > upper) { // 0.90 - 0.15
      final boolean massDefectOk =
          (massDefect >= lower && massDefect <= 1.0d) || (massDefect >= 0.0d
              && massDefect <= upper);
      if(!massDefectOk) {
        return false;
      }
    } else { // 0.4 - 0.8
      final boolean massDefectOk = lower <= massDefect && massDefect <= upper;
      if(!massDefectOk) {
        return false;
      }
    }
    return true;
  }

  public double getLowerEndpoint() {
    return lower;
  }

  public double getUpperEndpoint() {
    return upper;
  }
}
