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

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.MathUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Complementary joins of the channel consolidation, traces 1 to 2 times the tolerance away from the
 * seed of their channel that never collided with it. A join whose trace has its apex away from the
 * seed trace may be a separate peak in a neighboring chromatogram (defect 3 of the design note)
 * instead of the scatter of one ion. The first pass and the consolidation of the
 * {@link FastChromatogramBuilder} are replayed with a listener that records the apex scan of each
 * trace, the builder code stays unchanged.
 */
final class ComplementaryJoinMetrics {

  private ComplementaryJoinMetrics() {
  }

  /**
   * A trace that joined a channel through the complementary tolerance.
   *
   * @param mz           intensity weighted center of the trace
   * @param maxIntensity apex intensity of the trace
   * @param apexScan     scan index of the apex
   * @param apexMz       m/z of the apex data point
   * @param seedMz       center of the seed trace of the channel
   * @param seedFirst    first scan of the seed trace
   * @param seedLast     last scan of the seed trace
   */
  record Join(double mz, double maxIntensity, int apexScan, double apexMz, int firstScan,
              int lastScan, double seedMz, int seedFirst, int seedLast) {

    /**
     * @return true if the apex is more than maxScanDistance scans outside the seed trace
     */
    boolean isApexAway(int maxScanDistance) {
      return apexScan < seedFirst - maxScanDistance || apexScan > seedLast + maxScanDistance;
    }

    double ppmFromSeed() {
      return (mz - seedMz) / seedMz * 1E6;
    }
  }

  /**
   * @param joins          complementary joins
   * @param apexAway       joins with the apex more than the gap allowance outside the seed trace
   * @param aboveMinHeight apex away joins that reach the min height, the peaks that count
   * @param resolvedInBoth peaks resolved as feature in both lists
   * @param onlyAdap       peaks only resolved in the ADAP list, lost by the fast builder
   * @param onlyFast       peaks only resolved in the fast list
   * @param neither        peaks resolved in no list
   * @param examples       the peaks that are not resolved in both lists
   */
  record Result(int joins, int apexAway, int aboveMinHeight, int resolvedInBoth, int onlyAdap,
                int onlyFast, int neither, @NotNull List<String> examples) {

  }

