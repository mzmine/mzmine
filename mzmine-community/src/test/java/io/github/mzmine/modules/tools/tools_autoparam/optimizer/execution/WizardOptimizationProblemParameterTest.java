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

package io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution;

import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonInterfaceHplcWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassSpectrometerWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.custom_parameters.WizardMsPolarity;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.OptimizationParameterRegistry;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.ParameterDefinition;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.ParameterEstimationContext;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.ParameterEstimationTestData;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.PreparedParameter;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.PreparedParameterSet;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.ValueOrigin;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.WizardParameterDefinition;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.domain.OrdinalIntegerVariable;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.OptimizerParameters;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.OptimizationMetrics;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.OptimizerOptions;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.WarmStartInitialization;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.WarmStartSampling;
import io.github.mzmine.modules.tools.tools_autoparam.preclassification.PreclassificationParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.RealVariable;
import testutils.MZmineTestUtil;

class WizardOptimizationProblemParameterTest {

  @BeforeAll
  static void initialize() {
    MZmineTestUtil.startMzmineCore();
  }

  private static @NotNull ParameterEstimationContext context() {
    return ParameterEstimationTestData.context(fullSequence());
  }

  private static @NotNull WizardSequence fullSequence() {
    final WizardSequence sequence = ParameterEstimationTestData.sequence();
    for (final WizardPart part : WizardPart.values()) {
      if (sequence.get(part).isEmpty()) {
        sequence.set(part, part.getDefaultPresets()[0].create());
      }
    }
    return sequence;
  }

  private static @NotNull WizardMsPolarity msPolarity(@NotNull WizardSequence sequence) {
    return sequence.get(WizardPart.MS).orElseThrow()
        .getValue(MassSpectrometerWizardParameters.polarity);
  }

  @Test
  void polarityIsAFixedEstimateThatTheOptimizerNeverOffers() {
    final String ionMode = MassSpectrometerWizardParameters.polarity.getName();
    Assertions.assertTrue(OptimizationParameterRegistry.forSequence(fullSequence()).stream()
        .anyMatch(definition -> definition.name().equals(ionMode)));
    Assertions.assertTrue(OptimizationParameterRegistry.allSolutions().stream()
        .noneMatch(definition -> definition.name().equals(ionMode)));

    final ParameterSet positive = new PreclassificationParameters().cloneParameterSet();
    positive.setParameter(PreclassificationParameters.polarity, WizardMsPolarity.Positive);
    final ParameterEstimationContext context = ParameterEstimationTestData.context(fullSequence(),
        positive);
    final WizardOptimizationProblem problem = problem(context,
        PreparedParameterSet.prepare(context),
        List.of(ParameterEstimationTestData.MINIMUM_FEATURE_HEIGHT));
    final Solution solution = problem.newSolution();

    // every candidate batch filters the pre-classified polarity
    Assertions.assertEquals(WizardMsPolarity.Positive,
        msPolarity(problem.createWizardSequenceFromSolution(solution)));
    final WizardSequence wizard = fullSequence();
    problem.applySolutionToWizard(solution, wizard);
    Assertions.assertEquals(WizardMsPolarity.Positive, msPolarity(wizard));
  }

  @Test
  void noPolarityFilterKeepsTheWizardIonMode() {
    final ParameterEstimationContext context = context();
    final WizardOptimizationProblem problem = problem(context,
        PreparedParameterSet.prepare(context),
        List.of(ParameterEstimationTestData.MINIMUM_FEATURE_HEIGHT));
    final WizardSequence wizard = fullSequence();
    wizard.get(WizardPart.MS).orElseThrow()
        .setParameter(MassSpectrometerWizardParameters.polarity, WizardMsPolarity.Negative);

    problem.applySolutionToWizard(problem.newSolution(), wizard);
    Assertions.assertEquals(WizardMsPolarity.Negative, msPolarity(wizard));
  }

  private static @NotNull WizardOptimizationProblem problem(
      @NotNull ParameterEstimationContext context, @NotNull PreparedParameterSet prepared,
      @NotNull List<ParameterDefinition<?>> selected) {
    final ParameterSet parameters = OptimizerParameters.create(
        List.of(OptimizationMetrics.IPO_ISOTOPE_SCORE), 30);
    parameters.setParameter(OptimizerParameters.paramToOptimize, selected);
    return new WizardOptimizationProblem(context, prepared, parameters,
        new AtomicReference<>(TaskStatus.PROCESSING), 30, () -> false);
  }

  @Test
  void moeadCreatesAnObjectiveForEachSelectedMetric() {
    final OptimizerParameters parameters = new OptimizerParameters();
    OptimizerParameters.setOptimizerAndTargets(parameters, OptimizerOptions.MOEAD,
        List.of(OptimizationMetrics.IPO_ISOTOPE_SCORE, OptimizationMetrics.SLAW_INTEGRATION_SCORE));
    Assertions.assertEquals(2, WizardOptimizationProblem.calculateNumberOfObjectives(parameters));
  }

