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

package io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation;


import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.CachedFeatureDataAccess;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.correlation.CorrelationData;
import io.github.mzmine.datamodel.features.correlation.FullCorrelationData;
import io.github.mzmine.datamodel.features.correlation.R2RFullCorrelationData;
import io.github.mzmine.parameters.parametertypes.MinimumFeatureFilter;
import io.github.mzmine.util.ArrayUtils;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.maths.similarity.Similarity;
import io.github.mzmine.util.maths.similarity.SimilarityMeasure;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math.MathException;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.jetbrains.annotations.Nullable;

public class FeatureCorrelationUtil {

  // Logger.
  private static final Logger LOG = Logger.getLogger(FeatureCorrelationUtil.class.getName());


  /**
   * Check Feature shape correlation. If it does not meet the criteria, fshape corr is removed from
   * the correlation
   *
   * @param featureList
   * @param minFFilter              Correlation in minimum number of samples
   * @param corr                    the correlation to check
   * @param useTotalShapeCorrFilter Filter by total shape correlation
   * @param minTotalShapeCorrR
   * @param minShapeCorrR           filter by minimum correlation
   * @param shapeSimMeasure         the similarity measure
   * @return
   */
  public static boolean checkFShapeCorr(FeatureList featureList, MinimumFeatureFilter minFFilter,
      R2RFullCorrelationData corr, boolean useTotalShapeCorrFilter, double minTotalShapeCorrR,
      double minShapeCorrR, SimilarityMeasure shapeSimMeasure) {
    // check feature shape corr
    if (!corr.hasFeatureShapeCorrelation()) {
      return false;
    }

    // deletes correlations if criteria is not met
    corr.validateFeatureCorrelation(minTotalShapeCorrR, useTotalShapeCorrFilter, minShapeCorrR,
        shapeSimMeasure);
    // check for correlation in min samples
    if (corr.hasFeatureShapeCorrelation()) {
      checkMinFCorrelation(featureList, minFFilter, corr);
    }

    return corr.hasFeatureShapeCorrelation();
  }

  /**
   * Final check if there are enough F2FCorrelations in samples and groups
   *
   * @param minFFilter
   * @param corr
   */
  public static void checkMinFCorrelation(FeatureList featureList, MinimumFeatureFilter minFFilter,
      R2RFullCorrelationData corr) {
    List<RawDataFile> raw = new ArrayList<>();
    for (Map.Entry<RawDataFile, CorrelationData> e : corr.getCorrFeatureShape().entrySet()) {
      if (e.getValue() != null && e.getValue().isValid()) {
        raw.add(e.getKey());
      }
    }
    boolean hasMinimumFeatures =
        minFFilter == null || minFFilter.filterMinFeatures(featureList.getRawDataFiles(), raw);
    if (!hasMinimumFeatures) {
      // delete corr peak shape
      corr.setCorrFeatureShape(null);
    }
  }


  /**
   * Feature height correlation (used as a filter), feature shape correlation used to group
   *
   * @param data                option to preload data or keep data in memory for large scale row 2
   *                            row correlation (null will access data directly from features)
   * @param raws
   * @param testRow
   * @param row
   * @param useHeightCorrFilter
   * @return R2R correlation, returns null if it was filtered by height correlation. Check for
   * validity on result
   */
  public static R2RFullCorrelationData corrR2R(CachedFeatureDataAccess data, List<RawDataFile> raws,
      FeatureListRow testRow, FeatureListRow row, boolean doFShapeCorr, int minCorrelatedDataPoints,
      int minCorrDPOnFeatureEdge, int minDPFHeightCorr, double minHeight,
      double noiseLevelShapeCorr, boolean useHeightCorrFilter, SimilarityMeasure heightSimilarity,
      double minHeightCorr) {
    // check height correlation across all samples
    // only used as exclusion filter - not to group
    CorrelationData heightCorr = null;

    if (useHeightCorrFilter) {
      heightCorr = FeatureCorrelationUtil.corrR2RFeatureHeight(raws, testRow, row, minHeight,
          noiseLevelShapeCorr, minDPFHeightCorr);

      // significance is alpha. 0 is perfect
      double maxHeightCorrSlopeSignificance = 0.3;
      double minHeightCorrFoldChange = 10;
      // do not group if slope is negative / too low
      // go on if heightCorr is null
      if (useHeightCorrFilter && heightCorr != null && FeatureCorrelationUtil.isNegativeRegression(
          heightCorr, minHeightCorrFoldChange, maxHeightCorrSlopeSignificance, minDPFHeightCorr,
          minHeightCorr, heightSimilarity)) {
        return null;
      }
    }

    // feature shape correlation
    Map<RawDataFile, CorrelationData> featureCorrMap = null;
    if (doFShapeCorr) {
      featureCorrMap = FeatureCorrelationUtil.corrR2RFeatureShapes(data, raws, testRow, row,
          minCorrelatedDataPoints, minCorrDPOnFeatureEdge, noiseLevelShapeCorr);
    }

    if (featureCorrMap != null && featureCorrMap.isEmpty()) {
      featureCorrMap = null;
    }

    return new R2RFullCorrelationData(testRow, row, heightCorr, featureCorrMap);
  }

