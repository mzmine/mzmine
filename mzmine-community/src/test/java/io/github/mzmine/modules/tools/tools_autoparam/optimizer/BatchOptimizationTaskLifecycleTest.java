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

package io.github.mzmine.modules.tools.tools_autoparam.optimizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.modules.tools.batchwizard.BatchWizardTab;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.ParameterEstimationContext;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.PreparedParameterSet;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.RawDataAnalysis;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.PresetSelection;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.WizardParameterEstimationResult;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution.WizardOptimizationProblem;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.OptimizerAlgorithmModule;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.PatternSearchOptimizerModule;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.RawDataPreparation;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.moeaframework.core.Solution;

class BatchOptimizationTaskLifecycleTest {

  @BeforeAll
  static void initializeConfiguration() {
    MZmineCore.getConfiguration().getPreferences();
  }

  @Test
  void algorithmCreationFailurePublishesCompletedEstimateAndKeepsErrorStatus() {
    final TaskFixture fixture = TaskFixture.create();
    final OptimizerAlgorithmModule algorithmModule = Mockito.mock(OptimizerAlgorithmModule.class);
    Mockito.when(algorithmModule.getName()).thenReturn("failing optimizer");
    Mockito.when(algorithmModule.createAlgorithm(Mockito.any(), Mockito.any())).thenThrow(
        new IllegalStateException("algorithm setup failed"));

    try (MockedStatic<MZmineCore> core = Mockito.mockStatic(MZmineCore.class,
        Mockito.CALLS_REAL_METHODS)) {
      core.when(() -> MZmineCore.getModuleInstance(PatternSearchOptimizerModule.class)).thenReturn(
          algorithmModule);
      fixture.run(problem -> {
        fixture.estimateSolution.setAttribute(WizardOptimizationProblem.ATTR_TOTAL_FEATURES, 1L);
        Mockito.when(problem.getCompletedBatchExecutionCount()).thenReturn(1);
      });
    }

    assertEquals(TaskStatus.ERROR, fixture.task.getStatus());
    final OptimizationOutcome outcome = fixture.task.getOutcome();
    assertNotNull(outcome);
    assertTrue(outcome.front().contains(fixture.completedSolution));
    assertEquals(List.of(fixture.completedSolution), outcome.evaluatedSolutions());
    Mockito.verify(fixture.problem.get()).close();
  }

  @Test
  void cancellationBeforeFinalPublishingRetainsCompletedEstimateAndStaysCanceled() {
    final TaskFixture fixture = TaskFixture.create();

    fixture.run(problem -> {
      Mockito.doAnswer(_ -> {
        fixture.task.cancel();
        return null;
      }).when(problem).evaluate(fixture.estimateSolution);
      Mockito.when(problem.getCompletedBatchExecutionCount()).thenReturn(1);
    });

    assertEquals(TaskStatus.CANCELED, fixture.task.getStatus());
    final OptimizationOutcome outcome = fixture.task.getOutcome();
    assertNotNull(outcome);
    assertTrue(outcome.front().contains(fixture.completedSolution));
    assertEquals(List.of(fixture.completedSolution), outcome.evaluatedSolutions());
    Mockito.verify(fixture.problem.get()).cancel();
    Mockito.verify(fixture.problem.get()).close();
  }

  private static final class TaskFixture {
    private final BatchOptimizationMainTask task;
    private final WizardSequence sequence;
    private final ParameterSet preclassification;
    private final Solution estimateSolution = new Solution(0, 2);
    private final Solution completedSolution = new Solution(0, 2);
    private final AtomicReference<WizardOptimizationProblem> problem = new AtomicReference<>();

    private TaskFixture(final BatchOptimizationMainTask task, final WizardSequence sequence,
        final ParameterSet preclassification) {
      this.task = task;
      this.sequence = sequence;
      this.preclassification = preclassification;
      estimateSolution.setAttribute(WizardOptimizationProblem.ATTR_TOTAL_FEATURES, 0L);
      completedSolution.setObjectiveValue(0, 1d);
      completedSolution.setObjectiveValue(1, 1d);
    }

    static TaskFixture create() {
      final BatchWizardTab tab = Mockito.mock(BatchWizardTab.class);
      final WizardSequence sequence = Mockito.mock(WizardSequence.class);
      Mockito.when(tab.getSequence()).thenReturn(sequence);
      Mockito.when(sequence.copy()).thenReturn(sequence);
      final ParameterSet preclassification = Mockito.mock(ParameterSet.class);
      final OptimizerParameters parameters = OptimizerParameters.create(
          new SimpleOptimizerParameters());
      return new TaskFixture(new BatchOptimizationMainTask(null, Instant.now(),
          new File[]{new File("fixture.mzML")}, null, tab, parameters, preclassification), sequence,
          preclassification);
    }

    void run(final java.util.function.Consumer<WizardOptimizationProblem> configureProblem) {
      final RawDataFile rawFile = Mockito.mock(RawDataFile.class);
      final ParameterEstimationContext context = Mockito.mock(ParameterEstimationContext.class);
      Mockito.when(context.presetSelection()).thenReturn(PresetSelection.NONE);
      final RawDataAnalysis analysis = Mockito.mock(RawDataAnalysis.class);
      Mockito.when(analysis.files()).thenReturn(List.of());
      Mockito.when(context.analysis()).thenReturn(analysis);
      final PreparedParameterSet estimates = Mockito.mock(PreparedParameterSet.class);
      final WizardParameterEstimationResult estimation = new WizardParameterEstimationResult(context,
          estimates);

      // Keep queued window creation outside this task-orchestration test.
      try (MockedStatic<FxThread> fx = Mockito.mockStatic(FxThread.class);
          MockedStatic<RawDataPreparation> preparation = Mockito.mockStatic(
          RawDataPreparation.class);
          MockedStatic<WizardParameterEstimationResult> estimationResult = Mockito.mockStatic(
              WizardParameterEstimationResult.class);
          MockedConstruction<WizardOptimizationProblem> problems = Mockito.mockConstruction(
              WizardOptimizationProblem.class, (created, _) -> {
                problem.set(created);
                Mockito.when(created.newSolution()).thenReturn(estimateSolution);
                Mockito.when(created.getEvaluatedSolutions()).thenReturn(List.of(completedSolution));
                configureProblem.accept(created);
              })) {
        preparation.when(() -> RawDataPreparation.importFilesBlocking(Mockito.any(File[].class),
            Mockito.isNull())).thenReturn(List.of(rawFile));
        estimationResult.when(() -> WizardParameterEstimationResult.estimate(Mockito.anyList(),
            Mockito.anyList(), Mockito.eq(sequence), Mockito.eq(preclassification), Mockito.any(),
            Mockito.isNull(), Mockito.any())).thenReturn(estimation);
        task.run();
      }
    }
  }
}
