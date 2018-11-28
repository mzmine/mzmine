package net.sf.mzmine.util.maths;

import java.util.stream.DoubleStream;

public enum Weighting {
  // no weighting
  NONE("NONE", v -> 1),
  // linear -> no transform
  LINEAR("LINEAR", v -> v),
  // LOG: put to zero weight if it was zero
  LOG10("LOG10", v -> v == 0 ? 0 : Math.log10(v)), //
  LOG2("LOG2", v -> v == 0 ? 0 : Math.log(v) / Math.log(2)),
  /**
   * Sqare-root
   */
  SQRT("SQRT", "sqare root", Math::sqrt),
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
   * e.g., lOG(weight)-LOG(noise)
   * 
   * @param weight
   * @param noiseLevel
   * @param maxWeight
   * @return
   */
  public double transform(double weight, Double noiseLevel, Double maxWeight) {
    // e.g., lOG(weight)-LOG(noise)
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
  public double[] transform(double[] weights) {
    return DoubleStream.of(weights).map(this::transform).toArray();
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

  public String getLabel() {
    return label;
  }

  public String getDescription() {
    return description;
  }

  @FunctionalInterface
  private interface TransformFunction {
    public double transform(double v);
  }

}
