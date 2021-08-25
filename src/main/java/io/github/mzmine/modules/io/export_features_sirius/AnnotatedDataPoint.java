package io.github.mzmine.modules.io.export_features_sirius;

import io.github.mzmine.datamodel.impl.SimpleDataPoint;

public class AnnotatedDataPoint extends SimpleDataPoint {

  private final String annotation;

  public AnnotatedDataPoint(double mz, double intensity, String annotation) {
    super(mz, intensity);
    this.annotation = annotation;
  }

  public String getAnnotation() {
    return annotation;
  }
}
