package io.github.mzmine.modules.dataprocessing.id_biotransformer;

public enum SmilesSource {
  SPECTRAL_LIBRARY("Spectral library"), COMPOUND_DB("Compound DB"), ALL("All");

  private final String str;

  SmilesSource(String str) {
    this.str = str;
  }


  @Override
  public String toString() {
    return str;
  }
}
