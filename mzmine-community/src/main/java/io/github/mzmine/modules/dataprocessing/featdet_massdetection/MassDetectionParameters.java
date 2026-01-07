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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import io.github.mzmine.util.ExitCode;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MassDetectionParameters extends SimpleParameterSet {

  public static final ModuleOptionsEnumComboParameter<MassDetectors> massDetector = new ModuleOptionsEnumComboParameter<>(
      "Mass detector", "Algorithm to use for mass detection and its parameters.",
      MassDetectors.AUTO);

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter(
      new ScanSelection(1));

  public static final ComboParameter<SelectedScanTypes> scanTypes = new ComboParameter<>(
      "Scan types (IMS)", """
      Specifies the type of scans the mass detection should be applied to.
      All scans: Regular LC-MS data, and accumulated frames + mobility scans for IMS data.
      Frames only: Only applies mass detection to accumulated frames.
      Mobility scans only: Applies mass detection to mobility scans only.
      """, SelectedScanTypes.values(), SelectedScanTypes.SCANS);


  public static final BooleanParameter denormalizeMSnScans = new BooleanParameter(
      "Denormalize fragment scans (traps)", """
      Denormalize MS2 (MSn) scans by multiplying with the injection time. Encouraged before spectral merging.
      (only available in trap-based systems, like Orbitraps, trapped ion mobility spectrometry (tims), etc)
      This reduces the intensity differences between spectra acquired with different injection times
      and reverts to "raw" intensities.""", false);

  private static final Logger logger = Logger.getLogger(MassDetectionParameters.class.getName());

  public MassDetectionParameters() {
    super(new Parameter[]{dataFiles, scanSelection, scanTypes, denormalizeMSnScans, massDetector},
        "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_mass_detection/mass-detection.html");
  }

  /**
   * Check how many scans and frames there are with profile and centroided data.
   */
  private static @NotNull ScanCheckResult getScanCheckResult(RawDataFile[] selectedFiles,
      ScanSelection scanSel, SelectedScanTypes scanTypes) {
    long numCentroided = 0;
    long numProfile = 0;
    long numMobCentroided = 0;
    long numMobProfile = 0;

    for (RawDataFile file : selectedFiles) {
      Scan[] scans = scanSel.getMatchingScans(file);
      for (Scan s : scans) {
        if (scanTypes.applyTo(s)) {
          if (s.getSpectrumType() == MassSpectrumType.CENTROIDED) {
            numCentroided++;
          } else {
            numProfile++;
          }
        }
        if (s instanceof Frame f && (scanTypes == SelectedScanTypes.MOBLITY_SCANS
            || scanTypes == SelectedScanTypes.SCANS)) {
          final MassSpectrumType spectrumType = f.getMobilityScanStorage().getSpectrumType();
          if (spectrumType.isCentroided()) {
            numMobCentroided++;
          } else {
            numMobProfile++;
          }
        }
      }
    }

    ScanCheckResult result = new ScanCheckResult(numCentroided, numProfile, numMobCentroided,
        numMobProfile);
    return result;
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages,
      boolean skipRawDataAndFeatureListParameters) {
    final boolean superCheck = super.checkParameterValues(errorMessages,
        skipRawDataAndFeatureListParameters);
    // Check the selected mass detector
    MassDetectors detector = getValue(massDetector);

    // check if denormalize was selected that it matches to the mass detection algorithm
    boolean denorm = getValue(denormalizeMSnScans);
    boolean illegalDenormalizeMassDetectorCombo =
        denorm && !(detector == MassDetectors.FACTOR_OF_LOWEST);
    if (illegalDenormalizeMassDetectorCombo) {
      errorMessages.add("Spectral denormalization is currently only supported by the "
          + "Factor of the lowest mass detector; selected: " + detector);
      return false;
    }

    if (skipRawDataAndFeatureListParameters) {
      return superCheck;
    }

    // check files
    RawDataFile[] selectedFiles = getParameter(dataFiles).getValue().getMatchingRawDataFiles();
    getParameter(dataFiles).getValue().resetSelection(); // reset selection after evaluation.

    // If no file selected (e.g. in batch mode setup), just return
    if ((selectedFiles == null) || (selectedFiles.length == 0)) {
      return superCheck;
    }

    // Do an additional check for centroid/continuous data and show a
    // warning if there is a potential problem
    long numCentroided = 0, numProfile = 0;
    final ScanSelection scanSel = getParameter(scanSelection).getValue();
    final SelectedScanTypes scanTypes = getValue(MassDetectionParameters.scanTypes);
    final ScanCheckResult result = getScanCheckResult(selectedFiles, scanSel, scanTypes);

    // If no scans found, let's just stop here
    if (result.frameCentroided + result.frameProfile + result.mobScanCentroided
        + result.mobScanProfile == 0) {
      return superCheck;
    }

    // Check the selected mass detector
    StringBuilder msg = new StringBuilder();
    if ((result.frameType == MassSpectrumType.CENTROIDED
        || result.mobScanType == MassSpectrumType.CENTROIDED) && !detector.usesCentroidData()) {
      msg.append("""
          mzmine thinks you are running the profile mode mass detector on (mostly) centroided scans.
          This will likely produce wrong results. Try the Centroid mass detector or Factor of lowest signal mass detector instead.""");
    } else if ((result.frameType == MassSpectrumType.PROFILE
        || result.mobScanType == MassSpectrumType.PROFILE) && !detector.usesProfileData()) {
      msg.append("""
          mzmine thinks you are running the centroid on (mostly) profile scans.
          This will likely produce wrong results.""");
    }

    if (result.frameType != null && result.mobScanType != null
        && result.frameType != result.mobScanType && scanTypes == SelectedScanTypes.SCANS
        && detector != MassDetectors.AUTO) {
      msg.append("""
          
          Frames (%s) and mobility scans (%s) are of different scan types and thus require different mass detectors or the %s mass detector.
          It is recommended to run the Mass Detection twice with different noise levels for frames and mobility scans.
          Running as-is will likely produce wrong results.""".formatted(result.frameType,
          result.mobScanType, MassDetectors.AUTO.toString()));
    }
    if(!msg.isEmpty()) {
      msg.append("\nDo you want to continue any way?");
    }
    // open dialog on error
    if (!msg.isEmpty() && !DialogLoggerUtil.showDialogYesNo("Confirmation", msg.toString())) {
      return false;
    }

    final SelectedScanTypes types = getValue(MassDetectionParameters.scanTypes);
    if (types != SelectedScanTypes.SCANS && Arrays.stream(selectedFiles)
        .anyMatch(file -> !(file instanceof IMSRawDataFile))) {
      if (!DialogLoggerUtil.showDialogYesNo("Confirmation", """
          The scan types selection is set to "%s" but there are non IMS files selected.This will not add a mass list to the files:
          %s
          Do you want to continue anyway?""".formatted(types,
          Arrays.stream(selectedFiles).map(RawDataFile::getName)
              .collect(Collectors.joining("; "))))) {
        return false;
      }
    }

    return superCheck;
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    MassDetectorSetupDialog dialog = new MassDetectorSetupDialog(valueCheckRequired, this);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    // parameters were renamed but stayed the same type
    var nameParameterMap = super.getNameParameterMap();
    // we use the same parameters here so no need to increment the version. Loading will work fine
    nameParameterMap.put("Scans", getParameter(scanSelection));
    return nameParameterMap;
  }

  @NotNull
  @Override
  public IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  private record ScanCheckResult(long frameCentroided, long frameProfile, long mobScanCentroided,
                                 long mobScanProfile, @Nullable MassSpectrumType frameType,
                                 @Nullable MassSpectrumType mobScanType) {

    public ScanCheckResult(long frameCentroided, long frameProfile, long mobScanCentroided,
        long mobScanProfile) {
      final MassSpectrumType frameType;
      if (frameProfile + frameCentroided > 0) {
        frameType =
            (double) frameCentroided / (frameProfile + frameCentroided) < 0.5 ? MassSpectrumType.PROFILE
                : MassSpectrumType.CENTROIDED;
      } else {
        frameType = null;
      }

      final MassSpectrumType mobType;
      if (mobScanProfile + mobScanCentroided > 0) {
        mobType = (double) mobScanCentroided / (mobScanProfile + mobScanCentroided) < 0.5
            ? MassSpectrumType.PROFILE : MassSpectrumType.CENTROIDED;
      } else {
        mobType = null;
      }

      this(frameCentroided, frameProfile, mobScanCentroided, mobScanProfile, frameType, mobType);
    }

  }
}
