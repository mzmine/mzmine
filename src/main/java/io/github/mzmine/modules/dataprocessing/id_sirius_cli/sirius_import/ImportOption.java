package io.github.mzmine.modules.dataprocessing.id_sirius_cli.sirius_import;

public enum ImportOption {
  BEST("Best"), TOP_TEN("Top ten"), ALL("All");

  private final String name;
  ImportOption(String str) {
    name = str;
  }

  @Override
  public String toString() {
    return name;
  }
}