  /**
   * Replays the first pass and the consolidation, same thresholds as
   * {@link FastChromatogramBuilder#build}.
   */
  @NotNull
  static List<Join> findJoins(@NotNull MzIntensityScans scans, @NotNull MZTolerance tolerance,
      int minConsecutiveScans, double minHeight, @NotNull FastChromatogramBuilderOptions options) {
    final int minTraceDataPoints = minConsecutiveScans <= 1 ? 1 : 2;
    final int minTraceDataPointsWithoutHeight = Math.max(minTraceDataPoints, minConsecutiveScans);
    final TraceSummaryCollector summaries = new TraceSummaryCollector(minTraceDataPoints,
        minTraceDataPointsWithoutHeight, minHeight, options.needsCollisions());
    final ApexRecorder recorder = new ApexRecorder(summaries);
    final MassTraceSweeper sweeper = new MassTraceSweeper(tolerance, options, recorder);
    scans.reset();
    while (scans.nextScan()) {
      sweeper.processScan(scans);
    }
    sweeper.finish();
    final ChannelPlan plan = ChannelConsolidation.consolidate(summaries, tolerance, minHeight,
        options);

    final int n = summaries.getNumRecordedTraces();
    final long[] memberIds = plan.memberTraceIds();
    final int[] channelOf = new int[n];
    // the seed of a channel is its first trace in the order of the consolidation, the highest
    // max intensity, ties by trace id
    final int[] seedOf = new int[plan.numChannels()];
    Arrays.fill(seedOf, -1);
    for (int i = 0; i < n; i++) {
      final int member = Arrays.binarySearch(memberIds, summaries.traceIds.getLong(i));
      channelOf[i] = member < 0 ? -1 : plan.memberChannels()[member];
      if (channelOf[i] < 0) {
        continue;
      }
      final int seed = seedOf[channelOf[i]];
      if (seed < 0 || isVisitedBefore(summaries, i, seed)) {
        seedOf[channelOf[i]] = i;
      }
    }

    final List<Join> joins = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      if (channelOf[i] < 0) {
        continue;
      }
      final int seed = seedOf[channelOf[i]];
      final double center = summaries.centers.getDouble(i);
      final double seedCenter = summaries.centers.getDouble(seed);
      // traces within the tolerance of the seed joined directly
      if (Math.abs(center - seedCenter) <= tolerance.getMzToleranceForMass(center)) {
        continue;
      }
      joins.add(
          new Join(center, summaries.maxIntensities.getDouble(i), recorder.apexScans.getInt(i),
              recorder.apexMzs.getDouble(i), summaries.firstScans.getInt(i),
              summaries.lastScans.getInt(i), seedCenter, summaries.firstScans.getInt(seed),
              summaries.lastScans.getInt(seed)));
    }
    return joins;
  }

  private static boolean isVisitedBefore(@NotNull TraceSummaryCollector summaries, int a, int b) {
    final int result = Double.compare(summaries.maxIntensities.getDouble(a),
        summaries.maxIntensities.getDouble(b));
    return result > 0 || (result == 0 && summaries.traceIds.getLong(a) < summaries.traceIds.getLong(
        b));
  }

  /**
   * A peak is resolved in a list if a feature is within the tolerance of the trace center and
   * within the retention time tolerance of the trace apex. The examples name the fast chromatogram
   * with the apex of the trace and its chromatographic threshold of the resolver, which drops the
   * peak if its apex is below.
   *
   * @param scans                    the selected scans, index is the scan index of the builder
   * @param fastChromatogramList     the fast chromatograms before resolving
   * @param fastChromatograms        the same chromatograms in the order of the rows
   * @param chromatographicThreshold quantile of the intensities of a chromatogram that the resolver
   *                                 uses as threshold
   */
  @NotNull
  static Result classify(@NotNull List<Join> joins, @NotNull Scan[] scans,
      @NotNull FeatureList adapResolved, @NotNull FeatureList fastResolved,
      @NotNull FeatureList fastChromatogramList,
      @NotNull List<EvaluatedChromatogram> fastChromatograms, @NotNull MZTolerance tolerance,
      float rtTolerance, double minHeight, int maxScanDistance, double chromatographicThreshold) {
    final ChromatogramDataPointIndex fastIndex = new ChromatogramDataPointIndex(fastChromatograms,
        scans.length);
    int apexAway = 0;
    int aboveMinHeight = 0;
    int both = 0;
    int onlyAdap = 0;
    int onlyFast = 0;
    int neither = 0;
    final List<String> examples = new ArrayList<>();
    for (final Join join : joins) {
      if (!join.isApexAway(maxScanDistance)) {
        continue;
      }
      apexAway++;
      if (join.maxIntensity() < minHeight) {
        continue;
      }
      aboveMinHeight++;
      final float rt = scans[join.apexScan()].getRetentionTime();
      final boolean inAdap = isResolved(adapResolved, join.mz(), rt, tolerance, rtTolerance);
      final boolean inFast = isResolved(fastResolved, join.mz(), rt, tolerance, rtTolerance);
      final String category;
      if (inAdap && inFast) {
        both++;
        continue;
      } else if (inAdap) {
        onlyAdap++;
        category = "only adap";
      } else if (inFast) {
        onlyFast++;
        category = "only fast";
      } else {
        neither++;
        category = "neither";
      }
      examples.add("""
          %s: mz %.5f rt %.3f max %.3g apex scan %d, trace scans %d-%d, seed mz %.5f (%.1f ppm) \
          scans %d-%d; %s""".formatted(category, join.mz(), rt, join.maxIntensity(),
          join.apexScan(), join.firstScan(), join.lastScan(), join.seedMz(), join.ppmFromSeed(),
          join.seedFirst(), join.seedLast(),
          describeFastChromatogram(join, fastIndex, fastChromatogramList, fastChromatograms,
              chromatographicThreshold)));
    }
    return new Result(joins.size(), apexAway, aboveMinHeight, both, onlyAdap, onlyFast, neither,
        examples);
  }

  /**
   * @return the fast chromatogram with the apex data point of the trace and whether the
   * chromatographic threshold of the resolver removes the apex
   */
  @NotNull
  private static String describeFastChromatogram(@NotNull Join join,
      @NotNull ChromatogramDataPointIndex fastIndex, @NotNull FeatureList fastChromatogramList,
      @NotNull List<EvaluatedChromatogram> fastChromatograms, double chromatographicThreshold) {
    final int c = fastIndex.find(join.apexScan(), join.apexMz());
    if (c < 0) {
      return "apex in no fast chromatogram";
    }
    final EvaluatedChromatogram chromatogram = fastChromatograms.get(c);
    final IonTimeSeries<? extends Scan> series = fastChromatogramList.getRow(c).getFeatures()
        .getFirst().getFeatureData();
    // the resolver input, detected data points and flanking zeros
    final double[] intensities = series.getIntensityValues(new double[series.getNumberOfValues()]);
    final double threshold = MathUtils.calcQuantile(intensities, chromatographicThreshold);
    return "apex in fast chromatogram mz %.5f max %.3g, %d data points, threshold %.3g, apex %s".formatted(
        chromatogram.weightedMz(), chromatogram.intensities()[chromatogram.apexIndex()],
        chromatogram.size(), threshold,
        join.maxIntensity() < threshold ? "below threshold" : "above threshold");
  }

  private static boolean isResolved(@NotNull FeatureList flist, double mz, float rt,
      @NotNull MZTolerance tolerance, float rtTolerance) {
    final double tol = tolerance.getMzToleranceForMass(mz);
    for (final FeatureListRow row : flist.getRows()) {
      if (Math.abs(row.getAverageMZ() - mz) <= tol
          && Math.abs(row.getAverageRT() - rt) <= rtTolerance) {
        return true;
      }
    }
    return false;
  }

  /**
   * Records the apex scan of each trace that the wrapped collector records, index is the record
   * index of the collector.
   */
  private static final class ApexRecorder implements MassTraceSweepListener {

    private final @NotNull TraceSummaryCollector summaries;
    private final IntArrayList apexScans = new IntArrayList();
    private final DoubleArrayList apexMzs = new DoubleArrayList();
    private double[] slotApexIntensity = new double[1024];
    private double[] slotApexMz = new double[1024];
    private int[] slotApexScan = new int[1024];

    private ApexRecorder(@NotNull TraceSummaryCollector summaries) {
      this.summaries = summaries;
    }

    @Override
    public void onTraceCreated(@NotNull MassTraceSweeper sweeper, int slot) {
      if (slot >= slotApexScan.length) {
        final int capacity = Math.max(slot + 1, slotApexScan.length * 2);
        slotApexIntensity = Arrays.copyOf(slotApexIntensity, capacity);
        slotApexMz = Arrays.copyOf(slotApexMz, capacity);
        slotApexScan = Arrays.copyOf(slotApexScan, capacity);
      }
      slotApexIntensity[slot] = Double.NEGATIVE_INFINITY;
      summaries.onTraceCreated(sweeper, slot);
    }

    @Override
    public void onDataPointAssigned(@NotNull MassTraceSweeper sweeper, int slot, int scanIndex,
        double mz, double intensity) {
      // the first maximum like the max intensity of the sweeper
      if (intensity > slotApexIntensity[slot]) {
        slotApexIntensity[slot] = intensity;
        slotApexMz[slot] = mz;
        slotApexScan[slot] = scanIndex;
      }
      summaries.onDataPointAssigned(sweeper, slot, scanIndex, mz, intensity);
    }

    @Override
    public void onCollision(@NotNull MassTraceSweeper sweeper, int slotA, int slotB) {
      summaries.onCollision(sweeper, slotA, slotB);
    }

    @Override
    public void onTraceClosed(@NotNull MassTraceSweeper sweeper, int slot) {
      final int before = summaries.getNumRecordedTraces();
      summaries.onTraceClosed(sweeper, slot);
      if (summaries.getNumRecordedTraces() > before) {
        apexScans.add(slotApexScan[slot]);
        apexMzs.add(slotApexMz[slot]);
      }
    }

    @Override
    public void onScanFinished(@NotNull MassTraceSweeper sweeper, int scanIndex) {
      summaries.onScanFinished(sweeper, scanIndex);
    }

    @Override
    public boolean isCollisionTrackingEnabled() {
      return summaries.isCollisionTrackingEnabled();
    }
  }
}
