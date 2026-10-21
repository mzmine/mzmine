package io.github.mzmine.modules.dataprocessing.norm_intensity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Independent R reference: exact scale semantics and separately measured solver differences. */
class FeatureQcReferenceFixtureTest {

  private static final String ROOT = "/io/github/mzmine/modules/dataprocessing/norm_intensity/feature_qc/";

  @Test
  void affineFixtureChecksEveryFactor() throws IOException {
    final List<Entry> entries = fixture("affine_log");
    final FeatureQcResult result = calculate(entries, 1d);
    assertEquals(11, entries.size());
    for (final Entry entry : entries) {
      assertEquals(entry.target(), result.globalLogTargets().get(1), 1e-12);
      assertEquals(entry.factor(), result.rowFactors().get(1).get(entry.id()), 1e-10);
    }
  }

  @Test
  void nonlinearFixtureQuantifiesSolverDifferenceWithoutClaimingEquivalentCurves() throws IOException {
    final List<Entry> entries = fixture("nonlinear_log");
    final FeatureQcResult result = calculate(entries, .75d);
    double squaredDifference = 0d;
    double maximumDifference = 0d;
    for (final Entry entry : entries) {
      assertEquals(entry.target(), result.globalLogTargets().get(1), 1e-12);
      final double nativeFit = entry.target() - Math.log(result.rowFactors().get(1).get(entry.id()));
      final double difference = nativeFit - entry.fit();
      assertTrue(Double.isFinite(difference));
      squaredDifference += difference * difference;
      maximumDifference = Math.max(maximumDifference, Math.abs(difference));
    }
    final double rmse = Math.sqrt(squaredDifference / entries.size());
    System.out.printf("R/Commons nonlinear log-fit comparison: n=%d maxAbs=%.12g RMSE=%.12g%n",
        entries.size(), maximumDifference, rmse);
    // The nonlinear fixture deliberately exposes the distinct neighborhood/prediction algorithms.
    // This is a measured comparison, not an acceptance tolerance for scientific fit quality.
    assertTrue(maximumDifference > 1e-8);
  }

  @Test
  void multiBatchFixtureChecksEveryFactorAgainstOneGlobalTarget() throws IOException {
    final List<FeatureQcDriftNormalization.Sample> samples = new ArrayList<>();
    final Map<String, Double> values = new LinkedHashMap<>();
    for (int time = 0; time <= 8; time++) {
      for (final String batch : List.of("A", "B")) {
        final String id = batch + time;
        samples.add(new FeatureQcDriftNormalization.Sample(id, batch, time, time % 2 == 0));
        values.put(id, batch.equals("A") ? 10d : 20d);
      }
    }
    final FeatureQcResult result = FeatureQcDriftNormalization.calculate(samples, Map.of(1, values),
        new FeatureQcSettings(1d, 0, 5, 10d, .5d));
    for (final String line : lines("multi_batch_reference.csv").subList(1, 3)) {
      final String[] fields = line.split(",");
      assertEquals(Double.parseDouble(fields[2]), result.globalLogTargets().get(1), 1e-12);
      for (int time = 0; time <= 8; time++) {
        assertEquals(Double.parseDouble(fields[4]), result.rowFactors().get(1).get(fields[0] + time),
            1e-12);
      }
    }
  }

  @Test
  void missingnessFixtureExercisesEngineGlobalAndBatchDecisions() throws IOException {
    final List<String> records = lines("missingness_semantics.csv");
    for (final String line : records.subList(1, records.size())) {
      final String[] fields = line.split(",");
      final List<FeatureQcDriftNormalization.Sample> samples = new ArrayList<>();
      final Map<String, Double> values = new LinkedHashMap<>();
      for (int batchIndex = 0; batchIndex < 2; batchIndex++) {
        final String batch = batchIndex == 0 ? "A" : "B";
        final int total = Integer.parseInt(fields[1 + batchIndex * 2]);
        final int usable = Integer.parseInt(fields[2 + batchIndex * 2]);
        for (int index = 0; index < total; index++) {
          final String id = batch + index;
          samples.add(new FeatureQcDriftNormalization.Sample(id, batch, index, true));
          if (index < usable) values.put(id, 100d);
        }
      }
      final FeatureQcResult result = FeatureQcDriftNormalization.calculate(samples, Map.of(1, values),
          new FeatureQcSettings(1d, 0, 5, 10d, .5d));
      final boolean globallyEligible = Boolean.parseBoolean(fields[5]);
      final boolean batchAEligible = Boolean.parseBoolean(fields[6]);
      assertEquals(globallyEligible, !result.unavailableRows().containsKey(1), fields[0]);
      assertEquals(globallyEligible && batchAEligible,
          result.rowFactors().getOrDefault(1, Map.of()).containsKey("A0"), fields[0]);
      if (!(globallyEligible && batchAEligible)) {
        assertNotNull(result.unavailableReason(1, "A0"), fields[0]);
      }
    }
  }

  private static FeatureQcResult calculate(final List<Entry> entries, final double span) {
    final List<FeatureQcDriftNormalization.Sample> samples = new ArrayList<>();
    final Map<String, Double> values = new LinkedHashMap<>();
    for (final Entry entry : entries) {
      samples.add(new FeatureQcDriftNormalization.Sample(entry.id(), entry.batch(), entry.time(),
          entry.qc()));
      values.put(entry.id(), entry.input());
    }
    return FeatureQcDriftNormalization.calculate(samples, Map.of(1, values),
        new FeatureQcSettings(span, 0, 5, 10d, .5d));
  }

  private static List<Entry> fixture(final String kind) throws IOException {
    final List<Entry> entries = new ArrayList<>();
    final List<String> records = lines("feature_qc_reference.csv");
    for (final String line : records.subList(1, records.size())) {
      final String[] fields = line.split(",");
      if (kind.equals(fields[0])) {
        entries.add(new Entry(fields[1] + fields[2], fields[1], Double.parseDouble(fields[2]),
            Boolean.parseBoolean(fields[3]), Double.parseDouble(fields[4]),
            Double.parseDouble(fields[5]), Double.parseDouble(fields[6]), Double.parseDouble(fields[7])));
      }
    }
    return entries;
  }

  private static List<String> lines(final String name) throws IOException {
    try (final var input = FeatureQcReferenceFixtureTest.class.getResourceAsStream(ROOT + name)) {
      assertNotNull(input, name);
      return List.of(new String(input.readAllBytes(), StandardCharsets.UTF_8).trim().split("\\R"));
    }
  }

  private record Entry(String id, String batch, double time, boolean qc, double input, double target,
                       double fit, double factor) {
  }
}
