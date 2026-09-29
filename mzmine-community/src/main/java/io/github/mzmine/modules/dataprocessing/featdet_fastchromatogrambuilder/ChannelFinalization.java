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
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.longs.LongArrays;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Last step of the {@link FastChromatogramBuilder}, after the second pass all data points of the
 * channels are known:
 * <ol>
 *   <li>Complementary channels merge. One ion yields one data point per scan, so two channels up to
 *   the complementary tolerance apart that never share a scan are parts of one ion if they touch
 *   in time, e.g., the apex of an intense ion with a shifted m/z and its tails. The channel
 *   consolidation joins such traces only if they touch the seed trace, the traces of the tails are
 *   often further away. A part of one ion continues the intensity of the other part, so the target
 *   channel needs a data point within the gap allowance of the apex of the merging channel, at
 *   least the apex intensity divided by the intensity jump factor. Otherwise, the merging channel
 *   is a peak of its own, e.g., another ion that elutes while the baseline of an intense channel is
 *   missing.</li>
 *   <li>Channels that fail the filters merge the same way into a passing channel.</li>
 *   <li>Dips are bridged: a segment of another channel with high data points moves into a passing
 *   channel if it fills a dip between intense data points of the channel, within the dip bridge
 *   tolerance. The m/z of an ion that saturates the detector shifts at the apex, e.g., by up to 60
 *   ppm on GC-EI-QTOF data, beyond the tolerance and the complementary tolerance, and the intensity
 *   of its chromatogram drops to zero or noise within the peak. High means close to the most
 *   intense data point of all scans, the detector limit. A segment is a run of data points with
 *   gaps of at most the max gap scans. The channel has an intense data point within the gap
 *   allowance on both sides of the segment that continues the intensity at the segment edges, no
 *   data point of a similar intensity in a scan of the segment, and around the segment at least
 *   half as many data points as the segment. The channel data points in the scans of the segment
 *   are noise and are replaced.</li>
 *   <li>The data points of failed channels that did not merge fill a free scan of the closest
 *   passing channel within the tolerance or a hole between intense data points within the hole
 *   fill tolerance, the rules of the second pass. Without this, a failed channel would take its
 *   data points with it, e.g., the apex data points of an intense ion that a short trace of a
 *   co-eluting side signal took.</li>
 *   <li>Holes between intense data points are filled with the data point of a neighboring channel
 *   in the same scan that lies between both centers, one centroid of two ions that the instrument
 *   did not resolve. The data point stays in the neighboring channel as well.</li>
 * </ol>
 * Merged and filled data points never replace a data point, one data point per channel and scan.
 * Only a bridged segment replaces the weak data points of a dip. Only a coalesced fill puts one
 * data point into two chromatograms.
 */
final class ChannelFinalization {

  private static final int NONE = -1;
  // decision: the regular data points of a channel scatter within a fraction of the tolerance, a
  // centroid of two ions is pulled toward the weaker one by its share of the distance
  private static final double COALESCED_MIN_SHIFT = 0.5d;

  private final @NotNull MZTolerance tolerance;
  private final int minConsecutiveScans;
  private final double minGroupIntensity;
  private final double minHeight;
  private final double complementaryFactor;
  // the gap allowance of the consolidation: touching channels and the flanks of holes
  private final int maxScanDistance;
  private final double holeFillFactor;
  // min intensity of both flanks of a hole that recovered data points fill
  private final double minHoleFillIntensity;
  private final double dipBridgeFactor;
  private final double dipBridgeIntensityFraction;
  private final int coalescedMaxHoleScans;
  private final double intensityFactor;
  private final double logIntensityFactor;

  private int numMergedChannels = 0;
  private int numBridgedSegments = 0;
  private long numBridgedDataPoints = 0;
  private long numRecoveredDataPoints = 0;
  private long numCoalescedFills = 0;

  /**
   * @param tolerance max m/z distance of a data point to the center of a channel it fills
   * @param options   complementary, hole fill and dip bridge tolerance, gaps and intensity jumps,
   *                  see {@link FastChromatogramBuilderOptions}
   */
  ChannelFinalization(@NotNull MZTolerance tolerance, int minConsecutiveScans,
      double minGroupIntensity, double minHeight, @NotNull FastChromatogramBuilderOptions options) {
    this.tolerance = tolerance;
    this.minConsecutiveScans = minConsecutiveScans;
    this.minGroupIntensity = minGroupIntensity;
    this.minHeight = minHeight;
    complementaryFactor = options.complementaryToleranceFactor();
    maxScanDistance = options.maxGapScans() + 1;
    holeFillFactor = options.holeFillToleranceFactor();
    minHoleFillIntensity = options.holeFillFlank().minIntensity(minGroupIntensity, minHeight);
    dipBridgeFactor = options.dipBridgeToleranceFactor();
    dipBridgeIntensityFraction = options.dipBridgeIntensityFraction();
    coalescedMaxHoleScans = options.coalescedMaxHoleScans();
    intensityFactor = options.intensityJumpFactor();
    logIntensityFactor = Math.log(intensityFactor);
  }

