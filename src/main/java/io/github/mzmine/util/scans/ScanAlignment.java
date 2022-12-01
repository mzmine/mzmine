/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.util.scans;

import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Scan or mass list alignment based on data points array
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class ScanAlignment {

  public static final DataPointSorter sorter =
      new DataPointSorter(SortingProperty.Intensity, SortingDirection.Descending);

  // hide the constructor
  private ScanAlignment() {
  }

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
    ArrayList<DataPoint> sortedB = Lists.newArrayList(b);
    sortedB.sort(sorter);

    return alignOfSorted(mzTol, a, sortedB);
  }

  /**
   * Aligned data points within mzTolerance. Both arrays must be already sorted by intensity
   *
   * @param sortedA sorted by intensity
   * @param sortedB sorted by intensity
   * @return List of aligned data points
   */
  public static List<DataPoint[]> alignOfSorted(MZTolerance mzTol, DataPoint[] sortedA,
      DataPoint[] sortedB) {
    return alignOfSorted(mzTol, sortedA, Lists.newArrayList(sortedB));
  }

  /**
   * Aligned data points within mzTolerance. Both arrays must be already sorted by intensity
   *
   * @param sortedA sorted by intensity
   * @param sortedB sorted by intensity
   * @return List of aligned data points
   */
  public static List<DataPoint[]> alignOfSorted(MZTolerance mzTol, DataPoint[] sortedA,
      List<DataPoint> sortedB) {
    // add all datapoints of sortedA to the aligned list
    List<DataPoint[]> list = new ArrayList<>();
    for (DataPoint dpa : sortedA) {
      // match or null
      DataPoint dpb = findMatch(mzTol, dpa, sortedB);
      list.add(new DataPoint[]{dpa, dpb});
    }

    // insert all remaining DP from sorted b
    for (DataPoint dp : sortedB) {
      list.add(new DataPoint[]{null, dp});
    }

    return list;
  }


  /**
   * Aligned data points within mzTolerance. Sort by intensity and match every signal only once
   *
   * @param a
   * @param b
   * @return List of aligned data points
   */
  public static List<DataPoint[]> alignModAware(MZTolerance mzTol, DataPoint[] a, DataPoint[] b,
      double precursorMzA, double precursorMzB) {
    // sort by intensity
    Arrays.sort(a, sorter);
    // sort b
    ArrayList<DataPoint> sortedB = Lists.newArrayList(b);
    sortedB.sort(sorter);
    return alignOfSortedModAware(mzTol, a, sortedB, precursorMzA, precursorMzB);
  }

  /**
   * Aligned data points within mzTolerance. Both arrays must be already sorted by intensity
   *
   * @param sortedA sorted by intensity
   * @param sortedB sorted by intensity
   * @return List of aligned data points
   */
  public static List<DataPoint[]> alignOfSortedModAware(MZTolerance mzTol, DataPoint[] sortedA,
      DataPoint[] sortedB, double precursorMzA, double precursorMzB) {
    return alignOfSortedModAware(mzTol, sortedA, Lists.newArrayList(sortedB), precursorMzA,
        precursorMzB);
  }

  /**
   * Aligned data points within mzTolerance. Both arrays must be already sorted by intensity
   *
   * @param sortedA sorted by intensity
   * @param sortedB sorted by intensity
   * @return List of aligned data points
   */
  public static List<DataPoint[]> alignOfSortedModAware(MZTolerance mzTol, DataPoint[] sortedA,
      List<DataPoint> sortedB, double precursorMzA, double precursorMzB) {
    // add all datapoints of sortedA to the aligned list
    List<DataPoint[]> list = new ArrayList<>();
    for (DataPoint dpa : sortedA) {
      // match or null
      DataPoint dpb = findMatchModAware(mzTol, dpa, sortedB, precursorMzA, precursorMzB);
      list.add(new DataPoint[]{dpa, dpb});
    }

    // insert all remaining DP from sorted b
    for (DataPoint dp : sortedB) {
      list.add(new DataPoint[]{null, dp});
    }

    return list;
  }

  /**
   * get overlapping MZ range (lowerBound - mzTol and upperbound+ mzTol)
   *
   * @param mzTol
   * @param a
   * @param b
   * @return
   */
  public static Range<Double> getOverlapMZ(MZTolerance mzTol, DataPoint[] a, DataPoint[] b) {
    Range<Double> ra = getMZRange(a);
    Range<Double> rb = getMZRange(b);

    // no overlap
    if (!ra.isConnected(rb)) {
      return Range.singleton(0d);
    }

    Range<Double> intersect = ra.intersection(rb);
    // add mzTol
    double min = intersect.lowerEndpoint();
    min = mzTol.getToleranceRange(min).lowerEndpoint();
    double max = intersect.upperEndpoint();
    max = mzTol.getToleranceRange(max).upperEndpoint();
    return Range.closed(min, max);

  }

  /**
   * crop to overlapping MZ range (lowerBound - mzTol and upperbound+ mzTol)
   *
   * @param mzTol
   * @param a
   * @param b
   * @return DataPoint[a, b][cropped datapoints]
   */
  public static DataPoint[][] cropToOverlap(MZTolerance mzTol, DataPoint[] a, DataPoint[] b) {
    Range<Double> overlap = getOverlapMZ(mzTol, a, b);
    DataPoint[] newa =
        Arrays.stream(a).filter(d -> overlap.contains(d.getMZ())).toArray(DataPoint[]::new);
    DataPoint[] newb =
        Arrays.stream(b).filter(d -> overlap.contains(d.getMZ())).toArray(DataPoint[]::new);
    return new DataPoint[][]{newa, newb};
  }

  /**
   * Closed mz range of all data points
   *
   * @param dps
   * @return
   */
  public static Range<Double> getMZRange(DataPoint[] dps) {
    if (dps == null || dps.length == 0) {
      return Range.singleton(0d);
    }
    double min = dps[0].getMZ();
    double max = dps[0].getMZ();
    for (int i = 1; i < dps.length; i++) {
      DataPoint dp = dps[i];
      min = Math.min(min, dp.getMZ());
      max = Math.max(max, dp.getMZ());
    }
    return Range.closed(min, max);
  }

  /**
   * Removes the matched datapoint from the sorted b list
   *
   * @param mzTol   tolerance of matching
   * @param dpa     target data point to b matched
   * @param sortedB sorted by intensity
   * @return matched data point or null
   */
  private static DataPoint findMatch(MZTolerance mzTol, DataPoint dpa, List<DataPoint> sortedB) {
    for (DataPoint dpb : sortedB) {
      if (mzTol.checkWithinTolerance(dpa.getMZ(), dpb.getMZ())) {
        // remove from list and return
        sortedB.remove(dpb);
        return dpb;
      }
    }
    return null;
  }

  /**
   * Removes the matched datapoint from the sorted b list
   *
   * @param mzTol        tolerance of matching
   * @param dpa          target data point to b matched
   * @param sortedB      sorted by intensity
   * @param precursorMzA precursor m/z of a
   * @param precursorMzB precursor m/z of b
   * @return matched data point or null
   */
  private static DataPoint findMatchModAware(MZTolerance mzTol, DataPoint dpa,
      List<DataPoint> sortedB, double precursorMzA, double precursorMzB) {
    double deltaMZ = precursorMzB - precursorMzA;
    for (DataPoint dpb : sortedB) {
      // TODO how to handle cases where we have both the direct fragment and the modified fragment
      // as in shifted by the precursor m/z
      // currently we just use the one with the highest intensity
      if (mzTol.checkWithinTolerance(dpa.getMZ(), dpb.getMZ()) ||
          mzTol.checkWithinTolerance(dpa.getMZ() + deltaMZ, dpb.getMZ())) {
        // remove from list and return
        sortedB.remove(dpb);
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
      if (Arrays.stream(dps).noneMatch(Objects::isNull)) {
        aligned.add(dps);
      }
    }
    return aligned;
  }

  /**
   * Only keep entries with a data point for all aligned scans
   *
   * @param list
   * @return
   */
  public static DataPoint[][] removeUnaligned(DataPoint[][] list) {
    List<DataPoint[]> aligned = new ArrayList<>();

    for (int d = 0; d < list[0].length; d++) {
      final int dd = d;
      boolean noneNull = true;
      for (int i = 0; i < list.length && noneNull; i++) {
        if (list[i][d] == null) {
          noneNull = false;
        }
      }
      if (noneNull) {
        aligned.add(Arrays.stream(list).map(dp -> dp[dd]).toArray(DataPoint[]::new));
      }
    }
    // to array
    DataPoint[][] array = new DataPoint[list.length][aligned.size()];
    for (int l = 0; l < list.length; l++) {
      for (int dp = 0; dp < aligned.size(); dp++) {
        array[l][dp] = aligned.get(dp)[l];
      }
    }
    return array;
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
   * Might want to remove unaligned before. Missing data points are replaced by 0. <br> weighted
   * values = Intensity^weightI * m/z^weightMZ <br> Calculation similar to MassBank / NIST
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
        if (dp != null) {
          data[i][d] =
              Math.pow(dp.getIntensity(), weightIntensity) * Math.pow(dp.getMZ(), weightMZ);
        } else {
          data[i][d] = 0;
        }
      }
    }
    return data;
  }

  /**
   * Converts a list of aligned datapoints back to mass lists
   *
   * @param aligned aligned list(DataPoint)[dimension]
   * @return
   */
  public static DataPoint[][] convertBackToMassLists(List<DataPoint[]> aligned) {
    int dpCount = aligned.size();
    int maxDimensions = aligned.stream().mapToInt(dps -> dps.length).max().orElse(0);

    DataPoint[][] data = new DataPoint[maxDimensions][dpCount];
    for (int dp = 0; dp < dpCount; dp++) {
      for (int dim = 0; dim < maxDimensions; dim++) {
        if (dim < aligned.get(dp).length) {
          data[dim][dp] = aligned.get(dp)[dim];
        }
      }
    }
    return data;
  }
}
