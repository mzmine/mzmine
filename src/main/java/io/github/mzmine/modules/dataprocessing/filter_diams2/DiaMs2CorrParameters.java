/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_diams2;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.ToleranceType;
import java.text.DecimalFormat;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class DiaMs2CorrParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final ScanSelectionParameter ms2ScanSelection = new ScanSelectionParameter(
      "MS2 scan selection", "Select the ms2 scans you want included for DIA MS2 correlation.",
      new ScanSelection(2));

  public static final DoubleParameter minMs1Intensity = new DoubleParameter(
      "Minimum feature intensity",
      "The minimum MS1 intensity of a feature to be considered to build a pseudo MS2 spectrum.",
      MZmineCore.getConfiguration().getIntensityFormat(), 5E3);

  public static final DoubleParameter minMs2Intensity = new DoubleParameter(
      "Minimum fragment intensity", "Minimum intensity of fragment ions to be considered.",
      MZmineCore.getConfiguration().getIntensityFormat(), 1E3);

  public static final IntegerParameter numCorrPoints = new IntegerParameter(
      "Number of correlated points",
      "The number of points to be correlated in MS1 and MS2 features.\n"
          + "Depends on cycle time and chromatographic resolution.", 5);

  public static final DoubleParameter minPearson = new DoubleParameter(
      "Minimum pearson correlation",
      "The minimum pearson correlation (R\\u00B2) between the MS1 and MS2 ion shapes.",
      new DecimalFormat("0.00"), 0.80, 0d, 1d);

  public static final MZToleranceParameter ms2ScanToScanAccuracy = new MZToleranceParameter(
      ToleranceType.SCAN_TO_SCAN, 0.003, 15);

  public DiaMs2CorrParameters() {
    super(flists, ms2ScanSelection, minMs1Intensity, minMs2Intensity, numCorrPoints, minPearson,
        ms2ScanToScanAccuracy);
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    // parameters were renamed but stayed the same type
    var nameParameterMap = super.getNameParameterMap();
    // we use the same parameters here so no need to increment the version. Loading will work fine
    nameParameterMap.put("m/z tolerance", ms2ScanToScanAccuracy);
    return nameParameterMap;
  }
}
