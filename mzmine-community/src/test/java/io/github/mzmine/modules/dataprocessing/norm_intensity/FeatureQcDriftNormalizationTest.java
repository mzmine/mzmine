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
import org.junit.jupiter.api.Test;

class FeatureQcDriftNormalizationTest {

  private static final FeatureQcSettings SETTINGS = new FeatureQcSettings(.75d, 2, 5, 10d, .5d);

  @Test
  void restoresCommonGlobalLogTargetAcrossBatches() {
    final List<FeatureQcDriftNormalization.Sample> samples = new ArrayList<>();
    final Map<String, Double> values = new LinkedHashMap<>();
    for (int index = 0; index < 5; index++) {
      samples.add(new FeatureQcDriftNormalization.Sample("a" + index, "A", index, true));
      samples.add(new FeatureQcDriftNormalization.Sample("b" + index, "B", index, true));
      values.put("a" + index, 10d);
      values.put("b" + index, 20d);
    }

    final FeatureQcResult result = FeatureQcDriftNormalization.calculate(samples, Map.of(1, values),
        SETTINGS);

    assertEquals(Math.log(Math.sqrt(200d)), result.globalLogTargets().get(1), 1e-12);
    assertEquals(Math.sqrt(2d), result.rowFactors().get(1).get("a0"), 1e-12);
    assertEquals(1d / Math.sqrt(2d), result.rowFactors().get(1).get("b0"), 1e-12);
    assertEquals(1, result.availability().fullyCorrectedRows());
  }

  @Test
  void usesMedianOfLogsRatherThanLogOfArithmeticRawMedian() {
    final Map<String, Double> values = new LinkedHashMap<>();
    for (int index = 0; index < 6; index++) values.put("s" + index, index < 3 ? 1d : 9d);
    final List<FeatureQcDriftNormalization.Sample> samples = samples("A", 6);

    final FeatureQcResult result = FeatureQcDriftNormalization.calculate(samples, Map.of(7, values),
        new FeatureQcSettings(1d, 0, 5, 100d, .5d));

    assertEquals(Math.log(3d), result.globalLogTargets().get(7), 1e-12);
    assertTrue(Math.abs(Math.exp(result.globalLogTargets().get(7)) - 5d) > 1d);
  }

  @Test
  void keepsSupportedBatchWhenAnotherBatchCannotFit() {
    final List<FeatureQcDriftNormalization.Sample> samples = new ArrayList<>();
    final Map<String, Double> values = new LinkedHashMap<>();
    // Use distinct IDs for the second batch and intentionally leave three QC responses absent.
    for (int index = 0; index < 5; index++) {
      samples.add(new FeatureQcDriftNormalization.Sample("a" + index, "A", index, true));
      samples.add(new FeatureQcDriftNormalization.Sample("b" + index, "B", index, true));
      values.put("a" + index, 100d);
    }
    values.put("b0", 100d);
    values.put("b1", 100d);

    final FeatureQcResult result = FeatureQcDriftNormalization.calculate(samples, Map.of(1, values),
        SETTINGS);

    assertEquals(5, result.rowFactors().get(1).size());
    assertEquals(FeatureQcUnavailableReason.BATCH_INSUFFICIENT_USABLE_QCS,
        result.unavailableReason(1, "b0"));
    assertEquals(1d, result.rowFactors().get(1).get("a0"), 1e-12);
    final FeatureQcBatchDiagnostic batchB = result.diagnostics().rows().get(1).batches().get("B");
    assertEquals(0d, batchB.usableQcCoordinateMinimum(), 1e-12);
    assertEquals(1d, batchB.usableQcCoordinateMaximum(), 1e-12);
    assertEquals(1, result.availability().partiallyCorrectedRows());
  }

  @Test
  void appliesGlobalAndBatchMissingInequalitiesExactly() {
    final List<FeatureQcDriftNormalization.Sample> globalSamples = samples("A", 10);
    final Map<String, Double> globalValues = new LinkedHashMap<>();
    for (int index = 0; index < 5; index++) globalValues.put("s" + index, 10d);
    final FeatureQcResult global = FeatureQcDriftNormalization.calculate(globalSamples,
        Map.of(1, globalValues), SETTINGS);
    assertFalse(global.unavailableRows().containsKey(1)); // 50% is not greater than the threshold.
    assertEquals(1, global.availability().uncorrectedRows());

    final List<FeatureQcDriftNormalization.Sample> batchSamples = new ArrayList<>();
    final Map<String, Double> batchValues = new LinkedHashMap<>();
    for (int index = 0; index < 10; index++) {
      batchSamples.add(new FeatureQcDriftNormalization.Sample("a" + index, "A", index, true));
      batchSamples.add(new FeatureQcDriftNormalization.Sample("b" + index, "B", index, true));
      batchValues.put("a" + index, 10d);
      if (index < 5) batchValues.put("b" + index, 10d);
    }
    final FeatureQcResult batch = FeatureQcDriftNormalization.calculate(batchSamples,
        Map.of(2, batchValues), SETTINGS);
    assertEquals(FeatureQcUnavailableReason.BATCH_MISSING_QC_THRESHOLD,
        batch.unavailableReason(2, "b0"));
    assertEquals(1d, batch.rowFactors().get(2).get("a0"), 1e-12);
  }

