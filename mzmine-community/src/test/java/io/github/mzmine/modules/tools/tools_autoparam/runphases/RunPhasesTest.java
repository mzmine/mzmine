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

class RunPhasesTest {

  private static final double NaN = Double.NaN;
  private static final double DELTA = 1e-6;

  private static @NotNull RunPhases phases(double flowOn, double flowOff, double voidTime,
      double reequilibration, double earlySaltEnd, double lateSaltStart,
      @NotNull GradientEnd solvent, @NotNull GradientEnd pressure) {
    return new RunPhases(flowOn, flowOff, voidTime, NaN, reequilibration, earlySaltEnd,
        lateSaltStart, solvent, pressure);
  }

  @Test
  void startIsLatestOfFlowOnVoidAndSaltPlug() {
    final RunPhases p = phases(0.5, 16, 1.0, NaN, 0.3, NaN, GradientEnd.NONE, GradientEnd.NONE);
    Assertions.assertEquals(1.0, p.effectiveStart(), DELTA);
  }

  @Test
  void startFallsBackToZero() {
    final RunPhases p = phases(NaN, 16, NaN, NaN, NaN, NaN, GradientEnd.NONE, GradientEnd.NONE);
    Assertions.assertEquals(0, p.effectiveStart(), DELTA);
  }

  @Test
  void endWithoutPumpUsesEarlierOfSaltAndGradient() {
    final RunPhases salt = phases(NaN, 7.3, NaN, 7.12, NaN, 7.11, GradientEnd.NONE,
        GradientEnd.NONE);
    Assertions.assertEquals(RunPhaseEndSource.SALT, salt.endSource());
    Assertions.assertEquals(7.11, salt.effectiveEnd(), DELTA);

    final RunPhases gradient = phases(NaN, 10, NaN, 8.0, NaN, NaN, GradientEnd.NONE,
        GradientEnd.NONE);
    Assertions.assertEquals(RunPhaseEndSource.GRADIENT, gradient.endSource());
    Assertions.assertEquals(8.0, gradient.effectiveEnd(), DELTA);
  }

  @Test
  void endFallsBackToFlowOff() {
    final RunPhases p = phases(NaN, 16, NaN, NaN, NaN, NaN, GradientEnd.NONE, GradientEnd.NONE);
    Assertions.assertEquals(RunPhaseEndSource.FLOW, p.endSource());
    Assertions.assertEquals(16, p.effectiveEnd(), DELTA);
  }

  @Test
  void pumpResultIsExtendedWithoutMsConfirmation() {
    final RunPhases pressure = phases(NaN, 15, NaN, NaN, NaN, NaN, GradientEnd.NONE,
        new GradientEnd(NaN, 10));
    Assertions.assertEquals(RunPhaseEndSource.PRESSURE, pressure.endSource());
    Assertions.assertEquals(10.5, pressure.effectiveEnd(), DELTA);

    final RunPhases solvent = phases(NaN, 15, NaN, NaN, NaN, NaN, new GradientEnd(6, 10),
        GradientEnd.NONE);
    Assertions.assertEquals(RunPhaseEndSource.SOLVENT, solvent.endSource());
    Assertions.assertEquals(11, solvent.effectiveEnd(), DELTA);
  }

  @Test
  void pressureTakesPrecedenceOverSolventAsLowerBound() {
    final RunPhases p = phases(NaN, 15, NaN, NaN, NaN, NaN, new GradientEnd(6, 6.6),
        new GradientEnd(NaN, 6.8));
    Assertions.assertEquals(6.8, p.pumpLowerBound(), DELTA);
  }

  @Test
  void extendedEndIsCappedAtFlowOff() {
    final RunPhases p = phases(NaN, 10.5, NaN, NaN, NaN, NaN, new GradientEnd(6, 10),
        GradientEnd.NONE);
    Assertions.assertEquals(10.5, p.effectiveEnd(), DELTA);
  }

  @Test
  void msSideEndIsNeverBeforeThePumpBound() {
    final RunPhases p = phases(NaN, 7.3, NaN, NaN, NaN, 6.5, GradientEnd.NONE,
        new GradientEnd(NaN, 6.78));
    Assertions.assertEquals(RunPhaseEndSource.SALT, p.endSource());
    Assertions.assertEquals(6.78, p.effectiveEnd(), DELTA);
  }

  @Test
  void effectiveRangeIsNeverInverted() {
    final RunPhases p = phases(NaN, 0.5, 1.0, NaN, NaN, NaN, GradientEnd.NONE, GradientEnd.NONE);
    Assertions.assertEquals(1.0f, p.effectiveRtRange().lower(), 1e-6f);
    Assertions.assertEquals(1.0f, p.effectiveRtRange().upper(), 1e-6f);
  }
}
