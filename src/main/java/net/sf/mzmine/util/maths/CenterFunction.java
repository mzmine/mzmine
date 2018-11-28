package net.sf.mzmine.util.maths;

import javax.annotation.Nullable;
import net.sf.mzmine.util.MathUtils;

public class CenterFunction {
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
