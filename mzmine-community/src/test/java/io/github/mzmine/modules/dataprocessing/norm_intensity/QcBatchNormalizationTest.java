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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.modules.dataprocessing.norm_intensity.QcBatchNormalization.Sample;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class QcBatchNormalizationTest {

  @Test
  void calculate_interpolatesFactorsAtIrregularOrdersAndAlignsTwoBatches() {
    final List<Sample> samples = List.of(
        sample("a_before", "A", 0, false, Double.NaN),
        sample("a_qc_1", "A", 10, true, 10),
        sample("a_middle", "A", 20, false, Double.NaN),
        sample("a_qc_2", "A", 40, true, 20),
        sample("a_after", "A", 50, false, Double.NaN),
        sample("b_qc_1", "B", 100, true, 30),
        sample("b_middle", "B", 130, false, Double.NaN),
        sample("b_qc_2", "B", 160, true, 60));

    final QcBatchNormalization.Result result = QcBatchNormalization.calculate(samples, true);

    assertEquals(15d, result.batchReferenceMedians().get("A"));
    assertEquals(45d, result.batchReferenceMedians().get("B"));
    assertEquals(30d, result.interBatchReference());
    assertIterableEquals(List.of(samples.get(0).rawDataFile(), samples.get(4).rawDataFile()),
        result.edgeRawDataFiles());

    // A: QC factors 1.5 and .75; t=20 is one third from the later QC, so factor 1.25.
    // Batch correction then multiplies A by 2 and B by 2/3.
    assertEquals(3d, result.factors().get(samples.get(0).rawDataFile()));
    assertEquals(3d, result.factors().get(samples.get(1).rawDataFile()));
    assertEquals(2.5d, result.factors().get(samples.get(2).rawDataFile()));
    assertEquals(1.5d, result.factors().get(samples.get(3).rawDataFile()));
    assertEquals(1.5d, result.factors().get(samples.get(4).rawDataFile()));
    assertEquals(1d, result.factors().get(samples.get(5).rawDataFile()));
    assertEquals(.75d, result.factors().get(samples.get(6).rawDataFile()));
    assertEquals(.5d, result.factors().get(samples.get(7).rawDataFile()));
    assertIterableEquals(samples.stream().map(Sample::rawDataFile).toList(), result.factors().keySet());
    assertThrows(UnsupportedOperationException.class,
        () -> result.factors().put(rawDataFile("other"), 1d));
  }

  @Test
  void calculate_withoutBatchCorrectionKeepsIntraBatchFactors() {
    final List<Sample> samples = List.of(sample("a_qc_1", "A", 0, true, 10),
        sample("a_mid", "A", 3, false, Double.NaN), sample("a_qc_2", "A", 10, true, 20),
        sample("b_qc", "B", 0, true, 30));
    final QcBatchNormalization.Result result = QcBatchNormalization.calculate(samples, false);

    assertEquals(22.5d, result.interBatchReference());
    assertEquals(1.5d, result.factors().get(samples.get(0).rawDataFile()));
    assertEquals(1.275d, result.factors().get(samples.get(1).rawDataFile()), 1e-12);
    assertEquals(.75d, result.factors().get(samples.get(2).rawDataFile()));
    assertEquals(1d, result.factors().get(samples.get(3).rawDataFile()));
  }

  @Test
  void calculate_usesPrecomputedNativeFractionalTimestampWeightsWithoutRenormalizingThem() {
    final List<Sample> samples = List.of(sample("qc_1", "A", 0, true, 10),
        sample("sample", "A", 1, false, Double.NaN), sample("qc_2", "A", 3, true, 20));
    // Native pairwise ChronoUnit.SECONDS weights for 00.000, 01.600, and 03.200 are 1/3 and
    // 1/3. They intentionally do not sum to one, unlike global elapsed-second interpolation.
    final var result = QcBatchNormalization.calculate(samples,
        Map.of(samples.get(1).rawDataFile(), new ScalarQcBatchNormalization.ResolvedInterpolation<>(
            samples.get(0).rawDataFile(), 1d / 3d, samples.get(2).rawDataFile(), 1d / 3d)), false);

    assertEquals(.75d, result.factors().get(samples.get(1).rawDataFile()));
  }

  @Test
  void calculate_rejectsAmbiguousOrInvalidReferenceInputs() {
    assertThrows(IllegalArgumentException.class, () -> QcBatchNormalization.calculate(List.of(
        sample("only", "A", 0, false, Double.NaN)), false));
    assertThrows(IllegalArgumentException.class, () -> QcBatchNormalization.calculate(List.of(
        sample("qc", "A", 0, true, 0)), false));
    assertThrows(IllegalArgumentException.class, () -> QcBatchNormalization.calculate(List.of(
        sample("qc", "A", 0, true, Double.NaN)), false));
    assertThrows(IllegalArgumentException.class, () -> QcBatchNormalization.calculate(List.of(
        sample("qc", "A", 0, true, 1), sample("sample", "A", 0, false,
            Double.NaN)), false));
    final RawDataFile duplicate = rawDataFile("duplicate");
    assertThrows(IllegalArgumentException.class, () -> QcBatchNormalization.calculate(List.of(
        new Sample(duplicate, "A", 0, true, 1), new Sample(duplicate, "B", 1, true, 1)), false));
    assertThrows(IllegalArgumentException.class, () -> QcBatchNormalization.calculate(List.of(
        sample("qc", "A", Double.NaN, true, 1)), false));
  }

  @Test
  void calculate_distinguishesFilesWithTheSameDisplayName() {
    final RawDataFile firstQc = rawDataFile("duplicate-name");
    final RawDataFile sample = rawDataFile("sample");
    final RawDataFile secondQc = rawDataFile("duplicate-name");

    final var result = QcBatchNormalization.calculate(List.of(
        new Sample(firstQc, "A", 0, true, 10), new Sample(sample, "A", 1, false, Double.NaN),
        new Sample(secondQc, "A", 2, true, 20)), false);

    assertEquals(3, result.factors().size());
    assertTrue(result.factors().containsKey(firstQc));
    assertTrue(result.factors().containsKey(secondQc));
  }

  @Test
  void metric_matchesFactorNormalizationFunctionForEachIntensityMode() {
    final double[] values = {1d, 3d, 6d};
    assertEquals(3d, QcBatchNormalization.metric(values, FeatureIntensityNormalizationMode.MEDIAN));
    assertEquals(10d / 3d,
        QcBatchNormalization.metric(values, FeatureIntensityNormalizationMode.AVERAGE));
    assertEquals(10d, QcBatchNormalization.metric(values, FeatureIntensityNormalizationMode.SUM_TIC));
    assertEquals(6d, QcBatchNormalization.metric(values, FeatureIntensityNormalizationMode.MAX));

    final double factor = QcBatchNormalization.referenceFactor(15d, 10d);
    assertEquals(factor, new FactorNormalizationFunction(factor).getNormalizationFactor(0d, 0f));
  }

  @Test
  void sharedStagesCalculateReferenceInterpolationAndOptionalBatchAlignment() {
    final Map<String, Double> metrics = new LinkedHashMap<>();
    metrics.put("qc_1", 10d);
    metrics.put("qc_2", 20d);
    final var references = ScalarQcBatchNormalization.calculateReferenceFactors(metrics);
    final var withinBatch = ScalarQcBatchNormalization.calculateWithinBatchFactors(
        references.factors(), Map.of("sample", new ScalarQcBatchNormalization.ResolvedInterpolation<>(
            "qc_1", 1d / 3d, "qc_2", 1d / 3d)));

    assertEquals(15d, references.referenceLevel());
    assertEquals(.75d, withinBatch.get("sample"));
    final var scales = ScalarQcBatchNormalization.calculateInterBatchScales(
        Map.of("A", 15d, "B", 45d), true);
    assertEquals(30d, scales.interBatchReference());
    assertEquals(2d, scales.scales().get("A"));
    assertEquals(2d / 3d, scales.scales().get("B"));
  }

  private static @NotNull Sample sample(final @NotNull String name, final @NotNull String batch,
      final double order, final boolean reference, final double metric) {
    return new Sample(rawDataFile(name), batch, order, reference, metric);
  }

  private static @NotNull RawDataFile rawDataFile(final @NotNull String name) {
    return new RawDataFileImpl(name, null, null);
  }
}
