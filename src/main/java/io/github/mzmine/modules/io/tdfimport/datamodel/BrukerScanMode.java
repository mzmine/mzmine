package io.github.mzmine.modules.io.tdfimport.datamodel;

public enum BrukerScanMode {

  MS(0, "MS"), AUTO_MSMS(1, "Auto MS/MS"), MRM(2, "MRM"),
  IN_SOURCE_CID(3, "in-source CID"), BROADBAND_CID(4, "broadband CID"),
  PASEF(8, "PASEF"), DIA(9, "DIA"), PRM(10, "PRM"), MALDI(20, "MALDI"), UNKNOWN(-1, "Unknown");

  private final int num;

  private final String description;

  BrukerScanMode(final int num, final String description) {
    this.num = num;
    this.description = description;
  }

  public static BrukerScanMode fromScanMode(final int scanMode) {
    for(BrukerScanMode mode : BrukerScanMode.values()) {
      if(mode.num == scanMode) {
        return mode;
      }
    }
    return BrukerScanMode.UNKNOWN;
  }

  public int getNum() {
    return num;
  }

  public String getDescription() {
    return description;
  }
}
