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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonInterfaceHplcWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonMobilityWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.MassSpectrometerWizardParameterFactory;
import io.github.mzmine.modules.tools.tools_autoparam.DataFileStatistics;
import io.github.mzmine.modules.tools.tools_autoparam.RawDataParameterEstimation;
import io.github.mzmine.util.RawDataFileType;
import io.github.mzmine.util.RawDataFileTypeDetector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Ion interface and mass spectrometer presets that fit the raw data better than the presets
 * selected in the wizard. Applied before the parameters are estimated, so the estimates and the
 * optimizer start from the defaults of the fitting presets.
 *
 * @param changes the presets to replace, at most one per wizard part
 */
public record PresetSelection(@NotNull List<PresetChange> changes) {

  public static final PresetSelection NONE = new PresetSelection(List.of());

  /**
   * Chromatography presets that only differ in their defaults for the peak width.
   * <p>
   * decision: GC-CI is excluded, as it is a different ionization, and the wavelet preset, as it
   * selects a different chromatogram resolver. Both are kept when selected.
   */
  private static final List<IonInterfaceWizardParameterFactory> FWHM_PRESETS = List.of(
      IonInterfaceWizardParameterFactory.HPLC, IonInterfaceWizardParameterFactory.UHPLC,
      IonInterfaceWizardParameterFactory.HILIC);

  /**
   * Only these mass spectrometers are told apart by the injection time. Low res., Orbitrap Astral
   * and FT-ICR are kept when selected.
   */
  private static final List<MassSpectrometerWizardParameterFactory> INJECTION_TIME_PRESETS = List.of(
      MassSpectrometerWizardParameterFactory.QTOF, MassSpectrometerWizardParameterFactory.Orbitrap);

  public PresetSelection {
    changes = List.copyOf(changes);
  }

  /**
   * @param analysis the raw data measurements
   * @param sequence the wizard sequence with the currently selected presets
   * @return the presets to switch to, empty if the selected presets already fit
   */
  public static @NotNull PresetSelection select(@NotNull RawDataAnalysis analysis,
      @NotNull WizardSequence sequence) {
    final List<PresetChange> changes = new ArrayList<>(2);
    final PresetChange ionInterface = selectIonInterface(analysis, sequence);
    if (ionInterface != null) {
      changes.add(ionInterface);
    }
    final PresetChange massSpectrometer = selectMassSpectrometer(analysis, sequence);
    if (massSpectrometer != null) {
      changes.add(massSpectrometer);
    }
    return changes.isEmpty() ? NONE : new PresetSelection(changes);
  }

  /**
   * Selects the chromatography preset with the default FWHM closest to the estimated FWHM.
   */
  static @Nullable PresetChange selectIonInterface(@NotNull RawDataAnalysis analysis,
      @NotNull WizardSequence sequence) {
    final WizardStepParameters step = sequence.get(WizardPart.ION_INTERFACE).orElse(null);
    if (step == null || !(step.getFactory() instanceof IonInterfaceWizardParameterFactory current)
        || !FWHM_PRESETS.contains(current)) {
      return null;
    }
    final Double estimated = RawDataParameterEstimation.estimateFwhm(analysis.fwhms());
    if (estimated == null) {
      return null;
    }

    // decision: the current preset wins ties, so the selection only changes for a better fit
    IonInterfaceWizardParameterFactory closest = current;
    double closestDistance = Math.abs(presetFwhmMinutes(current) - estimated);
    for (final IonInterfaceWizardParameterFactory preset : FWHM_PRESETS) {
      final double distance = Math.abs(presetFwhmMinutes(preset) - estimated);
      if (distance < closestDistance) {
        closest = preset;
        closestDistance = distance;
      }
    }
    if (closest == current) {
      return null;
    }
    return new PresetChange(WizardPart.ION_INTERFACE, current, closest,
        "estimated FWHM %s min".formatted(ConfigService.getGuiFormats().rt(estimated)));
  }

  /**
   * Selects Orbitrap if the MS1 scans of a file that is not a Bruker TDF file have injection times,
   * otherwise QTOF.
   */
  static @Nullable PresetChange selectMassSpectrometer(@NotNull RawDataAnalysis analysis,
      @NotNull WizardSequence sequence) {
    final WizardStepParameters step = sequence.get(WizardPart.MS).orElse(null);
    if (step == null
        || !(step.getFactory() instanceof MassSpectrometerWizardParameterFactory current)
        || !INJECTION_TIME_PRESETS.contains(current)) {
      return null;
    }

    // assumption: the representative files stem from the same instrument, so a single file with
    // injection times is enough
    final boolean injectionTimes = analysis.files().stream().map(DataFileStatistics::file)
        .anyMatch(file -> !isBrukerTdf(file) && hasMs1InjectionTimes(file));
    final MassSpectrometerWizardParameterFactory target =
        injectionTimes ? MassSpectrometerWizardParameterFactory.Orbitrap
            : MassSpectrometerWizardParameterFactory.QTOF;
    if (target == current || !isAllowedWithIms(target, sequence)) {
      return null;
    }
    return new PresetChange(WizardPart.MS, current, target,
        injectionTimes ? "scans have injection times" : "scans have no injection times");
  }

  private static double presetFwhmMinutes(@NotNull IonInterfaceWizardParameterFactory preset) {
    return preset.create().getValue(IonInterfaceHplcWizardParameters.approximateChromatographicFWHM)
        .getToleranceInMinutes();
  }

  private static boolean isBrukerTdf(@NotNull RawDataFile file) {
    return RawDataFileTypeDetector.detectDataFileType(file.getAbsoluteFilePath())
        == RawDataFileType.BRUKER_TDF;
  }

  private static boolean hasMs1InjectionTimes(@NotNull RawDataFile file) {
    return file.getScans().stream()
        .anyMatch(scan -> scan.getMSLevel() == 1 && scan.hasInjectionTime());
  }

  /**
   * The wizard limits the mass spectrometers by the ion mobility preset, e.g., TWIMS is QTOF only.
   */
  private static boolean isAllowedWithIms(@NotNull MassSpectrometerWizardParameterFactory target,
      @NotNull WizardSequence sequence) {
    final IonMobilityWizardParameterFactory ims = sequence.get(WizardPart.IMS)
        .map(WizardStepParameters::getFactory)
        .filter(IonMobilityWizardParameterFactory.class::isInstance)
        .map(IonMobilityWizardParameterFactory.class::cast)
        .orElse(IonMobilityWizardParameterFactory.NO_IMS);
    return Arrays.asList(ims.getMatchingMassSpectrometerPresets()).contains(target);
  }

  public boolean isEmpty() {
    return changes.isEmpty();
  }

  /**
   * Replaces the changed parts with the default parameters of their new presets.
   *
   * @param sequence the sequence to modify, usually a copy of the wizard sequence
   */
  public void applyDefaultPresets(@NotNull WizardSequence sequence) {
    for (final PresetChange change : changes) {
      sequence.set(change.part(), change.to().create());
    }
  }

  public @NotNull String describe() {
    return changes.stream().map(PresetChange::describe).collect(Collectors.joining("\n"));
  }
}
