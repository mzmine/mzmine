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

package io.github.mzmine.modules.tools.tools_autoparam.runphases;

import io.github.mzmine.datamodel.PolarityType;
import java.util.Arrays;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Sodium formate clusters as marker for calibrant plugs and salt peaks. Sodium formate is used as
 * calibrant and also elutes as salt, e.g. at the end of a HILIC wash.
 */
final class SaltClusters {

  private static final Logger logger = Logger.getLogger(SaltClusters.class.getName());

  private static final double SODIUM_FORMATE = 67.98743;
  private static final double FORMATE_ANION = 44.99820;
  private static final double SODIUM_CATION = 22.98922;
  // decision: clusters above n=8 are often below the detection limit even in a calibrant plug
  private static final int CLUSTERS = 8;
  // decision: 25th percentile across clusters, most of the series has to rise together
  private static final double CLUSTER_QUANTILE = 0.25;
  // decision: 3 scans only removes single scan spikes, a calibrant plug may last just ~6 scans
  private static final int SMOOTHING_SCANS = 3;
  // decision: salt events are searched in the first and last 20% of the run, cluster flicker at
  // the start of an RP wash would otherwise be taken for a plug
  private static final double EVENT_RUN_FRACTION = 0.2;
  // the local salt level is the rolling median over +-0.75 min
  private static final double LOCAL_HALF_WIDTH = 0.75;
  // assumption: a calibrant plug or salt peak is at least 10x above its surroundings
  private static final double MIN_PROMINENCE = 1.0;
  // decision: a broad salt region (wash) does not cut the range, only short plugs or peaks do
  private static final double MAX_EVENT_WIDTH = 0.5;
  private static final int MIN_EVENT_SCANS = 4;
  // log10 TIC above its local level, 0.1 is about +25%
  private static final double MIN_TIC_RISE = 0.1;
  // the cut is placed where the event falls below 30% of its prominence (log scale), incl. tails
  private static final double EVENT_CUT_LEVEL = 0.3;

  private SaltClusters() {
  }

  /**
   * [(HCOONa)n + HCOO]- in negative and [(HCOONa)n + Na]+ in positive mode, n = 1 to
   * {@link #CLUSTERS}.
   */
  static double @NotNull [] sodiumFormateMzs(@NotNull PolarityType polarity) {
    final double base = polarity == PolarityType.NEGATIVE ? FORMATE_ANION : SODIUM_CATION;
    final double[] mzs = new double[CLUSTERS];
    for (int n = 1; n <= CLUSTERS; n++) {
      mzs[n - 1] = base + n * SODIUM_FORMATE;
    }
    return mzs;
  }

  /**
   * Salt signal per scan: lower quartile over the clusters of log10 intensity relative to that
   * cluster's run median, smoothed over {@link #SMOOTHING_SCANS}.
   * <p>
   * decision: normalizing per cluster keeps a single cluster that is always present as background
   * (e.g. the n=1 formate cluster in negative mode) from masking a calibrant plug in the others.
   *
   * @param clusterIntensities intensity per cluster and scan
   * @return log10 fold change of the salt clusters, 0 is the typical level
   */
  static double @NotNull [] saltTrace(double @NotNull [][] clusterIntensities) {
    final int n = clusterIntensities[0].length;
    final double[][] norm = new double[clusterIntensities.length][];
    for (int c = 0; c < clusterIntensities.length; c++) {
      final double[] log = TraceMath.log10(clusterIntensities[c]);
      final double median = TraceMath.median(log);
      norm[c] = Arrays.stream(log).map(v -> v - median).toArray();
    }
    final double[] salt = new double[n];
    final double[] scan = new double[clusterIntensities.length];
    for (int i = 0; i < n; i++) {
      for (int c = 0; c < clusterIntensities.length; c++) {
        scan[c] = norm[c][i];
      }
      salt[i] = TraceMath.quantile(scan, CLUSTER_QUANTILE);
    }
    return TraceMath.rollingMedian(salt, SMOOTHING_SCANS);
  }

