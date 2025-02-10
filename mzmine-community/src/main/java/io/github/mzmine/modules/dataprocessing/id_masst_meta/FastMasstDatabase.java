package io.github.mzmine.modules.dataprocessing.id_masst_meta;

public enum FastMasstDatabase {
  // value names equal the GNPS values
  METABOLOMICS_PAN_REPO_LATEST("metabolomicspanrepo_index_latest"), //
  GNPS_DATA("gnpsdata_index"), //
  GNPS_LIBRARY("gnpslibrary"), //
  MASSIVE_DATA("massivedata_index"), //
  MASSIVE_KB("massivekb_index");

  private final String title;

  FastMasstDatabase(String title) {
    this.title = title;
  }

  public String getTitle() {
    return title;
  }

  @Override
  public String toString() {
    return getTitle();
  }
}
