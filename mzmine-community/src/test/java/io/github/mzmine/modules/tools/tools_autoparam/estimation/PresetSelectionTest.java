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

import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassSpectrometerWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonMobilityWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.MassSpectrometerWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WizardParameterFactory;
import io.github.mzmine.modules.tools.tools_autoparam.preclassification.PreclassificationParameters;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testutils.MZmineTestUtil;

class PresetSelectionTest {

  @BeforeAll
  static void initialize() {
    MZmineTestUtil.startMzmineCore();
  }

  /**
   * Without data files, no scan has an injection time.
   */
  private static @NotNull RawDataAnalysis analysis(double @NotNull ... fwhms) {
    return new RawDataAnalysis(List.of(), fwhms, new double[0], new double[0], new double[0],
        new double[0], new double[0], Map.of());
  }

  private static @NotNull WizardSequence sequence(@NotNull IonInterfaceWizardParameterFactory lc,
      @NotNull MassSpectrometerWizardParameterFactory ms) {
    return sequence(lc, IonMobilityWizardParameterFactory.NO_IMS, ms);
  }

  private static @NotNull WizardSequence sequence(@NotNull IonInterfaceWizardParameterFactory lc,
      @NotNull IonMobilityWizardParameterFactory ims,
      @NotNull MassSpectrometerWizardParameterFactory ms) {
    final WizardSequence sequence = new WizardSequence();
    sequence.set(WizardPart.ION_INTERFACE, lc.create());
    sequence.set(WizardPart.IMS, ims.create());
    sequence.set(WizardPart.MS, ms.create());
    return sequence;
  }

  private static @Nullable WizardParameterFactory target(@NotNull PresetSelection selection,
      @NotNull WizardPart part) {
    return selection.changes().stream().filter(change -> change.part() == part)
        .map(PresetChange::to).findFirst().orElse(null);
  }

  @Test
  void narrowPeaksSwitchHplcToUhplc() {
    final PresetSelection selection = PresetSelection.select(analysis(0.04, 0.05, 0.06),
        sequence(IonInterfaceWizardParameterFactory.HPLC,
            MassSpectrometerWizardParameterFactory.QTOF));
    Assertions.assertEquals(IonInterfaceWizardParameterFactory.UHPLC,
        target(selection, WizardPart.ION_INTERFACE));
  }

  @Test
  void widePeaksSwitchToHilic() {
    final PresetSelection selection = PresetSelection.select(analysis(0.14, 0.16, 0.2),
        sequence(IonInterfaceWizardParameterFactory.UHPLC,
            MassSpectrometerWizardParameterFactory.QTOF));
    Assertions.assertEquals(IonInterfaceWizardParameterFactory.HILIC,
        target(selection, WizardPart.ION_INTERFACE));
  }

  @Test
  void fittingPresetIsKept() {
    final PresetSelection selection = PresetSelection.select(analysis(0.09, 0.1, 0.11),
        sequence(IonInterfaceWizardParameterFactory.HPLC,
            MassSpectrometerWizardParameterFactory.QTOF));
    Assertions.assertTrue(selection.isEmpty(), selection::describe);
  }

  @Test
  void waveletAndGcCiAreNeverSwitched() {
    for (final IonInterfaceWizardParameterFactory lc : List.of(
        IonInterfaceWizardParameterFactory.LC_WAVELET, IonInterfaceWizardParameterFactory.GC_CI)) {
      final PresetSelection selection = PresetSelection.select(analysis(0.3, 0.4, 0.5),
          sequence(lc, MassSpectrometerWizardParameterFactory.QTOF));
      Assertions.assertNull(target(selection, WizardPart.ION_INTERFACE), lc::toString);
    }
  }

  @Test
  void missingPeakWidthsKeepIonInterface() {
    final PresetSelection selection = PresetSelection.select(analysis(),
        sequence(IonInterfaceWizardParameterFactory.HPLC,
            MassSpectrometerWizardParameterFactory.QTOF));
    Assertions.assertTrue(selection.isEmpty(), selection::describe);
  }

  @Test
  void orbitrapWithoutInjectionTimesSwitchesToQtof() {
    final PresetSelection selection = PresetSelection.select(analysis(),
        sequence(IonInterfaceWizardParameterFactory.HPLC,
            MassSpectrometerWizardParameterFactory.Orbitrap));
    Assertions.assertEquals(MassSpectrometerWizardParameterFactory.QTOF,
        target(selection, WizardPart.MS));
  }

  @Test
  void otherMassSpectrometersAreKept() {
    for (final MassSpectrometerWizardParameterFactory ms : List.of(
        MassSpectrometerWizardParameterFactory.LOW_RES,
        MassSpectrometerWizardParameterFactory.Orbitrap_Astral,
        MassSpectrometerWizardParameterFactory.FTICR)) {
      final PresetSelection selection = PresetSelection.select(analysis(),
          sequence(IonInterfaceWizardParameterFactory.HPLC, ms));
      Assertions.assertNull(target(selection, WizardPart.MS), ms::toString);
    }
  }

