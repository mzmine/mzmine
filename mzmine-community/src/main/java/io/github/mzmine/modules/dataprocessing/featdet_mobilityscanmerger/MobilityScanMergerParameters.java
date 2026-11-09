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

package io.github.mzmine.modules.dataprocessing.featdet_mobilityscanmerger;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.ToleranceType;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.maths.Weighting;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MobilityScanMergerParameters extends SimpleParameterSet {

  public static final double DEFAULT_NOISE_LEVEL = 1E1;
  public static final IntensityMergingType DEFAULT_MERGING_TYPE = IntensityMergingType.SUMMED;
  public static final Weighting DEFAULT_WEIGHTING = Weighting.LINEAR;
  public static final ScanSelection DEFAULT_SCAN_SELECTION = ScanSelection.ALL_SCANS;
  public static final MZTolerance DEFAULT_MZ_TOLERANCE = new MZTolerance(0.005, 15);
  public static final int DEFAULT_MIN_DETECTIONS = 2;

  public static final RawDataFilesParameter rawDataFiles = new RawDataFilesParameter();

  public static final DoubleParameter noiseLevel = new DoubleParameter("Frame noise level",
      "Noise level for the merged frame. Merged signals below this threshold will be ignored.",
      MZmineCore.getConfiguration().getIntensityFormat(), DEFAULT_NOISE_LEVEL, 0d, 1E12);

  public static final ComboParameter<IntensityMergingType> mergingType = new ComboParameter<>(
      "Merging type", "Spectra merging algorithm", IntensityMergingType.values(),
      DEFAULT_MERGING_TYPE);

  public static final ComboParameter<Weighting> weightingType = new ComboParameter<>(
      "m/z weighting", "Weights m/z values by their intensities with the given function.",
      Weighting.values(), DEFAULT_WEIGHTING);

  public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter(
      DEFAULT_SCAN_SELECTION);

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(
      ToleranceType.SCAN_TO_SCAN, DEFAULT_MZ_TOLERANCE.getMzTolerance(),
      DEFAULT_MZ_TOLERANCE.getPpmTolerance(), false);

  public static final IntegerParameter minNumberOfDetections = new IntegerParameter(
      "Minimum detections per m/z", """
      Specify in how many mobility scans a summed m/z signal has to appear to be included in the frame.
      This can be useful to filter electrical noise signals that only appear in a single scan.""",
      DEFAULT_MIN_DETECTIONS, 1, Integer.MAX_VALUE);

  public MobilityScanMergerParameters() {
    super(new Parameter[]{rawDataFiles, noiseLevel, mergingType, weightingType, scanSelection,
            mzTolerance, minNumberOfDetections},
        "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_mobility_scan_merging/mobility-scan-merging.html");
  }

  public static @NotNull MobilityScanMergerParameters create(@NotNull RawDataFilesSelection files,
      final double noiseLevel, @NotNull IntensityMergingType mergingType,
      @NotNull Weighting weighting, @Nullable ScanSelection scanSelection,
      @NotNull MZTolerance mzTolerance, final int minDetections) {
    final MobilityScanMergerParameters param = (MobilityScanMergerParameters) new MobilityScanMergerParameters().cloneParameterSet();
    param.setParameter(MobilityScanMergerParameters.rawDataFiles, files);
    param.setParameter(MobilityScanMergerParameters.noiseLevel, noiseLevel);
    param.setParameter(MobilityScanMergerParameters.mergingType, mergingType);
    param.setParameter(MobilityScanMergerParameters.weightingType, weighting);
    // setting the value alone keeps the parameter inactive, which selects all scans
    param.getParameter(MobilityScanMergerParameters.scanSelection).setValue(scanSelection != null,
        Objects.requireNonNullElse(scanSelection, ScanSelection.ALL_SCANS));
    param.setParameter(MobilityScanMergerParameters.mzTolerance, mzTolerance);
    param.setParameter(MobilityScanMergerParameters.minNumberOfDetections, minDetections);
    return param;
  }

  public static @NotNull MobilityScanMergerParameters createDefault(
      @NotNull RawDataFilesSelection files) {
    return create(files, DEFAULT_NOISE_LEVEL, DEFAULT_MERGING_TYPE, DEFAULT_WEIGHTING,
        DEFAULT_SCAN_SELECTION, DEFAULT_MZ_TOLERANCE, DEFAULT_MIN_DETECTIONS);
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    MobilityScanMergerSetupDialog dialog = new MobilityScanMergerSetupDialog(valueCheckRequired,
        this);
    dialog.showAndWait();
    ExitCode code = dialog.getExitCode();
    return code;
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.ONLY;
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    // parameters were renamed but stayed the same type
    var nameParameterMap = super.getNameParameterMap();
    // we use the same parameters here so no need to increment the version. Loading will work fine
    nameParameterMap.put("m/z tolerance", getParameter(mzTolerance));
    return nameParameterMap;
  }

  @Override
  public int getVersion() {
    return 2;
  }
}