  @Test
  void recordsBoundaryMissingInputAndDeterministicDiagnostics() {
    final List<FeatureQcDriftNormalization.Sample> samples = new ArrayList<>();
    samples.add(new FeatureQcDriftNormalization.Sample("before", "A", -1d, false));
    samples.addAll(samples("A", 6));
    final Map<String, Double> values = new LinkedHashMap<>();
    for (int index = 0; index < 6; index++) values.put("s" + index, 100d);

    final FeatureQcResult result = FeatureQcDriftNormalization.calculate(samples, Map.of(1, values),
        new FeatureQcSettings(1d, 0, 5, 10d, .5d));
    final FeatureQcBatchDiagnostic diagnostic = result.diagnostics().rows().get(1).batches().get("A");

    assertTrue(result.boundarySampleIds().get(1).contains("before"));
    assertNull(result.unavailableReason(1, "before"));
    assertEquals(1d, result.rowFactors().get(1).get("before"), 1e-12);
    assertEquals(6, diagnostic.leaveOneOutPredictionCount());
    assertEquals(0d, diagnostic.leaveOneOutLogMse(), 1e-12);
    assertEquals(0d, diagnostic.beforeQcRsd(), 1e-12);
    assertEquals(0d, diagnostic.afterQcRsd(), 1e-12);
    assertEquals(0d, diagnostic.usableQcCoordinateMinimum(), 1e-12);
    assertEquals(5d, diagnostic.usableQcCoordinateMaximum(), 1e-12);
    assertEquals(FeatureQcSettings.METHOD_VERSION, result.diagnostics().methodVersion());
    assertEquals(12, diagnostic.fitPoints().size());
    assertEquals(-1d, diagnostic.fitPoints().getFirst().time(), 1e-12);
    assertNull(diagnostic.fitPoints().getFirst().observedLogQc());
    assertEquals(6, diagnostic.fitPoints().stream()
        .filter(point -> point.observedLogQc() != null).count());
  }

  @Test
  void rejectsFactorsForTheirBatchAndCancellationIsNotAFitFailure() {
    final List<FeatureQcDriftNormalization.Sample> samples = samples("A", 5);
    final Map<String, Double> values = Map.of("s0", 100d, "s1", 90d, "s2", 80d, "s3", 70d,
        "s4", 1d);
    final FeatureQcResult result = FeatureQcDriftNormalization.calculate(samples, Map.of(1, values),
        new FeatureQcSettings(.75d, 2, 5, 2d, .5d));
    assertEquals(FeatureQcUnavailableReason.FACTOR_OUT_OF_BOUNDS, result.unavailableReason(1, "s0"));
    assertThrows(java.util.concurrent.CancellationException.class,
        () -> FeatureQcDriftNormalization.calculate(samples, Map.of(1, values), SETTINGS, () -> true));
  }

  @Test
  void reportsNoQcReferencesAndNoPositiveInputWithoutTreatingEitherAsARequestFailure() {
    final List<FeatureQcDriftNormalization.Sample> noQcs = List.of(
        new FeatureQcDriftNormalization.Sample("study", "A", 0d, false));
    final FeatureQcResult noQcResult = FeatureQcDriftNormalization.calculate(noQcs,
        Map.of(1, Map.of("study", 10d)), SETTINGS);
    assertEquals(FeatureQcUnavailableReason.NO_QC_REFERENCES, noQcResult.unavailableRows().get(1));

    final FeatureQcResult noInputResult = FeatureQcDriftNormalization.calculate(samples("A", 5),
        Map.of(2, Map.of()), new FeatureQcSettings(.75d, 2, 5, 10d, 1d));
    assertEquals(FeatureQcUnavailableReason.NO_POSITIVE_FINITE_INPUT,
        noInputResult.unavailableRows().get(2));
  }

  @Test
  void reportsUnavailableLeaveOneOutInsteadOfMisleadingZeroScore() {
    final Map<String, Double> values = Map.of("s0", 100d, "s1", 100d, "s2", 100d, "s3", 100d,
        "s4", 100d);
    final FeatureQcBatchDiagnostic batch = FeatureQcDriftNormalization.calculate(samples("A", 5),
        Map.of(1, values), SETTINGS).diagnostics().rows().get(1).batches().get("A");
    assertNull(batch.leaveOneOutLogMse());
    assertEquals(0, batch.leaveOneOutPredictionCount());
    assertEquals(5, batch.leaveOneOutFailureCount());
  }

  private static List<FeatureQcDriftNormalization.Sample> samples(final String batch, final int count) {
    final List<FeatureQcDriftNormalization.Sample> samples = new ArrayList<>();
    for (int index = 0; index < count; index++) {
      samples.add(new FeatureQcDriftNormalization.Sample("s" + index, batch, index, true));
    }
    return samples;
  }
}