  /**
   * @param buffers               the data points of each channel, null without data points. Merges
   *                              and fills change the buffers in place.
   * @param centers               the center of each channel, ascending
   * @param maxDataPointIntensity the most intense data point of all scans, the detector limit of
   *                              saturated data, see the dip bridge
   * @return the chromatograms of the channels that pass the filters, sorted by center
   */
  @NotNull List<BuiltChromatogram> process(@Nullable ChannelBuffer @NotNull [] buffers,
      @NotNull double[] centers, double maxDataPointIntensity) {
    final int n = centers.length;
    final boolean[] passes = new boolean[n];
    final double[] maxIntensities = new double[n];
    final int[] byMax = new int[n];
    int numWithData = 0;
    for (int c = 0; c < n; c++) {
      final ChannelBuffer buffer = buffers[c];
      if (buffer == null) {
        continue;
      }
      passes[c] = FastChromatogramBuilder.passesFilters(buffer.scanIndices(), buffer.intensities(),
          buffer.size(), minConsecutiveScans, minGroupIntensity, minHeight);
      maxIntensities[c] = buffer.maxIntensity();
      byMax[numWithData++] = c;
    }
    // most intense first, ties by channel index
    IntArrays.quickSort(byMax, 0, numWithData, (a, b) -> {
      final int result = Double.compare(maxIntensities[b], maxIntensities[a]);
      return result != 0 ? result : Integer.compare(a, b);
    });

    // passing channels that were not merged, other channels may merge into them
    final boolean[] targets = new boolean[n];
    final boolean[] merged = new boolean[n];
    if (complementaryFactor > 1d) {
      // decision: a passing channel merges into a more intense one, the intense channel is the
      // apex of the ion and keeps its center
      for (int k = 0; k < numWithData; k++) {
        final int c = byMax[k];
        if (!passes[c]) {
          continue;
        }
        if (!mergeIntoComplementary(c, buffers, centers, maxIntensities, targets, merged)) {
          targets[c] = true;
        }
      }
      for (int k = 0; k < numWithData; k++) {
        final int c = byMax[k];
        if (!passes[c]) {
          mergeIntoComplementary(c, buffers, centers, maxIntensities, targets, merged);
        }
      }
    } else {
      System.arraycopy(passes, 0, targets, 0, n);
    }
    final DataPoints replaced = new DataPoints();
    if (dipBridgeFactor > 0d) {
      bridgeDips(buffers, centers, maxIntensities, passes, merged, targets,
          Math.max(minHeight, dipBridgeIntensityFraction * maxDataPointIntensity), replaced);
    }
    recoverDataPoints(buffers, centers, passes, merged, targets, replaced);
    if (coalescedMaxHoleScans > 0 && holeFillFactor > 0d) {
      fillCoalescedHoles(buffers, centers, targets);
    }

    final List<BuiltChromatogram> chromatograms = new ArrayList<>();
    for (int c = 0; c < n; c++) {
      if (targets[c]) {
        chromatograms.add(buffers[c].toChromatogram(centers[c]));
      }
    }
    return chromatograms;
  }

  /**
   * Merges the channel into the closest target that is complementary.
   *
   * @param maxIntensities max intensity of each channel, updated for the target
   * @return true if merged
   */
  private boolean mergeIntoComplementary(int channel, @Nullable ChannelBuffer @NotNull [] buffers,
      @NotNull double[] centers, @NotNull double[] maxIntensities, @NotNull boolean[] targets,
      @NotNull boolean[] merged) {
    final ChannelBuffer source = buffers[channel];
    final double center = centers[channel];
    final double window = complementaryFactor * tolerance.getMzToleranceForMass(center);
    int apex = 0;
    for (int i = 1; i < source.size(); i++) {
      if (source.intensities()[i] > source.intensities()[apex]) {
        apex = i;
      }
    }
    final int apexScan = source.scanIndices()[apex];
    final double minTouchIntensity = Math.max(minGroupIntensity,
        source.intensities()[apex] / intensityFactor);
    int best = NONE;
    double bestDistance = Double.POSITIVE_INFINITY;
    for (int c = channel - 1; c >= 0 && center - centers[c] <= window; c--) {
      if (targets[c] && center - centers[c] < bestDistance && isComplementary(source, buffers[c],
          apexScan, minTouchIntensity)) {
        best = c;
        bestDistance = center - centers[c];
      }
    }
    for (int c = channel + 1; c < centers.length && centers[c] - center <= window; c++) {
      if (targets[c] && centers[c] - center < bestDistance && isComplementary(source, buffers[c],
          apexScan, minTouchIntensity)) {
        best = c;
        bestDistance = centers[c] - center;
      }
    }
    if (best == NONE) {
      return false;
    }
    buffers[best].addAll(source);
    maxIntensities[best] = Math.max(maxIntensities[best], maxIntensities[channel]);
    merged[channel] = true;
    numMergedChannels++;
    return true;
  }