  @Test
  void declinedPresetsKeepTheSequence() {
    final WizardSequence sequence = sequence(IonInterfaceWizardParameterFactory.HPLC,
        MassSpectrometerWizardParameterFactory.QTOF);
    final ParameterEstimationContext context = ParameterEstimationContext.withFittingPresets(
        analysis(0.04, 0.05, 0.06), sequence, new PreclassificationParameters().cloneParameterSet(),
        _ -> false);
    Assertions.assertTrue(context.presetSelection().isEmpty());
    Assertions.assertEquals(IonInterfaceWizardParameterFactory.HPLC,
        context.sequence().get(WizardPart.ION_INTERFACE).map(WizardStepParameters::getFactory)
            .orElseThrow());
  }

  @Test
  void confirmedPresetsSwitchACopyOfTheSequence() {
    final WizardSequence sequence = sequence(IonInterfaceWizardParameterFactory.HPLC,
        MassSpectrometerWizardParameterFactory.QTOF);
    final ParameterEstimationContext context = ParameterEstimationContext.withFittingPresets(
        analysis(0.04, 0.05, 0.06), sequence, new PreclassificationParameters().cloneParameterSet(),
        _ -> true);
    Assertions.assertFalse(context.presetSelection().isEmpty());
    Assertions.assertEquals(IonInterfaceWizardParameterFactory.UHPLC,
        context.sequence().get(WizardPart.ION_INTERFACE).map(WizardStepParameters::getFactory)
            .orElseThrow());
    Assertions.assertEquals(IonInterfaceWizardParameterFactory.HPLC,
        sequence.get(WizardPart.ION_INTERFACE).map(WizardStepParameters::getFactory).orElseThrow());
  }

  @Test
  void imsFilesSwitchToTheirIonMobilityPreset() {
    final WizardSequence sequence = sequence(IonInterfaceWizardParameterFactory.HPLC,
        MassSpectrometerWizardParameterFactory.QTOF);
    final PresetChange change = PresetSelection.selectIonMobility(
        List.of(MobilityType.TIMS, MobilityType.TIMS), sequence,
        IonInterfaceWizardParameterFactory.HPLC);
    Assertions.assertNotNull(change);
    Assertions.assertEquals(IonMobilityWizardParameterFactory.TIMS, change.to());
    Assertions.assertTrue(change.describe().contains("none"), change::describe);

    Assertions.assertEquals(IonMobilityWizardParameterFactory.TWIMS,
        PresetSelection.selectIonMobility(List.of(MobilityType.TRAVELING_WAVE), sequence,
            IonInterfaceWizardParameterFactory.HPLC).to());
  }

  @Test
  void mixedOrUnsupportedMobilityKeepsTheIonMobilityPreset() {
    final WizardSequence sequence = sequence(IonInterfaceWizardParameterFactory.HPLC,
        MassSpectrometerWizardParameterFactory.QTOF);
    // a file without ion mobility
    Assertions.assertNull(
        PresetSelection.selectIonMobility(Arrays.asList(MobilityType.TIMS, null), sequence,
            IonInterfaceWizardParameterFactory.HPLC));
    Assertions.assertNull(
        PresetSelection.selectIonMobility(List.of(MobilityType.TIMS, MobilityType.DRIFT_TUBE),
            sequence, IonInterfaceWizardParameterFactory.HPLC));
    Assertions.assertNull(PresetSelection.selectIonMobility(List.of(MobilityType.FAIMS), sequence,
        IonInterfaceWizardParameterFactory.HPLC));
    // GC-EI only supports no ion mobility
    Assertions.assertNull(PresetSelection.selectIonMobility(List.of(MobilityType.TIMS), sequence,
        IonInterfaceWizardParameterFactory.GC_EI));
    // already selected
    Assertions.assertNull(PresetSelection.selectIonMobility(List.of(MobilityType.TIMS),
        sequence(IonInterfaceWizardParameterFactory.HPLC, IonMobilityWizardParameterFactory.TIMS,
            MassSpectrometerWizardParameterFactory.QTOF), IonInterfaceWizardParameterFactory.HPLC));
  }

  @Test
  void ionMobilityRequiresAnAllowedMassSpectrometer() {
    final PresetChange change = PresetSelection.selectMassSpectrometer(analysis(),
        sequence(IonInterfaceWizardParameterFactory.HPLC,
            MassSpectrometerWizardParameterFactory.LOW_RES),
        IonMobilityWizardParameterFactory.TWIMS);
    Assertions.assertNotNull(change);
    Assertions.assertEquals(MassSpectrometerWizardParameterFactory.QTOF, change.to());
  }

  @Test
  void ionMobilitySwitchAppliesItsMassSpectrometerDefaults() {
    final WizardSequence sequence = sequence(IonInterfaceWizardParameterFactory.HPLC,
        MassSpectrometerWizardParameterFactory.QTOF);
    new PresetSelection(List.of(
        new PresetChange(WizardPart.IMS, IonMobilityWizardParameterFactory.NO_IMS,
            IonMobilityWizardParameterFactory.TIMS, "test"))).applyDefaultPresets(sequence);

    final MassSpectrometerWizardParameters expected = MassSpectrometerWizardParameterFactory.createForIms(
        IonMobilityWizardParameterFactory.TIMS);
    Assertions.assertEquals(IonMobilityWizardParameterFactory.TIMS,
        sequence.get(WizardPart.IMS).map(WizardStepParameters::getFactory).orElseThrow());
    Assertions.assertEquals(
        expected.getValue(MassSpectrometerWizardParameters.minimumFeatureHeight),
        sequence.get(WizardPart.MS).orElseThrow()
            .getValue(MassSpectrometerWizardParameters.minimumFeatureHeight));
  }
}
