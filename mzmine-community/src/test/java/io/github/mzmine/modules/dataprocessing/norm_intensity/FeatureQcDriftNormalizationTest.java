/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class FeatureQcDriftNormalizationTest {

  private static final FeatureQcSettings FIXED = new FeatureQcSettings(false, .75d, 1, 2,
      5, 10d, .5d, 0);

  @Test
  void fixedSmoothingPreservesEachBatchQcMeanAndIgnoresStudyAbundances() {
    final List<FeatureQcDriftNormalization.Sample> samples = new ArrayList<>();
    final Map<String, Double> values = new LinkedHashMap<>();
    for (int index = 0; index < 5; index++) {
      samples.add(new FeatureQcDriftNormalization.Sample("a" + index, "A", index, true));
      samples.add(new FeatureQcDriftNormalization.Sample("b" + index, "B", index, true));
      values.put("a" + index, 10d);
      values.put("b" + index, 20d);
    }
    samples.add(new FeatureQcDriftNormalization.Sample("study", "A", 5, false));
    values.put("study", 1_000_000d);

    final FeatureQcResult result = FeatureQcDriftNormalization.calculate(samples, Map.of(1, values), FIXED);

    for (final double factor : result.rowFactors().get(1).values()) assertEquals(1d, factor, 1e-12);
    final var batches = result.diagnostics().rows().get(1).batches();
    assertEquals(10d, batches.get("A").correctionTarget());
    assertEquals(20d, batches.get("B").correctionTarget());
    assertEquals(1, result.availability().fullyCorrectedRows());
  }

  @Test
  void fixedSmoothingDoesNotRequireResponsesSmallEnoughForSquaredCrossValidationErrors() {
    final Map<String, Double> values = new LinkedHashMap<>();
    for (int index = 0; index < 5; index++) {
      values.put("s" + index, 1e200);
    }
    final var result = FeatureQcDriftNormalization.calculate(samples("A", 5), Map.of(1, values),
        FIXED);

    assertEquals(1, result.availability().fullyCorrectedRows());
    for (final double factor : result.rowFactors().get(1).values()) {
      assertEquals(1d, factor, 1e-12);
    }
    final var batch = result.diagnostics().rows().get(1).batches().get("A");
    assertEquals(1e200, batch.correctionTarget(), 1e188);
    assertNull(batch.leaveOneOutMse());
  }

  @Test
  void fixedQuadraticSmoothingCorrectsRawResponsesToTheArithmeticQcMean() {
    final List<FeatureQcDriftNormalization.Sample> samples = samples("A", 9);
    final Map<String, Double> values = new LinkedHashMap<>();
    double target = 0d;
    for (int i = 0; i < 9; i++) {
      final double value = 100d + i * i;
      values.put("s" + i, value);
      target += value / 9d;
    }
    final var settings = new FeatureQcSettings(false, 1d, 2, 0, 5, 10d, .5d, 0);
    final var result = FeatureQcDriftNormalization.calculate(samples, Map.of(1, values), settings);
    for (final var sample : samples) {
      assertEquals(target, values.get(sample.id()) * result.rowFactors().get(1).get(sample.id()), 1e-8);
    }
    final var batch = result.diagnostics().rows().get(1).batches().get("A");
    assertEquals(2, batch.selectedDegree());
    assertEquals(9, batch.selectedNeighborhoodSize());
    assertEquals(1d, batch.selectedSpan());
    assertNull(batch.leaveOneOutMse());
    assertEquals(0, batch.leaveOneOutPredictionCount());
    assertEquals(0, batch.leaveOneOutFailureCount());
    assertTrue(batch.validationUnavailableReason().contains("not performed"));
  }

  @Test
  void fixedSmoothingUsesInjectionOrderRegardlessOfAcquisitionIntervals() {
    final List<FeatureQcDriftNormalization.Sample> regular = samples("A", 9);
    final List<FeatureQcDriftNormalization.Sample> irregular = new ArrayList<>();
    final Map<String, Double> values = new LinkedHashMap<>();
    for (int i = 0; i < regular.size(); i++) {
      irregular.add(new FeatureQcDriftNormalization.Sample("s" + i, "A", i * i * 30d, true));
      values.put("s" + i, 100d + i * 10d);
    }
    final var a = FeatureQcDriftNormalization.calculate(regular, Map.of(1, values), FIXED);
    final var b = FeatureQcDriftNormalization.calculate(irregular, Map.of(1, values), FIXED);
    assertEquals(a.rowFactors(), b.rowFactors());
    for (final var sample : regular) {
      assertEquals(140d, values.get(sample.id()) * a.rowFactors().get(1).get(sample.id()), 1e-8);
    }
  }

  @Test
  void keepsSupportedBatchWhenAnotherBatchCannotFit() {
    final List<FeatureQcDriftNormalization.Sample> samples = new ArrayList<>();
    final Map<String, Double> values = new LinkedHashMap<>();
    for (int index = 0; index < 5; index++) {
      samples.add(new FeatureQcDriftNormalization.Sample("a" + index, "A", index, true));
      samples.add(new FeatureQcDriftNormalization.Sample("b" + index, "B", index, true));
      values.put("a" + index, 100d);
    }
    values.put("b0", 100d);
    values.put("b1", 100d);
    final var result = FeatureQcDriftNormalization.calculate(samples, Map.of(1, values), FIXED);

    assertEquals(5, result.rowFactors().get(1).size());
    assertEquals(FeatureQcUnavailableReason.BATCH_INSUFFICIENT_USABLE_QCS,
        result.unavailableReason(1, "b0"));
    final var batch = result.diagnostics().rows().get(1).batches().get("B");
    assertEquals(1d, batch.usableQcCoordinateMinimum());
    assertEquals(2d, batch.usableQcCoordinateMaximum());
    assertEquals(1, result.availability().partiallyCorrectedRows());
  }

  @Test
  void allowsExactlyHalfMissingAndRejectsMoreThanHalfWithinABatch() {
    final Map<String, Double> halfValues = new LinkedHashMap<>();
    for (int i = 0; i < 5; i++) halfValues.put("s" + i, 10d);
    final var half = FeatureQcDriftNormalization.calculate(samples("A", 10), Map.of(1, halfValues), FIXED);
    assertFalse(half.unavailableRows().containsKey(1));
    assertEquals(1, half.availability().fullyCorrectedRows());
    halfValues.remove("s4");
    final var global = FeatureQcDriftNormalization.calculate(samples("A", 10), Map.of(1, halfValues), FIXED);
    assertEquals(FeatureQcUnavailableReason.GLOBAL_MISSING_QC_THRESHOLD,
        global.unavailableRows().get(1));

    final List<FeatureQcDriftNormalization.Sample> samples = new ArrayList<>();
    final Map<String, Double> values = new LinkedHashMap<>();
    for (int i = 0; i < 12; i++) {
      samples.add(new FeatureQcDriftNormalization.Sample("a" + i, "A", i, true));
      samples.add(new FeatureQcDriftNormalization.Sample("b" + i, "B", i, true));
      values.put("a" + i, 10d);
      if (i < 5) values.put("b" + i, 10d);
    }
    final var result = FeatureQcDriftNormalization.calculate(samples, Map.of(1, values), FIXED);
    assertEquals(FeatureQcUnavailableReason.BATCH_MISSING_QC_THRESHOLD,
        result.unavailableReason(1, "b0"));
    assertEquals(1d, result.rowFactors().get(1).get("a0"), 1e-12);
  }

  @Test
  void recordsBoundariesRawFitPointsAndAutomaticSelectionDiagnostics() {
    final List<FeatureQcDriftNormalization.Sample> samples = new ArrayList<>();
    samples.add(new FeatureQcDriftNormalization.Sample("before", "A", -1d, false));
    samples.addAll(samples("A", 6));
    final Map<String, Double> values = new LinkedHashMap<>();
    for (int i = 0; i < 6; i++) values.put("s" + i, 100d);
    final var result = FeatureQcDriftNormalization.calculate(samples, Map.of(1, values), FeatureQcSettings.defaults());
    final var diagnostic = result.diagnostics().rows().get(1).batches().get("A");

    assertTrue(result.boundarySampleIds().get(1).contains("before"));
    assertNull(result.unavailableReason(1, "before"));
    assertEquals(1d, result.rowFactors().get(1).get("before"), 1e-12);
    assertEquals(6, diagnostic.leaveOneOutPredictionCount());
    assertEquals(0d, diagnostic.leaveOneOutMse(), 1e-12);
    assertEquals(0d, diagnostic.beforeQcRsd(), 1e-12);
    assertEquals(0d, diagnostic.afterQcRsd(), 1e-12);
    assertEquals(2d, diagnostic.usableQcCoordinateMinimum());
    assertEquals(7d, diagnostic.usableQcCoordinateMaximum());
    assertEquals(12, diagnostic.fitPoints().size());
    assertEquals(1d, diagnostic.fitPoints().getFirst().coordinate());
    assertNull(diagnostic.fitPoints().getFirst().observedQc());
    assertEquals(6, diagnostic.fitPoints().stream().filter(point -> point.observedQc() != null).count());
    assertEquals(100d, diagnostic.fitPoints().get(1).observedQc());
  }

  @Test
  void rejectsExcessiveFactorsAndDoesNotTreatCancellationAsFitFailure() {
    final Map<String, Double> values = Map.of("s0", 10d, "s1", 20d, "s2", 30d, "s3", 40d, "s4", 50d);
    final var settings = new FeatureQcSettings(false, 1d, 1, 0, 5, 2d, .5d, 0);
    final var result = FeatureQcDriftNormalization.calculate(samples("A", 5), Map.of(1, values), settings);
    assertEquals(FeatureQcUnavailableReason.FACTOR_OUT_OF_BOUNDS, result.unavailableReason(1, "s0"));
    assertThrows(java.util.concurrent.CancellationException.class,
        () -> FeatureQcDriftNormalization.calculate(samples("A", 5), Map.of(1, values), FIXED, () -> true));
  }

  @Test
  void reportsMissingReferencesAndUnavailableInputWithoutFailingTheRequest() {
    final var noQc = FeatureQcDriftNormalization.calculate(
        List.of(new FeatureQcDriftNormalization.Sample("study", "A", 0d, false)),
        Map.of(1, Map.of("study", 10d)), FIXED);
    assertEquals(FeatureQcUnavailableReason.NO_QC_REFERENCES, noQc.unavailableRows().get(1));
    final var noInput = FeatureQcDriftNormalization.calculate(samples("A", 5), Map.of(2, Map.of()),
        new FeatureQcSettings(false, .75d, 1, 2, 5, 10d, 1d, 0));
    assertEquals(FeatureQcUnavailableReason.BATCH_INSUFFICIENT_USABLE_QCS,
        noInput.unavailableReason(2, "s0"));
  }

  private static @NotNull List<FeatureQcDriftNormalization.Sample> samples(
      @NotNull final String batch, final int count) {
    final List<FeatureQcDriftNormalization.Sample> samples = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      samples.add(new FeatureQcDriftNormalization.Sample("s" + i, batch, i, true));
    }
    return samples;
  }
}
