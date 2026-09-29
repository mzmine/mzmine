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

import io.github.mzmine.datamodel.SimpleRange.SimpleFloatRange;
import org.jetbrains.annotations.NotNull;

/**
 * Phases of an LC-MS run: dead volume at the start, wash and re-equilibration at the end. All times
 * in minutes, NaN when not detected.
 *
 * @param flowOn          end of a no-flow or divert segment at the start of the run
 * @param flowOff         start of a no-flow segment at the end, otherwise the last scan
 * @param voidTime        void (dead volume) time, the background ions are suppressed
 * @param washStart       start of the wash seen in the background ion gradient proxy
 * @param reequilibration re-equilibration seen in the background ion gradient proxy
 * @param earlySaltEnd    end of a calibrant plug or salt peak near the start
 * @param lateSaltStart   start of a calibrant plug or salt peak near the end
 * @param solvent         wash and re-equilibration from the solvent composition, pump time
 * @param pressure        wash and re-equilibration from the pump pressure, pump time
 */
record RunPhases(double flowOn, double flowOff, double voidTime, double washStart,
                 double reequilibration, double earlySaltEnd, double lateSaltStart,
                 @NotNull GradientEnd solvent, @NotNull GradientEnd pressure) {

  // end extension when nothing on the MS side confirms the pump based re-equilibration
  private static final double SOLVENT_END_EXTENSION = 0.10;
  private static final double PRESSURE_END_EXTENSION = 0.05;

  /**
   * @return phases without any detection, the effective range is the full run
   */
  static @NotNull RunPhases undetected(double lastRt) {
    return new RunPhases(Double.NaN, lastRt, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
        Double.NaN, GradientEnd.NONE, GradientEnd.NONE);
  }

  /**
   * The latest of flow on, void and salt plug end.
   * <p>
   * decision: no derived start after the void (e.g. background recovery), it cut real early peaks.
   * Falls back to 0 when none of the three is detected.
   */
  double effectiveStart() {
    double start = 0;
    for (final double candidate : new double[]{flowOn, voidTime, earlySaltEnd}) {
      if (!Double.isNaN(candidate)) {
        start = Math.max(start, candidate);
      }
    }
    return start;
  }

  /**
   * The pump traces are in pump time, the MS sees a change later. Their re-equilibration is the
   * minimum the end may be set to: pressure is searched after the solvent result, salt events and G
   * after the pump result.
   *
   * @return the lower bound for the end, NaN without pump traces
   */
  double pumpLowerBound() {
    return pressure.hasReequilibration() ? pressure.reequilibration() : solvent.reequilibration();
  }

  /**
   * end: solvent, then pressure (both only set a lower bound), then salts, then G, then flow off
   */
  @NotNull RunPhaseEndSource endSource() {
    // salts and G are only searched after the pump lower bound, the earlier one ends the range
    if (!Double.isNaN(lateSaltStart) && (Double.isNaN(reequilibration)
        || lateSaltStart < reequilibration)) {
      return RunPhaseEndSource.SALT;
    }
    if (!Double.isNaN(reequilibration)) {
      return RunPhaseEndSource.GRADIENT;
    }
    if (pressure.hasReequilibration()) {
      return RunPhaseEndSource.PRESSURE;
    }
    if (solvent.hasReequilibration()) {
      return RunPhaseEndSource.SOLVENT;
    }
    return RunPhaseEndSource.FLOW;
  }

  double effectiveEnd() {
    final double bound = pumpLowerBound();
    final double end = switch (endSource()) {
      case SALT -> lateSaltStart;
      case GRADIENT -> reequilibration;
      // decision: no MS side confirmation after the pump result, extend it by the typical delay
      // to the MS. assumption: the percentage is relative to the retention time
      case SOLVENT -> bound * (1 + SOLVENT_END_EXTENSION);
      case PRESSURE -> bound * (1 + PRESSURE_END_EXTENSION);
      case FLOW -> flowOff;
    };
    // the pump result is the minimum, a salt cut may reach back before it
    return Math.min(flowOff, Double.isNaN(bound) ? end : Math.max(bound, end));
  }

  /**
   * @return the retention time range in minutes that contains the separation
   */
  /**
   * @return true if the start or the end is based on a detected event, false if both are only the
   * fallbacks (0 and the end of flow or of the run)
   */
  boolean hasDetection() {
    final boolean start =
        !Double.isNaN(flowOn) || !Double.isNaN(voidTime) || !Double.isNaN(earlySaltEnd);
    return start || endSource() != RunPhaseEndSource.FLOW;
  }

  @NotNull SimpleFloatRange effectiveRtRange() {
    final float start = (float) effectiveStart();
    return new SimpleFloatRange(start, Math.max(start, (float) effectiveEnd()));
  }

  @Override
  public @NotNull String toString() {
    return ("flow %.2f-%.2f | void %.2f | G wash %.2f reequil %.2f | salt %.2f/%.2f"
        + " | solvent %.2f/%.2f | pressure %.2f/%.2f | effective %.2f-%.2f (end: %s)").formatted(
        flowOn, flowOff, voidTime, washStart, reequilibration, earlySaltEnd, lateSaltStart,
        solvent.washStart(), solvent.reequilibration(), pressure.washStart(),
        pressure.reequilibration(), effectiveStart(), effectiveEnd(), endSource());
  }
}
