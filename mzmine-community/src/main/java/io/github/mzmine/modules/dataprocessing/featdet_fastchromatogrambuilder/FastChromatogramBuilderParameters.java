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

package io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.RTCorrectionParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.HiddenParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptOutParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.combowithinput.AutoOrCustomOption;
import io.github.mzmine.parameters.parametertypes.combowithinput.MZToleranceOrAuto;
import io.github.mzmine.parameters.parametertypes.combowithinput.MZToleranceOrAutoParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.ToleranceType;
import java.util.Collection;
import java.util.Map;
import javafx.scene.control.ButtonType;
import org.jetbrains.annotations.NotNull;

/**
 * decision: the parameter names equal the ADAP chromatogram builder parameters so that a batch step
 * can switch between both modules without losing its values.
 */
public class FastChromatogramBuilderParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter(
      new ScanSelection(1));

  public static final IntegerParameter minimumConsecutiveScans = new IntegerParameter(
      "Minimum consecutive scans", """
      This number of scans needs to be above the specified 'Minimum intensity for consecutive scans' to detect EICs.
      The optimal value depends on the chromatography system setup. The best way to set this parameter
      is by studying the raw data and determining what is the typical time span (number of data points) of chromatographic features.""",
      5, true, 1, null);

  public static final DoubleParameter minGroupIntensity = new DoubleParameter(
      "Minimum intensity for consecutive scans", """
      This threshold is only used to find consecutive scans (data points) above a certain intensity.
      All data points, even below this level can be added to a chromatogram but at least N consecutive scans need to be above.
      """, MZmineCore.getConfiguration().getIntensityFormat(), 0d);

  public static final DoubleParameter minHighestPoint = new DoubleParameter(
      "Minimum absolute height", """
      The consecutive scans need to reach this height. Signals below this intensity will not start a new chromatogram
      but can be added to an existing one.""", MZmineCore.getConfiguration().getIntensityFormat());

  // decision: auto by default, the presets per instrument type are only rough guesses
  public static final MZToleranceOrAutoParameter mzTolerance = new MZToleranceOrAutoParameter(
      new MZToleranceParameter(ToleranceType.SCAN_TO_SCAN, """
          Auto estimates the tolerance from a few data files: from the m/z scatter of the same \
          signal in consecutive scans and from the m/z spread of the data points in a test build \
          of the chromatograms. The estimate is logged and stored with the feature lists.
          Custom uses the defined tolerance.""", 0.002, 10), AutoOrCustomOption.AUTO);

  public static final StringParameter suffix = new StringParameter("Suffix",
      "This string is added to filename as suffix", "chromatograms");

  public static final BooleanParameter clearRtCorrection = new BooleanParameter(
      RTCorrectionParameters.clearPreviousCorrection.getName(), """
      If a file is processed multimple times, clearing potentially applied RT corrections ensures that
      the processing is reproducible for multiple runs. If no correction was applied previously,
      this parameter has no effect. Default = enabled.""", true);

  public static final HiddenParameter<Map<String, Boolean>> allowSingleScans = new HiddenParameter<>(
      new OptOutParameter("Allow single scan chromatograms",
          "Allows selection of single scans as chromatograms. This is useful for "
              + "feature table generation if MALDI point measurements."));

  public FastChromatogramBuilderParameters() {
    super(dataFiles, scanSelection, minimumConsecutiveScans, minGroupIntensity, minHighestPoint,
        mzTolerance, suffix, clearRtCorrection, allowSingleScans);
  }

  /**
   * Parameters with a custom m/z tolerance.
   */
  @NotNull
  public static FastChromatogramBuilderParameters create(@NotNull RawDataFilesSelection files,
      @NotNull ScanSelection scans, int minConsecutiveScans, @NotNull MZTolerance mzTolScans,
      @NotNull String nameSuffix, double minGroupInt, double minHeight,
      boolean clearRtCorrectionValue) {
    return create(files, scans, minConsecutiveScans, MZToleranceOrAuto.custom(mzTolScans),
        nameSuffix, minGroupInt, minHeight, clearRtCorrectionValue);
  }

  @NotNull
  public static FastChromatogramBuilderParameters create(@NotNull RawDataFilesSelection files,
      @NotNull ScanSelection scans, int minConsecutiveScans, @NotNull MZToleranceOrAuto mzTolScans,
      @NotNull String nameSuffix, double minGroupInt, double minHeight,
      boolean clearRtCorrectionValue) {
    final var param = new FastChromatogramBuilderParameters().cloneParameterSet();
    param.setParameter(dataFiles, files);
    param.setParameter(scanSelection, scans);
    param.setParameter(minimumConsecutiveScans, minConsecutiveScans);
    param.setParameter(mzTolerance, mzTolScans);
    param.setParameter(suffix, nameSuffix);
    param.setParameter(minGroupIntensity, minGroupInt);
    param.setParameter(minHighestPoint, minHeight);
    param.setParameter(clearRtCorrection, clearRtCorrectionValue);
    return (FastChromatogramBuilderParameters) param;
  }

  @NotNull
  @Override
  public IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    // same legacy names as the ADAP builder so old batch steps can switch to this module
    final var nameParameterMap = super.getNameParameterMap();
    nameParameterMap.put("Min group size in # of scans", getParameter(minimumConsecutiveScans));
    nameParameterMap.put("Group intensity threshold", getParameter(minGroupIntensity));
    nameParameterMap.put("Min highest intensity", getParameter(minHighestPoint));
    nameParameterMap.put("Scans", getParameter(scanSelection));
    nameParameterMap.put("Scan to scan accuracy (m/z)", getParameter(mzTolerance));
    return nameParameterMap;
  }

  @Override
  public void handleLoadedParameters(Map<String, Parameter<?>> loadedParams, int loadedVersion) {
    super.handleLoadedParameters(loadedParams, loadedVersion);
    if (!loadedParams.containsKey(clearRtCorrection.getName())) {
      setParameter(clearRtCorrection, true);
    }
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages) {
    if (!super.checkParameterValues(errorMessages)) {
      return false;
    }

    final Boolean singleScansOkOptOut = getParameter(allowSingleScans).getValue()
        .get("optoutsinglescancheck");

    if (getParameter(minimumConsecutiveScans).getValue() <= 1 && (singleScansOkOptOut == null
        || !singleScansOkOptOut)) {
      ButtonType buttonType = MZmineCore.getDesktop()
          .createAlertWithOptOut("Confirmation", "Single consecutive scan selected.",
              "The number of consecutive scans was set to <= 1.\nThis can lead to more noise"
                  + " detected as EICs.\nDo you want to proceed?", "Do not show again.",
              b -> this.getParameter(allowSingleScans).getValue().put("optoutsinglescancheck", b));
      return buttonType.equals(ButtonType.YES);
    }
    return true;
  }
}
