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
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Detects chromatograms, i.e., one extracted ion chromatogram per m/z channel across all scans, in
 * three steps without sorting or storing all data points:
 * <ol>
 *   <li>First pass over the scans: {@link MassTraceSweeper} connects data points of consecutive
 *   scans to mass traces, locally in retention time. Only trace statistics are kept.</li>
 *   <li>{@link ChannelConsolidation} groups the traces into m/z channels. The traces of one ion
 *   form one channel, which avoids duplicate chromatograms.</li>
 *   <li>Second pass: each data point is routed into the channel of its trace by
 *   {@link ChannelDataCollector}, the first pass records the trace of each data point in
 *   {@link TraceAssignments}. Remaining data points fill empty scans of the closest channel within
 *   the tolerance, which avoids holes.</li>
 *   <li>{@link ChannelFinalization} merges complementary channels of one ion, bridges dips of
 *   intense chromatograms with shifted segments of other channels, e.g., a saturated apex, and
 *   moves the data points of channels that fail the filters into the passing channels. Holes
 *   left by one centroid of two unresolved ions are filled with this centroid.</li>
 * </ol>
 * Channels need a minimum number of consecutive scans above the group intensity and within this
 * segment a minimum height, the same filters as in the ADAP chromatogram builder.
 * <p>
 * Memory is proportional to the active traces and the detected chromatograms plus two bytes per data
 * point for the trace assignments, the data points themselves are read from the scans in both
 * passes.
 */
public final class FastChromatogramBuilder {

  private final @NotNull MZTolerance tolerance;
  private final int minConsecutiveScans;
  private final double minGroupIntensity;
  private final double minHeight;
  private final @NotNull FastChromatogramBuilderOptions options;

  private @Nullable FastChromatogramBuilderStatistics statistics;

  /**
   * @param tolerance           scan to scan m/z tolerance
   * @param minConsecutiveScans min number of consecutive scans with an intensity of at least
   *                            minGroupIntensity
   * @param minGroupIntensity   min intensity of the consecutive scans
   * @param minHeight           min height within the consecutive scans. Only traces reaching this
   *                            height can start a chromatogram.
   */
  public FastChromatogramBuilder(@NotNull MZTolerance tolerance, int minConsecutiveScans,
      double minGroupIntensity, double minHeight) {
    this(tolerance, minConsecutiveScans, minGroupIntensity, minHeight,
        FastChromatogramBuilderOptions.DEFAULT);
  }

  /**
   * Constructor with the internal tuning options.
   */
  FastChromatogramBuilder(@NotNull MZTolerance tolerance, int minConsecutiveScans,
      double minGroupIntensity, double minHeight, @NotNull FastChromatogramBuilderOptions options) {
    this.tolerance = tolerance;
    this.minConsecutiveScans = minConsecutiveScans;
    this.minGroupIntensity = minGroupIntensity;
    this.minHeight = minHeight;
    this.options = options;
  }

  /**
   * @param scans      the scans in retention time order, each sorted by m/z
   * @param isCanceled checked regularly, may be null
   * @param progress   receives the progress from 0 to 1, may be null
   * @return the chromatograms sorted by center m/z or null if canceled
   */
  @Nullable
  public List<BuiltChromatogram> build(@NotNull MzIntensityScans scans,
      @Nullable BooleanSupplier isCanceled, @Nullable DoubleConsumer progress) {
    // decision: single data point traces, and short traces that cannot start a channel, keep their
    // data points loose. Loose data points still fill empty scans in the second pass, so this
    // only reduces the work for noise.
    final int minTraceDataPoints = minConsecutiveScans <= 1 ? 1 : 2;
    final int minTraceDataPointsWithoutHeight = Math.max(minTraceDataPoints, minConsecutiveScans);

    long start = System.nanoTime();
    final TraceSummaryCollector summaries = new TraceSummaryCollector(minTraceDataPoints,
        minTraceDataPointsWithoutHeight, minHeight, options.needsCollisions());
    // decision: two bytes per data point instead of detecting the traces again in the second
    // pass, which took as long as the first pass
    final TraceAssignments assignments = new TraceAssignments();
    final MassTraceSweeper firstPass = new MassTraceSweeper(tolerance, options, summaries,
        assignments);
    if (!sweep(scans, firstPass, isCanceled, progress, 0d, 0.45d)) {
      return null;
    }
    final long firstPassNanos = System.nanoTime() - start;

    start = System.nanoTime();
    final ChannelPlan plan = ChannelConsolidation.consolidate(summaries, tolerance, minHeight,
        options);
    final long consolidationNanos = System.nanoTime() - start;
    if (progress != null) {
      progress.accept(0.5d);
    }

    start = System.nanoTime();
    final ChannelDataCollector channels = new ChannelDataCollector(plan, tolerance,
        options.holeFillFlank().minIntensity(minGroupIntensity, minHeight), options);
    if (!route(scans, assignments, channels, isCanceled, progress, 0.5d, 0.45d)) {
      return null;
    }
    final long secondPassNanos = System.nanoTime() - start;

    start = System.nanoTime();
    final ChannelBuffer[] buffers = new ChannelBuffer[channels.numChannels()];
    for (int c = 0; c < buffers.length; c++) {
      buffers[c] = channels.getBuffer(c);
    }
    final ChannelFinalization finalization = new ChannelFinalization(tolerance, minConsecutiveScans,
        minGroupIntensity, minHeight, options);
    final List<BuiltChromatogram> chromatograms = finalization.process(buffers,
        plan.channelCenters(), firstPass.getMaxDataPointIntensity());
    final long finalizationNanos = System.nanoTime() - start;
    if (progress != null) {
      progress.accept(1d);
    }

    statistics = new FastChromatogramBuilderStatistics(scans.getNumberOfScans(),
        firstPass.getNumDataPoints(), firstPass.getMaxDataPointIntensity(),
        firstPass.getNumTracesCreated(),
        firstPass.getMaxActiveTraces(), plan.numRecordedTraces(), summaries.getNumCollisionEvents(),
        plan.numCollisionPairs(), plan.numChannels(), channels.getNumMemberDataPoints(),
        channels.getNumMemberConflicts(), channels.getNumLooseDataPoints(),
        channels.getNumLooseAssigned(), channels.getNumHoleFills(),
        finalization.getNumMergedChannels(), finalization.getNumBridgedSegments(),
        finalization.getNumBridgedDataPoints(), finalization.getNumRecoveredDataPoints(),
        finalization.getNumCoalescedFills(),
        chromatograms.size(), firstPassNanos, consolidationNanos, secondPassNanos,
        finalizationNanos);
    return chromatograms;
  }

