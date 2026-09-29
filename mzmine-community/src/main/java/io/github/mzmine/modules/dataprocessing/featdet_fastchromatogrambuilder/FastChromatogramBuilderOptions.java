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

import org.jetbrains.annotations.NotNull;

/**
 * Internal tuning options of the {@link FastChromatogramBuilder}. They are not user parameters, the
 * defaults were chosen with the benchmark in ChromatogramBuilderBenchmark (test sources).
 *
 * @param maxGapScans                  scans without data point after which a trace is closed.
 *                                     Longer gaps split traces, which are joined again in the
 *                                     channel consolidation. Small values keep the number of active
 *                                     traces low. Also the longest hole that is filled with the
 *                                     wider hole fill tolerance, and plus one the max distance of
 *                                     complementary traces and channels.
 * @param singleDataPointMaxGapScans   same for traces with a single data point. Most of these are
 *                                     noise, closing them early keeps the active traces few. The
 *                                     data point of a real signal is not lost, it fills its
 *                                     chromatogram as loose data point in the second pass.
 * @param intensityJumpFactor          intensity ratio between neighboring scans of a trace that is
 *                                     expected within a peak and matched without extra cost. Also
 *                                     the tolerated intensity ratio of a hole fill to the
 *                                     interpolated intensity, and of a complementary channel at its
 *                                     apex to the channel it merges into.
 * @param intensityJumpWeight          weight of the matching cost for larger intensity jumps, 0
 *                                     matches on the m/z distance only
 * @param separateCollidingTraces      traces within the tolerance that repeatedly receive data
 *                                     points in the same scans are kept in separate channels
 * @param maxCollisionFraction         collisions tolerated between traces of one channel as
 *                                     fraction of the smaller trace
 * @param complementaryToleranceFactor traces up to this multiple of the tolerance away from a
 *                                     channel join it if they overlap in time without ever sharing
 *                                     a scan, the typical pattern of one ion with a large m/z
 *                                     scatter. Also the window of the complementary channel merge
 *                                     after the second pass, see {@link ChannelFinalization}.
 *                                     Values <= 1 disable both.
 * @param holeFillToleranceFactor      holes between intense data points of a channel are filled
 *                                     with unused data points up to this multiple of the tolerance
 *                                     around the m/z of the neighbors. Values <= 0 disable this.
 * @param holeFillFlank                the min intensity of both neighbors of a hole that the second
 *                                     pass and the recovery of failed channels fill with the wider
 *                                     hole fill tolerance
 * @param dipBridgeToleranceFactor     a segment of another channel up to this multiple of the
 *                                     tolerance away that fills a dip between intense data points
 *                                     of a channel and continues their intensity moves into it, e.g.,
 *                                     the shifted m/z of a saturated apex, see
 *                                     {@link ChannelFinalization}. Values <= 0 disable this.
 * @param dipBridgeIntensityFraction   only segments with a data point of at least this fraction of
 *                                     the most intense data point of all scans are bridged, the
 *                                     high data points close to the detector limit whose m/z
 *                                     shifts
 * @param coalescedMaxHoleScans        holes between intense data points of a channel are filled
 *                                     with the data point of a neighboring channel in the same scan
 *                                     that lies between both centers, one centroid of two ions that
 *                                     the instrument did not resolve, see
 *                                     {@link ChannelFinalization}. Holes up to the max gap scans are
 *                                     filled scan by scan, longer holes up to this length only if
 *                                     every scan is filled. Values <= 0 disable this.
 */
