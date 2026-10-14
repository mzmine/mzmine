/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FeatureQcDriftNormalizationTest {

  @Test
  void usesRemainingUsableQcsAndHonorsCancellation() {
    final var samples = new java.util.ArrayList<>(samples());
    samples.add(new FeatureQcDriftNormalization.Sample("missing", "A", 2.5d, true));
    final Map<Integer, Map<String, Double>> values = Map.of(1,
        Map.of("s1", 100d, "s2", 100d, "s3", 100d, "s4", 100d, "s5", 100d));
    final var result = FeatureQcDriftNormalization.calculate(samples, values, 5, .75, 2, 10, false);
    assertTrue(result.unavailableRows().isEmpty());
    assertEquals(1d, result.rowFactors().get(1).get("missing"), 1e-10);
    assertTrue(result.boundarySampleIds().isEmpty());
    assertThrows(java.util.concurrent.CancellationException.class, () ->
        FeatureQcDriftNormalization.calculate(samples, values, 5, .75, 2, 10, false, () -> true));
  }

  @Test
  void fitsKnownLogDriftAndKeepsFlatFeatureNearOne() {
    final List<FeatureQcDriftNormalization.Sample> samples = samples();
    final Map<String, Double> drift = Map.of("s1", 100d, "s2", 90d, "s3", 80d, "s4", 70d,
        "s5", 60d);
    final Map<String, Double> flat = Map.of("s1", 100d, "s2", 100d, "s3", 100d, "s4", 100d,
        "s5", 100d);

    final var result = FeatureQcDriftNormalization.calculate(samples, Map.of(1, drift, 2, flat),
        5, .75d, 2, 10d, false);

    assertFalse(result.unavailableRows().containsKey(1));
    assertEquals(1d, result.rowFactors().get(2).get("s3"), 1e-10);
    // Target is the median QC response (80); a lower final response needs an upward factor.
    assertTrue(result.rowFactors().get(1).get("s5") > 1d);
    assertTrue(result.rowFactors().get(1).get("s1") < 1d);
  }

  @Test
  void leavesRowsUnavailableForMissingQcAndOutOfRangeFactors() {
    final Map<String, Double> missing = Map.of("s1", 100d, "s2", 90d, "s3", 80d, "s4", 70d);
    final Map<String, Double> extreme = Map.of("s1", 100d, "s2", 90d, "s3", 80d, "s4", 70d,
        "s5", 1d);

    final var result = FeatureQcDriftNormalization.calculate(samples(), Map.of(1, missing, 2, extreme),
        5, .75d, 2, 2d, false);

    assertEquals(2, result.unavailableRows().size());
    assertTrue(result.rowFactors().isEmpty());
  }

  @Test
  void flagsNearestBoundaryAndHandlesIndependentBatches() {
    final List<FeatureQcDriftNormalization.Sample> samples = List.of(
        new FeatureQcDriftNormalization.Sample("before", "A", -1d, false),
        new FeatureQcDriftNormalization.Sample("a1", "A", 0d, true),
        new FeatureQcDriftNormalization.Sample("a2", "A", 1d, true),
        new FeatureQcDriftNormalization.Sample("a3", "A", 2d, true),
        new FeatureQcDriftNormalization.Sample("a4", "A", 3d, true),
        new FeatureQcDriftNormalization.Sample("a5", "A", 4d, true),
        new FeatureQcDriftNormalization.Sample("b1", "B", 0d, true),
        new FeatureQcDriftNormalization.Sample("b2", "B", 1d, true),
        new FeatureQcDriftNormalization.Sample("b3", "B", 2d, true),
        new FeatureQcDriftNormalization.Sample("b4", "B", 3d, true),
        new FeatureQcDriftNormalization.Sample("b5", "B", 4d, true));
    final Map<String, Double> values = Map.of("a1", 10d, "a2", 10d, "a3", 10d, "a4", 10d,
        "a5", 10d, "b1", 20d, "b2", 20d, "b3", 20d, "b4", 20d, "b5", 20d);

    final var result = FeatureQcDriftNormalization.calculate(samples, Map.of(1, values), 5, .75d, 2,
        10d, true);

    assertTrue(result.boundarySampleIds().get(1).contains("before"));
    assertEquals(1.5d, result.rowFactors().get(1).get("a1"), 1e-10);
    assertEquals(.75d, result.rowFactors().get(1).get("b1"), 1e-10);
  }

  private static List<FeatureQcDriftNormalization.Sample> samples() {
    return List.of(new FeatureQcDriftNormalization.Sample("s1", "A", 0d, true),
        new FeatureQcDriftNormalization.Sample("s2", "A", 1d, true),
        new FeatureQcDriftNormalization.Sample("s3", "A", 2d, true),
        new FeatureQcDriftNormalization.Sample("s4", "A", 3d, true),
        new FeatureQcDriftNormalization.Sample("s5", "A", 4d, true));
  }
}
