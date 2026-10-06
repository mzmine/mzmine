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

import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.CustomizationWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonInterfaceHplcWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonMobilityWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassDetectorWizardOptions;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassSpectrometerWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.ParameterOverride;
import io.github.mzmine.modules.tools.batchwizard.subparameters.custom_parameters.WizardMassDetectorNoiseLevels;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonMobilityWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.MassSpectrometerWizardParameterFactory;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.domain.ChoiceSearchDomain;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.OrdinalIntegerVariable;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.domain.SearchScale;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution.IndexedParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.RealVariable;

class PreparedParameterSetTest {

  private static <T> void assertRoundTrip(@NotNull PreparedParameter<T> parameter) {
    final Solution solution = new Solution(1, 0);
    final IndexedParameter<T> indexed = new IndexedParameter<>(parameter, 0);
    indexed.initialize(solution);
    if (parameter.initialValue() instanceof RTTolerance expected) {
      final RTTolerance actual = (RTTolerance) indexed.value(solution);
      Assertions.assertEquals(expected.getToleranceInMinutes(), actual.getToleranceInMinutes());
      Assertions.assertEquals(Unit.MINUTES, actual.getUnit());
    } else {
      Assertions.assertEquals(parameter.initialValue(), indexed.value(solution),
          parameter.definition().name());
    }
  }

  private static <T> @NotNull PreparedParameter<T> withValue(
      @NotNull ParameterDefinition<T> definition, @NotNull T value,
      @NotNull ParameterEstimationContext context) {
    return new PreparedParameter<>(definition, value, ValueOrigin.RAW_DATA,
        definition.prepare(context).searchDomain());
  }

  @Test
  void minimumHeightKeepsItsTargetAcrossDirectAndIndexedApplication() {
    final WizardSequence direct = ParameterEstimationTestData.sequence();
    final WizardSequence indexed = ParameterEstimationTestData.sequence();
    final PreparedParameter<Double> parameter = OptimizationParameterRegistry.MINIMUM_FEATURE_HEIGHT.prepare(
        ParameterEstimationTestData.context(direct));
    Assertions.assertEquals(12_000d, parameter.initialValue());
    Assertions.assertEquals(ValueOrigin.RAW_DATA, parameter.origin());
    Assertions.assertEquals(SearchScale.LOGARITHMIC, parameter.searchDomain().searchScale());

    parameter.applyInitialValue(direct);
    final IndexedParameter<Double> binding = new IndexedParameter<>(parameter, 2);
    final Solution solution = new Solution(3, 0);
    binding.initialize(solution);
    binding.applyToWizard(solution, indexed);
    Assertions.assertEquals(12_000d, direct.get(WizardPart.MS).orElseThrow()
        .getValue(MassSpectrometerWizardParameters.minimumFeatureHeight));
    Assertions.assertEquals(direct.get(WizardPart.MS).orElseThrow()
            .getValue(MassSpectrometerWizardParameters.minimumFeatureHeight),
        indexed.get(WizardPart.MS).orElseThrow()
            .getValue(MassSpectrometerWizardParameters.minimumFeatureHeight));

    ((RealVariable) solution.getVariable(2)).setValue(25_000d);
    binding.applyToWizard(solution, indexed);
    Assertions.assertEquals(25_000d, indexed.get(WizardPart.MS).orElseThrow()
        .getValue(MassSpectrometerWizardParameters.minimumFeatureHeight));
    Assertions.assertEquals(12_000d, parameter.initialValue());
  }

  @Test
  void everyPreparedValueSurvivesTheOptimizerBoundary() {
    final PreparedParameterSet prepared = PreparedParameterSet.prepare(
        ParameterEstimationTestData.context(ParameterEstimationTestData.sequence()));
    for (final PreparedParameter<?> parameter : prepared.parameters()) {
      assertRoundTrip(parameter);
    }
  }

