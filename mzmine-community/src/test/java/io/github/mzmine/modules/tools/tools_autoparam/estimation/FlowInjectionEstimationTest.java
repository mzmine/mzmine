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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.SimpleRange.SimpleFloatRange;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonMobilityWizardParameterFactory;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.modules.tools.tools_autoparam.preclassification.PreclassificationParameters;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.AutoParamModule;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.AutoParamParameters;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.AutoParamTask;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.DataFileStatistics;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import testutils.MZmineTestUtil;

class FlowInjectionEstimationTest {

  @BeforeAll
  static void initialize() {
    MZmineTestUtil.startMzmineCore();
  }

  /**
   * Gaussian isotope signals in a one-minute run. With sigma 0.1 min, their extracted traces are
   * much wider than the chromatographic limit of 15% of the run.
   */
  private static RawDataFile file(double sigma, boolean isotopes) {
    return file(new double[]{sigma}, isotopes);
  }

  private static RawDataFile file(double[] sigmas, boolean isotopes) {
    final RawDataFile file = new RawDataFileImpl("injection", null, null, Color.BLACK);
    final int signalsPerEnvelope = isotopes ? 2 : 1;
    final double[] mzs = new double[sigmas.length * signalsPerEnvelope];
    for (int k = 0; k < sigmas.length; k++) {
      mzs[k * signalsPerEnvelope] = 200 + 50 * k;
      if (isotopes) {
        mzs[k * signalsPerEnvelope + 1] =
            mzs[k * signalsPerEnvelope] + IsotopePatternCalculator.THIRTHEEN_C_DISTANCE;
      }
    }
    for (int i = 0; i <= 200; i++) {
      final float rt = i / 200f;
      final double[] intensities = new double[mzs.length];
      for (int k = 0; k < sigmas.length; k++) {
        // Different apexes ensure each signal becomes a seed, including in mixed narrow/broad data.
        final double center = 0.25 + k * 0.1;
        final double intensity = 1E6 * Math.exp(-0.5 * Math.pow((rt - center) / sigmas[k], 2));
        intensities[k * signalsPerEnvelope] = intensity;
        if (isotopes) {
          intensities[k * signalsPerEnvelope + 1] = intensity * 0.2;
        }
      }
      file.addScan(
          new SimpleScan(file, i + 1, 1, rt, null, mzs, intensities, MassSpectrumType.CENTROIDED,
              PolarityType.POSITIVE, "", Range.closed(100d, 500d)));
    }
    return file;
  }

  private static WizardParameterEstimationResult estimate(RawDataFile file,
      IonInterfaceWizardParameterFactory preset) {
    return estimate(List.of(file), preset, _ -> false);
  }

  private static WizardParameterEstimationResult estimate(List<RawDataFile> files,
      IonInterfaceWizardParameterFactory preset, Predicate<PresetSelection> confirmation) {
    final WizardSequence sequence = ParameterEstimationTestData.sequence();
    sequence.set(WizardPart.ION_INTERFACE, preset.create());
    sequence.set(WizardPart.IMS, IonMobilityWizardParameterFactory.NO_IMS.create());
    return WizardParameterEstimationResult.estimate(files, null, sequence,
        new PreclassificationParameters().cloneParameterSet(), confirmation, null, () -> false);
  }

  private static DataFileStatistics statistics(RawDataFile file) {
    final DataFileStatistics stats = new AutoParamTask(null, Instant.now(),
        AutoParamParameters.of(List.of(file)), AutoParamModule.class, file, null,
        false).runAndGet();
    Assertions.assertNotNull(stats);
    return stats;
  }

  @Test
  void acceptingFlowInjectionUsesTheRetainedPeaksBeforeTheIsotopeGuard() {
    final RawDataFile file = file(new double[]{0.1, 0.1, 0.1}, true);
    final AtomicBoolean offered = new AtomicBoolean();
    final WizardParameterEstimationResult result = estimate(List.of(file),
        IonInterfaceWizardParameterFactory.HPLC, selection -> {
          Assertions.assertTrue(selection.changes().stream().anyMatch(
              change -> change.part() == WizardPart.ION_INTERFACE
                  && change.to() == IonInterfaceWizardParameterFactory.FLOW_INJECT));
          offered.set(true);
          return true;
        });
    Assertions.assertTrue(offered.get());
    Assertions.assertNotNull(result);
    Assertions.assertEquals(IonInterfaceWizardParameterFactory.FLOW_INJECT,
        result.context().sequence().get(WizardPart.ION_INTERFACE).orElseThrow().getFactory());
    Assertions.assertEquals(3, result.statistics().getFirst().featureStatistics().size());
    Assertions.assertNull(result.statistics().getFirst().effectiveRtRange());
    Assertions.assertEquals(3, result.context().analysis().heights().length);
  }

