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

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.SimpleRange.SimpleFloatRange;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Detects the phases of an LC-MS run (no flow, void, salt plugs, wash, re-equilibration) and
 * derives the retention time range that contains the separation.
 * <p>
 * Start: flow on, void and salt plug end, the latest one. End: solvent composition, then pump
 * pressure (both only a lower bound, pump time), then salt peaks, then the background ion gradient
 * proxy G, then the end of the run. See {@link RunPhases}.
 * <p>
 * Everything is derived from the MS1 scans in a few passes plus the optional pump traces of the raw
 * file, no feature detection is needed.
 */
public final class RunPhaseDetection {

  // assumption: fewer MS1 scans do not resolve the phases
  private static final int MIN_SCANS = 20;
  // rolling median window for S and G
  private static final double SMOOTHING_MINUTES = 0.3;
  private static final double STEP_LAG_MINUTES = 0.1;
  private static final double HOLD_MINUTES = 0.2;
  // no flow / divert valve: background ions are ~5x below their median
  private static final double NO_FLOW_LEVEL = -0.7;
  // the TIC has to jump by at least 3x at the flow on to refine the S based time
  private static final double MIN_FLOW_ON_TIC_STEP = 0.5;
  // flow on is the first scan above the no-flow TIC noise, in log10
  private static final double FLOW_ON_NOISE_FACTOR = 5;
  private static final double MIN_FLOW_ON_RISE = 0.2;
  // decision: small dips of the background ions also come from co-eluting matrix (lipids, salts
  // in HILIC). Only a near disappearance (>3x drop) is treated as the void.
  private static final double MIN_VOID_DROP = 0.5;
  // the void is searched in the first 30% of the run
  private static final double VOID_RUN_FRACTION = 0.3;
  // decision: a suppression that does not recover within 2 min is a gradient effect, no void
  private static final double MAX_VOID_RECOVERY_MINUTES = 2;
  // re-equilibration is searched in the last 40% of the run
  private static final double REEQUILIBRATION_RUN_FRACTION = 0.6;
  // the G step has to cover this share of the G range
  private static final double MIN_REEQUILIBRATION_STEP = 0.35;
  private static final double WASH_PLATEAU_TOLERANCE = 0.1;
  private static final double MIN_WASH_MINUTES = 0.4;

  private RunPhaseDetection() {
  }

  /**
   * @param file     the raw data file, used for the pump traces
   * @param ms1Scans the MS1 scans, with mass lists if available
   * @return the retention time range in minutes that contains the separation, null if there are too
   * few MS1 scans or neither the start nor the end is based on a detected event
   */
  public static @Nullable SimpleFloatRange detect(@NotNull RawDataFile file,
      @NotNull List<? extends Scan> ms1Scans) {
    if (ms1Scans.size() < MIN_SCANS) {
      return null;
    }
    final RunPhases phases = analyze(file, ms1Scans);
    // decision: no range from fallbacks only, callers decide how to handle undetected files
    return phases.hasDetection() ? phases.effectiveRtRange() : null;
  }