  @Test
  void typedToleranceIntegerAndNoiseBindingsApplyActualWizardValues() {
    final WizardSequence sequence = ParameterEstimationTestData.sequence();
    final ParameterEstimationContext context = ParameterEstimationTestData.context(sequence);
    final PreparedParameterSet estimates = new PreparedParameterSet(List.of(
        withValue(OptimizationParameterRegistry.FWHM, new RTTolerance(0.08f, Unit.MINUTES),
            context),
        withValue(OptimizationParameterRegistry.MINIMUM_CONSECUTIVE_SCANS, 6, context),
        withValue(OptimizationParameterRegistry.MS1_NOISE,
            new WizardMassDetectorNoiseLevels(MassDetectorWizardOptions.ABSOLUTE_NOISE_LEVEL, 500,
                200), context), withValue(OptimizationParameterRegistry.MZ_TOLERANCE,
            MzToleranceSearchOptions.ALL_TOLERANCE_OPTIONS[4], context)));
    estimates.applyEstimates(sequence);
    final var lc = sequence.get(WizardPart.ION_INTERFACE).orElseThrow();
    Assertions.assertEquals(0.08,
        lc.getValue(IonInterfaceHplcWizardParameters.approximateChromatographicFWHM)
            .getToleranceInMinutes(), 1e-6);
    Assertions.assertEquals(6, lc.getValue(IonInterfaceHplcWizardParameters.minNumberOfDataPoints));
    final var ms = sequence.get(WizardPart.MS).orElseThrow();
    Assertions.assertEquals(MzToleranceSearchOptions.ALL_TOLERANCE_OPTIONS[4],
        ms.getValue(MassSpectrometerWizardParameters.scanToScanMzTolerance));
    final WizardMassDetectorNoiseLevels noise = ms.getValue(
        MassSpectrometerWizardParameters.massDetectorOption);
    Assertions.assertEquals(500d, noise.getMs1NoiseLevel());
    Assertions.assertEquals(200d, noise.getMsnNoiseLevel());
  }

  @Test
  void mobilityStartsAtPresetDefaultButEstimateOnlyPreservesWizardEdits() {
    for (final IonMobilityWizardParameterFactory preset : IonMobilityWizardParameterFactory.values()) {
      if (preset == IonMobilityWizardParameterFactory.NO_IMS) {
        continue;
      }
      final WizardSequence sequence = new WizardSequence();
      sequence.set(WizardPart.IMS, preset.create());
      final PreparedParameterSet estimates = PreparedParameterSet.prepare(
          ParameterEstimationTestData.context(sequence));
      final PreparedParameter<?> parameter = estimates.parameters().getFirst();
      final double expected = sequence.get(WizardPart.IMS).orElseThrow()
          .getValue(IonMobilityWizardParameters.approximateImsFWHM);
      Assertions.assertEquals(ValueOrigin.PRESET_DEFAULT, parameter.origin());
      Assertions.assertEquals(expected, parameter.initialValue());
      assertRoundTrip(parameter);
      sequence.get(WizardPart.IMS).orElseThrow()
          .setParameter(IonMobilityWizardParameters.approximateImsFWHM, expected * 1.1);
      estimates.applyEstimates(sequence);
      Assertions.assertEquals(expected * 1.1, sequence.get(WizardPart.IMS).orElseThrow()
          .getValue(IonMobilityWizardParameters.approximateImsFWHM));
    }
  }

  @Test
  void noMobilityParameterWithoutIonMobility() {
    final WizardSequence sequence = new WizardSequence();
    sequence.set(WizardPart.IMS, IonMobilityWizardParameterFactory.NO_IMS.create());
    Assertions.assertTrue(
        PreparedParameterSet.prepare(ParameterEstimationTestData.context(sequence)).parameters()
            .isEmpty());
  }

  @Test
  void missingMeasurementsHaveFiniteFallbacksWithoutOverwritingWizardEdits() {
    final WizardSequence sequence = ParameterEstimationTestData.sequence();
    final ParameterEstimationContext context = new ParameterEstimationContext(
        RawDataAnalysis.analyze(List.of()), sequence);
    final PreparedParameter<Double> height = OptimizationParameterRegistry.MINIMUM_FEATURE_HEIGHT.prepare(
        context);
    final PreparedParameter<RTTolerance> rt = OptimizationParameterRegistry.INTER_SAMPLE_RT.prepare(
        context);
    Assertions.assertEquals(ValueOrigin.PRESET_DEFAULT, height.origin());
    Assertions.assertEquals(ValueOrigin.PRESET_DEFAULT, rt.origin());
    final PreparedParameterSet estimates = PreparedParameterSet.prepare(context);
    sequence.get(WizardPart.MS).orElseThrow()
        .setParameter(MassSpectrometerWizardParameters.minimumFeatureHeight, 123_456d);
    estimates.applyEstimates(sequence);
    Assertions.assertEquals(123_456d, sequence.get(WizardPart.MS).orElseThrow()
        .getValue(MassSpectrometerWizardParameters.minimumFeatureHeight));
    estimates.parameters().forEach(PreparedParameterSetTest::assertRoundTrip);
  }

