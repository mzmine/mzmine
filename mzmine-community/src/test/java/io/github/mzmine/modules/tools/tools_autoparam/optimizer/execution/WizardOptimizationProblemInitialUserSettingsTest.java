/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockConstruction;

import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassSpectrometerWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.DataImportWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowDdaWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.DataImportWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.MassSpectrometerWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows.WorkflowDDA;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.ParameterEstimationContext;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.ParameterEstimationTestData;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.PreparedParameterSet;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.OptimizerParameters;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.OptimizationMetrics;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.moeaframework.core.Solution;
import testutils.MZmineTestUtil;

class WizardOptimizationProblemInitialUserSettingsTest {

  @BeforeAll
  static void initialize() {
    MZmineTestUtil.startMzmineCore();
  }

  @Test
  void preservesInitialUserSettingsAndDisablesExportsOnlyInTheEvaluationCopy() {
    final WizardSequence initialUserSettings = new WizardSequence();
    final var dataImport = DataImportWizardParameterFactory.Data.create();
    final var massSpectrometer = MassSpectrometerWizardParameterFactory.QTOF.create();
    final var workflow = new WorkflowDDA().create();
    initialUserSettings.add(dataImport);
    initialUserSettings.add(massSpectrometer);
    initialUserSettings.add(workflow);
    // This value intentionally need not fit an optimizer search domain. The baseline must not rebuild
    // or clamp it before the evaluator receives its detached copy.
    massSpectrometer.setParameter(MassSpectrometerWizardParameters.minimumFeatureHeight, 123_456d);
    workflow.setParameter(WorkflowDdaWizardParameters.exportPath, true);
    dataImport.setParameter(DataImportWizardParameters.fileNames,
        new File[]{new File("initial-input.raw")});

    final WizardSequence evaluationCopy = WizardOptimizationProblem.copyForEvaluation(
        initialUserSettings);
    final var copiedMassSpectrometer = evaluationCopy.get(WizardPart.MS).orElseThrow();
    final var copiedWorkflow = evaluationCopy.get(WizardPart.WORKFLOW).orElseThrow();

    assertEquals(123_456d, copiedMassSpectrometer.getValue(
        MassSpectrometerWizardParameters.minimumFeatureHeight));
    assertFalse(copiedWorkflow.getValue(WorkflowDdaWizardParameters.exportPath));
    assertTrue(workflow.getValue(WorkflowDdaWizardParameters.exportPath));
    assertEquals(new File("initial-input.raw"), evaluationCopy.get(WizardPart.DATA_IMPORT)
        .orElseThrow().getValue(DataImportWizardParameters.fileNames)[0]);
  }

  @Test
  void reservesTheInitialUserSettingsComparisonOutsideTheSearchBudget() {
    final BatchExecutionBudget budget = new BatchExecutionBudget(3);

    assertEquals(1, budget.reserveAdditional());
    assertEquals(2, budget.reserve());
    assertEquals(1, budget.searchCount());
    assertEquals(2, budget.count());
    assertEquals(3, budget.maximum());
  }

  @Test
  void displaysTheActualInitialWizardValueInsteadOfAClampedEstimate() {
    final WizardSequence initialUserSettings = fullSequence();
    initialUserSettings.get(WizardPart.MS).orElseThrow()
        .setParameter(MassSpectrometerWizardParameters.minimumFeatureHeight, 123_456d);
    final ParameterEstimationContext context = ParameterEstimationTestData.context(
        initialUserSettings);
    final PreparedParameterSet prepared = PreparedParameterSet.prepare(context);
    final ParameterSet parameters = OptimizerParameters.create(
        List.of(OptimizationMetrics.IPO_ISOTOPE_SCORE), 30);
    parameters.setParameter(OptimizerParameters.paramToOptimize,
        List.of(ParameterEstimationTestData.MINIMUM_FEATURE_HEIGHT));
    parameters.setParameter(OptimizerParameters.evaluateInitialUserSettings, true);

    try (final var ignored = mockConstruction(OptimizationBatchEvaluator.class);
        final WizardOptimizationProblem problem = new WizardOptimizationProblem(context, prepared,
            parameters, new AtomicReference<>(TaskStatus.PROCESSING), 30, () -> false)) {
      final Solution initial = problem.evaluateInitialUserSettings(initialUserSettings);
      final IndexedParameter<?> minimumHeight = problem.getIndexedParameters().getFirst();

      assertEquals(0, initial.getNumberOfVariables(),
          "The comparison row must not be usable as an optimizer candidate");
      assertEquals(minimumHeight.formatNativeValue(123_456d), minimumHeight.formatValue(initial));
      assertFalse(minimumHeight.formatValue(problem.newSolution()).equals(
          minimumHeight.formatValue(initial)),
          "The prepared optimizer estimate must not be shown as the initial user setting");
    }
  }

  private static WizardSequence fullSequence() {
    final WizardSequence sequence = ParameterEstimationTestData.sequence();
    for (final WizardPart part : WizardPart.values()) {
      if (sequence.get(part).isEmpty()) {
        sequence.set(part, part.getDefaultPresets()[0].create());
      }
    }
    return sequence;
  }
}
