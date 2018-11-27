package net.sf.mzmine.util.maths;

import net.sf.mzmine.util.MathUtils;

public class CenterFunction {
  private final CenterMeasure measure;
  // weight transform is only applied to avg
  private final Transform weightTransform;

  public CenterFunction(CenterMeasure measure) {
    this(measure, Transform.NONE);
  }

  public CenterFunction(CenterMeasure measure, Transform weightTransform) {
    super();
    this.measure = measure;
    this.weightTransform = weightTransform;
  }

  public CenterMeasure getMeasure() {
    return measure;
  }

  public Transform getWeightTransform() {
    return weightTransform;
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
    return MathUtils.calcCenter(measure, values, weights, weightTransform);
  }
}
