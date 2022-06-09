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
 *
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
