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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFile;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeries;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeriesData;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.ChromatogramType;
import java.util.Arrays;
import java.util.Locale;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Wash and re-equilibration from the LC pump traces (solvent composition and pressure) stored as
 * {@link OtherDataFile}s. Not all files provide them.
 * <p>
 * assumption: the pump traces are in pump time. The MS sees a composition change later by the dwell
 * and column volume, so these boundaries are only a lower bound for the MS side end.
 */
final class PumpTraces {

  // traces are binned to this resolution first, pressure traces have up to ~4000 points per min
  private static final double BIN_MINUTES = 0.01;
  private static final int MIN_BINS = 10;
  // wash plateau of the solvent composition, relative to its full gradient amplitude
  private static final double SOLVENT_PLATEAU = 0.98;
  // decision: a composition trace that changes less than this (%) is isocratic, no gradient
  private static final double MIN_SOLVENT_AMPLITUDE = 5;
  private static final double PRESSURE_SMOOTH_HALF_WIDTH = 0.05;
  private static final double PRESSURE_INITIAL_MINUTES = 0.1;
  // the wash extreme has to differ from the initial pressure by this share of the pressure range
  private static final double MIN_PRESSURE_AMPLITUDE = 0.3;
  // after the extreme the pressure has to come back by at least half of its amplitude
  private static final double MIN_PRESSURE_RETURN = 0.5;
  // the wash plateau around the extreme, share of the pressure range
  private static final double PRESSURE_PLATEAU_TOLERANCE = 0.1;
  private static final double MIN_WASH_MINUTES = 0.4;

  private PumpTraces() {
  }

  /**
   * @return the solvent composition trace with the largest change, null if there is none
   */
  static @Nullable PumpTrace solventTrace(@NotNull RawDataFile raw, double rtFrom, double rtTo) {
    PumpTrace best = null;
    double bestAmplitude = MIN_SOLVENT_AMPLITUDE;
    for (final OtherDataFile other : raw.getOtherDataFiles()) {
      final OtherTimeSeriesData data = other.getOtherTimeSeriesData();
      // assumption: composition traces are the ones in percent, e.g. "Solvent Ratio B - [%]"
      if (data == null || !isPercent(data.getTimeSeriesRangeUnit())) {
        continue;
      }
      for (final OtherFeature feature : data.getRawTraces()) {
        final PumpTrace trace = binned(feature.getFeatureData(), rtFrom, rtTo);
        if (trace == null) {
          continue;
        }
        final double amplitude = TraceMath.max(trace.values()) - TraceMath.min(trace.values());
        if (amplitude > bestAmplitude) {
          bestAmplitude = amplitude;
          best = trace;
        }
      }
    }
    return best;
  }

  /**
   * @return the first pump pressure trace, null if there is none
   */
  static @Nullable PumpTrace pressureTrace(@NotNull RawDataFile raw, double rtFrom, double rtTo) {
    for (final OtherDataFile other : raw.getOtherDataFiles()) {
      final OtherTimeSeriesData data = other.getOtherTimeSeriesData();
      if (data == null || data.getChromatogramType() != ChromatogramType.PRESSURE) {
        continue;
      }
      for (final OtherFeature feature : data.getRawTraces()) {
        final PumpTrace trace = binned(feature.getFeatureData(), rtFrom, rtTo);
        if (trace != null) {
          return trace;
        }
      }
    }
    return null;
  }

  /**
   * The programmed gradient holds one solvent at a steady extreme during the wash and resets the
   * ratio to the initial one for re-equilibration.
   */
  static @NotNull GradientEnd solventEnd(@Nullable PumpTrace trace) {
    if (trace == null) {
      return GradientEnd.NONE;
    }
    final double[] t = trace.rt();
    final double[] c = trace.values();
    final double initial = c[0];
    int extreme = 0;
    for (int i = 0; i < c.length; i++) {
      if (Math.abs(c[i] - initial) > Math.abs(c[extreme] - initial)) {
        extreme = i;
      }
    }
    final double amplitude = c[extreme] - initial;
    if (Math.abs(amplitude) < MIN_SOLVENT_AMPLITUDE) {
      return GradientEnd.NONE;
    }
    int washStart = extreme;
    while (washStart > 0 && (c[washStart - 1] - initial) / amplitude >= SOLVENT_PLATEAU) {
      washStart--;
    }
    int washEnd = extreme;
    while (washEnd < c.length - 1 && (c[washEnd + 1] - initial) / amplitude >= SOLVENT_PLATEAU) {
      washEnd++;
    }
    // the run may end during the wash
    final double reequilibration = washEnd < c.length - 1 ? t[washEnd] : Double.NaN;
    return new GradientEnd(t[washStart], reequilibration);
  }

