package net.sf.mzmine.util.scans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;

/**
 * Scan or mass list alignment based on data points array
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class ScanAlignment {

  /**
   * Aligned data points in mzTolerance
   * 
   * @param a
   * @param b
   * @return List of aligned data points
   */
  public static List<DataPoint[]> align(MZTolerance mzTol, DataPoint[] a, DataPoint[] b) {
    List<DataPoint[]> list = new ArrayList<>();
    for (DataPoint dp : a)
      list.add(new DataPoint[] {dp, null});

    for (DataPoint dpb : b) {
      // find match
      DataPoint[] dp = findMatch(mzTol, list, dpb, 1);
      // add or create new
      if (dp == null)
        list.add(new DataPoint[] {null, dpb});
      else
        dp[1] = dpb;
    }
    return list;
  }

  /**
   * 
   * @param a
   * @param b
   * @return List of aligned data points
   */
  public static List<DataPoint[]> align(MZTolerance mzTol, List<DataPoint[]> scans) {
    if (scans.size() < 2)
      return null;

    // index of max TIC to start with
    int maxIndex = indexOfMaxTIC(scans);

    List<DataPoint[]> list = new ArrayList<>();
    for (DataPoint dp : scans.get(0)) {
      DataPoint[] dps = new DataPoint[scans.size()];
      dps[maxIndex] = dp;
      list.add(dps);
    }

    for (int i = 0; i < scans.size(); i++) {
      if (i == maxIndex)
        continue;

      for (DataPoint dpb : scans.get(i)) {
        // find match
        DataPoint[] dps = findMatch(mzTol, list, dpb, i);
        // add or create new
        if (dps == null) {
          dps = new DataPoint[scans.size()];
          dps[i] = dpb;
          list.add(dps);
        } else {
          dps[i] = dpb;
        }
      }
    }
    return list;
  }

  public static double getTIC(DataPoint[] scan) {
    return Arrays.stream(scan).mapToDouble(DataPoint::getIntensity).sum();
  }

  private static int indexOfMaxTIC(List<DataPoint[]> scans) {
    int maxI = 0;
    double max = 0;
    for (int i = 0; i < scans.size(); i++) {
      double tic = getTIC(scans.get(i));
      if (tic > max) {
        max = tic;
        maxI = i;
      }
    }
    return maxI;
  }


  /**
   * Most intense
   * 
   * @param mzTol
   * @param list
   * @param dpb
   * @param indexB
   * @return
   */
  private static DataPoint[] findMatch(MZTolerance mzTol, List<DataPoint[]> list, DataPoint dpb,
      int indexB) {
    DataPoint[] best = null;
    for (DataPoint[] dparray : list) {
      // continue if smaller than already inserted
      if (dparray[indexB] != null && dparray[indexB].getIntensity() > dpb.getIntensity())
        continue;

      boolean outOfMZTol = false;
      for (int i = 0; i < dparray.length && !outOfMZTol; i++) {
        DataPoint dp = dparray[i];
        if (dp != null && i != indexB) {
          if (mzTol.checkWithinTolerance(dp.getMZ(), dpb.getMZ()))
            best = dparray;
          else
            outOfMZTol = true;
        }
      }
    }
    return best;
  }

  /**
   * Only keep entries with a data point for all aligned scans
   * 
   * @param list
   * @return
   */
  public static List<DataPoint[]> removeUnaligned(List<DataPoint[]> list) {
    List<DataPoint[]> aligned = new ArrayList<>();
    for (DataPoint[] dps : list) {
      // all non null?
      if (Arrays.stream(dps).noneMatch(Objects::isNull))
        aligned.add(dps);
    }
    return aligned;
  }

  /**
   * Remove unaligned before
   * 
   * @param diffAligned
   * @return array of [data points][intensity in scans]
   */
  public static double[][] toIntensityArray(List<DataPoint[]> diffAligned) {
    double[][] data = new double[diffAligned.size()][];
    for (int i = 0; i < data.length; i++) {
      DataPoint[] dps = diffAligned.get(i);
      data[i] = new double[dps.length];
      for (int d = 0; d < dps.length; d++) {
        DataPoint dp = dps[d];
        data[i][d] = dp != null ? dp.getIntensity() : 0;
      }
    }
    return data;
  }
}
