package io.github.mzmine.modules.dataprocessing.norm_intensity;

import static org.junit.jupiter.api.Assertions.*;

import io.github.mzmine.modules.dataprocessing.norm_intensity.FeatureQcSmoothing.ManualLoess;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DunnQcNormalizationTest {

  @Test
  void dunnIsDefaultAndAlignsBatchesToCommonQcMedianRatherThanStudyAbundances() {
    final List<FeatureQcDriftNormalization.Sample> samples = new ArrayList<>();
    final Map<String, Double> values = new LinkedHashMap<>();
    for (final String batch : List.of("A", "B")) {
      for (int i = 0; i < 11; i++) {
        final String id = batch + i;
        samples.add(new FeatureQcDriftNormalization.Sample(id, batch, i * 50d, i % 2 == 0));
        values.put(id, i % 2 == 0 ? (batch.equals("A") ? 100d : 200d) : 1000d);
      }
    }
    final FeatureQcResult result = FeatureQcDriftNormalization.calculate(samples, Map.of(1, values),
        FeatureQcSettings.defaults());
    assertTrue(result.settings().automaticSmoothing());
    // QCs are 100 in batch A and 200 in batch B, both are scaled to the common median 150
    result.rowFactors().get(1).forEach((id, factor) ->
        assertEquals(id.startsWith("A") ? 1.5d : .75d, factor, 1e-10, id));
    final var diagnostic = result.diagnostics().rows().get(1).batches().get("B");
    assertEquals(150d, diagnostic.correctionTarget(), 1e-10);
    assertEquals(1, diagnostic.selectedDegree());
    assertEquals(6, diagnostic.leaveOneOutPredictionCount());
    assertTrue(diagnostic.validationScale().startsWith("raw response"));
    assertEquals(1d, diagnostic.fitPoints().getFirst().coordinate());
  }

  @Test
  void acquisitionIntervalsDoNotChangeDunnInjectionOrderFit() {
    final List<FeatureQcDriftNormalization.Sample> regular = new ArrayList<>();
    final List<FeatureQcDriftNormalization.Sample> irregular = new ArrayList<>();
    final Map<String, Double> values = new LinkedHashMap<>();
    for (int i = 0; i < 13; i++) {
      final String id = "s" + i;
      regular.add(new FeatureQcDriftNormalization.Sample(id, "A", i, i % 2 == 0));
      irregular.add(new FeatureQcDriftNormalization.Sample(id, "A", i * i * 30d, i % 2 == 0));
      values.put(id, 100d + 10d * i);
    }
    final FeatureQcResult a = FeatureQcDriftNormalization.calculate(regular, Map.of(1, values),
        FeatureQcSettings.defaults());
    final FeatureQcResult b = FeatureQcDriftNormalization.calculate(irregular, Map.of(1, values),
        FeatureQcSettings.defaults());
    assertEquals(a.rowFactors(), b.rowFactors());
    for (final var sample : regular) {
      assertEquals(160d, values.get(sample.id()) * a.rowFactors().get(1).get(sample.id()), 1e-8);
    }
  }

  @Test
  void automaticAndFixedSmoothingShareTheSameMissingnessRule() {
    final List<FeatureQcDriftNormalization.Sample> samples = new ArrayList<>();
    final Map<String, Double> values = new LinkedHashMap<>();
    for (int i = 0; i < 10; i++) {
      samples.add(new FeatureQcDriftNormalization.Sample("s" + i, "A", i, true));
      if (i % 2 == 0) values.put("s" + i, 100d);
    }
    final FeatureQcResult dunn = FeatureQcDriftNormalization.calculate(samples, Map.of(1, values),
        FeatureQcSettings.defaults());
    assertEquals(10, dunn.rowFactors().get(1).size());
    final FeatureQcResult fixed = FeatureQcDriftNormalization.calculate(samples, Map.of(1, values),
        new FeatureQcSettings(new ManualLoess(.75d, 1, 2), 5, 10d, .5d));
    assertEquals(dunn.rowFactors(), fixed.rowFactors());
  }
}
