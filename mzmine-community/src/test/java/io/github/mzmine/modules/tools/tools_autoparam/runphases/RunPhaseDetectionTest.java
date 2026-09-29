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

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Flow, void and gradient end detection on synthetic background ion traces.
 */
class RunPhaseDetectionTest {

  // 100 scans per minute over 10 min
  private static final double SCANS_PER_MINUTE = 100;
  private static final int N = 1001;
  private static final double NaN = Double.NaN;

  private static double @NotNull [] rt() {
    final double[] rt = new double[N];
    for (int i = 0; i < N; i++) {
      rt[i] = i / SCANS_PER_MINUTE;
    }
    return rt;
  }

  /**
   * S: no flow until 0.5 min, suppression by the void at 1.0-1.2 min.
   */
  private static double @NotNull [] suppression(boolean divert) {
    final double[] s = new double[N];
    for (int i = 0; i < N; i++) {
      final double t = i / SCANS_PER_MINUTE;
      s[i] = divert && t < 0.5 ? -5 : t >= 1.0 && t < 1.2 ? -1 : 0;
    }
    return s;
  }

  /**
   * G: flat until 3 min, rises to 1 at 7 min, wash until 8 min, re-equilibration step at 8 min.
   */
  private static double @NotNull [] gradient() {
    final double[] g = new double[N];
    for (int i = 0; i < N; i++) {
      final double t = i / SCANS_PER_MINUTE;
      g[i] = t < 3 ? 0 : t < 7 ? (t - 3) / 4 : t < 8 ? 1 : 0;
    }
    return g;
  }

  /**
   * log TIC, 5 before and 8 after the step, flat 8 without a step
   *
   * @param step time of the TIC step, NaN for no step
   */
  private static double @NotNull [] tic(double step) {
    final double[] tic = new double[N];
    for (int i = 0; i < N; i++) {
      tic[i] = !Double.isNaN(step) && i / SCANS_PER_MINUTE < step ? 5 : 8;
    }
    return tic;
  }

  @Test
  void flowOnVoidWashAndReequilibration() {
    final MsPhases p = RunPhaseDetection.msPhases(rt(), suppression(true), gradient(), tic(NaN),
        SCANS_PER_MINUTE, Double.NaN);
    Assertions.assertEquals(0.5, p.flowOn(), 0.01);
    Assertions.assertEquals(10, p.flowOff(), 0.01);
    // the step detection sees the drop one step lag (0.1 min) early
    Assertions.assertEquals(0.95, p.voidTime(), 0.1);
    Assertions.assertEquals(6.6, p.washStart(), 0.1);
    Assertions.assertEquals(8.0, p.reequilibration(), 0.1);
  }

  @Test
  void flowOnIsRefinedByTheTicStep() {
    // background ions ramp up late, the TIC jumps at the valve switch
    final MsPhases p = RunPhaseDetection.msPhases(rt(), suppression(true), gradient(), tic(0.4),
        SCANS_PER_MINUTE, Double.NaN);
    Assertions.assertEquals(0.4, p.flowOn(), 0.011);
  }

  @Test
  void flowOnIsTheFirstScanClearlyAboveTheNoFlowLevel() {
    // TIC ramps from 5 to 8 within 0.4-0.45 min, the middle of the step is only reached at 0.43
    final double[] tic = tic(0.4);
    for (int i = 40; i < 45; i++) {
      tic[i] = 5 + 3 * (i - 40) / 5d;
    }
    // a single spike during the no-flow segment is not the switch
    tic[10] = 8;
    final MsPhases p = RunPhaseDetection.msPhases(rt(), suppression(true), gradient(), tic,
        SCANS_PER_MINUTE, Double.NaN);
    Assertions.assertEquals(0.41, p.flowOn(), 0.001);
  }

  @Test
  void noFlowOnWithoutNoFlowSegment() {
    final MsPhases p = RunPhaseDetection.msPhases(rt(), suppression(false), gradient(), tic(NaN),
        SCANS_PER_MINUTE, Double.NaN);
    Assertions.assertTrue(Double.isNaN(p.flowOn()));
  }

  @Test
  void smallSuppressionIsNoVoid() {
    final double[] s = new double[N];
    for (int i = 0; i < N; i++) {
      // co-eluting matrix, less than 3x
      s[i] = i >= 100 && i < 120 ? -0.3 : 0;
    }
    final MsPhases p = RunPhaseDetection.msPhases(rt(), s, gradient(), tic(NaN), SCANS_PER_MINUTE,
        Double.NaN);
    Assertions.assertTrue(Double.isNaN(p.voidTime()));
  }

  @Test
  void reequilibrationIsOnlySearchedAfterThePumpBound() {
    final MsPhases p = RunPhaseDetection.msPhases(rt(), suppression(true), gradient(), tic(NaN),
        SCANS_PER_MINUTE, 9.0);
    Assertions.assertTrue(Double.isNaN(p.reequilibration()));
  }
}
