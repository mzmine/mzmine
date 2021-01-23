package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution;

public enum ResolvingDimension {
  RETENTION_TIME("Retention time"), MOBILITY("Mobility");

  private final String name;
  ResolvingDimension(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