  /**
   * @param apexScan          scan of the most intense data point of the source
   * @param minTouchIntensity the target needs a data point of at least this intensity within the
   *                          gap allowance of the apex of the source
   * @return true if both channels never share a scan and the target continues the intensity at the
   * apex of the source. Checking only at the apex keeps a long channel from merging because it
   * touches the peak of the target somewhere.
   */
  private boolean isComplementary(@NotNull ChannelBuffer source, @NotNull ChannelBuffer target,
      int apexScan, double minTouchIntensity) {
    final int[] targetScans = target.scanIndices();
    final double[] targetIntensities = target.intensities();
    final int from = target.indexOfScan(apexScan - maxScanDistance);
    boolean touch = false;
    for (int k = from >= 0 ? from : -from - 1;
        k < target.size() && targetScans[k] <= apexScan + maxScanDistance; k++) {
      if (targetScans[k] == apexScan) {
        return false;
      }
      touch |= targetIntensities[k] >= minTouchIntensity;
    }
    return touch && !shareScan(source, target);
  }

  private static boolean shareScan(@NotNull ChannelBuffer a, @NotNull ChannelBuffer b) {
    final int[] scansA = a.scanIndices();
    final int[] scansB = b.scanIndices();
    int i = 0;
    int j = 0;
    while (i < a.size() && j < b.size()) {
      if (scansA[i] == scansB[j]) {
        return true;
      }
      if (scansA[i] < scansB[j]) {
        i++;
      } else {
        j++;
      }
    }
    return false;
  }

