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

package io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MobilityType;

/**
 * RTTolerance allows specifying retention time tolerance it is either absolute (seconds or minutes)
 * or relative (percent) but as rest of MZmine codebase, it assumes that rt values (other than the
 * tolerance given in constructor) are in minutes in methods such as getToleranceRange or
 * checkWithinTolerance
 */
public class MobilityTolerance {

  private final double tolerance;
  private final MobilityType mobilityType;

  public MobilityTolerance(final double tolerance, MobilityType unit) {
    this.tolerance = tolerance;
    this.mobilityType = unit;
  }

  public double getTolerance() {
    return tolerance;
  }

  public MobilityType getMobilityType() {
    return mobilityType;
  }

  public Range<Double> getToleranceRange(final double mobility) {
    return getToleranceRange(mobility, this.mobilityType);
  }

  public Range<Double> getToleranceRange(final double mobility, MobilityType mobilityType) {
    if (mobilityType != this.mobilityType) {
      throw new IllegalArgumentException(
          "Argument mobilityType does match the mobility type of this tolerance!");
    }
    return Range.closed(mobility - tolerance, mobility + tolerance);
  }

  public boolean checkWithinTolerance(final double mobility1, final double mobility2) {
    return getToleranceRange(mobility1, this.mobilityType).contains(mobility2);
  }

  @Override
  public String toString() {
    return tolerance + " " + mobilityType.toString();
  }

}
