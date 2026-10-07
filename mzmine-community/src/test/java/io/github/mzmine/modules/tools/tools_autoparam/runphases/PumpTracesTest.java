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

import java.util.function.DoubleUnaryOperator;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PumpTracesTest {

  private static final double STEP = 0.01;

  private static @NotNull PumpTrace trace(double end, @NotNull DoubleUnaryOperator value) {
    final int n = (int) Math.round(end / STEP) + 1;
    final double[] rt = new double[n];
    final double[] values = new double[n];
    for (int i = 0; i < n; i++) {
      rt[i] = i * STEP;
      values[i] = value.applyAsDouble(rt[i]);
    }
    return new PumpTrace("test", rt, values);
  }

  private static double interpolate(double t, double t0, double v0, double t1, double v1) {
    return v0 + (v1 - v0) * (t - t0) / (t1 - t0);
  }

  /**
   * 5 %B until 1 min, ramp to 95 %B at 6 min, hold until 7 min, back to 5 %B at 7.1 min.
   */
  private static double gradient(double t) {
    if (t < 1) {
      return 5;
    }
    if (t < 6) {
      return interpolate(t, 1, 5, 6, 95);
    }
    if (t <= 7) {
      return 95;
    }
    if (t < 7.1) {
      return interpolate(t, 7, 95, 7.1, 5);
    }
    return 5;
  }

  /**
   * RP with ACN: 400 bar at the start, viscosity maximum at 3 min, lowest during the wash (6-7
   * min), back to 400 bar at 7.5 min.
   */
  private static double pressure(double t) {
    if (t < 0.5) {
      return 400;
    }
    if (t < 3) {
      return interpolate(t, 0.5, 400, 3, 450);
    }
    if (t < 6) {
      return interpolate(t, 3, 450, 6, 250);
    }
    if (t <= 7) {
      return 250;
    }
    if (t < 7.5) {
      return interpolate(t, 7, 250, 7.5, 400);
    }
    return 400;
  }

  @Test
  void solventWashAndReequilibration() {
    final GradientEnd end = PumpTraces.solventEnd(trace(9, PumpTracesTest::gradient));
    // plateau starts at 98% of the amplitude
    Assertions.assertEquals(5.9, end.washStart(), 0.02);
    Assertions.assertEquals(7.0, end.reequilibration(), 0.02);
  }

  @Test
  void solventRunEndingInTheWashHasNoReequilibration() {
    final GradientEnd end = PumpTraces.solventEnd(trace(6.5, PumpTracesTest::gradient));
    Assertions.assertEquals(5.9, end.washStart(), 0.02);
    Assertions.assertFalse(end.hasReequilibration());
  }

  @Test
  void isocraticSolventHasNoGradientEnd() {
    Assertions.assertEquals(GradientEnd.NONE, PumpTraces.solventEnd(trace(9, t -> 50)));
    Assertions.assertEquals(GradientEnd.NONE, PumpTraces.solventEnd(null));
  }

  @Test
  void pressureWashAndReequilibration() {
    final GradientEnd end = PumpTraces.pressureEnd(trace(9, PumpTracesTest::pressure), Double.NaN);
    // plateau is within 10% of the pressure range around the wash minimum
    Assertions.assertEquals(5.7, end.washStart(), 0.03);
    Assertions.assertEquals(7.07, end.reequilibration(), 0.03);
  }

  @Test
  void pressureWithoutReturnHasNoReequilibration() {
    final GradientEnd end = PumpTraces.pressureEnd(trace(9, t -> t <= 7 ? pressure(t) : 250),
        Double.NaN);
    Assertions.assertEquals(GradientEnd.NONE, end);
  }

  @Test
  void pressureIsOnlySearchedAfterTheSolventResult() {
    // the wash extreme at 6-7 min is before the lower bound, the trace stays flat afterwards
    final GradientEnd end = PumpTraces.pressureEnd(trace(9, PumpTracesTest::pressure), 7.8);
    Assertions.assertEquals(GradientEnd.NONE, end);
  }
}