  @Test
  void contextSwitchReusesFeatureObjectsAndLeavesOriginalStatisticsIntact() {
    final DataFileStatistics stats = statistics(file(new double[]{0.1, 0.1, 0.1}, true));
    Assertions.assertTrue(stats.featureStatistics().isEmpty());
    Assertions.assertNotNull(stats.rejectedStatistics());
    final RawDataAnalysis analysis = RawDataAnalysis.analyze(List.of(stats));
    final WizardSequence sequence = ParameterEstimationTestData.sequence();
    sequence.set(WizardPart.IMS, IonMobilityWizardParameterFactory.NO_IMS.create());
    final ParameterEstimationContext context = ParameterEstimationContext.withFittingPresets(
        analysis, sequence, new PreclassificationParameters().cloneParameterSet(), _ -> true);
    Assertions.assertSame(stats.rejectedStatistics(), context.analysis().files().getFirst());
    Assertions.assertTrue(stats.featureStatistics().isEmpty());
    Assertions.assertEquals(IonInterfaceWizardParameterFactory.HPLC,
        sequence.get(WizardPart.ION_INTERFACE).orElseThrow().getFactory());
    Assertions.assertSame(context.analysis(), context.analysis().withBroadRejectedSignals());

    final ParameterEstimationContext declined = ParameterEstimationContext.withFittingPresets(
        analysis, sequence, new PreclassificationParameters().cloneParameterSet(), _ -> false);
    Assertions.assertSame(analysis, declined.analysis());
  }

  @Test
  void decliningFlowInjectionKeepsTheChromatographicPoolAndItsGuard() {
    final AtomicBoolean offered = new AtomicBoolean();
    Assertions.assertThrows(IllegalStateException.class,
        () -> estimate(List.of(file(new double[]{0.1, 0.1, 0.1}, true)),
            IonInterfaceWizardParameterFactory.HPLC, _ -> {
              offered.set(true);
              return false;
            }));
    Assertions.assertTrue(offered.get());
  }

  @Test
  void oneBroadSeedDoesNotBecomeManySignalsAcrossTolerances() {
    final DataFileStatistics stats = statistics(file(0.1, true));
    Assertions.assertNotNull(stats.rejectedStatistics());
    Assertions.assertEquals(1, stats.rejectedStatistics().featureStatistics().size());
    Assertions.assertTrue(
        stats.rejectedStatistics().featureStatistics().getFirst().getEnvelopes().size() > 1);
    Assertions.assertNull(
        PresetSelection.selectIonInterface(RawDataAnalysis.analyze(List.of(stats)),
            ParameterEstimationTestData.sequence()));
  }

  @Test
  void mixedFilesDoNotSuggestFlowInjection() {
    final DataFileStatistics broad = statistics(file(new double[]{0.1, 0.1, 0.1}, true));
    final DataFileStatistics narrow = statistics(file(0.01, true));
    final RawDataAnalysis analysis = RawDataAnalysis.analyze(List.of(broad, narrow));
    final PresetChange change = PresetSelection.selectIonInterface(analysis,
        ParameterEstimationTestData.sequence());
    Assertions.assertTrue(
        change == null || change.to() != IonInterfaceWizardParameterFactory.FLOW_INJECT);
  }

  @Test
  void unsupportedBroadSignalsDoNotSuggestFlowInjection() {
    final DataFileStatistics stats = statistics(file(new double[]{0.1, 0.1, 0.1}, false));
    Assertions.assertNotNull(stats.rejectedStatistics());
    Assertions.assertTrue(stats.rejectedStatistics().featureStatistics().isEmpty());
    Assertions.assertNull(
        PresetSelection.selectIonInterface(RawDataAnalysis.analyze(List.of(stats)),
            ParameterEstimationTestData.sequence()));
  }

  @Test
  void suggestionRequiresBroadSignalsToDominateAndDecliningPreservesNarrowEstimates() {
    for (int broadCount : new int[]{3, 4}) {
      final double[] widths = new double[broadCount + 1];
      Arrays.fill(widths, 0.1);
      widths[broadCount] = 0.01;
      final DataFileStatistics stats = statistics(file(widths, true));
      Assertions.assertEquals(1, stats.featureStatistics().size());
      Assertions.assertNotNull(stats.rejectedStatistics());
      Assertions.assertEquals(broadCount, stats.rejectedStatistics().featureStatistics().size());
      final RawDataAnalysis analysis = RawDataAnalysis.analyze(List.of(stats));
      final WizardSequence sequence = ParameterEstimationTestData.sequence();
      sequence.set(WizardPart.IMS, IonMobilityWizardParameterFactory.NO_IMS.create());
      final PresetChange change = PresetSelection.selectIonInterface(analysis, sequence);
      Assertions.assertEquals(broadCount == 4,
          change != null && change.to() == IonInterfaceWizardParameterFactory.FLOW_INJECT);
      final ParameterEstimationContext declined = ParameterEstimationContext.withFittingPresets(
          analysis, sequence, new PreclassificationParameters().cloneParameterSet(), _ -> false);
      Assertions.assertSame(analysis, declined.analysis());
      Assertions.assertArrayEquals(stats.getLowestIsotopeHeights(), declined.analysis().heights());
    }
  }