  /**
   * Segments of other channels that fill a dip of a passing channel move into it, the most intense
   * segments first. A passing channel that loses a segment is checked against the filters again, if
   * it fails its remaining data points are recovered like those of other failed channels.
   *
   * @param maxIntensities max intensity of each channel after the complementary merges
   * @param minSegmentMax  only segments reaching this intensity are bridged, the high data points
   * @param replaced       receives the data points of the dips that the segments replace
   */
  private void bridgeDips(@Nullable ChannelBuffer @NotNull [] buffers, @NotNull double[] centers,
      @NotNull double[] maxIntensities, @NotNull boolean[] passes, @NotNull boolean[] merged,
      @NotNull boolean[] targets, double minSegmentMax, @NotNull DataPoints replaced) {
    final IntArrayList segmentChannels = new IntArrayList();
    final IntArrayList segmentFirstScans = new IntArrayList();
    final DoubleArrayList segmentMaxima = new DoubleArrayList();
    for (int c = 0; c < centers.length; c++) {
      final ChannelBuffer buffer = buffers[c];
      // only the channels with high data points have segments that are bridged
      if (buffer == null || merged[c] || maxIntensities[c] < minSegmentMax) {
        continue;
      }
      final int[] scans = buffer.scanIndices();
      final double[] intensities = buffer.intensities();
      for (int from = 0; from < buffer.size(); ) {
        double max = intensities[from];
        int to = from + 1;
        while (to < buffer.size() && scans[to] - scans[to - 1] <= maxScanDistance) {
          max = Math.max(max, intensities[to]);
          to++;
        }
        // decision: only high data points shift their m/z beyond the tolerance, close to the
        // detector limit. Lower segments of other channels are other ions: on Orbitrap data,
        // bridging all segments above the min height moved ~400 segments per file between
        // channels 1-3 tolerances apart and fewer ADAP features were found (-0.2%).
        if (max >= minSegmentMax) {
          segmentChannels.add(c);
          segmentFirstScans.add(scans[from]);
          segmentMaxima.add(max);
        }
        from = to;
      }
    }
    final int numSegments = segmentChannels.size();
    if (numSegments == 0) {
      return;
    }
    // most intense first, ties by channel and scan
    final int[] order = ChannelConsolidation.identity(numSegments);
    IntArrays.quickSort(order, 0, numSegments, (a, b) -> {
      int result = Double.compare(segmentMaxima.getDouble(b), segmentMaxima.getDouble(a));
      if (result == 0) {
        result = Integer.compare(segmentChannels.getInt(a), segmentChannels.getInt(b));
      }
      return result != 0 ? result
          : Integer.compare(segmentFirstScans.getInt(a), segmentFirstScans.getInt(b));
    });

    final boolean[] lost = new boolean[centers.length];
    for (final int s : order) {
      final int channel = segmentChannels.getInt(s);
      final ChannelBuffer source = buffers[channel];
      // earlier bridges may have moved data points into the source and extended the segment
      final int from = source.indexOfScan(segmentFirstScans.getInt(s));
      if (from < 0 || (from > 0
          && source.scanIndices()[from] - source.scanIndices()[from - 1] <= maxScanDistance)) {
        // the start of the segment moved or is inside a longer segment now, handled by its start
        continue;
      }
      final int[] scans = source.scanIndices();
      double minMz = Double.POSITIVE_INFINITY;
      double maxMz = Double.NEGATIVE_INFINITY;
      double segmentMin = Double.POSITIVE_INFINITY;
      double segmentMax = 0d;
      double sumMzIntensity = 0d;
      double sumIntensity = 0d;
      int to = from;
      do {
        final double mz = source.mzs()[to];
        final double intensity = source.intensities()[to];
        minMz = Math.min(minMz, mz);
        maxMz = Math.max(maxMz, mz);
        segmentMin = Math.min(segmentMin, intensity);
        segmentMax = Math.max(segmentMax, intensity);
        sumMzIntensity += mz * intensity;
        sumIntensity += intensity;
        to++;
      } while (to < source.size() && scans[to] - scans[to - 1] <= maxScanDistance);
      final double segmentMz = sumMzIntensity / sumIntensity;
      final int target = findDipToBridge(source, from, to, minMz, maxMz, segmentMz, segmentMin,
          segmentMax, channel, buffers, centers, targets);
      if (target == NONE) {
        continue;
      }
      buffers[target].addRangeReplacing(source, from, to, replaced::add);
      source.removeRange(from, to);
      lost[channel] = true;
      numBridgedSegments++;
      numBridgedDataPoints += to - from;
    }

    for (int c = 0; c < centers.length; c++) {
      if (lost[c] && passes[c] && !FastChromatogramBuilder.passesFilters(buffers[c].scanIndices(),
          buffers[c].intensities(), buffers[c].size(), minConsecutiveScans, minGroupIntensity,
          minHeight)) {
        // the rest of the channel is recovered with the failed channels
        passes[c] = false;
        targets[c] = false;
      }
    }
  }

  /**
   * @param from       first data point of the segment in the source
   * @param to         exclusive end of the segment
   * @param segmentMz  intensity weighted m/z of the segment
   * @param segmentMin min intensity of the segment
   * @param segmentMax max intensity of the segment
   * @return the target with a dip that the segment fills, the closest center within the dip bridge
   * tolerance, or NONE
   */
  private int findDipToBridge(@NotNull ChannelBuffer source, int from, int to, double minMz,
      double maxMz, double segmentMz, double segmentMin, double segmentMax, int sourceChannel,
      @Nullable ChannelBuffer @NotNull [] buffers, @NotNull double[] centers,
      @NotNull boolean[] targets) {
    int best = NONE;
    double bestDistance = Double.POSITIVE_INFINITY;
    // all data points of the segment are within the window of the target center
    for (int c = ChannelConsolidation.lowerBound(centers,
        maxMz - dipBridgeFactor * tolerance.getMzToleranceForMass(maxMz)); c < centers.length;
        c++) {
      final double window = dipBridgeFactor * tolerance.getMzToleranceForMass(centers[c]);
      if (centers[c] - minMz > window) {
        // the tolerance grows slower than the distance, all further centers are farther away
        break;
      }
      final double distance = Math.abs(centers[c] - segmentMz);
      if (c == sourceChannel || !targets[c] || maxMz - centers[c] > window
          || distance >= bestDistance || !fillsDip(buffers[c], source, from, to, segmentMin,
          segmentMax)) {
        continue;
      }
      best = c;
      bestDistance = distance;
    }
    return best;
  }

