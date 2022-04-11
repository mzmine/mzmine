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

package io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance;

import com.google.common.collect.Range;

/**
 * RTTolerance allows specifying retention time tolerance it is either absolute (seconds or minutes)
 * or relative (percent) but as rest of MZmine codebase, it assumes that rt values (other than the
 * tolerance given in constructor) are in minutes in methods such as getToleranceRange or
 * checkWithinTolerance
 */
public class MobilityTolerance {

  private final float tolerance;

  public MobilityTolerance(final float tolerance) {
    this.tolerance = tolerance;
  }

  public float getTolerance() {
    return tolerance;
  }

  public Range<Float> getToleranceRange(final float mobility) {
    return Range.closed(mobility - tolerance, mobility + tolerance);
  }

  public boolean checkWithinTolerance(final float mobility1, final float mobility2) {
    return getToleranceRange(mobility1).contains(mobility2);
  }

  @Override
  public String toString() {
    return "Mobility tolerance: " + tolerance;
  }

}