  /**
   * The wash drives the pressure to its extreme relative to the initial pressure (RP with ACN:
   * lowest, RP with IPA or HILIC: highest). Re-equilibration starts where the pressure leaves that
   * extreme and returns towards the initial pressure.
   * <p>
   * decision: the extreme instead of the steepest step, the return in HILIC can take minutes.
   *
   * @param minRt the solvent based re-equilibration, the pressure is only searched after it. NaN
   *              without a solvent trace
   */
  static @NotNull GradientEnd pressureEnd(@Nullable PumpTrace trace, double minRt) {
    if (trace == null) {
      return GradientEnd.NONE;
    }
    final double[] t = trace.rt();
    final int n = t.length;
    final int half = Math.max(1, (int) Math.round(PRESSURE_SMOOTH_HALF_WIDTH / BIN_MINUTES));
    final double[] p = TraceMath.rollingMedian(trace.values(), 2 * half + 1);
    final int initialPoints = Math.max(1, (int) Math.round(PRESSURE_INITIAL_MINUTES / BIN_MINUTES));
    if (n < 4 * initialPoints) {
      return GradientEnd.NONE;
    }
    final double initial = TraceMath.median(Arrays.copyOfRange(p, 0, initialPoints));

    // assumption: the wash is in the second half of the run, mid gradient viscosity maxima of
    // water/methanol are closer to the initial pressure than the wash extreme
    int from = n / 2;
    while (!Double.isNaN(minRt) && from < n - 1 && t[from] < minRt) {
      from++;
    }
    int extreme = from;
    for (int i = from; i < n; i++) {
      if (Math.abs(p[i] - initial) > Math.abs(p[extreme] - initial)) {
        extreme = i;
      }
    }
    final double amplitude = p[extreme] - initial;
    final double range = TraceMath.max(p) - TraceMath.min(p);
    if (Math.abs(amplitude) < MIN_PRESSURE_AMPLITUDE * range) {
      return GradientEnd.NONE;
    }
    // the run has to return towards the initial pressure, otherwise it ends in the wash
    boolean returned = false;
    for (int i = extreme; i < n; i++) {
      if ((p[i] - initial) / amplitude < 1 - MIN_PRESSURE_RETURN) {
        returned = true;
        break;
      }
    }
    if (!returned) {
      return GradientEnd.NONE;
    }
    final double level = 1 - PRESSURE_PLATEAU_TOLERANCE * range / Math.abs(amplitude);
    int onset = extreme;
    while (onset < n - 1 && (p[onset + 1] - initial) / amplitude >= level) {
      onset++;
    }
    int washStart = extreme;
    while (washStart > n / 2 && (p[washStart - 1] - initial) / amplitude >= level) {
      washStart--;
    }
    final double wash = t[onset] - t[washStart] >= MIN_WASH_MINUTES ? t[washStart] : Double.NaN;
    return new GradientEnd(wash, t[onset]);
  }

  private static boolean isPercent(@NotNull String unit) {
    final String u = unit.toLowerCase(Locale.ROOT);
    return u.contains("%") || u.contains("percent");
  }

  /**
   * Median per {@link #BIN_MINUTES} bin within the MS retention time range.
   */
  private static @Nullable PumpTrace binned(@NotNull OtherTimeSeries series, double rtFrom,
      double rtTo) {
    final int bins = (int) Math.ceil((rtTo - rtFrom) / BIN_MINUTES) + 1;
    final int[] counts = new int[bins];
    final int n = series.getNumberOfValues();
    for (int i = 0; i < n; i++) {
      final double rt = series.getRetentionTime(i);
      if (rt >= rtFrom && rt <= rtTo) {
        counts[(int) ((rt - rtFrom) / BIN_MINUTES)]++;
      }
    }
    final double[][] perBin = new double[bins][];
    for (int b = 0; b < bins; b++) {
      perBin[b] = new double[counts[b]];
    }
    for (int i = 0; i < n; i++) {
      final double rt = series.getRetentionTime(i);
      if (rt >= rtFrom && rt <= rtTo) {
        final int bin = (int) ((rt - rtFrom) / BIN_MINUTES);
        perBin[bin][--counts[bin]] = series.getIntensity(i);
      }
    }
    final double[] rt = new double[bins];
    final double[] values = new double[bins];
    int filled = 0;
    for (int b = 0; b < bins; b++) {
      if (perBin[b].length == 0) {
        continue;
      }
      rt[filled] = rtFrom + (b + 0.5) * BIN_MINUTES;
      values[filled] = TraceMath.median(perBin[b]);
      filled++;
    }
    if (filled < MIN_BINS) {
      return null;
    }
    return new PumpTrace(series.getName(), Arrays.copyOf(rt, filled),
        Arrays.copyOf(values, filled));
  }
}