  /**
   * @return true if the data points [from, to) of the source fill a dip of the target: the most
   * intense target data point within the gap allowance before and after the segment reaches the min
   * height and is within the intensity factor of the most intense segment data point at this edge,
   * the segment stays within the intensity factor of both (no valley, no larger peak), the target
   * data points in the scans of the segment are below the segment by more than the intensity
   * factor, and the target has at least half as many data points as the segment within the length
   * of the segment before and after it. The valley and the last condition keep the long segment of
   * an ion from moving into the short channel of its shifted apex data points, the valley also a
   * separate peak with weak edges in a gap of a background ion.
   */
  private boolean fillsDip(@NotNull ChannelBuffer target, @NotNull ChannelBuffer source, int from,
      int to, double segmentMin, double segmentMax) {
    final int[] sourceScans = source.scanIndices();
    final double[] sourceIntensities = source.intensities();
    final int first = sourceScans[from];
    final int last = sourceScans[to - 1];
    final int[] scans = target.scanIndices();
    final double[] intensities = target.intensities();
    final int size = target.size();
    final int found = target.indexOfScan(first);
    final int start = found >= 0 ? found : -found - 1;

    double before = 0d;
    for (int i = start - 1; i >= 0 && scans[i] >= first - maxScanDistance; i--) {
      before = Math.max(before, intensities[i]);
    }
    int i = start;
    int j = from;
    for (; i < size && scans[i] <= last; i++) {
      while (sourceScans[j] < scans[i]) {
        j++;
      }
      if (sourceScans[j] == scans[i] && intensities[i] * intensityFactor >= sourceIntensities[j]) {
        // a signal of the target in the same scan, no dip
        return false;
      }
    }
    final int end = i;
    double after = 0d;
    for (; i < size && scans[i] <= last + maxScanDistance; i++) {
      after = Math.max(after, intensities[i]);
    }
    if (before < minHeight || after < minHeight || segmentMax > intensityFactor * Math.max(before,
        after) || segmentMin * intensityFactor < Math.min(before, after)) {
      return false;
    }
    // continuity: the most intense segment data points at both edges
    double firstEdge = 0d;
    for (int k = from; k < to && sourceScans[k] < first + maxScanDistance; k++) {
      firstEdge = Math.max(firstEdge, sourceIntensities[k]);
    }
    double lastEdge = 0d;
    for (int k = to - 1; k >= from && sourceScans[k] > last - maxScanDistance; k--) {
      lastEdge = Math.max(lastEdge, sourceIntensities[k]);
    }
    if (Math.abs(Math.log(before / firstEdge)) > logIntensityFactor
        || Math.abs(Math.log(after / lastEdge)) > logIntensityFactor) {
      return false;
    }
    final int span = last - first + 1;
    final int aroundFrom = target.indexOfScan(first - span);
    final int aroundTo = target.indexOfScan(last + span + 1);
    final int around = (aroundTo >= 0 ? aroundTo : -aroundTo - 1) - (aroundFrom >= 0 ? aroundFrom
        : -aroundFrom - 1) - (end - start);
    return 2 * around >= to - from;
  }

  /**
   * The data points of failed channels that did not merge fill the passing channels, the most
   * intense first.
   *
   * @param replaced data points replaced by bridged segments, recovered the same way
   */
  private void recoverDataPoints(@Nullable ChannelBuffer @NotNull [] buffers,
      @NotNull double[] centers, @NotNull boolean[] passes, @NotNull boolean[] merged,
      @NotNull boolean[] targets, @NotNull DataPoints replaced) {
    final Targets reachable = new Targets(centers, targets);
    // decision: failed channels far from all passing channels are skipped, e.g., the many noise
    // channels of data without noise filter. Their data points would not fill any channel.
    final boolean[] recover = new boolean[centers.length];
    int count = replaced.size();
    for (int c = 0; c < centers.length; c++) {
      final ChannelBuffer buffer = buffers[c];
      if (buffer != null && !passes[c] && !merged[c] && buffer.size() > 0 && reachable.anyWithin(
          buffer.minMz(), buffer.maxMz(), recoveryWindow(buffer.maxMz()))) {
        recover[c] = true;
        count += buffer.size();
      }
    }
    if (count == 0) {
      return;
    }
    final int[] scans = new int[count];
    final double[] mzs = new double[count];
    final double[] intensities = new double[count];
    int k = 0;
    for (int c = 0; c < centers.length; c++) {
      if (!recover[c]) {
        continue;
      }
      final ChannelBuffer buffer = buffers[c];
      System.arraycopy(buffer.scanIndices(), 0, scans, k, buffer.size());
      System.arraycopy(buffer.mzs(), 0, mzs, k, buffer.size());
      System.arraycopy(buffer.intensities(), 0, intensities, k, buffer.size());
      k += buffer.size();
    }
    for (int p = 0; p < replaced.size(); p++, k++) {
      scans[k] = replaced.scans.getInt(p);
      mzs[k] = replaced.mzs.getDouble(p);
      intensities[k] = replaced.intensities.getDouble(p);
    }
    // most intense first, ties by scan and then by channel. Radix sort of the bits: positive
    // doubles have the order of their bits.
    final long[] intensityKeys = new long[count];
    final long[] scanKeys = new long[count];
    for (int p = 0; p < count; p++) {
      intensityKeys[p] = Long.MAX_VALUE - Double.doubleToLongBits(intensities[p]);
      scanKeys[p] = (long) scans[p] << 32 | p;
    }
    LongArrays.radixSort(intensityKeys, scanKeys);

    for (int i = 0; i < count; i++) {
      final int p = (int) scanKeys[i];
      int channel = findFreeChannel(scans[p], mzs[p], buffers, reachable);
      if (channel == NONE && holeFillFactor > 0d) {
        channel = findHole(scans[p], mzs[p], intensities[p], buffers, reachable);
      }
      if (channel != NONE) {
        buffers[channel].insert(scans[p], mzs[p], intensities[p]);
        numRecoveredDataPoints++;
      }
    }
  }

