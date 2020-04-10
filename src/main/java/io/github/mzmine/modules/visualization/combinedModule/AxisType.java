package io.github.mzmine.modules.visualization.combinedModule;

public enum AxisType {
  RETENTIONTIME("Retention time"), PRECURSORION("Precursor ion m/z"), PRODUCTION(
      "Product ion m/z"), NEUTRALLOSS("Neutral loss");

  private String type;

  AxisType(String type) {
    this.type = type;
  }

  public String toString() {
    return type;
  }
}
