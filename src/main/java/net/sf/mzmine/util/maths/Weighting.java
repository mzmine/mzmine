package net.sf.mzmine.util.maths;

import java.util.stream.DoubleStream;

public enum Weighting {
  // no weighting
  NONE("NONE", v -> 1),
  // linear -> no transform
  LINEAR("LINEAR", v -> v),
  // LOG
  LOG10("LOG10", Math::log), //
  LOG2("LOG2", Math::log),
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
   * Transform array
   * 
   * @param values
   * @return
   */
  public double[] transform(double[] values) {
    return DoubleStream.of(values).map(v -> f.transform(v)).toArray();
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