  /**
   * @return the window around a data point in which a recovered data point finds a channel, the
   * tolerance for free scans and the flank window of the holes
   */
  private double recoveryWindow(double mz) {
    return (holeFillFactor > 0d ? holeFillFactor + 2d : 1d) * tolerance.getMzToleranceForMass(mz);
  }

  /**
   * Same rule as for loose data points in the second pass.
   *
   * @return the closest target within the tolerance without data point in the scan or NONE
   */
  private int findFreeChannel(int scan, double mz, @Nullable ChannelBuffer @NotNull [] buffers,
      @NotNull Targets reachable) {
    final double[] centers = reachable.centers;
    int right = reachable.lookup.lowerBound(mz);
    int left = right - 1;
    while (left >= 0 || right < centers.length) {
      final double leftDistance = left >= 0 ? mz - centers[left] : Double.POSITIVE_INFINITY;
      final double rightDistance =
          right < centers.length ? centers[right] - mz : Double.POSITIVE_INFINITY;
      final int target;
      final double distance;
      if (leftDistance <= rightDistance) {
        target = left--;
        distance = leftDistance;
      } else {
        target = right++;
        distance = rightDistance;
      }
      if (distance > tolerance.getMzToleranceForMass(centers[target])) {
        // all remaining channels are farther away
        return NONE;
      }
      final int channel = reachable.channels[target];
      if (buffers[channel].indexOfScan(scan) < 0) {
        return channel;
      }
    }
    return NONE;
  }

  /**
   * The passing channels that other data points can fill, sorted by center.
   */
  private static final class Targets {

    // decision: 5 mDa bins hold few channels, like the lookup of the second pass
    private static final double LOOKUP_BIN_WIDTH = 0.005;

    private final double[] centers;
    private final int[] channels;
    private final BinnedLowerBound lookup;

    private Targets(@NotNull double[] allCenters, @NotNull boolean[] targets) {
      int n = 0;
      for (final boolean target : targets) {
        if (target) {
          n++;
        }
      }
      centers = new double[n];
      channels = new int[n];
      int k = 0;
      for (int c = 0; c < allCenters.length; c++) {
        if (targets[c]) {
          centers[k] = allCenters[c];
          channels[k++] = c;
        }
      }
      lookup = new BinnedLowerBound(centers, LOOKUP_BIN_WIDTH);
    }

    /**
     * @return true if a target center is within [min - window, max + window]
     */
    boolean anyWithin(double min, double max, double window) {
      final int i = lookup.lowerBound(min - window);
      return i < centers.length && centers[i] <= max + window;
    }
  }

