package io.github.mzmine.modules.dataprocessing.id_masst_meta;

public record FastMasstRequestConfig(double mzTol, double minSimilarity, int minMatchedSignals, boolean analogSearch, double analogMassBelow, double analogMassAbove, String dataIndexName) {
  public FastMasstRequestConfig createDefault() {
    return new FastMasstRequestConfig(0.02, 0.7, 4, false, 130, 200,
        FastMasstDatabase.METABOLOMICS_PAN_REPO_LATEST.getTitle());
  }

}