  /**
   * Correlation of feature to feature shapes in all RawDataFiles of two rows
   *
   * @param data
   * @param raws
   * @param row
   * @param g
   * @return Map of feature shape correlation data (can be empty NON null)
   */
  public static Map<RawDataFile, CorrelationData> corrR2RFeatureShapes(CachedFeatureDataAccess data,
      final List<RawDataFile> raws, FeatureListRow row, FeatureListRow g,
      int minCorrelatedDataPoints, int minCorrDPOnFeatureEdge, double noiseLevelShapeCorr) {
    HashMap<RawDataFile, CorrelationData> corrData = new HashMap<>();
    // go through all raw files
    for (RawDataFile raw : raws) {
      Feature f1 = row.getFeature(raw);
      Feature f2 = g.getFeature(raw);
      if (f1 != null && f2 != null) {
        // feature shape correlation
        CorrelationData correlationData = corrFeatureShape(data, f1, f2, true,
            minCorrelatedDataPoints, minCorrDPOnFeatureEdge, noiseLevelShapeCorr);

        // if correlation is really bad return null
        if (isNegativeRegression(correlationData, 5, 0.2, 7, 0.5, SimilarityMeasure.PEARSON)) {
          return null;
        }
        // enough data points
        if (correlationData != null && correlationData.getDPCount() >= minCorrelatedDataPoints) {
          corrData.put(raw, correlationData);
        }
      }
    }
    return corrData;
  }

