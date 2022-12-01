/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.util.scans.similarity;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import java.text.MessageFormat;

/**
 * Weights for intensity/mz weighting in similarity scoring
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class Weights {

  /**
   * Uses only intensity
   */
  public static final Weights NONE = new Weights("NONE", 1, 0) {
    @Override
    public DataPoint getWeighted(DataPoint dp) {
      return dp;
    }

    @Override
    public double apply(DataPoint dp) {
      return dp.getIntensity();
    }

    @Override
    public double apply(double mzVal, double intensityVal) {
      return intensityVal;
    }
  };
  /**
   * Weights for intensity and m/z <br> m/z weight is usually higher for GC-EI-MS (higher m/z ->
   * significant fragments) and lower for LC-MS
   */
  public static final Weights SQRT = new Weights("SQRT", 0.5, 0);
  public static final Weights MASSBANK = new Weights("MassBank", 0.5, 2);
  public static final Weights NIST_GC = new Weights("NIST (GC)", 0.6, 3);
  public static final Weights NIST11 = new Weights("NIST11 (LC)", 0.53, 1.3);
  public static final Weights[] VALUES = new Weights[]{NONE, SQRT, MASSBANK, NIST11, NIST_GC};

  private final String name;
  private final double mz;
  private final double intensity;

  private Weights(String name, double intensity, double mz) {
    this.name = name;
    this.mz = mz;
    this.intensity = intensity;
  }

  public double getIntensity() {
    return intensity;
  }

  public double getMz() {
    return mz;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return MessageFormat.format("{0} (mz^{1} * I^{2})", name, mz, intensity);
  }

  public double apply(DataPoint dp) {
    return apply(dp.getMZ(), dp.getIntensity());
  }

  public double apply(double mzVal, double intensityVal) {
    return Math.pow(intensityVal, intensity) * Math.pow(mzVal, mz);
  }

  public DataPoint getWeighted(DataPoint dp) {
    return new SimpleDataPoint(dp.getMZ(), apply(dp));
  }
}