  @Test
  void optimizedBatchOverrideReplacesBaselineAndPreservesUnrelatedCustomization() {
    final WizardSequence sequence = ParameterEstimationTestData.sequence();
    final ParameterEstimationContext context = ParameterEstimationTestData.context(sequence);
    final PreparedParameterSet baseline = new PreparedParameterSet(
        List.of(OptimizationParameterRegistry.TOP_TO_EDGE.prepare(context),
            OptimizationParameterRegistry.CHROMATOGRAPHIC_THRESHOLD.prepare(context)));
    baseline.applyEstimates(sequence);
    final List<IndexedParameter<?>> selected = IndexedParameter.bind(baseline,
        List.of(OptimizationParameterRegistry.TOP_TO_EDGE));
    final Solution candidate = new Solution(1, 0);
    selected.getFirst().initialize(candidate);
    ((RealVariable) candidate.getVariable(0)).setValue(2.1d);
    baseline.applyBaseline(sequence, Set.of(OptimizationParameterRegistry.TOP_TO_EDGE));
    selected.getFirst().applyToWizard(candidate, sequence);

    final var customization = sequence.get(WizardPart.CUSTOMIZATION).orElseThrow();
    final List<ParameterOverride> overrides = customization.getValue(
        CustomizationWizardParameters.overrides);
    Assertions.assertTrue(customization.getValue(CustomizationWizardParameters.enabled));
    Assertions.assertEquals(2, overrides.size());
    Assertions.assertEquals(2.1d, overrides.stream()
        .filter(p -> p.parameterWithValue().getName().equals("Min ratio of peak top/edge"))
        .findFirst().orElseThrow().parameterWithValue().getValue());
    Assertions.assertEquals(0.85d, overrides.stream()
        .filter(p -> p.parameterWithValue().getName().equals("Chromatographic threshold"))
        .findFirst().orElseThrow().parameterWithValue().getValue());
  }

  @Test
  void reorderedSubsetBindsByIdentityAndLeavesOtherEstimatesFixed() {
    final WizardSequence sequence = ParameterEstimationTestData.sequence();
    final ParameterEstimationContext context = ParameterEstimationTestData.context(sequence);
    final PreparedParameterSet baseline = PreparedParameterSet.prepare(context);
    final List<IndexedParameter<?>> selected = IndexedParameter.bind(baseline,
        List.of(OptimizationParameterRegistry.MINIMUM_CONSECUTIVE_SCANS,
            OptimizationParameterRegistry.MINIMUM_FEATURE_HEIGHT));
    final Solution candidate = new Solution(2, 0);
    selected.forEach(parameter -> parameter.initialize(candidate));
    Assertions.assertInstanceOf(OrdinalIntegerVariable.class, candidate.getVariable(0));
    ((RealVariable) candidate.getVariable(0)).setValue(6.4d);
    ((RealVariable) candidate.getVariable(1)).setValue(25_000d);
    baseline.applyBaseline(sequence, Set.of(OptimizationParameterRegistry.MINIMUM_CONSECUTIVE_SCANS,
        OptimizationParameterRegistry.MINIMUM_FEATURE_HEIGHT));
    selected.forEach(parameter -> parameter.applyToWizard(candidate, sequence));
    Assertions.assertEquals(6, sequence.get(WizardPart.ION_INTERFACE).orElseThrow()
        .getValue(IonInterfaceHplcWizardParameters.minNumberOfDataPoints));
    Assertions.assertEquals(25_000d, sequence.get(WizardPart.MS).orElseThrow()
        .getValue(MassSpectrometerWizardParameters.minimumFeatureHeight));
    Assertions.assertEquals(
        OptimizationParameterRegistry.FWHM.prepare(context).initialValue().getToleranceInMinutes(),
        sequence.get(WizardPart.ION_INTERFACE).orElseThrow()
            .getValue(IonInterfaceHplcWizardParameters.approximateChromatographicFWHM)
            .getToleranceInMinutes());
  }