  /**
   * feature shape correlation
   *
   * @param data option to preload feature data for large scale comparison. null to directly access
   *             data from features
   * @param f1
   * @param f2
   * @return feature shape correlation or null if not possible not enough data points for a
   * correlation
   */
  public static CorrelationData corrFeatureShape(CachedFeatureDataAccess data, Feature f1,
      Feature f2, boolean sameRawFile, int minCorrelatedDataPoints, int minCorrDPOnFeatureEdge,
      double noiseLevelShapeCorr) {
    // f1 should be the higher feature
    if (f1.getHeight() < f2.getHeight()) {
      Feature tmp = f1;
      f1 = f2;
      f2 = tmp;
    }

    List<Scan> scansA = f1.getScanNumbers();
    List<Scan> scansB = f2.getScanNumbers();

    if (scansA.size() < minCorrelatedDataPoints || scansB.size() < minCorrelatedDataPoints) {
      return null;
    }

    // access data from features or preloaded data access
    final double[] intensities1;
    final double[] intensities2;
    if (data == null) {
      intensities1 = f1.getFeatureData().getIntensityValues(new double[f1.getNumberOfDataPoints()]);
      intensities2 = f2.getFeatureData().getIntensityValues(new double[f2.getNumberOfDataPoints()]);
    } else {
      intensities1 = data.getIntensityValues(f1);
      intensities2 = data.getIntensityValues(f2);
    }

    // find array index of max intensity for feature1 sn1
    int maxIndexOfA = indexOfMax(intensities1);

    if (sameRawFile) {
      // index offset between f1 and f2 data arrays (not all features are based on the same scans)
      int maxIndexInB = scansB.indexOf(scansA.get(maxIndexOfA));

      // save max and min of intensity of val1(x)
      List<double[]> corrData = new ArrayList<>();

      // add all data points <=max
      int i1 = maxIndexOfA;
      int i2 = maxIndexInB;
      while (i1 >= 0 && i2 >= 0) {
        Scan s1 = scansA.get(i1);
        Scan s2 = scansB.get(i2);
        // add point, if not break
        if (s1 == s2 && intensities1[i1] >= noiseLevelShapeCorr
            && intensities2[i2] >= noiseLevelShapeCorr) {
          corrData.add(new double[]{intensities1[i1], intensities2[i2]});
        } else {
          // end of feature found
          break;
        }
        i1--;
        i2--;
      }

      // check min data points left from apex
      int left = corrData.size() - 1;
      if (left < minCorrDPOnFeatureEdge) {
        return null;
      }

      // add all dp>max
      i1 = maxIndexOfA + 1;
      i2 = maxIndexInB + 1;
      while (i1 < scansA.size() && i2 < scansB.size()) {
        Scan s1 = scansA.get(i1);
        Scan s2 = scansB.get(i2);
        // add point, if not break
        if (s1 == s2 && intensities1[i1] >= noiseLevelShapeCorr
            && intensities2[i2] >= noiseLevelShapeCorr) {
          corrData.add(new double[]{intensities1[i1], intensities2[i2]});
        } else {
          // end of peak found
          break;
        }
        i1++;
        i2++;
      }
      // check right and total dp
      int right = corrData.size() - 1 - left;
      // return pearson r
      if (corrData.size() >= minCorrelatedDataPoints && right >= minCorrDPOnFeatureEdge) {
        return new FullCorrelationData(corrData);
      }
    } else {
      // TODO if different raw file search for same rt
      throw new UnsupportedOperationException(
          "We currently do not support feature shape correlation in two different raw data files");
      // impute rt/I values if between 2 data points
      /*
      int indexLeftA = 0;
      int indexLeftB = 0;

      // find first data points
      for (; indexLeftA < intensities1.length; indexLeftA++) {
        if (intensities1[indexLeftA] >= noiseLevelShapeCorr) {
          break;
        }
      }
      for (; indexLeftB < intensities2.length; indexLeftB++) {
        if (intensities2[indexLeftB] >= noiseLevelShapeCorr) {
          break;
        }
      }

      float rtLeftA = scansA.get(indexLeftA).getRetentionTime();
      float rtLeftB = scansB.get(indexLeftB).getRetentionTime();
      while (true) {
//        if()
        break;
      }
*/
    }
    return null;
  }

  /**
   * Find index of maximum value
   */
  public static int indexOfMax(double[] values) {
    int maxIndex = 0;
    double max = 0;
    for (int i = 0; i < values.length; i++) {
      double val = values[i];
      if (val > max) {
        maxIndex = i;
        max = val;
      }
    }
    return maxIndex;
  }