  @Test
  void selectedAndUnselectedRtUseTheSamePreparedEstimate() {
    final ParameterEstimationContext context = context();
    final PreparedParameterSet prepared = PreparedParameterSet.prepare(context);
    final double expected = ParameterEstimationTestData.INTER_SAMPLE_RT.prepare(context)
        .initialValue().getToleranceInMinutes();

    for (final List<ParameterDefinition<?>> selection : List.<List<ParameterDefinition<?>>>of(
        List.of(ParameterEstimationTestData.MINIMUM_FEATURE_HEIGHT),
        List.of(ParameterEstimationTestData.INTER_SAMPLE_RT))) {
      final WizardOptimizationProblem problem = problem(context, prepared, selection);
      final WizardSequence applied = problem.createWizardSequenceFromSolution(
          problem.newSolution());
      Assertions.assertEquals(expected, applied.get(WizardPart.ION_INTERFACE).orElseThrow()
          .getValue(IonInterfaceHplcWizardParameters.interSampleRTTolerance)
          .getToleranceInMinutes());
    }
  }

  @Test
  void applyingASolutionKeepsUnrelatedWizardValuesAndMatchesTheEvaluatedValues() {
    final ParameterEstimationContext context = context();
    final PreparedParameterSet prepared = PreparedParameterSet.prepare(context);
    final List<ParameterDefinition<?>> selected = List.of(
        ParameterEstimationTestData.MINIMUM_FEATURE_HEIGHT);
    final WizardOptimizationProblem problem = problem(context, prepared, selected);

    // move the optimized value away from its estimate, so the test sees the solution value
    final Solution solution = problem.newSolution();
    final RealVariable variable = (RealVariable) solution.getVariable(0);
    variable.setValue((variable.getLowerBound() + variable.getUpperBound()) / 2);
    final WizardSequence evaluated = problem.createWizardSequenceFromSolution(solution);

    final WizardSequence wizard = context().sequence();
    final WizardStepParameters ionInterface = wizard.get(WizardPart.ION_INTERFACE).orElseThrow();
    final int userIsomers =
        ionInterface.getValue(IonInterfaceHplcWizardParameters.maximumIsomersInChromatogram) + 7;
    ionInterface.setParameter(IonInterfaceHplcWizardParameters.maximumIsomersInChromatogram,
        userIsomers);

    problem.applySolutionToWizard(solution, wizard);

    Assertions.assertEquals(userIsomers,
        ionInterface.getValue(IonInterfaceHplcWizardParameters.maximumIsomersInChromatogram));
    for (final PreparedParameter<?> parameter : prepared.parameters()) {
      final boolean estimated = parameter.origin() != ValueOrigin.PRESET_DEFAULT;
      if (!(parameter.definition() instanceof WizardParameterDefinition<?> definition) || (
          !estimated && !selected.contains(definition))) {
        continue;
      }
      final WizardStepParameters evaluatedStep = evaluated.get(definition.part()).orElseThrow();
      final WizardStepParameters appliedStep = wizard.get(definition.part()).orElseThrow();
      Assertions.assertTrue(appliedStep.getParameter(definition.parameter())
              .valueEquals(evaluatedStep.getParameter(definition.parameter())),
          () -> definition.name() + " differs from the evaluated value");
    }
    Assertions.assertTrue(wizard.get(WizardPart.MS).orElseThrow()
        .getParameter(MassSpectrometerWizardParameters.sampleToSampleMzTolerance).valueEquals(
            evaluated.get(WizardPart.MS).orElseThrow()
                .getParameter(MassSpectrometerWizardParameters.sampleToSampleMzTolerance)));
  }

  @Test
  void warmStartsUseTheBoundInitialValuesForEverySamplingStrategy() {
    final ParameterEstimationContext context = context();
    final PreparedParameterSet prepared = PreparedParameterSet.prepare(context);
    final WizardOptimizationProblem problem = problem(context, prepared,
        List.of(ParameterEstimationTestData.MINIMUM_CONSECUTIVE_SCANS,
            ParameterEstimationTestData.MINIMUM_FEATURE_HEIGHT,
            ParameterEstimationTestData.MZ_TOLERANCE));
    final Solution initial = problem.newSolution();
    for (final WarmStartSampling sampling : WarmStartSampling.values()) {
      final List<Solution> solutions = WarmStartInitialization.createSolutions(problem, 8,
          sampling);
      Assertions.assertEquals(8, solutions.size());
      for (int i = 0; i < initial.getNumberOfVariables(); i++) {
        Assertions.assertEquals(OrdinalIntegerVariable.effectiveValue(initial, i),
            OrdinalIntegerVariable.effectiveValue(solutions.getFirst(), i));
        for (final Solution solution : solutions) {
          final RealVariable variable = (RealVariable) solution.getVariable(i);
          Assertions.assertTrue(Double.isFinite(variable.getValue()));
          Assertions.assertTrue(variable.getValue() >= variable.getLowerBound());
          Assertions.assertTrue(variable.getValue() <= variable.getUpperBound());
        }
      }
    }
  }
}
