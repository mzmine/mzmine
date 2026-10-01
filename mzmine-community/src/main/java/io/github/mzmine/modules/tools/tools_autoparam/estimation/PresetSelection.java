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

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonInterfaceHplcWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonMobilityWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.MassSpectrometerWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WizardParameterFactory;
import io.github.mzmine.modules.tools.tools_autoparam.DataFileStatistics;
import io.github.mzmine.modules.tools.tools_autoparam.RawDataParameterEstimation;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.util.RawDataFileType;
import io.github.mzmine.util.RawDataFileTypeDetector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Ion interface, ion mobility and mass spectrometer presets that fit the raw data better than the
 * presets selected in the wizard. Applied before the parameters are estimated, so the estimates and
 * the optimizer start from the defaults of the fitting presets.
 *
 * @param changes the presets to replace, at most one per wizard part, in wizard part order
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
    final List<PresetChange> changes = new ArrayList<>(3);
    // each part is selected for the presets the previous parts switch to, as they limit each other
    final PresetChange ionInterface = selectIonInterface(analysis, sequence);
    if (ionInterface != null) {
      changes.add(ionInterface);
    }
    final IonInterfaceWizardParameterFactory newIonInterface =
        ionInterface != null ? (IonInterfaceWizardParameterFactory) ionInterface.to()
            : currentFactory(sequence, WizardPart.ION_INTERFACE,
                IonInterfaceWizardParameterFactory.class);

    final PresetChange ionMobility = selectIonMobility(analysis, sequence, newIonInterface);
    if (ionMobility != null) {
      changes.add(ionMobility);
    }
    final IonMobilityWizardParameterFactory newIonMobility =
        ionMobility != null ? (IonMobilityWizardParameterFactory) ionMobility.to()
            : Objects.requireNonNullElse(
                currentFactory(sequence, WizardPart.IMS, IonMobilityWizardParameterFactory.class),
                IonMobilityWizardParameterFactory.NO_IMS);

    final PresetChange massSpectrometer = selectMassSpectrometer(analysis, sequence,
        newIonMobility);
    if (massSpectrometer != null) {
      changes.add(massSpectrometer);
    }
    return changes.isEmpty() ? NONE : new PresetSelection(changes);
  }

  private static <T extends WizardParameterFactory> @Nullable T currentFactory(
      @NotNull WizardSequence sequence, @NotNull WizardPart part, @NotNull Class<T> type) {
    return sequence.get(part).map(WizardStepParameters::getFactory).filter(type::isInstance)
        .map(type::cast).orElse(null);
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
   * Selects the ion mobility preset matching the mobility type of the IMS files.
   *
   * @param ionInterface the ion interface preset after its own switch, limits the ion mobility
   *                     presets
   */
  static @Nullable PresetChange selectIonMobility(@NotNull RawDataAnalysis analysis,
      @NotNull WizardSequence sequence, @Nullable IonInterfaceWizardParameterFactory ionInterface) {
    final List<@Nullable MobilityType> fileMobilityTypes = analysis.files().stream()
        .map(DataFileStatistics::file)
        .map(file -> file instanceof IMSRawDataFile ims ? ims.getMobilityType() : null).toList();
    return selectIonMobility(fileMobilityTypes, sequence, ionInterface);
  }

  /**
   * @param fileMobilityTypes the mobility type of each file, null for files without ion mobility
   */
  static @Nullable PresetChange selectIonMobility(
      @NotNull List<@Nullable MobilityType> fileMobilityTypes, @NotNull WizardSequence sequence,
      @Nullable IonInterfaceWizardParameterFactory ionInterface) {
    final IonMobilityWizardParameterFactory current = currentFactory(sequence, WizardPart.IMS,
        IonMobilityWizardParameterFactory.class);
    // decision: only switch if all files are IMS files of one mobility type, mixed data keep the
    // selected preset
    if (current == null || fileMobilityTypes.isEmpty() || fileMobilityTypes.stream()
        .anyMatch(Objects::isNull) || fileMobilityTypes.stream().distinct().count() != 1) {
      return null;
    }
    final MobilityType mobilityType = fileMobilityTypes.getFirst();
    final IonMobilityWizardParameterFactory target = presetFor(mobilityType);
    if (target == null || target == current) {
      return null;
    }
    // e.g., GC-EI does not support ion mobility
    if (ionInterface != null && !Arrays.asList(ionInterface.getMatchingImsPresets())
        .contains(target)) {
      return null;
    }
    return new PresetChange(WizardPart.IMS, current, target,
        "files are %s data".formatted(mobilityType));
  }

  private static @Nullable IonMobilityWizardParameterFactory presetFor(
      @NotNull MobilityType mobilityType) {
    return switch (mobilityType) {
      case TIMS -> IonMobilityWizardParameterFactory.TIMS;
      case DRIFT_TUBE -> IonMobilityWizardParameterFactory.DTIMS;
      case TRAVELING_WAVE -> IonMobilityWizardParameterFactory.TWIMS;
      case SLIM -> IonMobilityWizardParameterFactory.SLIM;
      case OTHER -> IonMobilityWizardParameterFactory.IMS;
      // assumption: FAIMS has no wizard preset, none and mixed do not define a single preset
      case NONE, MIXED, FAIMS -> null;
    };
  }

  /**
   * Selects Orbitrap if the MS1 scans of a file that is not a Bruker TDF file have injection times,
   * otherwise QTOF. Switches to the first allowed mass spectrometer if the ion mobility preset does
   * not allow the selected one.
   *
   * @param ionMobility the ion mobility preset after its own switch, limits the mass spectrometers
   */
  static @Nullable PresetChange selectMassSpectrometer(@NotNull RawDataAnalysis analysis,
      @NotNull WizardSequence sequence, @NotNull IonMobilityWizardParameterFactory ionMobility) {
    final MassSpectrometerWizardParameterFactory current = currentFactory(sequence, WizardPart.MS,
        MassSpectrometerWizardParameterFactory.class);
    if (current == null) {
      return null;
    }
    // the wizard limits the mass spectrometers by the ion mobility preset, e.g., TWIMS is QTOF only
    final List<MassSpectrometerWizardParameterFactory> allowed = Arrays.asList(
        ionMobility.getMatchingMassSpectrometerPresets());
    if (!allowed.contains(current)) {
      // decision: the wizard would select the first allowed one anyway
      return new PresetChange(WizardPart.MS, current, allowed.getFirst(),
          "required by %s".formatted(ionMobility));
    }
    if (!INJECTION_TIME_PRESETS.contains(current)) {
      return null;
    }

    // assumption: the representative files stem from the same instrument, so a single file with
    // injection times is enough
    final boolean injectionTimes = analysis.files().stream().map(DataFileStatistics::file)
        .filter(Objects::nonNull)
        .anyMatch(file -> !isBrukerTdf(file) && hasMs1InjectionTimes(file));
    final MassSpectrometerWizardParameterFactory target =
        injectionTimes ? MassSpectrometerWizardParameterFactory.Orbitrap
            : MassSpectrometerWizardParameterFactory.QTOF;
    if (target == current || !allowed.contains(target)) {
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

    // mirror the wizard, which uses special mass spectrometer defaults for some ion mobility
    // presets, e.g., TIMS, if the mass spectrometer still has its default parameters
    changes.stream().filter(change -> change.part() == WizardPart.IMS)
        .map(change -> (IonMobilityWizardParameterFactory) change.to())
        .map(MassSpectrometerWizardParameterFactory::createForIms).filter(Objects::nonNull)
        .findFirst().ifPresent(
            msForIms -> sequence.get(WizardPart.MS).filter(WizardStepParameters::hasDefaultParameters)
                .ifPresent(ms -> ParameterUtils.copyParameters(msForIms, ms)));
  }

  public @NotNull String describe() {
    return changes.stream().map(PresetChange::describe).collect(Collectors.joining("\n"));
  }
}
