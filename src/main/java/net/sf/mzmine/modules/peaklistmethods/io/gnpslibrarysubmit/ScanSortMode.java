package net.sf.mzmine.modules.peaklistmethods.io.gnpslibrarysubmit;

public enum ScanSortMode {
  MAX_TIC, // sort by maximum TIC
  NUMBER_OF_SIGNALS; // sort by number of signals
  @Override
  public String toString() {
    return super.toString().replaceAll("_", " ");
  }
}