  /**
   * @param ms1Scans at least {@link #MIN_SCANS} MS1 scans
   */
  private static @NotNull RunPhases analyze(@NotNull RawDataFile file,
      @NotNull List<? extends Scan> ms1Scans) {
    final int n = ms1Scans.size();
    final double[] rt = new double[n];
    final double[] tic = new double[n];
    for (int i = 0; i < n; i++) {
      final MassSpectrum spectrum = TraceMath.spectrum(ms1Scans.get(i));
      rt[i] = ms1Scans.get(i).getRetentionTime();
      double sum = 0;
      for (int j = 0; j < spectrum.getNumberOfDataPoints(); j++) {
        sum += spectrum.getIntensityValue(j);
      }
      tic[i] = sum;
    }

    final double[] ions = BackgroundIons.ubiquitousIons(ms1Scans);
    // assumption: one polarity per file, polarity switching files are not handled here
    final double[] saltMzs = SaltClusters.sodiumFormateMzs(ms1Scans.getFirst().getPolarity());
    final double[][] ionIntensities = new double[ions.length][n];
    final double[][] saltIntensities = new double[saltMzs.length][n];
    for (int i = 0; i < n; i++) {
      final MassSpectrum spectrum = TraceMath.spectrum(ms1Scans.get(i));
      for (int k = 0; k < ions.length; k++) {
        ionIntensities[k][i] = TraceMath.maxInTolerance(spectrum, ions[k],
            BackgroundIons.ION_TOLERANCE);
      }
      for (int c = 0; c < saltMzs.length; c++) {
        saltIntensities[c][i] = TraceMath.maxInTolerance(spectrum, saltMzs[c],
            BackgroundIons.ION_TOLERANCE);
      }
    }

    final double scansPerMinute = n / Math.max(1e-6, rt[n - 1] - rt[0]);
    final int window = Math.max(3, (int) (scansPerMinute * SMOOTHING_MINUTES));
    final BackgroundTraces background = BackgroundIons.traces(ions, ionIntensities, saltMzs, window,
        n);
    final double[] logTic = TraceMath.log10(tic);
    final double[] salt = SaltClusters.saltTrace(saltIntensities);

    final PumpTrace solventTrace = PumpTraces.solventTrace(file, rt[0], rt[n - 1]);
    final PumpTrace pressureTrace = PumpTraces.pressureTrace(file, rt[0], rt[n - 1]);
    final GradientEnd solvent = PumpTraces.solventEnd(solventTrace);
    // pressure is only searched after the solvent result
    final GradientEnd pressure = PumpTraces.pressureEnd(pressureTrace, solvent.reequilibration());
    final double pumpBound =
        pressure.hasReequilibration() ? pressure.reequilibration() : solvent.reequilibration();

    // salt events and G are only searched after the pump result
    final MsPhases ms = msPhases(rt, background.suppression(), background.gradient(), logTic,
        scansPerMinute, pumpBound);
    final SaltEvents saltEvents = SaltClusters.events(rt, salt, logTic, pumpBound);

    return new RunPhases(ms.flowOn(), ms.flowOff(), ms.voidTime(), ms.washStart(),
        ms.reequilibration(), saltEvents.earlyEnd(), saltEvents.lateStart(), solvent, pressure);
  }