record FastChromatogramBuilderOptions(int maxGapScans, int singleDataPointMaxGapScans,
                                      double intensityJumpFactor, double intensityJumpWeight,
                                      boolean separateCollidingTraces, double maxCollisionFraction,
                                      double complementaryToleranceFactor,
                                      double holeFillToleranceFactor,
                                      @NotNull HoleFillFlank holeFillFlank,
                                      double dipBridgeToleranceFactor,
                                      double dipBridgeIntensityFraction,
                                      int coalescedMaxHoleScans) {

  static final FastChromatogramBuilderOptions DEFAULT = new FastChromatogramBuilderOptions(3, 0, 5d,
      0.25d, true, 0.1d, 2d, 2d, HoleFillFlank.MIN_GROUP_INTENSITY, 4d, 0.5d, 8);

  /**
   * The min intensity of the neighbors of a hole that is filled with the wider hole fill tolerance.
   */
  enum HoleFillFlank {
    /**
     * the min height of the builder, only intense signals
     */
    MIN_HEIGHT,
    /**
     * the min group intensity of the builder, the signals of the consecutive segments
     */
    MIN_GROUP_INTENSITY,
    /**
     * no min intensity, the intensity of the filling data point is still checked
     */
    ANY;

    /**
     * @return the min intensity of both neighbors of a hole
     */
    double minIntensity(double minGroupIntensity, double minHeight) {
      return switch (this) {
        case MIN_HEIGHT -> minHeight;
        case MIN_GROUP_INTENSITY -> minGroupIntensity;
        case ANY -> 0d;
      };
    }
  }

  FastChromatogramBuilderOptions {
    if (maxGapScans < 0 || singleDataPointMaxGapScans < 0) {
      throw new IllegalArgumentException("Gaps must be >= 0");
    }
    if (!(intensityJumpFactor >= 1d)) {
      throw new IllegalArgumentException("intensityJumpFactor must be >= 1");
    }
  }

  @NotNull FastChromatogramBuilderOptions withIntensityJumpWeight(double weight) {
    return new FastChromatogramBuilderOptions(maxGapScans, singleDataPointMaxGapScans,
        intensityJumpFactor, weight, separateCollidingTraces, maxCollisionFraction,
        complementaryToleranceFactor, holeFillToleranceFactor, holeFillFlank, dipBridgeToleranceFactor,
        dipBridgeIntensityFraction, coalescedMaxHoleScans);
  }

  @NotNull FastChromatogramBuilderOptions withSeparateCollidingTraces(boolean separate) {
    return new FastChromatogramBuilderOptions(maxGapScans, singleDataPointMaxGapScans,
        intensityJumpFactor, intensityJumpWeight, separate, maxCollisionFraction,
        complementaryToleranceFactor, holeFillToleranceFactor, holeFillFlank, dipBridgeToleranceFactor,
        dipBridgeIntensityFraction, coalescedMaxHoleScans);
  }

  @NotNull FastChromatogramBuilderOptions withComplementaryToleranceFactor(double factor) {
    return new FastChromatogramBuilderOptions(maxGapScans, singleDataPointMaxGapScans,
        intensityJumpFactor, intensityJumpWeight, separateCollidingTraces, maxCollisionFraction,
        factor, holeFillToleranceFactor, holeFillFlank, dipBridgeToleranceFactor,
        dipBridgeIntensityFraction, coalescedMaxHoleScans);
  }

  @NotNull FastChromatogramBuilderOptions withMaxGapScans(int gapScans,
      int singleDataPointGapScans) {
    return new FastChromatogramBuilderOptions(gapScans, singleDataPointGapScans,
        intensityJumpFactor, intensityJumpWeight, separateCollidingTraces, maxCollisionFraction,
        complementaryToleranceFactor, holeFillToleranceFactor, holeFillFlank, dipBridgeToleranceFactor,
        dipBridgeIntensityFraction, coalescedMaxHoleScans);
  }

  @NotNull FastChromatogramBuilderOptions withHoleFillToleranceFactor(double factor) {
    return new FastChromatogramBuilderOptions(maxGapScans, singleDataPointMaxGapScans,
        intensityJumpFactor, intensityJumpWeight, separateCollidingTraces, maxCollisionFraction,
        complementaryToleranceFactor, factor, holeFillFlank, dipBridgeToleranceFactor,
        dipBridgeIntensityFraction, coalescedMaxHoleScans);
  }

  @NotNull FastChromatogramBuilderOptions withHoleFillFlank(@NotNull HoleFillFlank flank) {
    return new FastChromatogramBuilderOptions(maxGapScans, singleDataPointMaxGapScans,
        intensityJumpFactor, intensityJumpWeight, separateCollidingTraces, maxCollisionFraction,
        complementaryToleranceFactor, holeFillToleranceFactor, flank, dipBridgeToleranceFactor,
        dipBridgeIntensityFraction, coalescedMaxHoleScans);
  }

  @NotNull FastChromatogramBuilderOptions withDipBridgeToleranceFactor(double factor) {
    return new FastChromatogramBuilderOptions(maxGapScans, singleDataPointMaxGapScans,
        intensityJumpFactor, intensityJumpWeight, separateCollidingTraces, maxCollisionFraction,
        complementaryToleranceFactor, holeFillToleranceFactor, holeFillFlank, factor,
        dipBridgeIntensityFraction, coalescedMaxHoleScans);
  }

  @NotNull FastChromatogramBuilderOptions withCoalescedMaxHoleScans(int scans) {
    return new FastChromatogramBuilderOptions(maxGapScans, singleDataPointMaxGapScans,
        intensityJumpFactor, intensityJumpWeight, separateCollidingTraces, maxCollisionFraction,
        complementaryToleranceFactor, holeFillToleranceFactor, holeFillFlank, dipBridgeToleranceFactor,
        dipBridgeIntensityFraction, scans);
  }

  /**
   * @return true if the first pass needs to record collisions
   */
  boolean needsCollisions() {
    return separateCollidingTraces || complementaryToleranceFactor > 1d;
  }

  /**
   * @return the m/z window for collisions as multiple of the tolerance
   */
  double collisionToleranceFactor() {
    return Math.max(1d, complementaryToleranceFactor);
  }
}
