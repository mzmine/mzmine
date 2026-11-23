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
import it.unimi.dsi.fastutil.ints.IntArrays;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Quality metrics of chromatograms on real data without ground truth.
 * <ul>
 *   <li>Fillable holes: a chromatogram misses a data point in a short gap (1 or 2 scans) although
 *   the mass list of this scan has a signal within the tolerance of the chromatogram m/z. Stolen if
 *   all these signals are in other chromatograms.</li>
 *   <li>Co-apex pairs: two chromatograms within two times the tolerance with their apex within two
 *   scans. Split pairs rarely share scans around the apex, the signal of one ion was split into
 *   two chromatograms, i.e., duplicates. Co-eluting pairs share most scans, distinct signals
 *   resolved by the instrument.</li>
 * </ul>
 */
final class ChromatogramQualityMetrics {

  private static final int APEX_SCAN_DISTANCE = 2;
  private static final int OVERLAP_WINDOW = 10;
  private static final double SPLIT_MAX_OVERLAP = 0.2;

  private ChromatogramQualityMetrics() {
  }

  /**
   * @param shortGapScans                  missing scans in gaps of 1 or 2 scans
   * @param fillableHoles                  short gap scans with a signal within the tolerance
   * @param stolenHoles                    fillable holes whose signals all belong to other
   *                                       chromatograms
   * @param chromatogramsWithFillableHoles chromatograms with at least one fillable hole
   * @param splitPairs                     co-apex pairs within the tolerance that rarely share
   *                                       scans, duplicates
   * @param coelutingPairs                 co-apex pairs within the tolerance sharing most scans
   * @param wideSplitPairs                 like split pairs but 1 to 2 times the tolerance apart
   * @param wideCoelutingPairs             like co-eluting pairs but 1 to 2 times the tolerance
   *                                       apart
   */
  record Result(int chromatograms, long dataPoints, long shortGapScans, long fillableHoles,
                long stolenHoles, int chromatogramsWithFillableHoles, int splitPairs,
                int coelutingPairs, int wideSplitPairs, int wideCoelutingPairs) {

  }

  /**
   * @param reference            number of reference chromatograms with an apex above the minimum
   *                             height
   * @param matched              reference chromatograms whose apex data point is in another
   *                             chromatogram
   * @param meanOverlap          mean fraction of the data points of a matched reference
   *                             chromatogram in the chromatogram that contains its apex
   * @param unmatchedFailSegment unmatched reference chromatograms whose consecutive segment does
   *                             not reach the minimum height, see
   *                             FastChromatogramBuilder#passesFilters
   */
  record CrossMatch(int reference, int matched, double meanOverlap, int unmatchedFailSegment) {

    double matchedFraction() {
      return reference == 0 ? 0 : (double) matched / reference;
    }
  }

  /**
   * @param dips          runs of up to {@link #MAX_DIP_SCANS} scans between two data points of at
   *                      least the flank intensity in which a chromatogram has no data point or
   *                      only data points below the weaker flank by more than the intensity factor
   * @param fillableDips  dips with a mass list data point in at least half of their scans that is
   *                      within the window around the interpolated m/z and within the intensity
   *                      factor of the log interpolated intensity of the flanks, e.g., a saturated
   *                      apex with a shifted m/z
   * @param fillableScans scans of the fillable dips with such a data point
   */
  record Dips(int dips, int fillableDips, long fillableScans, @NotNull List<String> examples) {

  }

  static final int MAX_DIP_SCANS = 50;

