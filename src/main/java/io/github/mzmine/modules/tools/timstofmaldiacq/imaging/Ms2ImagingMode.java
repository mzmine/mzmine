package io.github.mzmine.modules.tools.timstofmaldiacq.imaging;

public enum Ms2ImagingMode {
  SINGLE("single"), TRIPLE("triple");

  private final String str;

  Ms2ImagingMode(String str) {
    this.str = str;
  }

  @Override
  public String toString() {
    return str;
  }
}
