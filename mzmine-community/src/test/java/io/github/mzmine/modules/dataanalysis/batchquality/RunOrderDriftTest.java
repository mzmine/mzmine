package io.github.mzmine.modules.dataanalysis.batchquality;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mzmine.datamodel.FeatureStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class RunOrderDriftTest {

  @Test
  void summarizesRisingFallingFlatAndNonlinearTrends() {
    final var rising = RunOrderDrift.summarize(List.of(point("c", 3, 30), point("a", 1, 10), point("b", 2, 20)));
    assertEquals(RunOrderDrift.Status.AVAILABLE, rising.status());
    assertEquals(10d, rising.slope(), 1e-12);
    assertEquals(0d, rising.intercept(), 1e-12);
    assertEquals(1d, rising.rSquared(), 1e-12);
    assertEquals(1d, rising.changeFraction(), 1e-12); // (30 - 10) / mean(10, 20, 30)
    assertEquals(1d, rising.firstOrder());
    assertEquals(3d, rising.lastOrder());

    final var falling = RunOrderDrift.summarize(List.of(point("a", 1, 30), point("b", 2, 20), point("c", 3, 10)));
    assertEquals(-10d, falling.slope(), 1e-12);
    assertEquals(-1d, falling.changeFraction(), 1e-12);

    final var flat = RunOrderDrift.summarize(List.of(point("a", 1, 5), point("b", 2, 5), point("c", 3, 5)));
    assertEquals(0d, flat.slope(), 1e-12);
    assertNull(flat.rSquared());
    assertEquals(0d, flat.changeFraction(), 1e-12);

    final var nonlinear = RunOrderDrift.summarize(List.of(point("a", 1, 1), point("b", 2, 4), point("c", 3, 9)));
    assertEquals(4d, nonlinear.slope(), 1e-12);
    assertEquals(48d / 49d, nonlinear.rSquared(), 1e-12);

    final var large = RunOrderDrift.summarize(List.of(point("a", 1e150, 1e150),
        point("b", 2e150, 2e150), point("c", 3e150, 3e150)));
    assertEquals(RunOrderDrift.Status.AVAILABLE, large.status());
    assertEquals(1d, large.slope(), 1e-12);
    assertEquals(1d, large.rSquared(), 1e-12);
  }

  @Test
  void excludesGapFilledAndUnavailableMeasurementsButRetainsZero() {
    final var result = RunOrderDrift.summarize(List.of(point("a", 1, 0), point("b", 2, 10), point("c", 3, 20),
        new RunOrderDrift.Observation("estimated", 4, 40d, FeatureStatus.ESTIMATED),
        new RunOrderDrift.Observation("aggregated", 5, 50d, FeatureStatus.COMPOUND_AGGREGATED),
        new RunOrderDrift.Observation("missing", 6, null, FeatureStatus.DETECTED),
        new RunOrderDrift.Observation("nan", 7, Double.NaN, FeatureStatus.DETECTED),
        new RunOrderDrift.Observation("negative", 8, -1d, FeatureStatus.MANUAL)));
    assertEquals(RunOrderDrift.Status.AVAILABLE, result.status());
    assertEquals(3, result.usedPointCount());
    assertEquals(new RunOrderDrift.Exclusions(1, 1, 1, 1, 1), result.exclusions());
    assertEquals(10d, result.slope(), 1e-12);
  }

  @Test
  void reportsInsufficientAndInvalidOrderWithoutInventingRunOrder() {
    final var sparse = RunOrderDrift.summarize(List.of(point("a", 1, 10), point("b", 2, 20),
        new RunOrderDrift.Observation("estimated", 3, 30d, FeatureStatus.ESTIMATED)));
    assertEquals(RunOrderDrift.Status.INSUFFICIENT_USABLE_POINTS, sparse.status());
    assertEquals(1, sparse.exclusions().estimated());

    final var duplicate = RunOrderDrift.summarize(List.of(point("a", 1, 10), point("b", 1, 20), point("c", 3, 30)));
    assertEquals(RunOrderDrift.Status.INVALID_ORDER, duplicate.status());
    final var nonfinite = RunOrderDrift.summarize(List.of(point("a", 1, 10), point("b", Double.NaN, 20), point("c", 3, 30)));
    assertEquals(RunOrderDrift.Status.INVALID_ORDER, nonfinite.status());
  }

  private static RunOrderDrift.Observation point(final String sampleId, final double order, final double abundance) {
    return new RunOrderDrift.Observation(sampleId, order, abundance, FeatureStatus.DETECTED);
  }
}
