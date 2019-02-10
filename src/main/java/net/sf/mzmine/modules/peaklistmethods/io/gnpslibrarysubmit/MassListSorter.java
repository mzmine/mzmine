package net.sf.mzmine.modules.peaklistmethods.io.gnpslibrarysubmit;

import java.util.Comparator;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.util.ScanUtils;

public class MassListSorter implements Comparator<DataPoint[]> {
  private double noiseLevel;
  private ScanSortMode sort;

  public MassListSorter(ScanSortMode sort, double noiseLevel) {
    this.sort = sort;
    this.noiseLevel = noiseLevel;
  }

  @Override
  public int compare(DataPoint[] a, DataPoint[] b) {
    switch (sort) {
      case NUMBER_OF_SIGNALS:
        int result = Integer.compare(getNumberOfSignals(a), getNumberOfSignals(b));
        // same number of signals? use max TIC
        if (result == 0)
          return Double.compare(getTIC(a), getTIC(b));
      case MAX_TIC:
        return Double.compare(getTIC(a), getTIC(b));
    }
    throw new IllegalArgumentException("Should not reach. Not all cases of sort are considered");
  }

  /**
   * sum of intensity
   * 
   * @param a
   * @return
   */
  private double getTIC(DataPoint[] a) {
    return ScanUtils.getTIC(a, noiseLevel);
  }

  /**
   * Number of DP greater noise level
   * 
   * @param a
   * @return
   */
  private int getNumberOfSignals(DataPoint[] a) {
    return ScanUtils.getNumberOfSignals(a, noiseLevel);
  }

}