  /**
   * Same rule as the hole fill of the second pass: a hole of up to the max gap scans between two
   * data points of at least the min hole fill intensity, the data point within the hole fill tolerance around
   * the interpolated m/z and within the intensity jump factor of the interpolated intensity.
   *
   * @return the target with the closest interpolated m/z or NONE
   */
  private int findHole(int scan, double mz, double intensity,
      @Nullable ChannelBuffer @NotNull [] buffers, @NotNull Targets reachable) {
    // the flanks scatter around the center, the data point around the flanks
    final double window = (holeFillFactor + 2d) * tolerance.getMzToleranceForMass(mz);
    final double logIntensity = Math.log(intensity);
    final double[] centers = reachable.centers;
    int best = NONE;
    double bestDistance = Double.POSITIVE_INFINITY;
    for (int t = reachable.lookup.lowerBound(mz - window);
        t < centers.length && centers[t] <= mz + window; t++) {
      final int c = reachable.channels[t];
      final ChannelBuffer buffer = buffers[c];
      final int found = buffer.indexOfScan(scan);
      final int next = -found - 1;
      if (found >= 0 || next == 0 || next == buffer.size()) {
        continue;
      }
      final int previous = next - 1;
      final int scanBefore = buffer.scanIndices()[previous];
      final int scanAfter = buffer.scanIndices()[next];
      final double intensityBefore = buffer.intensities()[previous];
      final double intensityAfter = buffer.intensities()[next];
      if (scanAfter - scanBefore > maxScanDistance || intensityBefore < minHoleFillIntensity
          || intensityAfter < minHoleFillIntensity) {
        continue;
      }
      final double position = (double) (scan - scanBefore) / (scanAfter - scanBefore);
      final double mzBefore = buffer.mzs()[previous];
      final double expectedMz = mzBefore + position * (buffer.mzs()[next] - mzBefore);
      final double distance = Math.abs(mz - expectedMz);
      final double logBefore = Math.log(intensityBefore);
      final double expectedLogIntensity =
          logBefore + position * (Math.log(intensityAfter) - logBefore);
      if (distance <= holeFillFactor * tolerance.getMzToleranceForMass(expectedMz)
          && Math.abs(logIntensity - expectedLogIntensity) <= logIntensityFactor
          && distance < bestDistance) {
        best = c;
        bestDistance = distance;
      }
    }
    return best;
  }

  /**
   * Holes between two data points of at least the min height of a passing channel are filled with
   * the data point of a neighboring passing channel in the same scan, if it lies between the m/z
   * interpolated in the hole and the median m/z of its channel, is shifted from this median toward
   * the hole by at least the min shift and fits the hole like a hole fill. The data point stays in
   * its channel. Two ions that the instrument does not resolve yield one centroid between both m/z
   * when both are intense, e.g., m/z 262.120 and 262.133 on GC-EI-QTOF data give one centroid at
   * +23 to +32 ppm in 8 apex scans, which the closer channel takes. Holes up to the max gap scans
   * are filled scan by scan, longer holes only if every scan is filled. The fills are found first
   * and applied after all channels, so a shared data point is not shared again. Decision: the
   * median, not the intensity weighted center, the coalesced apex centroids pull the center of
   * their channel toward the hole, e.g., 262.1327 instead of 262.1333.
   */
  private void fillCoalescedHoles(@Nullable ChannelBuffer @NotNull [] buffers,
      @NotNull double[] centers, @NotNull boolean[] targets) {
    final Targets reachable = new Targets(centers, targets);
    final double[] medianMzs = new double[centers.length];
    Arrays.fill(medianMzs, Double.NaN);
    final IntArrayList fillChannels = new IntArrayList();
    final DataPoints fills = new DataPoints();
    for (int c = 0; c < centers.length; c++) {
      if (!targets[c]) {
        continue;
      }
      final ChannelBuffer buffer = buffers[c];
      final int[] scans = buffer.scanIndices();
      final double[] mzs = buffer.mzs();
      final double[] intensities = buffer.intensities();
      for (int i = 0; i + 1 < buffer.size(); i++) {
        final int holeScans = scans[i + 1] - scans[i] - 1;
        if (holeScans < 1 || holeScans > coalescedMaxHoleScans || intensities[i] < minHeight
            || intensities[i + 1] < minHeight) {
          continue;
        }
        final int holeStart = fills.size();
        final double logBefore = Math.log(intensities[i]);
        final double logAfter = Math.log(intensities[i + 1]);
        for (int scan = scans[i] + 1; scan < scans[i + 1]; scan++) {
          final double position = (double) (scan - scans[i]) / (holeScans + 1);
          final double expectedMz = mzs[i] + position * (mzs[i + 1] - mzs[i]);
          final double expectedLogIntensity = logBefore + position * (logAfter - logBefore);
          final int donor = findCoalescedDonor(c, scan, expectedMz, expectedLogIntensity, buffers,
              medianMzs, reachable);
          if (donor != NONE) {
            final ChannelBuffer source = buffers[donor];
            final int k = source.indexOfScan(scan);
            fills.add(scan, source.mzs()[k], source.intensities()[k]);
            fillChannels.add(c);
          }
        }
        // decision: a long hole is one coalescence over the apex of both ions, a partly filled
        // long hole is a hole of the ion
        if (holeScans >= maxScanDistance && fills.size() - holeStart < holeScans) {
          fills.truncate(holeStart);
          fillChannels.size(holeStart);
        }
      }
    }
    for (int k = 0; k < fills.size(); k++) {
      buffers[fillChannels.getInt(k)].insert(fills.scans.getInt(k), fills.mzs.getDouble(k),
          fills.intensities.getDouble(k));
    }
    numCoalescedFills += fills.size();
  }

