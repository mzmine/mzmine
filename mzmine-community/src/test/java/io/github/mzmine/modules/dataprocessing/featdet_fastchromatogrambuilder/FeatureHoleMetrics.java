/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder;

import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Holes in the final result on real data without ground truth. A dip is a run of up to
 * {@link #MAX_DIP_SCANS} scans between two data points (flanks) of a chromatogram in which the
 * chromatogram has no data point or only data points below the weaker flank by more than the
 * intensity factor. The resolved features of the same builder decide whether a dip matters:
 * <ul>
 *   <li>inside: both flanks are in one resolved feature, the feature has a hole</li>
 *   <li>split: the flanks are in two resolved features, the dip split one peak</li>
 *   <li>truncated: one flank is in a resolved feature, the feature ends at the dip</li>
 * </ul>
 * Dips outside of all resolved features do not change the result and are ignored. The expected
 * intensity of a dip scan is log interpolated between the flanks, a dip is intense if its highest
 * expected intensity reaches {@link #INTENSE_FRACTION} of the height of the feature, at the apex
 * if it reaches {@link #APEX_FRACTION}. A dip is fillable if the mass lists have the signal: in at
 * least half of its scans a data point within the window around the interpolated m/z and within the
 * intensity factor of the expected intensity that is unused or in another chromatogram without data
 * point in both flank scans. A chromatogram with a data point in a flank scan is a co-eluting
 * neighbor ion, not a part of the dipping ion, the rule of complementary channels that never share
 * a scan. Unfillable dips are dropouts of the raw data, which no builder can fill.
 */
final class FeatureHoleMetrics {

  // decision: 10% of the height changes the shape and the area of a feature, 50% is at the apex
  static final double INTENSE_FRACTION = 0.1;
  static final double APEX_FRACTION = 0.5;
  static final int MAX_DIP_SCANS = 50;

  private FeatureHoleMetrics() {
  }

  enum Location {
    INSIDE, SPLIT, TRUNCATED
  }

  /**
   * @param location           where the dip is relative to the resolved features
   * @param mz                 intensity weighted m/z of the chromatogram
   * @param firstScan          first scan of the dip
   * @param lastScan           last scan of the dip, inclusive
   * @param flankBefore        intensity of the data point before the dip
   * @param flankAfter         intensity of the data point after the dip
   * @param relativeIntensity  highest expected intensity relative to the height of the feature, the
   *                           higher feature for a split
   * @param fillableScans      dip scans with a mass list data point that fills them
   * @param stolenScans        fillable scans whose data point is in another chromatogram
   * @param fillableIntensity  intensity of the data points that fill the dip
   * @param dipDataPoints      data points of the chromatogram in the dip, all below the weaker
   *                           flank by more than the intensity factor
   * @param featureBefore      index of the resolved feature with the flank before the dip, -1 for
   *                           none
   * @param featureAfter       index of the resolved feature with the flank after the dip, -1 for
   *                           none
   * @param candidates         description of the filling data points, for the log
   */
  record Dip(@NotNull Location location, double mz, int firstScan, int lastScan,
             double flankBefore, double flankAfter, double relativeIntensity, int fillableScans,
             int stolenScans, double fillableIntensity, int dipDataPoints, int featureBefore,
             int featureAfter, @NotNull String candidates) {

    int length() {
      return lastScan - firstScan + 1;
    }

    boolean isFillable() {
      return 2 * fillableScans >= length();
    }

    boolean isIntense() {
      return relativeIntensity >= INTENSE_FRACTION;
    }

    /**
     * @return true if most filling data points are in other chromatograms
     */
    boolean isStolen() {
      return 2 * stolenScans > fillableScans;
    }

    @Override
    public @NotNull String toString() {
      return "%s mz %.5f scans %d-%d flanks %.3g / %.3g, %.0f%% of height, %d data points in the dip, %d of %d scans fillable (%d stolen):%s".formatted(
          location, mz, firstScan, lastScan, flankBefore, flankAfter, 100 * relativeIntensity,
          dipDataPoints, fillableScans, length(), stolenScans, candidates);
    }
  }

  /**
   * @param features           resolved features
   * @param intenseHoles       intense dips inside features, fillable
   * @param intenseHolesAll    intense dips inside features, including dropouts of the raw data
   * @param apexHoles          fillable dips inside features at the apex
   * @param splitPeaks         intense fillable dips that split one peak into two features
   * @param truncatedFeatures  intense fillable dips at which a feature ends
   * @param stolen             intense fillable dips whose filling data points are mostly in other
   *                           chromatograms
   * @param affectedFeatures   features with at least one intense fillable dip
   * @param lostIntensity      intensity of the filling data points of all fillable dips
   * @param featureIntensity   intensity of all data points of all features
   * @param examples           the intense fillable dips with the most missing intensity, up to 10
   *                           of each location
   */
  record Result(int features, int intenseHoles, int intenseHolesAll, int apexHoles,
                int splitPeaks, int truncatedFeatures, int stolen, int affectedFeatures,
                double lostIntensity, double featureIntensity, @NotNull List<String> examples) {

    /**
     * @return the intensity of the filling data points relative to the intensity of all features
     */
    double lostSignalFraction() {
      return featureIntensity > 0 ? lostIntensity / featureIntensity : 0;
    }
  }

  /**
   * @param resolved        detected data points of the resolved features
   * @param chromatograms   detected data points of the chromatograms that were resolved
   * @param windowFactor    multiple of the tolerance around the interpolated m/z to look for data
   *                        points that fill a dip
   * @param intensityFactor max ratio of a dip data point to the weaker flank and of a filling data
   *                        point to the expected intensity
   */
  @NotNull
  static Result evaluate(@NotNull List<EvaluatedChromatogram> resolved,
      @NotNull List<EvaluatedChromatogram> chromatograms, @NotNull double[][] massListMzs,
      @NotNull double[][] massListIntensities, @NotNull MZTolerance tolerance, double windowFactor,
      double intensityFactor) {
    final List<Dip> dips = findDips(resolved, chromatograms, massListMzs, massListIntensities,
        tolerance, windowFactor, intensityFactor);
    double totalIntensity = 0;
    for (final EvaluatedChromatogram feature : resolved) {
      for (final double intensity : feature.intensities()) {
        totalIntensity += intensity;
      }
    }

    final List<Dip> intenseFillable = new ArrayList<>();
    final IntOpenHashSet affected = new IntOpenHashSet();
    int intenseHolesAll = 0;
    int apexHoles = 0;
    double lostIntensity = 0;
    for (final Dip dip : dips) {
      if (dip.isFillable()) {
        lostIntensity += dip.fillableIntensity();
      }
      if (!dip.isIntense()) {
        continue;
      }
      if (dip.location() == Location.INSIDE) {
        intenseHolesAll++;
      }
      if (!dip.isFillable()) {
        continue;
      }
      intenseFillable.add(dip);
      if (dip.location() == Location.INSIDE && dip.relativeIntensity() >= APEX_FRACTION) {
        apexHoles++;
      }
      if (dip.featureBefore() >= 0) {
        affected.add(dip.featureBefore());
      }
      if (dip.featureAfter() >= 0) {
        affected.add(dip.featureAfter());
      }
    }

    int intenseHoles = 0;
    int splitPeaks = 0;
    int truncated = 0;
    int stolen = 0;
    for (final Dip dip : intenseFillable) {
      switch (dip.location()) {
        case INSIDE -> intenseHoles++;
        case SPLIT -> splitPeaks++;
        case TRUNCATED -> truncated++;
      }
      if (dip.isStolen()) {
        stolen++;
      }
    }
    // the dips with the most missing intensity, up to 10 of each location
    intenseFillable.sort(Comparator.comparingDouble(Dip::fillableIntensity).reversed());
    final List<String> examples = new ArrayList<>();
    for (final Location location : Location.values()) {
      intenseFillable.stream().filter(dip -> dip.location() == location).limit(10)
          .map(Dip::toString).forEach(examples::add);
    }
    return new Result(resolved.size(), intenseHoles, intenseHolesAll, apexHoles, splitPeaks,
        truncated, stolen, affected.size(), lostIntensity, totalIntensity, examples);
  }

  /**
   * @return all dips inside, between or at the end of resolved features, see
   * {@link #evaluate}
   */
  @NotNull
  static List<Dip> findDips(@NotNull List<EvaluatedChromatogram> resolved,
      @NotNull List<EvaluatedChromatogram> chromatograms, @NotNull double[][] massListMzs,
      @NotNull double[][] massListIntensities, @NotNull MZTolerance tolerance, double windowFactor,
      double intensityFactor) {
    final int numScans = massListMzs.length;
    final ChromatogramDataPointIndex owners = new ChromatogramDataPointIndex(chromatograms,
        numScans);

    // resolved features by the chromatogram of their apex data point
    final int numFeatures = resolved.size();
    final int[] first = new int[numFeatures];
    final int[] last = new int[numFeatures];
    final double[] heights = new double[numFeatures];
    final List<IntArrayList> featuresOfChromatogram = new ArrayList<>(chromatograms.size());
    for (int c = 0; c < chromatograms.size(); c++) {
      featuresOfChromatogram.add(null);
    }
    for (int f = 0; f < numFeatures; f++) {
      final EvaluatedChromatogram feature = resolved.get(f);
      final int apex = feature.apexIndex();
      if (apex < 0) {
        continue;
      }
      final int c = owners.find(feature.scans()[apex], feature.mzs()[apex]);
      if (c < 0) {
        continue;
      }
      first[f] = feature.scans()[0];
      last[f] = feature.scans()[feature.size() - 1];
      heights[f] = feature.intensities()[apex];
      if (featuresOfChromatogram.get(c) == null) {
        featuresOfChromatogram.set(c, new IntArrayList());
      }
      featuresOfChromatogram.get(c).add(f);
    }

    final List<Dip> dips = new ArrayList<>();
    final double logFactor = Math.log(intensityFactor);
    for (int c = 0; c < chromatograms.size(); c++) {
      final IntArrayList features = featuresOfChromatogram.get(c);
      if (features == null) {
        continue;
      }
      final EvaluatedChromatogram chrom = chromatograms.get(c);
      final int[] scans = chrom.scans();
      final double[] mzs = chrom.mzs();
      final double[] intensities = chrom.intensities();
      final int n = scans.length;
      int a = 0;
      while (a < n - 1) {
        final int b = rightFlank(scans, intensities, a, intensityFactor);
        if (b < 0 || !isDip(intensities, a, b, intensityFactor)) {
          a++;
          continue;
        }
        final int before = containing(features, first, last, scans[a]);
        final int after = containing(features, first, last, scans[b]);
        final Location location;
        final double height;
        if (before >= 0 && before == after) {
          location = Location.INSIDE;
          height = heights[before];
        } else if (before >= 0 && after >= 0) {
          location = Location.SPLIT;
          height = Math.max(heights[before], heights[after]);
        } else if (before >= 0 || after >= 0) {
          location = Location.TRUNCATED;
          height = heights[Math.max(before, after)];
        } else {
          a = b;
          continue;
        }

        // expected signal in the dip scans and the mass list data points that fill it
        final int length = scans[b] - scans[a] - 1;
        final double logBefore = Math.log(intensities[a]);
        final double logAfter = Math.log(intensities[b]);
        double maxExpected = 0;
        int fillable = 0;
        int stolen = 0;
        double fillIntensity = 0;
        final StringBuilder candidates = new StringBuilder();
        for (int s = scans[a] + 1; s < scans[b]; s++) {
          final double position = (double) (s - scans[a]) / (length + 1);
          final double mz = mzs[a] + position * (mzs[b] - mzs[a]);
          final double logExpected = logBefore + position * (logAfter - logBefore);
          maxExpected = Math.max(maxExpected, Math.exp(logExpected));
          final int k = closestFilling(massListMzs[s], massListIntensities[s], s, mz,
              windowFactor * tolerance.getMzToleranceForMass(mz), logExpected, logFactor, owners,
              chromatograms, scans[a], scans[b]);
          if (k < 0) {
            continue;
          }
          fillable++;
          final double fillMz = massListMzs[s][k];
          fillIntensity += massListIntensities[s][k];
          final int owner = owners.find(s, fillMz);
          if (owner >= 0) {
            stolen++;
          }
          if (candidates.length() < 300) {
            candidates.append(" [scan %d %+.1f ppm %.3g %s]".formatted(s, (fillMz - mz) / mz * 1E6,
                massListIntensities[s][k], owner < 0 ? "unused"
                    : "in %.5f".formatted(chromatograms.get(owner).weightedMz())));
          }
        }
        dips.add(new Dip(location, chrom.weightedMz(), scans[a] + 1, scans[b] - 1,
            intensities[a], intensities[b], maxExpected / height, fillable, stolen,
            fillIntensity, b - a - 1, before, after, candidates.toString()));
        a = b;
      }
    }
    return dips;
  }

  /**
   * @return the first data point after a that reaches the intensity of a / factor within
   * {@link #MAX_DIP_SCANS} scans, or -1. All data points in between are below.
   */
  private static int rightFlank(@NotNull int[] scans, @NotNull double[] intensities, int a,
      double intensityFactor) {
    final double threshold = intensities[a] / intensityFactor;
    for (int b = a + 1; b < scans.length && scans[b] - scans[a] - 1 <= MAX_DIP_SCANS; b++) {
      if (intensities[b] >= threshold) {
        return scans[b] - scans[a] >= 2 ? b : -1;
      }
    }
    return -1;
  }

  /**
   * @return true if all data points between both flanks are below the weaker flank by more than the
   * intensity factor
   */
  private static boolean isDip(@NotNull double[] intensities, int before, int after,
      double intensityFactor) {
    final double weakerFlank = Math.min(intensities[before], intensities[after]);
    for (int k = before + 1; k < after; k++) {
      if (intensities[k] * intensityFactor >= weakerFlank) {
        return false;
      }
    }
    return true;
  }

  /**
   * @return the feature of the chromatogram whose scan range contains the scan or -1
   */
  private static int containing(@NotNull IntArrayList features, @NotNull int[] first,
      @NotNull int[] last, int scan) {
    for (int i = 0; i < features.size(); i++) {
      final int f = features.getInt(i);
      if (first[f] <= scan && scan <= last[f]) {
        return f;
      }
    }
    return -1;
  }

  /**
   * @param scan        the dip scan
   * @param flankBefore scan of the flank before the dip
   * @param flankAfter  scan of the flank after the dip
   * @return index of the data point closest to the m/z within the window whose intensity is within
   * the factor of the expected intensity and that is unused or in a chromatogram without data point
   * in both flank scans, -1 if there is none
   */
  private static int closestFilling(@NotNull double[] scanMzs, @NotNull double[] scanIntensities,
      int scan, double mz, double window, double logExpected, double logFactor,
      @NotNull ChromatogramDataPointIndex owners,
      @NotNull List<EvaluatedChromatogram> chromatograms, int flankBefore, int flankAfter) {
    int best = -1;
    for (int k = ChannelConsolidation.lowerBound(scanMzs, mz - window);
        k < scanMzs.length && scanMzs[k] <= mz + window; k++) {
      if (Math.abs(Math.log(scanIntensities[k]) - logExpected) > logFactor || (best >= 0
          && Math.abs(scanMzs[k] - mz) >= Math.abs(scanMzs[best] - mz))) {
        continue;
      }
      final int owner = owners.find(scan, scanMzs[k]);
      if (owner >= 0 && (hasScan(chromatograms.get(owner), flankBefore) || hasScan(
          chromatograms.get(owner), flankAfter))) {
        continue;
      }
      best = k;
    }
    return best;
  }

  private static boolean hasScan(@NotNull EvaluatedChromatogram chrom, int scan) {
    return Arrays.binarySearch(chrom.scans(), scan) >= 0;
  }
}