  /**
   * Intensity dips inside intense chromatograms, the peak drops to zero or noise for a few scans.
   *
   * @param minFlank        min intensity of the data points before and after a dip
   * @param windowFactor    multiple of the tolerance around the interpolated m/z to look for data
   *                        points that could fill the dip
   * @param intensityFactor max ratio of a dip data point to the weaker flank and of a filling data
   *                        point to the interpolated intensity
   */
  @NotNull
  static Dips dips(@NotNull List<EvaluatedChromatogram> chromatograms,
      @NotNull double[][] massListMzs, @NotNull double[][] massListIntensities,
      @NotNull MZTolerance tolerance, double minFlank, double windowFactor,
      double intensityFactor) {
    final double logFactor = Math.log(intensityFactor);
    int dips = 0;
    int fillableDips = 0;
    long fillableScans = 0;
    final ChromatogramDataPointIndex owners = new ChromatogramDataPointIndex(chromatograms,
        massListMzs.length);
    final List<String> examples = new ArrayList<>();
    for (int c = 0; c < chromatograms.size(); c++) {
      final EvaluatedChromatogram chrom = chromatograms.get(c);
      final int[] scans = chrom.scans();
      final double[] mzs = chrom.mzs();
      final double[] intensities = chrom.intensities();
      int flank = -1;
      for (int i = 0; i < scans.length; i++) {
        if (intensities[i] < minFlank) {
          continue;
        }
        final int length = flank < 0 ? 0 : scans[i] - scans[flank] - 1;
        if (length >= 1 && length <= MAX_DIP_SCANS && isDip(intensities, flank, i,
            intensityFactor)) {
          dips++;
          int fillable = 0;
          final StringBuilder candidates = new StringBuilder();
          final double logBefore = Math.log(intensities[flank]);
          final double logAfter = Math.log(intensities[i]);
          for (int s = scans[flank] + 1; s < scans[i]; s++) {
            final double position = (double) (s - scans[flank]) / (length + 1);
            final double mz = mzs[flank] + position * (mzs[i] - mzs[flank]);
            final double logIntensity = logBefore + position * (logAfter - logBefore);
            final double window = windowFactor * tolerance.getMzToleranceForMass(mz);
            final double[] scanMzs = massListMzs[s];
            for (int k = ChannelConsolidation.lowerBound(scanMzs, mz - window);
                k < scanMzs.length && scanMzs[k] <= mz + window; k++) {
              if (Math.abs(Math.log(massListIntensities[s][k]) - logIntensity) <= logFactor) {
                fillable++;
                if (candidates.length() < 300) {
                  final int owner = owners.find(s, scanMzs[k]);
                  candidates.append(
                      " [scan %d %+.1f ppm %.3g %s]".formatted(s, (scanMzs[k] - mz) / mz * 1E6,
                          massListIntensities[s][k], owner < 0 ? "unused"
                              : "in %.5f".formatted(chromatograms.get(owner).weightedMz())));
                }
                break;
              }
            }
          }
          if (2 * fillable >= length) {
            fillableDips++;
            fillableScans += fillable;
            if (examples.size() < 30) {
              examples.add("mz %.5f scans %d-%d flanks %.3g / %.3g, %d of %d scans:%s".formatted(
                  chrom.weightedMz(), scans[flank], scans[i], intensities[flank], intensities[i],
                  fillable, length, candidates));
            }
          }
        }
        flank = i;
      }
    }
    return new Dips(dips, fillableDips, fillableScans, examples);
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

  @NotNull
  static Result evaluate(@NotNull List<EvaluatedChromatogram> chromatograms,
      @NotNull double[][] massListMzs, @NotNull MZTolerance tolerance) {
    final ChromatogramDataPointIndex used = new ChromatogramDataPointIndex(chromatograms,
        massListMzs.length);
    long dataPoints = 0;
    long gapScans = 0;
    long fillable = 0;
    long stolen = 0;
    int withFillable = 0;
    for (final EvaluatedChromatogram chrom : chromatograms) {
      dataPoints += chrom.size();
      final double mz = chrom.weightedMz();
      final double tol = tolerance.getMzToleranceForMass(mz);
      boolean hasFillable = false;
      final int[] scans = chrom.scans();
      for (int i = 1; i < scans.length; i++) {
        final int gap = scans[i] - scans[i - 1] - 1;
        if (gap < 1 || gap > 2) {
          continue;
        }
        for (int s = scans[i - 1] + 1; s < scans[i]; s++) {
          gapScans++;
          final double[] scanMzs = massListMzs[s];
          boolean any = false;
          boolean unused = false;
          for (int k = ChannelConsolidation.lowerBound(scanMzs, mz - tol);
              k < scanMzs.length && scanMzs[k] <= mz + tol; k++) {
            any = true;
            if (used.find(s, scanMzs[k]) < 0) {
              unused = true;
            }
          }
          if (any) {
            fillable++;
            hasFillable = true;
            if (!unused) {
              stolen++;
            }
          }
        }
      }
      if (hasFillable) {
        withFillable++;
      }
    }

    // co-apex pairs within two times the tolerance
    final int n = chromatograms.size();
    final double[] mzs = new double[n];
    final int[] apexScans = new int[n];
    for (int c = 0; c < n; c++) {
      final EvaluatedChromatogram chrom = chromatograms.get(c);
      mzs[c] = chrom.weightedMz();
      apexScans[c] = chrom.size() == 0 ? -1 : chrom.scans()[chrom.apexIndex()];
    }
    final int[] byMz = ChannelConsolidation.identity(n);
    IntArrays.quickSort(byMz, 0, n, (a, b) -> Double.compare(mzs[a], mzs[b]));
    final int[] pairs = new int[4];
    for (int i = 0; i < n; i++) {
      final int a = byMz[i];
      final double tol = tolerance.getMzToleranceForMass(mzs[a]);
      for (int j = i + 1; j < n && mzs[byMz[j]] <= mzs[a] + 2 * tol; j++) {
        final int b = byMz[j];
        if (Math.abs(apexScans[a] - apexScans[b]) > APEX_SCAN_DISTANCE) {
          continue;
        }
        final boolean within = mzs[byMz[j]] - mzs[a] <= tol;
        final boolean split =
            overlapAroundApex(chromatograms.get(a), chromatograms.get(b), apexScans[a])
                < SPLIT_MAX_OVERLAP;
        pairs[(within ? 0 : 2) + (split ? 0 : 1)]++;
      }
    }
    return new Result(n, dataPoints, gapScans, fillable, stolen, withFillable, pairs[0], pairs[1],
        pairs[2], pairs[3]);
  }

  /**
   * Matches the apex data points of the reference chromatograms exactly in the other list.
   */
  @NotNull
  static CrossMatch crossMatch(@NotNull List<EvaluatedChromatogram> reference,
      @NotNull List<EvaluatedChromatogram> other, int numScans, int minConsecutive,
      double minGroupIntensity, double minHeight) {
    final ChromatogramDataPointIndex index = new ChromatogramDataPointIndex(other, numScans);
    int numReference = 0;
    int matched = 0;
    int unmatchedFailSegment = 0;
    double overlapSum = 0;
    for (final EvaluatedChromatogram chrom : reference) {
      final int apex = chrom.apexIndex();
      if (apex < 0 || chrom.intensities()[apex] < minHeight) {
        continue;
      }
      numReference++;
      final int target = index.find(chrom.scans()[apex], chrom.mzs()[apex]);
      if (target < 0) {
        if (!FastChromatogramBuilder.passesFilters(chrom.scans(), chrom.intensities(), chrom.size(),
            minConsecutive, minGroupIntensity, minHeight)) {
          unmatchedFailSegment++;
        }
        continue;
      }
      matched++;
      int shared = 0;
      for (int i = 0; i < chrom.size(); i++) {
        if (index.find(chrom.scans()[i], chrom.mzs()[i]) == target) {
          shared++;
        }
      }
      overlapSum += (double) shared / chrom.size();
    }
    return new CrossMatch(numReference, matched, matched == 0 ? 0 : overlapSum / matched,
        unmatchedFailSegment);
  }

  /**
   * Fraction of scans with data points in both chromatograms around the apex, relative to the
   * chromatogram with fewer data points in the window.
   */
  private static double overlapAroundApex(@NotNull EvaluatedChromatogram a,
      @NotNull EvaluatedChromatogram b, int apex) {
    final int from = apex - OVERLAP_WINDOW;
    final int to = apex + OVERLAP_WINDOW;
    final int[] scansA = a.scans();
    final int[] scansB = b.scans();
    int i = lowerBound(scansA, from);
    int j = lowerBound(scansB, from);
    int countA = 0;
    int countB = 0;
    int shared = 0;
    while ((i < scansA.length && scansA[i] <= to) || (j < scansB.length && scansB[j] <= to)) {
      final int sa = i < scansA.length && scansA[i] <= to ? scansA[i] : Integer.MAX_VALUE;
      final int sb = j < scansB.length && scansB[j] <= to ? scansB[j] : Integer.MAX_VALUE;
      if (sa == sb) {
        shared++;
        countA++;
        countB++;
        i++;
        j++;
      } else if (sa < sb) {
        countA++;
        i++;
      } else {
        countB++;
        j++;
      }
    }
    final int smaller = Math.min(countA, countB);
    return smaller == 0 ? 0 : (double) shared / smaller;
  }

  /**
   * @return first index with values[index] >= key
   */
  private static int lowerBound(@NotNull int[] values, int key) {
    int lo = 0;
    int hi = values.length;
    while (lo < hi) {
      final int mid = (lo + hi) >>> 1;
      if (values[mid] < key) {
        lo = mid + 1;
      } else {
        hi = mid;
      }
    }
    return lo;
  }
}
