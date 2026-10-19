package io.github.mzmine.modules.tools.batch_resource_estimation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ScalingFitTest {

  @Test
  void fitsPowerLaw() {
    final ScalingFit fit = ScalingFit.fit(new double[]{2, 4, 8},
        new double[]{3 * Math.pow(2, 1.5), 3 * Math.pow(4, 1.5), 3 * Math.pow(8, 1.5)});
    assertEquals(1.5, fit.exponent(), 1e-9);
    assertEquals(3, fit.coefficient(), 1e-9);
  }

  @Test
  void zeroValuesScaleLinearly() {
    final ScalingFit fit = ScalingFit.fit(new double[]{2, 4}, new double[]{0, 0});
    assertEquals(0, fit.predict(100));
  }

  @Test
  void clampsSteepExponent() {
    final ScalingFit fit = ScalingFit.fit(new double[]{1, 2}, new double[]{1, 16});
    assertEquals(ScalingFit.MAX_EXPONENT, fit.exponent());
  }

  @Test
  void lastSegmentPassesThroughLastPoint() {
    // grows linearly first, then levels off
    final ScalingFit fit = ScalingFit.fitLastSegment(new double[]{2, 4, 8},
        new double[]{1, 2, 2.5});
    assertEquals(Math.log(1.25) / Math.log(2), fit.exponent(), 1e-9);
    assertEquals(2.5, fit.predict(8), 1e-9);

    // thread limited steps have the same x (the thread count) once all threads are busy, the
    // prediction is at that x and keeps the last value
    final ScalingFit saturated = ScalingFit.fitLastSegment(new double[]{2, 4, 4},
        new double[]{1, 2, 2.2});
    assertEquals(2.2, saturated.predict(4), 1e-9);
    final ScalingFit allSaturated = ScalingFit.fitLastSegment(new double[]{4, 4, 4},
        new double[]{2, 2.1, 2.2});
    assertEquals(0, allSaturated.exponent());
    assertEquals(2.2, allSaturated.predict(4), 1e-9);
  }

  @Test
  void constantXGivesProportionalFit() {
    // all subsets below the thread count use one thread wave
    final ScalingFit fit = ScalingFit.fit(new double[]{1, 1, 1}, new double[]{2, 2.2, 1.8});
    assertEquals(2, fit.predict(1), 1e-9);
    assertEquals(20, fit.predict(10), 1e-9);
  }
}