  /**
   * correlates the height profile of one row to another NO escape routine
   *
   * @param raw
   * @param row
   * @param g
   * @return Correlation data of i profile of max i (or null if no correlation)
   */
  public static CorrelationData corrR2RFeatureHeight(final List<RawDataFile> raw,
      FeatureListRow row, FeatureListRow g, double minHeight, double noiseLevel,
      int minDPFHeightCorr) {
    // minimum of two
    minDPFHeightCorr = Math.min(minDPFHeightCorr, 2);

    List<double[]> data = new ArrayList<>();
    // calc ratio
    double ratio = 0;
    SimpleRegression reg = new SimpleRegression();
    // go through all raw files
    for (RawDataFile rawDataFile : raw) {
      Feature f1 = row.getFeature(rawDataFile);
      Feature f2 = g.getFeature(rawDataFile);
      if (f1 != null && f2 != null) {
        // I profile correlation
        double a = f1.getHeight();
        double b = f2.getHeight();
        if (a >= minHeight && b >= minHeight) {
          data.add(new double[]{a, b});
          ratio += a / b;
          reg.addData(a, b);
        }
      }
    }

    ratio = ratio / data.size();
    if (ratio != 0) {
      // estimate missing values as noise level if > minHeight
      for (RawDataFile rawDataFile : raw) {
        Feature f1 = row.getFeature(rawDataFile);
        Feature f2 = g.getFeature(rawDataFile);

        boolean amissing = (f1 == null || f1.getHeight() < minHeight);
        boolean bmissing = (f2 == null || f2.getHeight() < minHeight);
        // xor
        if (amissing ^ bmissing) {
          double a = amissing ? f2.getHeight() * ratio : f1.getHeight();
          double b = bmissing ? f1.getHeight() / ratio : f2.getHeight();

          // only if both are >= min height
          if (a >= minHeight && b >= minHeight) {
            if (amissing) {
              a = Math.max(noiseLevel, f1 == null ? 0 : f1.getHeight());
            }
            if (bmissing) {
              b = Math.max(noiseLevel, f2 == null ? 0 : f2.getHeight());
            }
            data.add(new double[]{a, b});
            reg.addData(a, b);
          }
        }
      }
    }

    // TODO weighting of intensity corr
    if (data.size() < 2) {
      return null;
    } else {
      return new FullCorrelationData(data);
    }
  }


  /**
   * Only true if this should be filtered out. Need to have a minimum fold change to be
   * significant.
   *
   * @return
   */
  public static boolean isNegativeRegression(CorrelationData corr, double minFoldChange,
      double maxSlopeSignificance, int minDP, double minSimilarity,
      SimilarityMeasure heightSimilarity) {
    // do not check if data is not sufficient
    if (!isSufficientData(corr, minDP, minFoldChange)) {
      return false;
    }

    double significantSlope = 0;
    try {
      significantSlope = corr.getRegressionSignificance();
      if (Double.isNaN(significantSlope)) {
        return false;
      }
    } catch (MathException e) {
      LOG.log(Level.SEVERE, "slope significance cannot be calculated", e);
    }
    // if slope is negative
    // slope significance is low (alpha is high)
    // similarity is low
    return (corr.getSlope() <= 0 || (!Double.isNaN(significantSlope)
        && significantSlope > maxSlopeSignificance)
        || corr.getSimilarity(heightSimilarity) < minSimilarity);
  }

  /**
   * @param corr
   * @param minDP         minimum correlated data points
   * @param minFoldChange at least one data axis need to have >=maxFoldChange from min to max data
   * @return true if the correlation matches the rules false otherwise
   */
  public static boolean isSufficientData(CorrelationData corr, int minDP, double minFoldChange) {
    if (corr == null || (corr.getDPCount() < 3 || corr.getDPCount() < minDP)) {
      return false;
    }

    double maxFC = Math.max(Similarity.maxFoldChange(corr.getData(), 0),
        Similarity.maxFoldChange(corr.getData(), 1));
    // do not use as filter if
    if (maxFC < minFoldChange) {
      return false;
    }

    // is sufficient
    return true;
  }

  /**
   * Extension of the correlation util to process shapes with non-matching rts (or any other x
   * indices)
   *
   * @author <a href="https://github.com/SteffenHeu">SteffenHeu</a>
   */
  public static final class DIA {