  /**
   * Second pass: loads the scans again and routes their data points with the traces of the first
   * pass.
   *
   * @return false if canceled
   */
  private static boolean route(@NotNull MzIntensityScans scans,
      @NotNull TraceAssignments assignments, @NotNull ChannelDataCollector channels,
      @Nullable BooleanSupplier isCanceled, @Nullable DoubleConsumer progress, double progressStart,
      double progressRange) {
    final int numScans = Math.max(1, scans.getNumberOfScans());
    final ScanDataPoints points = new ScanDataPoints();
    scans.reset();
    int scanIndex = 0;
    while (scans.nextScan()) {
      if (isCanceled != null && isCanceled.getAsBoolean()) {
        return false;
      }
      points.load(scans);
      channels.routeScan(scanIndex, points, assignments.scan(scanIndex));
      assignments.release(scanIndex);
      scanIndex++;
      if (progress != null && (scanIndex & 63) == 0) {
        progress.accept(progressStart + progressRange * scanIndex / numScans);
      }
    }
    return true;
  }

  private static boolean sweep(@NotNull MzIntensityScans scans, @NotNull MassTraceSweeper sweeper,
      @Nullable BooleanSupplier isCanceled, @Nullable DoubleConsumer progress, double progressStart,
      double progressRange) {
    final int numScans = Math.max(1, scans.getNumberOfScans());
    scans.reset();
    int processed = 0;
    while (scans.nextScan()) {
      if (isCanceled != null && isCanceled.getAsBoolean()) {
        return false;
      }
      sweeper.processScan(scans);
      processed++;
      if (progress != null && (processed & 63) == 0) {
        progress.accept(progressStart + progressRange * processed / numScans);
      }
    }
    sweeper.finish();
    return true;
  }

  /**
   * The filters of the chromatograms, also used to compare chromatogram lists.
   *
   * @param scanIndices ascending, consecutive values are consecutive scans
   * @return true for at least minConsecutiveScans consecutive scans with an intensity of at least
   * minGroupIntensity and within this segment a data point of at least minHeight
   */
  public static boolean passesFilters(@NotNull int[] scanIndices, @NotNull double[] intensities,
      int size,
      int minConsecutiveScans, double minGroupIntensity, double minHeight) {
    if (minConsecutiveScans <= 1) {
      for (int i = 0; i < size; i++) {
        if (intensities[i] >= minHeight) {
          return true;
        }
      }
      return false;
    }
    int consecutive = 0;
    int lastScan = Integer.MIN_VALUE;
    double segmentMax = 0d;
    for (int i = 0; i < size; i++) {
      final double intensity = intensities[i];
      if (intensity < minGroupIntensity) {
        consecutive = 0;
        continue;
      }
      if (consecutive > 0 && scanIndices[i] == lastScan + 1) {
        consecutive++;
        segmentMax = Math.max(segmentMax, intensity);
      } else {
        // decision: the height is checked within the consecutive segment, the ADAP builder
        // documents this but never resets its maximum between segments
        consecutive = 1;
        segmentMax = intensity;
      }
      lastScan = scanIndices[i];
      if (consecutive >= minConsecutiveScans && segmentMax >= minHeight) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return the statistics of the last {@link #build} call or null
   */
  @Nullable
  public FastChromatogramBuilderStatistics getStatistics() {
    return statistics;
  }
}
