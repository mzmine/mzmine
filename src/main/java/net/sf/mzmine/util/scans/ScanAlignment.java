package net.sf.mzmine.util.scans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import com.google.common.collect.Lists;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.util.DataPointSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

/**
 * Scan or mass list alignment based on data points array
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class ScanAlignment {

  public static DataPointSorter sorter =
      new DataPointSorter(SortingProperty.Intensity, SortingDirection.Descending);

  /**
   * Aligned data points within mzTolerance. Sort by intensity and match every signal only once
   * 
   * @param a
   * @param b
   * @return List of aligned data points
   */
  public static List<DataPoint[]> align(MZTolerance mzTol, DataPoint[] a, DataPoint[] b) {
    // sort by intensity
    Arrays.sort(a, sorter);

    // sort b
    ArrayList<DataPoint> bsorted = Lists.newArrayList(b);
    bsorted.sort(sorter);

    // add all datapoints of a to the aligned list
    List<DataPoint[]> list = new ArrayList<>();
    for (DataPoint dpa : a) {
      // match or null
      DataPoint dpb = findMatch(mzTol, dpa, bsorted);
      list.add(new DataPoint[] {dpa, dpb});
    }

    // insert all remaining DP from sorted b
    for (DataPoint dp : bsorted) {
      list.add(new DataPoint[] {null, dp});
    }

    return list;
  }

  /**
   * Removes the matched datapoint from the sorted b list
   * 
   * @param mzTol
   * @param dpa
   * @param bsorted sorted by intensity
   * @return
   */
  private static DataPoint findMatch(MZTolerance mzTol, DataPoint dpa, List<DataPoint> bsorted) {
    for (DataPoint dpb : bsorted) {
      if (mzTol.checkWithinTolerance(dpa.getMZ(), dpb.getMZ())) {
        // remove from list and return
        bsorted.remove(dpb);
        return dpb;
      }
    }
    return null;
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
   * Remove unaligned before. Missing data points are replaced by 0
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

  /**
   * Might want to remove unaligned before. Missing data points are replaced by 0. <br>
   * weighted values = Intensity^weightI * m/z^weightMZ <br>
   * Calculation similar to MassBank / NIST
   * 
   * @param diffAligned
   * @param weightIntensity
   * @param weightMZ
   * @return array of [data points][intensity in scans]
   */
  public static double[][] toIntensityMatrixWeighted(List<DataPoint[]> diffAligned,
      double weightIntensity, double weightMZ) {
    double[][] data = new double[diffAligned.size()][];
    for (int i = 0; i < data.length; i++) {
      DataPoint[] dps = diffAligned.get(i);
      data[i] = new double[dps.length];
      for (int d = 0; d < dps.length; d++) {
        DataPoint dp = dps[d];
        if (dp != null)
          data[i][d] =
              Math.pow(dp.getIntensity(), weightIntensity) * Math.pow(dp.getMZ(), weightMZ);
        else
          data[i][d] = 0;
      }
    }
    return data;
  }
}
