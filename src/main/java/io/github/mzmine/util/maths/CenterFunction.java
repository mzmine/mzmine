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

import io.github.mzmine.util.MathUtils;
import org.jetbrains.annotations.Nullable;

public class CenterFunction {
  public static final Weighting DEFAULT_MZ_WEIGHTING = Weighting.LINEAR;

  private final CenterMeasure measure;
  // weight transform is only applied to avg
  private final Weighting weightTransform;

  // divide weight== noiseLevel will be 1
  private @Nullable Double noiseLevel = null;
  // cap weight to a max
  private @Nullable Double maxWeight = null;

  public CenterFunction(CenterMeasure measure) {
    this(measure, Weighting.NONE);
  }

  public CenterFunction(CenterMeasure measure, Weighting weightTransform) {
    super();
    this.measure = measure;
    this.weightTransform = weightTransform;
  }

  public CenterFunction(CenterMeasure measure, Weighting weightTransform, double noiseLevel,
      double maxWeight) {
    this(measure, weightTransform);
    this.noiseLevel = noiseLevel;
    this.maxWeight = maxWeight;
  }

  public CenterMeasure getMeasure() {
    return measure;
  }

  public Weighting getWeightTransform() {
    return weightTransform;
  }

  /**
   * Cap weight at a maximum. null for no maxWeight
   * 
   * @param maxWeight
   */
  public void setMaxWeight(Double maxWeight) {
    this.maxWeight = maxWeight;
  }

  /**
   * noise level will have weight 1. null No noise level
   * 
   * @param noiseLevel
   */
  public void setNoiseLevel(Double noiseLevel) {
    this.noiseLevel = noiseLevel;
  }

  /**
   * median or non-weighted average
   * 
   * @param center
   * @param values
   * @return
   */
  public double calcCenter(double[] values) {
    return MathUtils.calcCenter(measure, values);
  }

  /**
   * median or weighted average
   * 
   * @param center
   * @param values
   * @param weights
   * @param transform only used for center measure AVG (can also be Transform.NONE)
   * @return
   */
  public double calcCenter(double[] values, double[] weights) {
    return MathUtils.calcCenter(measure, values, weights, weightTransform, noiseLevel, maxWeight);
  }
}
