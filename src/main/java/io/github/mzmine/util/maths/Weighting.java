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

package io.github.mzmine.util.maths;

import java.util.stream.DoubleStream;

public enum Weighting {
  // no weighting
  NONE("None", v -> 1),
  // linear -> no transform
  LINEAR("Linear", v -> v),
  // logger: put to zero weight if it was zero
  logger10("log10", v -> v == 0 ? 0 : Math.log10(v)), //
  logger2("log2", v -> v == 0 ? 0 : Math.log(v) / Math.log(2)),
  /**
   * Square-root
   */
  SQRT("SQRT", "square root", Math::sqrt),
  /**
   * cube-root
   */
  CBRT("CBRT", "cube root", Math::cbrt);

  //
  private String label;
  private TransformFunction f;
  private String description;

  Weighting(String label, TransformFunction f) {
    this(label, label, f);
  }

  Weighting(String label, String description, TransformFunction f) {
    this.label = label;
    this.description = description;
    this.f = f;
  }

  /**
   * Transform value
   * 
   * @param v
   * @return
   */
  public double transform(double v) {
    return f.transform(v);
  }

  /**
   * e.g., lOG(weight)-logger(noise)
   * 
   * @param weight
   * @param noiseLevel
   * @param maxWeight
   * @return
   */
  public double transform(double weight, Double noiseLevel, Double maxWeight) {
    // e.g., lOG(weight)-logger(noise)
    double real = 0;
    if (noiseLevel != null)
      real = f.transform(weight) - f.transform(noiseLevel);
    else
      real = f.transform(weight);

    if (maxWeight != null)
      real = Math.min(real, maxWeight);
    return Math.max(0, real);
  }

  /**
   * Transform array
   * 
   * @param values
   * @return
   */
  public double[] transform(double[] values) {
    return DoubleStream.of(values).map(this::transform).toArray();
  }

  /**
   * Transform array with noiseLevel and maxHeight
   * 
   * @param weights
   * @param noiseLevel
   * @param maxWeight
   * @return
   */
  public double[] transform(double[] weights, Double noiseLevel, Double maxWeight) {
    return DoubleStream.of(weights).map(v -> transform(v, noiseLevel, maxWeight)).toArray();
  }

  @Override
  public String toString() {
    return this.label;
  }

  public String getDescription() {
    return description;
  }

  @FunctionalInterface
  private interface TransformFunction {
    public double transform(double v);
  }

}