  /**
   * @param channel              the channel with the hole
   * @param expectedMz           m/z interpolated between the flanks of the hole
   * @param expectedLogIntensity log intensity interpolated between the flanks of the hole
   * @return the passing channel whose data point in the scan fills the hole, the data point closest
   * to the expected m/z, or NONE
   */
  private int findCoalescedDonor(int channel, int scan, double expectedMz,
      double expectedLogIntensity, @Nullable ChannelBuffer @NotNull [] buffers,
      @NotNull double[] medianMzs, @NotNull Targets reachable) {
    final double fillWindow = holeFillFactor * tolerance.getMzToleranceForMass(expectedMz);
    // the data point is within the fill window of the hole and of the donor median
    final double window = 2d * fillWindow;
    int best = NONE;
    double bestDistance = Double.POSITIVE_INFINITY;
    for (int t = reachable.lookup.lowerBound(expectedMz - window);
        t < reachable.centers.length && reachable.centers[t] <= expectedMz + window; t++) {
      final int donor = reachable.channels[t];
      final ChannelBuffer source = buffers[donor];
      final int k = donor == channel ? -1 : source.indexOfScan(scan);
      if (k < 0) {
        continue;
      }
      final double mz = source.mzs()[k];
      final double distance = Math.abs(mz - expectedMz);
      if (distance > fillWindow || distance >= bestDistance
          || Math.abs(Math.log(source.intensities()[k]) - expectedLogIntensity)
          > logIntensityFactor) {
        continue;
      }
      // the median needs a sort, only for the few candidates
      final double donorMedian = medianMz(donor, buffers, medianMzs);
      final double shift = Math.abs(mz - donorMedian);
      // between the hole and the donor median, shifted toward the hole
      if ((mz - expectedMz) * (donorMedian - mz) <= 0d || shift > fillWindow
          || shift < COALESCED_MIN_SHIFT * tolerance.getMzToleranceForMass(donorMedian)) {
        continue;
      }
      best = donor;
      bestDistance = distance;
    }
    return best;
  }

  /**
   * @param medianMzs computed medians, NaN if not computed yet
   * @return the unweighted median m/z of the data points of the channel
   */
  private static double medianMz(int channel, @Nullable ChannelBuffer @NotNull [] buffers,
      @NotNull double[] medianMzs) {
    if (Double.isNaN(medianMzs[channel])) {
      final ChannelBuffer buffer = buffers[channel];
      final double[] mzs = Arrays.copyOf(buffer.mzs(), buffer.size());
      Arrays.sort(mzs);
      final int half = mzs.length / 2;
      medianMzs[channel] = mzs.length % 2 == 1 ? mzs[half] : 0.5d * (mzs[half - 1] + mzs[half]);
    }
    return medianMzs[channel];
  }

  /**
   * @return data points of neighboring channels that filled a hole, see {@link #fillCoalescedHoles}
   */
  long getNumCoalescedFills() {
    return numCoalescedFills;
  }

  /**
   * @return channels merged into another channel, complementary or failed
   */
  int getNumMergedChannels() {
    return numMergedChannels;
  }

  /**
   * @return segments of other channels that filled a dip of a channel
   */
  int getNumBridgedSegments() {
    return numBridgedSegments;
  }

  /**
   * @return data points of the bridged segments
   */
  long getNumBridgedDataPoints() {
    return numBridgedDataPoints;
  }

  /**
   * Growable list of data points in any order.
   */
  private static final class DataPoints {

    private final IntArrayList scans = new IntArrayList();
    private final DoubleArrayList mzs = new DoubleArrayList();
    private final DoubleArrayList intensities = new DoubleArrayList();

    void add(int scan, double mz, double intensity) {
      scans.add(scan);
      mzs.add(mz);
      intensities.add(intensity);
    }

    int size() {
      return scans.size();
    }

    void truncate(int size) {
      scans.size(size);
      mzs.size(size);
      intensities.size(size);
    }
  }

  /**
   * @return data points of failed channels that filled a passing channel
   */
  long getNumRecoveredDataPoints() {
    return numRecoveredDataPoints;
  }
}
