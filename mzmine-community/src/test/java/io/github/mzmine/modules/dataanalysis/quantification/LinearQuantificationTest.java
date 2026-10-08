package io.github.mzmine.modules.dataanalysis.quantification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

class LinearQuantificationTest {

  @Test
  void externalCalibrationBackCalculatesAndFlagsOutsideRange() {
    final var fit = LinearQuantification.fit(LinearQuantification.Mode.EXTERNAL,
        LinearQuantification.Weighting.UNWEIGHTED, LinearQuantification.Intercept.FREE,
        List.of(point("s1", 1, null, 12, null), point("s2", 2, null, 22, null), point("s3", 3, null, 32, null)));
    assertEquals(LinearQuantification.Status.AVAILABLE, fit.status());
    assertEquals(10d, fit.slope(), 1e-12);
    assertEquals(2d, fit.intercept(), 1e-12);
    assertEquals(2d, fit.estimate(22d, null, null, null).concentration(), 1e-12);
    assertEquals("outside_calibrated_range", fit.estimate(42d, null, null, null).rangeFlag());
    assertEquals(0d, fit.backCalculations().getFirst().residual(), 1e-12);
  }

  @Test
  void internalStandardUsesActualUnknownAmountAndNotCalibratorAmount() {
    final var fit = LinearQuantification.fit(LinearQuantification.Mode.INTERNAL_STANDARD,
        LinearQuantification.Weighting.UNWEIGHTED, LinearQuantification.Intercept.FIXED_ZERO,
        List.of(point("s1", 1, 10d, 20, 100d), point("s2", 4, 20d, 40, 100d), point("s3", 9, 30d, 60, 100d)));
    assertEquals(LinearQuantification.Status.AVAILABLE, fit.status());
    // y = analyte response / IS response = 2 * (analyte concentration / IS concentration)
    assertEquals(2d, fit.slope(), 1e-12);
    assertEquals(4d, fit.estimate(40d, 100d, 20d, null).concentration(), 1e-12);
    assertNull(fit.estimate(40d, 100d, null, null).concentration());
  }

  @Test
  void weightedAndSurrogateMethodsKeepInvalidInputsOutOfResults() {
    final var weighted = LinearQuantification.fit(LinearQuantification.Mode.EXTERNAL,
        LinearQuantification.Weighting.ONE_OVER_X, LinearQuantification.Intercept.FIXED_ZERO,
        List.of(point("zero", 0, null, 1, null), point("one", 1, null, 2, null), point("two", 2, null, 4, null)));
    assertEquals(1, weighted.rejectedPointCount());
    assertEquals(2d, weighted.slope(), 1e-12);
    final var surrogate = LinearQuantification.fit(LinearQuantification.Mode.SURROGATE_REFERENCE_EQUIVALENT,
        LinearQuantification.Weighting.UNWEIGHTED, LinearQuantification.Intercept.FIXED_ZERO,
        List.of(point("one", 1, null, 10, null), point("two", 2, null, 20, null)));
    assertEquals(1d, surrogate.estimate(10d, null, null, 1d).concentration(), 1e-12);
    assertEquals(0.5d, surrogate.estimate(10d, null, null, 2d).concentration(), 1e-12);
    assertFalse(surrogate.estimate(10d, null, null, null).rangeFlag().equals("within_calibrated_range"));
  }

  @Test
  void rejectsDegenerateAndNonfiniteCalibrationDataWithoutInventingValues() {
    final var degenerate = LinearQuantification.fit(LinearQuantification.Mode.EXTERNAL,
        LinearQuantification.Weighting.UNWEIGHTED, LinearQuantification.Intercept.FREE,
        List.of(point("a", 1, null, 10, null), point("b", 1, null, 12, null)));
    assertEquals(LinearQuantification.Status.INSUFFICIENT_POINTS, degenerate.status());
    final var nonfinite = LinearQuantification.fit(LinearQuantification.Mode.EXTERNAL,
        LinearQuantification.Weighting.UNWEIGHTED, LinearQuantification.Intercept.FREE,
        List.of(point("a", 1, null, Double.NaN, null), point("b", 2, null, 20, null)));
    assertEquals(LinearQuantification.Status.INSUFFICIENT_POINTS, nonfinite.status());
    assertNull(nonfinite.estimate(20d, null, null, null).concentration());
    final var negative = LinearQuantification.fit(LinearQuantification.Mode.EXTERNAL,
        LinearQuantification.Weighting.UNWEIGHTED, LinearQuantification.Intercept.FREE,
        List.of(point("a", 1, null, 20, null), point("b", 2, null, 10, null)));
    assertEquals(LinearQuantification.Status.DEGENERATE, negative.status());
    final var onePointFixedZero = LinearQuantification.fit(LinearQuantification.Mode.EXTERNAL,
        LinearQuantification.Weighting.UNWEIGHTED, LinearQuantification.Intercept.FIXED_ZERO,
        List.of(point("one", 1, null, 10, null)));
    assertEquals(LinearQuantification.Status.INSUFFICIENT_POINTS, onePointFixedZero.status());
    final var duplicateLevelsFixedZero = LinearQuantification.fit(LinearQuantification.Mode.EXTERNAL,
        LinearQuantification.Weighting.UNWEIGHTED, LinearQuantification.Intercept.FIXED_ZERO,
        List.of(point("a", 1, null, 10, null), point("b", 1, null, 10, null)));
    assertEquals(LinearQuantification.Status.INSUFFICIENT_POINTS, duplicateLevelsFixedZero.status());
  }

  @Test
  void weightedFitsUseTheDeclaredWeightsForNoisyCalibrationData() {
    final var points = List.of(point("a", 1, null, 2, null), point("b", 2, null, 5, null), point("c", 4, null, 7, null));
    final var unweighted = LinearQuantification.fit(LinearQuantification.Mode.EXTERNAL, LinearQuantification.Weighting.UNWEIGHTED,
        LinearQuantification.Intercept.FREE, points);
    assertEquals(1d, unweighted.intercept(), 1e-12); assertEquals(11d / 7d, unweighted.slope(), 1e-12);
    final var oneOverX = LinearQuantification.fit(LinearQuantification.Mode.EXTERNAL, LinearQuantification.Weighting.ONE_OVER_X,
        LinearQuantification.Intercept.FREE, points);
    assertEquals(7d / 13d, oneOverX.intercept(), 1e-12); assertEquals(23d / 13d, oneOverX.slope(), 1e-12);
    final var oneOverXSquared = LinearQuantification.fit(LinearQuantification.Mode.EXTERNAL, LinearQuantification.Weighting.ONE_OVER_X_SQUARED,
        LinearQuantification.Intercept.FREE, points);
    assertEquals(1d / 7d, oneOverXSquared.intercept(), 1e-12); assertEquals(2d, oneOverXSquared.slope(), 1e-12);
  }

  private static LinearQuantification.CalibrationPoint point(final String sample, final double concentration,
      final Double internalStandardConcentration, final double analyteResponse, final Double internalResponse) {
    return new LinearQuantification.CalibrationPoint(sample, concentration, internalStandardConcentration,
        analyteResponse, internalResponse);
  }
}
