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