    /**
     * Calculates {@link CorrelationData} for two peak shapes with non-equal but overlapping x value
     * ranges. Y values will be interpolated.
     *
     * @param x1                      x values of the first series
     * @param y1                      y values of the first series
     * @param x2                      x values of the second series
     * @param y2                      y values of the second series
     * @param minCorrelatedDataPoints mininum number of total correlated points.
     * @param minCorrDPOnFeatureEdge  miminum number of points per peak edge.
     * @param noiseLevelShapeCorr     minimum y value for correlation.
     * @return {@link CorrelationData} for the two shapes.
     */
    public static CorrelationData corrFeatureShape(final double[] x1, final double[] y1,
        final double[] x2, final double y2[], int minCorrelatedDataPoints,
        int minCorrDPOnFeatureEdge, double noiseLevelShapeCorr) {

      if (x1.length < minCorrelatedDataPoints || x2.length < minCorrelatedDataPoints) {
        return null;
      }

      double[][] f1, f2;

      // f1 should be the "longer" feature
      if (y1.length > y2.length) {
        f1 = new double[][]{x1, y1};
        f2 = new double[][]{x2, y2};
      } else {
        f1 = new double[][]{x2, y2};
        f2 = new double[][]{x1, y1};
      }

      // interpolate the feature shape of f2 onto f1
      var tempF2 = getInterpolatedShape(f1[0], f1[1], f2[0], f2[1]);

      // interpolate the shape of feature f1 onto original f2
      f1 = getInterpolatedShape(f2[0], f2[1], f1[0], f1[1]);
      f2 = tempF2;

      if (f2 == null || f1 == null) {
        return null;
      }

      final double[] intensities1 = f1[1];
      final double[] intensities2 = f2[1];

      // find array index of max intensity for feature1 sn1
      final int maxIndexOfA = indexOfMax(intensities1);

      // index offset between f1 and f2 data arrays (not all features are based on the same scans)
      final int maxIndexInB = ArrayUtils.indexOf(f1[0][maxIndexOfA], f2[0]);
      if (maxIndexInB == -1) {
        throw new IllegalStateException("Could not find original x value in interpolated shape.");
      }

      // save max and min of intensity of val1(x)
      List<double[]> corrData = new ArrayList<>();

      // add all data points <=max
      int i1 = maxIndexOfA;
      int i2 = maxIndexInB;
      while (i1 >= 0 && i2 >= 0) {
        double s1 = f1[0][i1];
        double s2 = f2[0][i2];
        // add point, if not break
        if (Double.compare(s1, s2) == 0 && intensities1[i1] >= noiseLevelShapeCorr && intensities2[i2] >= noiseLevelShapeCorr) {
          corrData.add(new double[]{intensities1[i1], intensities2[i2]});
        } else {
          // end of feature found
          break;
        }
        i1--;
        i2--;
      }

      // check min data points left from apex
      int left = corrData.size() - 1;
      if (left < minCorrDPOnFeatureEdge) {
        return null;
      }

      // add all dp>max
      i1 = maxIndexOfA + 1;
      i2 = maxIndexInB + 1;
      while (i1 < f1[0].length && i2 < f2[0].length) {
        double s1 = f1[0][i1];
        double s2 = f2[0][i2];
        // add point, if not break
        if (Double.compare(s1, s2) == 0 && intensities1[i1] >= noiseLevelShapeCorr
            && intensities2[i2] >= noiseLevelShapeCorr) {
          corrData.add(new double[]{intensities1[i1], intensities2[i2]});
        } else {
          // end of peak found
          break;
        }
        i1++;
        i2++;
      }
      // check right and total dp
      int right = corrData.size() - 1 - left;
      // return pearson r
      if (corrData.size() >= minCorrelatedDataPoints && right >= minCorrDPOnFeatureEdge) {
        return new FullCorrelationData(corrData);
      }

      return null;
    }

