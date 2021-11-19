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

package io.github.mzmine.parameters.parametertypes.tolerances;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.Feature;
import java.util.Collection;
import java.util.Objects;

/**
 * This class represents m/z tolerance. Tolerance is set using absolute (m/z) and relative (ppm)
 * values. The tolerance range is calculated as the maximum of the absolute and relative values.
 */
public class MZTolerance {

  // PPM conversion factor.
  private static final double MILLION = 1000000.0;

  // Tolerance has absolute (in m/z) and relative (in ppm) values
  private final double mzTolerance;
  private final double ppmTolerance;

  public MZTolerance(final double toleranceMZ, final double tolerancePPM) {
    mzTolerance = toleranceMZ;
    ppmTolerance = tolerancePPM;
  }

  public static MZTolerance getMaximumDataPointTolerance(Collection<Feature> features) {
    double maxPPM = 0d;
    double maxAbs = 0d;

    for (Feature f : features) {
      final Range<Double> mzRange = f.getRawDataPointsMZRange();
      final double mz = f.getMZ();
      final double abs = Math
          .max(Math.abs(mzRange.lowerEndpoint() - mz), Math.abs(mzRange.upperEndpoint() - mz));
      final double ppm = abs / mz * MILLION;
      if (abs > maxAbs) {
        maxAbs = abs;
      }
      if (ppm > maxPPM) {
        maxPPM = ppm;
      }
    }

    return new MZTolerance(maxAbs, maxPPM);
  }

  public double getMzTolerance() {
    return mzTolerance;
  }

  public double getPpmTolerance() {
    return ppmTolerance;
  }

  public double getMzToleranceForMass(final double mzValue) {
    return Math.max(mzTolerance, mzValue / MILLION * ppmTolerance);
  }

  public double getPpmToleranceForMass(final double mzValue) {
    return Math.max(ppmTolerance, mzTolerance / (mzValue / MILLION));
  }

  public Range<Double> getToleranceRange(final double mzValue) {
    final double absoluteTolerance = getMzToleranceForMass(mzValue);
    return Range.closed(mzValue - absoluteTolerance, mzValue + absoluteTolerance);
  }

  public Range<Double> getToleranceRange(final Range<Double> mzRange) {
    return Range.closed(mzRange.lowerEndpoint() - getMzToleranceForMass(mzRange.lowerEndpoint()),
        mzRange.upperEndpoint() + getMzToleranceForMass(mzRange.upperEndpoint()));
  }

  public boolean checkWithinTolerance(final double mz1, final double mz2) {
    return getToleranceRange(mz1).contains(mz2);
  }

  @Override
  public String toString() {
    return mzTolerance + " m/z or " + ppmTolerance + " ppm";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MZTolerance that = (MZTolerance) o;
    return Double.compare(that.getMzTolerance(), getMzTolerance()) == 0
        && Double.compare(that.getPpmTolerance(), getPpmTolerance()) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getMzTolerance(), getPpmTolerance());
  }
}