  @ParameterizedTest
  @EnumSource(value = IonInterfaceWizardParameterFactory.class, names = {"FLOW_INJECT",
      "DIRECT_INFUSION"})
  void switchedStatisticsUseTheFullRunInjectionTimeReference(
      IonInterfaceWizardParameterFactory preset) {
    final DataFileStatistics collected = statistics(file(new double[]{0.1, 0.1, 0.1}, true));
    Assertions.assertNotNull(collected.rejectedStatistics());
    final DataFileStatistics rejected = new DataFileStatistics(collected.file(),
        collected.rejectedStatistics().featureStatistics(), null, 20d);
    final DataFileStatistics stats = new DataFileStatistics(collected.file(), List.of(),
        new SimpleFloatRange(0.2f, 0.6f), 5d, rejected);
    final WizardSequence sequence = ParameterEstimationTestData.sequence();
    sequence.set(WizardPart.ION_INTERFACE, preset.create());
    final ParameterEstimationContext context = new ParameterEstimationContext(
        RawDataAnalysis.analyze(List.of(stats)), sequence);
    Assertions.assertSame(rejected, context.analysis().files().getFirst());
    Assertions.assertEquals(20d, context.analysis().files().getFirst().referenceInjectionTime());
    Assertions.assertNull(context.analysis().files().getFirst().effectiveRtRange());
    Assertions.assertEquals(5d, stats.referenceInjectionTime());
  }

  @Test
  void switchingFilesRecomputesCrossFileStatisticsFromTheBroadPool() {
    final WizardParameterEstimationResult result = estimate(
        List.of(file(new double[]{0.1, 0.1, 0.1}, true), file(new double[]{0.1, 0.1, 0.1}, true)),
        IonInterfaceWizardParameterFactory.HPLC, _ -> true);
    Assertions.assertNotNull(result);
    Assertions.assertEquals(6, result.context().analysis().heights().length);
    Assertions.assertFalse(result.context().analysis().sampleMzToleranceCounts().isEmpty());
    Assertions.assertTrue(
        result.statistics().stream().allMatch(stats -> stats.featureStatistics().size() == 3));
  }

  @ParameterizedTest
  @EnumSource(value = IonInterfaceWizardParameterFactory.class, names = {"FLOW_INJECT",
      "DIRECT_INFUSION"})
  void broadIsotopeSignalsProduceEstimates(IonInterfaceWizardParameterFactory preset) {
    final WizardParameterEstimationResult result = estimate(file(0.1, true), preset);
    Assertions.assertNotNull(result);
    Assertions.assertFalse(result.statistics().getFirst().featureStatistics().isEmpty());
    Assertions.assertNull(result.statistics().getFirst().effectiveRtRange());
    Assertions.assertEquals(ValueOrigin.RAW_DATA, result.estimates().parameters().stream()
        .filter(p -> p.definition() == OptimizationParameterRegistry.MINIMUM_FEATURE_HEIGHT)
        .findFirst().orElseThrow().origin());
  }

  @Test
  void chromatographyStillRejectsBroadBackgroundAndAcceptsNarrowPeaks() {
    for (final double sigma : new double[]{0.1, 0.01}) {
      final RawDataFile file = file(sigma, true);
      final DataFileStatistics stats = statistics(file);
      Assertions.assertEquals(sigma == 0.1, stats.featureStatistics().isEmpty());
    }
  }

  @Test
  void flowInjectionStillRequiresIsotopeEvidence() {
    final RawDataFile file = file(0.1, false);
    Assertions.assertThrows(IllegalStateException.class,
        () -> estimate(file, IonInterfaceWizardParameterFactory.FLOW_INJECT));
  }

  @Test
  void noIsotopeEvidenceFailsWithoutOfferingUnrelatedPresetChanges() {
    Assertions.assertThrows(IllegalStateException.class,
        () -> estimate(List.of(file(new double[]{0.1, 0.1, 0.1}, false)),
            IonInterfaceWizardParameterFactory.HPLC, _ -> {
              Assertions.fail("No preset change can rescue data without isotope evidence");
              return true;
            }));
  }
}
