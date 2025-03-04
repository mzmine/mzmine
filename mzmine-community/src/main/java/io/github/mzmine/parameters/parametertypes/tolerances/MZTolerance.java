/*
 * Copyright (c) 2004-2024 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
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

  public static final MZTolerance FIFTEEN_PPM_OR_FIVE_MDA = new MZTolerance(0.005, 15);

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
      final double abs = Math.max(Math.abs(mzRange.lowerEndpoint() - mz),
          Math.abs(mzRange.upperEndpoint() - mz));
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

  /**
   * Joins an m/z tolerance and ppm and absolute tolerance on the maximum values
   *
   * @param mzTol    existing tolerance
   * @param absolute default max tolerance
   * @param ppm      default max tolerance
   * @return a new tolerance of the max values of both
   */
  public static MZTolerance max(final MZTolerance mzTol, final double absolute, final double ppm) {
    return new MZTolerance(Math.max(absolute, mzTol.mzTolerance),
        Math.max(ppm, mzTol.ppmTolerance));
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
    final double dist = Math.abs(mz1 - mz2);
    // absolute then relative tolerance check
    return dist <= mzTolerance || dist <= mz1 / MILLION * ppmTolerance;
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

  public static MZTolerance[] getDefaultResolutions() {
    MZTolerance[] mzTol = new MZTolerance[4];
    mzTol[0] = new MZTolerance(0.00025, 0);
    mzTol[1] = new MZTolerance(0.001, 0);
    mzTol[2] = new MZTolerance(0.01, 0);
    mzTol[3] = new MZTolerance(0.1, 0);
    return mzTol;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getMzTolerance(), getPpmTolerance());
  }

}
