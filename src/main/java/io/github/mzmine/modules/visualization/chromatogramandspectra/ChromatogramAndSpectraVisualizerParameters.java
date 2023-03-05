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

package io.github.mzmine.modules.visualization.chromatogramandspectra;

import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.ToleranceType;
import java.util.Map;

public class ChromatogramAndSpectraVisualizerParameters extends SimpleParameterSet {

  public static final MZToleranceParameter chromMzTolerance = new MZToleranceParameter(
      ToleranceType.SCAN_TO_SCAN,
      "m/z tolerance of the chromatogram builder for extracted ion chromatograms (XICs)", 0.001, 10);

  public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter(
      "Chromatogram scan selection",
      "Parameters for scan selection the chromatogram will be build on.", new ScanSelection(1));

  public static final ComboParameter<TICPlotType> plotType = new ComboParameter<>("Plot type",
      "Type of the chromatogram plot.", TICPlotType.values(), TICPlotType.BASEPEAK);

  public static final ComboParameter<ShowLegendOptions> legendOptions = new ComboParameter<>(
      "Show legend", "AUTO means that legend is deactivated for large number of samples",
      ShowLegendOptions.values(), ShowLegendOptions.AUTO);


  public static final IntegerParameter maxSamplesFeaturePick = new IntegerParameter(
      "Maximum data files feature detection",
      "Only perform auto feature detection on sample set sizes smaller equal this value", 10, true);

  public ChromatogramAndSpectraVisualizerParameters() {
    super(chromMzTolerance, scanSelection, plotType, legendOptions, maxSamplesFeaturePick);
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    // parameters were renamed but stayed the same type
    var nameParameterMap = super.getNameParameterMap();
    // we use the same parameters here so no need to increment the version. Loading will work fine
    nameParameterMap.put("XIC tolerance", chromMzTolerance);
    return nameParameterMap;
  }
}