    /**
     * Interpolates a shape of two value series (x and y values) onto both x value arrays. Both x
     * value arrays must have overlapping value ranges. Y values for the "other" series will be
     * interpolated for the "main" series' x-values using basic 2-point interpolation (see
     * {@link DIA#interpolateY(double, double[], double[], int[])}). Values will only be
     * interpolated for the overlapping range. The final shape will contain both, the interpolated
     * and the original x and y values for the "other" series.
     *
     * @return Interpolated shape of the "other" series containing x ([0][]) and y ([1][]) values.
     * Null if there is no overlapping range.
     */
    @Nullable
    public static double[][] getInterpolatedShape(final double[] mainX, final double[] mainY,
        final double[] otherX, final double[] otherY) {
      assert mainX.length == mainY.length;
      assert otherX.length == otherY.length;

      // get number of overlapping points
      var mainRange = Range.closed(mainX[0], mainX[mainX.length - 1]);
      var otherRange = Range.closed(otherX[0], otherX[otherX.length - 1]);
      var overlap = mainRange.isConnected(otherRange) ? mainRange.intersection(otherRange) : null;
      if (overlap == null) {
        return null;
      }

      // find indices for the overlapping range
      final int[] otherIndicesEndExclusive = getAllowedRange(otherX, overlap);
      final int[] mainIndicesEndExclusive = getAllowedRange(mainX, overlap);
      if (otherIndicesEndExclusive == null || mainIndicesEndExclusive == null) {
        return null;
      }
      final int mainStart = mainIndicesEndExclusive[0];
      final int mainEnd = mainIndicesEndExclusive[1];
      final int otherStart = otherIndicesEndExclusive[0];
      final int otherEnd = otherIndicesEndExclusive[1];

      // create array for the interpolated data
      // copy the x values of both arrays to the new array
      final double newX[] = new double[mainEnd - mainStart + otherEnd - otherStart];
      System.arraycopy(mainX, mainStart, newX, 0, mainEnd - mainStart);
      System.arraycopy(otherX, otherStart, newX, mainEnd - mainStart, otherEnd - otherStart);
      Arrays.sort(newX);
      final double newY[] = new double[newX.length];

      final int[] lastVal = new int[]{0};
      for (int i = 0; i < newX.length; i++) {
        newY[i] = interpolateY(newX[i], otherX, otherY, lastVal);
      }

      return new double[][]{newX, newY};
    }

    /**
     * @param x             array of x-values, sorted by increasing value
     * @param allowedXRange value range for x-coordinates that may be included in the returned index
     *                      array.
     * @return An array of indices in the x array whose values are within the given range. the first
     * index is inclusive, the last index is exclusive for convenient usage with
     * {@link Arrays#copyOfRange(int[], int, int)} Null if there are no values within the given
     * range.
     */
    @Nullable
    private static int[] getAllowedRange(double[] x, Range<Double> allowedXRange) {
      int startIndex = -1;
      int endIndex = x.length - 1;

      for (int i = 0; i < x.length; i++) {
        if (allowedXRange.contains(x[i])) {
          startIndex = i;
          break;
        }
      }

      // no start within the given range.
      if (startIndex == -1) {
        return null;
      }

      for (int i = startIndex; i < x.length; i++) {
        if (!allowedXRange.contains(x[i])) {
          endIndex = i; // arrays.copyofrange is exclusive
          break;
        }
      }

      return new int[]{startIndex, endIndex};
    }

    /**
     * Interpolates a Y value from otherX and otherY at the given point x. x must be within the
     * bounds of otherX.
     *
     * @param x         The x value to interpolate y for.
     * @param otherX    The x values-.
     * @param otherY    The y values.
     * @param prevIndex An index to start searching for the new x value. Also serves as an *out*
     *                  variable to be passed to the next call.
     * @return the interpolated y value.
     */
    private static double interpolateY(double x, final double[] otherX, final double[] otherY, int[] prevIndex) {
      // check arguments
      assert otherX.length == otherY.length;
      if (!(otherX[0] <= x) || !(x <= otherX[otherX.length - 1])) {
        throw new IllegalArgumentException(
            String.format("Cannot interpolate y for x value %.3f within given bounds %.3f - %.3f", x, otherX[0], otherX[otherX.length - 1]));
      }

      int start = -1;
      int end = -1;

      // find the two points that lie around the given x value
      for (int i = prevIndex[0]; i < otherX.length; i++) { // could be optimized to start at the last found index
        if (otherX[i] >= x) { // may also be equal
          // if equal, return that value
          if (Double.compare(otherX[i], x) == 0) {
            prevIndex[0] = i;
            return otherY[i];
          }

          start = i - 1;
          break;
        }
      }

      for (int i = start; i < otherX.length; i++) {
        if (otherX[i] > x) {
          end = i;
          break;
        }
      }

      prevIndex[0] = end;

      return MathUtils.twoPointGetYForX(otherX[start], otherY[start], otherX[end], otherY[end], x);
    }
  }
}
