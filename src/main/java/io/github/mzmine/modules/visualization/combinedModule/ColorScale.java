package io.github.mzmine.modules.visualization.combinedModule;

public enum ColorScale {
  PRECURSORION("Precursor ion intensity"), PRODUCTION("Product ion intensity");
  private String type;

  ColorScale(String type) {
    this.type = type;
  }

  public String toString() {
    return type;
  }
}