  @Test
  void unavailableOrDuplicateSelectionsFailBeforeOptimization() {
    final PreparedParameterSet empty = new PreparedParameterSet(List.of());
    Assertions.assertThrows(IllegalArgumentException.class, () -> IndexedParameter.bind(empty,
        List.of(OptimizationParameterRegistry.MINIMUM_FEATURE_HEIGHT)));
    final PreparedParameterSet prepared = PreparedParameterSet.prepare(
        ParameterEstimationTestData.context(ParameterEstimationTestData.sequence()));
    Assertions.assertThrows(IllegalArgumentException.class, () -> IndexedParameter.bind(prepared,
        List.of(OptimizationParameterRegistry.MINIMUM_FEATURE_HEIGHT,
            OptimizationParameterRegistry.MINIMUM_FEATURE_HEIGHT)));
  }

  @Test
  void supportedWizardPresetsPrepareWithoutRawMeasurements() {
    for (final IonInterfaceWizardParameterFactory ionInterface : IonInterfaceWizardParameterFactory.values()) {
      if (ionInterface == IonInterfaceWizardParameterFactory.LC_WAVELET) {
        continue;
      }
      for (final MassSpectrometerWizardParameterFactory ms : MassSpectrometerWizardParameterFactory.values()) {
        final WizardSequence sequence = ParameterEstimationTestData.sequence();
        sequence.set(WizardPart.ION_INTERFACE, ionInterface.create());
        sequence.set(WizardPart.MS, ms.create());
        final PreparedParameterSet prepared = PreparedParameterSet.prepare(
            new ParameterEstimationContext(RawDataAnalysis.analyze(List.of()), sequence));
        prepared.parameters().forEach(PreparedParameterSetTest::assertRoundTrip);
      }
    }
  }

  @Test
  void choiceDomainsRetainValuesAndConstrainOrdinalEndpoints() {
    final ChoiceSearchDomain<ValueOrigin> domain = new ChoiceSearchDomain<>(
        List.of(ValueOrigin.HEURISTIC, ValueOrigin.RAW_DATA), 3);
    Assertions.assertEquals(3d, domain.encode(ValueOrigin.HEURISTIC));
    Assertions.assertEquals(ValueOrigin.HEURISTIC, domain.decode(2.5d));
    Assertions.assertEquals(ValueOrigin.RAW_DATA, domain.decode(4.5d));
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> domain.encode(ValueOrigin.PRESET_DEFAULT));
  }

  @Test
  void crossFileMzToleranceIsDerivedFromCountsDuringPreparation() {
    final var narrow = MzToleranceSearchOptions.ALL_TOLERANCE_OPTIONS[2];
    final var wide = MzToleranceSearchOptions.ALL_TOLERANCE_OPTIONS[5];
    final RawDataAnalysis analysis = new RawDataAnalysis(List.of(), new double[0], new double[0],
        new double[0], new double[0], new double[0], new double[0],
        java.util.Map.of(narrow, 2, wide, 8));
    final var estimated = OptimizationParameterRegistry.SAMPLE_TO_SAMPLE_MZ_TOLERANCE.prepare(
        new ParameterEstimationContext(analysis, ParameterEstimationTestData.sequence()));
    Assertions.assertEquals(wide, estimated.initialValue());
    Assertions.assertEquals(ValueOrigin.RAW_DATA, estimated.origin());

    // without cross-file statistics the preset is kept and not applied to the wizard
    final var fallback = OptimizationParameterRegistry.SAMPLE_TO_SAMPLE_MZ_TOLERANCE.prepare(
        new ParameterEstimationContext(RawDataAnalysis.analyze(List.of()),
            ParameterEstimationTestData.sequence()));
    Assertions.assertEquals(ValueOrigin.PRESET_DEFAULT, fallback.origin());
  }

}
