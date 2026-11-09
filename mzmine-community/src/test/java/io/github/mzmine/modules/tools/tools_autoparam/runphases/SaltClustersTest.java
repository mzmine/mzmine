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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SaltClustersTest {

  // 250 scans per minute over 10 min
  private static final double STEP = 0.004;
  private static final int N = 2501;
  private static final double TIC = 7;

  private static double @NotNull [] rt() {
    final double[] rt = new double[N];
    for (int i = 0; i < N; i++) {
      rt[i] = i * STEP;
    }
    return rt;
  }

  private static double @NotNull [] constant(double value) {
    final double[] v = new double[N];
    Arrays.fill(v, value);
    return v;
  }

  private static void set(double @NotNull [] values, double from, double to, double value) {
    for (int i = 0; i < N; i++) {
      if (i * STEP >= from && i * STEP < to) {
        values[i] = value;
      }
    }
  }

  @Test
  void sodiumFormateClusterMzs() {
    Assertions.assertEquals(112.98563, SaltClusters.sodiumFormateMzs(PolarityType.NEGATIVE)[0],
        1e-4);
    Assertions.assertEquals(90.97665, SaltClusters.sodiumFormateMzs(PolarityType.POSITIVE)[0],
        1e-4);
  }

  @Test
  void calibrantPlugAtTheStart() {
    final double[] salt = constant(0);
    final double[] tic = constant(TIC);
    // ~6 scans, like the timsTOF calibrant plug
    set(salt, 0.235, 0.26, 3);
    set(tic, 0.235, 0.26, TIC + 0.3);
    final SaltEvents events = SaltClusters.events(rt(), salt, tic, Double.NaN);
    Assertions.assertEquals(0.26, events.earlyEnd(), 0.01);
    Assertions.assertTrue(Double.isNaN(events.lateStart()));
  }

  @Test
  void saltPeakAtTheEnd() {
    final double[] salt = constant(0);
    final double[] tic = constant(TIC);
    set(salt, 9.0, 9.1, 3);
    set(tic, 9.0, 9.1, TIC + 0.5);
    final SaltEvents events = SaltClusters.events(rt(), salt, tic, Double.NaN);
    Assertions.assertTrue(Double.isNaN(events.earlyEnd()));
    Assertions.assertEquals(9.0, events.lateStart(), 0.01);
  }

  @Test
  void lateEventsAreOnlySearchedAfterTheLowerBound() {
    final double[] salt = constant(0);
    final double[] tic = constant(TIC);
    set(salt, 9.0, 9.1, 3);
    set(tic, 9.0, 9.1, TIC + 0.5);
    Assertions.assertTrue(Double.isNaN(SaltClusters.events(rt(), salt, tic, 9.5).lateStart()));
  }

  @Test
  void broadSaltPlateauCutsNothing() {
    final double[] salt = constant(0);
    final double[] tic = constant(TIC);
    // clusters forming during a wash
    set(salt, 8.0, 9.5, 2);
    set(tic, 8.0, 9.5, TIC + 0.3);
    Assertions.assertTrue(
        Double.isNaN(SaltClusters.events(rt(), salt, tic, Double.NaN).lateStart()));
  }

  @Test
  void clusterFlickerWithoutTicRiseCutsNothing() {
    final double[] salt = constant(0);
    set(salt, 9.0, 9.024, 3);
    Assertions.assertTrue(
        Double.isNaN(SaltClusters.events(rt(), salt, constant(TIC), Double.NaN).lateStart()));
  }
}
