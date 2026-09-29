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

  @Test
  void flowOnVoidWashAndReequilibration() {
    final MsPhases p = RunPhaseDetection.msPhases(rt(), suppression(true), gradient(),
        SCANS_PER_MINUTE, Double.NaN);
    Assertions.assertEquals(0.5, p.flowOn(), 0.01);
    Assertions.assertEquals(10, p.flowOff(), 0.01);
    // the step detection sees the drop one step lag (0.1 min) early
    Assertions.assertEquals(0.95, p.voidTime(), 0.1);
    Assertions.assertEquals(6.6, p.washStart(), 0.1);
    Assertions.assertEquals(8.0, p.reequilibration(), 0.1);
  }

  @Test
  void noFlowOnWithoutNoFlowSegment() {
    final MsPhases p = RunPhaseDetection.msPhases(rt(), suppression(false), gradient(),
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
    final MsPhases p = RunPhaseDetection.msPhases(rt(), s, gradient(), SCANS_PER_MINUTE,
        Double.NaN);
    Assertions.assertTrue(Double.isNaN(p.voidTime()));
  }

  @Test
  void reequilibrationIsOnlySearchedAfterThePumpBound() {
    final MsPhases p = RunPhaseDetection.msPhases(rt(), suppression(true), gradient(),
        SCANS_PER_MINUTE, 9.0);
    Assertions.assertTrue(Double.isNaN(p.reequilibration()));
  }
}