  /**
   * Short, prominent salt events near the start and end of the run. Broad salt regions, e.g.
   * clusters forming during the wash, are ignored.
   *
   * @param rt        retention times of the MS1 scans in minutes
   * @param saltLog   see {@link #saltTrace(double[][])}
   * @param logTic    log10 TIC per scan
   * @param minLateRt late events are only searched after this time, NaN for no limit
   */
  static @NotNull SaltEvents events(double @NotNull [] rt, double @NotNull [] saltLog,
      double @NotNull [] logTic, double minLateRt) {
    final int n = rt.length;
    final double[] local = TraceMath.rollingMedianRt(rt, saltLog, LOCAL_HALF_WIDTH);
    final double[] ticLocal = TraceMath.rollingMedianRt(rt, logTic, LOCAL_HALF_WIDTH);
    final double[] prominence = new double[n];
    final double[] ticProminence = new double[n];
    for (int i = 0; i < n; i++) {
      prominence[i] = saltLog[i] - local[i];
      ticProminence[i] = logTic[i] - ticLocal[i];
    }
    // decision: windows by retention time, MS1 scan density varies with DDA load
    final double runTime = rt[n - 1] - rt[0];
    final int earlyTo = TraceMath.indexAtRt(rt, rt[0] + runTime * EVENT_RUN_FRACTION);
    final int lateWindow = TraceMath.indexAtRt(rt, rt[n - 1] - runTime * EVENT_RUN_FRACTION);
    final int lateFrom = Double.isNaN(minLateRt) ? lateWindow
        : Math.max(lateWindow, TraceMath.indexAtRt(rt, minLateRt));

    final double early = event(rt, prominence, ticProminence,
        TraceMath.argMax(prominence, 0, earlyTo), true);
    final double late = lateFrom < n ? event(rt, prominence, ticProminence,
        TraceMath.argMax(prominence, lateFrom, n), false) : Double.NaN;
    return new SaltEvents(early, late);
  }

  /**
   * @param top   index of the highest prominence
   * @param early true to return the end of the event, false to return its start
   * @return the cut time or NaN if there is no short, prominent event
   */
  private static double event(double @NotNull [] rt, double @NotNull [] prominence,
      double @NotNull [] ticProminence, int top, boolean early) {
    final double max = prominence[top];
    if (Double.isNaN(max) || max < MIN_PROMINENCE) {
      return Double.NaN;
    }
    final int halfFrom = walk(prominence, top, -1, 0.5 * max);
    final int halfTo = walk(prominence, top, +1, 0.5 * max);
    final double width = rt[halfTo] - rt[halfFrom];
    // walk stops on the first scan below the level, count only the scans above it
    final int scans = halfTo - halfFrom - 1;
    double ticRise = Double.NEGATIVE_INFINITY;
    for (int i = halfFrom; i <= halfTo; i++) {
      ticRise = Math.max(ticRise, ticProminence[i]);
    }
    logger.finest(
        "salt event at %.2f min, prominence %.2f, width %.2f min, %d scans, TIC +%.2f".formatted(
            rt[top], max, width, scans, ticRise));
    // decision: a plug or salt peak lasts several scans, adds visibly to the TIC and is short.
    // Cluster flicker and broad salt regions in a wash cut nothing
    if (scans < MIN_EVENT_SCANS || ticRise < MIN_TIC_RISE || width > MAX_EVENT_WIDTH) {
      return Double.NaN;
    }
    return rt[walk(prominence, top, early ? +1 : -1, EVENT_CUT_LEVEL * max)];
  }

  /**
   * @return the first index from start in direction step where the value drops to level or below
   */
  private static int walk(double @NotNull [] values, int start, int step, double level) {
    int i = start;
    while (i + step >= 0 && i + step < values.length && values[i] > level) {
      i += step;
    }
    return i;
  }
}
