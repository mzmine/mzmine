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
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mzmine.modules.dataprocessing.norm_intensity.QcBatchNormalization.Sample;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class QcBatchNormalizationTest {

  @Test
  void calculate_interpolatesFactorsAtIrregularOrdersAndAlignsTwoBatches() {
    final List<Sample> samples = List.of(
        new Sample("a_before", "A", 0, false, Double.NaN),
        new Sample("a_qc_1", "A", 10, true, 10),
        new Sample("a_middle", "A", 20, false, Double.NaN),
        new Sample("a_qc_2", "A", 40, true, 20),
        new Sample("a_after", "A", 50, false, Double.NaN),
        new Sample("b_qc_1", "B", 100, true, 30),
        new Sample("b_middle", "B", 130, false, Double.NaN),
        new Sample("b_qc_2", "B", 160, true, 60));

    final QcBatchNormalization.Result result = QcBatchNormalization.calculate(samples, true);

    assertEquals(15d, result.batchReferenceMedians().get("A"));
    assertEquals(45d, result.batchReferenceMedians().get("B"));
    assertEquals(30d, result.interBatchReference());
    assertIterableEquals(List.of("a_before", "a_after"), result.edgeSampleIds());

    // A: QC factors 1.5 and .75; t=20 is one third from the later QC, so factor 1.25.
    // Batch correction then multiplies A by 2 and B by 2/3.
    assertEquals(3d, result.factors().get("a_before"));
    assertEquals(3d, result.factors().get("a_qc_1"));
    assertEquals(2.5d, result.factors().get("a_middle"));
    assertEquals(1.5d, result.factors().get("a_qc_2"));
    assertEquals(1.5d, result.factors().get("a_after"));
    assertEquals(1d, result.factors().get("b_qc_1"));
    assertEquals(.75d, result.factors().get("b_middle"));
    assertEquals(.5d, result.factors().get("b_qc_2"));
    assertIterableEquals(samples.stream().map(Sample::id).toList(), result.factors().keySet());
    assertThrows(UnsupportedOperationException.class, () -> result.factors().put("other", 1d));
  }

  @Test
  void calculate_withoutBatchCorrectionKeepsIntraBatchFactors() {
    final QcBatchNormalization.Result result = QcBatchNormalization.calculate(List.of(
        new Sample("a_qc_1", "A", 0, true, 10),
        new Sample("a_mid", "A", 3, false, Double.NaN),
        new Sample("a_qc_2", "A", 10, true, 20),
        new Sample("b_qc", "B", 0, true, 30)), false);

    assertEquals(22.5d, result.interBatchReference());
    assertEquals(1.5d, result.factors().get("a_qc_1"));
    assertEquals(1.275d, result.factors().get("a_mid"), 1e-12);
    assertEquals(.75d, result.factors().get("a_qc_2"));
    assertEquals(1d, result.factors().get("b_qc"));
  }

  @Test
  void calculate_usesFrozenNativeFractionalTimestampWeightsWithoutRenormalizingThem() {
    final List<Sample> samples = List.of(new Sample("qc_1", "A", 0, true, 10),
        new Sample("sample", "A", 1, false, Double.NaN),
        new Sample("qc_2", "A", 3, true, 20));
    // Native pairwise ChronoUnit.SECONDS weights for 00.000, 01.600, and 03.200 are 1/3 and
    // 1/3. They intentionally do not sum to one, unlike global elapsed-second interpolation.
    final var result = QcBatchNormalization.calculate(samples,
        Map.of("sample", new QcBatchNormalization.FrozenInterpolation("sample", "qc_1", 1d / 3d,
            "qc_2", 1d / 3d)), false);

    assertEquals(.75d, result.factors().get("sample"));
  }

  @Test
  void calculate_rejectsAmbiguousOrInvalidReferenceInputs() {
    assertThrows(IllegalArgumentException.class, () -> QcBatchNormalization.calculate(List.of(
        new Sample("only", "A", 0, false, Double.NaN)), false));
    assertThrows(IllegalArgumentException.class, () -> QcBatchNormalization.calculate(List.of(
        new Sample("qc", "A", 0, true, 0)), false));
    assertThrows(IllegalArgumentException.class, () -> QcBatchNormalization.calculate(List.of(
        new Sample("qc", "A", 0, true, Double.NaN)), false));
    assertThrows(IllegalArgumentException.class, () -> QcBatchNormalization.calculate(List.of(
        new Sample("qc", "A", 0, true, 1), new Sample("sample", "A", 0, false,
            Double.NaN)), false));
    assertThrows(IllegalArgumentException.class, () -> QcBatchNormalization.calculate(List.of(
        new Sample(" ", "A", 0, true, 1)), false));
    assertThrows(IllegalArgumentException.class, () -> QcBatchNormalization.calculate(List.of(
        new Sample("qc", "A", 0, true, 1), new Sample("qc", "B", 1, true, 1)), false));
    assertThrows(IllegalArgumentException.class, () -> QcBatchNormalization.calculate(List.of(
        new Sample("qc", "A", Double.NaN, true, 1)), false));
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
}