  /**
   * Flow, void and the gradient end from the background ion traces.
   *
   * @param s                    S, smoothed
   * @param g                    G, smoothed
   * @param logTic               log10 TIC, not smoothed, refines the flow on time
   * @param minReequilibrationRt re-equilibration is only searched after this, NaN for no limit
   */
  static @NotNull MsPhases msPhases(double @NotNull [] rt, double @NotNull [] s,
      double @NotNull [] g, double @NotNull [] logTic, double scansPerMinute,
      double minReequilibrationRt) {
    final int n = rt.length;
    final int lag = Math.max(2, (int) (scansPerMinute * STEP_LAG_MINUTES));
    final int hold = Math.max(3, (int) (scansPerMinute * HOLD_MINUTES));

    int a = 0;
    while (a < n - hold && !allAbove(s, a, a + hold, NO_FLOW_LEVEL)) {
      a++;
    }
    int b = n - 1;
    while (b > a + hold && !allAbove(s, b - hold, b, NO_FLOW_LEVEL)) {
      b--;
    }
    final double span = b - a;
    // flow on is only set if there is a no-flow segment at the start
    final double flowOn = a > 0 ? rt[flowOnScan(logTic, a, hold)] : Double.NaN;

    // void: strongest step down of the background ions early in the run = ion suppression
    double voidTime = Double.NaN;
    int recovered = a;
    int bestI = -1;
    double bestDrop = 0;
    // skip the flow-on transient
    for (int i = a + Math.max(2 * lag, hold); i < a + VOID_RUN_FRACTION * span && i + lag < n;
        i++) {
      final double drop = s[i - lag] - s[i + lag];
      if (drop > bestDrop) {
        bestDrop = drop;
        bestI = i;
      }
    }
    if (bestI >= 0 && bestDrop > MIN_VOID_DROP) {
      double min = s[bestI];
      int minI = bestI;
      for (int i = bestI; i < Math.min(n, bestI + 4 * lag); i++) {
        if (s[i] < min) {
          min = s[i];
          minI = i;
        }
      }
      final double target = min + 0.5 * (s[bestI - lag] - min);
      int j = minI;
      while (j < b && s[j] < target) {
        j++;
      }
      if (rt[j] - rt[bestI] <= MAX_VOID_RECOVERY_MINUTES) {
        voidTime = rt[bestI];
        recovered = j;
      }
    }

    // gradient proxy range after the early part, void and flow-on transients make G extreme
    final double[] gAfterStart = Arrays.copyOfRange(g, Math.max(recovered, (int) (a + 0.2 * span)),
        b + 1);
    final double range =
        TraceMath.quantile(gAfterStart, 0.97) - TraceMath.quantile(gAfterStart, 0.03);
    if (Double.isNaN(range)) {
      return new MsPhases(flowOn, rt[b], voidTime, Double.NaN, Double.NaN);
    }

    // re-equilibration: large and fast step of G late in the run
    double reequilibration = Double.NaN;
    int reI = -1;
    double bestStep = 0;
    final int windowStart = (int) (a + REEQUILIBRATION_RUN_FRACTION * span);
    final int reFrom = Double.isNaN(minReequilibrationRt) ? windowStart
        : Math.max(windowStart, TraceMath.indexAtRt(rt, minReequilibrationRt));
    for (int i = Math.max(lag, reFrom); i + lag <= b; i++) {
      final double step = Math.abs(g[i + lag] - g[i - lag]);
      if (step > bestStep) {
        bestStep = step;
        reI = i;
      }
    }
    if (reI >= 0 && bestStep > MIN_REEQUILIBRATION_STEP * range) {
      reequilibration = rt[reI];
    } else {
      reI = -1;
    }

    // wash: plateau of G right before re-equilibration (or the end)
    double wash = Double.NaN;
    final int end = reI >= 0 ? reI - lag : b;
    final double plateau = TraceMath.median(
        Arrays.copyOfRange(g, Math.max(a, end - 3 * hold), Math.max(a, end - lag)));
    int i = end - lag;
    while (i > a + 0.4 * span && Math.abs(g[i - 1] - plateau) < WASH_PLATEAU_TOLERANCE * range) {
      i--;
    }
    if (i >= 0 && rt[end] - rt[i] >= MIN_WASH_MINUTES) {
      wash = rt[i];
    }
    return new MsPhases(flowOn, rt[b], voidTime, wash, reequilibration);
  }

  /**
   * The background ions ramp up slowly after the flow starts and are smoothed, so S detects the
   * flow on late. The TIC jumps right at the switch.
   *
   * @param sFlowOn scan where S shows flow
   * @return the first scan of the TIC step that clearly leaves the no-flow level, sFlowOn if there
   * is no clear TIC step
   */
  private static int flowOnScan(double @NotNull [] logTic, int sFlowOn, int hold) {
    final double[] noFlow = Arrays.copyOfRange(logTic, 0, Math.max(1, sFlowOn - hold));
    final double before = TraceMath.median(noFlow);
    final double after = TraceMath.median(
        Arrays.copyOfRange(logTic, sFlowOn, Math.min(logTic.length, sFlowOn + hold)));
    if (!(after - before > MIN_FLOW_ON_TIC_STEP)) {
      return sFlowOn;
    }
    // decision: walk back from the S result, a spike during the no-flow segment is not the step.
    // First to the middle of the step, which is certainly part of it
    final double middle = (before + after) / 2;
    int i = sFlowOn;
    while (i > 0 && logTic[i - 1] > middle) {
      i--;
    }
    // then to the first scan that clearly leaves the no-flow level: above its noise (5x MAD),
    // at least 0.2 log (~1.6x) for very flat baselines
    final double mad = TraceMath.median(
        Arrays.stream(noFlow).map(v -> Math.abs(v - before)).toArray());
    final double threshold = before + Math.max(FLOW_ON_NOISE_FACTOR * mad, MIN_FLOW_ON_RISE);
    while (i > 0 && logTic[i - 1] > threshold) {
      i--;
    }
    return i;
  }

  private static boolean allAbove(double @NotNull [] values, int from, int to, double threshold) {
    for (int i = from; i <= to; i++) {
      if (values[i] <= threshold) {
        return false;
      }
    }
    return true;
  }
}
