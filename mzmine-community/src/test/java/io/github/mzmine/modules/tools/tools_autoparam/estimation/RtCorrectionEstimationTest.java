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

package io.github.mzmine.modules.tools.tools_autoparam.estimation;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonInterfaceHplcWizardParameters;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution.IndexedParameter;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.util.List;
import java.util.Map;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.RealVariable;

class RtCorrectionEstimationTest {

  private static @NotNull ParameterEstimate<Boolean> estimate(final double @NotNull [] medians,
      final double width) {
    return ParameterEstimators.rtCorrection(context(medians, width));
  }

  private static @NotNull ParameterEstimationContext context(final double @NotNull [] medians,
      final double width) {
    return new ParameterEstimationContext(
        new RawDataAnalysis(List.of(), new double[]{width}, new double[0], new double[0],
            new double[0], new double[0], medians, Map.of()),
        ParameterEstimationTestData.sequence());
  }

  @Test
  void detectsOutliersOnlyAboveBothThresholds() {
    Assertions.assertTrue(estimate(new double[]{0.01, 0.01, 0.06}, 0.1).initialValue());
    Assertions.assertTrue(estimate(new double[]{0, 0, 0.06, 0.08, 0}, 0.1).initialValue());
    Assertions.assertFalse(estimate(new double[]{0.01, 0.01, 0.04}, 0.1).initialValue());
    Assertions.assertFalse(estimate(new double[]{0.03, 0.03, 0.08}, 0.1).initialValue());
    Assertions.assertFalse(estimate(new double[]{0.01, 0.01, 0.05}, 0.1).initialValue());
    Assertions.assertFalse(estimate(new double[]{0.02, 0.02, 0.06}, 0.1).initialValue());
    Assertions.assertFalse(estimate(new double[]{0, 0, 0}, 0.1).initialValue());
  }

  @Test
  void insufficientMeasurementsPreserveWizardValueAndBothSearchChoices() {
    for (final double[] medians : List.of(new double[0], new double[]{0.1, 0.1})) {
      final var context = context(medians, 0.1);
      final var estimate = ParameterEstimators.rtCorrection(context);
      Assertions.assertEquals(ValueOrigin.PRESET_DEFAULT, estimate.origin());
      Assertions.assertFalse(estimate.searchDomain().decode(0));
      Assertions.assertTrue(estimate.searchDomain().decode(1));
      context.sequence().get(WizardPart.ION_INTERFACE).orElseThrow()
          .setParameter(IonInterfaceHplcWizardParameters.scanRtCorrection, true);
      PreparedParameterSet.prepare(context).applyEstimates(context.sequence());
      Assertions.assertTrue(context.sequence().get(WizardPart.ION_INTERFACE).orElseThrow()
          .getValue(IonInterfaceHplcWizardParameters.scanRtCorrection));
    }
    Assertions.assertEquals(ValueOrigin.PRESET_DEFAULT,
        estimate(new double[]{0, 0, 0.2}, Double.NaN).origin());
  }

  @Test
  void optimizerCanToggleCorrectionInWizard() {
    final var context = context(new double[]{0.01, 0.01, 0.08}, 0.1);
    final var parameter = OptimizationParameterRegistry.RT_CORRECTION.prepare(context);
    final var binding = new IndexedParameter<>(parameter, 0);
    final Solution solution = new Solution(1, 0);
    binding.initialize(solution);
    binding.applyToWizard(solution, context.sequence());
    Assertions.assertTrue(context.sequence().get(WizardPart.ION_INTERFACE).orElseThrow()
        .getValue(IonInterfaceHplcWizardParameters.scanRtCorrection));
    ((RealVariable) solution.getVariable(0)).setValue(0);
    binding.applyToWizard(solution, context.sequence());
    Assertions.assertFalse(context.sequence().get(WizardPart.ION_INTERFACE).orElseThrow()
        .getValue(IonInterfaceHplcWizardParameters.scanRtCorrection));
  }

  @Test
  void rowMedianUsesAbsoluteDifferencesForEarlyAndLateFeatures() {
    final RawDataFile[] files = new RawDataFile[5];
    for (int i = 0; i < files.length; i++) {
      files[i] = new RawDataFileImpl("file" + i, null, null, Color.BLACK);
    }
    try {
      final ModularFeatureList aligned = new ModularFeatureList("aligned", null, files);
      for (int i = 0; i < 5; i++) {
        final ModularFeatureListRow row = new ModularFeatureListRow(aligned, i);
        final float[] rts = {1f, 1f, 1f, 0.9f, 1.2f};
        for (int j = 0; j < files.length; j++) {
          final ModularFeature feature = new ModularFeature(aligned, files[j], null,
              FeatureStatus.DETECTED);
          feature.setMZ(100d + i);
          feature.setRT(rts[j] + i);
          row.addFeature(files[j], feature);
        }
        aligned.addRow(row);
        if (i < 4) {
          Assertions.assertEquals(0,
              RawDataAnalysis.extractFileMedianRtDeviations(aligned, 4).length);
        }
      }
      Assertions.assertArrayEquals(new double[]{0, 0, 0, 0.1, 0.2},
          RawDataAnalysis.extractFileMedianRtDeviations(aligned, 4), 1e-6);
      Assertions.assertEquals(0, RawDataAnalysis.extractFileMedianRtDeviations(aligned, 6).length);
    } finally {
      for (final RawDataFile file : files) {
        file.close();
      }
    }
  }
}
